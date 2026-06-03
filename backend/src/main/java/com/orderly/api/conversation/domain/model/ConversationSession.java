package com.orderly.api.conversation.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Chatbot session stored in Redis.
 * Key: conversation:{businessId}:{customerPhone}
 * TTL: 30 minutes (reset on each message)
 */
public class ConversationSession implements Serializable {

    private UUID businessId;
    private String customerPhone; // E.164 without @s.whatsapp.net
    private String instanceName; // Evolution API instance name
    private ConversationState state;
    private List<CartItem> cart;
    private String deliveryAddress;
    private String customerName;
    private UUID pendingProductId;
    private String pendingProductName;
    private BigDecimal pendingProductUnitPrice;
    private Instant lastActivityAt;
    private boolean humanActive;
    /** Payment method chosen by customer: CASH, NEQUI or TRANSFER. */
    private String selectedPaymentMethod;
    /**
     * ID of the order created in ORDER_CONFIRMATION, used to attach payment proof.
     */
    private String pendingOrderId;
    /** Complaint description typed by the customer before the optional photo. */
    private String pendingComplaintDescription;

    public ConversationSession() {
        this.cart = new ArrayList<>();
        this.state = ConversationState.IDLE;
        this.lastActivityAt = Instant.now();
        this.humanActive = false;
    }

    public static ConversationSession start(UUID businessId, String customerPhone, String instanceName) {
        ConversationSession session = new ConversationSession();
        session.businessId = businessId;
        session.customerPhone = customerPhone;
        session.instanceName = instanceName;
        session.state = ConversationState.GREETING;
        return session;
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public void addToCart(CartItem item) {
        cart.add(item);
    }

    public void clearCart() {
        cart.clear();
    }

    // ── Getters & setters ─────────────────────────────────────────────────────

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public ConversationState getState() {
        return state;
    }

    public void setState(ConversationState state) {
        this.state = state;
    }

    public List<CartItem> getCart() {
        return cart;
    }

    public void setCart(List<CartItem> cart) {
        this.cart = cart;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public UUID getPendingProductId() {
        return pendingProductId;
    }

    public void setPendingProductId(UUID pendingProductId) {
        this.pendingProductId = pendingProductId;
    }

    public String getPendingProductName() {
        return pendingProductName;
    }

    public void setPendingProductName(String pendingProductName) {
        this.pendingProductName = pendingProductName;
    }

    public BigDecimal getPendingProductUnitPrice() {
        return pendingProductUnitPrice;
    }

    public void setPendingProductUnitPrice(BigDecimal pendingProductUnitPrice) {
        this.pendingProductUnitPrice = pendingProductUnitPrice;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public boolean isHumanActive() {
        return humanActive;
    }

    public void setHumanActive(boolean humanActive) {
        this.humanActive = humanActive;
    }

    public String getSelectedPaymentMethod() {
        return selectedPaymentMethod;
    }

    public void setSelectedPaymentMethod(String selectedPaymentMethod) {
        this.selectedPaymentMethod = selectedPaymentMethod;
    }

    public String getPendingOrderId() {
        return pendingOrderId;
    }

    public void setPendingOrderId(String pendingOrderId) {
        this.pendingOrderId = pendingOrderId;
    }

    public String getPendingComplaintDescription() {
        return pendingComplaintDescription;
    }

    public void setPendingComplaintDescription(String pendingComplaintDescription) {
        this.pendingComplaintDescription = pendingComplaintDescription;
    }

    // ── Nested types ──────────────────────────────────────────────────────────

    public static class CartItem implements Serializable {
        private UUID productId;
        private String productName;
        private int quantity;
        private java.math.BigDecimal unitPrice;

        public CartItem() {
        }

        public CartItem(UUID productId, String productName, int quantity, java.math.BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public UUID getProductId() {
            return productId;
        }

        public void setProductId(UUID productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public java.math.BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(java.math.BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}
