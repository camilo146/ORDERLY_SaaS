package com.orderly.api.shared.domain;

/**
 * Base runtime exception for business rules and domain validation failures.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
