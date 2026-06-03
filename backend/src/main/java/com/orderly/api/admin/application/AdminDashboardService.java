package com.orderly.api.admin.application;

import com.orderly.api.admin.domain.model.AdminAuditLog;
import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.model.BusinessStatus;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.order.infrastructure.persistence.OrderJpaRepository;
import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.domain.model.SubscriptionStatus;
import com.orderly.api.plan.domain.port.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private final BusinessRepository businessRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final OrderJpaRepository orderJpaRepo;
    private final AuditLogService auditLogService;

    public AdminDashboardService(BusinessRepository businessRepo,
                                  SubscriptionRepository subscriptionRepo,
                                  OrderJpaRepository orderJpaRepo,
                                  AuditLogService auditLogService) {
        this.businessRepo = businessRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.orderJpaRepo = orderJpaRepo;
        this.auditLogService = auditLogService;
    }

    public GlobalDashboardMetrics getGlobalMetrics() {
        List<Business> allBusinesses = businessRepo.findAll();

        Map<BusinessStatus, Long> byStatus = allBusinesses.stream()
                .collect(Collectors.groupingBy(Business::status, Collectors.counting()));

        long totalActive    = byStatus.getOrDefault(BusinessStatus.ACTIVE, 0L);
        long totalTrialing  = byStatus.getOrDefault(BusinessStatus.TRIALING, 0L);
        long totalSuspended = byStatus.getOrDefault(BusinessStatus.SUSPENDED, 0L);
        long totalCancelled = byStatus.getOrDefault(BusinessStatus.CANCELLED, 0L);
        long totalSetup     = byStatus.getOrDefault(BusinessStatus.SETUP, 0L)
                + byStatus.getOrDefault(BusinessStatus.PENDING_WHATSAPP, 0L);

        List<Subscription> allSubscriptions = allBusinesses.stream()
                .map(b -> subscriptionRepo.findByBusinessId(b.id()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        long activeSubs   = allSubscriptions.stream().filter(s -> s.status() == SubscriptionStatus.ACTIVE).count();
        long trialSubs    = allSubscriptions.stream().filter(s -> s.status() == SubscriptionStatus.TRIAL).count();
        long suspendedSubs = allSubscriptions.stream().filter(s -> s.status() == SubscriptionStatus.SUSPENDED).count();

        OffsetDateTime startOfToday  = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime startOfMonth  = OffsetDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        long ordersToday     = orderJpaRepo.countOrdersSinceGlobal(startOfToday);
        long ordersThisMonth = orderJpaRepo.countOrdersSinceGlobal(startOfMonth);
        long ordersTotal     = orderJpaRepo.count();

        BigDecimal revenueThisMonth = orderJpaRepo.sumRevenueGlobalSince(startOfMonth);
        BigDecimal revenueTotal     = orderJpaRepo.sumRevenueGlobal();

        List<RecentActivityItem> recentActivity = auditLogService.findRecent(10)
                .stream().map(RecentActivityItem::from).toList();

        return new GlobalDashboardMetrics(
                allBusinesses.size(), totalActive, totalTrialing, totalSuspended, totalCancelled, totalSetup,
                activeSubs, trialSubs, suspendedSubs,
                ordersToday, ordersThisMonth, ordersTotal,
                revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO,
                revenueTotal != null ? revenueTotal : BigDecimal.ZERO,
                recentActivity);
    }

    public record GlobalDashboardMetrics(
            long totalBusinesses,
            long activeBusinesses,
            long trialingBusinesses,
            long suspendedBusinesses,
            long cancelledBusinesses,
            long setupBusinesses,
            long activeSubscriptions,
            long trialSubscriptions,
            long suspendedSubscriptions,
            long ordersToday,
            long ordersThisMonth,
            long ordersTotal,
            BigDecimal revenueThisMonth,
            BigDecimal revenueTotal,
            List<RecentActivityItem> recentActivity
    ) {}

    public record RecentActivityItem(
            String id,
            String actorEmail,
            String action,
            String targetType,
            String targetId,
            String targetName,
            String details,
            String createdAt
    ) {
        static RecentActivityItem from(AdminAuditLog log) {
            return new RecentActivityItem(
                    log.id().toString(),
                    log.actorEmail(),
                    log.action().name(),
                    log.targetType(),
                    log.targetId() != null ? log.targetId().toString() : null,
                    log.targetName(),
                    log.details(),
                    log.createdAt().toString());
        }
    }
}
