package com.orderly.api.plan.application;

import com.orderly.api.plan.domain.model.SubscriptionUsage;
import com.orderly.api.plan.domain.port.SubscriptionUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Finalizes billing periods and opens new ones on the first day of each month.
 * Logs overage charges to enable manual or automated invoicing.
 */
@Component
public class MonthlyResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyResetScheduler.class);

    private final SubscriptionUsageRepository usageRepo;
    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;

    public MonthlyResetScheduler(SubscriptionUsageRepository usageRepo,
                                  SubscriptionService subscriptionService,
                                  UsageTrackingService usageTrackingService) {
        this.usageRepo = usageRepo;
        this.subscriptionService = subscriptionService;
        this.usageTrackingService = usageTrackingService;
    }

    /**
     * Runs at 00:05 on the first day of every month (server timezone).
     * Finalizes open periods and opens new ones for active subscriptions.
     */
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void rollBillingPeriods() {
        log.info("Starting monthly billing period rollover...");
        List<SubscriptionUsage> openPeriods = usageRepo.findOpenPeriods();

        int finalized = 0;
        int opened = 0;

        for (SubscriptionUsage usage : openPeriods) {
            usageRepo.save(usage.closeperiod());
            logPeriodSummary(usage);
            finalized++;

            boolean reopened = subscriptionService.findByBusinessId(usage.businessId())
                    .filter(subscriptionService::isEligibleForReset)
                    .flatMap(subscriptionService::resolvePlan)
                    .map(plan -> {
                        subscriptionService.findByBusinessId(usage.businessId())
                                .ifPresent(sub -> usageTrackingService.openPeriodForSubscription(sub, plan));
                        return true;
                    })
                    .orElse(false);

            if (reopened) opened++;
        }

        log.info("Monthly rollover complete — {} periods finalized, {} new periods opened.", finalized, opened);
    }

    private void logPeriodSummary(SubscriptionUsage usage) {
        if (usage.overageCount() > 0) {
            log.warn("BILLING OVERAGE — business={} period={}/{} orders={} limit={} overage={} blocks={} chargeCOP={} chargeUSD={}",
                    usage.businessId(),
                    usage.periodStart().toLocalDate(),
                    usage.periodEnd().toLocalDate(),
                    usage.ordersCount(),
                    usage.planOrderLimit(),
                    usage.overageCount(),
                    usage.overageBlocks(),
                    usage.overageChargeCop(),
                    usage.overageChargeUsd());
        } else {
            log.info("Period finalized — business={} orders={}/{} (no overage)",
                    usage.businessId(), usage.ordersCount(), usage.planOrderLimit());
        }
    }
}
