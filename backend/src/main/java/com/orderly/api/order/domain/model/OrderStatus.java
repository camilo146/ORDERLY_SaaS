package com.orderly.api.order.domain.model;

/**
 * Supported order states for the MVP workflow.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    IN_PROGRESS,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
