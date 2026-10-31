package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CopilotDtos.SuggestRequest;
import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.entity.TrainingSummary;
import com.footballai.social.model.entity.UserLgpdPreferences;
import com.footballai.social.model.enums.LgpdPreference;
import com.footballai.social.model.enums.PostContentType;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** P0 #7: o Copilot exige o consentimento de IA e não publica prontidão (dado de saúde) sem shareMedicalData. */
class CopilotConsentTest {
    private final UserRepository users = mock(UserRepository.class);
    private final TrainingSummaryRepository training = mock(TrainingSummaryRepository.class);
    private final MatchPerformanceRepository matches = mock(MatchPerformanceRepository.class);
    private final DigitalTwinRepository twins = mock(DigitalTwinRepository.class);
    private final AthleteBadgeRepository badges = mock(AthleteBadgeRepository.class);
    private final UserLgpdPreferencesRepository prefs = mock(UserLgpdPreferencesRepository.class);
    private final CopilotService copilot = new CopilotService(users, training, matches, twins, badges, prefs);
    private final UUID me = UUID.randomUUID();
    private final AuthPrincipal p = new AuthPrincipal(me, UserRole.ATLETA, "s", "j", Instant.MAX);
    private final SuggestRequest treino = new SuggestRequest(null, PostContentType.TREINO, null);

    CopilotConsentTest() {
        Athlete a = new Athlete(); ReflectionTestUtils.setField(a, "id", me); a.setBirthDate(LocalDate.of(2000, 1, 1));
        when(users.findById(me)).thenReturn(Optional.of(a));
        when(twins.findByUserId(any())).thenReturn(Optional.empty());
        when(badges.findAllByAthleteIdAndActiveTrue(any())).thenReturn(List.of());
        TrainingSummary ts = new TrainingSummary(); ts.setAthleteId(me); ts.setIsoWeek("2026-W44"); ts.setSessionsDone(4); ts.setSessionsPlanned(5);
        ts.setLoadAu(new BigDecimal("480")); ts.setReadiness(78);
        when(training.findFirstByAthleteIdOrderByIsoWeekDesc(me)).thenReturn(Optional.of(ts));
    }

    private void consent(boolean ai, boolean medical) {
        UserLgpdPreferences pr = new UserLgpdPreferences(me);
        pr.set(LgpdPreference.AI_AGENT_PROCESSING, ai, Instant.now());
        pr.set(LgpdPreference.SHARE_MEDICAL_DATA, medical, Instant.now());
        when(prefs.findById(me)).thenReturn(Optional.of(pr));
    }

    @Test
    void suggestionRequiresAiConsent() {
        when(prefs.findById(me)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> copilot.suggest(p, treino)).isInstanceOf(ApiException.class).hasMessageContaining("Processamento por agentes de IA");
        consent(false, true);
        assertThatThrownBy(() -> copilot.suggest(p, treino)).isInstanceOf(ApiException.class);
    }

    @Test
    void readinessOnlyGoesPublicWithMedicalConsent() {
        consent(true, false);
        assertThat(copilot.suggest(p, treino).subtitle()).contains("Carga 480 AU").doesNotContain("prontidão").doesNotContain("78");
        consent(true, true);
        assertThat(copilot.suggest(p, treino).subtitle()).contains("prontidão 78%");
    }
}
