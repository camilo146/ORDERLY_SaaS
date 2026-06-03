package com.orderly.api.business.domain.model;

/**
 * Lifecycle states for a tenant business.
 */
public enum BusinessStatus {
    SETUP,
    PENDING_WHATSAPP,
    TRIALING,
    ACTIVE,
    SUSPENDED,
    CANCELLED
}
