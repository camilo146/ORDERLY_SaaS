package com.orderly.api.messaging.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * In-memory store for per-tenant WhatsApp channel configuration.
 */
@Service
public class WhatsAppChannelService {

    private final Map<UUID, ChannelConfig> store = new ConcurrentHashMap<>();

    public ChannelConfig getOrEmpty(UUID businessId) {
        return store.getOrDefault(businessId, new ChannelConfig(
                businessId.toString(), "", "", "", "", "NOT_CONNECTED",
                List.of(), List.of()));
    }

    public ChannelConfig save(UUID businessId, ChannelConfig config) {
        store.put(businessId, config);
        return config;
    }

    /** Marks the channel as ACTIVE (QR scanned / confirmed by user). */
    public ChannelConfig confirmConnection(UUID businessId) {
        ChannelConfig existing = getOrEmpty(businessId);
        ChannelConfig confirmed = new ChannelConfig(
                existing.businessId(), existing.displayName(), existing.phoneNumber(),
                existing.phoneNumberId(), existing.wabaId(), "ACTIVE",
                existing.paymentMethods(), existing.deliveryZones());
        store.put(businessId, confirmed);
        return confirmed;
    }

    public record ChannelConfig(
            String businessId,
            String displayName,
            String phoneNumber,
            String phoneNumberId,
            String wabaId,
            String status,
            java.util.List<String> paymentMethods,
            java.util.List<String> deliveryZones) {
    }
}
