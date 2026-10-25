package com.footballai.social.model.entity;

import com.footballai.social.model.enums.FollowTargetType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Seguir (RF3): {@code followerId → targetId}. Conexão = seguir mútuo (visibilidade {@code CONEXOES}). */
@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "target_id"}))
public class Follow {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "follower_id", nullable = false, columnDefinition = "BINARY(16)") private UUID followerId;
    @Column(name = "target_id", nullable = false, columnDefinition = "BINARY(16)") private UUID targetId;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 8) private FollowTargetType targetType;
    @Column(nullable = false) private Instant since;

    protected Follow() { }
    public Follow(UUID followerId, UUID targetId, FollowTargetType targetType, Instant since) {
        this.followerId = followerId; this.targetId = targetId; this.targetType = targetType; this.since = since;
    }

    public UUID getId() { return id; }
    public UUID getFollowerId() { return followerId; }
    public UUID getTargetId() { return targetId; }
    public FollowTargetType getTargetType() { return targetType; }
    public Instant getSince() { return since; }
}
