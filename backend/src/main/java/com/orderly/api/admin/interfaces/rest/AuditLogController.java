package com.orderly.api.admin.interfaces.rest;

import com.orderly.api.admin.application.AuditLogService;
import com.orderly.api.admin.domain.model.AdminAuditLog;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ceo/audit-logs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Validated
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLogResponse> findRecent(
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return auditLogService.findRecent(limit)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    public record AuditLogResponse(
            UUID id,
            UUID actorId,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            String targetName,
            String details,
            OffsetDateTime createdAt
    ) {
        public static AuditLogResponse from(AdminAuditLog log) {
            return new AuditLogResponse(log.id(), log.actorId(), log.actorEmail(),
                    log.action().name(), log.targetType(), log.targetId(), log.targetName(),
                    log.details(), log.createdAt());
        }
    }
}
