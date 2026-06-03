package com.orderly.api.plan.infrastructure.stripe;

import com.orderly.api.plan.application.SubscriptionService;
import com.orderly.api.plan.application.UsageTrackingService;
import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaEntity;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Processes Stripe webhook events and synchronizes them with internal subscription state.
 * All handlers are idempotent — events can be safely replayed.
 */
@Service
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);

    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;
    private final PlanJpaRepository planRepo;
    private final JdbcTemplate jdbc;

    public StripeWebhookHandler(SubscriptionService subscriptionService,
                                 UsageTrackingService usageTrackingService,
                                 PlanJpaRepository planRepo,
                                 JdbcTemplate jdbc) {
        this.subscriptionService = subscriptionService;
        this.usageTrackingService = usageTrackingService;
        this.planRepo = planRepo;
        this.jdbc = jdbc;
    }

    /**
     * Returns true if this event has already been processed (idempotency check).
     */
    public boolean isAlreadyProcessed(String eventId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stripe_processed_events WHERE event_id = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    /**
     * Marks an event as processed to prevent duplicate handling.
     */
    public void markProcessed(String eventId, String eventType) {
        jdbc.update(
                "INSERT INTO stripe_processed_events (event_id, event_type) VALUES (?, ?) ON CONFLICT DO NOTHING",
                eventId, eventType);
    }

    /**
     * checkout.session.completed — subscription payment or trial setup confirmed.
     * Links the Stripe subscription to the internal subscription and activates it.
     */
    @Transactional
    public void handleCheckoutSessionCompleted(Session session) {
        String businessIdStr = session.getMetadata() != null ? session.getMetadata().get("businessId") : null;
        if (businessIdStr == null) {
            log.warn("checkout.session.completed missing businessId in metadata, sessionId={}", session.getId());
            return;
        }

        UUID businessId = UUID.fromString(businessIdStr);
        String stripeSubscriptionId = session.getSubscription();

        Optional<Subscription> subOpt = subscriptionService.findByBusinessId(businessId);
        if (subOpt.isEmpty()) {
            log.warn("No internal subscription found for businessId={}", businessId);
            return;
        }

        Subscription sub = subOpt.get();
        OffsetDateTime now = OffsetDateTime.now();

        // Activate and link the Stripe subscription ID
        Subscription updated = sub.activateViaStripe(stripeSubscriptionId, now, now.plusMonths(1));
        subscriptionService.save(updated);

        log.info("Subscription activated via Stripe checkout: businessId={}, stripeSubId={}",
                businessId, stripeSubscriptionId);
    }

    /**
     * invoice.paid — successful payment; renews the billing period.
     */
    @Transactional
    public void handleInvoicePaid(Invoice invoice) {
        String stripeSubscriptionId = invoice.getSubscription();
        if (stripeSubscriptionId == null) return;

        Optional<Subscription> subOpt = subscriptionService.findByExternalSubscriptionId(stripeSubscriptionId);
        if (subOpt.isEmpty()) {
            log.debug("invoice.paid: no subscription found for stripeSubId={}", stripeSubscriptionId);
            return;
        }

        Subscription sub = subOpt.get();
        OffsetDateTime periodStart = epochToOffset(invoice.getPeriodStart());
        OffsetDateTime periodEnd   = epochToOffset(invoice.getPeriodEnd());

        Subscription renewed = sub.renewPeriod(periodStart, periodEnd);
        subscriptionService.save(renewed);

        // Open a new usage tracking period
        PlanJpaEntity plan = planRepo.findById(sub.planId()).orElse(null);
        if (plan != null) {
            usageTrackingService.openPeriodForSubscription(renewed, plan);
        }

        log.info("Subscription period renewed: stripeSubId={}, period={} → {}",
                stripeSubscriptionId, periodStart, periodEnd);
    }

    /**
     * invoice.payment_failed — payment overdue; enters grace period.
     */
    @Transactional
    public void handleInvoicePaymentFailed(Invoice invoice) {
        String stripeSubscriptionId = invoice.getSubscription();
        if (stripeSubscriptionId == null) return;

        Optional<Subscription> subOpt = subscriptionService.findByExternalSubscriptionId(stripeSubscriptionId);
        if (subOpt.isEmpty()) return;

        Subscription sub = subOpt.get();
        subscriptionService.save(sub.markPastDue());
        log.warn("Payment failed, subscription marked PAST_DUE: stripeSubId={}", stripeSubscriptionId);
    }

    /**
     * customer.subscription.updated — plan change, cancel scheduling, reactivation.
     */
    @Transactional
    public void handleSubscriptionUpdated(com.stripe.model.Subscription stripeSubscription) {
        String stripeSubId = stripeSubscription.getId();

        Optional<Subscription> subOpt = subscriptionService.findByExternalSubscriptionId(stripeSubId);
        if (subOpt.isEmpty()) {
            log.debug("subscription.updated: no internal subscription for stripeSubId={}", stripeSubId);
            return;
        }

        Subscription sub = subOpt.get();
        String stripeStatus = stripeSubscription.getStatus();
        boolean cancelAtPeriodEnd = Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd());

        Subscription updated = switch (stripeStatus) {
            case "active"    -> sub.withCancelAtPeriodEnd(cancelAtPeriodEnd)
                                   .renewPeriod(
                                       epochToOffset(stripeSubscription.getCurrentPeriodStart()),
                                       epochToOffset(stripeSubscription.getCurrentPeriodEnd()));
            case "trialing"  -> sub.withCancelAtPeriodEnd(cancelAtPeriodEnd);
            case "past_due"  -> sub.markPastDue();
            case "canceled"  -> sub.cancel();
            case "unpaid"    -> sub.suspend();
            default          -> sub;
        };

        subscriptionService.save(updated);
        log.info("Subscription updated from Stripe: stripeSubId={}, status={}, cancelAtEnd={}",
                stripeSubId, stripeStatus, cancelAtPeriodEnd);
    }

    /**
     * customer.subscription.deleted — hard cancellation at period end or immediately.
     */
    @Transactional
    public void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSubscription) {
        String stripeSubId = stripeSubscription.getId();

        subscriptionService.findByExternalSubscriptionId(stripeSubId).ifPresent(sub -> {
            subscriptionService.save(sub.cancel());
            log.info("Subscription cancelled via Stripe deletion: stripeSubId={}", stripeSubId);
        });
    }

    private OffsetDateTime epochToOffset(Long epochSeconds) {
        if (epochSeconds == null) return OffsetDateTime.now();
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }
}
