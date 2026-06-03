package com.orderly.api.plan.interfaces.rest;

import com.orderly.api.plan.application.SubscriptionService;
import com.orderly.api.plan.application.UsageTrackingService;
import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.domain.model.SubscriptionUsage;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaEntity;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import com.orderly.api.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;
    private final PlanJpaRepository planRepo;
    private final TenantAccessService tenantAccessService;

    public SubscriptionController(SubscriptionService subscriptionService,
                                   UsageTrackingService usageTrackingService,
                                   PlanJpaRepository planRepo,
                                   TenantAccessService tenantAccessService) {
        this.subscriptionService = subscriptionService;
        this.usageTrackingService = usageTrackingService;
        this.planRepo = planRepo;
        this.tenantAccessService = tenantAccessService;
    }

    // ── Plans catalogue ────────────────────────────────────────────────────

    @GetMapping("/plans")
    public List<PlanResponse> listPlans() {
        return planRepo.findAll().stream().map(PlanResponse::from).toList();
    }

    // ── Subscription CRUD ─────────────────────────────────────────────────

    @GetMapping("/businesses/{businessId}/subscription")
    public ResponseEntity<SubscriptionStatusResponse> getSubscription(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        return subscriptionService.findByBusinessId(businessId)
                .map(subscription -> {
                    SubscriptionUsage usage = usageTrackingService.getCurrentPeriodUsage(businessId).orElse(null);
                    PlanJpaEntity plan = planRepo.findById(subscription.planId()).orElse(null);
                    return ResponseEntity.ok(SubscriptionStatusResponse.from(subscription, plan, usage));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/businesses/{businessId}/subscription")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionStatusResponse createTrial(@PathVariable UUID businessId,
                                                   @Valid @RequestBody PlanAssignRequest request) {
        tenantAccessService.validateTenantAccess(businessId);
        Subscription subscription = subscriptionService.createTrial(businessId, request.planCode());
        PlanJpaEntity plan = planRepo.findById(subscription.planId()).orElse(null);
        SubscriptionUsage usage = usageTrackingService.getCurrentPeriodUsage(businessId).orElse(null);
        return SubscriptionStatusResponse.from(subscription, plan, usage);
    }

    @PostMapping("/businesses/{businessId}/subscription/activate")
    public SubscriptionStatusResponse activate(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        Subscription subscription = subscriptionService.activate(businessId);
        PlanJpaEntity plan = planRepo.findById(subscription.planId()).orElse(null);
        SubscriptionUsage usage = usageTrackingService.getCurrentPeriodUsage(businessId).orElse(null);
        return SubscriptionStatusResponse.from(subscription, plan, usage);
    }

    @PostMapping("/businesses/{businessId}/subscription/cancel")
    public SubscriptionStatusResponse cancel(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        Subscription subscription = subscriptionService.cancel(businessId);
        PlanJpaEntity plan = planRepo.findById(subscription.planId()).orElse(null);
        return SubscriptionStatusResponse.from(subscription, plan, null);
    }

    @PatchMapping("/businesses/{businessId}/subscription/plan")
    public SubscriptionStatusResponse changePlan(@PathVariable UUID businessId,
                                                  @Valid @RequestBody PlanAssignRequest request) {
        tenantAccessService.validateTenantAccess(businessId);
        Subscription subscription = subscriptionService.changePlan(businessId, request.planCode());
        PlanJpaEntity plan = planRepo.findById(subscription.planId()).orElse(null);
        SubscriptionUsage usage = usageTrackingService.getCurrentPeriodUsage(businessId).orElse(null);
        return SubscriptionStatusResponse.from(subscription, plan, usage);
    }

    @GetMapping("/businesses/{businessId}/subscription/usage")
    public UsageHistoryResponse getUsageHistory(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        return new UsageHistoryResponse(businessId,
                usageTrackingService.getCurrentPeriodUsage(businessId)
                        .map(UsagePeriodResponse::from).orElse(null));
    }

    // ── Response records ──────────────────────────────────────────────────

    public record PlanAssignRequest(@NotBlank String planCode) {}

    public record PlanResponse(
            UUID id, String code, String name,
            BigDecimal monthlyPriceCop, BigDecimal monthlyPriceUsd,
            BigDecimal annualPriceCop, BigDecimal annualPriceUsd,
            int monthlyOrderLimit, int productLimit, int userLimit,
            int trialDays, boolean analyticsEnabled,
            boolean multiLocation, boolean customBranding, boolean prioritySupport,
            BigDecimal overageRateCop, BigDecimal overageRateUsd, int overageBlockSize) {

        static PlanResponse from(PlanJpaEntity p) {
            return new PlanResponse(
                    p.getId(), p.getCode(), p.getName(),
                    p.getMonthlyPrice(), p.getMonthlyPriceUsd(),
                    p.getAnnualPrice(), p.getAnnualPriceUsd(),
                    orZero(p.getMaxOrdersPerMonth()),
                    orZero(p.getMaxProducts()),
                    orZero(p.getMaxDashboardUsers()),
                    orZeroInt(p.getTrialDays()),
                    Boolean.TRUE.equals(p.getAnalyticsEnabled()),
                    Boolean.TRUE.equals(p.getMultiLocation()),
                    Boolean.TRUE.equals(p.getCustomBranding()),
                    Boolean.TRUE.equals(p.getPrioritySupport()),
                    p.getOverageRateCop() != null ? p.getOverageRateCop() : BigDecimal.ZERO,
                    p.getOverageRateUsd() != null ? p.getOverageRateUsd() : BigDecimal.ZERO,
                    p.getOverageBlockSize() != null ? p.getOverageBlockSize() : 500);
        }

        private static int orZero(Integer v) { return v != null ? v : 0; }
        private static int orZeroInt(Integer v) { return v != null ? v : 14; }
    }

    public record SubscriptionStatusResponse(
            UUID subscriptionId, UUID businessId,
            String planCode, String planName, String status,
            OffsetDateTime trialEndsAt,
            OffsetDateTime currentPeriodStart, OffsetDateTime currentPeriodEnd,
            boolean cancelAtPeriodEnd, OffsetDateTime cancelledAt,
            UsagePeriodResponse currentUsage) {

        static SubscriptionStatusResponse from(Subscription s, PlanJpaEntity plan, SubscriptionUsage usage) {
            return new SubscriptionStatusResponse(
                    s.id(), s.businessId(),
                    plan != null ? plan.getCode() : null,
                    plan != null ? plan.getName() : null,
                    s.status().name(),
                    s.trialEndsAt(), s.currentPeriodStart(), s.currentPeriodEnd(),
                    s.cancelAtPeriodEnd(), s.cancelledAt(),
                    usage != null ? UsagePeriodResponse.from(usage) : null);
        }
    }

    public record UsagePeriodResponse(
            OffsetDateTime periodStart, OffsetDateTime periodEnd,
            int planOrderLimit, int ordersCount, int ordersRemaining,
            int overageCount, int overageBlocks,
            BigDecimal overageChargeCop, BigDecimal overageChargeUsd,
            boolean isFinalized) {

        static UsagePeriodResponse from(SubscriptionUsage u) {
            int remaining = Math.max(0, u.planOrderLimit() - u.ordersCount());
            return new UsagePeriodResponse(
                    u.periodStart(), u.periodEnd(),
                    u.planOrderLimit(), u.ordersCount(), remaining,
                    u.overageCount(), u.overageBlocks(),
                    u.overageChargeCop(), u.overageChargeUsd(),
                    u.isFinalized());
        }
    }

    public record UsageHistoryResponse(UUID businessId, UsagePeriodResponse currentPeriod) {}
}
