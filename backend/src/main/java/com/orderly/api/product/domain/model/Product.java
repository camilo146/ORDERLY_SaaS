package com.orderly.api.product.domain.model;

import com.orderly.api.shared.domain.DomainException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Product aggregate for a tenant catalog item.
 */
public final class Product {

    private final UUID id;
    private final UUID businessId;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final BigDecimal price;
    private final boolean available;
    private final OffsetDateTime createdAt;
    private final Integer stock;

    private Product(UUID id, UUID businessId, String name, String description, String imageUrl, BigDecimal price,
            boolean available, OffsetDateTime createdAt, Integer stock) {
        this.id = Objects.requireNonNull(id);
        this.businessId = Objects.requireNonNull(businessId);
        this.name = requireText(name, "Product name is required.");
        this.description = description == null ? "" : description.trim();
        this.imageUrl = normalizeImageUrl(imageUrl);
        this.price = requirePositive(price);
        this.available = available;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.stock = stock;
    }

    public static Product create(UUID businessId, String name, String description, BigDecimal price) {
        return create(businessId, name, description, null, price);
    }

    public static Product create(UUID businessId, String name, String description, String imageUrl, BigDecimal price) {
        return new Product(UUID.randomUUID(), businessId, name, description, imageUrl, price, true,
                OffsetDateTime.now(), null);
    }

    public static Product reconstitute(UUID id, UUID businessId, String name, String description,
            String imageUrl, BigDecimal price, boolean available, OffsetDateTime createdAt, Integer stock) {
        return new Product(id, businessId, name, description, imageUrl, price, available, createdAt, stock);
    }

    public UUID id() {
        return id;
    }

    public UUID businessId() {
        return businessId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public BigDecimal price() {
        return price;
    }

    public boolean available() {
        return available;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public Integer stock() {
        return stock;
    }

    public Product withImageUrl(String imageUrl) {
        return new Product(id, businessId, name, description, imageUrl, price, available, createdAt, stock);
    }

    public Product withUpdatedFields(String name, String description, BigDecimal price, boolean available, Integer stock) {
        return new Product(id, businessId, name, description, imageUrl, price, available, createdAt, stock);
    }

    public Product decrementStock(int quantity) {
        if (stock == null) return this;
        int newStock = Math.max(0, stock - quantity);
        return new Product(id, businessId, name, description, imageUrl, price, available, createdAt, newStock);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(message);
        }
        return value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new DomainException("Product price must be positive.");
        }
        return value;
    }

    private static String normalizeImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
