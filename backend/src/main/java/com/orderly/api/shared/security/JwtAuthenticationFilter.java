package com.orderly.api.shared.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts a Bearer token from the Authorization header and populates the
 * Spring Security context for the duration of the request.
 *
 * Failures are logged with enough context for security auditing while avoiding
 * leaking token content into log files.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final InMemoryUserAccountService userAccountService;

    public JwtAuthenticationFilter(JwtService jwtService, InMemoryUserAccountService userAccountService) {
        this.jwtService = jwtService;
        this.userAccountService = userAccountService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            UserPrincipal principal = userAccountService.loadPrincipal(username);

            if (principal.forcedLogoutAt() != null) {
                java.time.OffsetDateTime issuedAt = jwtService.extractIssuedAt(token);
                if (issuedAt.isBefore(principal.forcedLogoutAt())) {
                    log.debug("[AUTH] Token revoked by force-logout for user {} ip={}", username, request.getRemoteAddr());
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            log.debug("[AUTH] Expired token for request {} from {}", request.getRequestURI(), request.getRemoteAddr());
            SecurityContextHolder.clearContext();

        } catch (JwtException e) {
            // Malformed, tampered, or unsupported token — log as warning for security audit.
            log.warn("[AUTH] Invalid JWT token: type={} ip={} path={}",
                    e.getClass().getSimpleName(), request.getRemoteAddr(), request.getRequestURI());
            SecurityContextHolder.clearContext();

        } catch (RuntimeException e) {
            // Covers UserNotFoundException and other unexpected failures during user lookup.
            log.warn("[AUTH] Token validation failure: {} ip={} path={}",
                    e.getMessage(), request.getRemoteAddr(), request.getRequestURI());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
