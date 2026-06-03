package com.orderly.api.shared.security;

import java.util.UUID;

/**
 * Immutable local user account representation.
 */
public record UserAccount(
        UUID id,
        String email,
        String fullName,
        String passwordHash,
        String role) {
}
