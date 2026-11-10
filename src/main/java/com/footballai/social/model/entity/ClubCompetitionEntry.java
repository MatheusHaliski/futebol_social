package com.footballai.social.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="club_competition_entries")
public class ClubCompetitionEntry extends ClubResource {
    @Column(name="team_id", columnDefinition="BINARY(16)")
    @NotNull
    public UUID teamId;
    @Column(name="competition", length=120)
    @NotBlank @Size(max=120)
    public String competition;
    @Column(name="organizer", length=120)
    @Size(max=120)
    public String organizer;
    @Column(name="country", length=2)
    @Size(max=2)
    public String country;
    @Column(name="season", length=30)
    @NotBlank @Size(max=30)
    public String season;
    @Column(name="format", length=32)
    @NotBlank @Pattern(regexp="LEAGUE|CUP")
    public String format;
    @Column(name="division")
    @Min(1) @Max(100)
    public Integer division;
    @Column(name="stage", length=80)
    @Size(max=80)
    public String stage;
    @Column(name="group_name", length=80)
    @Size(max=80)
    public String groupName;
    @Column(name="outcome", length=80)
    @Size(max=80)
    public String outcome;
    @Column(name="position")
    @Min(1)
    public Integer position;
    @Column(name="round")
    @Min(0)
    public Integer round;
    @Column(name="played")
    @Min(0)
    public Integer played;
    @Column(name="wins")
    @Min(0)
    public Integer wins;
    @Column(name="draws")
    @Min(0)
    public Integer draws;
    @Column(name="losses")
    @Min(0)
    public Integer losses;
    @Column(name="goals_for")
    @Min(0)
    public Integer goalsFor;
    @Column(name="goals_against")
    @Min(0)
    public Integer goalsAgainst;
    @Column(name="points")

    public Integer points;
    @Column(name="champion")

    public boolean champion;
    @Column(name="as_of")
    @NotNull
    public LocalDate asOf;
}
