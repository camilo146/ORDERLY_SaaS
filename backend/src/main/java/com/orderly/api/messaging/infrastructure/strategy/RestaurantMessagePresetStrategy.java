package com.orderly.api.messaging.infrastructure.strategy;

import com.orderly.api.messaging.domain.model.MessageEventType;
import com.orderly.api.messaging.domain.service.MessagePresetStrategy;
import org.springframework.stereotype.Component;

/**
 * Default message set for restaurants.
 */
@Component
public class RestaurantMessagePresetStrategy implements MessagePresetStrategy {

    @Override
    public boolean supports(String businessType) {
        return "RESTAURANT".equalsIgnoreCase(businessType);
    }

    @Override
    public String resolve(MessageEventType eventType) {
        return switch (eventType) {
            case WELCOME_MESSAGE -> "Hola {{customer_name}}, bienvenido a {{business_name}} 🍽️";
            case ORDER_RECEIVED -> "Recibimos tu pedido #{{order_number}}. En breve te confirmamos.";
            case ORDER_IN_PREPARATION -> "Tu pedido #{{order_number}} ya está en preparación.";
            case ORDER_READY -> "Tu pedido #{{order_number}} está listo para entrega.";
            case HUMAN_HANDOFF_STARTED -> "Te conectamos con un asesor para ayudarte mejor.";
            default -> "Gracias por escribir a {{business_name}}.";
        };
    }
}
