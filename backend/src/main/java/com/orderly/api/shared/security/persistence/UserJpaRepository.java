package com.orderly.api.shared.security.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmailVerificationToken(String token);
    Optional<UserJpaEntity> findByPasswordResetToken(String token);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.emailVerified = false AND u.createdAt < :cutoff AND u.emailVerificationToken IS NOT NULL")
    List<UserJpaEntity> findUnverifiedBefore(@Param("cutoff") OffsetDateTime cutoff);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.lastLoginAt < :cutoff AND u.emailVerified = true AND u.inactivityEmailSentAt IS NULL")
    List<UserJpaEntity> findInactiveUsersWithNoReminderSince(@Param("cutoff") OffsetDateTime cutoff);
}
