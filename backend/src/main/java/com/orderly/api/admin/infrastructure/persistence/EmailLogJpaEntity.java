package com.orderly.api.admin.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_logs", indexes = {
        @Index(name = "idx_email_recipient", columnList = "recipient_email"),
        @Index(name = "idx_email_created", columnList = "created_at")
})
public class EmailLogJpaEntity {

    @Id
    private UUID id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(nullable = false)
    private String subject;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_by_id")
    private UUID sentById;

    @Column(name = "sent_by_email")
    private String sentByEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EmailLogJpaEntity() {}

    public EmailLogJpaEntity(UUID id, String recipientEmail, String recipientName,
                              String subject, String bodyText, String status,
                              OffsetDateTime sentAt, String errorMessage,
                              UUID sentById, String sentByEmail, OffsetDateTime createdAt) {
        this.id = id;
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.subject = subject;
        this.bodyText = bodyText;
        this.status = status;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
        this.sentById = sentById;
        this.sentByEmail = sentByEmail;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientName() { return recipientName; }
    public String getSubject() { return subject; }
    public String getBodyText() { return bodyText; }
    public String getStatus() { return status; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public String getErrorMessage() { return errorMessage; }
    public UUID getSentById() { return sentById; }
    public String getSentByEmail() { return sentByEmail; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
