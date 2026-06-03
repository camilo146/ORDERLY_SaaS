package com.orderly.api.admin.application;

import com.orderly.api.admin.domain.model.AdminAction;
import com.orderly.api.admin.domain.model.AdminAuditLog;
import com.orderly.api.admin.domain.port.AdminAuditLogRepository;
import com.orderly.api.shared.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AdminAuditLogRepository repository;

    public AuditLogService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(UserPrincipal actor, AdminAction action, String targetType,
                       UUID targetId, String targetName, String details, String ip) {
        AdminAuditLog log = AdminAuditLog.record(
                actor.userId(), actor.getUsername(), action,
                targetType, targetId, targetName, details, ip);
        repository.save(log);
    }

    public List<AdminAuditLog> findRecent(int limit) {
        return repository.findRecent(limit);
    }
}
