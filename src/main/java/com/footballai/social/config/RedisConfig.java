package com.footballai.social.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis: sessões ({@code session:*}, {@code rt:*}), blacklist de jti ({@code bl:*}), buckets de rate limit
 * ({@code bucket:*}), step-up ({@code stepup:*}) e cache de páginas de feed ({@code feed:*}).
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        StringRedisSerializer keys = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer values = new GenericJackson2JsonRedisSerializer();
        t.setKeySerializer(keys); t.setHashKeySerializer(keys);
        t.setValueSerializer(values); t.setHashValueSerializer(values);
        t.afterPropertiesSet();
        return t;
    }
}
