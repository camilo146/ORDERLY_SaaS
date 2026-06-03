package com.orderly.api.business.application;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Input model for the create business use case.
 */
public record CreateBusinessCommand(
        UUID ownerId,
        @NotBlank String name,
        @NotBlank String businessType,
        String countryCode,
        String currencyCode,
        String timezone) {
}
