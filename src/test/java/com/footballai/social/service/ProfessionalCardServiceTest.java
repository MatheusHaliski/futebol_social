package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ProfessionalCardServiceTest {
    private final LocalDate today=LocalDate.now(Clock.systemUTC());
    private static <T extends User> T identified(T user) {ReflectionTestUtils.setField(user,"id",UUID.randomUUID());user.setDisplayName("Perfil esportivo");user.setUsername("perfil");return user;}
    private MatchPerformance match(boolean official) {MatchPerformance m=new MatchPerformance();m.setMatchDate(today);m.setMinutesPlayed(90);m.setCompetition("Campeonato estadual");m.setOfficial(official);m.setSeason(IsoWeeks.season(today));return m;}

    @Test void absentMatchEvidenceDoesNotProduceFakeZeroMetricsOrRatings() {
        Athlete a=identified(new Athlete());a.setPosition(Position.ATACANTE);
        var card=ProfessionalCardService.athlete(a,List.of(),today);
        assertThat(card.metrics()).isEmpty();assertThat(card.evidence()).contains("Sem partidas");
        assertThat(card.context().asOf()).isNull();
    }
    @Test void goalkeeperSummaryUsesCleanSheetsWithMinutesAndOriginInsteadOfGoalScoring() {
        Athlete a=identified(new Athlete());a.setPosition(Position.GOLEIRO);
        MatchPerformance m=match(true);m.setCleanSheet(true);
        var card=ProfessionalCardService.athlete(a,List.of(m),today);
        assertThat(card.metrics().subList(0,3)).extracting(f->f.key()).containsExactly("minutes","cleanSheets","games");
        assertThat(card.metrics().get(1).value()).isEqualTo("1");assertThat(card.evidence()).contains("1 oficiais","0 autorrelatadas");
    }
    @Test void defenderSummarySeparatesRecordedTacklesFromARealZero() {
        Athlete a=identified(new Athlete());a.setPosition(Position.ZAGUEIRO);
        var card=ProfessionalCardService.athlete(a,List.of(match(false)),today);
        assertThat(card.metrics()).anySatisfy(f->{assertThat(f.key()).isEqualTo("tackles");assertThat(f.value()).isEqualTo("0");});
        assertThat(card.metrics()).noneMatch(f->f.key().equals("rating"));assertThat(card.evidence()).contains("1 autorrelatadas");
    }
    @Test void youthSummaryKeepsAgeMinimizedAndNeverProjectsHealth() {
        Athlete a=identified(new Athlete());a.setBirthDate(today.minusYears(16));
        var card=ProfessionalCardService.athlete(a,List.of(),today);
        assertThat(card.facts()).anyMatch(f->f.key().equals("age")&&f.value().equals("SUB-17"));
        assertThat(card.facts()).noneMatch(f->f.value().contains("16 anos")||Set.of("readiness","injury","medical","email").contains(f.key()));
    }
    @Test void managerSummaryUsesOnlyVisibleCurrentMandatesAndDeclaredTitles() {
        Manager m=identified(new Manager());m.setHasCurrentClub(false);m.setTransferSituation(TransferSituation.FREE_AGENT);
        Club c=identified(new Club());ClubManagerTenure t=new ClubManagerTenure();t.managerUserId=m.getId();t.clubId=c.getId();t.startDate=today.minusDays(1);t.teamId=UUID.randomUUID();
        var hidden=ProfessionalCardService.manager(m,List.of(t),Map.of(),List.of(),List.of(),today);
        assertThat(hidden.membership()).isEqualTo("Com clube");assertThat(hidden.affiliation()).isNull();assertThat(hidden.transferSituation()).isEqualTo("NOT_FOR_SALE");
        assertThat(hidden.metrics()).anySatisfy(f->{assertThat(f.key()).isEqualTo("tenures");assertThat(f.value()).isEqualTo("0");});
        assertThat(hidden.metrics()).noneMatch(f->f.key().equals("transferFee"));
    }
    @Test void batchRespectsVisibilityBeforeReadingPerformanceAndPreservesRequestedOrder() {
        UserRepository users=mock(UserRepository.class);AthleteRepository athletes=mock(AthleteRepository.class);MatchPerformanceRepository matches=mock(MatchPerformanceRepository.class);
        AccessPolicy access=mock(AccessPolicy.class);Athlete hidden=identified(new Athlete()),visible=identified(new Athlete());
        when(users.findAllById(any())).thenReturn(List.of(hidden,visible));when(access.canViewProfile(null,visible)).thenReturn(true);
        when(matches.findAllByAthleteIdInAndSeason(anyList(),anyString())).thenReturn(List.of());
        var service=new ProfessionalCardService(users,athletes,matches,mock(ClubTeamRepository.class),mock(ClubManagerTenureRepository.class),mock(ClubCompetitionEntryRepository.class),mock(ManagerCareerTitleRepository.class),access,mock(StorageService.class));
        assertThat(service.cards(null,List.of(hidden.getId(),visible.getId(),visible.getId()))).extracting(c->c.id()).containsExactly(visible.getId());
        verify(matches).findAllByAthleteIdInAndSeason(List.of(visible.getId()),IsoWeeks.season(today));
        assertThatThrownBy(()->service.cards(null,Collections.nCopies(61,visible.getId()))).isInstanceOf(ApiException.class);
    }
    @Test void equalReferenceDatesPreferTheMoreRecentSeasonInsteadOfMixingHistoricStandings() {
        ClubCompetitionEntry old=new ClubCompetitionEntry();old.asOf=today;old.season="2025";old.competition="Liga";old.position=10;
        ClubCompetitionEntry current=new ClubCompetitionEntry();current.asOf=today;current.season="2026";current.competition="Liga";current.position=2;
        assertThat(ProfessionalCardService.latest(List.of(old,current),today)).isSameAs(current);
        current.asOf=today.plusDays(1);assertThat(ProfessionalCardService.latest(List.of(old,current),today)).isSameAs(old);
    }
}
