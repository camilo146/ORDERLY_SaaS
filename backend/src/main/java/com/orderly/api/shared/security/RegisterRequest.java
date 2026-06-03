package com.orderly.api.shared.security;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input payload for self-service business registration.
 */
public record RegisterRequest(
                @NotBlank @Email String email,
                @NotBlank @Size(min = 2, max = 100) String fullName,
                @NotBlank @Size(min = 8, max = 72) String password,
                @NotBlank String businessName,
                @NotBlank String businessType,
                String countryCode,
                String currencyCode,
                String timezone,
                List<InitialProduct> products,
                @Size(max = 80) String botName,
                @Size(max = 10) String botEmoji) {

        /** Minimum catalog product entered during onboarding. */
        public record InitialProduct(
                        @NotBlank String name,
                        String description,
                        @NotNull @DecimalMin("0.01") BigDecimal price) {
        }
}
