package com.orderly.api.dashboard.interfaces.rest;

import com.orderly.api.business.interfaces.rest.BusinessResponse;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.dashboard.application.DashboardQueryService;
import com.orderly.api.dashboard.application.SystemOverviewResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints for the super admin panel.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPanelController {

    private final DashboardQueryService dashboardQueryService;
    private final BusinessRepository businessRepository;

    public AdminPanelController(DashboardQueryService dashboardQueryService, BusinessRepository businessRepository) {
        this.dashboardQueryService = dashboardQueryService;
        this.businessRepository = businessRepository;
    }

    @GetMapping("/overview")
    public SystemOverviewResponse overview() {
        return dashboardQueryService.getSystemOverview();
    }

    @GetMapping("/businesses")
    public List<BusinessResponse> businesses() {
        return businessRepository.findAll().stream()
                .map(BusinessResponse::from)
                .toList();
    }
}
