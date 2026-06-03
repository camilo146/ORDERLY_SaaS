package com.orderly.api.dashboard.interfaces.rest;

import com.orderly.api.dashboard.application.BusinessDashboardResponse;
import com.orderly.api.dashboard.application.DashboardQueryService;
import com.orderly.api.shared.tenant.TenantAccessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints for tenant admin dashboards.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class BusinessDashboardController {

    private final DashboardQueryService dashboardQueryService;
    private final TenantAccessService tenantAccessService;

    public BusinessDashboardController(DashboardQueryService dashboardQueryService,
            TenantAccessService tenantAccessService) {
        this.dashboardQueryService = dashboardQueryService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public BusinessDashboardResponse overview(@PathVariable UUID businessId) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        return dashboardQueryService.getBusinessOverview(tenantId);
    }
}
