package com.footballai.social.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Janela fixa de 60 s: INCR + EXPIRE. Se o Redis cair, falha aberto (registra aviso) para não derrubar a API. */
@Service
public class RateLimitService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    public record Decision(boolean allowed, long remaining, long retryAfterSeconds) { }

    private final StringRedisTemplate redis;
    public RateLimitService(StringRedisTemplate redis) { this.redis = redis; }

    public Decision check(String key, int limitPerMinute) {
        try {
            Long n = redis.opsForValue().increment(key);
            if (n == null) return new Decision(true, limitPerMinute, 0);
            if (n == 1) redis.expire(key, Duration.ofSeconds(60));
            if (n > limitPerMinute) {
                Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
                return new Decision(false, 0, ttl == null || ttl < 0 ? 60 : ttl);
            }
            return new Decision(true, limitPerMinute - n, 0);
        } catch (DataAccessException e) {
            log.warn("Rate limit indisponível (Redis): {} — falhando aberto", e.getMessage());
            return new Decision(true, limitPerMinute, 0);
        }
    }
}
