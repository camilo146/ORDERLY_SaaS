package com.orderly.api.plan.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class Subscription {

    private final UUID id;
    private final UUID businessId;
    private final UUID planId;
    private final SubscriptionStatus status;
    private final String provider;
    private final String externalSubscriptionId;
    private final OffsetDateTime trialEndsAt;
    private final OffsetDateTime currentPeriodStart;
    private final OffsetDateTime currentPeriodEnd;
    private final boolean cancelAtPeriodEnd;
    private final OffsetDateTime cancelledAt;
    private final OffsetDateTime createdAt;

    private Subscription(UUID id, UUID businessId, UUID planId, SubscriptionStatus status,
                          String provider, String externalSubscriptionId,
                          OffsetDateTime trialEndsAt, OffsetDateTime currentPeriodStart,
                          OffsetDateTime currentPeriodEnd, boolean cancelAtPeriodEnd,
                          OffsetDateTime cancelledAt, OffsetDateTime createdAt) {
        this.id = id;
        this.businessId = businessId;
        this.planId = planId;
        this.status = status;
        this.provider = provider;
        this.externalSubscriptionId = externalSubscriptionId;
        this.trialEndsAt = trialEndsAt;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
    }

    public static Subscription createTrial(UUID businessId, UUID planId, int trialDays) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime trialEnd = now.plusDays(trialDays);
        return new Subscription(UUID.randomUUID(), businessId, planId,
                SubscriptionStatus.TRIAL, "manual", null,
                trialEnd, now, trialEnd, false, null, now);
    }

    public static Subscription reconstitute(UUID id, UUID businessId, UUID planId,
                                             SubscriptionStatus status, String provider,
                                             String externalSubscriptionId,
                                             OffsetDateTime trialEndsAt,
                                             OffsetDateTime currentPeriodStart,
                                             OffsetDateTime currentPeriodEnd,
                                             boolean cancelAtPeriodEnd,
                                             OffsetDateTime cancelledAt,
                                             OffsetDateTime createdAt) {
        return new Subscription(id, businessId, planId, status, provider,
                externalSubscriptionId, trialEndsAt, currentPeriodStart, currentPeriodEnd,
                cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    public Subscription activate(OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        return new Subscription(id, businessId, planId, SubscriptionStatus.ACTIVE,
                provider, externalSubscriptionId, trialEndsAt, periodStart, periodEnd,
                cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    public Subscription suspend() {
        return new Subscription(id, businessId, planId, SubscriptionStatus.SUSPENDED,
                provider, externalSubscriptionId, trialEndsAt, currentPeriodStart, currentPeriodEnd,
                cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    public Subscription markPastDue() {
        return new Subscription(id, businessId, planId, SubscriptionStatus.PAST_DUE,
                provider, externalSubscriptionId, trialEndsAt, currentPeriodStart, currentPeriodEnd,
                cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    public Subscription cancel() {
        return new Subscription(id, businessId, planId, SubscriptionStatus.CANCELED,
                provider, externalSubscriptionId, trialEndsAt, currentPeriodStart, currentPeriodEnd,
                cancelAtPeriodEnd, OffsetDateTime.now(), createdAt);
    }

    public Subscription withPlan(UUID newPlanId) {
        return new Subscription(id, businessId, newPlanId, status,
                provider, externalSubscriptionId, trialEndsAt, currentPeriodStart, currentPeriodEnd,
                cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    public Subscription withExternalId(String externalId) {
        return new Subscription(id, businessId, planId, status, provider, externalId,
                trialEndsAt, currentPeriodStart, currentPeriodEnd, cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    public Subscription extendTrial(int days) {
        OffsetDateTime newTrialEnd = (trialEndsAt != null ? trialEndsAt : OffsetDateTime.now()).plusDays(days);
        return new Subscription(id, businessId, planId, SubscriptionStatus.TRIAL,
                provider, externalSubscriptionId, newTrialEnd, currentPeriodStart, newTrialEnd,
                cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    public Subscription grantFreeMonths(int months) {
        OffsetDateTime newEnd = (currentPeriodEnd != null ? currentPeriodEnd : OffsetDateTime.now()).plusMonths(months);
        return new Subscription(id, businessId, planId, SubscriptionStatus.ACTIVE,
                provider, externalSubscriptionId, trialEndsAt, currentPeriodStart, newEnd,
                cancelAtPeriodEnd, null, createdAt);
    }

    public Subscription reactivate(OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        return new Subscription(id, businessId, planId, SubscriptionStatus.ACTIVE,
                provider, externalSubscriptionId, trialEndsAt, periodStart, periodEnd,
                false, null, createdAt);
    }

    /** Activates the subscription via Stripe, setting the provider and external ID. */
    public Subscription activateViaStripe(String stripeSubscriptionId,
                                          OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        return new Subscription(id, businessId, planId, SubscriptionStatus.ACTIVE,
                "stripe", stripeSubscriptionId, trialEndsAt, periodStart, periodEnd,
                false, null, createdAt);
    }

    /** Sets the Stripe subscription ID while keeping current status. */
    public Subscription withStripeId(String stripeSubscriptionId) {
        return new Subscription(id, businessId, planId, status, "stripe", stripeSubscriptionId,
                trialEndsAt, currentPeriodStart, currentPeriodEnd, cancelAtPeriodEnd, cancelledAt, createdAt);
    }

    /** Updates the cancel-at-period-end flag (scheduled cancellation from Stripe). */
    public Subscription withCancelAtPeriodEnd(boolean value) {
        return new Subscription(id, businessId, planId, status, provider, externalSubscriptionId,
                trialEndsAt, currentPeriodStart, currentPeriodEnd, value, cancelledAt, createdAt);
    }

    /** Renews the billing period on invoice.paid and clears past-due state. */
    public Subscription renewPeriod(OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        return new Subscription(id, businessId, planId, SubscriptionStatus.ACTIVE, provider, externalSubscriptionId,
                trialEndsAt, periodStart, periodEnd, false, null, createdAt);
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL;
    }

    public UUID id() { return id; }
    public UUID businessId() { return businessId; }
    public UUID planId() { return planId; }
    public SubscriptionStatus status() { return status; }
    public String provider() { return provider; }
    public String externalSubscriptionId() { return externalSubscriptionId; }
    public OffsetDateTime trialEndsAt() { return trialEndsAt; }
    public OffsetDateTime currentPeriodStart() { return currentPeriodStart; }
    public OffsetDateTime currentPeriodEnd() { return currentPeriodEnd; }
    public boolean cancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public OffsetDateTime cancelledAt() { return cancelledAt; }
    public OffsetDateTime createdAt() { return createdAt; }
}
