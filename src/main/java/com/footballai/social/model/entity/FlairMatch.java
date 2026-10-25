package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Duelo 1×1 em 3 lances: o desafiante escolhe o eixo (PP/RP/MP) de cada lance; Snap dobra os pontos em disputa. */
@Entity
@Table(name = "flair_matches", indexes = @Index(name = "ix_flair_match_user", columnList = "challenger_id, created_at"))
public class FlairMatch {
    public enum Status { IN_PROGRESS, FINISHED }

    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "challenger_id", nullable = false, columnDefinition = "BINARY(16)") private UUID challengerId;
    /** nulo = oponente controlado pelo sistema. */
    @Column(name = "opponent_id", columnDefinition = "BINARY(16)") private UUID opponentId;
    @Column(name = "opponent_label", nullable = false, length = 80) private String opponentLabel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private Status status = Status.IN_PROGRESS;
    @Column(name = "rounds_json", nullable = false, columnDefinition = "JSON") private String roundsJson = "[]";
    @Column(name = "rounds_played", nullable = false) private int roundsPlayed = 0;
    @Column(name = "challenger_wins", nullable = false) private int challengerWins = 0;
    @Column(name = "opponent_wins", nullable = false) private int opponentWins = 0;
    @Column(name = "snap_used", nullable = false) private boolean snapUsed = false;
    @Column(name = "points_awarded", nullable = false) private int pointsAwarded = 0;
    @Column(name = "deck_json", nullable = false, columnDefinition = "JSON") private String deckJson = "{}";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "finished_at") private Instant finishedAt;

    protected FlairMatch() { }
    public FlairMatch(UUID challengerId, UUID opponentId, String opponentLabel, String deckJson, Instant createdAt) {
        this.challengerId = challengerId; this.opponentId = opponentId; this.opponentLabel = opponentLabel; this.deckJson = deckJson; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getChallengerId() { return challengerId; }
    public UUID getOpponentId() { return opponentId; }
    public String getOpponentLabel() { return opponentLabel; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getRoundsJson() { return roundsJson; }
    public void setRoundsJson(String roundsJson) { this.roundsJson = roundsJson; }
    public int getRoundsPlayed() { return roundsPlayed; }
    public void setRoundsPlayed(int roundsPlayed) { this.roundsPlayed = roundsPlayed; }
    public int getChallengerWins() { return challengerWins; }
    public void setChallengerWins(int challengerWins) { this.challengerWins = challengerWins; }
    public int getOpponentWins() { return opponentWins; }
    public void setOpponentWins(int opponentWins) { this.opponentWins = opponentWins; }
    public boolean isSnapUsed() { return snapUsed; }
    public void setSnapUsed(boolean snapUsed) { this.snapUsed = snapUsed; }
    public int getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public String getDeckJson() { return deckJson; }
    public void setDeckJson(String deckJson) { this.deckJson = deckJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
