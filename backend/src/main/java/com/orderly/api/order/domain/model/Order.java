package com.orderly.api.order.domain.model;

import com.orderly.api.shared.domain.DomainException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order aggregate root for tenant order intake.
 */
public final class Order {

    private final UUID id;
    private final UUID businessId;
    private final String customerName;
    private final String customerWhatsapp;
    private final String deliveryType;
    private final String notes;
    private final List<OrderLine> items;
    private final BigDecimal totalAmount;
    private final OrderStatus status;
    private final OffsetDateTime createdAt;
    private final String paymentMethod;
    private final String paymentProofUrl;
    private final String cancelReason;

    private Order(
            UUID id,
            UUID businessId,
            String customerName,
            String customerWhatsapp,
            String deliveryType,
            String notes,
            List<OrderLine> items,
            BigDecimal totalAmount,
            OrderStatus status,
            OffsetDateTime createdAt,
            String paymentMethod,
            String paymentProofUrl,
            String cancelReason) {
        this.id = Objects.requireNonNull(id);
        this.businessId = Objects.requireNonNull(businessId);
        this.customerName = requireText(customerName, "Customer name is required.");
        this.customerWhatsapp = requireText(customerWhatsapp, "Customer WhatsApp is required.");
        this.deliveryType = requireText(deliveryType, "Delivery type is required.");
        this.notes = notes == null ? "" : notes.trim();
        this.items = List.copyOf(items);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.paymentMethod = paymentMethod;
        this.paymentProofUrl = paymentProofUrl;
        this.cancelReason = cancelReason;
        if (items.isEmpty()) {
            throw new DomainException("An order must contain at least one item.");
        }
    }

    public static Order create(
            UUID businessId,
            String customerName,
            String customerWhatsapp,
            String deliveryType,
            String notes,
            List<OrderLine> items,
            BigDecimal totalAmount,
            String paymentMethod) {
        return new Order(
                UUID.randomUUID(),
                businessId,
                customerName,
                customerWhatsapp,
                deliveryType,
                notes,
                items,
                totalAmount,
                OrderStatus.PENDING,
                OffsetDateTime.now(),
                paymentMethod,
                null,
                null);
    }

    public static Order reconstitute(UUID id, UUID businessId, String customerName, String customerWhatsapp,
            String deliveryType, String notes, List<OrderLine> items, BigDecimal totalAmount,
            OrderStatus status, OffsetDateTime createdAt, String paymentMethod, String paymentProofUrl,
            String cancelReason) {
        return new Order(id, businessId, customerName, customerWhatsapp, deliveryType, notes,
                items, totalAmount, status, createdAt, paymentMethod, paymentProofUrl, cancelReason);
    }

    public Order updateStatus(OrderStatus newStatus) {
        return new Order(id, businessId, customerName, customerWhatsapp, deliveryType, notes, items, totalAmount,
                newStatus, createdAt, paymentMethod, paymentProofUrl, cancelReason);
    }

    public Order withPaymentProof(String proofUrl) {
        return new Order(id, businessId, customerName, customerWhatsapp, deliveryType, notes, items, totalAmount,
                status, createdAt, paymentMethod, proofUrl, cancelReason);
    }

    public Order withCancelReason(String reason) {
        return new Order(id, businessId, customerName, customerWhatsapp, deliveryType, notes, items, totalAmount,
                OrderStatus.CANCELLED, createdAt, paymentMethod, paymentProofUrl, reason);
    }

    public UUID id() {
        return id;
    }

    public UUID businessId() {
        return businessId;
    }

    public String customerName() {
        return customerName;
    }

    public String customerWhatsapp() {
        return customerWhatsapp;
    }

    public String deliveryType() {
        return deliveryType;
    }

    public String notes() {
        return notes;
    }

    public List<OrderLine> items() {
        return items;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public OrderStatus status() {
        return status;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public String paymentMethod() {
        return paymentMethod;
    }

    public String paymentProofUrl() {
        return paymentProofUrl;
    }

    public String cancelReason() {
        return cancelReason;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(message);
        }
        return value.trim();
    }
}
