package com.footballai.social.model.entity;

import com.footballai.social.model.enums.StaffSpecialty;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_staff")
@DiscriminatorValue("STAFF")
@PrimaryKeyJoinColumn(name = "user_id")
public class Staff extends User {
    @Enumerated(EnumType.STRING) @Column(length = 24) private StaffSpecialty specialty;
    @Column(name = "cbf_license", length = 40) private String cbfLicense;
    @Column(name = "license_level", length = 20) private String licenseLevel;
    @Column(name = "current_club_id", columnDefinition = "BINARY(16)") private UUID currentClubId;
    @Column(name = "is_head_coach", nullable = false) private boolean headCoach;

    @Override public UserRole role() { return UserRole.STAFF; }
    public StaffSpecialty getSpecialty() { return specialty; }
    public void setSpecialty(StaffSpecialty specialty) { this.specialty = specialty; }
    public String getCbfLicense() { return cbfLicense; }
    public void setCbfLicense(String cbfLicense) { this.cbfLicense = cbfLicense; }
    public String getLicenseLevel() { return licenseLevel; }
    public void setLicenseLevel(String licenseLevel) { this.licenseLevel = licenseLevel; }
    public UUID getCurrentClubId() { return currentClubId; }
    public void setCurrentClubId(UUID currentClubId) { this.currentClubId = currentClubId; }
    public boolean isHeadCoach() { return headCoach; }
    public void setHeadCoach(boolean headCoach) { this.headCoach = headCoach; }
}
