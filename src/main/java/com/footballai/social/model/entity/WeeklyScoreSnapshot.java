package com.footballai.social.model.entity;

import com.footballai.social.model.enums.RarityTier;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Snapshot idempotente do cálculo semanal (UNIQUE twin_id + iso_week). */
@Entity
@Table(name = "weekly_score_snapshots", uniqueConstraints = @UniqueConstraint(columnNames = {"twin_id", "iso_week"}))
public class WeeklyScoreSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "twin_id", nullable = false, columnDefinition = "BINARY(16)") private UUID twinId;
    @Column(name = "iso_week", nullable = false, length = 8) private String isoWeek;
    @Column(name = "physical_score", nullable = false, precision = 7, scale = 4) private BigDecimal physicalScore;
    @Column(name = "technical_score", nullable = false, precision = 7, scale = 4) private BigDecimal technicalScore;
    @Column(name = "match_score", nullable = false, precision = 7, scale = 4) private BigDecimal matchScore;
    @Column(name = "badge_bonus", nullable = false, precision = 7, scale = 4) private BigDecimal badgeBonus;
    @Column(name = "score_total", nullable = false) private int scoreTotal;
    @Enumerated(EnumType.STRING) @Column(name = "rarity_tier", nullable = false, length = 12) private RarityTier rarityTier;
    @Column(name = "inputs_hash", nullable = false, length = 64) private String inputsHash;
    @Column(name = "calculated_at", nullable = false) private Instant calculatedAt;

    protected WeeklyScoreSnapshot() { }
    public WeeklyScoreSnapshot(UUID twinId, String isoWeek, BigDecimal physicalScore, BigDecimal technicalScore, BigDecimal matchScore,
                               BigDecimal badgeBonus, int scoreTotal, RarityTier rarityTier, String inputsHash, Instant calculatedAt) {
        this.twinId = twinId; this.isoWeek = isoWeek; this.physicalScore = physicalScore; this.technicalScore = technicalScore;
        this.matchScore = matchScore; this.badgeBonus = badgeBonus; this.scoreTotal = scoreTotal; this.rarityTier = rarityTier;
        this.inputsHash = inputsHash; this.calculatedAt = calculatedAt;
    }
    /** Recalculo idempotente: atualiza o snapshot da mesma semana em vez de apagar/recriar (evita violar o UNIQUE no flush). */
    public void update(BigDecimal physicalScore, BigDecimal technicalScore, BigDecimal matchScore, BigDecimal badgeBonus, int scoreTotal, RarityTier rarityTier, String inputsHash, Instant calculatedAt) {
        this.physicalScore = physicalScore; this.technicalScore = technicalScore; this.matchScore = matchScore; this.badgeBonus = badgeBonus;
        this.scoreTotal = scoreTotal; this.rarityTier = rarityTier; this.inputsHash = inputsHash; this.calculatedAt = calculatedAt;
    }

    public UUID getId() { return id; }
    public UUID getTwinId() { return twinId; }
    public String getIsoWeek() { return isoWeek; }
    public BigDecimal getPhysicalScore() { return physicalScore; }
    public BigDecimal getTechnicalScore() { return technicalScore; }
    public BigDecimal getMatchScore() { return matchScore; }
    public BigDecimal getBadgeBonus() { return badgeBonus; }
    public int getScoreTotal() { return scoreTotal; }
    public RarityTier getRarityTier() { return rarityTier; }
    public String getInputsHash() { return inputsHash; }
    public Instant getCalculatedAt() { return calculatedAt; }
}
