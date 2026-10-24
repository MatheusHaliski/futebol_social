package com.footballai.social.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Central de Compliance LGPD (RF5/RF23) — telas Conta · Privacidade · Aparência · Idioma · Seus dados. */
public final class LgpdDtos {
    private LgpdDtos() { }

    public record PreferenceDTO(String key, boolean enabled, String article, String articleLabel, boolean sensitive, boolean requiresStepUp) { }
    public record PrivacyCenterDTO(UUID userId, String accountStatus, String visibility, List<PreferenceDTO> preferences,
                                   Instant deletionRequestedAt, Instant scheduledPurgeAt, List<Map<String, String>> sessions) { }
    public record UpdatePreferenceRequest(@NotNull Boolean enabled) { }
    public record ConsentEntryDTO(String preference, boolean oldValue, boolean newValue, String article, String articleLabel, Instant changedAt) { }
    public record ExportDTO(UUID id, String status, Instant requestedAt, Instant readyAt, Instant expiresAt, String downloadUrl, Long sizeBytes, String failureReason) { }
    public record StepUpRequest(@NotBlank String password) { }
    public record StepUpResponse(String stepUpToken, long expiresInSeconds) { }
    public record DeletionDTO(String status, Instant deletionRequestedAt, Instant scheduledPurgeAt, int graceDays, String message) { }
}
