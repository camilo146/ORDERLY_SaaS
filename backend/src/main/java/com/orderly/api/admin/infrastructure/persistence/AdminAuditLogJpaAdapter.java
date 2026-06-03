package com.orderly.api.admin.infrastructure.persistence;

import com.orderly.api.admin.domain.model.AdminAction;
import com.orderly.api.admin.domain.model.AdminAuditLog;
import com.orderly.api.admin.domain.port.AdminAuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdminAuditLogJpaAdapter implements AdminAuditLogRepository {

    private final AdminAuditLogJpaRepository jpa;

    public AdminAuditLogJpaAdapter(AdminAuditLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AdminAuditLog save(AdminAuditLog log) {
        AdminAuditLogJpaEntity entity = toEntity(log);
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<AdminAuditLog> findRecent(int limit) {
        return jpa.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AdminAuditLogJpaEntity toEntity(AdminAuditLog log) {
        return new AdminAuditLogJpaEntity(
                log.id(), log.actorId(), log.actorEmail(), log.action().name(),
                log.targetType(), log.targetId(), log.targetName(),
                log.details(), log.ipAddress(), log.createdAt());
    }

    private AdminAuditLog toDomain(AdminAuditLogJpaEntity entity) {
        return AdminAuditLog.reconstitute(
                entity.getId(), entity.getActorId(), entity.getActorEmail(),
                AdminAction.valueOf(entity.getAction()),
                entity.getTargetType(), entity.getTargetId(), entity.getTargetName(),
                entity.getDetails(), entity.getIpAddress(), entity.getCreatedAt());
    }
}
