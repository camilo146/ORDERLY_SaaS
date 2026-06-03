package com.orderly.api.shared.domain;

/**
 * Thrown when a tenant has reached their plan's active order limit.
 */
public class PlanLimitExceededException extends RuntimeException {

    public PlanLimitExceededException(String message) {
        super(message);
    }
}
