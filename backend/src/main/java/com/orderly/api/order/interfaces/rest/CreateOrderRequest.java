package com.orderly.api.order.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * HTTP request payload for order creation.
 */
public record CreateOrderRequest(
        @NotBlank String customerName,
        @NotBlank String customerWhatsapp,
        @NotBlank String deliveryType,
        String notes,
        String paymentMethod,
        @NotEmpty List<@Valid OrderItemRequest> items) {
    public record OrderItemRequest(
            @NotNull UUID productId,
            int quantity,
            String notes) {
    }
}
