package com.footballai.social.model.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;

import java.io.Serializable;
import java.util.UUID;

/** Fan-out de posts por usuário e janela (RF3). Partição (user_id, feed_window); clustering published_at DESC. */
@Table("feed_entries")
public class FeedEntry {

    @PrimaryKeyClass
    public static class Key implements Serializable {
        @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0) private UUID userId;
        @PrimaryKeyColumn(name = "feed_window", type = PrimaryKeyType.PARTITIONED, ordinal = 1) private String feedWindow;
        @PrimaryKeyColumn(name = "published_at", type = PrimaryKeyType.CLUSTERED, ordinal = 2, ordering = Ordering.DESCENDING) private UUID publishedAt;

        public Key() { }
        public Key(UUID userId, String feedWindow, UUID publishedAt) { this.userId = userId; this.feedWindow = feedWindow; this.publishedAt = publishedAt; }
        public UUID getUserId() { return userId; }
        public String getFeedWindow() { return feedWindow; }
        public UUID getPublishedAt() { return publishedAt; }
    }

    @PrimaryKey private Key key;
    @Column("post_id") private UUID postId;
    @Column("author_id") private UUID authorId;
    @Column("author_role") private String authorRole;
    @Column("content_type") private String contentType;
    @Column("severity") private String severity;
    @Column("alert_type") private String alertType;
    @Column("source_role") private String sourceRole;
    @Column("title") private String title;
    @Column("subtitle") private String subtitle;
    @Column("age_rating") private String ageRating;
    @Column("trending_score") private Double trendingScore;

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getSourceRole() { return sourceRole; }
    public void setSourceRole(String sourceRole) { this.sourceRole = sourceRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public Double getTrendingScore() { return trendingScore; }
    public void setTrendingScore(Double trendingScore) { this.trendingScore = trendingScore; }
}
