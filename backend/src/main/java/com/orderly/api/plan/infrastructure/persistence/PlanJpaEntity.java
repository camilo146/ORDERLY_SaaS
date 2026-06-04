package com.orderly.api.plan.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for subscription plans.
 * Maps to the 'plans' table (populated by V1 + V2 + V6 Flyway migrations).
 */
@Entity
@Table(name = "plans")
public class PlanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String code;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "monthly_price", precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "annual_price", precision = 12, scale = 2)
    private BigDecimal annualPrice;

    @Column(name = "monthly_price_usd", precision = 12, scale = 4)
    private BigDecimal monthlyPriceUsd;

    @Column(name = "annual_price_usd", precision = 12, scale = 4)
    private BigDecimal annualPriceUsd;

    @Column(name = "max_products")
    private Integer maxProducts;

    @Column(name = "max_orders_per_month")
    private Integer maxOrdersPerMonth;

    @Column(name = "max_active_orders")
    private Integer maxActiveOrders; // null = unlimited (ENTERPRISE)

    @Column(name = "max_dashboard_users")
    private Integer maxDashboardUsers;

    @Column(name = "overage_rate_cop", precision = 12, scale = 2)
    private BigDecimal overageRateCop;

    @Column(name = "overage_rate_usd", precision = 10, scale = 4)
    private BigDecimal overageRateUsd;

    @Column(name = "overage_block_size")
    private Integer overageBlockSize;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "analytics_enabled")
    private Boolean analyticsEnabled;

    @Column(name = "multi_location")
    private Boolean multiLocation;

    @Column(name = "custom_branding")
    private Boolean customBranding;

    @Column(name = "priority_support")
    private Boolean prioritySupport;

    @Column(name = "stripe_price_id_monthly", length = 255)
    private String stripePriceIdMonthly;

    @Column(name = "stripe_price_id_annual", length = 255)
    private String stripePriceIdAnnual;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected PlanJpaEntity() {
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public BigDecimal getAnnualPrice() { return annualPrice; }
    public BigDecimal getMonthlyPriceUsd() { return monthlyPriceUsd; }
    public BigDecimal getAnnualPriceUsd() { return annualPriceUsd; }
    public Integer getMaxProducts() { return maxProducts; }
    public Integer getMaxOrdersPerMonth() { return maxOrdersPerMonth; }
    public Integer getMaxActiveOrders() { return maxActiveOrders; }
    public Integer getMaxDashboardUsers() { return maxDashboardUsers; }
    public BigDecimal getOverageRateCop() { return overageRateCop; }
    public BigDecimal getOverageRateUsd() { return overageRateUsd; }
    public Integer getOverageBlockSize() { return overageBlockSize; }
    public Integer getTrialDays() { return trialDays; }
    public Boolean getAnalyticsEnabled() { return analyticsEnabled; }
    public Boolean getMultiLocation() { return multiLocation; }
    public Boolean getCustomBranding() { return customBranding; }
    public Boolean getPrioritySupport() { return prioritySupport; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getStripePriceIdMonthly() { return stripePriceIdMonthly; }
    public String getStripePriceIdAnnual() { return stripePriceIdAnnual; }
}
