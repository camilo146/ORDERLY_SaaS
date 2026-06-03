package com.orderly.api.messaging.infrastructure.strategy;

import com.orderly.api.messaging.domain.model.MessageEventType;
import com.orderly.api.messaging.domain.service.MessagePresetStrategy;
import org.springframework.stereotype.Component;

/**
 * Default message set for pharmacies.
 */
@Component
public class PharmacyMessagePresetStrategy implements MessagePresetStrategy {

    @Override
    public boolean supports(String businessType) {
        return "PHARMACY".equalsIgnoreCase(businessType);
    }

    @Override
    public String resolve(MessageEventType eventType) {
        return switch (eventType) {
            case WELCOME_MESSAGE -> "Hola {{customer_name}}, te atiende {{business_name}} 💊";
            case ORDER_RECEIVED -> "Estamos revisando la disponibilidad de tu pedido #{{order_number}}.";
            case HUMAN_HANDOFF_STARTED -> "Un asesor revisará tu solicitud en unos minutos.";
            default -> "Gracias por comunicarte con {{business_name}}.";
        };
    }
}
