package com.orderly.api.plan.application;

import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.domain.model.SubscriptionUsage;
import com.orderly.api.plan.domain.port.SubscriptionRepository;
import com.orderly.api.plan.domain.port.SubscriptionUsageRepository;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaEntity;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks monthly order usage per business and computes overage charges.
 * Soft limit: orders are never blocked; excess usage is recorded for billing.
 */
@Service
public class UsageTrackingService {

    private static final Logger log = LoggerFactory.getLogger(UsageTrackingService.class);

    private final SubscriptionUsageRepository usageRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final PlanJpaRepository planRepo;

    public UsageTrackingService(SubscriptionUsageRepository usageRepo,
                                 SubscriptionRepository subscriptionRepo,
                                 PlanJpaRepository planRepo) {
        this.usageRepo = usageRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
    }

    /**
     * Called after each order is created. Increments the monthly counter and
     * tracks overage charges if the plan limit has been exceeded.
     */
    @Transactional
    public void recordOrderCreated(UUID businessId) {
        OffsetDateTime now = OffsetDateTime.now();
        Optional<SubscriptionUsage> usageOpt = usageRepo.findCurrentPeriod(businessId, now);

        if (usageOpt.isEmpty()) {
            Optional<Subscription> subOpt = subscriptionRepo.findByBusinessId(businessId);
            if (subOpt.isEmpty()) {
                log.debug("No subscription for business {}, skipping usage tracking", businessId);
                return;
            }
            Subscription sub = subOpt.get();
            planRepo.findById(sub.planId()).ifPresent(plan -> openPeriodForSubscription(sub, plan));
            usageOpt = usageRepo.findCurrentPeriod(businessId, now);
        }

        usageOpt.ifPresent(usage -> {
            SubscriptionUsage updated = usage.incrementOrders();
            usageRepo.save(updated);
            if (updated.overageCount() > 0) {
                log.info("Business {} overage — extra orders: {}, blocks: {}, charge COP: {}",
                        businessId, updated.overageCount(), updated.overageBlocks(), updated.overageChargeCop());
            }
        });
    }

    /**
     * Opens a new billing period for a subscription. Idempotent: does nothing
     * if a non-finalized period for the current month already exists.
     */
    @Transactional
    public void openPeriodForSubscription(Subscription subscription, PlanJpaEntity plan) {
        OffsetDateTime now = OffsetDateTime.now();
        if (usageRepo.findCurrentPeriod(subscription.businessId(), now).isPresent()) return;

        OffsetDateTime periodStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime periodEnd = periodStart.plusMonths(1);

        int orderLimit = plan.getMaxOrdersPerMonth() != null ? plan.getMaxOrdersPerMonth() : 0;
        int blockSize = plan.getOverageBlockSize() != null ? plan.getOverageBlockSize() : 500;
        BigDecimal rateCop = plan.getOverageRateCop() != null ? plan.getOverageRateCop() : BigDecimal.ZERO;
        BigDecimal rateUsd = plan.getOverageRateUsd() != null ? plan.getOverageRateUsd() : BigDecimal.ZERO;

        SubscriptionUsage usage = SubscriptionUsage.openPeriod(
                subscription.businessId(), subscription.id(),
                periodStart, periodEnd, orderLimit, blockSize, rateCop, rateUsd);
        usageRepo.save(usage);
        log.info("Opened billing period for business {} ({} → {})", subscription.businessId(), periodStart, periodEnd);
    }

    public Optional<SubscriptionUsage> getCurrentPeriodUsage(UUID businessId) {
        return usageRepo.findCurrentPeriod(businessId, OffsetDateTime.now());
    }
}
