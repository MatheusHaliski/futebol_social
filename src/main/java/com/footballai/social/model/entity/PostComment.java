package com.footballai.social.model.entity;

import com.footballai.social.model.enums.CommentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Comentário em card (RF3): um nível de resposta ({@code parentId}); remoção é lógica e zera o texto. */
@Entity
@Table(name = "post_comments")
public class PostComment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "post_id", nullable = false, columnDefinition = "BINARY(16)") private UUID postId;
    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)") private UUID authorId;
    @Column(name = "parent_id", columnDefinition = "BINARY(16)") private UUID parentId;
    @Column(nullable = false, length = 500) private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private CommentStatus status = CommentStatus.VISIBLE;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "removed_at") private Instant removedAt;

    protected PostComment() { }
    public PostComment(UUID postId, UUID authorId, UUID parentId, String body, Instant createdAt) {
        this.postId = postId; this.authorId = authorId; this.parentId = parentId; this.body = body; this.createdAt = createdAt;
    }

    /** Remoção lógica: o texto some do banco; a linha fica para manter a árvore de respostas e a auditoria. */
    public void remove(Instant when) { this.status = CommentStatus.REMOVED; this.body = ""; this.removedAt = when; }

    public UUID getId() { return id; }
    public UUID getPostId() { return postId; }
    public UUID getAuthorId() { return authorId; }
    public UUID getParentId() { return parentId; }
    public String getBody() { return body; }
    public CommentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRemovedAt() { return removedAt; }
}
