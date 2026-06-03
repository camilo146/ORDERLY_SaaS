package com.orderly.api.plan.infrastructure.persistence;

import com.orderly.api.plan.domain.model.SubscriptionUsage;
import com.orderly.api.plan.domain.port.SubscriptionUsageRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionUsageJpaAdapter implements SubscriptionUsageRepository {

    private final SubscriptionUsageJpaRepository jpaRepo;

    public SubscriptionUsageJpaAdapter(SubscriptionUsageJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public SubscriptionUsage save(SubscriptionUsage usage) {
        SubscriptionUsageJpaEntity entity = jpaRepo.findById(usage.id())
                .orElseGet(SubscriptionUsageJpaEntity::new);
        mapToEntity(usage, entity);
        return mapToDomain(jpaRepo.save(entity));
    }

    @Override
    public Optional<SubscriptionUsage> findCurrentPeriod(UUID businessId, OffsetDateTime now) {
        return jpaRepo.findCurrentPeriod(businessId, now).map(this::mapToDomain);
    }

    @Override
    public List<SubscriptionUsage> findAllByBusinessId(UUID businessId) {
        return jpaRepo.findAllByBusinessIdOrderByPeriodStartDesc(businessId)
                .stream().map(this::mapToDomain).toList();
    }

    @Override
    public List<SubscriptionUsage> findOpenPeriods() {
        return jpaRepo.findOpenPeriods().stream().map(this::mapToDomain).toList();
    }

    private void mapToEntity(SubscriptionUsage u, SubscriptionUsageJpaEntity e) {
        e.setId(u.id());
        e.setBusinessId(u.businessId());
        e.setSubscriptionId(u.subscriptionId());
        e.setPeriodStart(u.periodStart());
        e.setPeriodEnd(u.periodEnd());
        e.setPlanOrderLimit(u.planOrderLimit());
        e.setOrdersCount(u.ordersCount());
        e.setOverageCount(u.overageCount());
        e.setOverageBlockSize(u.overageBlockSize());
        e.setOverageBlocks(u.overageBlocks());
        e.setOverageRateCop(u.overageRateCop());
        e.setOverageRateUsd(u.overageRateUsd());
        e.setOverageChargeCop(u.overageChargeCop());
        e.setOverageChargeUsd(u.overageChargeUsd());
        e.setFinalized(u.isFinalized());
        if (e.getCreatedAt() == null) e.setCreatedAt(u.createdAt() != null ? u.createdAt() : OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
    }

    private SubscriptionUsage mapToDomain(SubscriptionUsageJpaEntity e) {
        return SubscriptionUsage.reconstitute(
                e.getId(), e.getBusinessId(), e.getSubscriptionId(),
                e.getPeriodStart(), e.getPeriodEnd(),
                e.getPlanOrderLimit(), e.getOrdersCount(), e.getOverageCount(),
                e.getOverageBlockSize(), e.getOverageBlocks(),
                e.getOverageRateCop(), e.getOverageRateUsd(),
                e.getOverageChargeCop(), e.getOverageChargeUsd(),
                e.isFinalized(), e.getCreatedAt());
    }
}
