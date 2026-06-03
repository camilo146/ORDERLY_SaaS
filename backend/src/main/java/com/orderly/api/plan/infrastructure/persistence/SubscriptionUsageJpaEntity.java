package com.orderly.api.plan.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_usage")
public class SubscriptionUsageJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    @Column(name = "plan_order_limit", nullable = false)
    private int planOrderLimit;

    @Column(name = "orders_count", nullable = false)
    private int ordersCount = 0;

    @Column(name = "overage_count", nullable = false)
    private int overageCount = 0;

    @Column(name = "overage_block_size", nullable = false)
    private int overageBlockSize = 500;

    @Column(name = "overage_blocks", nullable = false)
    private int overageBlocks = 0;

    @Column(name = "overage_rate_cop", precision = 12, scale = 2)
    private BigDecimal overageRateCop = BigDecimal.ZERO;

    @Column(name = "overage_rate_usd", precision = 10, scale = 4)
    private BigDecimal overageRateUsd = BigDecimal.ZERO;

    @Column(name = "overage_charge_cop", precision = 12, scale = 2)
    private BigDecimal overageChargeCop = BigDecimal.ZERO;

    @Column(name = "overage_charge_usd", precision = 10, scale = 4)
    private BigDecimal overageChargeUsd = BigDecimal.ZERO;

    @Column(name = "is_finalized", nullable = false)
    private boolean isFinalized = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected SubscriptionUsageJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public OffsetDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(OffsetDateTime periodStart) { this.periodStart = periodStart; }

    public OffsetDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(OffsetDateTime periodEnd) { this.periodEnd = periodEnd; }

    public int getPlanOrderLimit() { return planOrderLimit; }
    public void setPlanOrderLimit(int planOrderLimit) { this.planOrderLimit = planOrderLimit; }

    public int getOrdersCount() { return ordersCount; }
    public void setOrdersCount(int ordersCount) { this.ordersCount = ordersCount; }

    public int getOverageCount() { return overageCount; }
    public void setOverageCount(int overageCount) { this.overageCount = overageCount; }

    public int getOverageBlockSize() { return overageBlockSize; }
    public void setOverageBlockSize(int overageBlockSize) { this.overageBlockSize = overageBlockSize; }

    public int getOverageBlocks() { return overageBlocks; }
    public void setOverageBlocks(int overageBlocks) { this.overageBlocks = overageBlocks; }

    public BigDecimal getOverageRateCop() { return overageRateCop; }
    public void setOverageRateCop(BigDecimal v) { this.overageRateCop = v; }

    public BigDecimal getOverageRateUsd() { return overageRateUsd; }
    public void setOverageRateUsd(BigDecimal v) { this.overageRateUsd = v; }

    public BigDecimal getOverageChargeCop() { return overageChargeCop; }
    public void setOverageChargeCop(BigDecimal v) { this.overageChargeCop = v; }

    public BigDecimal getOverageChargeUsd() { return overageChargeUsd; }
    public void setOverageChargeUsd(BigDecimal v) { this.overageChargeUsd = v; }

    public boolean isFinalized() { return isFinalized; }
    public void setFinalized(boolean finalized) { isFinalized = finalized; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
