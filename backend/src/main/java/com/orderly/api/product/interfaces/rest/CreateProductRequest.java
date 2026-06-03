package com.orderly.api.product.interfaces.rest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * HTTP request payload for product creation.
 */
public record CreateProductRequest(
                @NotBlank String name,
                String description,
                String imageUrl,
                @NotNull @DecimalMin(value = "0.01") BigDecimal price) {
}
