package com.footballai.social.model.entity;

import com.footballai.social.config.CryptoConverter;
import com.footballai.social.model.enums.HealthSpecialty;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.model.enums.VerificationLevel;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_health_staff")
@DiscriminatorValue("STAFF_SAUDE")
@PrimaryKeyJoinColumn(name = "user_id")
public class HealthStaff extends User {
    @Enumerated(EnumType.STRING) @Column(length = 24) private HealthSpecialty specialty;
    @Convert(converter = CryptoConverter.class) @Column(name = "professional_registry", length = 512) private String professionalRegistry;
    @Column(name = "registry_uf", length = 2) private String registryUf;
    @Column(name = "current_club_id", columnDefinition = "BINARY(16)") private UUID currentClubId;
    @Enumerated(EnumType.STRING) @Column(name = "verification_level", nullable = false, length = 16) private VerificationLevel verificationLevel = VerificationLevel.COMUNIDADE;

    @Override public UserRole role() { return UserRole.STAFF_SAUDE; }
    public boolean canOriginateInjuryRecord() { return verificationLevel.countsForRarity(); }
    public HealthSpecialty getSpecialty() { return specialty; }
    public void setSpecialty(HealthSpecialty specialty) { this.specialty = specialty; }
    public String getProfessionalRegistry() { return professionalRegistry; }
    public void setProfessionalRegistry(String professionalRegistry) { this.professionalRegistry = professionalRegistry; }
    public String getRegistryUf() { return registryUf; }
    public void setRegistryUf(String registryUf) { this.registryUf = registryUf; }
    public UUID getCurrentClubId() { return currentClubId; }
    public void setCurrentClubId(UUID currentClubId) { this.currentClubId = currentClubId; }
    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }
}
