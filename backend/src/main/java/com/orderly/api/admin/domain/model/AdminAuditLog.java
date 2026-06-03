package com.orderly.api.admin.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AdminAuditLog {

    private final UUID id;
    private final UUID actorId;
    private final String actorEmail;
    private final AdminAction action;
    private final String targetType;
    private final UUID targetId;
    private final String targetName;
    private final String details;
    private final String ipAddress;
    private final OffsetDateTime createdAt;

    private AdminAuditLog(UUID id, UUID actorId, String actorEmail, AdminAction action,
                          String targetType, UUID targetId, String targetName,
                          String details, String ipAddress, OffsetDateTime createdAt) {
        this.id = id;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public static AdminAuditLog record(UUID actorId, String actorEmail, AdminAction action,
                                       String targetType, UUID targetId, String targetName,
                                       String details, String ipAddress) {
        return new AdminAuditLog(UUID.randomUUID(), actorId, actorEmail, action,
                targetType, targetId, targetName, details, ipAddress, OffsetDateTime.now());
    }

    public static AdminAuditLog reconstitute(UUID id, UUID actorId, String actorEmail, AdminAction action,
                                              String targetType, UUID targetId, String targetName,
                                              String details, String ipAddress, OffsetDateTime createdAt) {
        return new AdminAuditLog(id, actorId, actorEmail, action, targetType, targetId,
                targetName, details, ipAddress, createdAt);
    }

    public UUID id() { return id; }
    public UUID actorId() { return actorId; }
    public String actorEmail() { return actorEmail; }
    public AdminAction action() { return action; }
    public String targetType() { return targetType; }
    public UUID targetId() { return targetId; }
    public String targetName() { return targetName; }
    public String details() { return details; }
    public String ipAddress() { return ipAddress; }
    public OffsetDateTime createdAt() { return createdAt; }
}
