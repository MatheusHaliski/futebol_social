package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.VitrineDtos.PitchDTO;
import com.footballai.social.controller.dto.VitrineDtos.VitrineDTO;
import com.footballai.social.service.VitrineService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Aba Vitrine do Jogador: painel dinâmico (a SPA consulta a cada {@code refreshSeconds} e redesenha quando {@code revision} muda)
 * e o Copilot "Por que contratar este jogador?". Mesma visibilidade do perfil — senão 404.
 */
@RestController
@RequestMapping("/v1/athletes/{id}/vitrine")
public class VitrineController {
    private final VitrineService vitrine;
    public VitrineController(VitrineService vitrine) { this.vitrine = vitrine; }

    @GetMapping
    public VitrineDTO panel(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return vitrine.vitrine(p, id); }

    /** Copilot: exige o consentimento de IA do atleta (titular dos dados) e não analisa menores de 18 — 403 com código estável. */
    @PostMapping("/copilot")
    public PitchDTO copilot(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return vitrine.pitch(p, id); }
}
