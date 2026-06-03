package com.orderly.api.conversation.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderly.api.conversation.domain.model.ConversationSession;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Manages chatbot sessions in Redis.
 * Key pattern: conversation:{businessId}:{customerPhone}
 * TTL: 30 minutes (auto-reset on write).
 */
@Component
public class ConversationSessionStore {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "conversation:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ConversationSessionStore(RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<ConversationSession> find(String businessId, String customerPhone) {
        Object raw = redisTemplate.opsForValue().get(key(businessId, customerPhone));
        if (raw == null)
            return Optional.empty();
        try {
            ConversationSession session = objectMapper.convertValue(raw, ConversationSession.class);
            return Optional.of(session);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void save(String businessId, String customerPhone, ConversationSession session) {
        session.touch();
        redisTemplate.opsForValue().set(key(businessId, customerPhone), session, SESSION_TTL);
    }

    public void delete(String businessId, String customerPhone) {
        redisTemplate.delete(key(businessId, customerPhone));
    }

    private String key(String businessId, String customerPhone) {
        return KEY_PREFIX + businessId + ":" + customerPhone;
    }
}
