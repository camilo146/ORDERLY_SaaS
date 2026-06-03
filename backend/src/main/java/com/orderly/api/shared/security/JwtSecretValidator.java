package com.orderly.api.shared.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates JWT secret strength at application startup.
 *
 * Behavior:
 * - Null / blank secret  → throws (app cannot start).
 * - Secret shorter than 32 bytes → throws.
 * - Secret in KNOWN_INSECURE_SECRETS → throws (no warning-only mode in any environment).
 *
 * Rationale: a known secret lets an attacker self-sign tokens as SUPER_ADMIN.
 * Failing fast at startup is the only safe option; warning-only mode can be ignored.
 *
 * To generate a secure secret:
 *   openssl rand -hex 32
 * Then set:  ORDERLY_JWT_SECRET=<generated value>
 */
@Component
public class JwtSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    private static final Set<String> KNOWN_INSECURE_SECRETS = Set.of(
            "orderly-super-secret-key-orderly-super-secret-key",
            "secret",
            "changeme",
            "password",
            "jwt-secret",
            "mysecret"
    );

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    public JwtSecretValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void validate() {
        String secret = jwtProperties.jwtSecret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "[SECURITY] orderly.security.jwt-secret is not configured. " +
                    "Set ORDERLY_JWT_SECRET env var: export ORDERLY_JWT_SECRET=$(openssl rand -hex 32)");
        }

        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "[SECURITY] orderly.security.jwt-secret is too short (" + secret.length() +
                    " chars). HMAC-SHA256 requires at least " + MIN_SECRET_BYTES + " bytes. " +
                    "Generate one with: openssl rand -hex 32");
        }

        if (KNOWN_INSECURE_SECRETS.contains(secret)) {
            throw new IllegalStateException(
                    "[SECURITY] orderly.security.jwt-secret is a publicly-known default value. " +
                    "Any attacker can forge valid tokens for any user including SUPER_ADMIN. " +
                    "Set ORDERLY_JWT_SECRET env var: export ORDERLY_JWT_SECRET=$(openssl rand -hex 32)");
        }

        // Warn if the secret looks like the application.yml dev-placeholder (contains known markers).
        if (secret.contains("development-only") || secret.contains("NOT-safe-for-production")) {
            log.warn("╔══════════════════════════════════════════════════════════════════╗");
            log.warn("║  [SECURITY] JWT secret appears to be the development placeholder.║");
            log.warn("║  Set ORDERLY_JWT_SECRET before deploying to any shared environment║");
            log.warn("╚══════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("[SECURITY] JWT secret validated: {} bytes ✓", secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        }
    }
}
