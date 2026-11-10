package com.footballai.social.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="club_teams")
public class ClubTeam extends ClubResource {
    @Column(name="name", length=120)
    @NotBlank @Size(max=120)
    public String name;
    @Column(name="category", length=30)
    @NotBlank @Size(max=30)
    public String category;
    @Column(name="modality", length=30)
    @NotBlank @Size(max=30)
    public String modality;
    @Column(name="gender", length=30)
    @Size(max=30)
    public String gender;
    @Column(name="playing_style", length=80)
    @Size(max=80)
    public String playingStyle;
    @Column(name="wanted_positions", length=200)
    @Size(max=200)
    public String wantedPositions;
    @Column(name="preferred_foot", length=5)
    @Size(max=5)
    public String preferredFoot;
    @Column(name="languages", length=120)
    @Size(max=120)
    public String languages;
    @Column(name="vacancies")
    @Min(0) @Max(200)
    public Integer vacancies;
}
