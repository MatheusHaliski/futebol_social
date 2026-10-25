package com.footballai.social.model.entity;

import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

@Entity
@Table(name = "user_agents")
@DiscriminatorValue("AGENTE")
@PrimaryKeyJoinColumn(name = "user_id")
public class Agent extends User {
    @Column(name = "fifa_license_id", length = 40) private String fifaLicenseId;
    @Column(name = "license_status", length = 12) private String licenseStatus = "PENDENTE";
    @Column(name = "agency_name", length = 120) private String agencyName;

    @Override public UserRole role() { return UserRole.AGENTE; }
    public String getFifaLicenseId() { return fifaLicenseId; }
    public void setFifaLicenseId(String fifaLicenseId) { this.fifaLicenseId = fifaLicenseId; }
    public String getLicenseStatus() { return licenseStatus; }
    public void setLicenseStatus(String licenseStatus) { this.licenseStatus = licenseStatus; }
    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
}
