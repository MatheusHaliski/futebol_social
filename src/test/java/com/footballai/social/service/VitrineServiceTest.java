package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.VitrineDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.ContractStatus;
import com.footballai.social.model.enums.LgpdPreference;
import com.footballai.social.model.enums.Position;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Vitrine do Jogador: totais sem zeros inventados, atributos do momento explicados, forma, e o Copilot com consentimento do atleta. */
class VitrineServiceTest {
    private final MatchPerformanceRepository matches = mock(MatchPerformanceRepository.class);
    private final ScoutingAttributeRepository scouting = mock(ScoutingAttributeRepository.class);
    private final TechnicalEvaluationRepository evaluations = mock(TechnicalEvaluationRepository.class);
    private final DigitalTwinRepository twins = mock(DigitalTwinRepository.class);
    private final WeeklyScoreSnapshotRepository snapshots = mock(WeeklyScoreSnapshotRepository.class);
    private final UserLgpdPreferencesRepository prefs = mock(UserLgpdPreferencesRepository.class);
    private final CardAssembler assembler = mock(CardAssembler.class);
    private final AccessPolicy access = mock(AccessPolicy.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);
    private final VitrineService service = new VitrineService(matches, scouting, evaluations, twins, snapshots, prefs, assembler, access, clock);

    private final UUID athleteId = UUID.randomUUID();
    private final AuthPrincipal owner = new AuthPrincipal(athleteId, UserRole.ATLETA, "s", "j", Instant.MAX);
    private final AuthPrincipal scout = new AuthPrincipal(UUID.randomUUID(), UserRole.SCOUT, "s", "j", Instant.MAX);

    private static MatchPerformance match(LocalDate date, boolean official, int minutes, int goals, int assists, int pc, int pa, int dc, int da, String rating) {
        MatchPerformance m = new MatchPerformance();
        m.setMatchDate(date); m.setOfficial(official); m.setMinutesPlayed(minutes); m.setGoals(goals); m.setAssists(assists);
        m.setPassesCompleted(pc); m.setPassesAttempted(pa); m.setDribblesCompleted(dc); m.setDribblesAttempted(da);
        m.setRating(rating == null ? null : new BigDecimal(rating)); m.setSeason(String.valueOf(date.getYear()));
        return m;
    }

    private static ScoutingAttribute week(String isoWeek, int velocidade, int passe, int finalizacao) {
        ScoutingAttribute s = new ScoutingAttribute();
        s.setIsoWeek(isoWeek); s.setVelocidade(BigDecimal.valueOf(velocidade)); s.setPasse(BigDecimal.valueOf(passe)); s.setFinalizacao(BigDecimal.valueOf(finalizacao));
        s.setDrible(BigDecimal.valueOf(70)); s.setDesarme(BigDecimal.valueOf(40)); s.setPosicionamento(BigDecimal.valueOf(72)); s.setResistencia(BigDecimal.valueOf(75));
        return s;
    }

    private Athlete athlete(int age) {
        Athlete a = new Athlete();
        ReflectionTestUtils.setField(a, "id", athleteId);
        a.setDisplayName("<PREENCHER_ATLETA>"); a.setBirthDate(LocalDate.now().minusYears(age).minusDays(10));
        a.setPosition(Position.MEIA); a.setContractStatus(ContractStatus.AGENTE_LIVRE);
        when(access.requireProfile(any(), eq(athleteId), any())).thenReturn(a);
        return a;
    }

    private void aiConsent(boolean on) {
        UserLgpdPreferences pr = new UserLgpdPreferences(athleteId);
        pr.set(LgpdPreference.AI_AGENT_PROCESSING, on, Instant.now());
        when(prefs.findById(athleteId)).thenReturn(Optional.of(pr));
    }

    /** 10 partidas oficiais: as 5 antigas com 80 % de passes e nota 6,5; as 5 recentes com 90 % de passes, 1 gol cada e nota 7,6. */
    private List<MatchPerformance> tenMatches() {
        List<MatchPerformance> ms = new ArrayList<>();
        LocalDate d = LocalDate.of(2026, 8, 1);
        for (int i = 0; i < 5; i++) ms.add(match(d.plusWeeks(i), true, 90, 0, 1, 40, 50, 3, 6, "6.50"));
        for (int i = 5; i < 10; i++) ms.add(match(d.plusWeeks(i), true, 90, 1, 0, 45, 50, 4, 6, "7.60"));
        return ms;
    }

    @Test
    void totalsComputeAccuracyAndPer90WithoutInventingZeros() {
        TotalsDTO t = VitrineService.totals(List.of(
                match(LocalDate.of(2026, 9, 1), true, 90, 2, 1, 30, 40, 3, 5, "7.50"),
                match(LocalDate.of(2026, 9, 8), false, 90, 0, 0, 0, 0, 0, 0, null)));   // autorrelato antigo, sem passes/dribles registrados
        assertThat(t.games()).isEqualTo(2);
        assertThat(t.officialGames()).isEqualTo(1);
        assertThat(t.passAccuracy()).isEqualByComparingTo("75.0");
        assertThat(t.dribbleSuccess()).isEqualByComparingTo("60.0");
        assertThat(t.goalsPer90()).isEqualByComparingTo("1.00");      // 2 gols em 180 min
        assertThat(t.avgRating()).isEqualByComparingTo("7.50");       // só partidas com nota entram na média

        TotalsDTO empty = VitrineService.totals(List.of(match(LocalDate.of(2026, 9, 1), true, 0, 0, 0, 0, 0, 0, 0, null)));
        assertThat(empty.passAccuracy()).isNull();                    // sem tentativa: sem aproveitamento (nunca 0 %)
        assertThat(empty.dribbleSuccess()).isNull();
        assertThat(empty.goalsPer90()).isNull();
        assertThat(empty.avgRating()).isNull();
    }

    @Test
    void liveAttributesBlendWeeklyScoutingWithRecentFormAndExplainIt() {
        List<MatchPerformance> ms = tenMatches();
        TotalsDTO prior = VitrineService.totals(ms.subList(0, 5)), recent = VitrineService.totals(ms.subList(5, 10));
        List<LiveAttributeDTO> attrs = VitrineService.liveAttributes(List.of(week("2026-W43", 85, 78, 70), week("2026-W44", 87, 80, 72)), prior, recent);
        Map<String, LiveAttributeDTO> byKey = attrs.stream().collect(Collectors.toMap(LiveAttributeDTO::key, a -> a));

        assertThat(attrs).extracting(LiveAttributeDTO::label).containsExactly("Velocidade", "Chute", "Passe", "Drible", "Desarme", "Posicionamento", "Resistência");
        LiveAttributeDTO vel = byKey.get("velocidade");                // a súmula não mede velocidade: vale o scouting da semana
        assertThat(vel.value()).isEqualTo(87);
        assertThat(vel.weeklyDelta()).isEqualTo(2);
        assertThat(vel.formAdjustment()).isZero();
        assertThat(vel.trend()).isEqualTo("UP");
        assertThat(vel.history()).containsExactly(85, 87);
        assertThat(vel.reason()).contains("+2 no scouting (W43 → W44)");

        LiveAttributeDTO passe = byKey.get("passe");                   // 90 % recentes vs 80 % antes: +10 p.p. ÷ 2 = +5
        assertThat(passe.base()).isEqualTo(80);
        assertThat(passe.formAdjustment()).isEqualTo(5);
        assertThat(passe.value()).isEqualTo(85);
        assertThat(passe.reason()).contains("forma +5").contains("passes certos 90% nas últimas 5 vs 80% antes");

        LiveAttributeDTO chute = byKey.get("finalizacao");             // 1,00 gol/90 vs 0,00: Δ × 10 = 10 → limitado a +5
        assertThat(chute.label()).isEqualTo("Chute");
        assertThat(chute.formAdjustment()).isEqualTo(VitrineService.MAX_FORM_ADJUSTMENT);
        assertThat(chute.value()).isEqualTo(77);
    }

    @Test
    void shortHistoryUsesOnlyTheScoutingWeek() {
        List<MatchPerformance> ms = tenMatches().subList(7, 10);       // só 3 partidas: nada "antes" para comparar
        TotalsDTO prior = VitrineService.totals(List.of()), recent = VitrineService.totals(ms);
        List<LiveAttributeDTO> attrs = VitrineService.liveAttributes(List.of(week("2026-W44", 99, 99, 99)), prior, recent);
        assertThat(attrs).allSatisfy(a -> { assertThat(a.formAdjustment()).isZero(); assertThat(a.previous()).isNull(); assertThat(a.value()).isLessThanOrEqualTo(100); });
        assertThat(VitrineService.liveAttributes(List.of(), prior, recent)).isEmpty();
    }

    @Test
    void formComparesLastFiveRatedMatchesWithThePreviousOnes() {
        List<MatchPerformance> ms = tenMatches();
        FormDTO up = VitrineService.form(ms);
        assertThat(up.trend()).isEqualTo("UP");
        assertThat(up.label()).isEqualTo("Em alta");
        assertThat(up.recentAvgRating()).isEqualByComparingTo("7.60");
        assertThat(up.referenceAvgRating()).isEqualByComparingTo("6.50");
        assertThat(up.lastRatings()).hasSize(5);

        List<MatchPerformance> reversed = new ArrayList<>(ms.subList(5, 10));
        reversed.addAll(ms.subList(0, 5));
        assertThat(VitrineService.form(reversed).trend()).isEqualTo("DOWN");
        assertThat(VitrineService.form(List.of()).sample()).isZero();
    }

    @Test
    void copilotNeedsTheAthletesAiConsentAndSkipsMinors() {
        athlete(22);
        when(prefs.findById(athleteId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pitch(scout, athleteId)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("ATHLETE_AI_OPT_OUT");
        assertThatThrownBy(() -> service.pitch(owner, athleteId)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("AI_PROCESSING_OPT_IN");

        athlete(16);
        aiConsent(true);
        assertThatThrownBy(() -> service.pitch(scout, athleteId)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("COPILOT_MINOR");
    }

    @Test
    void hiddenProfileIsNotFoundForPanelAndCopilot() {
        when(access.requireProfile(any(), eq(athleteId), any())).thenThrow(ApiException.notFound("Atleta"));
        assertThatThrownBy(() -> service.vitrine(scout, athleteId)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("NOT_FOUND");
        assertThatThrownBy(() -> service.pitch(scout, athleteId)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("NOT_FOUND");
    }

    @Test
    void pitchAnswersWhyHireWithEvidenceAndNoHealthData() {
        athlete(22);
        aiConsent(true);
        when(matches.findAllByAthleteIdOrderByMatchDateAsc(athleteId)).thenReturn(tenMatches());
        when(scouting.findTop12ByAthleteIdOrderByIsoWeekDesc(athleteId)).thenReturn(List.of(week("2026-W44", 87, 82, 72), week("2026-W43", 85, 80, 70)));
        when(twins.findByUserId(athleteId)).thenReturn(Optional.empty());
        when(evaluations.findAllByAthleteIdAndWeekOfBetween(any(), any(), any())).thenReturn(List.of());

        PitchDTO p = service.pitch(scout, athleteId);
        assertThat(p.question()).isEqualTo("Por que contratar este jogador?");
        assertThat(p.headline()).startsWith("Meia de 22 anos, agente livre, com");
        assertThat(p.reasons()).isNotEmpty().hasSizeLessThanOrEqualTo(VitrineService.MAX_REASONS);
        assertThat(p.reasons()).extracting(ReasonDTO::title).contains("Produção ofensiva", "Passe confiável", "Momento em alta");
        assertThat(p.reasons()).allSatisfy(r -> assertThat(r.evidence()).isNotBlank());
        assertThat(p.answer()).startsWith("Porque <PREENCHER_ATLETA> reúne");
        assertThat(p.confidence()).isEqualTo("ALTA");                  // 10 súmulas oficiais + scouting
        assertThat(p.sources()).contains("10 súmulas oficiais", "Scouting W44");
        assertThat(p.disclaimer()).contains("LGPD Art. 11").contains("LGPD Art. 20");
        assertThat(p.aiGenerated()).isTrue();
        String all = String.join(" ", p.headline(), p.answer(), String.valueOf(p.reasons()), String.valueOf(p.attention()));
        assertThat(all).doesNotContainIgnoringCase("prontidão").doesNotContainIgnoringCase("lesão").doesNotContain("ACWR");
    }

    @Test
    void panelFlagsCopilotAvailabilityAndChangesRevisionWithNewData() {
        athlete(22);
        aiConsent(false);
        List<MatchPerformance> ms = new ArrayList<>(tenMatches());
        when(matches.findAllByAthleteIdOrderByMatchDateAsc(athleteId)).thenReturn(ms);
        when(scouting.findTop12ByAthleteIdOrderByIsoWeekDesc(athleteId)).thenReturn(List.of(week("2026-W44", 87, 82, 72)));
        when(twins.findByUserId(athleteId)).thenReturn(Optional.empty());

        VitrineDTO v = service.vitrine(scout, athleteId);
        assertThat(v.copilot().available()).isFalse();
        assertThat(v.copilot().code()).isEqualTo("ATHLETE_AI_OPT_OUT");
        assertThat(v.career().games()).isEqualTo(10);
        assertThat(v.recentMatches()).hasSize(VitrineService.HISTORY_MATCHES).first().extracting(MatchLineDTO::date).isEqualTo(LocalDate.of(2026, 8, 1).plusWeeks(9));
        assertThat(v.seasons()).extracting(SeasonLineDTO::season).containsExactly("2026");
        assertThat(v.refreshSeconds()).isPositive();

        ms.add(match(LocalDate.of(2026, 10, 30), true, 90, 2, 0, 40, 44, 5, 6, "8.40"));
        assertThat(service.vitrine(scout, athleteId).revision()).isNotEqualTo(v.revision());
    }

    @Test
    void matchSheetNeverHasMoreCompletedThanAttempted() {
        assertThat(PerformanceService.completedAttempted(null, null, "passes")).containsExactly(0, 0);
        assertThat(PerformanceService.completedAttempted(12, null, "passes")).containsExactly(12, 12);
        assertThat(PerformanceService.completedAttempted(30, 41, "passes")).containsExactly(30, 41);
        assertThatThrownBy(() -> PerformanceService.completedAttempted(9, 6, "dribles")).isInstanceOf(ApiException.class).extracting("code").isEqualTo("MATCH_INVALID");
    }
}
