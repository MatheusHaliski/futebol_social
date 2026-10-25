package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.AthleteCardDTO;
import com.footballai.social.controller.dto.PerformanceDtos.DnaDTO;
import com.footballai.social.controller.dto.PerformanceDtos.EvolutionDTO;
import com.footballai.social.model.entity.DigitalTwin;
import com.footballai.social.repository.DigitalTwinRepository;
import com.footballai.social.service.ApiException;
import com.footballai.social.service.TwinService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Gêmeo Digital: resumo, evolução, DNA de jogo e comparador (sub-abas da janela GEMEO). */
@RestController
@RequestMapping("/v1/twins")
public class TwinController {
    private final TwinService twinService;
    private final DigitalTwinRepository twins;
    public TwinController(TwinService twinService, DigitalTwinRepository twins) { this.twinService = twinService; this.twins = twins; }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AuthPrincipal p) {
        DigitalTwin t = twins.findByUserId(p.userId()).orElseThrow(() -> ApiException.notFound("Gêmeo"));
        return Map.of("id", t.getId(), "status", t.getStatus(), "physicalStatus", t.getPhysicalStatus(), "scoreTotal", t.getScoreTotal(),
                "tier", t.getRarityTier(), "crestIcon", t.getRarityTier().crestIcon(), "rewardTier", t.getRewardTier(), "lastCalculatedWeek", String.valueOf(t.getLastCalculatedWeek()));
    }

    @GetMapping("/{athleteId}/evolution")
    public EvolutionDTO evolution(@PathVariable UUID athleteId) { return twinService.evolution(athleteId); }

    @GetMapping("/{athleteId}/dna")
    public DnaDTO dna(@PathVariable UUID athleteId) { return twinService.dna(athleteId); }

    @GetMapping("/compare")
    public List<AthleteCardDTO> compare(@AuthenticationPrincipal AuthPrincipal p, @RequestParam String ids) {
        return twinService.compare(p, Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(UUID::fromString).toList());
    }
}
