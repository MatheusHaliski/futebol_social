package com.footballai.social.model.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Telemetria do atleta (Gêmeo). Partição (athlete_id, year_month); dados de saúde = sensíveis (Art. 11). */
@Table("telemetry_data")
public class TelemetryData {

    @PrimaryKeyClass
    public static class Key implements Serializable {
        @PrimaryKeyColumn(name = "athlete_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0) private UUID athleteId;
        @PrimaryKeyColumn(name = "year_month", type = PrimaryKeyType.PARTITIONED, ordinal = 1) private String yearMonth;
        @PrimaryKeyColumn(name = "recorded_at", type = PrimaryKeyType.CLUSTERED, ordinal = 2, ordering = Ordering.DESCENDING) private Instant recordedAt;

        public Key() { }
        public Key(UUID athleteId, String yearMonth, Instant recordedAt) { this.athleteId = athleteId; this.yearMonth = yearMonth; this.recordedAt = recordedAt; }
        public UUID getAthleteId() { return athleteId; }
        public String getYearMonth() { return yearMonth; }
        public Instant getRecordedAt() { return recordedAt; }
    }

    @PrimaryKey private Key key;
    @Column("event_type") private String eventType;
    @Column("metric_type") private String metricType;
    @Column("distance_km") private BigDecimal distanceKm;
    @Column("duration_min") private Integer durationMin;
    @Column("rpe") private Integer rpe;
    @Column("load_au") private BigDecimal loadAu;
    @Column("max_speed_kmh") private BigDecimal maxSpeedKmh;
    @Column("passes_ok") private Integer passesOk;
    @Column("passes_total") private Integer passesTotal;
    /** Só preenchido com locationHistory = true. */
    @Column("gps_track") private String gpsTrack;

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public Integer getRpe() { return rpe; }
    public void setRpe(Integer rpe) { this.rpe = rpe; }
    public BigDecimal getLoadAu() { return loadAu; }
    public void setLoadAu(BigDecimal loadAu) { this.loadAu = loadAu; }
    public BigDecimal getMaxSpeedKmh() { return maxSpeedKmh; }
    public void setMaxSpeedKmh(BigDecimal maxSpeedKmh) { this.maxSpeedKmh = maxSpeedKmh; }
    public Integer getPassesOk() { return passesOk; }
    public void setPassesOk(Integer passesOk) { this.passesOk = passesOk; }
    public Integer getPassesTotal() { return passesTotal; }
    public void setPassesTotal(Integer passesTotal) { this.passesTotal = passesTotal; }
    public String getGpsTrack() { return gpsTrack; }
    public void setGpsTrack(String gpsTrack) { this.gpsTrack = gpsTrack; }
}
