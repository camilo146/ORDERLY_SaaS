package com.orderly.api.business.interfaces.rest;

import com.orderly.api.business.domain.model.Business;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Business representation returned by the API.
 */
public record BusinessResponse(
        UUID id,
        String name,
        String slug,
        String businessType,
        String status,
        String countryCode,
        String currencyCode,
        String timezone,
        OffsetDateTime createdAt) {
    public static BusinessResponse from(Business business) {
        return new BusinessResponse(
                business.id(),
                business.name(),
                business.slug(),
                business.businessType().name(),
                business.status().name(),
                business.countryCode(),
                business.currencyCode(),
                business.timezone(),
                business.createdAt());
    }
}
