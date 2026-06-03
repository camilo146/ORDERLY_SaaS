package com.orderly.api.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Evolution API connection properties (bound from application.yml).
 */
@ConfigurationProperties(prefix = "orderly.evolution")
public record EvolutionProperties(
        String baseUrl,
        String apiKey,
        String webhookSecret,
        String appBaseUrl) {
}
