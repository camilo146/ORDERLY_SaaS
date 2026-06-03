package com.orderly.api.business.domain.event;

import java.util.UUID;

public record BusinessWhatsAppConnectedEvent(
        UUID businessId,
        String businessSlug,
        String instanceName) {
}
