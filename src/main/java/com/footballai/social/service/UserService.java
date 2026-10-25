package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.AuthDtos.UpdateMeRequest;
import com.footballai.social.controller.dto.AuthDtos.UserSummaryDTO;
import com.footballai.social.model.entity.DigitalTwin;
import com.footballai.social.model.entity.HealthStaff;
import com.footballai.social.model.entity.Scout;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.VerificationLevel;
import com.footballai.social.model.enums.Wallpaper;
import com.footballai.social.repository.DigitalTwinRepository;
import com.footballai.social.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final DigitalTwinRepository twins;
    private final StorageService storage;

    public UserService(UserRepository users, DigitalTwinRepository twins, StorageService storage) { this.users = users; this.twins = twins; this.storage = storage; }

    public User require(UUID id) { return users.findById(id).orElseThrow(() -> ApiException.notFound("Usuário")); }

    public UserSummaryDTO summary(User u) {
        Optional<DigitalTwin> twin = u.getRole().hasDigitalTwin() ? twins.findByUserId(u.getId()) : Optional.empty();
        return new UserSummaryDTO(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(), u.getRole().label(), u.getRole().ringColor(),
                u.getRole().dotClass(), u.getStatus().name(), u.getVisibility().name(), u.isVerified(),
                twin.map(t -> t.getRarityTier().name()).orElse(null), twin.map(t -> t.getRarityTier().crestIcon()).orElse(null),
                u.getLocale(), u.isDarkMode(), u.getFontScale(), u.getUiAccent().name(), u.getUiSurface().name(), u.getWallpaper().name(), wallpaperUrl(u));
    }

    public UserSummaryDTO me(AuthPrincipal p) { return summary(require(p.userId())); }

    /** Preferências de interface (Aparência · Idioma) e dados básicos — nunca o papel (imutável). */
    @Transactional
    public UserSummaryDTO update(AuthPrincipal p, UpdateMeRequest r) {
        User u = require(p.userId());
        if (r.displayName() != null) u.setDisplayName(r.displayName());
        if (r.bio() != null) u.setBio(r.bio());
        if (r.locale() != null) u.setLocale(r.locale());
        if (r.darkMode() != null) u.setDarkMode(r.darkMode());
        if (r.fontScale() != null) u.setFontScale(r.fontScale());
        if (r.visibility() != null) u.setVisibility(r.visibility());
        if (r.stateCode() != null) u.setStateCode(r.stateCode().toUpperCase());
        if (r.city() != null) u.setCity(r.city());
        if (r.uiAccent() != null) u.setUiAccent(r.uiAccent());
        if (r.uiSurface() != null) u.setUiSurface(r.uiSurface());
        if (r.wallpaper() != null) { u.setWallpaper(r.wallpaper()); if (r.wallpaper() != Wallpaper.CUSTOM) u.setWallpaperKey(null); }
        if (r.wallpaperKey() != null) { u.setWallpaperKey(r.wallpaperKey()); u.setWallpaper(Wallpaper.CUSTOM); }
        return summary(users.save(u));
    }

    /** URL da tela de fundo: asset público das paletas ou mídia do usuário (pré-assinada). */
    private String wallpaperUrl(User u) {
        if (u.getWallpaper() == Wallpaper.CUSTOM) return u.getWallpaperKey() == null ? null : storage.mediaUrl(u.getWallpaperKey());
        return u.getWallpaper().url();
    }

    public static VerificationLevel verificationLevelOf(User u) {
        if (u instanceof Scout s) return s.getVerificationLevel();
        if (u instanceof HealthStaff h) return h.getVerificationLevel();
        return u.isVerified() ? VerificationLevel.VERIFICADO : VerificationLevel.COMUNIDADE;
    }
}
