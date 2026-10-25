package com.footballai.social.model.entity;

import com.footballai.social.model.enums.UserRole;
import com.footballai.social.model.enums.VerificationLevel;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_scouts")
@DiscriminatorValue("SCOUT")
@PrimaryKeyJoinColumn(name = "user_id")
public class Scout extends User {
    @Column(length = 120) private String agency;
    @Column(name = "regions_of_interest", columnDefinition = "JSON") private String regionsOfInterest;
    @Enumerated(EnumType.STRING) @Column(name = "verification_level", nullable = false, length = 16) private VerificationLevel verificationLevel = VerificationLevel.COMUNIDADE;
    @Column(name = "affiliation_id", columnDefinition = "BINARY(16)") private UUID affiliationId;

    @Override public UserRole role() { return UserRole.SCOUT; }
    public String getAgency() { return agency; }
    public void setAgency(String agency) { this.agency = agency; }
    public String getRegionsOfInterest() { return regionsOfInterest; }
    public void setRegionsOfInterest(String regionsOfInterest) { this.regionsOfInterest = regionsOfInterest; }
    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }
    public UUID getAffiliationId() { return affiliationId; }
    public void setAffiliationId(UUID affiliationId) { this.affiliationId = affiliationId; }
}
