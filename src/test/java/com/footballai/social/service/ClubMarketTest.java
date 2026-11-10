package com.footballai.social.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ClubDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClubMarketTest {
    final ClubRepository clubs=mock(ClubRepository.class);final AthleteRepository athletes=mock(AthleteRepository.class);
    final AccessPolicy access=mock(AccessPolicy.class);final ClubMembershipRepository memberships=mock(ClubMembershipRepository.class);
    final ClubTeamRepository teams=mock(ClubTeamRepository.class);
    final ClubService service=new ClubService(clubs,athletes,access,teams,mock(ClubManagerTenureRepository.class),mock(ClubCompetitionEntryRepository.class),mock(ClubFinanceSnapshotRepository.class),mock(ClubVenueRepository.class),memberships,mock(CardAssembler.class),new ObjectMapper(),mock(UserLgpdPreferencesRepository.class));
    final UUID athleteId=UUID.randomUUID(),clubId=UUID.randomUUID();final Athlete athlete=new Athlete();
    final AuthPrincipal owner=new AuthPrincipal(athleteId,UserRole.ATLETA,"s","j",Instant.MAX);
    ClubMarketTest() {ReflectionTestUtils.setField(athlete,"id",athleteId);when(athletes.locked(athleteId)).thenReturn(Optional.of(athlete));when(athletes.findById(athleteId)).thenReturn(Optional.of(athlete));}
    static Market market(String status,boolean free,String contract) {return new Market(status,free,"Titular",null,null,contract,null,Position.MEIA,null,null,null,null,null,null,"BR",null,null,com.footballai.social.model.enums.TransferSituation.NOT_FOR_SALE);}
    @Test void absenceOfDataIsNotAZeroCompatibilityScore() {
        Club c=new Club();c.setCountryCode(null);Athlete a=new Athlete();a.setCountryCode(null);ClubTeam t=new ClubTeam();
        var result=ClubService.calculate(a,c,t);assertThat(result.score()).isNull();assertThat(result.coverage()).isZero();assertThat(result.status()).isEqualTo("INSUFFICIENT_DATA");
    }
    @Test void compatibilityUsesRelevantCriteriaAndReportsRealMismatchesAsZero() {
        Club c=new Club();c.setCountryCode(null);Athlete a=new Athlete();a.setCountryCode(null);a.setPosition(Position.MEIA);a.setCategoria("SUB-20");ClubTeam t=new ClubTeam();t.wantedPositions="GOLEIRO";t.category="SUB-17";
        var result=ClubService.calculate(a,c,t);assertThat(result.coverage()).isEqualTo(55);assertThat(result.score()).isZero();
        a.setPosition(Position.GOLEIRO);a.setCategoria("sub-17");assertThat(ClubService.calculate(a,c,t).score()).isEqualTo(100);
        t.wantedPositions="MEIA, GOLEIRO";assertThat(ClubService.calculate(a,c,t).score()).isEqualTo(100);
        a.setDisplayName("Outro nome");a.setNationalities("PT");a.setEmail("privado@exemplo.dev");a.setPasswordHash("hash");
        assertThat(ClubService.calculate(a,c,t).score()).isEqualTo(100);
        assertThat(ClubService.calculate(a,c,t).factors()).noneMatch(f->Set.of("nationalities","email","passwordHash","displayName","injury").contains(f.field()));
    }
    @Test void cannotDeclareFreeTransferOrNoClubWhileLinked() {
        athlete.setCurrentClubId(clubId);
        assertThatThrownBy(()->service.market(owner,market("WITHOUT_CLUB",true,"AGENTE_LIVRE"))).isInstanceOf(ApiException.class).hasMessageContaining("Encerre");
        athlete.setCurrentClubId(null);
        assertThatThrownBy(()->service.market(owner,market("ATTACHED",true,"CONTRATADO"))).isInstanceOf(ApiException.class).hasMessageContaining("Transferência livre");
        assertThatThrownBy(()->service.market(owner,market("WITHOUT_CLUB",false,"CONTRATADO"))).isInstanceOf(ApiException.class).hasMessageContaining("Sem clube");
    }
    @Test void freeAgentDeclarationClearsCurrentNameButPreservesLastClub() {
        athlete.setCurrentClubName("Último Clube");athlete.setLastClubName("Outro");
        service.market(owner,market("WITHOUT_CLUB",true,"AGENTE_LIVRE"));
        assertThat(athlete.getAffiliationStatus()).isEqualTo("WITHOUT_CLUB");assertThat(athlete.getCurrentClubName()).isNull();assertThat(athlete.getFreeTransferConfirmed()).isTrue();
    }
    @Test void acceptingMembershipRequiresOwnershipAndCurrentDates() {
        ClubMembership invite=new ClubMembership();invite.id=UUID.randomUUID();invite.athleteId=UUID.randomUUID();invite.clubId=clubId;invite.status="PENDING";invite.startDate=LocalDate.now();
        when(memberships.findById(invite.id)).thenReturn(Optional.of(invite));
        assertThatThrownBy(()->service.accept(owner,invite.id,true)).isInstanceOf(ApiException.class).hasMessageContaining("Convite");
        invite.athleteId=athleteId;invite.startDate=LocalDate.now().plusDays(1);
        assertThatThrownBy(()->service.accept(owner,invite.id,true)).isInstanceOf(ApiException.class).hasMessageContaining("vigentes");
        assertThat(invite.status).isEqualTo("PENDING");assertThat(athlete.getCurrentClubId()).isNull();
    }
    @Test void acceptedMembershipLinksAthleteAndClosesPreviousMembership() {
        Club c=new Club();ReflectionTestUtils.setField(c,"id",clubId);c.setDisplayName("Novo Clube");when(clubs.findById(clubId)).thenReturn(Optional.of(c));
        ClubMembership invite=new ClubMembership();invite.id=UUID.randomUUID();invite.athleteId=athleteId;invite.clubId=clubId;invite.status="PENDING";invite.startDate=LocalDate.now();invite.source="Clube e atleta";
        ClubMembership old=new ClubMembership();old.status="ACTIVE";old.athleteId=athleteId;old.startDate=LocalDate.now().minusDays(30);
        when(memberships.findById(invite.id)).thenReturn(Optional.of(invite));when(memberships.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(old));
        service.accept(owner,invite.id,true);
        assertThat(invite.status).isEqualTo("ACTIVE");assertThat(old.status).isEqualTo("ENDED");assertThat(athlete.getCurrentClubId()).isEqualTo(clubId);assertThat(athlete.getCurrentClubName()).isEqualTo("Novo Clube");assertThat(athlete.getContractStatus()).isEqualTo(ContractStatus.CONTRATADO);assertThat(athlete.getFreeTransferConfirmed()).isFalse();
    }
    @Test void expiredMembershipPreservesEndDateAndClearsCurrentClubWithoutClaimingFreeTransfer() {
        ClubMembership m=new ClubMembership();m.athleteId=athleteId;m.clubId=clubId;m.status="ACTIVE";m.endDate=LocalDate.now().minusDays(1);
        athlete.setCurrentClubId(clubId);athlete.setCurrentClubName("Clube anterior");athlete.setFreeTransferConfirmed(true);
        when(memberships.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(m));
        service.reconcileExpiredMemberships();
        assertThat(m.status).isEqualTo("ENDED");assertThat(m.endDate).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(athlete.getCurrentClubId()).isNull();assertThat(athlete.getLastClubName()).isEqualTo("Clube anterior");assertThat(athlete.getFreeTransferConfirmed()).isFalse();
    }
}
