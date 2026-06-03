package com.orderly.api.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated user representation placed into the Spring Security context.
 */
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final String role;
    private final OffsetDateTime forcedLogoutAt;

    public UserPrincipal(UUID userId, String email, String fullName, String passwordHash, String role) {
        this(userId, email, fullName, passwordHash, role, null);
    }

    public UserPrincipal(UUID userId, String email, String fullName, String passwordHash,
                         String role, OffsetDateTime forcedLogoutAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.forcedLogoutAt = forcedLogoutAt;
    }

    public UUID userId() {
        return userId;
    }

    public String fullName() {
        return fullName;
    }

    public String role() {
        return role;
    }

    public OffsetDateTime forcedLogoutAt() {
        return forcedLogoutAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
