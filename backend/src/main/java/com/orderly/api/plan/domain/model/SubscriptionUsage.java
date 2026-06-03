package com.orderly.api.plan.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class SubscriptionUsage {

    private final UUID id;
    private final UUID businessId;
    private final UUID subscriptionId;
    private final OffsetDateTime periodStart;
    private final OffsetDateTime periodEnd;
    private final int planOrderLimit;
    private final int ordersCount;
    private final int overageCount;
    private final int overageBlockSize;
    private final int overageBlocks;
    private final BigDecimal overageRateCop;
    private final BigDecimal overageRateUsd;
    private final BigDecimal overageChargeCop;
    private final BigDecimal overageChargeUsd;
    private final boolean finalized;
    private final OffsetDateTime createdAt;

    private SubscriptionUsage(UUID id, UUID businessId, UUID subscriptionId,
                               OffsetDateTime periodStart, OffsetDateTime periodEnd,
                               int planOrderLimit, int ordersCount, int overageCount,
                               int overageBlockSize, int overageBlocks,
                               BigDecimal overageRateCop, BigDecimal overageRateUsd,
                               BigDecimal overageChargeCop, BigDecimal overageChargeUsd,
                               boolean finalized, OffsetDateTime createdAt) {
        this.id = id;
        this.businessId = businessId;
        this.subscriptionId = subscriptionId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.planOrderLimit = planOrderLimit;
        this.ordersCount = ordersCount;
        this.overageCount = overageCount;
        this.overageBlockSize = overageBlockSize;
        this.overageBlocks = overageBlocks;
        this.overageRateCop = overageRateCop;
        this.overageRateUsd = overageRateUsd;
        this.overageChargeCop = overageChargeCop;
        this.overageChargeUsd = overageChargeUsd;
        this.finalized = finalized;
        this.createdAt = createdAt;
    }

    public static SubscriptionUsage openPeriod(UUID businessId, UUID subscriptionId,
                                                OffsetDateTime periodStart, OffsetDateTime periodEnd,
                                                int planOrderLimit, int overageBlockSize,
                                                BigDecimal overageRateCop, BigDecimal overageRateUsd) {
        return new SubscriptionUsage(UUID.randomUUID(), businessId, subscriptionId,
                periodStart, periodEnd, planOrderLimit, 0, 0,
                overageBlockSize, 0, overageRateCop, overageRateUsd,
                BigDecimal.ZERO, BigDecimal.ZERO, false, OffsetDateTime.now());
    }

    public static SubscriptionUsage reconstitute(UUID id, UUID businessId, UUID subscriptionId,
                                                  OffsetDateTime periodStart, OffsetDateTime periodEnd,
                                                  int planOrderLimit, int ordersCount, int overageCount,
                                                  int overageBlockSize, int overageBlocks,
                                                  BigDecimal overageRateCop, BigDecimal overageRateUsd,
                                                  BigDecimal overageChargeCop, BigDecimal overageChargeUsd,
                                                  boolean finalized, OffsetDateTime createdAt) {
        return new SubscriptionUsage(id, businessId, subscriptionId, periodStart, periodEnd,
                planOrderLimit, ordersCount, overageCount, overageBlockSize, overageBlocks,
                overageRateCop, overageRateUsd, overageChargeCop, overageChargeUsd, finalized, createdAt);
    }

    public SubscriptionUsage incrementOrders() {
        int newCount = this.ordersCount + 1;
        int newOverage = Math.max(0, newCount - planOrderLimit);
        int newBlocks = newOverage > 0
                ? (int) Math.ceil((double) newOverage / overageBlockSize)
                : 0;
        BigDecimal newChargeCop = overageRateCop.multiply(BigDecimal.valueOf(newBlocks));
        BigDecimal newChargeUsd = overageRateUsd.multiply(BigDecimal.valueOf(newBlocks));
        return new SubscriptionUsage(id, businessId, subscriptionId, periodStart, periodEnd,
                planOrderLimit, newCount, newOverage, overageBlockSize, newBlocks,
                overageRateCop, overageRateUsd, newChargeCop, newChargeUsd, finalized, createdAt);
    }

    public SubscriptionUsage closeperiod() {
        return new SubscriptionUsage(id, businessId, subscriptionId, periodStart, periodEnd,
                planOrderLimit, ordersCount, overageCount, overageBlockSize, overageBlocks,
                overageRateCop, overageRateUsd, overageChargeCop, overageChargeUsd, true, createdAt);
    }

    public UUID id() { return id; }
    public UUID businessId() { return businessId; }
    public UUID subscriptionId() { return subscriptionId; }
    public OffsetDateTime periodStart() { return periodStart; }
    public OffsetDateTime periodEnd() { return periodEnd; }
    public int planOrderLimit() { return planOrderLimit; }
    public int ordersCount() { return ordersCount; }
    public int overageCount() { return overageCount; }
    public int overageBlockSize() { return overageBlockSize; }
    public int overageBlocks() { return overageBlocks; }
    public BigDecimal overageRateCop() { return overageRateCop; }
    public BigDecimal overageRateUsd() { return overageRateUsd; }
    public BigDecimal overageChargeCop() { return overageChargeCop; }
    public BigDecimal overageChargeUsd() { return overageChargeUsd; }
    public boolean isFinalized() { return finalized; }
    public OffsetDateTime createdAt() { return createdAt; }
}
