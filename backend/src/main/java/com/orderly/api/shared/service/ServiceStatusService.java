package com.orderly.api.shared.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks whether the ordering service is enabled or paused per tenant.
 * When disabled, the chatbot returns a friendly out-of-service message.
 */
@Service
public class ServiceStatusService {

    private final Map<UUID, Boolean> statusMap = new ConcurrentHashMap<>();

    public boolean isEnabled(UUID businessId) {
        return statusMap.getOrDefault(businessId, true);
    }

    public boolean toggle(UUID businessId) {
        boolean current = isEnabled(businessId);
        statusMap.put(businessId, !current);
        return !current;
    }

    public void setEnabled(UUID businessId, boolean enabled) {
        statusMap.put(businessId, enabled);
    }
}
