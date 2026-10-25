package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.PerformanceDtos.*;
import com.footballai.social.model.entity.MatchPerformance;
import com.footballai.social.service.PerformanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Entradas do Rarity Engine e do status físico: telemetria, plano semanal, avaliações, partidas e lesões. */
@RestController
@RequestMapping("/v1")
public class PerformanceController {
    private final PerformanceService perf;
    public PerformanceController(PerformanceService perf) { this.perf = perf; }

    @PostMapping("/telemetry")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingWeekDTO telemetry(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody TelemetryRequest r) { return perf.recordTelemetry(p, r); }

    @PutMapping("/telemetry/plan")
    public TrainingWeekDTO plan(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody PlanRequest r) { return perf.setPlan(p, r); }

    @GetMapping("/twins/{athleteId}/load")
    public List<TrainingWeekDTO> load(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID athleteId) { return perf.load(p, athleteId); }

    @PostMapping("/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationDTO evaluate(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody EvaluationRequest r) { return perf.evaluate(p, r); }

    @PostMapping("/matches/performances")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> match(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody MatchRequest r) {
        MatchPerformance m = perf.recordMatch(p, r);
        return Map.of("id", m.getId(), "athleteId", m.getAthleteId(), "matchDate", m.getMatchDate(), "official", m.isOfficial(), "season", m.getSeason());
    }

    @PostMapping("/injuries")
    @ResponseStatus(HttpStatus.CREATED)
    public InjuryDTO injury(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody InjuryRequest r) { return perf.reportInjury(p, r); }

    @PostMapping("/injuries/{id}/clear")
    public InjuryDTO clear(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return perf.clearInjury(p, id); }

    @GetMapping("/athletes/{id}/injuries")
    public List<InjuryDTO> injuries(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return perf.injuries(p, id); }
}
