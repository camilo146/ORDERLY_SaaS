package com.orderly.api.business.domain.port;

import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.model.BusinessStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for business aggregates.
 */
public interface BusinessRepository {

    Business save(Business business);

    Optional<Business> findById(UUID id);

    boolean existsBySlug(String slug);

    Optional<Business> findBySlug(String slug);

    List<Business> findAllByOwnerId(UUID ownerId);

    List<Business> findAll();

    void updateBotIdentity(UUID businessId, String botName, String botEmoji);

    void updateStatus(UUID businessId, BusinessStatus newStatus);

    void deleteById(UUID businessId);
}
