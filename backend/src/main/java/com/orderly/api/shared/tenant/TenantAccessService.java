package com.orderly.api.shared.tenant;

import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.shared.domain.DomainException;
import com.orderly.api.shared.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Validates that the authenticated user can access the requested tenant.
 */
@Service
public class TenantAccessService {

    private final BusinessRepository businessRepository;

    public TenantAccessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public UUID validateTenantAccess(UUID businessId) {
        UUID currentTenant = TenantContext.getTenantId()
                .orElseThrow(() -> new DomainException("Missing X-Business-Id header."));

        if (!currentTenant.equals(businessId)) {
            throw new DomainException("Tenant header does not match the requested business.");
        }

        UserPrincipal principal = currentPrincipal();
        if ("SUPER_ADMIN".equalsIgnoreCase(principal.role())) {
            return businessId;
        }

        boolean hasAccess = businessRepository.findAllByOwnerId(principal.userId()).stream()
                .anyMatch(business -> business.id().equals(businessId));

        if (!hasAccess) {
            throw new AccessDeniedException("You do not have access to this business.");
        }

        return businessId;
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Authentication is required.");
        }
        return principal;
    }
}
