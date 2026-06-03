package com.orderly.api.shared.security.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "forced_logout_at")
    private OffsetDateTime forcedLogoutAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(UUID id, String email, String fullName,
            String passwordHash, String role, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getForcedLogoutAt() {
        return forcedLogoutAt;
    }

    public void setForcedLogoutAt(OffsetDateTime forcedLogoutAt) {
        this.forcedLogoutAt = forcedLogoutAt;
    }
}
