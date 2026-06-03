package com.orderly.api.plan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionUsageJpaRepository extends JpaRepository<SubscriptionUsageJpaEntity, UUID> {

    @Query("SELECT u FROM SubscriptionUsageJpaEntity u " +
           "WHERE u.businessId = :businessId " +
           "AND u.periodStart <= :now AND u.periodEnd > :now " +
           "AND u.isFinalized = false")
    Optional<SubscriptionUsageJpaEntity> findCurrentPeriod(
            @Param("businessId") UUID businessId,
            @Param("now") OffsetDateTime now);

    List<SubscriptionUsageJpaEntity> findAllByBusinessIdOrderByPeriodStartDesc(UUID businessId);

    @Query("SELECT u FROM SubscriptionUsageJpaEntity u WHERE u.isFinalized = false")
    List<SubscriptionUsageJpaEntity> findOpenPeriods();
}
