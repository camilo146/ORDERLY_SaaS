package com.orderly.api.admin.domain.port;

import com.orderly.api.admin.domain.model.AdminAuditLog;

import java.util.List;

public interface AdminAuditLogRepository {
    AdminAuditLog save(AdminAuditLog log);
    List<AdminAuditLog> findRecent(int limit);
}
