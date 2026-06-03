package com.orderly.api.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

public interface AdminAuditLogJpaRepository extends JpaRepository<AdminAuditLogJpaEntity, UUID> {

    List<AdminAuditLogJpaEntity> findAllByOrderByCreatedAtDesc(PageRequest pageRequest);
}
