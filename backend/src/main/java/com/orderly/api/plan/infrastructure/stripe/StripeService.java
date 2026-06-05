package com.orderly.api.plan.infrastructure.stripe;

import com.orderly.api.business.infrastructure.persistence.BusinessJpaRepository;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import com.orderly.api.shared.domain.DomainException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.model.billingportal.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.billingportal.SessionCreateParams.Builder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Core Stripe operations: customer management, checkout sessions, customer portal.
 * All Stripe API calls are isolated in this class to simplify future provider swaps.
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final StripeProperties stripeProperties;
    private final BusinessJpaRepository businessRepo;

    public StripeService(StripeProperties stripeProperties,
                         BusinessJpaRepository businessRepo) {
        this.stripeProperties = stripeProperties;
        this.businessRepo = businessRepo;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = stripeProperties.secretKey();
        if (stripeProperties.isConfigured()) {
            log.info("Stripe initialized with live/test key.");
        } else {
            log.warn("Stripe is running with placeholder key — checkout sessions will fail until STRIPE_SECRET_KEY is set.");
        }
    }

    /**
     * Returns the Stripe customer ID for a business, creating one if it doesn't exist yet.
     * The customer ID is persisted in the businesses table for future lookups.
     */
    @Transactional
    public String findOrCreateCustomer(UUID businessId, String ownerEmail, String businessName) {
        var entity = businessRepo.findById(businessId)
                .orElseThrow(() -> new DomainException("Business not found: " + businessId));

        if (entity.getStripeCustomerId() != null) {
            return entity.getStripeCustomerId();
        }

        try {
            // Search Stripe first to avoid creating duplicate customers
            CustomerListParams listParams = CustomerListParams.builder()
                    .setEmail(ownerEmail)
                    .setLimit(1L)
                    .build();
            CustomerCollection existing = Customer.list(listParams);
            String customerId;

            if (!existing.getData().isEmpty()) {
                customerId = existing.getData().get(0).getId();
                log.info("Reusing existing Stripe customer {} for business {}", customerId, businessId);
            } else {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("businessId", businessId.toString());
                metadata.put("businessName", businessName);

                CustomerCreateParams createParams = CustomerCreateParams.builder()
                        .setEmail(ownerEmail)
                        .setName(businessName)
                        .putAllMetadata(metadata)
                        .build();
                Customer customer = Customer.create(createParams);
                customerId = customer.getId();
                log.info("Created Stripe customer {} for business {}", customerId, businessId);
            }

            businessRepo.updateStripeCustomerId(businessId, customerId);
            return customerId;

        } catch (StripeException e) {
            log.error("Stripe customer operation failed for business {}: {}", businessId, e.getMessage());
            throw new DomainException("No se pudo procesar la operación de pago. Intenta de nuevo.");
        }
    }

    /**
     * Creates a Stripe Checkout Session for subscription purchase.
     * The businessId is stored in session metadata so webhooks can correlate the event.
     */
    public String createCheckoutSession(String customerId, String priceId,
                                        UUID businessId, String planCode, int trialDays) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("businessId", businessId.toString());
            metadata.put("planCode", planCode);

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(stripeProperties.successUrl() + "&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(stripeProperties.cancelUrl())
                    .putAllMetadata(metadata)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build());

            if (trialDays > 0) {
                builder.setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .setTrialPeriodDays((long) trialDays)
                                .putAllMetadata(metadata)
                                .build());
            }

            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(builder.build());
            log.info("Stripe checkout session {} created for business {}", session.getId(), businessId);
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed for business {}: {}", businessId, e.getMessage());
            throw new DomainException("No se pudo iniciar el proceso de pago. Intenta de nuevo.");
        }
    }

    /**
     * Creates a Stripe Customer Portal Session for managing billing, payment methods, and invoices.
     */
    public String createPortalSession(String customerId) {
        try {
            Builder builder = com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setReturnUrl(stripeProperties.portalReturnUrl());

            Session session = Session.create(builder.build());
            log.info("Stripe portal session created for customer {}", customerId);
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Stripe portal session creation failed for customer {}: {}", customerId, e.getMessage());
            throw new DomainException("No se pudo abrir el portal de facturación. Intenta de nuevo.");
        }
    }

    public StripeProperties properties() {
        return stripeProperties;
    }
}
