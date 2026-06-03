package com.orderly.api.messaging.domain.model;

/**
 * Supported customer-facing message events.
 */
public enum MessageEventType {
    WELCOME_MESSAGE,
    AFTER_HOURS_MESSAGE,
    ORDER_RECEIVED,
    ORDER_CONFIRMED,
    ORDER_IN_PREPARATION,
    ORDER_READY,
    ORDER_DELIVERED,
    HUMAN_HANDOFF_STARTED,
    HUMAN_HANDOFF_RESOLVED
}
