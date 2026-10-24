package com.footballai.social.model.entity;

import com.footballai.social.model.enums.MetricType;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Avaliação técnica (40 % do Rarity). Peso: comissão 1.00 · scout homologado 0.80 · comunidade 0.50. */
@Entity
@Table(name = "technical_evaluations")
public class TechnicalEvaluation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Column(name = "evaluator_id", nullable = false, columnDefinition = "BINARY(16)") private UUID evaluatorId;
    @Enumerated(EnumType.STRING) @Column(name = "evaluator_role", nullable = false, length = 32) private UserRole evaluatorRole;
    @Column(nullable = false, precision = 4, scale = 2) private BigDecimal rating;
    @Column(nullable = false, precision = 3, scale = 2) private BigDecimal weight;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private MetricType dimension;
    @Column(name = "week_of", nullable = false) private LocalDate weekOf;
    @Column(length = 500) private String comment;
    @Column(name = "counts_for_rarity", nullable = false) private boolean countsForRarity = true;

    public UUID getId() { return id; }
    public UUID getAthleteId() { return athleteId; }
    public void setAthleteId(UUID athleteId) { this.athleteId = athleteId; }
    public UUID getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(UUID evaluatorId) { this.evaluatorId = evaluatorId; }
    public UserRole getEvaluatorRole() { return evaluatorRole; }
    public void setEvaluatorRole(UserRole evaluatorRole) { this.evaluatorRole = evaluatorRole; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public MetricType getDimension() { return dimension; }
    public void setDimension(MetricType dimension) { this.dimension = dimension; }
    public LocalDate getWeekOf() { return weekOf; }
    public void setWeekOf(LocalDate weekOf) { this.weekOf = weekOf; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isCountsForRarity() { return countsForRarity; }
    public void setCountsForRarity(boolean countsForRarity) { this.countsForRarity = countsForRarity; }
}
