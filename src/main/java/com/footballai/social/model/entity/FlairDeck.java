package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Escalação de 3 cartas com um capitão (índice 0–2). */
@Entity
@Table(name = "flair_decks")
public class FlairDeck {
    @Id @Column(name = "owner_id", columnDefinition = "BINARY(16)") private UUID ownerId;
    @Column(name = "card1_id", columnDefinition = "BINARY(16)") private UUID card1;
    @Column(name = "card2_id", columnDefinition = "BINARY(16)") private UUID card2;
    @Column(name = "card3_id", columnDefinition = "BINARY(16)") private UUID card3;
    @Column(name = "captain_index", nullable = false) private int captainIndex = 0;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected FlairDeck() { }
    public FlairDeck(UUID ownerId) { this.ownerId = ownerId; this.updatedAt = Instant.now(); }
    public void set(UUID c1, UUID c2, UUID c3, int captainIndex, Instant now) { this.card1 = c1; this.card2 = c2; this.card3 = c3; this.captainIndex = Math.max(0, Math.min(2, captainIndex)); this.updatedAt = now; }
    public UUID[] cards() { return new UUID[]{card1, card2, card3}; }
    public boolean isComplete() { return card1 != null && card2 != null && card3 != null; }
    public UUID getOwnerId() { return ownerId; }
    public int getCaptainIndex() { return captainIndex; }
    public Instant getUpdatedAt() { return updatedAt; }
}
