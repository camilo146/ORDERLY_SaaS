package com.orderly.api.customer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    Optional<CustomerJpaEntity> findByBusinessIdAndWhatsappNumber(UUID businessId, String whatsappNumber);

    long countByBusinessId(UUID businessId);
}
