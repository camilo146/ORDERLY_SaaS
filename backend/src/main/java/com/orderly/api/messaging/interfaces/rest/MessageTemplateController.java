package com.orderly.api.messaging.interfaces.rest;

import com.orderly.api.messaging.application.TemplatePreviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoints for message preview and customization support.
 */
@RestController
@RequestMapping("/api/v1/message-templates")
public class MessageTemplateController {

    private final TemplatePreviewService templatePreviewService;

    public MessageTemplateController(TemplatePreviewService templatePreviewService) {
        this.templatePreviewService = templatePreviewService;
    }

    /**
     * Generates a preview for a message template using input variables.
     */
    @PostMapping("/preview")
    public TemplatePreviewResponse preview(@Valid @RequestBody TemplatePreviewRequest request) {
        String renderedText = templatePreviewService.preview(
                request.businessType(),
                request.eventType(),
                request.customTemplate(),
                request.variables() == null ? Map.of() : request.variables());

        return new TemplatePreviewResponse(renderedText);
    }
}
