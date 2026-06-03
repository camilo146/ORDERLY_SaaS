package com.orderly.api.order.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable line item snapshot inside an order.
 */
public record OrderLine(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String notes) {
}
