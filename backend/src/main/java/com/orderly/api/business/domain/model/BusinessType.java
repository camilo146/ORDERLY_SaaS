package com.orderly.api.business.domain.model;

import com.orderly.api.shared.domain.DomainException;

import java.util.Arrays;

/**
 * Supported tenant verticals for the MVP.
 */
public enum BusinessType {
    RESTAURANT,
    PHARMACY,
    GENERAL_STORE,
    AUTO_PARTS,
    FASHION;

    public static BusinessType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(normalize(value)))
                .findFirst()
                .orElseThrow(() -> new DomainException("Unsupported business type: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_');
    }
}
