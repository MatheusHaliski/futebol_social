package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Livro de pontos (append-only). Pontos não são compráveis nem conversíveis em dinheiro. */
@Entity
@Table(name = "points_ledger", indexes = @Index(name = "ix_points_user", columnList = "user_id, at"))
public class PointsLedger {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Column(nullable = false) private int delta;
    @Column(nullable = false, length = 24) private String reason;
    @Column(name = "ref_id", columnDefinition = "BINARY(16)") private UUID refId;
    @Column(name = "at", nullable = false) private Instant at;

    protected PointsLedger() { }
    public PointsLedger(UUID userId, int delta, String reason, UUID refId, Instant at) { this.userId = userId; this.delta = delta; this.reason = reason; this.refId = refId; this.at = at; }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public int getDelta() { return delta; }
    public String getReason() { return reason; }
    public UUID getRefId() { return refId; }
    public Instant getAt() { return at; }
}
