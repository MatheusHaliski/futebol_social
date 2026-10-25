package com.footballai.social.model.entity;

import com.footballai.social.model.enums.BadgeType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Auditoria append-only das concessões de badge (BadgeAwardService é determinístico e auditável). */
@Entity
@Table(name = "badge_audit_log")
public class BadgeAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Enumerated(EnumType.STRING) @Column(name = "badge_type", nullable = false, length = 24) private BadgeType badgeType;
    @Column(nullable = false, length = 16) private String action;
    @Column(length = 160) private String reason;
    @Column(name = "at", nullable = false) private Instant at;

    protected BadgeAuditLog() { }
    public BadgeAuditLog(UUID athleteId, BadgeType badgeType, String action, String reason, Instant at) {
        this.athleteId = athleteId; this.badgeType = badgeType; this.action = action; this.reason = reason; this.at = at;
    }
    public UUID getId() { return id; }
    public UUID getAthleteId() { return athleteId; }
    public BadgeType getBadgeType() { return badgeType; }
    public String getAction() { return action; }
    public String getReason() { return reason; }
    public Instant getAt() { return at; }
}
