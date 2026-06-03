package com.orderly.api.admin.interfaces.rest;

import com.orderly.api.admin.application.BusinessAdminService;
import com.orderly.api.admin.application.BusinessAdminService.ImpersonationResponse;
import com.orderly.api.admin.application.BusinessAdminService.PurgeResult;
import com.orderly.api.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ceo/businesses")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BusinessAdminController {

    private final BusinessAdminService service;

    public BusinessAdminController(BusinessAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<BusinessAdminDetailResponse> listAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BusinessAdminDetailResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/suspend")
    public BusinessAdminDetailResponse suspend(@PathVariable UUID id,
                                                @AuthenticationPrincipal UserPrincipal actor,
                                                HttpServletRequest req) {
        return service.suspend(id, actor, req);
    }

    @PostMapping("/{id}/activate")
    public BusinessAdminDetailResponse activate(@PathVariable UUID id,
                                                 @AuthenticationPrincipal UserPrincipal actor,
                                                 HttpServletRequest req) {
        return service.activate(id, actor, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @AuthenticationPrincipal UserPrincipal actor,
                       HttpServletRequest req) {
        service.delete(id, actor, req);
    }

    @PostMapping("/{id}/change-plan")
    public BusinessAdminDetailResponse changePlan(@PathVariable UUID id,
                                                   @Valid @RequestBody ChangePlanRequest request,
                                                   @AuthenticationPrincipal UserPrincipal actor,
                                                   HttpServletRequest req) {
        return service.changePlan(id, request.planCode(), actor, req);
    }

    @PostMapping("/{id}/extend-trial")
    public BusinessAdminDetailResponse extendTrial(@PathVariable UUID id,
                                                    @Valid @RequestBody ExtendTrialRequest request,
                                                    @AuthenticationPrincipal UserPrincipal actor,
                                                    HttpServletRequest req) {
        return service.extendTrial(id, request.days(), actor, req);
    }

    @PostMapping("/{id}/free-months")
    public BusinessAdminDetailResponse grantFreeMonths(@PathVariable UUID id,
                                                        @Valid @RequestBody FreeMonthsRequest request,
                                                        @AuthenticationPrincipal UserPrincipal actor,
                                                        HttpServletRequest req) {
        return service.grantFreeMonths(id, request.months(), actor, req);
    }

    @PostMapping("/{id}/reset-usage")
    public BusinessAdminDetailResponse resetUsage(@PathVariable UUID id,
                                                   @AuthenticationPrincipal UserPrincipal actor,
                                                   HttpServletRequest req) {
        return service.resetUsage(id, actor, req);
    }

    @PostMapping("/{id}/impersonate")
    public ImpersonationResponse impersonate(@PathVariable UUID id,
                                              @AuthenticationPrincipal UserPrincipal actor,
                                              HttpServletRequest req) {
        return service.impersonate(id, actor, req);
    }

    @PostMapping("/{id}/force-logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forceLogout(@PathVariable UUID id,
                             @AuthenticationPrincipal UserPrincipal actor,
                             HttpServletRequest req) {
        service.forceLogout(id, actor, req);
    }

    // ── Request records ───────────────────────────────────────────────────────

    public record ChangePlanRequest(@NotBlank String planCode) {}

    public record ExtendTrialRequest(@Min(1) @Max(365) int days) {}

    public record FreeMonthsRequest(@Min(1) @Max(24) int months) {}
}
