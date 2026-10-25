package com.footballai.social.model;

import com.footballai.social.model.entity.DigitalTwin;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.model.enums.TwinStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DigitalTwinStateMachineTest {
    private final Instant now = Instant.parse("2026-10-24T12:00:00Z");

    @Test
    void happyPathDraftActiveSuspendedPendingDeleteAnonymized() {
        DigitalTwin t = new DigitalTwin(UUID.randomUUID());
        assertThat(t.getStatus()).isEqualTo(TwinStatus.DRAFT);
        t.transitionTo(TwinStatus.ACTIVE, now);
        t.transitionTo(TwinStatus.SUSPENDED, now);
        t.transitionTo(TwinStatus.PENDING_DELETE, now);
        t.transitionTo(TwinStatus.ANONYMIZED, now);
        assertThat(t.getStatus().isTerminal()).isTrue();
        assertThat(t.getActivatedAt()).isEqualTo(now);
    }

    @Test
    void pendingDeleteCanBeCancelledBackToActive() {
        DigitalTwin t = new DigitalTwin(UUID.randomUUID());
        t.transitionTo(TwinStatus.ACTIVE, now);
        t.transitionTo(TwinStatus.PENDING_DELETE, now);
        t.transitionTo(TwinStatus.ACTIVE, now);
        assertThat(t.getStatus()).isEqualTo(TwinStatus.ACTIVE);
    }

    @Test
    void invalidTransitionsAreRejected() {
        DigitalTwin t = new DigitalTwin(UUID.randomUUID());
        assertThatThrownBy(() -> t.transitionTo(TwinStatus.ANONYMIZED, now)).isInstanceOf(InvalidTwinTransitionException.class);
        t.transitionTo(TwinStatus.ACTIVE, now);
        assertThatThrownBy(() -> t.transitionTo(TwinStatus.DRAFT, now)).isInstanceOf(InvalidTwinTransitionException.class);
        t.transitionTo(TwinStatus.PENDING_DELETE, now);
        t.transitionTo(TwinStatus.DELETED, now);
        assertThatThrownBy(() -> t.transitionTo(TwinStatus.ACTIVE, now)).isInstanceOf(InvalidTwinTransitionException.class);
    }

    @Test
    void weeklyScoreUpdatesTierDeterministically() {
        DigitalTwin t = new DigitalTwin(UUID.randomUUID());
        t.applyWeeklyScore(1650, "2026-W43");
        assertThat(t.getRarityTier()).isEqualTo(RarityTier.LEGENDARY);
        assertThat(t.getLastCalculatedWeek()).isEqualTo("2026-W43");
    }
}
