package com.orderly.api.plan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {

    Optional<PlanJpaEntity> findByNameIgnoreCase(String name);

    Optional<PlanJpaEntity> findByCodeIgnoreCase(String code);

    @Query(value = "SELECT p.* FROM plans p " +
            "INNER JOIN businesses b ON b.plan_id = p.id " +
            "WHERE b.id = :businessId", nativeQuery = true)
    Optional<PlanJpaEntity> findByBusinessId(@Param("businessId") UUID businessId);
}
