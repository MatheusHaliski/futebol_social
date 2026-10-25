package com.footballai.social.model;

import com.footballai.social.model.enums.RarityTier;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RarityTierTest {
    @Test
    void boundariesMatchSpecification() {
        assertThat(RarityTier.fromScore(0)).isEqualTo(RarityTier.COMMON);
        assertThat(RarityTier.fromScore(400)).isEqualTo(RarityTier.COMMON);
        assertThat(RarityTier.fromScore(401)).isEqualTo(RarityTier.RARE);
        assertThat(RarityTier.fromScore(900)).isEqualTo(RarityTier.RARE);
        assertThat(RarityTier.fromScore(901)).isEqualTo(RarityTier.EPIC);
        assertThat(RarityTier.fromScore(1600)).isEqualTo(RarityTier.EPIC);
        assertThat(RarityTier.fromScore(1601)).isEqualTo(RarityTier.LEGENDARY);
        assertThat(RarityTier.fromScore(2400)).isEqualTo(RarityTier.LEGENDARY);
        assertThat(RarityTier.fromScore(2401)).isEqualTo(RarityTier.MYTHIC);
        assertThat(RarityTier.fromScore(99999)).isEqualTo(RarityTier.MYTHIC);
    }

    @Test
    void negativeScoreIsRejected() {
        assertThatThrownBy(() -> RarityTier.fromScore(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyTierHasAUniqueCrestIconInsteadOfText() {
        assertThat(Arrays.stream(RarityTier.values()).map(RarityTier::crestIcon).distinct()).hasSize(5);
        assertThat(RarityTier.MYTHIC.crestIcon()).isEqualTo("ic-tier-mythic");
    }
}
