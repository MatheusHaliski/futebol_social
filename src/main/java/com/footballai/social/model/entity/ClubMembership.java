package com.footballai.social.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="club_memberships")
public class ClubMembership extends ClubResource {
    @Column(name="team_id", columnDefinition="BINARY(16)")
    @NotNull
    public UUID teamId;
    @Column(name="athlete_id", columnDefinition="BINARY(16)")
    @NotNull
    public UUID athleteId;
    @Column(name="parent_club_id", columnDefinition="BINARY(16)")

    public UUID parentClubId;
    @Column(name="kind", length=32)
    @NotBlank @Pattern(regexp="PERMANENT|LOAN|ACADEMY")
    public String kind;
    @Column(name="start_date")
    @NotNull
    public LocalDate startDate;
    @Column(name="end_date")

    public LocalDate endDate;
    @Column(name="shirt_number")
    @Min(1) @Max(999)
    public Integer shirtNumber;
    @Column(name="captain")

    public boolean captain;
    @JsonProperty(access=JsonProperty.Access.READ_ONLY)
    @Column(name="status", length=32)
    @Pattern(regexp="PENDING|ACTIVE|ENDED|REJECTED")
    public String status;
}
