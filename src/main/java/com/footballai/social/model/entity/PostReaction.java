package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Curtir / Salvar de um usuário num post (idempotente por UNIQUE post_id + user_id + kind). */
@Entity
@Table(name = "post_reactions", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id", "kind"}))
public class PostReaction {
    public enum Kind { LIKE, SAVE }

    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "post_id", nullable = false, columnDefinition = "BINARY(16)") private UUID postId;
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Kind kind;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PostReaction() { }
    public PostReaction(UUID postId, UUID userId, Kind kind, Instant createdAt) { this.postId = postId; this.userId = userId; this.kind = kind; this.createdAt = createdAt; }
    public UUID getId() { return id; }
    public UUID getPostId() { return postId; }
    public UUID getUserId() { return userId; }
    public Kind getKind() { return kind; }
    public Instant getCreatedAt() { return createdAt; }
}
