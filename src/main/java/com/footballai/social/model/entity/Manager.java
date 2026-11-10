package com.footballai.social.model.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.model.enums.TransferSituation;

/** Manager humano: treinador principal, assistente ou gestor esportivo; distinto do agente de atletas. */
@Entity @Table(name="user_managers") @DiscriminatorValue("MANAGER") @PrimaryKeyJoinColumn(name="user_id")
public class Manager extends User {
    @Override public UserRole role() { return UserRole.MANAGER; }
    @Column(name="role_function", length=32) private String function = "HEAD_COACH";
    public String getFunction() { return function; }
    public void setFunction(String value) { function = value; }
    @Column(name="nationalities", length=120) private String nationalities;
    public String getNationalities() { return nationalities; }
    public void setNationalities(String value) { nationalities = value; }
    @Column(name="languages", length=120) private String languages;
    public String getLanguages() { return languages; }
    public void setLanguages(String value) { languages = value; }
    @Column(name="license_name", length=120) private String licenseName;
    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String value) { licenseName = value; }
    @Column(name="license_level", length=40) private String licenseLevel;
    public String getLicenseLevel() { return licenseLevel; }
    public void setLicenseLevel(String value) { licenseLevel = value; }
    @Column(name="preferred_formation", length=40) private String preferredFormation;
    public String getPreferredFormation() { return preferredFormation; }
    public void setPreferredFormation(String value) { preferredFormation = value; }
    @Column(name="playing_style", length=120) private String playingStyle;
    public String getPlayingStyle() { return playingStyle; }
    public void setPlayingStyle(String value) { playingStyle = value; }
    @Column(name="career_started_on") private LocalDate careerStartedOn;
    public LocalDate getCareerStartedOn() { return careerStartedOn; }
    public void setCareerStartedOn(LocalDate value) { careerStartedOn = value; }
    @Column(name="has_current_club") private Boolean hasCurrentClub;
    public Boolean getHasCurrentClub() { return hasCurrentClub; }
    public void setHasCurrentClub(Boolean value) { hasCurrentClub = value; }
    @Column(name="club_status_source", length=240) private String clubStatusSource;
    public String getClubStatusSource() { return clubStatusSource; }
    public void setClubStatusSource(String value) { clubStatusSource = value; }
    @Column(name="club_status_as_of") private LocalDate clubStatusAsOf;
    public LocalDate getClubStatusAsOf() { return clubStatusAsOf; }
    public void setClubStatusAsOf(LocalDate value) { clubStatusAsOf = value; }
    @Enumerated(EnumType.STRING) @Column(name="transfer_situation",length=24,nullable=false)
    private TransferSituation transferSituation = TransferSituation.NOT_FOR_SALE;
    public TransferSituation getTransferSituation() { return transferSituation; }
    public void setTransferSituation(TransferSituation value) { transferSituation = value; }
}
