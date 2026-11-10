package com.footballai.social.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="club_finance_snapshots")
public class ClubFinanceSnapshot extends ClubResource {
    @Column(name="team_id", columnDefinition="BINARY(16)")

    public UUID teamId;
    @Column(name="season", length=30)
    @NotBlank @Size(max=30)
    public String season;
    @Column(name="currency", length=32)
    @NotBlank @Pattern(regexp="[A-Z]{3}")
    public String currency;
    @Column(name="payroll", precision=18, scale=6)
    @DecimalMin("0")
    @Digits(integer=12,fraction=6) public BigDecimal payroll;
    @Column(name="payroll_period", length=32)
    @NotBlank @Pattern(regexp="MONTH|YEAR")
    public String payrollPeriod;
    @Column(name="squad_value", precision=18, scale=6)
    @DecimalMin("0")
    @Digits(integer=12,fraction=6) public BigDecimal squadValue;
    @Column(name="recruitment_budget", precision=18, scale=6)
    @DecimalMin("0")
    @Digits(integer=12,fraction=6) public BigDecimal recruitmentBudget;
    @Column(name="valuation_type", length=32)
    @NotBlank @Pattern(regexp="DECLARED|ESTIMATED|VERIFIED")
    public String valuationType;
    @Column(name="as_of")
    @NotNull
    public LocalDate asOf;
    @Column(name="published")

    public boolean published;
}
