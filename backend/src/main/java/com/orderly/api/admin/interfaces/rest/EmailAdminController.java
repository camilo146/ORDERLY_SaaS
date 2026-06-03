package com.orderly.api.admin.interfaces.rest;

import com.orderly.api.admin.application.EmailAdminService;
import com.orderly.api.admin.infrastructure.persistence.EmailLogJpaEntity;
import com.orderly.api.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ceo/emails")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class EmailAdminController {

    private final EmailAdminService emailService;

    public EmailAdminController(EmailAdminService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public EmailLogResponse send(@Valid @RequestBody SendEmailRequest request,
                                  @AuthenticationPrincipal UserPrincipal actor,
                                  HttpServletRequest req) {
        EmailLogJpaEntity entity = emailService.send(
                request.recipientEmail(), request.recipientName(),
                request.subject(), request.bodyText(), actor, req);
        return EmailLogResponse.from(entity);
    }

    @GetMapping("/logs")
    public List<EmailLogResponse> logs() {
        return emailService.findRecentLogs().stream()
                .map(EmailLogResponse::from)
                .toList();
    }

    // ── Request / Response records ────────────────────────────────────────────

    public record SendEmailRequest(
            @Email @NotBlank String recipientEmail,
            String recipientName,
            @NotBlank String subject,
            @NotBlank String bodyText
    ) {}

    public record EmailLogResponse(
            UUID id,
            String recipientEmail,
            String recipientName,
            String subject,
            String status,
            OffsetDateTime sentAt,
            String sentByEmail,
            OffsetDateTime createdAt
    ) {
        public static EmailLogResponse from(EmailLogJpaEntity e) {
            return new EmailLogResponse(e.getId(), e.getRecipientEmail(), e.getRecipientName(),
                    e.getSubject(), e.getStatus(), e.getSentAt(), e.getSentByEmail(), e.getCreatedAt());
        }
    }
}
