package com.orderly.api.business.interfaces.rest;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request for tenant business creation.
 */
public record CreateBusinessRequest(
        @NotBlank String name,
        @NotBlank String businessType,
        String countryCode,
        String currencyCode,
        String timezone) {
}
