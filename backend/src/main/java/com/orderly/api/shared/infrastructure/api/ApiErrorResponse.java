package com.orderly.api.shared.infrastructure.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard API error payload.
 */
public record ApiErrorResponse(
        String code,
        String message,
        List<String> details,
        OffsetDateTime timestamp) {
}
