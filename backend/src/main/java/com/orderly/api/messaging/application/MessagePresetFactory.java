package com.orderly.api.messaging.application;

import com.orderly.api.messaging.domain.model.MessageEventType;
import com.orderly.api.messaging.domain.service.MessagePresetStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Selects the most suitable preset strategy for a business type.
 */
@Component
public class MessagePresetFactory {

    private final List<MessagePresetStrategy> strategies;

    public MessagePresetFactory(List<MessagePresetStrategy> strategies) {
        this.strategies = strategies;
    }

    public String resolve(String businessType, MessageEventType eventType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(businessType))
                .findFirst()
                .map(strategy -> strategy.resolve(eventType))
                .orElse("Hola {{customer_name}}, gracias por escribir a {{business_name}}.");
    }
}
