package com.footballai.social.model.entity;

import com.footballai.social.config.CryptoConverter;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

/** Registro de lesão — dado sensível (Art. 11): cifrado e visível a terceiros só com shareMedicalData. */
@Entity
@Table(name = "injury_records")
public class InjuryRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Convert(converter = CryptoConverter.class) @Column(name = "injury_type", nullable = false, length = 512) private String injuryType;
    @Column(name = "body_part", length = 40) private String bodyPart;
    @Column(nullable = false, length = 12) private String severity;
    @Column(name = "started_at", nullable = false) private LocalDate startedAt;
    @Column(name = "expected_return_date") private LocalDate expectedReturnDate;
    @Column(name = "cleared_at") private LocalDate clearedAt;
    @Column(name = "reported_by", columnDefinition = "BINARY(16)") private UUID reportedBy;
    @Convert(converter = CryptoConverter.class) @Column(length = 2048) private String notes;

    public boolean isActive() { return clearedAt == null; }
    public UUID getId() { return id; }
    public UUID getAthleteId() { return athleteId; }
    public void setAthleteId(UUID athleteId) { this.athleteId = athleteId; }
    public String getInjuryType() { return injuryType; }
    public void setInjuryType(String injuryType) { this.injuryType = injuryType; }
    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate startedAt) { this.startedAt = startedAt; }
    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    public LocalDate getClearedAt() { return clearedAt; }
    public void setClearedAt(LocalDate clearedAt) { this.clearedAt = clearedAt; }
    public UUID getReportedBy() { return reportedBy; }
    public void setReportedBy(UUID reportedBy) { this.reportedBy = reportedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
