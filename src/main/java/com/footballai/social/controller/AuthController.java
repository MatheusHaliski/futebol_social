package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.config.RateLimitFilter;
import com.footballai.social.controller.dto.AuthDtos.*;
import com.footballai.social.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF1/RF2 — cadastro polimórfico, login, refresh com rotação, logout e termos Velion. */
@RestController
@RequestMapping("/v1")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @GetMapping("/terms/current")
    public TermsDTO terms() { return auth.currentTerms(); }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r, HttpServletRequest req) {
        return auth.register(r, RateLimitFilter.clientIp(req), req.getHeader("User-Agent"));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        return auth.login(r, RateLimitFilter.clientIp(req), req.getHeader("User-Agent"));
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest r) { return auth.refresh(r.refreshToken()); }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthPrincipal p) { auth.logout(p); return ResponseEntity.noContent().build(); }

    @PostMapping("/auth/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthPrincipal p) { auth.logoutAll(p); return ResponseEntity.noContent().build(); }

    @PostMapping("/auth/terms/accept")
    public ResponseEntity<Void> acceptTerms(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody AcceptTermsRequest r, HttpServletRequest req) {
        auth.acceptTerms(p.userId(), r.termsVersion(), RateLimitFilter.clientIp(req), req.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }
}
