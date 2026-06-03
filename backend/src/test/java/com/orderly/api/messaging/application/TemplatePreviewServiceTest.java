package com.orderly.api.messaging.application;

import com.orderly.api.messaging.infrastructure.strategy.GenericRetailMessagePresetStrategy;
import com.orderly.api.messaging.infrastructure.strategy.PharmacyMessagePresetStrategy;
import com.orderly.api.messaging.infrastructure.strategy.RestaurantMessagePresetStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplatePreviewServiceTest {

    @Test
    void shouldRenderRestaurantPreviewWithVariables() {
        TemplatePreviewService service = buildService();

        String rendered = service.preview(
                "restaurant",
                "WELCOME_MESSAGE",
                null,
                Map.of("customer_name", "Laura", "business_name", "Pizza Nova"));

        assertTrue(rendered.contains("Laura"));
        assertTrue(rendered.contains("Pizza Nova"));
    }

    private TemplatePreviewService buildService() {
        MessagePresetFactory factory = new MessagePresetFactory(List.of(
                new RestaurantMessagePresetStrategy(),
                new PharmacyMessagePresetStrategy(),
                new GenericRetailMessagePresetStrategy()));

        return new TemplatePreviewService(factory);
    }
}
