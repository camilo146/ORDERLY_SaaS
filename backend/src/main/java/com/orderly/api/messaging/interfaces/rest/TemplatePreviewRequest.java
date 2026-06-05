package com.orderly.api.messaging.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * HTTP request for template preview generation.
 */
public record TemplatePreviewRequest(
        @NotBlank @Size(max = 100) String businessType,
        @NotBlank @Size(max = 100) String eventType,
        @Size(max = 5_000) String customTemplate,
        Map<String, Object> variables) {
}
