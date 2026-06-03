package com.orderly.api.analytics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChurnAlertJpaRepository extends JpaRepository<ChurnAlertJpaEntity, UUID> {

    List<ChurnAlertJpaEntity> findAllByResolvedFalseOrderByCreatedAtDesc();

    List<ChurnAlertJpaEntity> findAllByBusinessIdAndResolvedFalse(UUID businessId);

    @Query("SELECT c FROM ChurnAlertJpaEntity c WHERE c.resolved = false " +
            "AND c.severity = :severity ORDER BY c.createdAt DESC")
    List<ChurnAlertJpaEntity> findUnresolvedBySeverity(@Param("severity") String severity);

    boolean existsByBusinessIdAndAlertTypeAndResolvedFalse(UUID businessId, String alertType);
}
