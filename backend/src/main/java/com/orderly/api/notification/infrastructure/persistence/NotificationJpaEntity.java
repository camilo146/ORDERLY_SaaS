package com.orderly.api.notification.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for notification_queue table.
 */
@Entity
@Table(name = "notification_queue", indexes = @Index(name = "idx_notif_status_scheduled", columnList = "status,scheduled_at"))
public class NotificationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(nullable = false)
    private String status; // PENDING | SENT | FAILED

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected NotificationJpaEntity() {
    }

    public NotificationJpaEntity(UUID id, UUID businessId, String recipientPhone,
            String messageType, String messageBody, OffsetDateTime scheduledAt) {
        this.id = id;
        this.businessId = businessId;
        this.recipientPhone = recipientPhone;
        this.messageType = messageType;
        this.messageBody = messageBody;
        this.status = "PENDING";
        this.scheduledAt = scheduledAt;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
