package com.orderly.api.order.interfaces.rest;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request payload for order status changes.
 */
public record UpdateOrderStatusRequest(@NotBlank String status) {
}
