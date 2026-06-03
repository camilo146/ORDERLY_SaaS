package com.orderly.api.order.application;

import com.orderly.api.order.domain.model.Order;

/**
 * Domain event emitted when an order is created or updated.
 */
public record OrderChangedEvent(String eventType, Order order) {
}
