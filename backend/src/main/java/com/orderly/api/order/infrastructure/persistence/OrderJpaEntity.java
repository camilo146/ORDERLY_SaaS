package com.orderly.api.order.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for orders.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_business_id", columnList = "business_id"),
        @Index(name = "idx_orders_status", columnList = "business_id,status")
})
public class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(nullable = false)
    private String status;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_whatsapp", nullable = false)
    private String customerWhatsapp;

    @Column(name = "delivery_type", nullable = false)
    private String deliveryType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_proof_url", length = 500)
    private String paymentProofUrl;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "placed_at", nullable = false)
    private OffsetDateTime placedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OrderJpaEntity() {
    }

    public OrderJpaEntity(UUID id, UUID businessId, String customerName, String customerWhatsapp,
            String deliveryType, String notes, BigDecimal totalAmount, String paymentMethod,
            String status, OffsetDateTime createdAt) {
        this.id = id;
        this.businessId = businessId;
        this.customerName = customerName;
        this.customerWhatsapp = customerWhatsapp;
        this.deliveryType = deliveryType;
        this.notes = notes;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.placedAt = createdAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerWhatsapp() {
        return customerWhatsapp;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public String getNotes() {
        return notes;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String url) {
        this.paymentProofUrl = url;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public OffsetDateTime getPlacedAt() {
        return placedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
