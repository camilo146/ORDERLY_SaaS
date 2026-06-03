package com.orderly.api.business.application;

import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.model.BusinessType;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.business.domain.service.SlugGenerator;
import com.orderly.api.business.infrastructure.config.BusinessDefaultsProperties;
import com.orderly.api.shared.domain.DomainException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Coordinates business creation and lookup rules.
 */
@Service
public class DefaultBusinessService implements CreateBusinessUseCase, FindBusinessUseCase {

    private final BusinessRepository businessRepository;
    private final SlugGenerator slugGenerator;
    private final BusinessDefaultsProperties defaults;

    public DefaultBusinessService(
            BusinessRepository businessRepository,
            SlugGenerator slugGenerator,
            BusinessDefaultsProperties defaults) {
        this.businessRepository = businessRepository;
        this.slugGenerator = slugGenerator;
        this.defaults = defaults;
    }

    @Override
    public Business create(CreateBusinessCommand command) {
        String slug = slugGenerator.generate(command.name());
        validateUniqueSlug(slug);

        Business business = Business.create(
                command.ownerId(),
                command.name(),
                slug,
                BusinessType.fromValue(command.businessType()),
                firstNonBlank(command.countryCode(), defaults.defaultCountryCode()),
                firstNonBlank(command.currencyCode(), defaults.defaultCurrencyCode()),
                firstNonBlank(command.timezone(), defaults.defaultTimezone()));

        return businessRepository.save(business);
    }

    @Override
    public Business findById(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new DomainException("Business not found: " + businessId));
    }

    private void validateUniqueSlug(String slug) {
        if (businessRepository.existsBySlug(slug)) {
            throw new DomainException("A business with the same slug already exists.");
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
