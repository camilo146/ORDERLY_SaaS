package com.orderly.api.admin.application;

import com.orderly.api.admin.domain.model.AdminAction;
import com.orderly.api.admin.interfaces.rest.BusinessAdminDetailResponse;
import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.model.BusinessStatus;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.order.infrastructure.persistence.OrderJpaRepository;
import com.orderly.api.plan.application.SubscriptionService;
import com.orderly.api.plan.application.UsageTrackingService;
import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.domain.model.SubscriptionStatus;
import com.orderly.api.plan.domain.port.SubscriptionRepository;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import com.orderly.api.shared.domain.DomainException;
import com.orderly.api.shared.security.InMemoryUserAccountService;
import com.orderly.api.shared.security.JwtService;
import com.orderly.api.shared.security.UserPrincipal;
import com.orderly.api.shared.security.persistence.UserJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessAdminService {

    private static final Logger log = LoggerFactory.getLogger(BusinessAdminService.class);

    private final BusinessRepository businessRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;
    private final PlanJpaRepository planRepo;
    private final OrderJpaRepository orderJpaRepo;
    private final UserJpaRepository userJpaRepo;
    private final InMemoryUserAccountService userAccountService;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public BusinessAdminService(BusinessRepository businessRepo,
                                 SubscriptionRepository subscriptionRepo,
                                 SubscriptionService subscriptionService,
                                 UsageTrackingService usageTrackingService,
                                 PlanJpaRepository planRepo,
                                 OrderJpaRepository orderJpaRepo,
                                 UserJpaRepository userJpaRepo,
                                 InMemoryUserAccountService userAccountService,
                                 JwtService jwtService,
                                 AuditLogService auditLogService) {
        this.businessRepo = businessRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.subscriptionService = subscriptionService;
        this.usageTrackingService = usageTrackingService;
        this.planRepo = planRepo;
        this.orderJpaRepo = orderJpaRepo;
        this.userJpaRepo = userJpaRepo;
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    public List<BusinessAdminDetailResponse> findAll() {
        return businessRepo.findAll().stream()
                .map(this::buildDetail)
                .toList();
    }

    public BusinessAdminDetailResponse findById(UUID businessId) {
        return buildDetail(requireBusiness(businessId));
    }

    @Transactional
    public BusinessAdminDetailResponse suspend(UUID businessId, UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        businessRepo.updateStatus(businessId, BusinessStatus.SUSPENDED);
        subscriptionRepo.findByBusinessId(businessId)
                .filter(s -> s.status() == SubscriptionStatus.ACTIVE || s.status() == SubscriptionStatus.TRIAL)
                .ifPresent(s -> subscriptionRepo.save(s.suspend()));
        auditLogService.record(actor, AdminAction.BUSINESS_SUSPENDED,
                "BUSINESS", businessId, business.name(), null, ipFrom(req));
        log.info("[CEO] Business {} suspended by {}", businessId, actor.getUsername());
        return findById(businessId);
    }

    @Transactional
    public BusinessAdminDetailResponse activate(UUID businessId, UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        businessRepo.updateStatus(businessId, BusinessStatus.ACTIVE);
        subscriptionRepo.findByBusinessId(businessId)
                .filter(s -> s.status() == SubscriptionStatus.SUSPENDED)
                .ifPresent(s -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    subscriptionRepo.save(s.reactivate(now, now.plusMonths(1)));
                });
        auditLogService.record(actor, AdminAction.BUSINESS_ACTIVATED,
                "BUSINESS", businessId, business.name(), null, ipFrom(req));
        log.info("[CEO] Business {} activated by {}", businessId, actor.getUsername());
        return findById(businessId);
    }

    @Transactional
    public PurgeResult purgeAllCancelledBusinesses(UserPrincipal actor, HttpServletRequest req) {
        List<Business> cancelled = businessRepo.findAll().stream()
                .filter(b -> b.status() == BusinessStatus.CANCELLED)
                .toList();

        for (Business business : cancelled) {
            businessRepo.deleteById(business.id());
            log.warn("[CEO] Purged CANCELLED business {} ({}) by {}", business.id(), business.name(), actor.getUsername());
        }

        if (!cancelled.isEmpty()) {
            auditLogService.record(actor, AdminAction.CANCELLED_BUSINESSES_PURGED,
                    "PLATFORM", null, null,
                    "Purged " + cancelled.size() + " cancelled businesses — all data permanently removed", ipFrom(req));
        }

        log.warn("[CEO] Purge complete: {} cancelled businesses removed by {}", cancelled.size(), actor.getUsername());
        return new PurgeResult(cancelled.size());
    }

    @Transactional
    public void delete(UUID businessId, UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        // Record audit BEFORE deletion — audit_log uses VARCHAR target_id (no FK), so it survives
        auditLogService.record(actor, AdminAction.BUSINESS_DELETED,
                "BUSINESS", businessId, business.name(), "Hard delete — all data removed", ipFrom(req));
        // Hard delete: PostgreSQL ON DELETE CASCADE removes all child rows automatically
        // (subscriptions, subscription_usage, orders, order_items, products, customers,
        //  complaints, conversations, notification_queue, message_templates, etc.)
        businessRepo.deleteById(businessId);
        log.warn("[CEO] Business {} permanently deleted by {}", businessId, actor.getUsername());
    }

    @Transactional
    public BusinessAdminDetailResponse changePlan(UUID businessId, String planCode,
                                                   UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        subscriptionService.changePlan(businessId, planCode);
        auditLogService.record(actor, AdminAction.PLAN_CHANGED,
                "BUSINESS", businessId, business.name(), "Plan: " + planCode, ipFrom(req));
        log.info("[CEO] Plan changed for business {} → {} by {}", businessId, planCode, actor.getUsername());
        return findById(businessId);
    }

    @Transactional
    public BusinessAdminDetailResponse extendTrial(UUID businessId, int days,
                                                    UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        Subscription subscription = subscriptionService.getOrThrow(businessId);
        subscriptionRepo.save(subscription.extendTrial(days));
        auditLogService.record(actor, AdminAction.TRIAL_EXTENDED,
                "BUSINESS", businessId, business.name(), days + " días extra de trial", ipFrom(req));
        return findById(businessId);
    }

    @Transactional
    public BusinessAdminDetailResponse grantFreeMonths(UUID businessId, int months,
                                                        UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        Subscription subscription = subscriptionService.getOrThrow(businessId);
        subscriptionRepo.save(subscription.grantFreeMonths(months));
        auditLogService.record(actor, AdminAction.FREE_MONTHS_GRANTED,
                "BUSINESS", businessId, business.name(), months + " meses gratis", ipFrom(req));
        return findById(businessId);
    }

    @Transactional
    public BusinessAdminDetailResponse resetUsage(UUID businessId, UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        // Delegamos al UsageTrackingService para que abra un período limpio
        subscriptionRepo.findByBusinessId(businessId).ifPresent(sub ->
                planRepo.findById(sub.planId()).ifPresent(plan ->
                        usageTrackingService.openPeriodForSubscription(sub, plan)));
        auditLogService.record(actor, AdminAction.USAGE_RESET,
                "BUSINESS", businessId, business.name(), null, ipFrom(req));
        return findById(businessId);
    }

    public ImpersonationResponse impersonate(UUID businessId, UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        UserPrincipal ownerPrincipal = userAccountService.loadById(business.ownerId());
        String token = jwtService.createImpersonationToken(ownerPrincipal, businessId, actor.getUsername());
        auditLogService.record(actor, AdminAction.BUSINESS_IMPERSONATED,
                "BUSINESS", businessId, business.name(),
                "Owner: " + ownerPrincipal.getUsername(), ipFrom(req));
        log.warn("[CEO] Impersonation issued for business {} by {}", businessId, actor.getUsername());
        return new ImpersonationResponse(token, ownerPrincipal.getUsername(),
                ownerPrincipal.fullName(), OffsetDateTime.now().plusMinutes(15));
    }

    @Transactional
    public void forceLogout(UUID businessId, UserPrincipal actor, HttpServletRequest req) {
        Business business = requireBusiness(businessId);
        userAccountService.forceLogout(business.ownerId());
        auditLogService.record(actor, AdminAction.FORCE_LOGOUT,
                "BUSINESS", businessId, business.name(), null, ipFrom(req));
        log.warn("[CEO] Force logout for owner of {} by {}", businessId, actor.getUsername());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Business requireBusiness(UUID businessId) {
        return businessRepo.findById(businessId)
                .orElseThrow(() -> new DomainException("Business not found: " + businessId));
    }

    private BusinessAdminDetailResponse buildDetail(Business business) {
        var owner = userJpaRepo.findById(business.ownerId()).orElse(null);
        String ownerEmail = owner != null ? owner.getEmail() : "—";
        String ownerName  = owner != null ? owner.getFullName() : "—";

        var subOpt = subscriptionRepo.findByBusinessId(business.id());
        var planOpt = subOpt.flatMap(s -> planRepo.findById(s.planId()));

        BusinessAdminDetailResponse.PlanInfo planInfo = planOpt.map(p ->
                new BusinessAdminDetailResponse.PlanInfo(
                        p.getId(), p.getCode(), p.getName(),
                        p.getMonthlyPrice(), p.getMonthlyPriceUsd()))
                .orElse(null);

        BusinessAdminDetailResponse.SubscriptionInfo subInfo = subOpt.map(s ->
                new BusinessAdminDetailResponse.SubscriptionInfo(
                        s.status().name(), s.trialEndsAt(), s.currentPeriodEnd(), s.provider()))
                .orElse(null);

        var usageOpt = usageTrackingService.getCurrentPeriodUsage(business.id());
        BusinessAdminDetailResponse.UsageInfo usageInfo = usageOpt.map(u ->
                new BusinessAdminDetailResponse.UsageInfo(
                        u.ordersCount(), u.planOrderLimit(), u.overageCount()))
                .orElse(new BusinessAdminDetailResponse.UsageInfo(0, 0, 0));

        long totalOrders = orderJpaRepo.countByBusinessId(business.id());
        BigDecimal totalRevenue = orderJpaRepo.sumRevenueByBusiness(business.id());

        return new BusinessAdminDetailResponse(
                business.id(), business.name(), business.slug(),
                business.businessType().name(), business.status().name(),
                business.countryCode(), business.currencyCode(), business.timezone(),
                business.createdAt(), business.ownerId(), ownerEmail, ownerName,
                planInfo, subInfo, usageInfo,
                (int) totalOrders,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
    }

    private String ipFrom(HttpServletRequest req) {
        // X-Real-IP is set by nginx to $remote_addr (the TCP-level source IP), which cannot
        // be spoofed by the client regardless of what they put in X-Forwarded-For.
        // Using X-Forwarded-For here would let any caller inject an arbitrary IP into the
        // audit log, making the log useless as a forensic artifact.
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.strip();
        }
        return req.getRemoteAddr();
    }

    public record ImpersonationResponse(
            String token,
            String ownerEmail,
            String ownerName,
            OffsetDateTime expiresAt
    ) {}

    public record PurgeResult(int deletedCount) {}
}
