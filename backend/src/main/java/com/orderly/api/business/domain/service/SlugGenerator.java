package com.orderly.api.business.domain.service;

/**
 * Generates URL-safe slugs for tenant businesses.
 */
public interface SlugGenerator {

    String generate(String source);
}
