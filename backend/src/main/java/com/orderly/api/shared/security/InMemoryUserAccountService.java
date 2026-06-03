package com.orderly.api.shared.security;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.orderly.api.shared.domain.DomainException;
import com.orderly.api.shared.security.persistence.UserJpaEntity;
import com.orderly.api.shared.security.persistence.UserJpaRepository;

/**
 * User account service backed by JPA — data survives backend restarts.
 *
 * Seed account password resolution (checked in order for each role):
 *  1. Role-specific env var   (ORDERLY_SUPERADMIN_PASSWORD / ORDERLY_ADMIN_PASSWORD / ORDERLY_OPERATOR_PASSWORD)
 *  2. Shared fallback env var (ORDERLY_SEED_PASSWORD) — backward-compatible with previous config
 *  3. Hardcoded dev default   "Orderly123!" — triggers a startup warning
 *
 * Using separate env vars per account is strongly recommended so that compromising
 * the lowest-privilege account (operator) does not reveal the superadmin password.
 */
@Service
public class InMemoryUserAccountService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryUserAccountService.class);

    private static final String DEFAULT_SEED_PASSWORD = "Orderly123!";

    // Each account resolves its own env var first, then the shared fallback, then the default.
    private static final String SUPER_ADMIN_PASSWORD =
            resolvePassword("ORDERLY_SUPERADMIN_PASSWORD", "ORDERLY_SEED_PASSWORD", DEFAULT_SEED_PASSWORD);
    private static final String ADMIN_PASSWORD =
            resolvePassword("ORDERLY_ADMIN_PASSWORD", "ORDERLY_SEED_PASSWORD", DEFAULT_SEED_PASSWORD);
    private static final String OPERATOR_PASSWORD =
            resolvePassword("ORDERLY_OPERATOR_PASSWORD", "ORDERLY_SEED_PASSWORD", DEFAULT_SEED_PASSWORD);

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserJpaRepository userRepository;

    public InMemoryUserAccountService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserPrincipal authenticate(String email, String rawPassword) {
        UserJpaEntity entity = findEntity(email);
        if (!passwordEncoder.matches(rawPassword, entity.getPasswordHash())) {
            throw new DomainException("Invalid email or password.");
        }
        return toPrincipal(entity);
    }

    public UserPrincipal loadPrincipal(String email) {
        return toPrincipal(findEntity(email));
    }

    private UserJpaEntity findEntity(String email) {
        return userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new DomainException("Invalid email or password."));
    }

    private UserPrincipal toPrincipal(UserJpaEntity entity) {
        return new UserPrincipal(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getForcedLogoutAt());
    }

    public UserPrincipal registerNew(String email, String fullName, String rawPassword, String role) {
        String key = normalize(email);
        if (userRepository.existsByEmail(key)) {
            throw new DomainException("Email already registered.");
        }
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(),
                key,
                fullName.trim(),
                passwordEncoder.encode(rawPassword),
                role,
                OffsetDateTime.now());
        userRepository.save(entity);
        return toPrincipal(entity);
    }

    public UserPrincipal loadById(UUID userId) {
        UserJpaEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found: " + userId));
        return toPrincipal(entity);
    }

    public void forceLogout(UUID userId) {
        UserJpaEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found: " + userId));
        entity.setForcedLogoutAt(OffsetDateTime.now());
        userRepository.save(entity);
    }

    /** Called once at startup to ensure default accounts exist. */
    public void seedDefaults() {
        warnIfUsingDefaults();
        seedIfAbsent("superadmin@orderly.local", "Super Administrador ORDERLY", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        seedIfAbsent("admin@orderly.local",      "Administrador ORDERLY",       ADMIN_PASSWORD,       "ADMIN");
        seedIfAbsent("ops@orderly.local",         "Operaciones ORDERLY",         OPERATOR_PASSWORD,    "OPERATOR");
    }

    private void warnIfUsingDefaults() {
        boolean superAdminDefault = DEFAULT_SEED_PASSWORD.equals(SUPER_ADMIN_PASSWORD);
        boolean adminDefault      = DEFAULT_SEED_PASSWORD.equals(ADMIN_PASSWORD);
        boolean operatorDefault   = DEFAULT_SEED_PASSWORD.equals(OPERATOR_PASSWORD);

        if (superAdminDefault || adminDefault || operatorDefault) {
            log.warn("╔══════════════════════════════════════════════════════════════════════╗");
            log.warn("║  [SECURITY] One or more seed accounts use the default password.     ║");
            log.warn("║  Set per-account env vars before deploying to any shared environment:║");
            log.warn("║    ORDERLY_SUPERADMIN_PASSWORD   (SUPER_ADMIN account)              ║");
            log.warn("║    ORDERLY_ADMIN_PASSWORD        (ADMIN account)                   ║");
            log.warn("║    ORDERLY_OPERATOR_PASSWORD     (OPERATOR account)                ║");
            log.warn("╚══════════════════════════════════════════════════════════════════════╝");
        }
    }

    private void seedIfAbsent(String email, String fullName, String rawPassword, String role) {
        String key = normalize(email);
        if (!userRepository.existsByEmail(key)) {
            userRepository.save(new UserJpaEntity(
                    UUID.randomUUID(), key, fullName,
                    passwordEncoder.encode(rawPassword), role,
                    OffsetDateTime.now()));
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * Resolves a password from environment variables in priority order.
     *
     * @param specificVar role-specific env var (checked first)
     * @param fallbackVar shared fallback env var (backward compat)
     * @param ultimate    hardcoded dev default (last resort)
     */
    private static String resolvePassword(String specificVar, String fallbackVar, String ultimate) {
        String specific = System.getenv(specificVar);
        if (specific != null && !specific.isBlank()) return specific;
        String fallback = System.getenv(fallbackVar);
        if (fallback != null && !fallback.isBlank()) return fallback;
        return ultimate;
    }
}
