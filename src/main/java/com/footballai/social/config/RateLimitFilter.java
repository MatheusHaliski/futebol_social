package com.footballai.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballai.social.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Rate limit por IP e por usuário (janela fixa de 60 s em Redis). Login/registro têm cota própria.
 * Resposta 429 com {@code Retry-After}. Estáticos e health não contam.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService limiter;
    private final AppProperties props;
    private final ObjectMapper mapper;

    public RateLimitFilter(RateLimitService limiter, AppProperties props, ObjectMapper mapper) {
        this.limiter = limiter; this.props = props; this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return !p.startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String ip = clientIp(req);
        String path = req.getRequestURI();
        RateLimitService.Decision d;
        if (path.startsWith("/v1/auth/login") || path.startsWith("/v1/auth/register") || path.startsWith("/v1/me/step-up")) {
            d = limiter.check("bucket:login:" + ip, props.rateLimit().loginPerMinute());
        } else {
            d = limiter.check("bucket:ip:" + ip, props.rateLimit().ipPerMinute());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (d.allowed() && auth != null && auth.getPrincipal() instanceof AuthPrincipal p) {
                d = limiter.check("bucket:user:" + p.userId(), props.rateLimit().userPerMinute());
            }
        }
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, d.remaining())));
        if (!d.allowed()) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(d.retryAfterSeconds()));
            res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            mapper.writeValue(res.getOutputStream(), Map.of("status", 429, "code", "RATE_LIMITED", "detail", "Muitas requisições — tente novamente em " + d.retryAfterSeconds() + "s"));
            return;
        }
        chain.doFilter(req, res);
    }

    public static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
