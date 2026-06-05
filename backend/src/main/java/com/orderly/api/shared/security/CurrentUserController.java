package com.orderly.api.shared.security;

import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.business.interfaces.rest.BusinessResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the authenticated user profile at the root API path expected by the frontend.
 */
@RestController
public class CurrentUserController {

    private final BusinessRepository businessRepository;

    public CurrentUserController(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    /**
     * Returns the current user's profile and businesses.
     *
     * The response includes the token that arrived with THIS request rather than
     * minting a fresh one. Previously, calling /me issued a new JWT on every
     * invocation — an attacker with a stolen token could call /me in a loop to
     * rotate it indefinitely, effectively bypassing the 60-minute expiry window.
     * Returning the existing token preserves the AuthResponse contract while
     * closing that extension attack.
     */
    @GetMapping("/api/v1/me")
    public AuthResponse me(@AuthenticationPrincipal UserPrincipal principal,
                           HttpServletRequest request) {
        List<BusinessResponse> businesses = businessRepository.findAllByOwnerId(principal.userId())
                .stream()
                .map(BusinessResponse::from)
                .toList();

        String token = extractBearerToken(request);

        return new AuthResponse(
                token,
                new AuthResponse.AuthUserResponse(
                        principal.userId().toString(),
                        principal.fullName(),
                        principal.getUsername(),
                        principal.role()),
                businesses);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
}
