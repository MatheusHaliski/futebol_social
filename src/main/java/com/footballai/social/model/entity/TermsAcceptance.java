package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Aceite imutável dos termos (RF2) — versão, data, IP e user-agent. */
@Entity
@Table(name = "terms_acceptances")
public class TermsAcceptance {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Column(name = "terms_version", nullable = false, length = 20) private String termsVersion;
    @Column(name = "accepted_at", nullable = false, updatable = false) private Instant acceptedAt;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", length = 255) private String userAgent;

    protected TermsAcceptance() { }
    public TermsAcceptance(UUID userId, String termsVersion, Instant acceptedAt, String ipAddress, String userAgent) {
        this.userId = userId; this.termsVersion = termsVersion; this.acceptedAt = acceptedAt;
        this.ipAddress = ipAddress; this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length()));
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTermsVersion() { return termsVersion; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
}
