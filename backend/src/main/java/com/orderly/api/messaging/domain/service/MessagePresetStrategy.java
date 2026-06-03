package com.orderly.api.messaging.domain.service;

import com.orderly.api.messaging.domain.model.MessageEventType;

/**
 * Strategy for selecting default text per business vertical and event.
 */
public interface MessagePresetStrategy {

    boolean supports(String businessType);

    String resolve(MessageEventType eventType);
}
