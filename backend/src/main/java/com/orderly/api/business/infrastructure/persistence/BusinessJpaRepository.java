package com.orderly.api.business.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for business entities.
 */
public interface BusinessJpaRepository extends JpaRepository<BusinessJpaEntity, UUID> {

    boolean existsBySlugIgnoreCase(String slug);

    java.util.Optional<BusinessJpaEntity> findBySlugIgnoreCase(String slug);

    List<BusinessJpaEntity> findAllByOwnerId(UUID ownerId);

    @Transactional
    @Modifying
    @Query("UPDATE BusinessJpaEntity b SET b.botName = :botName, b.botEmoji = :botEmoji WHERE b.id = :id")
    void updateBotIdentity(@Param("id") UUID id, @Param("botName") String botName, @Param("botEmoji") String botEmoji);

    @Transactional
    @Modifying
    @Query("UPDATE BusinessJpaEntity b SET b.status = :status WHERE b.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    java.util.Optional<BusinessJpaEntity> findByStripeCustomerId(String stripeCustomerId);

    @Transactional
    @Modifying
    @Query("UPDATE BusinessJpaEntity b SET b.stripeCustomerId = :customerId WHERE b.id = :id")
    void updateStripeCustomerId(@Param("id") UUID id, @Param("customerId") String customerId);
}
