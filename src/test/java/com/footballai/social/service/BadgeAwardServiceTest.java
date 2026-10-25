package com.footballai.social.service;

import com.footballai.social.model.entity.AthleteBadge;
import com.footballai.social.model.enums.BadgeType;
import com.footballai.social.model.enums.PhysicalStatus;
import com.footballai.social.model.enums.Position;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** RF4.1: status físico exclusivo, regras de badges, top 3 com DM primeiro e sigilo médico. */
class BadgeAwardServiceTest {
    private final UUID id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-24T12:00:00Z");

    private AthleteBadge b(BadgeType t, int counter) { return new AthleteBadge(id, t, counter, "2026", now, null); }

    @Test
    void physicalStatusIsMutuallyExclusiveWithInjuryFirst() {
        assertThat(BadgeAwardService.derivePhysicalStatus(true, new BigDecimal("1.0"), 100)).isEqualTo(PhysicalStatus.INJURED);
        assertThat(BadgeAwardService.derivePhysicalStatus(false, new BigDecimal("1.62"), 90)).isEqualTo(PhysicalStatus.FATIGUE_RISK);
        assertThat(BadgeAwardService.derivePhysicalStatus(false, new BigDecimal("1.0"), 55)).isEqualTo(PhysicalStatus.FATIGUE_RISK);
        assertThat(BadgeAwardService.derivePhysicalStatus(false, new BigDecimal("1.1"), 85)).isEqualTo(PhysicalStatus.READY);
        assertThat(PhysicalStatus.INJURED.blocksCallUp()).isTrue();
        assertThat(PhysicalStatus.READY.blocksCallUp()).isFalse();
    }

    @Test
    void seasonBadgesFollowDeterministicRules() {
        var stats = new BadgeAwardService.SeasonStats(22, 14, 7, 1980, 1, 0, 0, 3, 2, 4, new BigDecimal("7.9"));
        Map<BadgeType, Integer> due = BadgeAwardService.seasonBadges(stats, Position.MEIA, true);
        assertThat(due).containsEntry(BadgeType.ARTILHEIRO, 14).containsEntry(BadgeType.GARCOM, 7).containsEntry(BadgeType.MOTORZINHO, 1980)
                .containsEntry(BadgeType.MVP, 2).containsEntry(BadgeType.EM_CHAMAS, 4).containsKey(BadgeType.FAIR_PLAY).containsKey(BadgeType.LIDER_CAPITAO)
                .doesNotContainKey(BadgeType.PAREDAO).doesNotContainKey(BadgeType.XERIFAO);
        var gk = BadgeAwardService.seasonBadges(new BadgeAwardService.SeasonStats(23, 0, 0, 1980, 1, 0, 5, 0, 0, 0, null), Position.GOLEIRO, false);
        assertThat(gk).containsEntry(BadgeType.PAREDAO, 5);
    }

    @Test
    void topThreeNeverExceedsThreeAndInjuredComesFirst() {
        List<AthleteBadge> all = List.of(b(BadgeType.ARTILHEIRO, 14), b(BadgeType.MVP, 3), b(BadgeType.DEPARTAMENTO_MEDICO, 0), b(BadgeType.GARCOM, 7), b(BadgeType.MOTORZINHO, 1980));
        List<AthleteBadge> top = BadgeAwardService.top3(all, true);
        assertThat(top).hasSize(3);
        assertThat(top.get(0).getBadgeType()).isEqualTo(BadgeType.DEPARTAMENTO_MEDICO);
        assertThat(top.get(1).getBadgeType()).isEqualTo(BadgeType.MVP);
    }

    @Test
    void medicalBadgesAreHiddenWithoutShareMedicalData() {
        List<AthleteBadge> all = List.of(b(BadgeType.DEPARTAMENTO_MEDICO, 0), b(BadgeType.ARTILHEIRO, 14));
        assertThat(BadgeAwardService.top3(all, false)).extracting(AthleteBadge::getBadgeType).containsExactly(BadgeType.ARTILHEIRO);
    }

    @Test
    void bonusIsCapped() {
        BadgeAwardService svc = new BadgeAwardService(null, null, null, null, null, null, null);
        assertThat(svc.badgeBonus(List.of(b(BadgeType.MVP, 20)))).isEqualByComparingTo("300");
        assertThat(svc.badgeBonus(List.of(b(BadgeType.MVP, 2), b(BadgeType.FAIR_PLAY, 0)))).isEqualByComparingTo("65");
    }
}
