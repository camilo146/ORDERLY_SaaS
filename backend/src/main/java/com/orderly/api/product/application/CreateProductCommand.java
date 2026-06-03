package com.orderly.api.product.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Input model for product creation.
 */
public record CreateProductCommand(
                UUID businessId,
                @NotBlank String name,
                String description,
                String imageUrl,
                @NotNull @DecimalMin(value = "0.01") BigDecimal price) {
}
