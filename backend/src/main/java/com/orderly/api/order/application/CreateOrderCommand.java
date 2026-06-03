package com.orderly.api.order.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Input model for order creation.
 */
public record CreateOrderCommand(
        UUID businessId,
        @NotBlank String customerName,
        @NotBlank String customerWhatsapp,
        @NotBlank String deliveryType,
        String notes,
        @NotEmpty List<OrderItemInput> items,
        String paymentMethod) {
    public record OrderItemInput(
            @NotNull UUID productId,
            int quantity,
            String notes) {
    }
}
