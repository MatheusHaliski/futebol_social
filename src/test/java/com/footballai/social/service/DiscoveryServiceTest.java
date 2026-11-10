package com.footballai.social.service;

import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscoveryServiceTest {
    final UserRepository users=mock(UserRepository.class);
    final FollowRepository follows=mock(FollowRepository.class);
    final ClubManagerTenureRepository mandates=mock(ClubManagerTenureRepository.class);
    final DiscoveryService service=new DiscoveryService(users,follows,mandates);
    @Test void publicRegistryDoesNotExposeAuthenticationOrMedicalData() {
        List<String> fields=service.fields().stream().map(DiscoveryService.Field::key).toList();
        assertThat(fields).contains("manager.hasCurrentClub","manager.transferSituation","athlete.transferSituation","athlete.affiliationStatus","finance.payroll","managerTenure.contractUntil","venue.street");
        assertThat(fields).noneMatch(f->f.toLowerCase().contains("password")||f.toLowerCase().contains("email")||f.contains("guardianConsent")||f.contains("injury"));
        for(String forbidden:List.of("passwordHash:eq:secret","email:eq:a@b.test","athlete.weightKg:gte:50","managerTenure.clubId:eq:fake"))
            assertThatThrownBy(()->service.parse(List.of(forbidden))).isInstanceOf(ApiException.class);
    }
    @Test void validatesFiltersOperatorsLengthsAndPagination() {
        assertThat(service.parse(List.of("displayName:contains:Atleta","athlete.heightCm:gte:180","competition.season:eq:2026"))).hasSize(3);
        for(String bad:List.of("displayName:gte:A","manager.hasCurrentClub:contains:false","manager.hasCurrentClub:isnull:yes","athlete.position:gte:MEIA","displayName:eq:"+"a".repeat(241)))
            assertThatThrownBy(()->service.parse(List.of(bad))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.parse(Collections.nCopies(31,"city:eq:Curitiba"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.search(null,"ALL",null,List.of(),-1,20,"name")).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.search(null,"ALL",null,List.of(),0,101,"name")).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.search(null,"ALL",null,List.of(),0,20,"passwordHash")).isInstanceOf(ApiException.class);
    }
    @Test void typedValuesAreRejectedBeforeBuildingInvalidQueries() {
        CriteriaBuilder cb=mock(CriteriaBuilder.class);Path<?> path=mock(Path.class);
        for(String raw:List.of("athlete.heightCm:gte:alto","athlete.position:eq:INVALID","manager.hasCurrentClub:eq:yes","manager.careerStartedOn:gte:ontem","athlete.currentClubId:eq:not-a-uuid")) {
            var f=service.parse(List.of(raw)).get(0);
            assertThatThrownBy(()->DiscoveryService.compare(cb,path,f)).isInstanceOf(ApiException.class);
        }
        assertThat(DiscoveryService.escape("100%_\\")).isEqualTo("100\\%\\_\\\\");
    }
    @Test void oneBatchLookupOverridesStaleManagerDeclarationInAllReturnedLabels() {
        Manager a=new Manager(),b=new Manager();UUID aid=UUID.randomUUID(),bid=UUID.randomUUID();
        ReflectionTestUtils.setField(a,"id",aid);ReflectionTestUtils.setField(b,"id",bid);a.setHasCurrentClub(false);a.setTransferSituation(TransferSituation.FREE_AGENT);b.setHasCurrentClub(null);
        when(users.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<User>(List.of(a,b)));
        ClubManagerTenure active=new ClubManagerTenure();active.managerUserId=aid;
        when(mandates.findAll(any(Specification.class))).thenReturn(List.of(active));
        var result=service.search(null,"MANAGER",null,List.of(),0,20,"name");
        assertThat(result.items().get(0).market()).isEqualTo("Com clube");
        assertThat(result.items().get(0).transferSituation()).isEqualTo(TransferSituation.NOT_FOR_SALE);
        assertThat(result.items().get(1).market()).isEqualTo("Vínculo não informado");
        verify(mandates,times(1)).findAll(any(Specification.class));
        assertThat(a.getHasCurrentClub()).isFalse(); // projection doesn't rewrite the user's declaration
    }
    @Test void effectiveTransferSituationIsReturnedForPlayersAndManagersWithoutMutatingDeclarations() {
        Athlete a=new Athlete();a.setTransferSituation(TransferSituation.FREE_AGENT);
        assertThat(DiscoveryService.result(a,Set.of()).transferSituation()).isEqualTo(TransferSituation.NOT_FOR_SALE);
        a.setAffiliationStatus("WITHOUT_CLUB");a.setContractStatus(ContractStatus.AGENTE_LIVRE);
        assertThat(DiscoveryService.result(a,Set.of()).transferSituation()).isEqualTo(TransferSituation.FREE_AGENT);
        assertThat(DiscoveryService.result(a,Set.of()).market()).isEqualTo("Sem clube · Agente livre");
        a.setCurrentClubId(UUID.randomUUID());
        assertThat(DiscoveryService.result(a,Set.of()).transferSituation()).isEqualTo(TransferSituation.NOT_FOR_SALE);
        Manager m=new Manager();ReflectionTestUtils.setField(m,"id",UUID.randomUUID());m.setTransferSituation(TransferSituation.FREE_AGENT);m.setHasCurrentClub(false);
        assertThat(DiscoveryService.result(m,Set.of()).transferSituation()).isEqualTo(TransferSituation.FREE_AGENT);
        assertThat(DiscoveryService.result(m,Set.of(m.getId())).transferSituation()).isEqualTo(TransferSituation.NOT_FOR_SALE);
        assertThat(m.getTransferSituation()).isEqualTo(TransferSituation.FREE_AGENT);
    }
    @Test void noManagerRowsDoesNotIssueMandateLookup() {
        when(users.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<User>(List.of(new Athlete(),new Club())));
        service.search(null,"ALL",null,List.of(),0,20,"name");verifyNoInteractions(mandates);
    }
    @SuppressWarnings("unchecked")
    @Test void managerWithoutClubRequiresExplicitFalseAndAbsenceOfActiveMandate() {
        Root<User> root=mock(Root.class,RETURNS_DEEP_STUBS);Root<Manager> manager=mock(Root.class,RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query=mock(CriteriaQuery.class);CriteriaBuilder cb=mock(CriteriaBuilder.class);
        Subquery<UUID> sq=mock(Subquery.class);Root<ClubManagerTenure> tenure=mock(Root.class,RETURNS_DEEP_STUBS);
        when(query.subquery(UUID.class)).thenReturn(sq);when(sq.from(ClubManagerTenure.class)).thenReturn(tenure);when(sq.select(any())).thenReturn(sq);when(cb.treat(root,Manager.class)).thenReturn(manager);
        Predicate has=mock(Predicate.class),not=mock(Predicate.class),falseDeclared=mock(Predicate.class);
        when(cb.exists(sq)).thenReturn(has);when(cb.not(has)).thenReturn(not);when(cb.isFalse(manager.get("hasCurrentClub"))).thenReturn(falseDeclared);
        var f=service.parse(List.of("manager.hasCurrentClub:eq:false")).get(0);
        DiscoveryService.managerClubPredicate(root,query,cb,f);
        verify(cb).and(not,falseDeclared);
        verify(cb).lessThanOrEqualTo(tenure.get("startDate"),LocalDate.now());
        verify(cb).greaterThanOrEqualTo(tenure.get("endDate"),LocalDate.now());
        verify(cb,never()).isNull(manager.get("hasCurrentClub"));
    }
}
