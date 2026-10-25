package com.footballai.social.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballai.social.TestProps;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.LgpdPreference;
import com.footballai.social.model.enums.TwinStatus;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** RF5: opt-in com step-up para dado sensível, trilha de consentimento, soft delete de 30 dias e anonimização. */
class LgpdServiceTest {
    private final Instant now = Instant.parse("2026-10-24T12:00:00Z");
    private UserRepository users; private UserLgpdPreferencesRepository prefs; private ConsentAuditLogRepository consents; private DigitalTwinRepository twins;
    private PostRepository posts; private InjuryRecordRepository injuries; private TrainingSummaryRepository training; private DataExportRequestRepository exports;
    private PostReactionRepository reactions; private RedisSessionService sessions; private StorageService storage;
    private LgpdService svc;
    private final UUID userId = UUID.randomUUID();
    private final AuthPrincipal principal = new AuthPrincipal(userId, UserRole.ATLETA, "sid", "jti", Instant.MAX);

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class); prefs = mock(UserLgpdPreferencesRepository.class); consents = mock(ConsentAuditLogRepository.class);
        twins = mock(DigitalTwinRepository.class); posts = mock(PostRepository.class); injuries = mock(InjuryRecordRepository.class);
        training = mock(TrainingSummaryRepository.class); exports = mock(DataExportRequestRepository.class); reactions = mock(PostReactionRepository.class);
        sessions = mock(RedisSessionService.class); storage = mock(StorageService.class);
        svc = new LgpdService(users, prefs, consents, mock(TermsAcceptanceRepository.class), twins, mock(WeeklyScoreSnapshotRepository.class), mock(AthleteBadgeRepository.class),
                injuries, posts, reactions, mock(TechnicalEvaluationRepository.class), training, exports, sessions, storage,
                mock(com.footballai.social.repository.FlairCardRepository.class), mock(com.footballai.social.repository.FlairMatchRepository.class),
                new org.springframework.transaction.support.TransactionTemplate(mock(org.springframework.transaction.PlatformTransactionManager.class)), new ObjectMapper(), TestProps.props(),
                Clock.fixed(now, ZoneOffset.UTC));
        when(prefs.findById(userId)).thenReturn(Optional.of(new UserLgpdPreferences(userId)));
        when(prefs.save(any())).thenAnswer(i -> i.getArgument(0));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        when(twins.save(any())).thenAnswer(i -> i.getArgument(0));
        when(exports.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void enablingSensitivePreferenceRequiresStepUp() {
        when(sessions.verifyStepUpToken(userId, null)).thenReturn(false);
        assertThatThrownBy(() -> svc.updatePreference(principal, LgpdPreference.SHARE_MEDICAL_DATA, true, null, "ip", "ua"))
                .isInstanceOf(ApiException.class).hasMessageContaining("Art. 11");
        verify(consents, never()).save(any());
    }

    @Test
    void preferenceChangeIsAuditedWithLegalBasis() {
        when(sessions.verifyStepUpToken(userId, "tok")).thenReturn(true);
        var dto = svc.updatePreference(principal, LgpdPreference.SHARE_MEDICAL_DATA, true, "tok", "ip", "ua");
        assertThat(dto.enabled()).isTrue();
        ArgumentCaptor<ConsentAuditLog> cap = ArgumentCaptor.forClass(ConsentAuditLog.class);
        verify(consents).save(cap.capture());
        assertThat(cap.getValue().isOldValue()).isFalse();
        assertThat(cap.getValue().isNewValue()).isTrue();
        assertThat(cap.getValue().getLgpdArticle().name()).isEqualTo("ART_11");
        // revogar nunca exige step-up (Art. 8º, §5º)
        svc.updatePreference(principal, LgpdPreference.SHARE_MEDICAL_DATA, false, null, "ip", "ua");
        verify(consents, times(2)).save(any());
    }

    @Test
    void deletionIsSoftScheduledThirtyDaysAndRevokesSessions() {
        Athlete u = new Athlete(); u.setStatus(AccountStatus.ACTIVE); ReflectionTestUtils.setField(u, "id", userId);
        when(users.findById(userId)).thenReturn(Optional.of(u));
        when(sessions.verifyStepUpToken(userId, "tok")).thenReturn(true);
        DigitalTwin twin = new DigitalTwin(userId); twin.transitionTo(TwinStatus.ACTIVE, now);
        when(twins.findByUserId(userId)).thenReturn(Optional.of(twin));

        var dto = svc.requestDeletion(principal, "tok");
        assertThat(dto.status()).isEqualTo("PENDING_DELETE");
        assertThat(dto.scheduledPurgeAt()).isEqualTo(now.plus(30, ChronoUnit.DAYS));
        assertThat(twin.getStatus()).isEqualTo(TwinStatus.PENDING_DELETE);
        verify(sessions).revokeAllSessions(userId);
        verify(users, never()).delete(any());

        var restored = svc.cancelDeletion(principal);
        assertThat(restored.status()).isEqualTo("ACTIVE");
        assertThat(twin.getStatus()).isEqualTo(TwinStatus.ACTIVE);
    }

    @Test
    void anonymizeOnlyAfterGraceAndStripsPii() {
        Athlete u = new Athlete(); u.setStatus(AccountStatus.ACTIVE); u.setEmail("<PREENCHER_EMAIL>"); u.setUsername("atleta1"); u.setDisplayName("Nome");
        ReflectionTestUtils.setField(u, "id", userId);
        u.requestDeletion(now.minus(31, ChronoUnit.DAYS), 30);
        when(users.findById(userId)).thenReturn(Optional.of(u));
        when(posts.findAllByAuthorId(userId)).thenReturn(List.of());
        when(training.findAllByAthleteIdOrderByIsoWeekDesc(userId)).thenReturn(List.of());
        when(exports.findAllByUserIdOrderByRequestedAtDesc(userId)).thenReturn(List.of());
        when(twins.findByUserId(userId)).thenReturn(Optional.empty());

        svc.anonymize(userId);
        assertThat(u.getStatus()).isEqualTo(AccountStatus.ANONYMIZED);
        assertThat(u.getUsername()).startsWith("anon_");
        assertThat(u.getDisplayName()).isEqualTo("Usuário removido");
        verify(injuries).deleteAllByAthleteId(userId);
        verify(sessions).revokeAllSessions(userId);

        Athlete fresh = new Athlete(); fresh.setStatus(AccountStatus.ACTIVE); fresh.requestDeletion(now, 30);
        UUID other = UUID.randomUUID();
        when(users.findById(other)).thenReturn(Optional.of(fresh));
        assertThatThrownBy(() -> svc.anonymize(other)).isInstanceOf(ApiException.class).hasMessageContaining("30 dias");
    }
}
