package com.orderly.api.admin.interfaces.rest;

import com.orderly.api.admin.application.AdminDashboardService;
import com.orderly.api.admin.application.AdminDashboardService.GlobalDashboardMetrics;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaEntity;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ceo")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CeoDashboardController {

    private final AdminDashboardService dashboardService;
    private final PlanJpaRepository planRepo;

    public CeoDashboardController(AdminDashboardService dashboardService,
                                   PlanJpaRepository planRepo) {
        this.dashboardService = dashboardService;
        this.planRepo = planRepo;
    }

    @GetMapping("/dashboard")
    public GlobalDashboardMetrics dashboard() {
        return dashboardService.getGlobalMetrics();
    }

    @GetMapping("/plans")
    public List<PlanJpaEntity> plans() {
        return planRepo.findAll();
    }
}
