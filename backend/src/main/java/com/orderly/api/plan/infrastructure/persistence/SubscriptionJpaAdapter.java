package com.orderly.api.plan.infrastructure.persistence;

import com.orderly.api.plan.domain.model.Subscription;
import com.orderly.api.plan.domain.model.SubscriptionStatus;
import com.orderly.api.plan.domain.port.SubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionJpaAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository jpaRepo;

    public SubscriptionJpaAdapter(SubscriptionJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = jpaRepo.findById(subscription.id())
                .orElseGet(SubscriptionJpaEntity::new);
        mapToEntity(subscription, entity);
        return mapToDomain(jpaRepo.save(entity));
    }

    @Override
    public Optional<Subscription> findByBusinessId(UUID businessId) {
        return jpaRepo.findByBusinessId(businessId).map(this::mapToDomain);
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return jpaRepo.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId) {
        return jpaRepo.findByExternalSubscriptionId(externalSubscriptionId).map(this::mapToDomain);
    }

    private void mapToEntity(Subscription s, SubscriptionJpaEntity e) {
        e.setId(s.id());
        e.setBusinessId(s.businessId());
        e.setPlanId(s.planId());
        e.setStatus(s.status().name());
        e.setProvider(s.provider() != null ? s.provider() : "manual");
        e.setExternalSubscriptionId(s.externalSubscriptionId());
        e.setTrialEndsAt(s.trialEndsAt());
        e.setCurrentPeriodStart(s.currentPeriodStart());
        e.setCurrentPeriodEnd(s.currentPeriodEnd());
        e.setCancelAtPeriodEnd(s.cancelAtPeriodEnd());
        e.setCancelledAt(s.cancelledAt());
        if (e.getCreatedAt() == null) e.setCreatedAt(s.createdAt() != null ? s.createdAt() : OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
    }

    private Subscription mapToDomain(SubscriptionJpaEntity e) {
        return Subscription.reconstitute(
                e.getId(), e.getBusinessId(), e.getPlanId(),
                parseStatus(e.getStatus()), e.getProvider(),
                e.getExternalSubscriptionId(), e.getTrialEndsAt(),
                e.getCurrentPeriodStart(), e.getCurrentPeriodEnd(),
                e.isCancelAtPeriodEnd(), e.getCancelledAt(), e.getCreatedAt());
    }

    private SubscriptionStatus parseStatus(String status) {
        if (status == null) return SubscriptionStatus.TRIAL;
        return switch (status.toUpperCase()) {
            case "ACTIVE"            -> SubscriptionStatus.ACTIVE;
            case "PAST_DUE"          -> SubscriptionStatus.PAST_DUE;
            case "SUSPENDED"         -> SubscriptionStatus.SUSPENDED;
            case "CANCELED", "CANCELLED", "EXPIRED" -> SubscriptionStatus.CANCELED;
            default                  -> SubscriptionStatus.TRIAL;
        };
    }
}
