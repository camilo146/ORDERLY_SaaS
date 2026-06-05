package com.orderly.api.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Distributed, Redis-backed rate limiter using an atomic Lua script.
 *
 * Advantages over in-memory ConcurrentHashMap:
 * - State survives backend restarts (state stored in Redis with TTL).
 * - Works correctly across multiple backend instances (horizontal scaling).
 * - Window counters are automatically GC'd by Redis TTL — no manual cleanup.
 *
 * The Lua script executes INCR + conditional EXPIRE in a single round-trip,
 * eliminating the TOCTOU race between the two Redis calls.
 *
 * Failure mode: if Redis is unavailable, {@link #increment} returns {@code -1},
 * signalling the caller to activate its in-memory fallback. This "fail open"
 * behaviour prevents Redis downtime from blocking all authenticated traffic.
 */
@Component
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);
    private static final String KEY_PREFIX = "orderly:rl:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> incrScript;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // Atomic Lua: INCR the counter; set TTL only when it is the first request in the window.
        // The conditional prevents overwriting the TTL on subsequent requests within the same window.
        this.incrScript = new DefaultRedisScript<>(
                "local c = redis.call('INCR', KEYS[1]) " +
                "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                "return c",
                Long.class);
    }

    /**
     * Atomically increments the counter for {@code key} within the given window.
     *
     * @param key           unique rate-limit key (e.g., "127.0.0.1::/api/v1/auth/login")
     * @param windowSeconds TTL of the counting window in seconds
     * @return current request count after increment, or {@code -1} if Redis is unavailable
     */
    public long increment(String key, long windowSeconds) {
        try {
            Long count = redisTemplate.execute(
                    incrScript,
                    Collections.singletonList(KEY_PREFIX + key),
                    String.valueOf(windowSeconds));
            return count != null ? count : 1L;
        } catch (Exception e) {
            log.warn("[RATE-LIMIT] Redis unavailable — activating in-memory fallback: {}", e.getMessage());
            return -1L;
        }
    }
}
