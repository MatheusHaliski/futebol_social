package com.footballai.social.service;

import com.footballai.social.model.entity.Post;
import com.footballai.social.model.enums.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RF3: classificação estrita (bolinha + borda), título obrigatório, age-chip e tendência. */
class PostRulesTest {

    @Test
    void roleCapabilityMatrix() {
        assertThat(UserRole.ATLETA.canPublish(PostContentType.LESAO_ALERTA, VerificationLevel.COMUNIDADE)).isTrue();
        assertThat(UserRole.STAFF_SAUDE.canPublish(PostContentType.LESAO_ALERTA, VerificationLevel.COMUNIDADE)).isFalse();
        assertThat(UserRole.STAFF_SAUDE.canPublish(PostContentType.LESAO_ALERTA, VerificationLevel.VERIFICADO)).isTrue();
        assertThat(UserRole.IMPRENSA.canPublish(PostContentType.LESAO_ALERTA, VerificationLevel.HOMOLOGADO)).isFalse();
        assertThat(UserRole.TORCEDOR.canPublish(PostContentType.INSTITUCIONAL, null)).isFalse();
        assertThat(UserRole.CLUBE.canPublish(PostContentType.INSTITUCIONAL, null)).isTrue();
        assertThat(UserRole.AGENTE_IA.isAutomated()).isTrue();
    }

    @Test
    void sevenBordersWithColoursGlyphsAndDefaultWindows() {
        assertThat(PostContentType.values()).hasSize(7);
        assertThat(PostContentType.LESAO_ALERTA.sensitive()).isTrue();
        assertThat(PostContentType.LESAO_ALERTA.hazard()).isTrue();
        assertThat(PostContentType.TREINO.defaultWindow()).isEqualTo(FeedWindow.GEMEO);
        assertThat(PostContentType.RELATORIO_TATICO.vslGlyph().glyph()).isEqualTo("▭");
        assertThat(PostContentType.ESPECIAL.borderColor()).isEqualTo("#f5c542");
    }

    @Test
    void prePersistEnforcesTitleSensitiveRatingAndDefaultWindow() throws Exception {
        Method pre = Post.class.getDeclaredMethod("prePersist"); pre.setAccessible(true);
        Post p = new Post();
        p.setAuthorId(UUID.randomUUID()); p.setAuthorRole(UserRole.ATLETA); p.setContentType(PostContentType.LESAO_ALERTA);
        assertThatThrownBy(() -> { try { pre.invoke(p); } catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); } }).isInstanceOf(IllegalStateException.class);
        p.setTitle("Lesão grau 2 na coxa direita confirmada"); p.setSubtitle("teaser"); p.setMediaKeys("[\"m/1.jpg\"]");
        pre.invoke(p);
        assertThat(p.getAgeRating()).isEqualTo(AgeRating.SENSITIVE);
        assertThat(p.getWindow()).isEqualTo(FeedWindow.ALERTA);
        assertThat(p.getSubtitle()).isNull(); // posts com mídia não exibem subtítulo
    }

    @Test
    void titleWordRuleAndTrend() {
        assertThat(PostService.wordCount("Comparador: Atacantes Sub-20 PR — Top 5")).isEqualTo(7);
        assertThat(PostService.wordCount("  ")).isZero();
        assertThat(TrendDirection.fromDelta(1.4)).isEqualTo(TrendDirection.HOT);
        assertThat(TrendDirection.fromDelta(0.2)).isEqualTo(TrendDirection.UP);
        assertThat(TrendDirection.fromDelta(0.0)).isEqualTo(TrendDirection.FLAT);
        assertThat(TrendDirection.fromDelta(-0.5)).isEqualTo(TrendDirection.DOWN);
        assertThat(AgeRating.SENSITIVE.label()).isEqualTo("SENSÍVEL");
    }
}
