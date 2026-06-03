package com.orderly.api.complaint.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplaintJpaRepository extends JpaRepository<ComplaintJpaEntity, UUID> {
    List<ComplaintJpaEntity> findAllByBusinessIdOrderByCreatedAtDesc(UUID businessId);
}
