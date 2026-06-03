package com.orderly.api.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limits inbound webhook requests to prevent DoS and order-flooding attacks.
 *
 * Limits: 60 requests per minute per source IP.
 * The Evolution API sends at most a few events per second per instance in normal operation.
 * 60 req/min is generous for legitimate traffic but stops automated abuse.
 */
@Component
public class WebhookRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebhookRateLimitInterceptor.class);

    private static final int MAX_PER_MINUTE = 60;
    private static final long WINDOW_MILLIS = 60_000L;
    // Cleanup counters older than 10 minutes to prevent unbounded memory growth.
    private static final long STALE_THRESHOLD_MILLIS = 10 * WINDOW_MILLIS;
    private static final long CLEANUP_INTERVAL_MILLIS = 5 * WINDOW_MILLIS;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private volatile long lastCleanup = Instant.now().toEpochMilli();

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        evictStaleCounters();

        String ip = request.getRemoteAddr();
        WindowCounter counter = counters.computeIfAbsent(ip, k -> new WindowCounter());
        int current = counter.incrementAndGet();

        if (current > MAX_PER_MINUTE) {
            log.warn("[WEBHOOK-RATE-LIMIT] IP={} requests={} path={}", ip, current, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many webhook requests.\"}");
            return false;
        }

        return true;
    }

    private void evictStaleCounters() {
        long now = Instant.now().toEpochMilli();
        if (now - lastCleanup > CLEANUP_INTERVAL_MILLIS) {
            counters.entrySet().removeIf(e -> e.getValue().isStale(STALE_THRESHOLD_MILLIS));
            lastCleanup = now;
        }
    }

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
