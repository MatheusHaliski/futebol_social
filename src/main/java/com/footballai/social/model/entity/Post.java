package com.footballai.social.model.entity;

import com.footballai.social.model.enums.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Post do feed (RF3). O feed nunca mostra texto corrido: {@code title} obrigatório (5–12 palavras),
 * {@code subtitle} teaser opcional e {@code content} completo apenas em PostView.
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "ix_posts_window_published", columnList = "window_name, published_at"),
        @Index(name = "ix_posts_author", columnList = "author_id")})
@EntityListeners(AuditingEntityListener.class)
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)") private UUID authorId;
    @Enumerated(EnumType.STRING) @Column(name = "author_role", nullable = false, length = 32) private UserRole authorRole;
    @Enumerated(EnumType.STRING) @Column(name = "content_type", nullable = false, length = 24) private PostContentType contentType;
    @Enumerated(EnumType.STRING) @Column(name = "window_name", nullable = false, length = 12) private FeedWindow window;
    @Column(nullable = false, length = 90) private String title;
    @Column(length = 160) private String subtitle;
    @Column(length = 5000) private String content;
    @Column(name = "media_keys", columnDefinition = "JSON") private String mediaKeys;
    @Enumerated(EnumType.STRING) @Column(name = "age_rating", nullable = false, length = 12) private AgeRating ageRating = AgeRating.SAFE;
    @Column(name = "ai_generated", nullable = false) private boolean aiGenerated = false;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private PostStatus status = PostStatus.PUBLISHED;
    @Enumerated(EnumType.STRING) @Column(length = 12) private AlertSeverity severity;
    @Enumerated(EnumType.STRING) @Column(name = "alert_type", length = 16) private AlertType alertType;
    @Column(name = "club_id", columnDefinition = "BINARY(16)") private UUID clubId;
    @Column(name = "state_code", length = 2) private String stateCode;
    @Column(name = "drill_id", columnDefinition = "BINARY(16)") private UUID drillId;
    @Column(name = "published_at", nullable = false) private Instant publishedAt;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "like_count", nullable = false) private int likeCount;
    @Column(name = "comment_count", nullable = false) private int commentCount;
    @Column(name = "share_count", nullable = false) private int shareCount;
    @Column(name = "save_count", nullable = false) private int saveCount;
    @Column(name = "view_count", nullable = false) private long viewCount;
    @Column(name = "trending_score", nullable = false, precision = 8, scale = 3) private BigDecimal trendingScore = BigDecimal.ZERO;
    @Column(name = "trending_delta", nullable = false, precision = 8, scale = 3) private BigDecimal trendingDelta = BigDecimal.ZERO;

    public TrendDirection trend() { return TrendDirection.fromDelta(trendingDelta.doubleValue()); }
    public VslGlyph vslGlyph() { return contentType.vslGlyph(); }
    public boolean hasMedia() { return mediaKeys != null && !mediaKeys.isBlank() && !"[]".equals(mediaKeys.trim()); }

    /** Regras do card: título obrigatório; conteúdo sensível força SENSÍVEL; janela default do tipo. */
    @PrePersist
    void prePersist() {
        if (title == null || title.isBlank()) throw new IllegalStateException("title é obrigatório");
        if (contentType.sensitive()) ageRating = AgeRating.SENSITIVE;
        if (window == null) window = contentType.defaultWindow();
        if (publishedAt == null) publishedAt = Instant.now();
        if (hasMedia()) subtitle = null;
    }

    public void anonymizeContent() {
        this.title = "[conteúdo removido]"; this.subtitle = null; this.content = null; this.mediaKeys = null;
    }

    public void like(int delta) { likeCount = Math.max(0, likeCount + delta); }
    public void save(int delta) { saveCount = Math.max(0, saveCount + delta); }
    public void share() { shareCount++; }
    public void view() { viewCount++; }

    public UUID getId() { return id; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public UserRole getAuthorRole() { return authorRole; }
    public void setAuthorRole(UserRole authorRole) { this.authorRole = authorRole; }
    public PostContentType getContentType() { return contentType; }
    public void setContentType(PostContentType contentType) { this.contentType = contentType; }
    public FeedWindow getWindow() { return window; }
    public void setWindow(FeedWindow window) { this.window = window; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMediaKeys() { return mediaKeys; }
    public void setMediaKeys(String mediaKeys) { this.mediaKeys = mediaKeys; }
    public AgeRating getAgeRating() { return ageRating; }
    public void setAgeRating(AgeRating ageRating) { this.ageRating = ageRating; }
    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public UUID getDrillId() { return drillId; }
    public void setDrillId(UUID drillId) { this.drillId = drillId; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public int getShareCount() { return shareCount; }
    public int getSaveCount() { return saveCount; }
    public long getViewCount() { return viewCount; }
    public BigDecimal getTrendingScore() { return trendingScore; }
    public void setTrendingScore(BigDecimal trendingScore) { this.trendingScore = trendingScore; }
    public BigDecimal getTrendingDelta() { return trendingDelta; }
    public void setTrendingDelta(BigDecimal trendingDelta) { this.trendingDelta = trendingDelta; }
}
