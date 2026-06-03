package com.orderly.api.complaint.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "complaints")
public class ComplaintJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ComplaintJpaEntity() {
    }

    public ComplaintJpaEntity(UUID id, UUID businessId, String customerPhone, String description,
            String evidenceUrl, String status, OffsetDateTime createdAt) {
        this.id = id;
        this.businessId = businessId;
        this.customerPhone = customerPhone;
        this.description = description;
        this.evidenceUrl = evidenceUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getDescription() {
        return description;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
