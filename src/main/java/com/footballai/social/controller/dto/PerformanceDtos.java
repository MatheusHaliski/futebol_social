package com.footballai.social.controller.dto;

import com.footballai.social.model.enums.EventType;
import com.footballai.social.model.enums.MetricType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Entradas do Rarity Engine: telemetria (30 %), avaliações técnicas (40 %), partidas (30 %) e lesões (status físico). */
public final class PerformanceDtos {
    private PerformanceDtos() { }

    public record TelemetryRequest(@NotNull EventType eventType, @NotNull MetricType metricType, @Min(1) @Max(300) int durationMin, @Min(1) @Max(10) int rpe,
                                   @DecimalMin("0") BigDecimal distanceKm, @DecimalMin("0") BigDecimal maxSpeedKmh, @Min(0) Integer passesOk, @Min(0) Integer passesTotal,
                                   String gpsTrackGeoJson) { }
    public record PlanRequest(@Min(0) @Max(14) int sessionsPlanned, String isoWeek) { }
    public record TrainingWeekDTO(String isoWeek, int sessionsPlanned, int sessionsDone, BigDecimal loadAu, BigDecimal chronicLoadAu, BigDecimal acwr, BigDecimal avgRpe, BigDecimal distanceKm, int readiness, String physicalStatus) { }

    public record EvaluationRequest(@NotNull UUID athleteId, @NotNull @DecimalMin("0") @DecimalMax("10") BigDecimal rating, @NotNull MetricType dimension, LocalDate weekOf, @Size(max = 500) String comment) { }
    public record EvaluationDTO(UUID id, UUID athleteId, String evaluatorRole, BigDecimal rating, BigDecimal weight, String dimension, LocalDate weekOf, boolean countsForRarity) { }

    public record MatchRequest(UUID athleteId, @NotNull LocalDate matchDate, @Size(max = 80) String competition, boolean official, @Min(0) @Max(120) int minutesPlayed,
                               @DecimalMin("0") @DecimalMax("10") BigDecimal rating, @Min(0) int goals, @Min(0) int assists, boolean cleanSheet, @Min(0) int tackles,
                               @Min(0) @Max(2) int yellowCards, @Min(0) @Max(1) int redCards, boolean mvp) { }

    public record InjuryRequest(UUID athleteId, @NotBlank @Size(max = 120) String injuryType, @Size(max = 40) String bodyPart, @NotNull @Pattern(regexp = "LEVE|MODERADA|GRAVE") String severity,
                                LocalDate startedAt, LocalDate expectedReturnDate, @Size(max = 1000) String notes, boolean publishAlert) { }
    public record InjuryDTO(UUID id, UUID athleteId, String injuryType, String bodyPart, String severity, LocalDate startedAt, LocalDate expectedReturnDate, LocalDate clearedAt, boolean active) { }

    public record EvolutionDTO(UUID athleteId, String tier, String crestIcon, int scoreTotal, List<PointDTO> points, List<String> milestones) { }
    public record PointDTO(String isoWeek, int scoreTotal, String tier, BigDecimal physical, BigDecimal technical, BigDecimal match) { }
    public record DnaDTO(UUID athleteId, List<String> axes, List<Integer> values, List<String> styles, String isoWeek) { }
}
