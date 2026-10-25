package com.footballai.social.service;

import com.footballai.social.controller.dto.CardDtos.CardArtDTO;
import com.footballai.social.model.entity.CardArt;
import com.footballai.social.model.enums.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Arte de fundo: segunda cor e gradiente, regra sóbria dos alertas e versão sóbria para exibição. */
class CardArtTest {

    @Test
    void secondColourIsNormalisedAndDefaultsToMainWhenTheGradientNeedsIt() {
        CardArt a = new CardArt("#FF5D5D", "#3B82F6", CardGradient.DIAGONAL, CardMaterial.CETIM, CardLayout.HERO, CardStyle.FAIXA);
        assertThat(a.getColor()).isEqualTo("#ff5d5d");
        assertThat(a.getColor2()).isEqualTo("#3b82f6");
        assertThat(a.needsColor2()).isTrue();
        // degradê sem segunda cor: monocromático, nunca uma cor inventada
        assertThat(new CardArt("#00d47a", null, CardGradient.DEGRADE, CardMaterial.FOSCO, CardLayout.CLASSICO, CardStyle.GRAFITE).getColor2()).isEqualTo("#00d47a");
        // padronagem bicolor também pede a segunda cor
        assertThat(new CardArt("#a06bff", null, CardGradient.SUAVE, CardMaterial.FOSCO, CardLayout.CLASSICO, CardStyle.LISTRAS).getColor2()).isEqualTo("#a06bff");
        // gradiente de uma cor não inventa a segunda
        assertThat(new CardArt("#a06bff", null, CardGradient.SUAVE, CardMaterial.FOSCO, CardLayout.CLASSICO, CardStyle.NEON).getColor2()).isNull();
        assertThat(new CardArt("#a06bff", "  ", CardGradient.SOLIDO, null, null, null).getColor2()).isNull();
        assertThatThrownBy(() -> new CardArt("#a06bff", "azul", CardGradient.DEGRADE, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alertCardsOnlyAcceptSoberArt() {
        CardArt sober = new CardArt("#ff5d5d", null, CardGradient.SOLIDO, CardMaterial.MALHA, CardLayout.MINIMAL, CardStyle.GRAFITE);
        assertThat(sober.validateFor(PostContentType.LESAO_ALERTA)).isSameAs(sober);
        assertThatThrownBy(() -> new CardArt("#ff5d5d", null, CardGradient.RADIAL, CardMaterial.FOSCO, null, CardStyle.GRAFITE).validateFor(PostContentType.LESAO_ALERTA))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("gradiente");
        assertThatThrownBy(() -> new CardArt("#ff5d5d", null, CardGradient.SUAVE, CardMaterial.HOLOGRAFICO, null, CardStyle.GRAFITE).validateFor(PostContentType.INSTITUCIONAL))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("material");
        assertThatThrownBy(() -> new CardArt("#ff5d5d", null, CardGradient.SUAVE, CardMaterial.FOSCO, null, CardStyle.OURO).validateFor(PostContentType.INSTITUCIONAL))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Grafite");
        // os demais tipos aceitam qualquer combinação
        CardArt festive = new CardArt("#f5c542", "#ff9f43", CardGradient.DIAGONAL, CardMaterial.PRISMA, CardLayout.HERO, CardStyle.MOSAICO);
        assertThat(festive.validateFor(PostContentType.ESPECIAL)).isSameAs(festive);
    }

    @Test
    void legacyFestiveArtOnAlertCardsIsDisplayedSober() {
        CardArt legacy = new CardArt("#ff5d5d", CardMaterial.METALICO, CardLayout.HERO, CardStyle.OURO);
        CardArtDTO dto = CardArtDTO.of(legacy.soberFor(PostContentType.INSTITUCIONAL));
        assertThat(dto.material()).isEqualTo("FOSCO");
        assertThat(dto.style()).isEqualTo("GRAFITE");
        assertThat(dto.layout()).isEqualTo("HERO");
        assertThat(dto.color()).isEqualTo("#ff5d5d");
        assertThat(CardArtDTO.of(legacy.soberFor(PostContentType.LIGA)).style()).isEqualTo("OURO");
        assertThat(new CardArt().soberFor(PostContentType.LESAO_ALERTA).isEmpty()).isTrue();
    }
}
