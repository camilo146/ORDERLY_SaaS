package com.orderly.api.business.infrastructure.service;

import com.orderly.api.business.domain.service.SlugGenerator;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Default slug strategy for business names.
 */
@Component
public class DefaultSlugGenerator implements SlugGenerator {

    @Override
    public String generate(String source) {
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        return normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
