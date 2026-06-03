package com.orderly.api.shared.security;

import com.orderly.api.business.interfaces.rest.BusinessResponse;

import java.util.List;

/**
 * Login response payload including token and visible businesses.
 */
public record AuthResponse(
        String accessToken,
        AuthUserResponse user,
        List<BusinessResponse> businesses) {
    public record AuthUserResponse(String id, String fullName, String email, String role) {
    }
}
