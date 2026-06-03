package com.orderly.api.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable JWT settings.
 */
@ConfigurationProperties(prefix = "orderly.security")
public record JwtProperties(
        String jwtSecret,
        long tokenValidityMinutes) {
}
