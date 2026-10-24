package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.config.RateLimitFilter;
import com.footballai.social.controller.dto.LgpdDtos.*;
import com.footballai.social.model.enums.LgpdPreference;
import com.footballai.social.service.AuthService;
import com.footballai.social.service.LgpdService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RF5/RF23 — Central de Privacidade: preferências (opt-in), histórico de consentimento, exportação (Art. 18, V),
 * exclusão com graça de 30 dias e restauração (Art. 18, VI), step-up por senha para ações sensíveis.
 */
@RestController
@RequestMapping("/v1/me")
public class LgpdController {
    private final LgpdService lgpd;
    private final AuthService auth;
    public LgpdController(LgpdService lgpd, AuthService auth) { this.lgpd = lgpd; this.auth = auth; }

    @GetMapping("/privacy")
    public PrivacyCenterDTO center(@AuthenticationPrincipal AuthPrincipal p) { return lgpd.center(p); }

    @PutMapping("/privacy/{preference}")
    public PreferenceDTO update(@AuthenticationPrincipal AuthPrincipal p, @PathVariable LgpdPreference preference, @Valid @RequestBody UpdatePreferenceRequest r,
                                @RequestHeader(value = "X-Step-Up-Token", required = false) String stepUp, HttpServletRequest req) {
        return lgpd.updatePreference(p, preference, r.enabled(), stepUp, RateLimitFilter.clientIp(req), req.getHeader("User-Agent"));
    }

    @GetMapping("/consent-history")
    public List<ConsentEntryDTO> history(@AuthenticationPrincipal AuthPrincipal p) { return lgpd.consentHistory(p); }

    @PostMapping("/step-up")
    public StepUpResponse stepUp(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody StepUpRequest r) {
        return lgpd.stepUp(p, auth.checkPassword(p.userId(), r.password()));
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExportDTO export(@AuthenticationPrincipal AuthPrincipal p) { return lgpd.requestExport(p); }

    @GetMapping("/exports")
    public List<ExportDTO> exports(@AuthenticationPrincipal AuthPrincipal p) { return lgpd.listExports(p); }

    @DeleteMapping
    public DeletionDTO delete(@AuthenticationPrincipal AuthPrincipal p, @RequestHeader(value = "X-Step-Up-Token", required = false) String stepUp) {
        return lgpd.requestDeletion(p, stepUp);
    }

    @PostMapping("/restore")
    public DeletionDTO restore(@AuthenticationPrincipal AuthPrincipal p) { return lgpd.cancelDeletion(p); }
}
