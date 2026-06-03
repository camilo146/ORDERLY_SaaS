package com.orderly.api.messaging.application;

import com.orderly.api.messaging.domain.model.MessageEventType;
import com.orderly.api.shared.domain.DomainException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Renders customizable message previews for the dashboard.
 */
@Service
public class TemplatePreviewService {

    private final MessagePresetFactory messagePresetFactory;

    public TemplatePreviewService(MessagePresetFactory messagePresetFactory) {
        this.messagePresetFactory = messagePresetFactory;
    }

    public String preview(String businessType, String eventType, String customTemplate, Map<String, Object> variables) {
        String resolvedTemplate = resolveTemplate(businessType, eventType, customTemplate);
        return applyVariables(resolvedTemplate, variables);
    }

    private String resolveTemplate(String businessType, String eventType, String customTemplate) {
        if (customTemplate != null && !customTemplate.isBlank()) {
            return customTemplate;
        }

        try {
            MessageEventType messageEventType = MessageEventType.valueOf(eventType.trim().toUpperCase());
            return messagePresetFactory.resolve(businessType, messageEventType);
        } catch (IllegalArgumentException exception) {
            throw new DomainException("Unsupported event type: " + eventType);
        }
    }

    private String applyVariables(String template, Map<String, Object> variables) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            rendered = rendered.replace(key, String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}
