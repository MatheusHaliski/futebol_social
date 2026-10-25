package com.footballai.social.model.entity;

import com.footballai.social.model.enums.LgpdPreference;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Preferências de consentimento — TODAS nascem FALSE (opt-in explícito, Art. 7º, I / Art. 11). */
@Entity
@Table(name = "user_lgpd_preferences")
public class UserLgpdPreferences {
    @Id @Column(name = "user_id", columnDefinition = "BINARY(16)") private UUID userId;
    @Column(name = "share_telemetry_with_clubs", nullable = false) private boolean shareTelemetryWithClubs = false;
    @Column(name = "share_telemetry_with_scouts", nullable = false) private boolean shareTelemetryWithScouts = false;
    @Column(name = "share_medical_data", nullable = false) private boolean shareMedicalData = false;
    @Column(name = "share_biometric_data", nullable = false) private boolean shareBiometricData = false;
    @Column(name = "location_history", nullable = false) private boolean locationHistory = false;
    @Column(name = "share_with_brands", nullable = false) private boolean shareWithBrands = false;
    @Column(name = "personalized_ads", nullable = false) private boolean personalizedAds = false;
    @Column(name = "ai_agent_processing", nullable = false) private boolean aiAgentProcessing = false;
    @Column(name = "updated_at") private Instant updatedAt;

    protected UserLgpdPreferences() { }
    public UserLgpdPreferences(UUID userId) { this.userId = userId; }

    public boolean get(LgpdPreference p) {
        return switch (p) {
            case SHARE_TELEMETRY_WITH_CLUBS -> shareTelemetryWithClubs;
            case SHARE_TELEMETRY_WITH_SCOUTS -> shareTelemetryWithScouts;
            case SHARE_MEDICAL_DATA -> shareMedicalData;
            case SHARE_BIOMETRIC_DATA -> shareBiometricData;
            case LOCATION_HISTORY -> locationHistory;
            case SHARE_WITH_BRANDS -> shareWithBrands;
            case PERSONALIZED_ADS -> personalizedAds;
            case AI_AGENT_PROCESSING -> aiAgentProcessing;
        };
    }

    public void set(LgpdPreference p, boolean value, Instant now) {
        switch (p) {
            case SHARE_TELEMETRY_WITH_CLUBS -> shareTelemetryWithClubs = value;
            case SHARE_TELEMETRY_WITH_SCOUTS -> shareTelemetryWithScouts = value;
            case SHARE_MEDICAL_DATA -> shareMedicalData = value;
            case SHARE_BIOMETRIC_DATA -> shareBiometricData = value;
            case LOCATION_HISTORY -> locationHistory = value;
            case SHARE_WITH_BRANDS -> shareWithBrands = value;
            case PERSONALIZED_ADS -> personalizedAds = value;
            case AI_AGENT_PROCESSING -> aiAgentProcessing = value;
        }
        this.updatedAt = now;
    }

    public Map<LgpdPreference, Boolean> asMap() {
        Map<LgpdPreference, Boolean> m = new EnumMap<>(LgpdPreference.class);
        for (LgpdPreference p : LgpdPreference.values()) m.put(p, get(p));
        return m;
    }

    public void revokeAll(Instant now) { for (LgpdPreference p : LgpdPreference.values()) set(p, false, now); }

    public UUID getUserId() { return userId; }
    public boolean isShareMedicalData() { return shareMedicalData; }
    public boolean isShareBiometricData() { return shareBiometricData; }
    public boolean isLocationHistory() { return locationHistory; }
    public boolean isShareTelemetryWithClubs() { return shareTelemetryWithClubs; }
    public boolean isShareTelemetryWithScouts() { return shareTelemetryWithScouts; }
    public boolean isAiAgentProcessing() { return aiAgentProcessing; }
    public Instant getUpdatedAt() { return updatedAt; }
}
