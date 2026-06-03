package com.orderly.api.messaging.interfaces.rest;

import com.orderly.api.shared.service.BusinessHoursService;
import com.orderly.api.shared.tenant.TenantAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/settings/hours")
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;
    private final TenantAccessService tenantAccessService;

    public BusinessHoursController(BusinessHoursService businessHoursService, TenantAccessService tenantAccessService) {
        this.businessHoursService = businessHoursService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public List<BusinessHoursService.DaySchedule> getHours(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        return businessHoursService.getHours(businessId);
    }

    @PutMapping
    public List<BusinessHoursService.DaySchedule> saveHours(
            @PathVariable UUID businessId,
            @RequestBody List<BusinessHoursService.DaySchedule> schedule) {
        tenantAccessService.validateTenantAccess(businessId);
        return businessHoursService.saveHours(businessId, schedule);
    }
}
