package com.footballai.social.service;

import com.footballai.social.model.enums.FeedWindow;
import com.footballai.social.model.enums.RarityTier;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/** RF3: parser dos filtros segmentados por janela. */
class FeedQueryTest {
    @Test
    void parsesScoutingAttributesCompareAndSource() {
        var p = new LinkedMultiValueMap<String, String>();
        p.add("attr[velocidade]", "gt:80"); p.add("attr[passe]", "gte:85"); p.add("attr[invalido]", "zz");
        p.add("compare", "11111111-1111-1111-1111-111111111111,22222222-2222-2222-2222-222222222222"); p.add("source", "HOMOLOGADO"); p.add("size", "500");
        FeedQuery q = FeedQuery.parse(FeedWindow.SCOUTING, p);
        assertThat(q.attributes).hasSize(2);
        assertThat(q.attributes.get(0)).isEqualTo(new FeedQuery.AttributeFilter("velocidade", "gt", 80));
        assertThat(q.compare).hasSize(2);
        assertThat(q.source).isNotNull();
        assertThat(q.size).isEqualTo(50);
        assertThat(q.applied()).containsKey("attr").containsKey("compare");
    }

    @Test
    void parsesPerfilTiersAndCommonFilters() {
        var p = new LinkedMultiValueMap<String, String>();
        p.add("tier", "LEGENDARY,MYTHIC"); p.add("tier", "epic"); p.add("position", "meia"); p.add("ageMin", "16"); p.add("ageMax", "23");
        p.add("role", "ATLETA"); p.add("state", "pr"); p.add("sort", "trending"); p.add("q", "preencher");
        FeedQuery q = FeedQuery.parse(FeedWindow.PERFIL, p);
        assertThat(q.tiers).containsExactly(RarityTier.LEGENDARY, RarityTier.MYTHIC, RarityTier.EPIC);
        assertThat(q.position.name()).isEqualTo("MEIA");
        assertThat(q.state).isEqualTo("PR");
        assertThat(q.sort.name()).isEqualTo("TRENDING");
        assertThat(q.ageMin).isEqualTo(16); assertThat(q.ageMax).isEqualTo(23);
        assertThat(q.cacheKey(null)).startsWith("feed:PERFIL:anon:");
    }

    @Test
    void alertaSourceIsARoleList() {
        var p = new LinkedMultiValueMap<String, String>();
        p.add("severity", "ALTA,MEDIA"); p.add("alertType", "LESAO"); p.add("source", "IMPRENSA,CLUBE"); p.add("contentType", "LESAO_ALERTA,especial");
        FeedQuery q = FeedQuery.parse(FeedWindow.ALERTA, p);
        assertThat(q.severities).hasSize(2); assertThat(q.alertTypes).hasSize(1); assertThat(q.sources).hasSize(2); assertThat(q.source).isNull();
        assertThat(q.contentTypes).hasSize(2); assertThat(q.applied()).containsKey("contentType");
    }
}
