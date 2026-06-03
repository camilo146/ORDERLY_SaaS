package com.orderly.api.plan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
    Optional<SubscriptionJpaEntity> findByBusinessId(UUID businessId);
    Optional<SubscriptionJpaEntity> findByExternalSubscriptionId(String externalSubscriptionId);
}
