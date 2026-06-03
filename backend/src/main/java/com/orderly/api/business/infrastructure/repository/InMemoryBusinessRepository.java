package com.orderly.api.business.infrastructure.repository;

import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.model.BusinessStatus;
import com.orderly.api.business.domain.port.BusinessRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory adapter — desactivado en favor del JPA adapter.
 * Mantenido solo como fallback de emergencia.
 */
@Repository
public class InMemoryBusinessRepository implements BusinessRepository {

    private final Map<UUID, Business> storage = new ConcurrentHashMap<>();

    @Override
    public Business save(Business business) {
        storage.put(business.id(), business);
        return business;
    }

    @Override
    public Optional<Business> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean existsBySlug(String slug) {
        return storage.values().stream().anyMatch(business -> business.slug().equalsIgnoreCase(slug));
    }

    @Override
    public Optional<Business> findBySlug(String slug) {
        return storage.values().stream()
                .filter(b -> b.slug().equalsIgnoreCase(slug))
                .findFirst();
    }

    @Override
    public List<Business> findAllByOwnerId(UUID ownerId) {
        return storage.values().stream()
                .filter(business -> business.ownerId().equals(ownerId))
                .toList();
    }

    @Override
    public List<Business> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public void updateBotIdentity(UUID businessId, String botName, String botEmoji) {
        // In-memory: no-op (not used in production)
    }

    @Override
    public void updateStatus(UUID businessId, BusinessStatus newStatus) {
        // In-memory: no-op (not used in production)
    }

    @Override
    public void deleteById(UUID businessId) {
        storage.remove(businessId);
    }
}
