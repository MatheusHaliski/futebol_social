package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ManagerDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.TransferSituation;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Perfil de Manager: nunca serializa a entidade User nem seus dados de autenticação. */
@Service
@Transactional(readOnly=true)
public class ManagerService {
    private final ManagerRepository managers;
    private final ManagerCareerTitleRepository titles;
    private final ClubManagerTenureRepository tenures;
    private final ClubCompetitionEntryRepository competitions;
    private final ClubRepository clubs;
    private final AccessPolicy access;
    public ManagerService(ManagerRepository managers, ManagerCareerTitleRepository titles,
                          ClubManagerTenureRepository tenures, ClubCompetitionEntryRepository competitions,
                          ClubRepository clubs, AccessPolicy access) {
        this.managers=managers; this.titles=titles; this.tenures=tenures;
        this.competitions=competitions; this.clubs=clubs; this.access=access;
    }
    private Manager visible(AuthPrincipal viewer, UUID id) {
        User user=access.requireProfile(viewer,id,"Manager");
        if (!(user instanceof Manager m)) throw ApiException.notFound("Manager");
        return m;
    }
    private Manager owner(AuthPrincipal viewer, UUID id) {
        if (viewer==null || !viewer.userId().equals(id)) throw ApiException.forbidden("MANAGER_OWNER_REQUIRED","Somente o titular pode editar o perfil de Manager");
        return managers.findById(id).orElseThrow(()->ApiException.notFound("Manager"));
    }
    private List<ClubManagerTenure> managerTenures(UUID id) {
        return tenures.findAll((root,q,cb)->cb.equal(root.get("managerUserId"),id));
    }
    static boolean current(ClubManagerTenure t, LocalDate today) {
        return !t.startDate.isAfter(today) && (t.endDate==null || !t.endDate.isBefore(today));
    }
    public Profile profile(AuthPrincipal viewer, UUID id) {
        Manager m=visible(viewer,id); boolean own=viewer!=null && viewer.userId().equals(id);
        LocalDate today=LocalDate.now(); Map<UUID,Club> byClub=new HashMap<>();
        List<ClubManagerTenure> mandates=managerTenures(id);
        boolean hasActiveMandate=mandates.stream().anyMatch(t->current(t,today));
        List<ClubManagerTenure> all=mandates.stream().filter(t->{
            Club c=clubs.findById(t.clubId).orElse(null);
            if (c!=null && (own || access.canViewProfile(viewer,c))) { byClub.put(c.getId(),c); return true; }
            return false;
        }).sorted(Comparator.comparing((ClubManagerTenure t)->t.startDate).reversed()).toList();
        List<Tenure> history=all.stream().map(t->new Tenure(t.id,t.clubId,byClub.get(t.clubId).getDisplayName(),t.teamId,t.function,t.startDate,t.endDate,t.contractUntil,t.interim,
            own||t.transferFeePublic?t.transferFee:null,own||t.transferFeePublic?t.currency:null,t.source)).toList();
        Set<UUID> active=all.stream().filter(t->current(t,today)).map(t->t.id).collect(Collectors.toSet());
        List<Tenure> current=history.stream().filter(t->active.contains(t.id())).toList();
        List<ClubCareerMetric> services=serviceTotals(all,byClub,today);
        int maxSpells=services.stream().mapToInt(ClubCareerMetric::spells).max().orElse(0);
        long maxDays=services.stream().mapToLong(ClubCareerMetric::serviceDays).max().orElse(0);
        List<LeaguePosition> positions=new ArrayList<>();
        // Only explicit manager-team links: never attribute an entire club's other teams to the manager.
        for (UUID clubId:current.stream().map(Tenure::clubId).distinct().toList()) {
            Set<UUID> teamIds=current.stream().filter(t->t.clubId().equals(clubId)).map(Tenure::teamId).collect(Collectors.toSet());
            Map<List<String>,ClubCompetitionEntry> latest=new HashMap<>();
            for (ClubCompetitionEntry e:competitions.findAllByClubId(clubId)) {
                if (!"LEAGUE".equals(e.format)||!teamIds.contains(e.teamId)||e.asOf.isAfter(today)) continue;
                List<String> key=Arrays.asList(e.teamId.toString(),e.competition);
                latest.merge(key,e,(a,b)->a.asOf.isAfter(b.asOf)?a:b);
            }
            latest.values().stream().sorted(Comparator.comparing((ClubCompetitionEntry e)->e.asOf).reversed())
                .forEach(e->positions.add(new LeaguePosition(clubId,e.teamId,e.competition,e.season,e.stage,e.groupName,e.position,e.asOf)));
        }
        Boolean hasClub=hasActiveMandate?Boolean.TRUE:m.getHasCurrentClub();
        return new Profile(m.getId(),m.getDisplayName(),"@"+m.getUsername(),m.getBio(),m.getCountryCode(),m.getStateCode(),m.getCity(),
            m.getFunction(),m.getNationalities(),m.getLanguages(),m.getLicenseName(),m.getLicenseLevel(),m.getPreferredFormation(),m.getPlayingStyle(),m.getCareerStartedOn(),
            hasClub,m.getClubStatusSource(),m.getClubStatusAsOf(),current,history,titles.findAllByManagerIdOrderByAwardedOnDesc(id).stream().map(ManagerService::title).toList(),
            services.stream().filter(s->s.spells()==maxSpells).toList(),services.stream().filter(s->s.serviceDays()==maxDays).toList(),positions,"MATCH_COUNTS_NOT_RECORDED",own,effectiveTransferSituation(m,hasActiveMandate));
    }
    static TransferSituation effectiveTransferSituation(Manager m,boolean hasActiveMandate) {
        TransferSituation declared=m.getTransferSituation()==null?TransferSituation.NOT_FOR_SALE:m.getTransferSituation();
        return declared==TransferSituation.FREE_AGENT && (hasActiveMandate || !Boolean.FALSE.equals(m.getHasCurrentClub()))
            ? TransferSituation.NOT_FOR_SALE : declared;
    }
    /** Unions overlapping team mandates so service days aren't counted twice. */
    static List<ClubCareerMetric> serviceTotals(List<ClubManagerTenure> tenures, Map<UUID,Club> clubs, LocalDate today) {
        List<ClubCareerMetric> result=new ArrayList<>();
        tenures.stream().filter(t->!t.startDate.isAfter(today)).collect(Collectors.groupingBy(t->t.clubId)).forEach((id,items)->{
            List<ClubManagerTenure> sorted=items.stream().sorted(Comparator.comparing(t->t.startDate)).toList();
            LocalDate start=null,end=null; long days=0; int spells=0;
            for (ClubManagerTenure t:sorted) {
                LocalDate until=t.endDate==null||t.endDate.isAfter(today)?today:t.endDate;
                if (until.isBefore(t.startDate)) continue;
                if (end==null || t.startDate.isAfter(end.plusDays(1))) {
                    if (start!=null) days+=ChronoUnit.DAYS.between(start,end)+1;
                    start=t.startDate; end=until; spells++;
                } else if(until.isAfter(end)) end=until;
            }
            if(start!=null) days+=ChronoUnit.DAYS.between(start,end)+1;
            result.add(new ClubCareerMetric(id,clubs.get(id).getDisplayName(),spells,days));
        });
        result.sort(Comparator.comparing(ClubCareerMetric::clubName).thenComparing(ClubCareerMetric::clubId));
        return result;
    }
    @Transactional
    public Profile update(AuthPrincipal viewer, UUID id, EditManager r) {
        Manager m=owner(viewer,id);
        boolean active=managerTenures(id).stream().anyMatch(t->current(t,LocalDate.now()));
        TransferSituation transfer=r.transferSituation()==null?m.getTransferSituation():r.transferSituation();
        if(transfer==TransferSituation.FREE_AGENT && (!Boolean.FALSE.equals(r.hasCurrentClub()) || active))
            throw ApiException.badRequest("MANAGER_FREE_AGENT_INVALID","Agente livre exige declaração explícita de manager sem clube e ausência de mandato vigente");
        if(Boolean.FALSE.equals(r.hasCurrentClub()) && active)
            throw ApiException.conflict("MANAGER_ACTIVE_TENURE","Encerre o vínculo vigente antes de declarar manager sem clube");
        if(r.hasCurrentClub()!=null && (r.clubStatusSource()==null||r.clubStatusSource().isBlank()||r.clubStatusAsOf()==null))
            throw ApiException.badRequest("MANAGER_CLUB_EVIDENCE_REQUIRED","Informe a origem e a data da situação de vínculo");
        m.setFunction(r.function()); m.setNationalities(codes(r.nationalities())); m.setLanguages(codes(r.languages()));
        m.setLicenseName(r.licenseName()); m.setLicenseLevel(r.licenseLevel());m.setPreferredFormation(r.preferredFormation());m.setPlayingStyle(r.playingStyle());
        m.setCareerStartedOn(r.careerStartedOn());m.setHasCurrentClub(active?Boolean.TRUE:r.hasCurrentClub());
        m.setClubStatusSource(r.clubStatusSource());m.setClubStatusAsOf(r.clubStatusAsOf());m.setTransferSituation(transfer);managers.save(m);
        return profile(viewer,id);
    }
    private static String codes(String v) { return v==null||v.isBlank()?null:v.toUpperCase(Locale.ROOT); }
    private static CareerTitle title(ManagerCareerTitle t) {
        return new CareerTitle(t.id,t.clubId,t.clubName,t.competition,t.season,t.category,t.awardedOn,t.source,"DECLARED");
    }
    @Transactional
    public CareerTitle addTitle(AuthPrincipal viewer,UUID id,CareerTitleRequest r) {
        owner(viewer,id);
        if(r.clubId()!=null && !(access.requireProfile(viewer,r.clubId(),"Clube") instanceof Club)) throw ApiException.notFound("Clube");
        ManagerCareerTitle t=new ManagerCareerTitle();t.managerId=id;t.clubId=r.clubId();t.clubName=r.clubName();t.competition=r.competition();
        t.season=r.season();t.category=r.category();t.awardedOn=r.awardedOn();t.source=r.source();t.createdAt=Instant.now();return title(titles.save(t));
    }
    @Transactional
    public void removeTitle(AuthPrincipal viewer,UUID id,UUID titleId) {
        owner(viewer,id);ManagerCareerTitle t=titles.findById(titleId).orElseThrow(()->ApiException.notFound("Título"));
        if(!t.managerId.equals(id)) throw ApiException.notFound("Título");titles.delete(t);
    }
}
