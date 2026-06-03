package com.orderly.api.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Creates and validates JWT tokens for stateless API authentication.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.tokenValidityMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("userId", principal.userId().toString())
                .claim("role", principal.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("userId", String.class));
    }

    public OffsetDateTime extractIssuedAt(String token) {
        Date issuedAt = parseClaims(token).getIssuedAt();
        return issuedAt.toInstant().atOffset(java.time.ZoneOffset.UTC);
    }

    /** Short-lived token (15 min) that allows a CEO to act as a business owner. */
    public String createImpersonationToken(UserPrincipal targetOwner, UUID businessId, String adminEmail) {
        Instant now = Instant.now();
        Instant expiration = now.plus(15, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(targetOwner.getUsername())
                .claim("userId", targetOwner.userId().toString())
                .claim("role", targetOwner.role())
                .claim("businessId", businessId.toString())
                .claim("impersonated", true)
                .claim("impersonatedBy", adminEmail)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
