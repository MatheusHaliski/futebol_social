package com.footballai.social.model.entity;

import com.footballai.social.model.enums.BadgeType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Badge "Athletic Persona" (RF4.1) — UNIQUE(athlete_id, badge_type, season). */
@Entity
@Table(name = "athlete_badges", uniqueConstraints = @UniqueConstraint(columnNames = {"athlete_id", "badge_type", "season"}))
public class AthleteBadge {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Enumerated(EnumType.STRING) @Column(name = "badge_type", nullable = false, length = 24) private BadgeType badgeType;
    @Column(nullable = false) private int counter;
    @Column(nullable = false, length = 9) private String season;
    @Column(name = "awarded_at", nullable = false) private Instant awardedAt;
    @Column(name = "awarded_by", columnDefinition = "BINARY(16)") private UUID awardedBy;
    @Column(nullable = false) private boolean active = true;

    protected AthleteBadge() { }
    public AthleteBadge(UUID athleteId, BadgeType badgeType, int counter, String season, Instant awardedAt, UUID awardedBy) {
        this.athleteId = athleteId; this.badgeType = badgeType; this.counter = counter; this.season = season;
        this.awardedAt = awardedAt; this.awardedBy = awardedBy;
    }
    public UUID getId() { return id; }
    public UUID getAthleteId() { return athleteId; }
    public BadgeType getBadgeType() { return badgeType; }
    public int getCounter() { return counter; }
    public void setCounter(int counter) { this.counter = counter; }
    public String getSeason() { return season; }
    public Instant getAwardedAt() { return awardedAt; }
    public UUID getAwardedBy() { return awardedBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
