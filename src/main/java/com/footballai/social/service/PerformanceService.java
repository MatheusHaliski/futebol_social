package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.PerformanceDtos.*;
import com.footballai.social.model.cassandra.TelemetryData;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import com.footballai.social.repository.cassandra.TelemetryDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Ingestão das entradas do Gêmeo: telemetria (Cassandra + resumo semanal), avaliações, partidas e lesões. */
@Service
public class PerformanceService {
    private static final Logger log = LoggerFactory.getLogger(PerformanceService.class);

    private final TrainingSummaryRepository training;
    private final TechnicalEvaluationRepository evaluations;
    private final MatchPerformanceRepository matches;
    private final InjuryRecordRepository injuries;
    private final DigitalTwinRepository twins;
    private final UserLgpdPreferencesRepository prefs;
    private final UserService users;
    private final BadgeAwardService badges;
    private final PostRepository posts;
    private final ObjectProvider<TelemetryDataRepository> telemetry;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public PerformanceService(TrainingSummaryRepository training, TechnicalEvaluationRepository evaluations, MatchPerformanceRepository matches, InjuryRecordRepository injuries,
                              DigitalTwinRepository twins, UserLgpdPreferencesRepository prefs, UserService users, BadgeAwardService badges, PostRepository posts,
                              ObjectProvider<TelemetryDataRepository> telemetry) {
        this(training, evaluations, matches, injuries, twins, prefs, users, badges, posts, telemetry, Clock.systemUTC());
    }

    PerformanceService(TrainingSummaryRepository training, TechnicalEvaluationRepository evaluations, MatchPerformanceRepository matches, InjuryRecordRepository injuries,
                       DigitalTwinRepository twins, UserLgpdPreferencesRepository prefs, UserService users, BadgeAwardService badges, PostRepository posts,
                       ObjectProvider<TelemetryDataRepository> telemetry, Clock clock) {
        this.training = training; this.evaluations = evaluations; this.matches = matches; this.injuries = injuries; this.twins = twins; this.prefs = prefs;
        this.users = users; this.badges = badges; this.posts = posts; this.telemetry = telemetry; this.clock = clock;
    }

    /* ---------------- telemetria (30 %) ---------------- */

    /** Carga da sessão = RPE × minutos (AU). ACWR = carga da semana / média das 4 últimas. Prontidão cai com ACWR e RPE altos. */
    @Transactional
    public TrainingWeekDTO recordTelemetry(AuthPrincipal p, TelemetryRequest r) {
        requireAthlete(p);
        LocalDate today = LocalDate.now(clock);
        String week = IsoWeeks.of(today);
        TrainingSummary ts = training.findByAthleteIdAndIsoWeek(p.userId(), week).orElseGet(() -> { TrainingSummary n = new TrainingSummary(); n.setAthleteId(p.userId()); n.setIsoWeek(week); n.setSessionsPlanned(5); return n; });
        BigDecimal load = BigDecimal.valueOf((long) r.rpe() * r.durationMin());
        ts.setLoadAu(ts.getLoadAu().add(load));
        ts.setSessionsDone(ts.getSessionsDone() + 1);
        ts.setAvgRpe(ts.getAvgRpe() == null ? BigDecimal.valueOf(r.rpe()) : ts.getAvgRpe().multiply(BigDecimal.valueOf(ts.getSessionsDone() - 1)).add(BigDecimal.valueOf(r.rpe())).divide(BigDecimal.valueOf(ts.getSessionsDone()), 2, RoundingMode.HALF_EVEN));
        if (r.distanceKm() != null) ts.setDistanceKm((ts.getDistanceKm() == null ? BigDecimal.ZERO : ts.getDistanceKm()).add(r.distanceKm()));
        ts.setChronicLoadAu(chronicLoad(p.userId(), week, ts.getLoadAu()));
        ts.setReadiness(readiness(ts.acwr(), ts.getAvgRpe()));
        training.save(ts);
        // telemetria bruta (Cassandra) — GPS só com locationHistory = true
        TelemetryDataRepository repo = telemetry.getIfAvailable();
        if (repo != null) {
            try {
                boolean gps = prefs.findById(p.userId()).map(UserLgpdPreferences::isLocationHistory).orElse(false);
                TelemetryData t = new TelemetryData();
                t.setKey(new TelemetryData.Key(p.userId(), today.toString().substring(0, 7), clock.instant()));
                t.setEventType(r.eventType().name()); t.setMetricType(r.metricType().name()); t.setDurationMin(r.durationMin()); t.setRpe(r.rpe());
                t.setDistanceKm(r.distanceKm()); t.setMaxSpeedKmh(r.maxSpeedKmh()); t.setLoadAu(load); t.setPassesOk(r.passesOk()); t.setPassesTotal(r.passesTotal());
                t.setGpsTrack(gps ? r.gpsTrackGeoJson() : null);
                repo.save(t);
            } catch (RuntimeException e) { log.warn("telemetria Cassandra falhou: {}", e.getMessage()); }
        }
        PhysicalStatus status = refreshPhysicalStatus(p.userId(), ts);
        return toDto(ts, status);
    }

    BigDecimal chronicLoad(UUID athleteId, String currentWeek, BigDecimal currentLoad) {
        List<TrainingSummary> prev = training.findAllByAthleteIdOrderByIsoWeekDesc(athleteId).stream().filter(t -> !t.getIsoWeek().equals(currentWeek)).limit(3).toList();
        BigDecimal sum = currentLoad; int n = 1;
        for (TrainingSummary t : prev) { sum = sum.add(t.getLoadAu()); n++; }
        return sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_EVEN);
    }

    /** 100 − penalidades: ACWR fora de 0,8–1,3 (até −30) e RPE médio alto (até −20). */
    static int readiness(BigDecimal acwr, BigDecimal avgRpe) {
        int r = 100;
        if (acwr.compareTo(new BigDecimal("1.3")) > 0) r -= Math.min(30, acwr.subtract(new BigDecimal("1.3")).multiply(BigDecimal.valueOf(100)).intValue());
        if (acwr.compareTo(new BigDecimal("0.8")) < 0) r -= 10;
        if (avgRpe != null && avgRpe.compareTo(new BigDecimal("7")) > 0) r -= Math.min(20, avgRpe.subtract(new BigDecimal("7")).multiply(BigDecimal.valueOf(10)).intValue());
        return Math.max(0, r);
    }

    @Transactional
    public TrainingWeekDTO setPlan(AuthPrincipal p, PlanRequest r) {
        requireAthlete(p);
        String week = r.isoWeek() == null ? IsoWeeks.of(LocalDate.now(clock)) : r.isoWeek();
        TrainingSummary ts = training.findByAthleteIdAndIsoWeek(p.userId(), week).orElseGet(() -> { TrainingSummary n = new TrainingSummary(); n.setAthleteId(p.userId()); n.setIsoWeek(week); return n; });
        ts.setSessionsPlanned(r.sessionsPlanned());
        training.save(ts);
        return toDto(ts, twins.findByUserId(p.userId()).map(DigitalTwin::getPhysicalStatus).orElse(PhysicalStatus.READY));
    }

    public List<TrainingWeekDTO> load(AuthPrincipal viewer, UUID athleteId) {
        if (!canSeeMedical(viewer, athleteId)) throw ApiException.forbidden("MEDICAL_DATA_PRIVATE", "Carga e recuperação são privadas por padrão (Art. 11)");
        PhysicalStatus st = twins.findByUserId(athleteId).map(DigitalTwin::getPhysicalStatus).orElse(PhysicalStatus.READY);
        return training.findAllByAthleteIdOrderByIsoWeekDesc(athleteId).stream().limit(8).map(t -> toDto(t, st)).toList();
    }

    private TrainingWeekDTO toDto(TrainingSummary t, PhysicalStatus st) {
        return new TrainingWeekDTO(t.getIsoWeek(), t.getSessionsPlanned(), t.getSessionsDone(), t.getLoadAu(), t.getChronicLoadAu(), t.acwr(), t.getAvgRpe(), t.getDistanceKm(), t.getReadiness(), st.name());
    }

    private PhysicalStatus refreshPhysicalStatus(UUID athleteId, TrainingSummary ts) {
        boolean injured = !injuries.findAllByAthleteIdAndClearedAtIsNull(athleteId).isEmpty();
        PhysicalStatus st = BadgeAwardService.derivePhysicalStatus(injured, ts == null ? null : ts.acwr(), ts == null ? 100 : ts.getReadiness());
        badges.syncPhysicalStatus(athleteId, IsoWeeks.season(LocalDate.now(clock)), st, clock.instant());
        return st;
    }

    /* ---------------- avaliações (40 %) ---------------- */

    @Transactional
    public EvaluationDTO evaluate(AuthPrincipal p, EvaluationRequest r) {
        User evaluator = users.require(p.userId());
        UserRole role = evaluator.getRole();
        VerificationLevel level = UserService.verificationLevelOf(evaluator);
        BigDecimal weight; boolean counts;
        switch (role) {
            case STAFF, CLUBE -> { weight = BigDecimal.ONE; counts = true; }
            case SCOUT -> { weight = level == VerificationLevel.HOMOLOGADO ? new BigDecimal("0.80") : new BigDecimal("0.50"); counts = level.countsForRarity(); }
            case IMPRENSA, TORCEDOR, AGENTE -> { weight = new BigDecimal("0.50"); counts = false; }
            default -> throw ApiException.forbidden("CANNOT_EVALUATE", "Papel " + role.label() + " não avalia atletas");
        }
        if (r.athleteId().equals(p.userId())) throw ApiException.forbidden("SELF_EVALUATION", "Não é possível avaliar a si mesmo");
        TechnicalEvaluation e = new TechnicalEvaluation();
        e.setAthleteId(r.athleteId()); e.setEvaluatorId(p.userId()); e.setEvaluatorRole(role); e.setRating(r.rating()); e.setWeight(weight);
        e.setDimension(r.dimension()); e.setWeekOf(r.weekOf() == null ? LocalDate.now(clock) : r.weekOf()); e.setComment(r.comment()); e.setCountsForRarity(counts);
        e = evaluations.save(e);
        return new EvaluationDTO(e.getId(), e.getAthleteId(), role.name(), e.getRating(), e.getWeight(), e.getDimension().name(), e.getWeekOf(), e.isCountsForRarity());
    }

    /* ---------------- partidas (30 %) ---------------- */

    @Transactional
    public MatchPerformance recordMatch(AuthPrincipal p, MatchRequest r) {
        User who = users.require(p.userId());
        UUID athleteId = r.athleteId() == null ? p.userId() : r.athleteId();
        boolean official = r.official();
        if (athleteId.equals(p.userId())) official = false; // auto-relato nunca é oficial
        else if (!(who.getRole() == UserRole.STAFF || who.getRole() == UserRole.CLUBE || who.getRole() == UserRole.OFICIAL_LIGA))
            throw ApiException.forbidden("CANNOT_REPORT_MATCH", "Só comissão, clube ou liga registram partidas de terceiros");
        MatchPerformance m = new MatchPerformance();
        m.setAthleteId(athleteId); m.setMatchDate(r.matchDate()); m.setCompetition(r.competition()); m.setOfficial(official); m.setMinutesPlayed(r.minutesPlayed());
        m.setRating(r.rating()); m.setGoals(r.goals()); m.setAssists(r.assists()); m.setCleanSheet(r.cleanSheet()); m.setTackles(r.tackles());
        m.setYellowCards(r.yellowCards()); m.setRedCards(r.redCards()); m.setMvp(r.mvp()); m.setSeason(IsoWeeks.season(r.matchDate()));
        m = matches.save(m);
        badges.onWeeklyScore(athleteId, IsoWeeks.of(r.matchDate()));
        return m;
    }

    /* ---------------- lesões (sensível) ---------------- */

    @Transactional
    public InjuryDTO reportInjury(AuthPrincipal p, InjuryRequest r) {
        User who = users.require(p.userId());
        UUID athleteId = r.athleteId() == null ? p.userId() : r.athleteId();
        boolean self = athleteId.equals(p.userId());
        if (!self && !(who instanceof HealthStaff hs && hs.canOriginateInjuryRecord()))
            throw ApiException.forbidden("CANNOT_REPORT_INJURY", "Só o próprio atleta ou staff de saúde verificado registra lesões (Art. 11)");
        InjuryRecord i = new InjuryRecord();
        i.setAthleteId(athleteId); i.setInjuryType(r.injuryType()); i.setBodyPart(r.bodyPart()); i.setSeverity(r.severity());
        i.setStartedAt(r.startedAt() == null ? LocalDate.now(clock) : r.startedAt()); i.setExpectedReturnDate(r.expectedReturnDate()); i.setReportedBy(p.userId()); i.setNotes(r.notes());
        i = injuries.save(i);
        TrainingSummary ts = training.findFirstByAthleteIdOrderByIsoWeekDesc(athleteId).orElse(null);
        refreshPhysicalStatus(athleteId, ts);
        if (r.publishAlert()) {
            Post post = new Post();
            post.setAuthorId(p.userId()); post.setAuthorRole(who.getRole()); post.setContentType(PostContentType.LESAO_ALERTA); post.setWindow(FeedWindow.ALERTA);
            post.setTitle("Atualização médica: afastamento com retorno previsto em " + (r.expectedReturnDate() == null ? "data a definir" : r.expectedReturnDate()));
            post.setSubtitle("Departamento médico confirma diagnóstico e prazo de retorno."); post.setSeverity(AlertSeverity.ALTA); post.setAlertType(AlertType.LESAO);
            post.setPublishedAt(clock.instant()); post.setStateCode(who.getStateCode());
            posts.save(post);
        }
        return toDto(i);
    }

    @Transactional
    public InjuryDTO clearInjury(AuthPrincipal p, UUID id) {
        InjuryRecord i = injuries.findById(id).orElseThrow(() -> ApiException.notFound("Lesão"));
        User who = users.require(p.userId());
        if (!i.getAthleteId().equals(p.userId()) && !(who instanceof HealthStaff hs && hs.canOriginateInjuryRecord())) throw ApiException.forbidden("CANNOT_CLEAR", "Sem permissão");
        i.setClearedAt(LocalDate.now(clock)); injuries.save(i);
        refreshPhysicalStatus(i.getAthleteId(), training.findFirstByAthleteIdOrderByIsoWeekDesc(i.getAthleteId()).orElse(null));
        return toDto(i);
    }

    public List<InjuryDTO> injuries(AuthPrincipal viewer, UUID athleteId) {
        if (!canSeeMedical(viewer, athleteId)) throw ApiException.forbidden("MEDICAL_DATA_PRIVATE", "Dados de saúde não compartilhados (Art. 11)");
        return injuries.findAllByAthleteId(athleteId).stream().map(this::toDto).toList();
    }

    private InjuryDTO toDto(InjuryRecord i) { return new InjuryDTO(i.getId(), i.getAthleteId(), i.getInjuryType(), i.getBodyPart(), i.getSeverity(), i.getStartedAt(), i.getExpectedReturnDate(), i.getClearedAt(), i.isActive()); }

    /* ---------------- util ---------------- */

    boolean canSeeMedical(AuthPrincipal viewer, UUID athleteId) {
        if (viewer.userId().equals(athleteId)) return true;
        if (viewer.role() == UserRole.STAFF_SAUDE) return true; // staff de saúde verificado opera sob Art. 11, II (tutela da saúde)
        return prefs.findById(athleteId).map(UserLgpdPreferences::isShareMedicalData).orElse(false);
    }

    private void requireAthlete(AuthPrincipal p) { if (p.role() != UserRole.ATLETA) throw ApiException.forbidden("ATHLETE_ONLY", "Só atletas registram telemetria"); }

    public Instant now() { return clock.instant(); }
    public ZoneOffset zone() { return ZoneOffset.UTC; }
}
