package com.orderly.api.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    @Query("SELECT n FROM NotificationJpaEntity n " +
            "WHERE n.status = 'PENDING' AND n.scheduledAt <= :now " +
            "ORDER BY n.scheduledAt ASC")
    List<NotificationJpaEntity> findDueNotifications(@Param("now") OffsetDateTime now);

    long countByBusinessIdAndStatus(UUID businessId, String status);
}
