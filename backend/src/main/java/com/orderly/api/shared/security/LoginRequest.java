package com.orderly.api.shared.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload.
 */
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password) {
}
