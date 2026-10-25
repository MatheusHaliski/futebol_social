package com.footballai.social.model.entity;

import com.footballai.social.model.enums.RarityTier;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Carta FLAIR: snapshot do Gêmeo de um atleta (tier, PP/RP/MP) na posse de um usuário. Sem valor monetário. */
@Entity
@Table(name = "flair_cards", indexes = @Index(name = "ix_flair_owner", columnList = "owner_id"))
public class FlairCard {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "owner_id", nullable = false, columnDefinition = "BINARY(16)") private UUID ownerId;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Column(name = "athlete_name", nullable = false, length = 80) private String athleteName;
    @Column(name = "athlete_position", length = 16) private String athletePosition;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private RarityTier tier;
    @Column(nullable = false) private int pp;
    @Column(nullable = false) private int rp;
    @Column(nullable = false) private int mp;
    @Column(name = "score_total", nullable = false) private int scoreTotal;
    @Column(nullable = false, length = 9) private String season;
    @Column(nullable = false, length = 12) private String source;
    @Column(name = "acquired_at", nullable = false) private Instant acquiredAt;

    protected FlairCard() { }
    public FlairCard(UUID ownerId, UUID athleteId, String athleteName, String athletePosition, RarityTier tier, int pp, int rp, int mp, int scoreTotal, String season, String source, Instant acquiredAt) {
        this.ownerId = ownerId; this.athleteId = athleteId; this.athleteName = athleteName; this.athletePosition = athletePosition; this.tier = tier;
        this.pp = pp; this.rp = rp; this.mp = mp; this.scoreTotal = scoreTotal; this.season = season; this.source = source; this.acquiredAt = acquiredAt;
    }

    /** Poder = score × multiplicador do tier (× 1,2 se capitão). */
    public int power(boolean captain) {
        return tier.flairMultiplier().multiply(java.math.BigDecimal.valueOf(scoreTotal)).multiply(captain ? new java.math.BigDecimal("1.2") : java.math.BigDecimal.ONE)
                .setScale(0, java.math.RoundingMode.HALF_EVEN).intValue();
    }
    public int axis(String axis) { return switch (axis) { case "PP" -> pp; case "RP" -> rp; case "MP" -> mp; default -> throw new IllegalArgumentException("eixo inválido"); }; }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getAthleteId() { return athleteId; }
    public String getAthleteName() { return athleteName; }
    public void setAthleteName(String athleteName) { this.athleteName = athleteName; }
    public String getAthletePosition() { return athletePosition; }
    public RarityTier getTier() { return tier; }
    public int getPp() { return pp; }
    public int getRp() { return rp; }
    public int getMp() { return mp; }
    public int getScoreTotal() { return scoreTotal; }
    public String getSeason() { return season; }
    public String getSource() { return source; }
    public Instant getAcquiredAt() { return acquiredAt; }
}
