package com.footballai.social.service;

import com.footballai.social.TestProps;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.model.enums.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RF1: access JWT HS512 de 15 min com sub/role/sid/jti, sem PII. */
class JwtTokenServiceTest {
    private final Instant t0 = Instant.parse("2026-10-24T12:00:00Z");

    @Test
    void roundTripCarriesOnlyNonPiiClaims() {
        JwtTokenService svc = new JwtTokenService(TestProps.props(), Clock.fixed(t0, ZoneOffset.UTC));
        UUID user = UUID.randomUUID();
        var issued = svc.issueAccessToken(user, UserRole.ATLETA, "sid-1");
        AuthPrincipal p = svc.parse(issued.token());
        assertThat(p.userId()).isEqualTo(user);
        assertThat(p.role()).isEqualTo(UserRole.ATLETA);
        assertThat(p.sessionId()).isEqualTo("sid-1");
        assertThat(p.jti()).isEqualTo(issued.jti());
        assertThat(p.expiresAt()).isEqualTo(t0.plus(Duration.ofMinutes(15)));
        assertThat(issued.token()).doesNotContain("@");
    }

    @Test
    void expiredAndTamperedTokensAreRejected() {
        JwtTokenService issuer = new JwtTokenService(TestProps.props(), Clock.fixed(t0, ZoneOffset.UTC));
        String token = issuer.issueAccessToken(UUID.randomUUID(), UserRole.SCOUT, "sid").token();
        JwtTokenService later = new JwtTokenService(TestProps.props(), Clock.fixed(t0.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.parse(token)).isInstanceOf(ExpiredJwtException.class);
        assertThatThrownBy(() -> issuer.parse(token.substring(0, token.length() - 3) + "abc")).isInstanceOf(JwtException.class);
    }
}
