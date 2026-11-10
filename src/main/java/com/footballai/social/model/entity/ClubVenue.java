package com.footballai.social.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="club_venues")
public class ClubVenue extends ClubResource {
    @Column(name="name", length=120)
    @NotBlank @Size(max=120)
    public String name;
    @Column(name="venue_type", length=32)
    @NotBlank @Pattern(regexp="STADIUM|TRAINING_CENTER|ACADEMY")
    public String venueType;
    @Column(name="street", length=160)
    @NotBlank @Size(max=160)
    public String street;
    @Column(name="number", length=20)
    @Size(max=20)
    public String number;
    @Column(name="district", length=80)
    @Size(max=80)
    public String district;
    @Column(name="city", length=80)
    @NotBlank @Size(max=80)
    public String city;
    @Column(name="state", length=80)
    @Size(max=80)
    public String state;
    @Column(name="postal_code", length=20)
    @Size(max=20)
    public String postalCode;
    @Column(name="country", length=2)
    @NotBlank @Size(max=2)
    public String country;
    @Column(name="capacity")
    @Min(0)
    public Integer capacity;
    @Column(name="surface", length=40)
    @Size(max=40)
    public String surface;
    @Column(name="ownership", length=40)
    @Size(max=40)
    public String ownership;
    @Column(name="latitude", precision=18, scale=6)
    @DecimalMin("-90") @DecimalMax("90")
    @Digits(integer=12,fraction=6) public BigDecimal latitude;
    @Column(name="longitude", precision=18, scale=6)
    @DecimalMin("-180") @DecimalMax("180")
    @Digits(integer=12,fraction=6) public BigDecimal longitude;
    @Column(name="start_date")
    @NotNull
    public LocalDate startDate;
    @Column(name="end_date")

    public LocalDate endDate;
}
