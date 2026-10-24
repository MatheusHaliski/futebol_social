package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resumo semanal de treinos (30 % do Rarity: telemetria física + assiduidade), agregado a partir da
 * telemetria do Cassandra. Mantém no MySQL apenas o agregado (classe LGPD D).
 */
@Entity
@Table(name = "training_summaries", uniqueConstraints = @UniqueConstraint(columnNames = {"athlete_id", "iso_week"}))
public class TrainingSummary {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Column(name = "iso_week", nullable = false, length = 8) private String isoWeek;
    @Column(name = "sessions_planned", nullable = false) private int sessionsPlanned;
    @Column(name = "sessions_done", nullable = false) private int sessionsDone;
    @Column(name = "load_au", nullable = false, precision = 9, scale = 2) private BigDecimal loadAu = BigDecimal.ZERO;
    @Column(name = "chronic_load_au", nullable = false, precision = 9, scale = 2) private BigDecimal chronicLoadAu = BigDecimal.ZERO;
    @Column(name = "avg_rpe", precision = 4, scale = 2) private BigDecimal avgRpe;
    @Column(name = "distance_km", precision = 7, scale = 2) private BigDecimal distanceKm;
    @Column(name = "readiness", nullable = false) private int readiness = 100;

    public UUID getId() { return id; }
    public UUID getAthleteId() { return athleteId; }
    public void setAthleteId(UUID athleteId) { this.athleteId = athleteId; }
    public String getIsoWeek() { return isoWeek; }
    public void setIsoWeek(String isoWeek) { this.isoWeek = isoWeek; }
    public int getSessionsPlanned() { return sessionsPlanned; }
    public void setSessionsPlanned(int sessionsPlanned) { this.sessionsPlanned = sessionsPlanned; }
    public int getSessionsDone() { return sessionsDone; }
    public void setSessionsDone(int sessionsDone) { this.sessionsDone = sessionsDone; }
    public BigDecimal getLoadAu() { return loadAu; }
    public void setLoadAu(BigDecimal loadAu) { this.loadAu = loadAu; }
    public BigDecimal getChronicLoadAu() { return chronicLoadAu; }
    public void setChronicLoadAu(BigDecimal chronicLoadAu) { this.chronicLoadAu = chronicLoadAu; }
    public BigDecimal getAvgRpe() { return avgRpe; }
    public void setAvgRpe(BigDecimal avgRpe) { this.avgRpe = avgRpe; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public int getReadiness() { return readiness; }
    public void setReadiness(int readiness) { this.readiness = readiness; }
    /** ACWR 7:28 — acute:chronic workload ratio (gatilho RISCO_FADIGA > 1.5). */
    public BigDecimal acwr() {
        if (chronicLoadAu == null || chronicLoadAu.signum() == 0) return BigDecimal.ONE;
        return loadAu.divide(chronicLoadAu, 2, java.math.RoundingMode.HALF_EVEN);
    }
}
