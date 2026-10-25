package com.footballai.social.service;

import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.repository.DigitalTwinRepository;
import com.footballai.social.repository.WeeklyScoreSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** CORE: determinismo, pesos 30/40/30, faixas de tier e arredondamento HALF_EVEN. */
class RarityEngineServiceTest {

    private RarityEngineService engine;

    @BeforeEach
    void setUp() {
        engine = new RarityEngineService(mock(DigitalTwinRepository.class), mock(WeeklyScoreSnapshotRepository.class),
                (athleteId, week) -> null, mock(BadgeAwardService.class), mock(NotificationService.class), Clock.fixed(Instant.parse("2026-10-26T03:00:00Z"), ZoneOffset.UTC));
    }

    private static RarityEngineService.RarityInputs inputs(int planned, int done, String acwr, int readiness, String wRating, String wSum,
                                                           int matches, int minutes, String avgRating, int contrib, String bonus) {
        return new RarityEngineService.RarityInputs("2026-W43", planned, done, new BigDecimal(acwr), readiness, new BigDecimal(wRating), new BigDecimal(wSum),
                matches, minutes, new BigDecimal(avgRating), contrib, new BigDecimal(bonus));
    }

    @Test
    void sameInputsProduceSameScoreTierAndHash() {
        var in = inputs(5, 4, "1.10", 82, "25.6", "3.0", 2, 160, "7.8", 2, "50");
        var a = engine.calculate(in);
        var b = engine.calculate(inputs(5, 4, "1.10", 82, "25.6", "3.0", 2, 160, "7.8", 2, "50"));
        assertThat(a.scoreTotal()).isEqualTo(b.scoreTotal());
        assertThat(a.tier()).isEqualTo(b.tier());
        assertThat(a.inputsHash()).isEqualTo(b.inputsHash()).hasSize(64);
    }

    @Test
    void perfectInputsReachMaximumScalePlusBonus() {
        var r = engine.calculate(inputs(5, 5, "1.0", 100, "10", "1", 2, 180, "10", 10, "300"));
        assertThat(r.physicalScore()).isEqualByComparingTo("1.0000");
        assertThat(r.technicalScore()).isEqualByComparingTo("1.0000");
        assertThat(r.matchScore()).isEqualByComparingTo("1.0000");
        assertThat(r.scoreTotal()).isEqualTo(3300);
        assertThat(r.tier()).isEqualTo(RarityTier.MYTHIC);
    }

    @Test
    void emptyWeekScoresZeroAndCommon() {
        var r = engine.calculate(inputs(0, 0, "1.0", 0, "0", "0", 0, 0, "0", 0, "0"));
        assertThat(r.scoreTotal()).isZero();
        assertThat(r.tier()).isEqualTo(RarityTier.COMMON);
    }

    @Test
    void weightsAreThirtyFortyThirty() {
        // só a parcela técnica no máximo → 0.40 × 3000 = 1200 → EPIC
        var tech = engine.calculate(inputs(0, 0, "1.0", 0, "10", "1", 0, 0, "0", 0, "0"));
        assertThat(tech.scoreTotal()).isEqualTo(1200);
        assertThat(tech.tier()).isEqualTo(RarityTier.EPIC);
        // só a física no máximo → 0.30 × 3000 = 900 → RARE (limite superior)
        var phys = engine.calculate(inputs(5, 5, "1.0", 100, "0", "0", 0, 0, "0", 0, "0"));
        assertThat(phys.scoreTotal()).isEqualTo(900);
        assertThat(phys.tier()).isEqualTo(RarityTier.RARE);
        // só partidas no máximo → 900
        var match = engine.calculate(inputs(0, 0, "1.0", 0, "0", "0", 1, 90, "10", 10, "0"));
        assertThat(match.scoreTotal()).isEqualTo(900);
    }

    @Test
    void badgeBonusIsCappedAtThreeHundred() {
        var r = engine.calculate(inputs(0, 0, "1.0", 0, "0", "0", 0, 0, "0", 0, "9999"));
        assertThat(r.badgeBonus()).isEqualByComparingTo("300");
        assertThat(r.scoreTotal()).isEqualTo(300);
    }

    @Test
    void acwrOutsideSafeZonePenalisesLoadQuality() {
        var safe = engine.physicalScore(inputs(0, 0, "1.0", 100, "0", "0", 0, 0, "0", 0, "0"));
        var risky = engine.physicalScore(inputs(0, 0, "1.9", 100, "0", "0", 0, 0, "0", 0, "0"));
        assertThat(safe).isEqualByComparingTo("0.4000");
        assertThat(risky).isEqualByComparingTo("0.2000");
    }

    @Test
    void roundingIsHalfEven() {
        // 0.30 × 0.5 × 3000 = 450 exato; 0.30 × 0.50017 × 3000 = 450.153 → 450
        var r = engine.calculate(inputs(0, 0, "1.0", 0, "0", "0", 1, 45, "0", 0, "0"));
        assertThat(r.matchScore()).isEqualByComparingTo("0.2500");
        assertThat(r.scoreTotal()).isEqualTo(225);
    }
}
