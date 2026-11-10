package com.footballai.social.model.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="manager_career_titles")
public class ManagerCareerTitle {
    @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(columnDefinition="BINARY(16)") public UUID id;
    @Column(name="manager_id",columnDefinition="BINARY(16)") public UUID managerId;
    @Column(name="club_id",columnDefinition="BINARY(16)") public UUID clubId;
    @Column(name="club_name",length=120) public String clubName;
    @Column(length=120) public String competition;
    @Column(length=30) public String season;
    @Column(length=40) public String category;
    @Column(name="awarded_on") public LocalDate awardedOn;
    @Column(length=240) public String source;
    @Column(name="created_at") public Instant createdAt;
}
