package com.footballai.social.service;

import com.footballai.social.config.AppProperties;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Access JWT HS512 (15 min): claims sub, role, sid, jti, iat, exp — nunca e-mail ou nome. */
@Service
public class JwtTokenService {

    public record IssuedToken(String token, String jti, Instant expiresAt) { }

    private final SecretKey key;
    private final AppProperties.Jwt cfg;
    private final Clock clock;

    public JwtTokenService(AppProperties props) { this(props, Clock.systemUTC()); }

    public JwtTokenService(AppProperties props, Clock clock) {
        this.cfg = props.jwt();
        this.clock = clock;
        byte[] raw = cfg.secret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < 64) {
            try { raw = MessageDigest.getInstance("SHA-512").digest(raw); } catch (Exception e) { throw new IllegalStateException(e); }
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    public IssuedToken issueAccessToken(UUID userId, UserRole role, String sessionId) {
        Instant now = clock.instant(), exp = now.plus(cfg.accessTtl());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti).subject(userId.toString()).issuer(cfg.issuer())
                .claim("role", role.name()).claim("sid", sessionId)
                .issuedAt(Date.from(now)).expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS512).compact();
        return new IssuedToken(token, jti, exp);
    }

    /** @throws io.jsonwebtoken.JwtException token inválido/expirado */
    public AuthPrincipal parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).requireIssuer(cfg.issuer()).clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token).getPayload();
        return new AuthPrincipal(UUID.fromString(c.getSubject()), UserRole.valueOf(c.get("role", String.class)), c.get("sid", String.class), c.getId(), c.getExpiration().toInstant());
    }

    public long accessTtlSeconds() { return cfg.accessTtl().toSeconds(); }
}
