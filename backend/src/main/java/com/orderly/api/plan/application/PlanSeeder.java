package com.orderly.api.plan.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds the three commercial plans at startup.
 * Idempotent — uses INSERT ... ON CONFLICT DO NOTHING.
 * Plan pricing: STARTER $17/mo, GROWTH $55/mo, BUSINESS $199/mo (USD).
 */
@Component
public class PlanSeeder {

    private static final Logger log = LoggerFactory.getLogger(PlanSeeder.class);

    private final JdbcTemplate jdbc;

    public PlanSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedPlans() {
        try {
            int seeded =
                seedIfMissing("starter",  "Starter",  1700,  17.00f,  500,  null, 1, 7, false, 9900, 1.99f, 500, false, false, false) +
                seedIfMissing("growth",   "Growth",   5500,  55.00f, 2000,  null, 3, 7, true,  9900, 1.99f, 500, false, false, true)  +
                seedIfMissing("business", "Business", 19900, 199.00f,    0,  null, 0, 7, true,     0, 0.00f, 500, true,  true,  true);

            if (seeded > 0) log.info("Seeded {} plan(s) into the plans table.", seeded);
        } catch (Exception e) {
            log.warn("Plan seeding skipped (schema may not be ready): {}", e.getMessage());
        }
    }

    /**
     * Inserts a plan if no plan with this code exists yet.
     * maxOrdersPerMonth=0 means unlimited (stored as NULL in DB).
     * maxDashboardUsers=0 means unlimited (stored as NULL in DB).
     */
    private int seedIfMissing(String code, String name,
                               int monthlyCentsCop, float monthlyUsd,
                               int orderLimitMonthly, Integer maxActiveOrders,
                               int dashboardUsers, int trialDays, boolean analytics,
                               int overageCop, float overageUsd, int blockSize,
                               boolean multiLocation, boolean customBranding, boolean prioritySupport) {
        String sql = """
                INSERT INTO plans (
                    code, name,
                    monthly_price, annual_price,
                    monthly_price_usd, annual_price_usd,
                    max_orders_per_month, max_active_orders, max_dashboard_users,
                    trial_days, analytics_enabled,
                    overage_rate_cop, overage_rate_usd, overage_block_size,
                    multi_location, custom_branding, priority_support
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (code) DO NOTHING
                """;

        Integer orderCol = orderLimitMonthly == 0 ? null : orderLimitMonthly;
        Integer usersCol  = dashboardUsers == 0    ? null : dashboardUsers;
        double annualUsd  = Math.round(monthlyUsd * 0.80f * 12 * 100.0) / 100.0;
        double annualCop  = monthlyCentsCop * 10.0;

        return jdbc.update(sql,
                code, name,
                (double) monthlyCentsCop, annualCop,
                (double) monthlyUsd, annualUsd,
                orderCol, maxActiveOrders, usersCol,
                trialDays, analytics,
                (double) overageCop, (double) overageUsd, blockSize,
                multiLocation, customBranding, prioritySupport);
    }
}
