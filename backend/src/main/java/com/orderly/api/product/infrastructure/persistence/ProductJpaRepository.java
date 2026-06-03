package com.orderly.api.product.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    List<ProductJpaEntity> findAllByBusinessIdOrderBySortOrderAsc(UUID businessId);

    Optional<ProductJpaEntity> findByIdAndBusinessId(UUID id, UUID businessId);

    long countByBusinessId(UUID businessId);

    Optional<ProductJpaEntity> findFirstByBusinessIdAndIsAvailableTrue(UUID businessId);

    @Query("SELECT COUNT(p) FROM ProductJpaEntity p WHERE p.businessId = :bid AND p.isAvailable = true")
    long countAvailableByBusiness(@Param("bid") UUID businessId);
}
