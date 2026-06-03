package com.orderly.api.complaint.interfaces.rest;

import com.orderly.api.complaint.domain.model.Complaint;
import com.orderly.api.complaint.domain.port.ComplaintRepository;
import com.orderly.api.shared.tenant.TenantAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoint for listing complaints received via WhatsApp chatbot.
 * GET /api/v1/businesses/{businessId}/complaints
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final TenantAccessService tenantAccessService;

    public ComplaintController(ComplaintRepository complaintRepository,
            TenantAccessService tenantAccessService) {
        this.complaintRepository = complaintRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> list(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        List<ComplaintResponse> result = complaintRepository.findAllByBusinessId(businessId)
                .stream()
                .map(ComplaintResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    public record ComplaintResponse(
            UUID id,
            String customerPhone,
            String description,
            String evidenceUrl,
            String status,
            String createdAt) {

        public static ComplaintResponse from(Complaint c) {
            return new ComplaintResponse(
                    c.id(),
                    c.customerPhone(),
                    c.description(),
                    c.evidenceUrl(),
                    c.status().name(),
                    c.createdAt().toString());
        }
    }
}
