package com.orderly.api.product.application;

import java.math.BigDecimal;

public record UpdateProductCommand(String name, String description, BigDecimal price, boolean available, Integer stock) {
}
