package com.asg.aiusecase.cache;

import com.asg.aiusecase.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private static final String VERSION_KEY = "cache-version";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("Redis cache read failed key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("Redis cache write failed key={}: {}", key, e.getMessage());
        }
    }

    public String finalKey(String hash) {
        return prefix("final") + ":" + version() + ":" + hash;
    }

    public String vectorKey(String hash) {
        return prefix("vector") + ":" + version() + ":" + hash;
    }

    public String semanticKey(String hash) {
        return prefix("semantic") + ":" + version() + ":" + hash;
    }

    public String llmKey(String hash) {
        return prefix("llm") + ":" + version() + ":" + hash;
    }

    public void invalidateSemanticState() {
        try {
            redisTemplate.opsForValue().increment(prefix(VERSION_KEY));
        } catch (Exception e) {
            log.warn("Redis cache invalidation failed: {}", e.getMessage());
        }
    }

    private long version() {
        String key = prefix(VERSION_KEY);
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null) {
            redisTemplate.opsForValue().setIfAbsent(key, "1");
            return 1;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            redisTemplate.opsForValue().set(key, "1");
            return 1;
        }
    }

    private String prefix(String segment) {
        return properties.getCache().getKeyPrefix() + ":" + segment;
    }
}
