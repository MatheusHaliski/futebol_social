package com.footballai.social.service;

import com.footballai.social.config.AppProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Sessões em Redis (RF1): {@code session:{userId}:{sessionId}} (HASH, TTL 7 d), {@code sessions:{userId}} (SET),
 * {@code rt:{refreshId}} (rotação com detecção de reuso), {@code bl:{jti}} (blacklist até o exp do access),
 * {@code stepup:{userId}} (reautenticação 5 min), {@code login_fail:{emailHash}} (15 min).
 */
@Service
public class RedisSessionService {

    public record Session(String sessionId, UUID userId, String refreshToken, Instant expiresAt) { }

    private static final SecureRandom RNG = new SecureRandom();
    private final StringRedisTemplate redis;
    private final AppProperties props;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RedisSessionService(StringRedisTemplate redis, AppProperties props) { this(redis, props, Clock.systemUTC()); }
    public RedisSessionService(StringRedisTemplate redis, AppProperties props, Clock clock) { this.redis = redis; this.props = props; this.clock = clock; }

    private Duration refreshTtl() { return props.jwt().refreshTtl(); }
    private static String sessionKey(UUID userId, String sid) { return "session:" + userId + ":" + sid; }
    private static String userSetKey(UUID userId) { return "sessions:" + userId; }
    private static String rtKey(String refreshId) { return "rt:" + refreshId; }

    public Session createSession(UUID userId, String ip, String userAgent) {
        String sid = UUID.randomUUID().toString();
        Instant now = clock.instant(), exp = now.plus(refreshTtl());
        String refreshToken = newRefreshToken();
        String refreshId = hash(refreshToken);
        Map<String, String> h = new LinkedHashMap<>();
        h.put("device.userAgent", userAgent == null ? "" : userAgent);
        h.put("device.ip", ip == null ? "" : ip);
        h.put("createdAt", now.toString()); h.put("lastSeenAt", now.toString());
        h.put("refreshTokenId", refreshId); h.put("state", "ACTIVE");
        redis.opsForHash().putAll(sessionKey(userId, sid), h);
        redis.expire(sessionKey(userId, sid), refreshTtl());
        redis.opsForSet().add(userSetKey(userId), sid);
        redis.expire(userSetKey(userId), refreshTtl());
        storeRefresh(refreshId, sid, userId, exp);
        return new Session(sid, userId, refreshToken, exp);
    }

    /** Rotaciona o refresh token. Reuso de token já rotacionado/revogado = sessão comprometida → revoga tudo da sessão. */
    public Optional<Session> rotateRefresh(String refreshToken) {
        String refreshId = hash(refreshToken);
        Map<Object, Object> rt = redis.opsForHash().entries(rtKey(refreshId));
        if (rt.isEmpty()) return Optional.empty();
        UUID userId = UUID.fromString((String) rt.get("userId"));
        String sid = (String) rt.get("sessionId");
        if ("true".equals(rt.get("revoked")) || rt.get("rotatedTo") != null) {
            revokeSession(userId, sid); // detecção de reuso
            return Optional.empty();
        }
        if (!isSessionActive(userId, sid)) return Optional.empty();
        Instant now = clock.instant(), exp = now.plus(refreshTtl());
        String next = newRefreshToken(), nextId = hash(next);
        redis.opsForHash().put(rtKey(refreshId), "rotatedTo", nextId);
        redis.expire(rtKey(refreshId), refreshTtl());
        storeRefresh(nextId, sid, userId, exp);
        redis.opsForHash().put(sessionKey(userId, sid), "refreshTokenId", nextId);
        redis.opsForHash().put(sessionKey(userId, sid), "lastSeenAt", now.toString());
        redis.expire(sessionKey(userId, sid), refreshTtl());
        redis.expire(userSetKey(userId), refreshTtl()); // o índice acompanha a renovação: logout-all e exclusão LGPD continuam alcançando a sessão
        return Optional.of(new Session(sid, userId, next, exp));
    }

    private void storeRefresh(String refreshId, String sid, UUID userId, Instant exp) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("sessionId", sid); h.put("userId", userId.toString());
        h.put("issuedAt", clock.instant().toString()); h.put("expiresAt", exp.toString()); h.put("revoked", "false");
        redis.opsForHash().putAll(rtKey(refreshId), h);
        redis.expire(rtKey(refreshId), refreshTtl());
    }

    public boolean isSessionActive(UUID userId, String sid) {
        Object state = redis.opsForHash().get(sessionKey(userId, sid), "state");
        return "ACTIVE".equals(state);
    }

    public void touch(UUID userId, String sid) {
        try { redis.opsForHash().put(sessionKey(userId, sid), "lastSeenAt", clock.instant().toString()); } catch (RuntimeException ignored) { }
    }

    public void revokeSession(UUID userId, String sid) {
        Object rtId = redis.opsForHash().get(sessionKey(userId, sid), "refreshTokenId");
        if (rtId != null) redis.opsForHash().put(rtKey((String) rtId), "revoked", "true");
        redis.delete(sessionKey(userId, sid));
        redis.opsForSet().remove(userSetKey(userId), sid);
    }

    public void revokeAllSessions(UUID userId) {
        Set<String> sids = redis.opsForSet().members(userSetKey(userId));
        if (sids != null) sids.forEach(sid -> revokeSession(userId, sid));
        redis.delete(userSetKey(userId));
        redis.delete("stepup:" + userId);
    }

    public List<Map<String, String>> listSessions(UUID userId) {
        Set<String> sids = redis.opsForSet().members(userSetKey(userId));
        List<Map<String, String>> out = new ArrayList<>();
        if (sids == null) return out;
        for (String sid : sids) {
            Map<Object, Object> h = redis.opsForHash().entries(sessionKey(userId, sid));
            if (h.isEmpty()) continue;
            Map<String, String> m = new LinkedHashMap<>();
            m.put("sessionId", sid);
            h.forEach((k, v) -> { if (!"refreshTokenId".equals(k)) m.put(String.valueOf(k), String.valueOf(v)); });
            out.add(m);
        }
        return out;
    }

    public void blacklist(String jti, Instant accessExpiresAt) {
        Duration ttl = Duration.between(clock.instant(), accessExpiresAt);
        if (ttl.isNegative() || ttl.isZero()) return;
        redis.opsForValue().set("bl:" + jti, "1", ttl);
    }

    public boolean isBlacklisted(String jti) { return Boolean.TRUE.equals(redis.hasKey("bl:" + jti)); }

    public String issueStepUpToken(UUID userId) {
        String token = newRefreshToken();
        redis.opsForValue().set("stepup:" + userId, hash(token), Duration.ofMinutes(5));
        return token;
    }

    public boolean verifyStepUpToken(UUID userId, String token) {
        if (token == null) return false;
        String stored = redis.opsForValue().get("stepup:" + userId);
        return stored != null && MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), hash(token).getBytes(StandardCharsets.UTF_8));
    }

    public long recordLoginFailure(String emailHash) {
        Long n = redis.opsForValue().increment("login_fail:" + emailHash);
        if (n != null && n == 1) redis.expire("login_fail:" + emailHash, Duration.ofMinutes(15));
        return n == null ? 0 : n;
    }

    public long loginFailures(String emailHash) {
        String v = redis.opsForValue().get("login_fail:" + emailHash);
        return v == null ? 0 : Long.parseLong(v);
    }

    public void clearLoginFailures(String emailHash) { redis.delete("login_fail:" + emailHash); }

    private static String newRefreshToken() {
        byte[] b = new byte[32]; RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String hash(String value) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
