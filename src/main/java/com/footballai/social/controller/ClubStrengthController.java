package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ClubStrengthDtos.ClubStrengthDTO;
import com.footballai.social.controller.dto.ClubStrengthDtos.SimulationDTO;
import com.footballai.social.service.ClubStrengthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Força do clube (score global e por setor) e "Simular contratação" na Vitrine do Jogador. */
@RestController
@RequestMapping("/v1")
public class ClubStrengthController {
    private final ClubStrengthService strength;
    public ClubStrengthController(ClubStrengthService strength) { this.strength = strength; }

    @GetMapping("/clubs/{id}/strength")
    public ClubStrengthDTO strength(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return strength.strength(p, id); }

    @GetMapping("/athletes/{id}/vitrine/simulate")
    public SimulationDTO simulate(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @RequestParam UUID clubId) { return strength.simulate(p, id, clubId); }
}
