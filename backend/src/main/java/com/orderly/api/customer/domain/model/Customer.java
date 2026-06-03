package com.orderly.api.customer.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Customer(
        UUID id,
        UUID businessId,
        String whatsappNumber,
        String fullName,
        OffsetDateTime lastOrderAt,
        OffsetDateTime createdAt) {
}
