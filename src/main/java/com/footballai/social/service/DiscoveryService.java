package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** One public-field registry drives both UI and SQL. No entity/User serialization, SQL fragments or client-chosen paths. */
@Service @Transactional(readOnly=true)
public class DiscoveryService {
 public record Field(String key,String label,String entity,String type,List<String> operators) {}
 public record Result(UUID id,String name,String handle,String entity,String country,String state,String city,boolean verified,String category,String club,String market,String href,TransferSituation transferSituation) {}
 public record Results(List<Result> items,long total,int page,int size,int pages) {}
 record Binding(Field descriptor,Class<? extends User> owner,Class<?> relation,String attribute,Class<?> javaType) {}
 record Filter(Binding binding,String op,String value) {}
 private final UserRepository users;private final FollowRepository follows;private final ClubManagerTenureRepository managerTenures;
 private static final Map<String,Binding> FIELDS=new LinkedHashMap<>();
 static {
  common("displayName","Nome",String.class);common("username","Usuário",String.class);common("bio","Biografia",String.class);common("countryCode","País do perfil",String.class);common("stateCode","Estado",String.class);common("city","Cidade",String.class);common("verified","Verificado",boolean.class);
  root(Club.class,"CLUBE","club",new String[]{"legalName","shortName","federationCode","federation","clubeTipo","division","foundedYear","stadium","website","publicContact","colors","ownershipType","president","history","honours","lat","lng"});
  root(Athlete.class,"ATLETA","athlete",new String[]{"position","dominantFoot","heightCm","currentClubId","currentClubName","lastClubName","contractStatus","contractUntil","shirtNumber","captain","categoria","affiliationStatus","freeTransferConfirmed","marketVerifiedOn","marketSource","transferSituation","nationalities","languages","playingStyle"});
  root(Manager.class,"MANAGER","manager",new String[]{"function","nationalities","languages","licenseName","licenseLevel","preferredFormation","playingStyle","careerStartedOn","hasCurrentClub","clubStatusSource","clubStatusAsOf","transferSituation"});
  relatedFor("managerHistory","Carreira do Manager",Manager.class,ClubManagerTenure.class);relatedFor("managerTitle","Títulos do Manager",Manager.class,ManagerCareerTitle.class);
  related("team","Equipe",ClubTeam.class);related("managerTenure","Comissão e histórico",ClubManagerTenure.class);related("competition","Liga / copa e histórico",ClubCompetitionEntry.class);related("finance","Finanças divulgadas",ClubFinanceSnapshot.class);related("venue","Estádio / instalação",ClubVenue.class);
 }
 private static final Map<String,String> LABELS=Map.ofEntries(Map.entry("name","Nome"),Map.entry("category","Categoria"),Map.entry("modality","Modalidade"),Map.entry("gender","Categoria de gênero"),Map.entry("playingStyle","Modelo de jogo"),Map.entry("wantedPositions","Posições procuradas"),Map.entry("preferredFoot","Pé preferido"),Map.entry("languages","Idiomas"),Map.entry("vacancies","Vagas"),Map.entry("competition","Competição"),Map.entry("season","Temporada"),Map.entry("format","Formato"),Map.entry("position","Posição"),Map.entry("division","Divisão"),Map.entry("asOf","Data de referência"),Map.entry("payroll","Folha salarial"),Map.entry("squadValue","Valor do elenco"),Map.entry("currency","Moeda"),Map.entry("startDate","Início"),Map.entry("endDate","Término"));
 // Registration deliberately uses declared fields, not a client-supplied reflection expression.
 static String label(String name){return switch(name){case "affiliationStatus"->"Situação de clube";case "hasCurrentClub"->"Possui clube atual";case "transferSituation"->"Situação de transferência";case "freeTransferConfirmed"->"Transferência livre declarada";case "nationalities"->"Nacionalidades declaradas";case "contractUntil"->"Contrato até";case "currentClubName"->"Clube atual (nome)";case "currentClubId"->"Clube atual (ID)";case "categoria"->"Categoria";case "heightCm"->"Altura (cm)";case "dominantFoot"->"Pé dominante";case "contractStatus"->"Situação contratual";case "shirtNumber"->"Número da camisa";default->name.replaceAll("([a-z])([A-Z])","$1 $2");};}
 static List<String> ops(Class<?> type){if(type==boolean.class||type==Boolean.class)return List.of("eq","isnull");if(type==String.class)return List.of("eq","contains","isnull");if(type.isEnum()||type==UUID.class)return List.of("eq","isnull");return List.of("eq","gte","lte","isnull");}
 static void register(String key,String label,String entity,Class<? extends User> owner,Class<?> related,String attr,Class<?> type){FIELDS.put(key,new Binding(new Field(key,label,entity,type==String.class||type.isEnum()||type==UUID.class?"text":type==LocalDate.class?"date":type==boolean.class||type==Boolean.class?"boolean":type==Instant.class?"datetime":"number",ops(type)),owner,related,attr,type));}
 static void common(String attr,String label,Class<?> type){register(attr,label,"ALL",User.class,null,attr,type);}
 static void root(Class<? extends User> c,String entity,String prefix,String[] fields){for(String f:fields)try{var method=c.getMethod((f.equals("captain")?"is":"get")+Character.toUpperCase(f.charAt(0))+f.substring(1));register(prefix+"."+f,label(f),entity,c,null,f,method.getReturnType());}catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}}
 static void related(String prefix,String group,Class<? extends ClubResource> c){for(var f:c.getFields())if(!Set.of("id","clubId","version","createdAt","updatedAt").contains(f.getName()))register(prefix+"."+f.getName(),group+": "+label(f.getName()),"CLUBE",Club.class,c,f.getName(),f.getType());}
 static void relatedFor(String prefix,String group,Class<? extends User> owner,Class<?> type){
  for(var f:type.getFields())if(!Set.of("id","managerId","managerUserId","version","createdAt","updatedAt").contains(f.getName()))register(prefix+"."+f.getName(),group+": "+label(f.getName()),"MANAGER",owner,type,f.getName(),f.getType());
 }
 public DiscoveryService(UserRepository users,FollowRepository follows,ClubManagerTenureRepository managerTenures){this.users=users;this.follows=follows;this.managerTenures=managerTenures;}
 public List<Field> fields(){return FIELDS.values().stream().map(Binding::descriptor).toList();}
 List<Filter> parse(List<String> raw){if(raw.size()>30)throw ApiException.badRequest("FILTER_LIMIT","Use até 30 filtros");List<Filter> out=new ArrayList<>();for(String f:raw){String[] p=f.split(":",3);Binding b=p.length==3?FIELDS.get(p[0]):null;if(b==null||!b.descriptor.operators.contains(p[1])||p[2].length()>240)throw ApiException.badRequest("FILTER_INVALID","Campo ou operador inválido");if(p[1].equals("isnull")&&!Set.of("true","false").contains(p[2]))throw ApiException.badRequest("FILTER_INVALID","isnull aceita true ou false");out.add(new Filter(b,p[1],p[2]));}return out;}
 public Results search(AuthPrincipal viewer,String entity,String text,List<String> raw,int page,int size,String order){
  if(page<0||page>10000||size<1||size>100)throw ApiException.badRequest("PAGE_INVALID","Página inválida");
  if(text!=null&&text.length()>120)throw ApiException.badRequest("SEARCH_INVALID","Busca muito longa");
  if(!Set.of("ALL","CLUBE","ATLETA","MANAGER").contains(entity))throw ApiException.badRequest("ENTITY_INVALID","Entidade inválida");
  Map<String,String> sort=Map.of("name","displayName","city","city","country","countryCode","recent","createdAt");
  String sortField=sort.get(order);if(sortField==null)throw ApiException.badRequest("SORT_INVALID","Ordem inválida");
  List<Filter> filters=parse(raw);Set<UUID> connections=viewer==null?Set.of():new HashSet<>(follows.findConnectionIds(viewer.userId()));
  Specification<User> spec=(r,q,b)->{
   List<Predicate> ps=new ArrayList<>();ps.add(visible(r,q,b,viewer,connections));
   if(!entity.equals("ALL"))ps.add(b.equal(r.type(),entity.equals("CLUBE")?Club.class:entity.equals("ATLETA")?Athlete.class:Manager.class));
   if(text!=null&&!text.isBlank())ps.add(b.or(b.like(b.lower(r.get("displayName")),"%"+escape(text.toLowerCase(Locale.ROOT))+"%",'\\'),b.like(b.lower(r.get("username")),"%"+escape(text.toLowerCase(Locale.ROOT))+"%",'\\')));
   Map<String,List<Filter>> related=new LinkedHashMap<>();
   for(Filter f:filters){Binding binding=f.binding;if(binding.relation!=null){related.computeIfAbsent(binding.descriptor.key.split("\\.")[0],k->new ArrayList<>()).add(f);continue;}
    if(binding.owner==Manager.class&&binding.attribute.equals("hasCurrentClub")) {
     ps.add(b.equal(r.type(),Manager.class));ps.add(managerClubPredicate(r,q,b,f));continue;
    }
    Expression<?> path;
    if(binding.owner==Manager.class&&binding.attribute.equals("transferSituation"))path=effectiveManagerTransfer(r,q,b);
    else if(binding.owner==Athlete.class&&binding.attribute.equals("transferSituation"))path=effectiveAthleteTransfer(b.treat(r,Athlete.class),b);
    else path=binding.owner==User.class?r.get(binding.attribute):b.treat(r,binding.owner).get(binding.attribute);
    if(binding.owner!=User.class)ps.add(b.equal(r.type(),binding.owner));ps.add(compare(b,path,f));
   }
   for(var group:related.entrySet()){
    Binding context=group.getValue().get(0).binding;Class<?> relation=context.relation;boolean managerContext=context.owner==Manager.class;String ownerField=managerContext?(relation==ManagerCareerTitle.class?"managerId":"managerUserId"):"clubId";
    Subquery<UUID> sq=q.subquery(UUID.class);Root<?> record=sq.from(relation);List<Predicate> predicates=new ArrayList<>();predicates.add(b.equal(record.get(ownerField),r.get("id")));
    if(managerContext&&relation==ClubManagerTenure.class){
     Subquery<UUID> clubAccess=q.subquery(UUID.class);Root<Club> linked=clubAccess.from(Club.class);
     Predicate openClub=b.equal(linked.get("visibility"),ProfileVisibility.PUBLICO);
     if(!connections.isEmpty())openClub=b.or(openClub,b.and(linked.get("id").in(connections),b.equal(linked.get("visibility"),ProfileVisibility.CONEXOES)));
     if(viewer!=null)openClub=b.or(openClub,b.equal(linked.get("id"),viewer.userId()));
     clubAccess.select(linked.get("id")).where(b.equal(linked.get("id"),record.get("clubId")),b.equal(linked.get("status"),AccountStatus.ACTIVE),openClub);
     predicates.add(viewer==null?b.exists(clubAccess):b.or(b.equal(record.get("managerUserId"),viewer.userId()),b.exists(clubAccess)));
    }
    if(relation==ClubFinanceSnapshot.class)predicates.add(viewer==null?b.isTrue(record.get("published")):b.or(b.isTrue(record.get("published")),b.equal(record.get("clubId"),viewer.userId())));
    if(relation==ClubManagerTenure.class&&group.getValue().stream().anyMatch(f->Set.of("transferFee","currency").contains(f.binding.attribute)))predicates.add(viewer==null?b.isTrue(record.get("transferFeePublic")):b.or(b.isTrue(record.get("transferFeePublic")),b.or(b.equal(record.get("clubId"),viewer.userId()),b.equal(record.get("managerUserId"),viewer.userId()))));
    for(Filter f:group.getValue())predicates.add(compare(b,record.get(f.binding.attribute),f));sq.select(record.get(ownerField)).where(predicates.toArray(Predicate[]::new));ps.add(b.equal(r.type(),context.owner));ps.add(b.exists(sq));
   }
   return b.and(ps.toArray(Predicate[]::new));
  };
  Page<User> result=users.findAll(spec,PageRequest.of(page,size,Sort.by(order.equals("recent")?Sort.Direction.DESC:Sort.Direction.ASC,sortField).and(Sort.by("id"))));
  Set<UUID> ids=new HashSet<>();for(User u:result.getContent())if(u instanceof Manager)ids.add(u.getId());
  Set<UUID> activeManagers=new HashSet<>();
  if(!ids.isEmpty()) {
   LocalDate today=LocalDate.now();
   managerTenures.findAll((r,q,b)->b.and(r.get("managerUserId").in(ids),b.lessThanOrEqualTo(r.get("startDate"),today),b.or(b.isNull(r.get("endDate")),b.greaterThanOrEqualTo(r.get("endDate"),today)))).forEach(t->activeManagers.add(t.managerUserId));
  }
  return new Results(result.getContent().stream().map(u->result(u,activeManagers)).toList(),result.getTotalElements(),page,size,result.getTotalPages());
 }
 /** Effective club state is temporal: an active mandate overrides a stale/null declaration. */
 static Predicate managerHasMandate(Root<User> r,CriteriaQuery<?> q,CriteriaBuilder b) {
  Subquery<UUID> active=q.subquery(UUID.class);Root<ClubManagerTenure> t=active.from(ClubManagerTenure.class);LocalDate today=LocalDate.now();
  active.select(t.get("managerUserId")).where(b.equal(t.get("managerUserId"),r.get("id")),b.lessThanOrEqualTo(t.get("startDate"),today),b.or(b.isNull(t.get("endDate")),b.greaterThanOrEqualTo(t.get("endDate"),today)));
  return b.exists(active);
 }
 static Expression<TransferSituation> effectiveManagerTransfer(Root<User> r,CriteriaQuery<?> q,CriteriaBuilder b) {
  Root<Manager> manager=b.treat(r,Manager.class);Path<TransferSituation> declared=manager.get("transferSituation");Path<Boolean> hasClub=manager.get("hasCurrentClub");
  Predicate invalidFree=b.and(b.equal(declared,TransferSituation.FREE_AGENT),b.or(managerHasMandate(r,q,b),b.isNull(hasClub),b.isTrue(hasClub)));
  return b.<TransferSituation>selectCase().when(invalidFree,TransferSituation.NOT_FOR_SALE).otherwise(declared);
 }
 static Expression<TransferSituation> effectiveAthleteTransfer(Root<Athlete> a,CriteriaBuilder b) {
  Path<TransferSituation> declared=a.get("transferSituation");Path<String> state=a.get("affiliationStatus");
  Predicate invalidFree=b.and(b.equal(declared,TransferSituation.FREE_AGENT),b.or(b.isNotNull(a.get("currentClubId")),b.isNull(state),b.notEqual(state,"WITHOUT_CLUB"),b.notEqual(a.get("contractStatus"),ContractStatus.AGENTE_LIVRE)));
  return b.<TransferSituation>selectCase().when(invalidFree,TransferSituation.NOT_FOR_SALE).otherwise(declared);
 }
 static Predicate managerClubPredicate(Root<User> r,CriteriaQuery<?> q,CriteriaBuilder b,Filter f) {
  Predicate hasMandate=managerHasMandate(r,q,b);Path<Boolean> declared=b.treat(r,Manager.class).get("hasCurrentClub");
  if(f.op.equals("isnull"))return f.value.equals("true")?b.and(b.not(hasMandate),b.isNull(declared)):b.or(hasMandate,b.isNotNull(declared));
  List<Predicate> alternatives=new ArrayList<>();
  try {for(String value:f.value.split(","))alternatives.add(parseBoolean(value.trim())?b.or(hasMandate,b.isTrue(declared)):b.and(b.not(hasMandate),b.isFalse(declared)));}
  catch(IllegalArgumentException e){throw ApiException.badRequest("FILTER_VALUE_INVALID","Informe true ou false para vínculo de Manager");}
  return b.or(alternatives.toArray(Predicate[]::new));
 }
 static Predicate visible(Root<User> r,CriteriaQuery<?> q,CriteriaBuilder b,AuthPrincipal p,Set<UUID> connections){
  Subquery<UUID> blocked=q.subquery(UUID.class);Root<Athlete> a=blocked.from(Athlete.class);blocked.select(a.get("id")).where(b.equal(a.get("id"),r.get("id")),b.greaterThan(a.get("birthDate"),LocalDate.now().minusYears(18)),b.isNull(a.get("guardianConsentAt")));
  Predicate pub=b.and(b.equal(r.get("visibility"),ProfileVisibility.PUBLICO),b.not(b.exists(blocked)));
  Predicate allowed=pub;if(p!=null){allowed=b.or(allowed,b.equal(r.get("id"),p.userId()));if(!connections.isEmpty())allowed=b.or(allowed,b.and(r.get("id").in(connections),r.get("visibility").in(ProfileVisibility.PUBLICO,ProfileVisibility.CONEXOES)));}
  return b.and(b.equal(r.get("status"),AccountStatus.ACTIVE),allowed);
 }
 static String escape(String text){return text.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
 @SuppressWarnings({"unchecked","rawtypes"}) static Predicate compare(CriteriaBuilder b,Expression<?> path,Filter f){
  if(f.op.equals("isnull"))return f.value.equals("true")?b.isNull(path):b.isNotNull(path);
  try{
   if(f.op.equals("contains"))return b.like(b.lower(path.as(String.class)),"%"+escape(f.value.toLowerCase(Locale.ROOT))+"%",'\\');
   List<Object> values=new ArrayList<>();for(String raw:f.op.equals("eq")?f.value.split(","):new String[]{f.value}){String v=raw.trim();Class<?> type=f.binding.javaType;
    Object parsed=type==String.class?v:type==UUID.class?UUID.fromString(v):type==LocalDate.class?LocalDate.parse(v):type==Instant.class?Instant.parse(v):type==Integer.class?Integer.valueOf(v):type==BigDecimal.class?new BigDecimal(v):type==boolean.class||type==Boolean.class?parseBoolean(v):type.isEnum()?Enum.valueOf((Class)type,v):v;values.add(parsed);}
   if(f.op.equals("eq"))return path.in(values);
   Comparable value=(Comparable)values.get(0);return f.op.equals("gte")?b.greaterThanOrEqualTo((Expression)path,value):b.lessThanOrEqualTo((Expression)path,value);
  }catch(IllegalArgumentException|java.time.format.DateTimeParseException e){throw ApiException.badRequest("FILTER_VALUE_INVALID","Valor inválido para "+f.binding.descriptor.label);}
 }
 static Boolean parseBoolean(String v){if(!v.equals("true")&&!v.equals("false"))throw new IllegalArgumentException();return Boolean.valueOf(v);}
 static TransferSituation effectiveTransferSituation(Athlete a) {
  TransferSituation declared=a.getTransferSituation()==null?TransferSituation.NOT_FOR_SALE:a.getTransferSituation();
  return declared==TransferSituation.FREE_AGENT && (a.getCurrentClubId()!=null || !"WITHOUT_CLUB".equals(a.getAffiliationStatus()) || a.getContractStatus()!=ContractStatus.AGENTE_LIVRE)
      ? TransferSituation.NOT_FOR_SALE : declared;
 }
 static Result result(User u,Set<UUID> activeManagers){
  String cat=null,club=null,market=null;TransferSituation transfer=null;String href="#/perfil?role="+u.getRole().name()+"&q="+java.net.URLEncoder.encode(Objects.toString(u.getUsername(),""),java.nio.charset.StandardCharsets.UTF_8);
  if(u instanceof Club)href="#/clube?clubId="+u.getId();
  if(u instanceof Athlete a){transfer=effectiveTransferSituation(a);cat=a.getCategoria();club=a.getCurrentClubName();market="WITHOUT_CLUB".equals(a.getAffiliationStatus())?(transfer==TransferSituation.FREE_AGENT?"Sem clube · Agente livre":"Sem clube"):"ATTACHED".equals(a.getAffiliationStatus())?"Vinculado a clube":"Situação não informada";href="#/vitrine?athleteId="+a.getId();}
  if(u instanceof Manager m){transfer=ManagerService.effectiveTransferSituation(m,activeManagers.contains(m.getId()));market=activeManagers.contains(m.getId())?"Com clube":Boolean.FALSE.equals(m.getHasCurrentClub())?(transfer==TransferSituation.FREE_AGENT?"Manager sem clube · Agente livre":"Manager sem clube"):Boolean.TRUE.equals(m.getHasCurrentClub())?"Com clube":"Vínculo não informado";href="#/manager?managerId="+m.getId();}
  return new Result(u.getId(),u.getDisplayName(),u.getUsername(),u.getRole().name(),u.getCountryCode(),u.getStateCode(),u.getCity(),u.isVerified(),cat,club,market,href,transfer);
 }
}
