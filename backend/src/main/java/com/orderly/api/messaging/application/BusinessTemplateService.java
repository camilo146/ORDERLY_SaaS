package com.orderly.api.messaging.application;

import com.orderly.api.messaging.domain.model.MessageEventType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for per-tenant message template overrides.
 */
@Service
public class BusinessTemplateService {

    /** businessId -> eventType -> body */
    private final Map<UUID, Map<String, String>> store = new ConcurrentHashMap<>();

    private final MessagePresetFactory presetFactory;

    public BusinessTemplateService(MessagePresetFactory presetFactory) {
        this.presetFactory = presetFactory;
    }

    public List<TemplateDto> listForBusiness(UUID businessId, String businessType) {
        Map<String, String> overrides = store.getOrDefault(businessId, Map.of());
        return Arrays.stream(MessageEventType.values()).map(event -> {
            String eventKey = event.name();
            String body = overrides.getOrDefault(eventKey,
                    presetFactory.resolve(businessType, event));
            return new TemplateDto(eventKey, labelFor(event), body);
        }).toList();
    }

    public TemplateDto updateTemplate(UUID businessId, String businessType, String eventType, String body) {
        store.computeIfAbsent(businessId, k -> new ConcurrentHashMap<>())
                .put(eventType.toUpperCase(), body);
        return new TemplateDto(eventType.toUpperCase(), labelFor(
                MessageEventType.valueOf(eventType.toUpperCase())), body);
    }

    private String labelFor(MessageEventType event) {
        return switch (event) {
            case WELCOME_MESSAGE -> "Saludo de bienvenida";
            case AFTER_HOURS_MESSAGE -> "Fuera de horario";
            case ORDER_RECEIVED -> "Pedido recibido";
            case ORDER_CONFIRMED -> "Pedido confirmado";
            case ORDER_IN_PREPARATION -> "En preparación";
            case ORDER_READY -> "Listo para entrega";
            case ORDER_DELIVERED -> "Pedido entregado";
            case HUMAN_HANDOFF_STARTED -> "Handoff a operador";
            case HUMAN_HANDOFF_RESOLVED -> "Handoff resuelto";
        };
    }

    public record TemplateDto(String eventType, String label, String body) {
    }
}
