package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.AuthDtos.UpdateMeRequest;
import com.footballai.social.controller.dto.AuthDtos.UserSummaryDTO;
import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.DigitalTwinRepository;
import com.footballai.social.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Aparência (material, gradiente, fonte, densidade, contraste) e foto do perfil: só mídia enviada pela própria conta. */
class UserServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final DigitalTwinRepository twins = mock(DigitalTwinRepository.class);
    private final StorageService storage = mock(StorageService.class);
    private final UserService svc = new UserService(users, twins, storage);
    private final UUID me = UUID.randomUUID();
    private final Athlete u = new Athlete();
    private final AuthPrincipal p = new AuthPrincipal(me, UserRole.ATLETA, "s", "j", Instant.MAX);

    UserServiceTest() {
        ReflectionTestUtils.setField(u, "id", me); u.setUsername("atleta"); u.setDisplayName("Atleta"); u.setStatus(AccountStatus.ACTIVE);
        when(users.findById(me)).thenReturn(Optional.of(u));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        when(twins.findByUserId(any())).thenReturn(Optional.empty());
        when(storage.mediaUrl(anyString())).thenAnswer(i -> "https://media.local/" + i.getArgument(0));
    }

    private static UpdateMeRequest req(UiMaterial m, UiGradient g, UiFont f, UiDensity d, UiContrast c, String wallpaperKey, String avatarKey) {
        return new UpdateMeRequest(null, null, null, null, null, null, null, null, null, null, null, wallpaperKey, m, g, f, d, c, avatarKey);
    }

    @Test
    void appearanceAxesArePersistedAndReturned() {
        UserSummaryDTO dto = svc.update(p, req(UiMaterial.VIDRO, UiGradient.CREPUSCULO, UiFont.LEGIVEL, UiDensity.COMPACTA, UiContrast.ALTO, null, null));
        assertThat(dto.uiMaterial()).isEqualTo("VIDRO");
        assertThat(dto.uiGradient()).isEqualTo("CREPUSCULO");
        assertThat(dto.uiFont()).isEqualTo("LEGIVEL");
        assertThat(dto.uiDensity()).isEqualTo("COMPACTA");
        assertThat(dto.uiContrast()).isEqualTo("ALTO");
        // campos ausentes não mudam nada
        dto = svc.update(p, req(null, null, null, null, null, null, null));
        assertThat(dto.uiMaterial()).isEqualTo("VIDRO");
        assertThat(new Athlete().getUiGradient()).isEqualTo(UiGradient.HOLOFOTE);   // padrão do layout
    }

    @Test
    void avatarAndWallpaperMustBeMediaUploadedByTheSameAccount() {
        String mine = "media/" + me + "/foto.jpg";
        UserSummaryDTO dto = svc.update(p, req(null, null, null, null, null, null, mine));
        assertThat(u.getAvatarKey()).isEqualTo(mine);
        assertThat(dto.avatarUrl()).isEqualTo("https://media.local/" + mine);

        String someoneElse = "media/" + UUID.randomUUID() + "/foto.jpg";
        assertThatThrownBy(() -> svc.update(p, req(null, null, null, null, null, null, someoneElse))).isInstanceOf(ApiException.class).hasMessageContaining("sua conta");
        assertThatThrownBy(() -> svc.update(p, req(null, null, null, null, null, someoneElse, null))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> svc.update(p, req(null, null, null, null, null, "media/" + me + "/../outro/x.jpg", null))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> svc.update(p, req(null, null, null, null, null, "exports/" + me + "/dump.zip", null))).isInstanceOf(ApiException.class);
        assertThat(u.getAvatarKey()).isEqualTo(mine);
        assertThat(u.getWallpaper()).isEqualTo(Wallpaper.NONE);

        // vazio remove a foto
        dto = svc.update(p, req(null, null, null, null, null, null, ""));
        assertThat(u.getAvatarKey()).isNull();
        assertThat(dto.avatarUrl()).isNull();
    }
}
