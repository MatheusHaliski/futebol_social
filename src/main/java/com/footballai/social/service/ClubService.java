package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ClubDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;
import java.lang.reflect.Modifier;

@Service
@Transactional(readOnly=true)
public class ClubService {
 private final ClubRepository clubs; private final AthleteRepository athletes; private final AccessPolicy access;
 private final ClubTeamRepository teams; private final ClubManagerTenureRepository managers;
 private final ClubCompetitionEntryRepository competitions; private final ClubFinanceSnapshotRepository finances;
 private final ClubVenueRepository venues; private final ClubMembershipRepository memberships;
 private final CardAssembler cards; private final ObjectMapper json; private final UserLgpdPreferencesRepository prefs;
 public ClubService(ClubRepository clubs, AthleteRepository athletes, AccessPolicy access, ClubTeamRepository teams,
   ClubManagerTenureRepository managers, ClubCompetitionEntryRepository competitions, ClubFinanceSnapshotRepository finances,
   ClubVenueRepository venues, ClubMembershipRepository memberships, CardAssembler cards, ObjectMapper json, UserLgpdPreferencesRepository prefs) {
  this.clubs=clubs;this.athletes=athletes;this.access=access;this.teams=teams;this.managers=managers;
  this.competitions=competitions;this.finances=finances;this.venues=venues;this.memberships=memberships;this.cards=cards;this.json=json;this.prefs=prefs;
 }
 public Club visible(AuthPrincipal p,UUID id) {
  Club c=clubs.findById(id).orElseThrow(()->ApiException.notFound("Clube"));
  if(!access.canViewProfile(p,c)) throw ApiException.notFound("Clube"); return c;
 }
 public Club owner(AuthPrincipal p,UUID id) {
  if(p==null || !p.userId().equals(id) || p.role()!=UserRole.CLUBE) throw ApiException.forbidden("CLUB_OWNER_REQUIRED","Só o clube pode editar seu perfil");
  return clubs.locked(id).orElseThrow(()->ApiException.notFound("Clube"));
 }
 public static Summary summary(Club c) { return new Summary(c.getId(),c.getUsername(),new Identity(c.getDisplayName(),c.getLegalName(),c.getShortName(),c.getFederationCode(),c.getFederation(),c.getClubeTipo(),c.getDivision(),c.getFoundedYear(),c.getCountryCode(),c.getStateCode(),c.getCity(),c.getWebsite(),c.getPublicContact(),c.getColors(),c.getOwnershipType(),c.getPresident(),c.getHistory(),c.getHonours()),c.isVerified()); }
 public Profile profile(AuthPrincipal p,UUID id) {
  Club c=visible(p,id);boolean own=p!=null && p.userId().equals(id);
  List<Athlete> squad=athletes.findAll((r,q,b)->b.equal(r.get("currentClubId"),id)).stream().filter(a->access.canViewProfile(p,a)).toList();
  return new Profile(summary(c),own,teams.findAllByClubId(id),managers.findAllByClubId(id).stream().map(m->managerView(m,own)).toList(),
   competitions.findAllByClubId(id),finances.findAllByClubId(id).stream().filter(f->own||f.published).toList(),venues.findAllByClubId(id),
   memberships.findAllByClubId(id).stream().filter(m->own || !"PENDING".equals(m.status)).filter(m->athletes.findById(m.athleteId).map(a->access.canViewProfile(p,a)).orElse(false)).toList(),cards.athleteCards(squad,p));
 }
 private Map<String,Object> managerView(ClubManagerTenure m,boolean own){
  Map<String,Object> out=json.convertValue(m,new TypeReference<Map<String,Object>>(){});
  out.put("current",active(m.startDate,m.endDate));
  if(!own&&!m.transferFeePublic) {out.remove("transferFee");out.remove("currency");} return out;
 }
 @Transactional public Summary update(AuthPrincipal p,UUID id,Identity r) {
  Club c=owner(p,id); c.setDisplayName(r.name());c.setLegalName(r.legalName());c.setShortName(r.shortName());c.setFederationCode(r.federationCode());c.setFederation(r.federation());c.setClubeTipo(r.type());c.setDivision(r.division());c.setFoundedYear(r.foundedYear());c.setCountryCode(r.country());c.setStateCode(r.state());c.setCity(r.city());c.setWebsite(r.website());c.setPublicContact(r.publicContact());c.setColors(r.colors());c.setOwnershipType(r.ownershipType());c.setPresident(r.president());c.setHistory(r.history());c.setHonours(r.honours());return summary(clubs.save(c));
 }
 private void team(UUID club,UUID teamId){ if(teamId!=null && !teams.findById(teamId).map(t->club.equals(t.clubId)).orElse(false)) throw ApiException.badRequest("TEAM_INVALID","Equipe não pertence ao clube"); }
 public static boolean active(LocalDate start,LocalDate end) { LocalDate now=LocalDate.now(Clock.systemUTC());return start!=null&&!start.isAfter(now)&&(end==null||!end.isBefore(now)); }
 private static void dates(LocalDate start,LocalDate end){ if(start!=null&&end!=null&&end.isBefore(start))throw ApiException.badRequest("DATES_INVALID","Fim deve ser posterior ao início"); }
 private void validate(ClubResource r) {
  if(r instanceof ClubTeam t) { tokens(t.wantedPositions,Arrays.stream(Position.values()).map(Enum::name).toList()); }
  if(r instanceof ClubManagerTenure m) {team(r.clubId,m.teamId);dates(m.startDate,m.endDate);dates(m.startDate,m.contractUntil);
   if(m.transferFee!=null&&m.currency==null)throw ApiException.badRequest("CURRENCY_REQUIRED","Informe a moeda da taxa");
   if(m.managerUserId!=null && !access.requireProfile(null,m.managerUserId,"Manager").getRole().name().equals("MANAGER"))throw ApiException.badRequest("MANAGER_INVALID","Informe um perfil Manager público");
   if(managers.findAllByClubId(r.clubId).stream().anyMatch(x->!Objects.equals(x.id,m.id)&&x.teamId.equals(m.teamId)&&x.function.equals(m.function)&&overlaps(x.startDate,x.endDate,m.startDate,m.endDate)))throw ApiException.conflict("MANAGER_OVERLAP","Já há um mandato desta função no período. Encerre o anterior primeiro.");
  }
  if(r instanceof ClubCompetitionEntry c) {team(r.clubId,c.teamId);if(c.asOf.isAfter(LocalDate.now()))throw ApiException.badRequest("FUTURE_SNAPSHOT","Classificação deve ter data de referência atual ou passada");}
  if(r instanceof ClubFinanceSnapshot f) {team(r.clubId,f.teamId);}
  if(r instanceof ClubVenue v) dates(v.startDate,v.endDate);
  if(r instanceof ClubMembership m){team(r.clubId,m.teamId);dates(m.startDate,m.endDate);}
 }
 static boolean overlaps(LocalDate a,LocalDate b,LocalDate c,LocalDate d){return (b==null||!b.isBefore(c))&&(d==null||!d.isBefore(a));}
 static void tokens(String text,List<String> allowed){if(text!=null&&!text.isBlank())for(String t:text.split(","))if(!allowed.contains(t.trim().toUpperCase(Locale.ROOT)))throw ApiException.badRequest("TOKEN_INVALID","Valor inválido: "+t);}
 private <T extends ClubResource> T save(AuthPrincipal p,UUID club,UUID id,T data,JpaRepository<T,UUID> repo){
  owner(p,club);T target=data;
  if(id!=null){target=repo.findById(id).orElseThrow(()->ApiException.notFound("Registro"));if(!club.equals(target.clubId))throw ApiException.notFound("Registro");
   try {for(var f:data.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers()))f.set(target,f.get(data));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
   target.source=data.source;
  }else target.createdAt=Instant.now();
  target.clubId=club;target.updatedAt=Instant.now();validate(target);return repo.save(target);
 }
 @Transactional public ClubTeam team(AuthPrincipal p,UUID c,UUID id,ClubTeam r){return save(p,c,id,r,teams);}
 @Transactional public ClubManagerTenure manager(AuthPrincipal p,UUID c,UUID id,ClubManagerTenure r){return save(p,c,id,r,managers);}
 @Transactional public ClubCompetitionEntry competition(AuthPrincipal p,UUID c,UUID id,ClubCompetitionEntry r){return save(p,c,id,r,competitions);}
 @Transactional public ClubFinanceSnapshot finance(AuthPrincipal p,UUID c,UUID id,ClubFinanceSnapshot r){return save(p,c,id,r,finances);}
 @Transactional public ClubVenue venue(AuthPrincipal p,UUID c,UUID id,ClubVenue r){return save(p,c,id,r,venues);}
 @Transactional public ClubMembership invite(AuthPrincipal p,UUID c,ClubMembership r){
  owner(p,c);access.requireProfile(p,r.athleteId,"Atleta");if(!athletes.existsById(r.athleteId))throw ApiException.notFound("Atleta");
  if(memberships.findAllByClubId(c).stream().anyMatch(m->m.athleteId.equals(r.athleteId)&&("PENDING".equals(m.status)||"ACTIVE".equals(m.status))))throw ApiException.conflict("MEMBERSHIP_EXISTS","Já existe convite ou vínculo ativo");
  r.status="PENDING";return save(p,c,null,r,memberships);
 }
 public List<ClubMembership> invitations(AuthPrincipal p){return memberships.findAll((r,q,b)->b.and(b.equal(r.get("athleteId"),p.userId()),b.equal(r.get("status"),"PENDING")));}
 @Transactional public ClubMembership accept(AuthPrincipal p,UUID id,boolean accept){
  Athlete a=athletes.locked(p.userId()).orElseThrow(()->ApiException.notFound("Atleta"));
  ClubMembership m=memberships.findById(id).orElseThrow(()->ApiException.notFound("Convite"));
  if(!a.getId().equals(m.athleteId))throw ApiException.notFound("Convite");
  if(!"PENDING".equals(m.status))throw ApiException.conflict("INVITATION_CLOSED","Convite já respondido");
  if(accept){if(!active(m.startDate,m.endDate))throw ApiException.badRequest("MEMBERSHIP_DATES","Só aceite vínculos vigentes");
   List<ClubMembership> old=memberships.findAll((r,q,b)->b.and(b.equal(r.get("athleteId"),a.getId()),b.equal(r.get("status"),"ACTIVE")));
   for(ClubMembership prev:old){prev.status="ENDED";prev.endDate=LocalDate.now();prev.updatedAt=Instant.now();}
   a.setLastClubName(a.getCurrentClubName());a.setCurrentClubId(m.clubId);a.setCurrentClubName(clubs.findById(m.clubId).orElseThrow().getDisplayName());a.setAffiliationStatus("ATTACHED");a.setContractStatus(ContractStatus.CONTRATADO);a.setTransferSituation(TransferSituation.NOT_FOR_SALE);a.setFreeTransferConfirmed(false);a.setMarketVerifiedOn(LocalDate.now());a.setMarketSource(m.source);a.setShirtNumber(m.shirtNumber);a.setCaptain(m.captain);m.status="ACTIVE";
  }else m.status="REJECTED";m.updatedAt=Instant.now();return m;
 }
 @Transactional public ClubMembership end(AuthPrincipal p,UUID club,UUID id){
  ClubMembership m=memberships.findById(id).orElseThrow(()->ApiException.notFound("Vínculo"));
  if(!m.clubId.equals(club))throw ApiException.notFound("Vínculo");
  if(!p.userId().equals(m.athleteId))owner(p,club);
  Athlete a=athletes.locked(m.athleteId).orElseThrow();
  if(!"ACTIVE".equals(m.status))throw ApiException.conflict("MEMBERSHIP_INACTIVE","Vínculo não está ativo");
  m.status="ENDED";m.endDate=LocalDate.now();m.updatedAt=Instant.now();
  if(club.equals(a.getCurrentClubId())){a.setLastClubName(a.getCurrentClubName());a.setCurrentClubId(null);a.setCurrentClubName(null);a.setAffiliationStatus("WITHOUT_CLUB");a.setContractStatus(ContractStatus.AGENTE_LIVRE);a.setTransferSituation(TransferSituation.FREE_AGENT);a.setContractUntil(null);a.setFreeTransferConfirmed(false);a.setMarketVerifiedOn(LocalDate.now());a.setMarketSource("Fim de vínculo confirmado na plataforma");}return m;
 }
 /** Reconcile explicitly dated sporting relationships; never infer free-transfer eligibility. */
 @org.springframework.scheduling.annotation.Scheduled(fixedDelay=60000)
 @Transactional public void reconcileExpiredMemberships(){
  LocalDate today=LocalDate.now(Clock.systemUTC());
  List<ClubMembership> expired=memberships.findAll((r,q,b)->b.and(b.equal(r.get("status"),"ACTIVE"),b.lessThan(r.get("endDate"),today)));
  for(ClubMembership m:expired){
   Athlete a=athletes.locked(m.athleteId).orElse(null);m.status="ENDED";m.updatedAt=Instant.now();
   if(a!=null&&m.clubId.equals(a.getCurrentClubId())){
    a.setLastClubName(a.getCurrentClubName());a.setCurrentClubId(null);a.setCurrentClubName(null);
    a.setAffiliationStatus("WITHOUT_CLUB");a.setContractStatus(ContractStatus.AGENTE_LIVRE);a.setTransferSituation(TransferSituation.FREE_AGENT);
    a.setContractUntil(null);a.setFreeTransferConfirmed(false);a.setMarketVerifiedOn(today);a.setMarketSource("Fim do período de vínculo registrado na plataforma");
   }
  }
 }
 public Map<String,Object> market(AuthPrincipal p){Athlete a=athletes.findById(p.userId()).orElseThrow(()->ApiException.notFound("Atleta"));
  Map<String,Object> out=new LinkedHashMap<>();out.put("transferSituation",a.getTransferSituation());out.put("affiliationStatus",a.getAffiliationStatus());out.put("freeTransferConfirmed",a.getFreeTransferConfirmed());out.put("source",a.getMarketSource());out.put("externalClubName",a.getCurrentClubName());out.put("currentClubId",a.getCurrentClubId());out.put("lastClubName",a.getLastClubName());out.put("contractStatus",a.getContractStatus());out.put("contractUntil",a.getContractUntil());out.put("position",a.getPosition());out.put("dominantFoot",a.getDominantFoot());out.put("heightCm",a.getHeightCm());out.put("category",a.getCategoria());out.put("nationalities",a.getNationalities());out.put("languages",a.getLanguages());out.put("playingStyle",a.getPlayingStyle());out.put("country",a.getCountryCode());out.put("state",a.getStateCode());out.put("city",a.getCity());return out;}
 @Transactional public Map<String,Object> market(AuthPrincipal p,Market r){
  Athlete a=athletes.locked(p.userId()).orElseThrow(()->ApiException.notFound("Atleta"));
  ContractStatus effective=r.contractStatus()==null?a.getContractStatus():ContractStatus.valueOf(r.contractStatus());
  TransferSituation transfer=r.transferSituation()==null?a.getTransferSituation():r.transferSituation();
  if(transfer==TransferSituation.FREE_AGENT&&(!"WITHOUT_CLUB".equals(r.affiliationStatus())||effective!=ContractStatus.AGENTE_LIVRE))throw ApiException.badRequest("FREE_AGENT_CONFLICT","Agente livre exige declaração sem clube e sem contrato ativo");
  if(a.getCurrentClubId()!=null && (!"ATTACHED".equals(r.affiliationStatus())||r.freeTransferConfirmed()||effective==ContractStatus.AGENTE_LIVRE))throw ApiException.conflict("ACTIVE_MEMBERSHIP","Encerre o vínculo atual antes de declarar-se sem clube");
  if(r.freeTransferConfirmed()&&(!"WITHOUT_CLUB".equals(r.affiliationStatus())||effective!=ContractStatus.AGENTE_LIVRE))throw ApiException.badRequest("FREE_TRANSFER_INVALID","Transferência livre requer sem clube e agente livre");
  if(!"UNKNOWN".equals(r.affiliationStatus())&&(r.source()==null||r.source().isBlank()))throw ApiException.badRequest("SOURCE_REQUIRED","Informe a origem da declaração");
  if(a.getCurrentClubId()==null&&"ATTACHED".equals(r.affiliationStatus())&&(r.externalClubName()==null||r.externalClubName().isBlank()))throw ApiException.badRequest("CLUB_NAME_REQUIRED","Informe o clube externo");
  if("WITHOUT_CLUB".equals(r.affiliationStatus())&&effective==ContractStatus.CONTRATADO)throw ApiException.badRequest("MARKET_CONFLICT","Sem clube não pode manter contrato ativo");
  a.setTransferSituation(transfer);a.setAffiliationStatus(r.affiliationStatus());a.setFreeTransferConfirmed(r.freeTransferConfirmed());a.setMarketSource(r.source());a.setMarketVerifiedOn(LocalDate.now());
  if(a.getCurrentClubId()==null)a.setCurrentClubName("ATTACHED".equals(r.affiliationStatus())?r.externalClubName():null);
  a.setLastClubName(r.lastClubName());if(r.contractStatus()!=null)a.setContractStatus(ContractStatus.valueOf(r.contractStatus()));a.setContractUntil(r.contractUntil());a.setPosition(r.position());a.setDominantFoot(r.dominantFoot());a.setHeightCm(r.heightCm());a.setCategoria(r.category());a.setNationalities(r.nationalities());a.setLanguages(r.languages());a.setPlayingStyle(r.playingStyle());a.setCountryCode(r.country());a.setStateCode(r.state());a.setCity(r.city());return market(p);
 }
 public Compatibility compatibility(AuthPrincipal p,UUID athlete,UUID club,UUID team){
  User target=access.requireProfile(p,athlete,"Atleta");if(!(target instanceof Athlete a))throw ApiException.notFound("Atleta");Club c=visible(p,club);if(team==null)throw ApiException.badRequest("TEAM_REQUIRED","Selecione uma equipe");team(club,team);
  if(a.isMinor()||!prefs.findById(a.getId()).map(UserLgpdPreferences::isAiAgentProcessing).orElse(false))throw ApiException.forbidden("AI_PROCESSING_OPT_IN","O atleta deve ser adulto e autorizar processamento por IA");
  ClubTeam t=teams.findById(team).orElseThrow(()->ApiException.notFound("Equipe"));return calculate(a,c,t);
 }
 static Compatibility calculate(Athlete a,Club c,ClubTeam t){
  List<Factor> fs=List.of(factor("position","Posição procurada",35,a.getPosition()==null?null:a.getPosition().name(),t.wantedPositions),factor("category","Categoria",20,a.getCategoria(),t.category),factor("playingStyle","Modelo de jogo",20,a.getPlayingStyle(),t.playingStyle),factor("languages","Idiomas",10,a.getLanguages(),t.languages),factor("dominantFoot","Pé preferido",10,a.getDominantFoot(),t.preferredFoot),factor("country","Adaptação geográfica (país do perfil)",2,a.getCountryCode(),c.getCountryCode()),factor("state","Estado",1,a.getStateCode(),c.getStateCode()),factor("city","Cidade",2,a.getCity(),c.getCity()));
  int coverage=fs.stream().filter(f->f.match()!=null).mapToInt(Factor::weight).sum();int points=fs.stream().filter(f->f.match()!=null).mapToInt(f->f.weight()*f.match()).sum();
  return new Compatibility(a.getId(),c.getId(),t.id,coverage<50?null:Math.round((float)points/coverage),coverage,"club-fit-v1",coverage<50?"INSUFFICIENT_DATA":"AVAILABLE",fs,Instant.now());
 }
 static Factor factor(String key,String label,int weight,String value,String expected){
  if(value==null||value.isBlank()||expected==null||expected.isBlank())return new Factor(key,label,weight,null,"Dados não informados em um dos perfis");
  Set<String> values=new HashSet<>(Arrays.stream(value.split(",")).map(s->s.trim().toUpperCase(Locale.ROOT)).toList());boolean match=Arrays.stream(expected.split(",")).map(s->s.trim().toUpperCase(Locale.ROOT)).anyMatch(values::contains);
  return new Factor(key,label,weight,match?100:0,match?"Há correspondência entre os perfis":"Sem correspondência neste critério");
 }
}
