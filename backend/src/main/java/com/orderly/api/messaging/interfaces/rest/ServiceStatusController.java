package com.orderly.api.messaging.interfaces.rest;

import com.orderly.api.shared.service.ServiceStatusService;
import com.orderly.api.shared.tenant.TenantAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/settings/service-status")
public class ServiceStatusController {

    private final ServiceStatusService serviceStatusService;
    private final TenantAccessService tenantAccessService;

    public ServiceStatusController(ServiceStatusService serviceStatusService, TenantAccessService tenantAccessService) {
        this.serviceStatusService = serviceStatusService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public Map<String, Object> getStatus(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        return Map.of("enabled", serviceStatusService.isEnabled(businessId));
    }

    @PostMapping("/toggle")
    public Map<String, Object> toggle(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        boolean newState = serviceStatusService.toggle(businessId);
        return Map.of("enabled", newState);
    }
}
