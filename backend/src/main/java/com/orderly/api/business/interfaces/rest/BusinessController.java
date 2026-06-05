package com.orderly.api.business.interfaces.rest;

import com.orderly.api.business.application.CreateBusinessCommand;
import com.orderly.api.business.application.CreateBusinessUseCase;
import com.orderly.api.business.application.FindBusinessUseCase;
import com.orderly.api.shared.security.UserPrincipal;
import com.orderly.api.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for tenant business management.
 */
@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final CreateBusinessUseCase createBusinessUseCase;
    private final FindBusinessUseCase findBusinessUseCase;
    private final TenantAccessService tenantAccessService;

    public BusinessController(CreateBusinessUseCase createBusinessUseCase,
                              FindBusinessUseCase findBusinessUseCase,
                              TenantAccessService tenantAccessService) {
        this.createBusinessUseCase = createBusinessUseCase;
        this.findBusinessUseCase = findBusinessUseCase;
        this.tenantAccessService = tenantAccessService;
    }

    /**
     * Creates a new tenant business.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBusinessRequest request) {
        CreateBusinessCommand command = new CreateBusinessCommand(
                principal.userId(),
                request.name(),
                request.businessType(),
                request.countryCode(),
                request.currencyCode(),
                request.timezone());

        return BusinessResponse.from(createBusinessUseCase.create(command));
    }

    /**
     * Retrieves a business by its identifier.
     *
     * [SECURITY] validateTenantAccess ensures the caller owns this businessId (or is SUPER_ADMIN).
     * Without this check any authenticated user could read any tenant's details (IDOR).
     * The frontend sets X-Business-Id matching the URL param, so this check succeeds for
     * legitimate requests and blocks cross-tenant probing.
     */
    @GetMapping("/{businessId}")
    public BusinessResponse findById(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        return BusinessResponse.from(findBusinessUseCase.findById(businessId));
    }
}
