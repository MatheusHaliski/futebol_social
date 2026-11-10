package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ManagerDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.model.enums.TransferSituation;
import com.footballai.social.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManagerServiceTest {
    private final ManagerRepository managers=mock(ManagerRepository.class);
    private final ManagerCareerTitleRepository titles=mock(ManagerCareerTitleRepository.class);
    private final ClubManagerTenureRepository tenures=mock(ClubManagerTenureRepository.class);
    private final ClubCompetitionEntryRepository competitions=mock(ClubCompetitionEntryRepository.class);
    private final ClubRepository clubs=mock(ClubRepository.class);
    private final AccessPolicy access=mock(AccessPolicy.class);
    private final ManagerService service=new ManagerService(managers,titles,tenures,competitions,clubs,access);
    private final UUID id=UUID.randomUUID(), clubId=UUID.randomUUID();
    private final Manager manager=new Manager();
    private final Club club=new Club();
    private final AuthPrincipal owner=new AuthPrincipal(id,UserRole.MANAGER,"s","j",Instant.MAX);
    ManagerServiceTest() {
        ReflectionTestUtils.setField(manager,"id",id);manager.setDisplayName("Treinador");manager.setUsername("manager");
        ReflectionTestUtils.setField(club,"id",clubId);club.setDisplayName("Clube");
        when(access.requireProfile(any(),eq(id),eq("Manager"))).thenReturn(manager);
        when(managers.findById(id)).thenReturn(Optional.of(manager));
        when(clubs.findById(clubId)).thenReturn(Optional.of(club));
        when(access.canViewProfile(any(),eq(club))).thenReturn(true);
    }
    private ClubManagerTenure mandate(LocalDate from,LocalDate to) {
        ClubManagerTenure t=new ClubManagerTenure();t.id=UUID.randomUUID();t.managerUserId=id;t.clubId=clubId;t.teamId=UUID.randomUUID();t.function="HEAD_COACH";
        t.startDate=from;t.endDate=to;t.source="Clube";return t;
    }
    @Test void unknownClubStatusIsNotAutomaticallyFreeAgent() {
        assertThat(new Manager().getHasCurrentClub()).isNull();
        assertThat(service.profile(owner,id).hasCurrentClub()).isNull();
        manager.setHasCurrentClub(false);
        assertThat(service.profile(owner,id).hasCurrentClub()).isFalse();
        assertThat(UserRole.MANAGER.isHuman()).isTrue();
        assertThat(UserRole.MANAGER.hasDigitalTwin()).isFalse();
    }
    @Test void cannotDeclareWithoutClubWhileLinkedToCurrentMandate() {
        when(tenures.findAll(any(Specification.class))).thenReturn(List.of(mandate(LocalDate.now().minusMonths(1),null)));
        EditManager r=new EditManager("HEAD_COACH","BR",null,null,null,null,null,null,false,"Titular",LocalDate.now(),TransferSituation.NOT_FOR_SALE);
        assertThatThrownBy(()->service.update(owner,id,r)).isInstanceOf(ApiException.class).hasMessageContaining("Encerre o vínculo");
        verify(managers,never()).save(any());
    }
    @Test void freeAgentRequiresExplicitNoClubAndNoCurrentMandate() {
        EditManager unknown=new EditManager("HEAD_COACH","BR",null,null,null,null,null,null,null,null,null,TransferSituation.FREE_AGENT);
        assertThatThrownBy(()->service.update(owner,id,unknown)).isInstanceOf(ApiException.class).hasMessageContaining("Agente livre exige");
        EditManager free=new EditManager("HEAD_COACH","BR",null,null,null,null,null,null,false,"Titular",LocalDate.now(),TransferSituation.FREE_AGENT);
        assertThat(service.update(owner,id,free).transferSituation()).isEqualTo(TransferSituation.FREE_AGENT);
        assertThat(manager.getHasCurrentClub()).isFalse();
        when(tenures.findAll(any(Specification.class))).thenReturn(List.of(mandate(LocalDate.now().minusDays(1),null)));
        assertThatThrownBy(()->service.update(owner,id,free)).isInstanceOf(ApiException.class).hasMessageContaining("Agente livre exige");
    }
    @Test void requiresOwnershipForProfileAndTitles() {
        AuthPrincipal stranger=new AuthPrincipal(UUID.randomUUID(),UserRole.MANAGER,"s","j",Instant.MAX);
        assertThatThrownBy(()->service.update(stranger,id,null)).isInstanceOf(ApiException.class).hasMessageContaining("titular");
        assertThatThrownBy(()->service.addTitle(stranger,id,null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(titles);
    }
    @Test void feesArePrivateAndCurrentClubExpiresByActualEndNotPlannedContract() {
        ClubManagerTenure t=mandate(LocalDate.now().minusMonths(3),null);t.contractUntil=LocalDate.now().minusDays(1);t.transferFee=new BigDecimal("1000");t.currency="BRL";
        when(tenures.findAll(any(Specification.class))).thenReturn(List.of(t));
        Profile me=service.profile(owner,id);assertThat(me.currentClubs()).hasSize(1);assertThat(me.currentClubs().get(0).transferFee()).isEqualByComparingTo("1000");
        AuthPrincipal reader=new AuthPrincipal(UUID.randomUUID(),UserRole.SCOUT,"s","j",Instant.MAX);
        Profile other=service.profile(reader,id);assertThat(other.currentClubs().get(0).transferFee()).isNull();assertThat(other.currentClubs().get(0).currency()).isNull();
        t.transferFeePublic=true;assertThat(service.profile(reader,id).currentClubs().get(0).transferFee()).isEqualByComparingTo("1000");
        t.endDate=LocalDate.now().minusDays(1);assertThat(service.profile(reader,id).currentClubs()).isEmpty();
    }
    @Test void activeMandateOverridesStaleFreeAgentEvenIfClubDetailsArePrivate() {
        manager.setHasCurrentClub(false);manager.setTransferSituation(TransferSituation.FREE_AGENT);
        ClubManagerTenure future=mandate(LocalDate.now().plusDays(1),null);
        when(tenures.findAll(any(Specification.class))).thenReturn(List.of(future));
        assertThat(service.profile(owner,id).transferSituation()).isEqualTo(TransferSituation.FREE_AGENT);
        ClubManagerTenure current=mandate(LocalDate.now().minusDays(1),null);
        when(tenures.findAll(any(Specification.class))).thenReturn(List.of(current));
        AuthPrincipal reader=new AuthPrincipal(UUID.randomUUID(),UserRole.SCOUT,"s","j",Instant.MAX);
        when(access.canViewProfile(reader,club)).thenReturn(false);
        Profile p=service.profile(reader,id);
        assertThat(p.history()).isEmpty();assertThat(p.currentClubs()).isEmpty();
        assertThat(p.hasCurrentClub()).isTrue();assertThat(p.transferSituation()).isEqualTo(TransferSituation.NOT_FOR_SALE);
        assertThat(manager.getHasCurrentClub()).isFalse();assertThat(manager.getTransferSituation()).isEqualTo(TransferSituation.FREE_AGENT);
        current.endDate=LocalDate.now().minusDays(1);
        assertThat(service.profile(owner,id).transferSituation()).isEqualTo(TransferSituation.FREE_AGENT);
    }
    @Test void overlappingTeamMandatesDoNotDoubleCountDaysAndReturnSpells() {
        LocalDate today=LocalDate.of(2026,11,9);
        ClubManagerTenure a=mandate(LocalDate.of(2025,1,1),LocalDate.of(2025,1,10));
        ClubManagerTenure b=mandate(LocalDate.of(2025,1,5),LocalDate.of(2025,1,20));
        ClubManagerTenure c=mandate(LocalDate.of(2025,2,1),LocalDate.of(2025,2,5));
        ClubManagerTenure future=mandate(today.plusDays(1),null);
        var totals=ManagerService.serviceTotals(List.of(a,b,c,future),Map.of(clubId,club),today);
        assertThat(totals).hasSize(1);assertThat(totals.get(0).serviceDays()).isEqualTo(25);assertThat(totals.get(0).spells()).isEqualTo(2);
    }
    @Test void onlyLatestKnownLeaguePositionPerTeamCompetitionIsPresented() {
        ClubManagerTenure t=mandate(LocalDate.now().minusMonths(2),null);
        when(tenures.findAll(any(Specification.class))).thenReturn(List.of(t));
        ClubCompetitionEntry old=new ClubCompetitionEntry();old.clubId=clubId;old.teamId=t.teamId;old.competition="Liga";old.season="2025";old.format="LEAGUE";old.asOf=LocalDate.now().minusDays(300);old.position=7;
        ClubCompetitionEntry latest=new ClubCompetitionEntry();latest.clubId=clubId;latest.teamId=t.teamId;latest.competition="Liga";latest.season="2026";latest.format="LEAGUE";latest.asOf=LocalDate.now();latest.position=2;
        when(competitions.findAllByClubId(clubId)).thenReturn(List.of(old,latest));
        var p=service.profile(owner,id);assertThat(p.leaguePositions()).hasSize(1);assertThat(p.leaguePositions().get(0).position()).isEqualTo(2);
    }
}
