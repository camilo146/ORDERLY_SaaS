package com.orderly.api.analytics.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for churn_alerts table (populated by ChurnAlertScheduler).
 */
@Entity
@Table(name = "churn_alerts", indexes = @Index(name = "idx_churn_business", columnList = "business_id,created_at"))
public class ChurnAlertJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 20)
    private String severity; // RED | YELLOW | GREEN

    @Column(name = "alert_type", nullable = false)
    private String alertType; // e.g. NO_ORDERS_7D, NO_ORDERS_30D, etc.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ChurnAlertJpaEntity() {
    }

    public ChurnAlertJpaEntity(UUID id, UUID businessId, String severity, String alertType, String description) {
        this.id = id;
        this.businessId = businessId;
        this.severity = severity;
        this.alertType = alertType;
        this.description = description;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
