package com.orderly.api.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailLogJpaRepository extends JpaRepository<EmailLogJpaEntity, UUID> {

    List<EmailLogJpaEntity> findTop50ByOrderByCreatedAtDesc();
}
