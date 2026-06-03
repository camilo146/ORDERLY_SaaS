package com.orderly.api.messaging.infrastructure.strategy;

import com.orderly.api.messaging.domain.model.MessageEventType;
import com.orderly.api.messaging.domain.service.MessagePresetStrategy;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Default message set for generic retail businesses.
 */
@Component
public class GenericRetailMessagePresetStrategy implements MessagePresetStrategy {

    private static final Set<String> SUPPORTED_TYPES = Set.of("GENERAL_STORE", "AUTO_PARTS", "FASHION");

    @Override
    public boolean supports(String businessType) {
        return businessType != null && SUPPORTED_TYPES.contains(businessType.toUpperCase());
    }

    @Override
    public String resolve(MessageEventType eventType) {
        return switch (eventType) {
            case WELCOME_MESSAGE -> "Hola {{customer_name}}, gracias por contactar a {{business_name}}.";
            case ORDER_RECEIVED -> "Tu pedido #{{order_number}} fue recibido correctamente.";
            case ORDER_DELIVERED -> "Tu pedido fue entregado. Gracias por tu compra.";
            default -> "Estamos listos para ayudarte desde {{business_name}}.";
        };
    }
}
