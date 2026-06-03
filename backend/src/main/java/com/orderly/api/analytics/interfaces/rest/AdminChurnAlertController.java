package com.orderly.api.analytics.interfaces.rest;

import com.orderly.api.analytics.infrastructure.persistence.ChurnAlertJpaEntity;
import com.orderly.api.analytics.infrastructure.persistence.ChurnAlertJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for churn alert management (superadmin / ops only).
 */
@RestController
@RequestMapping("/api/v1/admin/churn-alerts")
public class AdminChurnAlertController {

    private final ChurnAlertJpaRepository alertRepo;

    public AdminChurnAlertController(ChurnAlertJpaRepository alertRepo) {
        this.alertRepo = alertRepo;
    }

    /**
     * GET /api/v1/admin/churn-alerts
     * Returns all unresolved churn alerts.
     */
    @GetMapping
    // [SECURITY FIX VUL-05] Corregido role mismatch: 'SUPERADMIN' y 'OPS' no
    // existían en el sistema.
    // Los roles reales son SUPER_ADMIN y OPERATOR. El bug bloqueaba el acceso a
    // superadmins legítimos.
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    public ResponseEntity<List<ChurnAlertResponse>> getUnresolvedAlerts() {
        List<ChurnAlertResponse> alerts = alertRepo.findAllByResolvedFalseOrderByCreatedAtDesc()
                .stream().map(ChurnAlertResponse::from).toList();
        return ResponseEntity.ok(alerts);
    }

    /**
     * PATCH /api/v1/admin/churn-alerts/{id}/resolve
     * Marks a churn alert as resolved.
     */
    @PatchMapping("/{id}/resolve")
    // [SECURITY FIX VUL-05] Idem — usar roles reales del sistema
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    public ResponseEntity<Void> resolveAlert(@PathVariable UUID id) {
        var opt = alertRepo.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        ChurnAlertJpaEntity alert = opt.get();
        alert.setResolved(true);
        alert.setResolvedAt(OffsetDateTime.now());
        alertRepo.save(alert);
        return ResponseEntity.noContent().build();
    }

    // ── DTO ────────────────────────────────────────────────────────────────────

    public record ChurnAlertResponse(
            UUID id,
            UUID businessId,
            String severity,
            String alertType,
            String description,
            boolean resolved,
            OffsetDateTime createdAt) {

        static ChurnAlertResponse from(ChurnAlertJpaEntity e) {
            return new ChurnAlertResponse(e.getId(), e.getBusinessId(), e.getSeverity(),
                    e.getAlertType(), e.getDescription(), e.isResolved(), e.getCreatedAt());
        }
    }
}
