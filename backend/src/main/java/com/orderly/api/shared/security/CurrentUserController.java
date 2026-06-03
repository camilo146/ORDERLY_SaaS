package com.orderly.api.shared.security;

import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.business.interfaces.rest.BusinessResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the authenticated user profile at the root API path expected by the
 * frontend contract.
 */
@RestController
public class CurrentUserController {

    private final BusinessRepository businessRepository;
    private final JwtService jwtService;

    public CurrentUserController(BusinessRepository businessRepository, JwtService jwtService) {
        this.businessRepository = businessRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/api/v1/me")
    public AuthResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        List<BusinessResponse> businesses = businessRepository.findAllByOwnerId(principal.userId())
                .stream()
                .map(BusinessResponse::from)
                .toList();

        return new AuthResponse(
                jwtService.createToken(principal),
                new AuthResponse.AuthUserResponse(
                        principal.userId().toString(),
                        principal.fullName(),
                        principal.getUsername(),
                        principal.role()),
                businesses);
    }
}
