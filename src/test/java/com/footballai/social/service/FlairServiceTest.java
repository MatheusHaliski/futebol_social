package com.footballai.social.service;

import com.footballai.social.controller.dto.FlairDtos.FlairCardDTO;
import com.footballai.social.model.enums.RarityTier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** RF6: odds visíveis e determinísticas, poder por tier/capitão, lesionado não pontua. */
class FlairServiceTest {
    private static FlairCardDTO card(String tier, int pp, int rp, int mp, int score, boolean captain) {
        return new FlairCardDTO(UUID.randomUUID(), UUID.randomUUID(), "<PREENCHER>", "MEIA", tier, "ic-tier-" + tier.toLowerCase(), "#fff", pp, rp, mp, score, 0, captain, "PACK", Instant.EPOCH);
    }

    @Test
    void displayedOddsAreTheRealOnesWhenTiersAreMissingFromThePool() {
        // pool com COMMON, RARE e EPIC só: LEGENDARY e MYTHIC saem do sorteio e os outros sobem na proporção (55:25:12)
        var weights = FlairService.effectiveWeights(java.util.EnumSet.of(RarityTier.COMMON, RarityTier.RARE, RarityTier.EPIC));
        var labels = FlairService.oddsLabels(weights);
        assertThat(labels).containsEntry("COMMON", "59,8%").containsEntry("RARE", "27,2%").containsEntry("EPIC", "13%")
                .containsEntry("LEGENDARY", "0%").containsEntry("MYTHIC", "0%");
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(total).isEqualTo(92);
        var counts = new java.util.EnumMap<RarityTier, Integer>(RarityTier.class);
        for (int roll = 0; roll < total; roll++) counts.merge(FlairService.drawTier(roll, weights), 1, Integer::sum);
        assertThat(counts).containsOnlyKeys(RarityTier.COMMON, RarityTier.RARE, RarityTier.EPIC)   // tier ausente nunca sai
                .containsEntry(RarityTier.COMMON, 55).containsEntry(RarityTier.RARE, 25).containsEntry(RarityTier.EPIC, 12);
        // pool completo: as odds nominais
        assertThat(FlairService.oddsLabels(FlairService.effectiveWeights(java.util.EnumSet.allOf(RarityTier.class))))
                .containsEntry("COMMON", "55%").containsEntry("MYTHIC", "2%");
        // só LEGENDARY e MYTHIC (o caso do estudo): 75% / 25%, nunca o pool inteiro escondido atrás de 55/25/12/6/2
        assertThat(FlairService.oddsLabels(FlairService.effectiveWeights(java.util.EnumSet.of(RarityTier.LEGENDARY, RarityTier.MYTHIC))))
                .containsEntry("LEGENDARY", "75%").containsEntry("MYTHIC", "25%").containsEntry("COMMON", "0%");
    }

    @Test
    void oddsSumToHundredAndDrawFollowsBands() {
        assertThat(FlairService.ODDS.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(100);
        assertThat(FlairService.drawTier(0)).isEqualTo(RarityTier.COMMON);
        assertThat(FlairService.drawTier(54)).isEqualTo(RarityTier.COMMON);
        assertThat(FlairService.drawTier(55)).isEqualTo(RarityTier.RARE);
        assertThat(FlairService.drawTier(80)).isEqualTo(RarityTier.EPIC);
        assertThat(FlairService.drawTier(92)).isEqualTo(RarityTier.LEGENDARY);
        assertThat(FlairService.drawTier(98)).isEqualTo(RarityTier.MYTHIC);
        assertThat(FlairService.drawTier(99)).isEqualTo(RarityTier.MYTHIC);
    }

    @Test
    void seedIsDeterministicPerUserAndDay() {
        assertThat(FlairService.seed("u|2026-10-24")).isEqualTo(FlairService.seed("u|2026-10-24"));
        assertThat(FlairService.seed("u|2026-10-24")).isNotEqualTo(FlairService.seed("u|2026-10-25"));
    }

    @Test
    void axisValueAppliesTierMultiplierCaptainBonusAndInjuryRule() {
        assertThat(FlairService.axisValue(card("COMMON", 80, 70, 60, 300, false), "PP")).isEqualTo(80);
        assertThat(FlairService.axisValue(card("MYTHIC", 80, 70, 60, 2500, false), "PP")).isEqualTo(104);   // ×1,30
        assertThat(FlairService.axisValue(card("LEGENDARY", 80, 70, 60, 1700, true), "RP")).isEqualTo(101); // 70 × 1,2 × 1,2 = 100,8 → HALF_EVEN
        assertThat(FlairService.axisValue(card("MYTHIC", 99, 99, 99, 0, true), "MP")).isZero();            // lesionado/pendente: score 0
    }
}
