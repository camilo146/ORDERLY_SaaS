package com.orderly.api.complaint.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Complaint submitted by a customer via WhatsApp chatbot.
 */
public final class Complaint {

    private final UUID id;
    private final UUID businessId;
    private final String customerPhone;
    private final String description;
    private final String evidenceUrl;
    private final ComplaintStatus status;
    private final OffsetDateTime createdAt;

    private Complaint(UUID id, UUID businessId, String customerPhone, String description,
            String evidenceUrl, ComplaintStatus status, OffsetDateTime createdAt) {
        this.id = Objects.requireNonNull(id);
        this.businessId = Objects.requireNonNull(businessId);
        this.customerPhone = Objects.requireNonNull(customerPhone);
        this.description = Objects.requireNonNull(description);
        this.evidenceUrl = evidenceUrl;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static Complaint create(UUID businessId, String customerPhone, String description, String evidenceUrl) {
        return new Complaint(UUID.randomUUID(), businessId, customerPhone, description,
                evidenceUrl, ComplaintStatus.PENDING, OffsetDateTime.now());
    }

    public static Complaint restore(UUID id, UUID businessId, String customerPhone, String description,
            String evidenceUrl, ComplaintStatus status, OffsetDateTime createdAt) {
        return new Complaint(id, businessId, customerPhone, description, evidenceUrl, status, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID businessId() {
        return businessId;
    }

    public String customerPhone() {
        return customerPhone;
    }

    public String description() {
        return description;
    }

    public String evidenceUrl() {
        return evidenceUrl;
    }

    public ComplaintStatus status() {
        return status;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }
}
