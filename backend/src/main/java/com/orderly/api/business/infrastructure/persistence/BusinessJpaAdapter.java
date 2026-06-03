package com.orderly.api.business.infrastructure.persistence;

import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.model.BusinessStatus;
import com.orderly.api.business.domain.model.BusinessType;
import com.orderly.api.business.domain.port.BusinessRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed adapter for business persistence (H2 file or PostgreSQL).
 */
@Repository
@Primary
public class BusinessJpaAdapter implements BusinessRepository {

    private final BusinessJpaRepository repository;

    @PersistenceContext
    private EntityManager em;

    public BusinessJpaAdapter(BusinessJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Business save(Business business) {
        BusinessJpaEntity entity = toEntity(business);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Business> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return repository.existsBySlugIgnoreCase(slug);
    }

    @Override
    public Optional<Business> findBySlug(String slug) {
        return repository.findBySlugIgnoreCase(slug).map(this::toDomain);
    }

    @Override
    public java.util.List<Business> findAllByOwnerId(UUID ownerId) {
        return repository.findAllByOwnerId(ownerId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public java.util.List<Business> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void updateBotIdentity(UUID businessId, String botName, String botEmoji) {
        repository.updateBotIdentity(businessId, botName, botEmoji);
    }

    @Override
    @Transactional
    public void updateStatus(UUID businessId, BusinessStatus newStatus) {
        repository.updateStatus(businessId, newStatus.name());
    }

    @Override
    @Transactional
    public void deleteById(UUID businessId) {
        // Delete in FK-dependency order (children before parent).
        // Only tables that have JPA entity mappings (Flyway is disabled — ddl-auto:update
        // only creates tables backed by @Entity classes).
        nativeDelete("DELETE FROM order_items        WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM subscription_usage WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM orders             WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM subscriptions      WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM products           WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM customers          WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM complaints         WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM churn_alerts       WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM notification_queue WHERE business_id = :id", businessId);
        nativeDelete("DELETE FROM businesses         WHERE id = :id",          businessId);
    }

    private void nativeDelete(String sql, UUID id) {
        em.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }

    private BusinessJpaEntity toEntity(Business business) {
        return new BusinessJpaEntity(
                business.id(),
                business.ownerId(),
                business.name(),
                business.slug(),
                business.businessType().name(),
                business.status().name(),
                business.countryCode(),
                business.currencyCode(),
                business.timezone(),
                business.createdAt());
    }

    private Business toDomain(BusinessJpaEntity entity) {
        return Business.restore(
                entity.getId(),
                entity.getOwnerId(),
                entity.getName(),
                entity.getSlug(),
                BusinessType.valueOf(entity.getBusinessType()),
                BusinessStatus.valueOf(entity.getStatus()),
                entity.getCountryCode(),
                entity.getCurrencyCode(),
                entity.getTimezone(),
                entity.getCreatedAt(),
                entity.getBotName(),
                entity.getBotEmoji(),
                entity.getPaymentNequi(),
                entity.getPaymentBankName(),
                entity.getPaymentBankAccount(),
                entity.getPaymentBankHolder());
    }
}
