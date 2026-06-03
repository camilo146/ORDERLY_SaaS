package com.orderly.api.product.interfaces.rest;

import com.orderly.api.product.domain.model.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Product representation returned by the API.
 */
public record ProductResponse(
        UUID id,
        UUID businessId,
        String name,
        String description,
        String imageUrl,
        BigDecimal price,
        boolean available,
        OffsetDateTime createdAt,
        Integer stock) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id(),
                product.businessId(),
                product.name(),
                product.description(),
                product.imageUrl(),
                product.price(),
                product.available(),
                product.createdAt(),
                product.stock());
    }
}
