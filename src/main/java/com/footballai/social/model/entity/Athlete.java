package com.footballai.social.model.entity;

import com.footballai.social.model.enums.ContractStatus;
import com.footballai.social.model.enums.Position;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_athletes")
@DiscriminatorValue("ATLETA")
@PrimaryKeyJoinColumn(name = "user_id")
public class Athlete extends User {

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Position position;

    @Column(name = "dominant_foot", length = 5)
    private String dominantFoot;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "current_club_id", columnDefinition = "BINARY(16)")
    private UUID currentClubId;

    @Column(name = "current_club_name", length = 120)
    private String currentClubName;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", nullable = false, length = 16)
    private ContractStatus contractStatus = ContractStatus.AGENTE_LIVRE;

    @Column(name = "contract_until")
    private LocalDate contractUntil;

    @Column(name = "shirt_number")
    private Integer shirtNumber;

    @Column(name = "is_captain", nullable = false)
    private boolean captain = false;

    @Column(length = 20)
    private String categoria;

    /** Consentimento do responsável para menores de 18 (Art. 14). */
    @Column(name = "guardian_consent_at")
    private Instant guardianConsentAt;

    @Override public UserRole role() { return UserRole.ATLETA; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public String getDominantFoot() { return dominantFoot; }
    public void setDominantFoot(String dominantFoot) { this.dominantFoot = dominantFoot; }
    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public UUID getCurrentClubId() { return currentClubId; }
    public void setCurrentClubId(UUID currentClubId) { this.currentClubId = currentClubId; }
    public String getCurrentClubName() { return currentClubName; }
    public void setCurrentClubName(String currentClubName) { this.currentClubName = currentClubName; }
    public ContractStatus getContractStatus() { return contractStatus; }
    public void setContractStatus(ContractStatus contractStatus) { this.contractStatus = contractStatus; }
    public LocalDate getContractUntil() { return contractUntil; }
    public void setContractUntil(LocalDate contractUntil) { this.contractUntil = contractUntil; }
    public Integer getShirtNumber() { return shirtNumber; }
    public void setShirtNumber(Integer shirtNumber) { this.shirtNumber = shirtNumber; }
    public boolean isCaptain() { return captain; }
    public void setCaptain(boolean captain) { this.captain = captain; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Instant getGuardianConsentAt() { return guardianConsentAt; }
    public void setGuardianConsentAt(Instant guardianConsentAt) { this.guardianConsentAt = guardianConsentAt; }
}
