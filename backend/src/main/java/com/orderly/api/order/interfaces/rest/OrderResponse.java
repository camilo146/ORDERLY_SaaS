package com.orderly.api.order.interfaces.rest;

import com.orderly.api.order.domain.model.Order;
import com.orderly.api.order.domain.model.OrderLine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order representation returned by the API.
 */
public record OrderResponse(
        UUID id,
        UUID businessId,
        String customerName,
        String customerWhatsapp,
        String deliveryType,
        String notes,
        String status,
        BigDecimal totalAmount,
        List<OrderLineResponse> items,
        OffsetDateTime createdAt,
        String paymentMethod,
        String paymentProofUrl,
        String cancelReason) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(),
                order.businessId(),
                order.customerName(),
                order.customerWhatsapp(),
                order.deliveryType(),
                order.notes(),
                order.status().name(),
                order.totalAmount(),
                order.items().stream().map(OrderLineResponse::from).toList(),
                order.createdAt(),
                order.paymentMethod(),
                order.paymentProofUrl(),
                order.cancelReason());
    }

    public record OrderLineResponse(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            String notes) {
        static OrderLineResponse from(OrderLine line) {
            return new OrderLineResponse(line.productId(), line.productName(), line.quantity(), line.unitPrice(),
                    line.subtotal(), line.notes());
        }
    }
}
