package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Versão dos Termos de Uso / Política de Privacidade da Velion (proprietária da Orange Social). */
@Entity
@Table(name = "terms_versions")
public class TermsVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(nullable = false, unique = true, length = 20) private String version;
    @Column(nullable = false, length = 80) private String owner = "Velion (Orange Social)";
    @Column(name = "document_url", nullable = false) private String documentUrl;
    @Column(name = "privacy_policy_url", nullable = false) private String privacyPolicyUrl;
    @Column(name = "effective_from", nullable = false) private Instant effectiveFrom;
    @Column(name = "is_current", nullable = false) private boolean current;

    public UUID getId() { return id; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
    public String getPrivacyPolicyUrl() { return privacyPolicyUrl; }
    public void setPrivacyPolicyUrl(String privacyPolicyUrl) { this.privacyPolicyUrl = privacyPolicyUrl; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
}
