package com.orderly.api.plan.application;

import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.domain.model.SubscriptionStatus;
import com.orderly.api.plan.domain.port.SubscriptionRepository;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaEntity;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import com.orderly.api.shared.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the full subscription lifecycle for tenant businesses.
 * Designed to be payment-provider agnostic — ready for Stripe or Wompi integration.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepo;
    private final PlanJpaRepository planRepo;
    private final UsageTrackingService usageTrackingService;

    public SubscriptionService(SubscriptionRepository subscriptionRepo,
                                PlanJpaRepository planRepo,
                                UsageTrackingService usageTrackingService) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
        this.usageTrackingService = usageTrackingService;
    }

    /**
     * Creates a trial subscription for a business on the given plan.
     * Trial length is determined by the plan's trialDays field.
     */
    @Transactional
    public Subscription createTrial(UUID businessId, String planCode) {
        PlanJpaEntity plan = planRepo.findByCodeIgnoreCase(planCode)
                .orElseThrow(() -> new DomainException("Plan not found: " + planCode));

        if (subscriptionRepo.findByBusinessId(businessId).isPresent()) {
            throw new DomainException("Business already has a subscription. Use changePlan to upgrade/downgrade.");
        }

        int trialDays = plan.getTrialDays() != null ? plan.getTrialDays() : 14;
        Subscription subscription = Subscription.createTrial(businessId, plan.getId(), trialDays);
        Subscription saved = subscriptionRepo.save(subscription);

        usageTrackingService.openPeriodForSubscription(saved, plan);
        log.info("Trial subscription created for business {} on plan {} ({} days)", businessId, planCode, trialDays);
        return saved;
    }

    /**
     * Moves a TRIAL subscription to ACTIVE and starts a fresh monthly period.
     */
    @Transactional
    public Subscription activate(UUID businessId) {
        Subscription subscription = getOrThrow(businessId);
        OffsetDateTime now = OffsetDateTime.now();
        Subscription activated = subscription.activate(now, now.plusMonths(1));
        Subscription saved = subscriptionRepo.save(activated);
        log.info("Subscription activated for business {}", businessId);
        return saved;
    }

    /**
     * Suspends a subscription (e.g., payment failure after grace period).
     */
    @Transactional
    public Subscription suspend(UUID businessId) {
        return subscriptionRepo.save(getOrThrow(businessId).suspend());
    }

    /**
     * Marks a subscription as PAST_DUE (payment overdue, still in grace period).
     */
    @Transactional
    public Subscription markPastDue(UUID businessId) {
        return subscriptionRepo.save(getOrThrow(businessId).markPastDue());
    }

    /**
     * Cancels a subscription. Sets cancelledAt timestamp.
     */
    @Transactional
    public Subscription cancel(UUID businessId) {
        Subscription cancelled = subscriptionRepo.save(getOrThrow(businessId).cancel());
        log.info("Subscription cancelled for business {}", businessId);
        return cancelled;
    }

    /**
     * Changes the plan of an existing subscription, opening a new billing period.
     */
    @Transactional
    public Subscription changePlan(UUID businessId, String newPlanCode) {
        PlanJpaEntity plan = planRepo.findByCodeIgnoreCase(newPlanCode)
                .orElseThrow(() -> new DomainException("Plan not found: " + newPlanCode));

        Subscription current = getOrThrow(businessId);
        Subscription updated = current.withPlan(plan.getId());
        Subscription saved = subscriptionRepo.save(updated);

        usageTrackingService.openPeriodForSubscription(saved, plan);
        log.info("Plan changed for business {} → {}", businessId, newPlanCode);
        return saved;
    }

    /**
     * Sets an external payment provider ID (e.g., Stripe subscription ID).
     */
    @Transactional
    public Subscription linkExternalSubscription(UUID businessId, String externalId) {
        Subscription current = getOrThrow(businessId);
        return subscriptionRepo.save(current.withExternalId(externalId));
    }

    public Optional<Subscription> findByBusinessId(UUID businessId) {
        return subscriptionRepo.findByBusinessId(businessId);
    }

    public Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId) {
        return subscriptionRepo.findByExternalSubscriptionId(externalSubscriptionId);
    }

    /** Persists a subscription domain object directly (used by webhook handlers). */
    @Transactional
    public Subscription save(Subscription subscription) {
        return subscriptionRepo.save(subscription);
    }

    public Subscription getOrThrow(UUID businessId) {
        return subscriptionRepo.findByBusinessId(businessId)
                .orElseThrow(() -> new DomainException("No subscription found for business: " + businessId));
    }

    /**
     * Resolves the plan entity for a given subscription.
     * Used by the monthly reset scheduler.
     */
    public Optional<PlanJpaEntity> resolvePlan(Subscription subscription) {
        return planRepo.findById(subscription.planId());
    }

    public boolean isEligibleForReset(Subscription subscription) {
        return subscription.status() == SubscriptionStatus.ACTIVE
                || subscription.status() == SubscriptionStatus.TRIAL;
    }
}
