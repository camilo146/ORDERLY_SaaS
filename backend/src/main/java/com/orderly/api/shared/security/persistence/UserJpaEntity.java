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

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token", unique = true)
    private String emailVerificationToken;

    @Column(name = "email_verification_expires_at")
    private OffsetDateTime emailVerificationExpiresAt;

    @Column(name = "password_reset_token", unique = true)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private OffsetDateTime passwordResetExpiresAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "onboarding_email_sent_at")
    private OffsetDateTime onboardingEmailSentAt;

    @Column(name = "inactivity_email_sent_at")
    private OffsetDateTime inactivityEmailSentAt;

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

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isEmailVerified() { return Boolean.TRUE.equals(emailVerified); }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }

    public OffsetDateTime getEmailVerificationExpiresAt() { return emailVerificationExpiresAt; }
    public void setEmailVerificationExpiresAt(OffsetDateTime emailVerificationExpiresAt) { this.emailVerificationExpiresAt = emailVerificationExpiresAt; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public OffsetDateTime getPasswordResetExpiresAt() { return passwordResetExpiresAt; }
    public void setPasswordResetExpiresAt(OffsetDateTime passwordResetExpiresAt) { this.passwordResetExpiresAt = passwordResetExpiresAt; }

    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public OffsetDateTime getOnboardingEmailSentAt() { return onboardingEmailSentAt; }
    public void setOnboardingEmailSentAt(OffsetDateTime onboardingEmailSentAt) { this.onboardingEmailSentAt = onboardingEmailSentAt; }

    public OffsetDateTime getInactivityEmailSentAt() { return inactivityEmailSentAt; }
    public void setInactivityEmailSentAt(OffsetDateTime inactivityEmailSentAt) { this.inactivityEmailSentAt = inactivityEmailSentAt; }
}
