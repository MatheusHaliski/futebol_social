package com.footballai.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballai.social.service.JwtTokenService;
import com.footballai.social.service.RedisSessionService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Valida o access JWT, a blacklist de jti e a sessão Redis (invalidação em tempo real). */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwt;
    private final RedisSessionService sessions;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtTokenService jwt, RedisSessionService sessions, ObjectMapper mapper) {
        this.jwt = jwt; this.sessions = sessions; this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(req, res); return; }
        try {
            AuthPrincipal p = jwt.parse(header.substring(7).trim());
            if (sessions.isBlacklisted(p.jti()) || !sessions.isSessionActive(p.userId(), p.sessionId())) {
                reject(res, "SESSION_REVOKED", "Sessão encerrada — faça login novamente"); return;
            }
            var auth = new UsernamePasswordAuthenticationToken(p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role().name())));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
            sessions.touch(p.userId(), p.sessionId());
        } catch (JwtException | IllegalArgumentException e) {
            reject(res, "INVALID_TOKEN", "Access token inválido ou expirado"); return;
        }
        chain.doFilter(req, res);
    }

    private void reject(HttpServletResponse res, String code, String detail) throws IOException {
        SecurityContextHolder.clearContext();
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), Map.of("status", 401, "code", code, "detail", detail));
    }
}
