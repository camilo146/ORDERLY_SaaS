package com.orderly.api.shared.security;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input payload for self-service business registration.
 */
public record RegisterRequest(
                @NotBlank @Email @Size(max = 254) String email,
                @NotBlank @Size(min = 2, max = 100) String fullName,
                @NotBlank @Size(min = 8, max = 72)
                @Pattern(
                        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                        message = "La contraseña debe contener al menos una letra minúscula, una mayúscula y un número")
                String password,
                @NotBlank @Size(max = 150) String businessName,
                @NotBlank @Size(max = 100) String businessType,
                @Size(max = 2) String countryCode,
                @Size(max = 3) String currencyCode,
                @Size(max = 60) String timezone,
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
