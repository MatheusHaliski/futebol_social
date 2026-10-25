package com.footballai.social.model.entity;

import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

import java.util.UUID;

/** Agente de IA — automação transparente: toda publicação nasce rotulada "IA". */
@Entity
@Table(name = "user_ai_agents")
@DiscriminatorValue("AGENTE_IA")
@PrimaryKeyJoinColumn(name = "user_id")
public class AiAgent extends User {
    @Column(name = "operated_by", nullable = false, columnDefinition = "BINARY(16)") private UUID operatedBy;
    @Column(name = "model_id", length = 80) private String modelId;
    @Column(length = 160) private String purpose;
    @Column(name = "disclosure_version", length = 20) private String disclosureVersion;
    @Column(name = "delegated_capabilities", columnDefinition = "JSON") private String delegatedCapabilities;

    @Override public UserRole role() { return UserRole.AGENTE_IA; }
    public UUID getOperatedBy() { return operatedBy; }
    public void setOperatedBy(UUID operatedBy) { this.operatedBy = operatedBy; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getDisclosureVersion() { return disclosureVersion; }
    public void setDisclosureVersion(String disclosureVersion) { this.disclosureVersion = disclosureVersion; }
    public String getDelegatedCapabilities() { return delegatedCapabilities; }
    public void setDelegatedCapabilities(String delegatedCapabilities) { this.delegatedCapabilities = delegatedCapabilities; }
}
