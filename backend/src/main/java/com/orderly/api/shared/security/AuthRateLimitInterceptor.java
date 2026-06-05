package com.orderly.api.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter for authentication endpoints.
 *
 * Limits:
 *   POST /api/v1/auth/login              → 5 attempts / minute / IP
 *   POST /api/v1/auth/register           → 3 attempts / minute / IP
 *   POST /api/v1/auth/forgot-password    → 3 attempts / minute / IP
 *   POST /api/v1/auth/reset-password     → 5 attempts / minute / IP
 *   POST /api/v1/auth/resend-verification→ 2 attempts / minute / IP
 *   GET  /api/v1/auth/verify-email       → 10 attempts / minute / IP
 *
 * Security properties:
 * - Uses Redis as the primary counter store so limits survive restarts and work
 *   correctly across multiple backend instances (horizontal scaling).
 * - Falls back to an in-memory ConcurrentHashMap if Redis is unavailable ("fail open").
 * - WindowCounter uses a synchronized method to prevent the TOCTOU race condition.
 * - X-Forwarded-For is only trusted when remoteAddr is a configured trusted proxy.
 */
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitInterceptor.class);

    private static final int LOGIN_MAX_PER_MINUTE               = 5;
    private static final int REGISTER_MAX_PER_MINUTE            = 3;
    private static final int FORGOT_PASSWORD_MAX_PER_MINUTE     = 3;
    private static final int RESET_PASSWORD_MAX_PER_MINUTE      = 5;
    private static final int RESEND_VERIFICATION_MAX_PER_MINUTE = 2;
    private static final int VERIFY_EMAIL_MAX_PER_MINUTE        = 10;
    private static final long WINDOW_SECONDS       = 60L;
    private static final long WINDOW_MILLIS        = WINDOW_SECONDS * 1000L;
    private static final long STALE_THRESHOLD_MILLIS  = 10 * WINDOW_MILLIS;
    private static final long CLEANUP_INTERVAL_MILLIS =  5 * WINDOW_MILLIS;

    @Value("${orderly.security.trusted-proxy-ips:}")
    private String trustedProxyIps;

    private final RedisRateLimiter redisRateLimiter;
    private final Map<String, WindowCounter> fallbackCounters = new ConcurrentHashMap<>();
    private volatile long lastCleanup = Instant.now().toEpochMilli();

    public AuthRateLimitInterceptor(RedisRateLimiter redisRateLimiter) {
        this.redisRateLimiter = redisRateLimiter;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        String path = request.getRequestURI();
        if (!isProtected(path)) {
            return true;
        }

        String ip  = resolveClientIp(request);
        int    max = resolveMax(path);
        String key = ip + "::" + path;

        long count = redisRateLimiter.increment(key, WINDOW_SECONDS);

        if (count == -1L) {
            // Redis unavailable: fall back to in-memory counter (single-instance guarantee only)
            evictStaleCounters();
            count = fallbackCounters.computeIfAbsent(key, k -> new WindowCounter()).incrementAndGet();
        }

        if (count > max) {
            log.warn("[RATE-LIMIT] IP={} path={} attempts={}", ip, path, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please wait 60 seconds before retrying.\"}");
            return false;
        }

        return true;
    }

    private boolean isProtected(String path) {
        return path.endsWith("/auth/login")
                || path.endsWith("/auth/register")
                || path.endsWith("/auth/forgot-password")
                || path.endsWith("/auth/reset-password")
                || path.endsWith("/auth/resend-verification")
                || path.endsWith("/auth/verify-email");
    }

    private int resolveMax(String path) {
        if (path.endsWith("/register"))            return REGISTER_MAX_PER_MINUTE;
        if (path.endsWith("/forgot-password"))     return FORGOT_PASSWORD_MAX_PER_MINUTE;
        if (path.endsWith("/reset-password"))      return RESET_PASSWORD_MAX_PER_MINUTE;
        if (path.endsWith("/resend-verification")) return RESEND_VERIFICATION_MAX_PER_MINUTE;
        if (path.endsWith("/verify-email"))        return VERIFY_EMAIL_MAX_PER_MINUTE;
        return LOGIN_MAX_PER_MINUTE;
    }

    /**
     * Resolves the real client IP.
     *
     * X-Forwarded-For is only consulted when the direct caller (remoteAddr) is a
     * configured trusted proxy. This prevents attackers from bypassing rate limiting
     * by spoofing the X-Forwarded-For header.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // X-Forwarded-For format: "client, proxy1, proxy2"
                // The leftmost entry is the original client IP.
                return forwarded.split(",")[0].strip();
            }
        }

        return remoteAddr;
    }

    /**
     * Returns true only if {@code remoteAddr} exactly matches one of the configured
     * trusted proxy IP addresses. An empty configuration means no proxy is trusted.
     */
    private boolean isTrustedProxy(String remoteAddr) {
        if (trustedProxyIps == null || trustedProxyIps.isBlank()) {
            return false;
        }
        return Arrays.stream(trustedProxyIps.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(remoteAddr::equals);
    }

    private void evictStaleCounters() {
        long now = Instant.now().toEpochMilli();
        if (now - lastCleanup > CLEANUP_INTERVAL_MILLIS) {
            fallbackCounters.entrySet().removeIf(e -> e.getValue().isStale(STALE_THRESHOLD_MILLIS));
            lastCleanup = now;
        }
    }

    /**
     * Thread-safe sliding-window counter.
     *
     * Uses a synchronized method so the window-expiry check and counter reset are
     * atomic with respect to each other. The previous non-synchronized implementation
     * had a TOCTOU race: multiple threads could all see the window as expired, all
     * reset the counter to 0, and then all increment — effectively multiplying the
     * effective rate limit by the number of concurrent threads.
     */
    private static final class WindowCounter {
        private int count = 0;
        private long windowStart = Instant.now().toEpochMilli();

        synchronized int incrementAndGet() {
            long now = Instant.now().toEpochMilli();
            if (now - windowStart > WINDOW_MILLIS) {
                count = 0;
                windowStart = now;
            }
            return ++count;
        }

        synchronized boolean isStale(long thresholdMillis) {
            return Instant.now().toEpochMilli() - windowStart > thresholdMillis;
        }
    }
}
