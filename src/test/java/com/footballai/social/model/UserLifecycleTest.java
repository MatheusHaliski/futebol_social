package com.footballai.social.model;

import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.ProfileVisibility;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RF5: soft delete de 30 dias, cancelamento e anonimização; defaults de privacidade. */
class UserLifecycleTest {
    private final Instant now = Instant.parse("2026-10-24T12:00:00Z");

    @Test
    void newUserIsPrivateByDefault() {
        Athlete a = new Athlete();
        assertThat(a.getVisibility()).isEqualTo(ProfileVisibility.SOMENTE_EU);
        assertThat(a.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(a.ringColor()).isEqualTo("#00d47a");
    }

    @Test
    void deletionIsSoftWithThirtyDayGrace() {
        Athlete a = new Athlete();
        a.requestDeletion(now, 30);
        assertThat(a.getStatus()).isEqualTo(AccountStatus.PENDING_DELETE);
        assertThat(a.getScheduledPurgeAt()).isEqualTo(now.plus(30, ChronoUnit.DAYS));
        assertThat(a.isPurgeDue(now.plus(29, ChronoUnit.DAYS))).isFalse();
        assertThat(a.isPurgeDue(now.plus(30, ChronoUnit.DAYS))).isTrue();
        a.cancelDeletion();
        assertThat(a.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(a.getScheduledPurgeAt()).isNull();
    }

    @Test
    void cancelWithoutPendingDeletionFails() {
        assertThatThrownBy(() -> new Athlete().cancelDeletion()).isInstanceOf(IllegalStateException.class);
    }
}
