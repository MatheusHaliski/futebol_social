package com.footballai.social.model.entity;

import com.footballai.social.model.enums.ClubeTipo;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_clubs")
@DiscriminatorValue("CLUBE")
@PrimaryKeyJoinColumn(name = "user_id")
public class Club extends User {
    @Column(name = "legal_name", length = 160) private String legalName;
    @Column(name = "federation_code", length = 40) private String federationCode;
    @Enumerated(EnumType.STRING) @Column(name = "clube_tipo", length = 24) private ClubeTipo clubeTipo;
    @Column private Integer division;
    @Column(name = "founded_year") private Integer foundedYear;
    @Column(length = 120) private String stadium;
    @Column(name = "crest_key") private String crestKey;
    @Column(name = "verified_by", columnDefinition = "BINARY(16)") private UUID verifiedBy;
    @Column(name = "ccf_valid_until") private LocalDate ccfValidUntil;
    @Column(precision = 9, scale = 6) private BigDecimal lat;
    @Column(precision = 9, scale = 6) private BigDecimal lng;

    @Override public UserRole role() { return UserRole.CLUBE; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getFederationCode() { return federationCode; }
    public void setFederationCode(String federationCode) { this.federationCode = federationCode; }
    public ClubeTipo getClubeTipo() { return clubeTipo; }
    public void setClubeTipo(ClubeTipo clubeTipo) { this.clubeTipo = clubeTipo; }
    public Integer getDivision() { return division; }
    public void setDivision(Integer division) { this.division = division; }
    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }
    public String getStadium() { return stadium; }
    public void setStadium(String stadium) { this.stadium = stadium; }
    public String getCrestKey() { return crestKey; }
    public void setCrestKey(String crestKey) { this.crestKey = crestKey; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(UUID verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDate getCcfValidUntil() { return ccfValidUntil; }
    public void setCcfValidUntil(LocalDate ccfValidUntil) { this.ccfValidUntil = ccfValidUntil; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    @Column(name="short_name", length=40) private String shortName;
    public String getShortName() { return shortName; }
    public void setShortName(String value) { shortName = value; }
    @Column(name="website", length=240) private String website;
    public String getWebsite() { return website; }
    public void setWebsite(String value) { website = value; }
    @Column(name="public_contact", length=160) private String publicContact;
    public String getPublicContact() { return publicContact; }
    public void setPublicContact(String value) { publicContact = value; }
    @Column(name="federation", length=120) private String federation;
    public String getFederation() { return federation; }
    public void setFederation(String value) { federation = value; }
    @Column(name="colors", length=80) private String colors;
    public String getColors() { return colors; }
    public void setColors(String value) { colors = value; }
    @Column(name="ownership_type", length=40) private String ownershipType;
    public String getOwnershipType() { return ownershipType; }
    public void setOwnershipType(String value) { ownershipType = value; }
    @Column(name="president", length=120) private String president;
    public String getPresident() { return president; }
    public void setPresident(String value) { president = value; }
    @Column(name="history", length=2000) private String history;
    public String getHistory() { return history; }
    public void setHistory(String value) { history = value; }
    @Column(name="honours", length=2000) private String honours;
    public String getHonours() { return honours; }
    public void setHonours(String value) { honours = value; }
}
