package com.orderly.api.business.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable defaults for new tenant creation.
 */
@ConfigurationProperties(prefix = "orderly.business")
public record BusinessDefaultsProperties(
        String defaultCountryCode,
        String defaultCurrencyCode,
        String defaultTimezone) {
}
