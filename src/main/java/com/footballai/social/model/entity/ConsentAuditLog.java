package com.footballai.social.model.entity;

import com.footballai.social.model.enums.LgpdArticle;
import com.footballai.social.model.enums.LgpdPreference;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Trilha de consentimento append-only (Art. 6º, X — responsabilização). Retenção: 5 anos. */
@Entity
@Table(name = "consent_audit_log")
public class ConsentAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private LgpdPreference preference;
    @Column(name = "old_value", nullable = false) private boolean oldValue;
    @Column(name = "new_value", nullable = false) private boolean newValue;
    @Enumerated(EnumType.STRING) @Column(name = "lgpd_article", nullable = false, length = 24) private LgpdArticle lgpdArticle;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", length = 255) private String userAgent;
    @Column(name = "changed_at", nullable = false, updatable = false) private Instant changedAt;

    protected ConsentAuditLog() { }
    public ConsentAuditLog(UUID userId, LgpdPreference preference, boolean oldValue, boolean newValue, String ipAddress, String userAgent, Instant changedAt) {
        this.userId = userId; this.preference = preference; this.oldValue = oldValue; this.newValue = newValue;
        this.lgpdArticle = newValue ? preference.article() : LgpdArticle.ART_8_P5; // revogação
        this.ipAddress = ipAddress; this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length()));
        this.changedAt = changedAt;
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LgpdPreference getPreference() { return preference; }
    public boolean isOldValue() { return oldValue; }
    public boolean isNewValue() { return newValue; }
    public LgpdArticle getLgpdArticle() { return lgpdArticle; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getChangedAt() { return changedAt; }
}
