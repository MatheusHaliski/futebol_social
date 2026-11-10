package com.footballai.social.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="club_manager_tenures")
public class ClubManagerTenure extends ClubResource {
    @Column(name="team_id", columnDefinition="BINARY(16)")
    @NotNull
    public UUID teamId;
    @Column(name="name", length=120)
    @NotBlank @Size(max=120)
    public String name;
    @Column(name="role_function", length=32)
    @NotBlank @Pattern(regexp="HEAD_COACH|SPORTING_DIRECTOR|ASSISTANT")
    public String function;
    @Column(name="start_date")
    @NotNull
    public LocalDate startDate;
    @Column(name="end_date")

    public LocalDate endDate;
    @Column(name="interim")

    public boolean interim;
    @Column(name="nationality", length=2)
    @Size(max=2)
    public String nationality;

    @Column(name="manager_user_id", columnDefinition="BINARY(16)") public UUID managerUserId;
    @Column(name="contract_until") public LocalDate contractUntil;
    @DecimalMin("0") @Column(name="transfer_fee", precision=18, scale=6) @Digits(integer=12,fraction=6) public BigDecimal transferFee;
    @Pattern(regexp="[A-Z]{3}") @Column(length=3) public String currency;
    @Column(name="transfer_fee_public") public boolean transferFeePublic;
}
