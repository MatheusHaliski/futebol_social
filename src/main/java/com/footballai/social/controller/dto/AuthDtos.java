package com.footballai.social.controller.dto;

import com.footballai.social.model.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() { }

    /**
     * Cadastro polimórfico (RF2). {@code profile} traz os campos do papel: position, dominantFoot, heightCm, clubName,
     * contractStatus, specialty, legalName, clubeTipo, agency, tradeName, categoria, outlet, midiaTipo, entityName,
     * entidadeTipo, operatedBy, modelId, purpose…
     */
    public record RegisterRequest(@NotNull UserRole role,
                                  @NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 10, max = 72) String password,
                                  @NotBlank @Pattern(regexp = "^[a-z0-9_.]{3,30}$") String username,
                                  @NotBlank @Size(max = 80) String displayName,
                                  LocalDate birthDate,
                                  boolean acceptTerms,
                                  @NotBlank String termsVersion,
                                  @Size(max = 2) String stateCode,
                                  @Size(max = 80) String city,
                                  Map<String, String> profile) { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record RefreshRequest(@NotBlank String refreshToken) { }
    public record LogoutRequest(String refreshToken) { }
    public record AcceptTermsRequest(@NotBlank String termsVersion) { }

    public record UserSummaryDTO(UUID id, String username, String displayName, String role, String roleLabel, String ringColor, String dotClass,
                                 String status, String visibility, boolean verified, String tier, String crestIcon, String locale, boolean darkMode, BigDecimal fontScale,
                                 String uiAccent, String uiSurface, String wallpaper, String wallpaperUrl) { }

    public record AuthResponse(String tokenType, String accessToken, long expiresIn, String refreshToken, String sessionId, UserSummaryDTO user) { }

    public record TermsDTO(String version, String owner, String documentUrl, String privacyPolicyUrl, Instant effectiveFrom) { }

    /** Conta + Aparência (tema, fonte, cor de destaque, tom das barras/painéis, tela de fundo) + Idioma + visibilidade. */
    public record UpdateMeRequest(@Size(max = 80) String displayName, @Size(max = 280) String bio, @Size(max = 5) String locale, Boolean darkMode,
                                  @DecimalMin("0.8") @DecimalMax("1.4") BigDecimal fontScale, ProfileVisibility visibility, @Size(max = 2) String stateCode, @Size(max = 80) String city,
                                  UiAccent uiAccent, UiSurface uiSurface, Wallpaper wallpaper, @Size(max = 255) String wallpaperKey) { }
}
