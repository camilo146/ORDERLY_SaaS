package com.orderly.api.messaging.interfaces.rest;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * HTTP request for template preview generation.
 */
public record TemplatePreviewRequest(
        @NotBlank String businessType,
        @NotBlank String eventType,
        String customTemplate,
        Map<String, Object> variables) {
}
