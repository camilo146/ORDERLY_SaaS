package com.orderly.api.business.application;

import com.orderly.api.business.infrastructure.config.BusinessDefaultsProperties;
import com.orderly.api.business.infrastructure.repository.InMemoryBusinessRepository;
import com.orderly.api.business.infrastructure.service.DefaultSlugGenerator;
import com.orderly.api.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultBusinessServiceTest {

    @Test
    void shouldCreateBusinessWithConfiguredDefaults() {
        DefaultBusinessService service = buildService();

        var business = service.create(new CreateBusinessCommand(java.util.UUID.randomUUID(), "Farmacia Central",
                "pharmacy", null, null, null));

        assertEquals("Farmacia Central", business.name());
        assertEquals("farmacia-central", business.slug());
        assertEquals("CO", business.countryCode());
        assertEquals("COP", business.currencyCode());
        assertEquals("America/Bogota", business.timezone());
    }

    @Test
    void shouldRejectDuplicateSlug() {
        DefaultBusinessService service = buildService();
        java.util.UUID ownerId = java.util.UUID.randomUUID();
        service.create(new CreateBusinessCommand(ownerId, "Mi Tienda", "general_store", null, null, null));

        assertThrows(DomainException.class,
                () -> service
                        .create(new CreateBusinessCommand(ownerId, "Mi Tienda", "general_store", null, null, null)));
    }

    private DefaultBusinessService buildService() {
        return new DefaultBusinessService(
                new InMemoryBusinessRepository(),
                new DefaultSlugGenerator(),
                new BusinessDefaultsProperties("CO", "COP", "America/Bogota"));
    }
}
