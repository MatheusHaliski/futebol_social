package com.footballai.social.service;

import com.footballai.social.model.entity.CardArt;
import com.footballai.social.model.enums.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Telemetria → prontidão, Copilot (título 5–12 palavras, tipo por papel, arte por tier) e arte do card. */
class PerformanceRulesTest {
    @Test
    void readinessPenalisesHighAcwrAndRpe() {
        assertThat(PerformanceService.readiness(new BigDecimal("1.0"), new BigDecimal("6"))).isEqualTo(100);
        assertThat(PerformanceService.readiness(new BigDecimal("1.5"), new BigDecimal("6"))).isEqualTo(80);
        assertThat(PerformanceService.readiness(new BigDecimal("2.0"), new BigDecimal("9"))).isEqualTo(50);
        assertThat(PerformanceService.readiness(new BigDecimal("0.5"), null)).isEqualTo(90);
    }

    @Test
    void copilotKeepsTitleBetweenFiveAndTwelveWords() {
        assertThat(PostService.wordCount(CopilotService.clampWords("Treino"))).isBetween(5, 12);
        assertThat(PostService.wordCount(CopilotService.clampWords("um dois três quatro cinco seis sete oito nove dez onze doze treze catorze"))).isEqualTo(12);
        assertThat(CopilotService.clampWords("Sessão de sprints com 25 km/h sustentados hoje")).isEqualTo("Sessão de sprints com 25 km/h sustentados hoje");
    }

    @Test
    void copilotInfersTypeFromRoleAndDraft() {
        assertThat(CopilotService.inferType(UserRole.ATLETA, null)).isEqualTo(PostContentType.TREINO);
        assertThat(CopilotService.inferType(UserRole.ATLETA, "gol de fora da área na partida")).isEqualTo(PostContentType.PARTIDA_MOMENTO);
        assertThat(CopilotService.inferType(UserRole.SCOUT, "")).isEqualTo(PostContentType.RELATORIO_TATICO);
        assertThat(CopilotService.inferType(UserRole.ATLETA, "lesão no tornozelo")).isEqualTo(PostContentType.LESAO_ALERTA);
        assertThat(CopilotService.inferType(UserRole.IMPRENSA, "lesão no tornozelo")).isEqualTo(PostContentType.INSTITUCIONAL);
        assertThat(CopilotService.inferType(UserRole.OFICIAL_LIGA, null)).isEqualTo(PostContentType.LIGA);
    }

    @Test
    void copilotArtFollowsBorderColourAndTier() {
        var art = CopilotService.suggestArt(PostContentType.PARTIDA_MOMENTO, RarityTier.MYTHIC);
        assertThat(art.color()).isEqualTo("#3b82f6");
        assertThat(art.material()).isEqualTo("HOLOGRAFICO");
        assertThat(art.layout()).isEqualTo("HERO");
        assertThat(art.style()).isEqualTo("OURO");
        assertThat(CopilotService.suggestArt(PostContentType.TREINO, RarityTier.COMMON).material()).isEqualTo("FOSCO");
    }

    @Test
    void cardArtValidatesHexColour() {
        assertThat(new CardArt("#F5C542", CardMaterial.VIDRO, CardLayout.HERO, CardStyle.NEON).getColor()).isEqualTo("#f5c542");
        assertThat(new CardArt().isEmpty()).isTrue();
        assertThatThrownBy(() -> new CardArt("dourado", null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
