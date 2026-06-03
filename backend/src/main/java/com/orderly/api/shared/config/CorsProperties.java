package com.orderly.api.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS allowed-origins read from orderly.cors.allowed-origins (env: ORDERLY_CORS_ORIGINS).
 * Comma-separated values are automatically split by Spring Boot into a List.
 */
@ConfigurationProperties(prefix = "orderly.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
