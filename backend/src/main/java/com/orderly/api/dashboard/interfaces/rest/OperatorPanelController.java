package com.orderly.api.dashboard.interfaces.rest;

import com.orderly.api.dashboard.application.DashboardQueryService;
import com.orderly.api.dashboard.application.OperatorOverviewResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for operations and support dashboards.
 */
@RestController
@RequestMapping("/api/v1/operator")
@PreAuthorize("hasAnyRole('OPERATOR','ADMIN','SUPER_ADMIN')")
public class OperatorPanelController {

    private final DashboardQueryService dashboardQueryService;

    public OperatorPanelController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/overview")
    public OperatorOverviewResponse overview() {
        return dashboardQueryService.getOperatorOverview();
    }
}
