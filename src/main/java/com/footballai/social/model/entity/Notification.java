package com.footballai.social.model.entity;

import com.footballai.social.model.enums.NotificationType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Notificação pessoal (sino): quem fez o quê sobre o quê. Apagada na anonimização do destinatário; ator anonimizado vira nulo. */
@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private NotificationType type;
    @Column(name = "actor_id", columnDefinition = "BINARY(16)") private UUID actorId;
    @Column(name = "ref_type", length = 12) private String refType;
    @Column(name = "ref_id", columnDefinition = "BINARY(16)") private UUID refId;
    @Column(nullable = false, length = 120) private String title;
    @Column(length = 280) private String body;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "read_at") private Instant readAt;

    protected Notification() { }
    public Notification(UUID userId, NotificationType type, UUID actorId, String refType, UUID refId, String title, String body, Instant createdAt) {
        this.userId = userId; this.type = type; this.actorId = actorId; this.refType = refType; this.refId = refId;
        this.title = title; this.body = body; this.createdAt = createdAt;
    }

    public void markRead(Instant when) { if (readAt == null) readAt = when; }
    public boolean isUnread() { return readAt == null; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getRefType() { return refType; }
    public UUID getRefId() { return refId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
