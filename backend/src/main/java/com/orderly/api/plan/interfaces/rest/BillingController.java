package com.orderly.api.plan.interfaces.rest;

import com.orderly.api.business.infrastructure.persistence.BusinessJpaRepository;
import com.orderly.api.plan.application.SubscriptionService;
import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import com.orderly.api.plan.infrastructure.stripe.StripeProperties;
import com.orderly.api.plan.infrastructure.stripe.StripeService;
import com.orderly.api.shared.domain.DomainException;
import com.orderly.api.shared.security.persistence.UserJpaRepository;
import com.orderly.api.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Billing endpoints: Stripe Checkout Session and Customer Portal Session.
 * These are the only entry points into Stripe-specific payment flows.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/billing")
public class BillingController {

    private final StripeService stripeService;
    private final StripeProperties stripeProperties;
    private final SubscriptionService subscriptionService;
    private final BusinessJpaRepository businessRepo;
    private final UserJpaRepository userRepo;
    private final PlanJpaRepository planRepo;
    private final TenantAccessService tenantAccessService;

    public BillingController(StripeService stripeService,
                              StripeProperties stripeProperties,
                              SubscriptionService subscriptionService,
                              BusinessJpaRepository businessRepo,
                              UserJpaRepository userRepo,
                              PlanJpaRepository planRepo,
                              TenantAccessService tenantAccessService) {
        this.stripeService = stripeService;
        this.stripeProperties = stripeProperties;
        this.subscriptionService = subscriptionService;
        this.businessRepo = businessRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.tenantAccessService = tenantAccessService;
    }

    /**
     * Creates a Stripe Checkout Session for the given plan.
     * Returns the hosted Stripe checkout URL — the frontend redirects to it.
     */
    @PostMapping("/checkout-session")
    public ResponseEntity<SessionUrlResponse> createCheckoutSession(
            @PathVariable UUID businessId,
            @Valid @RequestBody CheckoutRequest request) {

        tenantAccessService.validateTenantAccess(businessId);

        var business = businessRepo.findById(businessId)
                .orElseThrow(() -> new DomainException("Business not found"));

        String ownerEmail = resolveOwnerEmail(business.getOwnerId());
        String customerId = stripeService.findOrCreateCustomer(businessId, ownerEmail, business.getName());

        String priceId = stripeProperties.resolvePriceId(request.planCode(), request.cycle());

        var planOpt = planRepo.findByCodeIgnoreCase(request.planCode());
        int trialDays = planOpt.map(p -> p.getTrialDays() != null ? p.getTrialDays() : 14).orElse(14);

        String url = stripeService.createCheckoutSession(customerId, priceId, businessId, request.planCode(), trialDays);
        return ResponseEntity.ok(new SessionUrlResponse(url));
    }

    /**
     * Creates a Stripe Customer Portal Session.
     * Returns the portal URL — the frontend redirects to it for self-service billing management.
     */
    @PostMapping("/portal-session")
    public ResponseEntity<SessionUrlResponse> createPortalSession(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);

        var business = businessRepo.findById(businessId)
                .orElseThrow(() -> new DomainException("Business not found"));

        if (business.getStripeCustomerId() == null) {
            throw new DomainException("No Stripe customer found for this business. Complete a checkout first.");
        }

        String url = stripeService.createPortalSession(business.getStripeCustomerId());
        return ResponseEntity.ok(new SessionUrlResponse(url));
    }

    // ── Request / Response records ─────────────────────────────────────────

    public record CheckoutRequest(
            @NotBlank String planCode,
            @NotBlank @Pattern(regexp = "monthly|annual") String cycle
    ) {}

    public record SessionUrlResponse(String url) {}

    // ── Private helpers ────────────────────────────────────────────────────

    private String resolveOwnerEmail(UUID ownerId) {
        return userRepo.findById(ownerId)
                .map(u -> u.getEmail())
                .orElseThrow(() -> new DomainException("Owner not found for business"));
    }
}
