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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rate limit por IP e por usuário (janela fixa de 60 s em Redis). Login/registro têm cota própria.
 * Resposta 429 com {@code Retry-After}. Estáticos e health não contam.
 * O IP do cliente é o endereço da conexão; {@code X-Forwarded-For} só vale quando a conexão vem de um proxy listado em
 * {@code app.rate-limit.trusted-proxies} — senão qualquer cliente trocaria de IP (e de cota) a cada requisição.
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
        String ip = resolveClientIp(req, trusted());
        req.setAttribute(CLIENT_IP_ATTR, ip);
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

    static final String CLIENT_IP_ATTR = "fas.clientIp";

    private Set<String> trusted() {
        List<String> list = props.rateLimit().trustedProxies();
        return list == null ? Set.of() : list.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    /** IP já resolvido pelo filtro (auditoria de aceite e de consentimento usa o mesmo valor que o rate limit). */
    public static String clientIp(HttpServletRequest req) {
        Object resolved = req.getAttribute(CLIENT_IP_ATTR);
        return resolved instanceof String s ? s : req.getRemoteAddr();
    }

    /** Conexão de proxy confiável: o cliente é o salto mais à direita do {@code X-Forwarded-For} que não é proxy confiável. */
    static String resolveClientIp(HttpServletRequest req, Set<String> trustedProxies) {
        String remote = req.getRemoteAddr();
        if (!trustedProxies.contains(remote)) return remote;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) return remote;
        String[] hops = xff.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!hop.isEmpty() && !trustedProxies.contains(hop)) return hop;
        }
        return remote;
    }
}
