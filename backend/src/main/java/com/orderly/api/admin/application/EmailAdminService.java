package com.orderly.api.admin.application;

import com.orderly.api.admin.domain.model.AdminAction;
import com.orderly.api.admin.infrastructure.persistence.EmailLogJpaEntity;
import com.orderly.api.admin.infrastructure.persistence.EmailLogJpaRepository;
import com.orderly.api.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles email sending through the CEO panel.
 * In development, emails are logged to the console.
 * In production, wire a real SMTP or transactional email provider here.
 */
@Service
public class EmailAdminService {

    private static final Logger log = LoggerFactory.getLogger(EmailAdminService.class);

    private final EmailLogJpaRepository emailLogRepo;
    private final AuditLogService auditLogService;

    public EmailAdminService(EmailLogJpaRepository emailLogRepo, AuditLogService auditLogService) {
        this.emailLogRepo = emailLogRepo;
        this.auditLogService = auditLogService;
    }

    public EmailLogJpaEntity send(String recipientEmail, String recipientName,
                                   String subject, String bodyText,
                                   UserPrincipal actor, HttpServletRequest req) {
        log.info("[CEO EMAIL] To: {} | Subject: {} | Sent by: {}", recipientEmail, subject, actor.getUsername());

        String status = "SENT";
        String errorMessage = null;
        OffsetDateTime sentAt = OffsetDateTime.now();

        // TODO: replace with real SMTP/SES/Resend integration
        try {
            deliverEmail(recipientEmail, subject, bodyText);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            sentAt = null;
            log.error("[CEO EMAIL] Failed to deliver email to {}: {}", recipientEmail, e.getMessage());
        }

        EmailLogJpaEntity entity = new EmailLogJpaEntity(
                UUID.randomUUID(), recipientEmail, recipientName,
                subject, bodyText, status, sentAt, errorMessage,
                actor.userId(), actor.getUsername(), OffsetDateTime.now());
        EmailLogJpaEntity saved = emailLogRepo.save(entity);

        auditLogService.record(actor, AdminAction.EMAIL_SENT, "EMAIL",
                null, recipientEmail, "Subject: " + subject, ipFrom(req));

        return saved;
    }

    public List<EmailLogJpaEntity> findRecentLogs() {
        return emailLogRepo.findTop50ByOrderByCreatedAtDesc();
    }

    private void deliverEmail(String to, String subject, String body) {
        // Stub: log only. Replace with JavaMailSender, Resend, SES, etc.
        log.info("╔══════════════════════════════════════════════");
        log.info("║ [EMAIL STUB] To: {}", to);
        log.info("║ Subject: {}", subject);
        log.info("║ Body: {}", body.length() > 200 ? body.substring(0, 200) + "..." : body);
        log.info("╚══════════════════════════════════════════════");
    }

    private String ipFrom(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
