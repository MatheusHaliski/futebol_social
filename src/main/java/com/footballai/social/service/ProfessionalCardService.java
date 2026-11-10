package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ProfessionalCardDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Fetch a page's sporting evidence in batches, keeping authorization independent from discovery. */
@Service
@Transactional(readOnly = true)
public class ProfessionalCardService {
    private final UserRepository users;
    private final AthleteRepository athletes;
    private final MatchPerformanceRepository matches;
    private final ClubTeamRepository teams;
    private final ClubManagerTenureRepository tenures;
    private final ClubCompetitionEntryRepository competitions;
    private final ManagerCareerTitleRepository titles;
    private final AccessPolicy access;
    private final StorageService storage;

    public ProfessionalCardService(UserRepository users, AthleteRepository athletes, MatchPerformanceRepository matches,
            ClubTeamRepository teams, ClubManagerTenureRepository tenures, ClubCompetitionEntryRepository competitions,
            ManagerCareerTitleRepository titles, AccessPolicy access, StorageService storage) {
        this.users=users; this.athletes=athletes; this.matches=matches; this.teams=teams; this.tenures=tenures;
        this.competitions=competitions; this.titles=titles; this.access=access; this.storage=storage;
    }

    public List<Card> cards(AuthPrincipal viewer, List<UUID> ids) {
        if (ids.size()>60) throw ApiException.badRequest("CARD_LIMIT", "Selecione até 60 perfis por vez");
        if (ids.isEmpty()) return List.of();
        LocalDate today=LocalDate.now(Clock.systemUTC());
        Map<UUID,User> visible=new HashMap<>();
        users.findAllById(new LinkedHashSet<>(ids)).stream().filter(u->access.canViewProfile(viewer,u))
            .forEach(u->visible.put(u.getId(),u));
        List<UUID> athleteIds=visible.values().stream().filter(Athlete.class::isInstance).map(User::getId).toList();
        List<UUID> clubIds=visible.values().stream().filter(Club.class::isInstance).map(User::getId).toList();
        List<UUID> managerIds=visible.values().stream().filter(Manager.class::isInstance).map(User::getId).toList();
        Map<UUID,List<MatchPerformance>> performances=athleteIds.isEmpty()?Map.of():matches.findAllByAthleteIdInAndSeason(athleteIds,IsoWeeks.season(today)).stream()
            .filter(m->m.getMatchDate()!=null&&!m.getMatchDate().isAfter(today))
            .collect(Collectors.groupingBy(MatchPerformance::getAthleteId));
        List<ClubManagerTenure> mandates=clubIds.isEmpty()&&managerIds.isEmpty()?List.of():tenures.findAll((r,q,b)->{
            List<jakarta.persistence.criteria.Predicate> predicates=new ArrayList<>();
            if(!clubIds.isEmpty())predicates.add(r.get("clubId").in(clubIds));
            if(!managerIds.isEmpty())predicates.add(r.get("managerUserId").in(managerIds));
            return b.or(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        });
        Map<UUID,User> relatedClubs=new HashMap<>(visible);
        users.findAllById(mandates.stream().map(m->m.clubId).collect(Collectors.toSet())).stream()
            .filter(c->access.canViewProfile(viewer,c)).forEach(c->relatedClubs.put(c.getId(),c));
        List<UUID> contextClubs=new ArrayList<>(clubIds);
        for(ClubManagerTenure t:mandates)if(relatedClubs.containsKey(t.clubId)&&!contextClubs.contains(t.clubId))contextClubs.add(t.clubId);
        List<ClubCompetitionEntry> standings=contextClubs.isEmpty()?List.of():competitions.findAll((r,q,b)->r.get("clubId").in(contextClubs));
        List<ClubTeam> clubTeams=contextClubs.isEmpty()?List.of():teams.findAll((r,q,b)->r.get("clubId").in(contextClubs));
        Map<UUID,Long> rosterCounts=clubIds.isEmpty()?Map.of():athletes.findAll((r,q,b)->r.get("currentClubId").in(clubIds)).stream()
            .filter(a->access.canViewProfile(viewer,a)).collect(Collectors.groupingBy(Athlete::getCurrentClubId,Collectors.counting()));
        Map<UUID,List<ManagerCareerTitle>> careerTitles=managerIds.isEmpty()?Map.of():titles.findAllByManagerIdIn(managerIds).stream()
            .filter(t->!t.awardedOn.isAfter(today)).collect(Collectors.groupingBy(t->t.managerId));
        List<Card> result=new ArrayList<>();
        for(UUID id:new LinkedHashSet<>(ids)) {
            User user=visible.get(id); if(user==null)continue;
            Card card;
            if(user instanceof Athlete a) card=athlete(a,performances.getOrDefault(id,List.of()),today);
            else if(user instanceof Club c) card=club(c,clubTeams,rosterCounts.getOrDefault(id,0L),mandates,standings,today);
            else if(user instanceof Manager m) card=manager(m,mandates,relatedClubs,standings,careerTitles.getOrDefault(id,List.of()),clubTeams,today);
            else card=new Card(id,user.getRole().name(),user.getDisplayName(),"@"+user.getUsername(),user.isVerified(),null,
                user.getRole().label(),null,null,null,location(user),List.of(),null,"Dados do perfil", "#/perfil?role="+user.getRole().name());
            String photo=user.getAvatarKey()==null?null:storage.mediaUrl(user.getAvatarKey());
            result.add(new Card(card.id(),card.role(),card.name(),card.handle(),card.verified(),photo,card.headline(),card.affiliation(),card.membership(),card.transferSituation(),card.facts(),card.metrics(),card.context(),card.evidence(),card.href()));
        }
        return result;
    }

    static Card athlete(Athlete a,List<MatchPerformance> performances,LocalDate today) {
        List<Fact> facts=new ArrayList<>();
        add(facts,"position","Posição",a.getPosition()==null?null:title(a.getPosition().name()),null);
        add(facts,"category","Categoria",a.getCategoria(),null);
        if(a.age()!=null)add(facts,"age",a.isMinor()?"Categoria etária":"Idade",a.isMinor()?AccessPolicy.youthCategory(a.age()):a.age()+" anos",null);
        add(facts,"foot","Pé dominante",a.getDominantFoot()==null?null:switch(a.getDominantFoot().toUpperCase(Locale.ROOT)){case "D"->"Direito";case "E"->"Esquerdo";case "A"->"Ambidestro";default->a.getDominantFoot();},null);
        add(facts,"height","Altura",a.getHeightCm()==null?null:a.getHeightCm()+" cm",null);
        add(facts,"contract","Contrato até",a.getContractUntil()==null?null:a.getContractUntil().toString(),a.getCurrentClubName());
        facts.addAll(location(a));
        List<Fact> metrics=new ArrayList<>();
        List<MatchPerformance> played=performances.stream().filter(m->m.getMinutesPlayed()>0).toList();
        BadgeAwardService.SeasonStats stats=BadgeAwardService.aggregate(played);
        if(!played.isEmpty()) {
            add(metrics,"minutes","Minutos",stats.minutes(),"Temporada "+IsoWeeks.season(today));
            if(a.getPosition()==Position.GOLEIRO) add(metrics,"cleanSheets","Jogos sem sofrer gol",stats.cleanSheets(),stats.games()+" partidas registradas");
            else if(Set.of(Position.ZAGUEIRO,Position.LATERAL,Position.VOLANTE).contains(a.getPosition()==null?Position.MEIA:a.getPosition()))add(metrics,"tackles","Desarmes",stats.tackles(),stats.games()+" partidas registradas");
            else add(metrics,"goals","Gols",stats.goals(),stats.games()+" partidas registradas");
            add(metrics,"games","Partidas",stats.games(),"Com minutos em campo");
            add(metrics,"assists","Assistências",stats.assists(),null);
            add(metrics,"rating","Nota média",stats.avgRating(),"Somente partidas com nota informada");
        }
        MatchPerformance latest=performances.stream().max(Comparator.comparing(MatchPerformance::getMatchDate)).orElse(null);
        Context context=new Context(latest==null?null:latest.getCompetition(),IsoWeeks.season(today),null,null,latest==null?null:latest.getMatchDate(),"Registros de partidas do atleta",null);
        long official=played.stream().filter(MatchPerformance::isOfficial).count();
        String evidence=played.isEmpty()?"Sem partidas com minutos registrados nesta temporada":played.size()+" partidas na amostra · "+official+" oficiais · "+(played.size()-official)+" autorrelatadas";
        return new Card(a.getId(),"ATLETA",a.getDisplayName(),"@"+a.getUsername(),a.isVerified(),null,
            a.getPosition()==null?"Posição não informada":title(a.getPosition().name()),a.getCurrentClubName(),
            "WITHOUT_CLUB".equals(a.getAffiliationStatus())?"Sem clube":"ATTACHED".equals(a.getAffiliationStatus())||a.getCurrentClubId()!=null?"Com clube":"Vínculo não informado",
            DiscoveryService.effectiveTransferSituation(a).name(),facts,metrics,context,evidence,"#/vitrine?athleteId="+a.getId());
    }

    static Card club(Club c,List<ClubTeam> allTeams,long roster,List<ClubManagerTenure> mandates,List<ClubCompetitionEntry> entries,LocalDate today) {
        List<ClubTeam> teams=allTeams.stream().filter(t->c.getId().equals(t.clubId)).toList();
        List<Fact> facts=new ArrayList<>(location(c));
        add(facts,"type","Tipo de clube",c.getClubeTipo()==null?null:title(c.getClubeTipo().name()),null);
        add(facts,"federation","Federação",c.getFederation(),null);
        List<ClubManagerTenure> current=mandates.stream().filter(t->c.getId().equals(t.clubId)&&ManagerService.current(t,today))
            .sorted(Comparator.comparing((ClubManagerTenure t)->t.startDate).reversed()).toList();
        ClubManagerTenure coach=current.stream().filter(t->"HEAD_COACH".equals(t.function)).findFirst().orElse(null);
        add(facts,"manager","Treinador",coach==null?null:coach.name,coach==null?null:"Equipe "+teams.stream().filter(t->t.id.equals(coach.teamId)).map(t->t.name).findFirst().orElse("não informada"));
        List<Fact> metrics=new ArrayList<>();add(metrics,"squad","Jogadores visíveis",roster,"O total respeita a visibilidade dos perfis");
        add(metrics,"teams","Equipes cadastradas",teams.size(),null);
        List<ClubTeam> recruiting=teams.stream().filter(t->t.vacancies!=null).toList();
        add(metrics,"vacancies","Vagas informadas",recruiting.isEmpty()?null:recruiting.stream().mapToInt(t->t.vacancies).sum(),"Por equipe; sem inferir vagas não declaradas");
        ClubCompetitionEntry latest=latest(entries.stream().filter(e->c.getId().equals(e.clubId)).toList(),today);
        return new Card(c.getId(),"CLUBE",c.getDisplayName(),"@"+c.getUsername(),c.isVerified(),null,
            c.getShortName()==null?"Clube de futebol":c.getShortName(),coach==null?null:coach.name,null,null,facts,metrics,context(latest,teams.stream().filter(t->latest!=null&&t.id.equals(latest.teamId)).map(t->t.name).findFirst().orElse(null)),
            "Elenco visível e dados cadastrados pelo clube", "#/clube?clubId="+c.getId());
    }

    static Card manager(Manager m,List<ClubManagerTenure> mandates,Map<UUID,User> clubs,List<ClubCompetitionEntry> entries,List<ManagerCareerTitle> titles,LocalDate today) {
        return manager(m,mandates,clubs,entries,titles,List.of(),today);
    }
    static Card manager(Manager m,List<ClubManagerTenure> mandates,Map<UUID,User> clubs,List<ClubCompetitionEntry> entries,List<ManagerCareerTitle> titles,List<ClubTeam> teams,LocalDate today) {
        List<ClubManagerTenure> own=mandates.stream().filter(t->m.getId().equals(t.managerUserId)).toList();
        boolean active=own.stream().anyMatch(t->ManagerService.current(t,today));
        List<ClubManagerTenure> current=own.stream().filter(t->ManagerService.current(t,today)&&clubs.containsKey(t.clubId))
            .sorted(Comparator.comparing((ClubManagerTenure t)->t.startDate).reversed()).toList();
        ClubManagerTenure main=current.isEmpty()?null:current.get(0);
        List<Fact> facts=new ArrayList<>();add(facts,"function","Função",function(m.getFunction()),null);
        add(facts,"formation","Formação preferida",m.getPreferredFormation(),null);add(facts,"style","Modelo de jogo",m.getPlayingStyle(),null);
        add(facts,"license","Licença",m.getLicenseName(),m.getLicenseLevel());add(facts,"languages","Idiomas",m.getLanguages(),null);
        add(facts,"contract","Contrato até",main==null||main.contractUntil==null?null:main.contractUntil.toString(),main==null?null:clubs.get(main.clubId).getDisplayName());facts.addAll(location(m));
        List<ClubManagerTenure> visibleHistory=own.stream().filter(t->clubs.containsKey(t.clubId)&&!t.startDate.isAfter(today)).toList();
        List<Fact> metrics=new ArrayList<>();add(metrics,"titles","Títulos declarados",titles.size(),"Informados pelo titular; não são títulos verificados automaticamente");
        add(metrics,"clubs","Clubes no histórico",visibleHistory.stream().map(t->t.clubId).distinct().count(),"Vínculos visíveis cadastrados");
        add(metrics,"tenures","Mandatos vigentes",current.size(),"Somente equipes e clubes visíveis");
        ClubCompetitionEntry latest=main==null?null:latest(entries.stream().filter(e->main.teamId.equals(e.teamId)&&main.clubId.equals(e.clubId)).toList(),today);
        String membership=active||Boolean.TRUE.equals(m.getHasCurrentClub())?"Com clube":Boolean.FALSE.equals(m.getHasCurrentClub())?"Manager sem clube":"Vínculo não informado";
        String affiliation=main==null?null:clubs.get(main.clubId).getDisplayName()+(current.size()>1?" + "+(current.size()-1)+" vínculo(s)":"");
        return new Card(m.getId(),"MANAGER",m.getDisplayName(),"@"+m.getUsername(),m.isVerified(),null,function(m.getFunction()),affiliation,membership,
            ManagerService.effectiveTransferSituation(m,active).name(),facts,metrics,context(latest,teams.stream().filter(t->latest!=null&&t.id.equals(latest.teamId)).map(t->t.name).findFirst().orElse(null)),"Carreira cadastrada · títulos declarados pelo titular", "#/manager?managerId="+m.getId());
    }
    static ClubCompetitionEntry latest(List<ClubCompetitionEntry> entries,LocalDate today) {
        return entries.stream().filter(e->e.asOf!=null&&!e.asOf.isAfter(today)).max(Comparator.comparing((ClubCompetitionEntry e)->e.asOf)
            .thenComparing(e->e.season,Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(e->e.updatedAt,Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(e->e.competition,Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
    }
    static Context context(ClubCompetitionEntry e) {return context(e,null);}
    static Context context(ClubCompetitionEntry e,String teamName) {return e==null?null:new Context(e.competition,e.season,e.stage,e.position,e.asOf,e.source,teamName);}
    static List<Fact> location(User u) {List<Fact> out=new ArrayList<>();String place=java.util.stream.Stream.of(u.getCity(),u.getStateCode(),u.getCountryCode()).filter(x->x!=null&&!x.isBlank()).collect(Collectors.joining(" · "));add(out,"location","Localização",place.isBlank()?null:place,null);return out;}
    static void add(List<Fact> out,String key,String label,Object value,String note) {if(value!=null&&!value.toString().isBlank())out.add(new Fact(key,label,value.toString(),note));}
    static String title(String v) {return v.substring(0,1).toUpperCase(Locale.ROOT)+v.substring(1).toLowerCase(Locale.ROOT).replace('_',' ');}
    static String function(String v) {return v==null?"Manager":"HEAD_COACH".equals(v)?"Treinador principal":"SPORTING_DIRECTOR".equals(v)?"Gestor esportivo":"Assistente técnico";}
}
