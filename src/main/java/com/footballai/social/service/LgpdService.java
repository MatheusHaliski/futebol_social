package com.footballai.social.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballai.social.config.AppProperties;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.LgpdDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.ExportStatus;
import com.footballai.social.model.enums.LgpdPreference;
import com.footballai.social.model.enums.TwinStatus;
import com.footballai.social.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * RF5/RF23 — Central de Compliance LGPD (Lei 13.709/2018), privacy by design:
 * opt-in explícito (tudo FALSE), trilha de consentimento, exportação JSON (Art. 18, V), exclusão com graça de 30 dias
 * e anonimização (Art. 18, VI), step-up para dados sensíveis (Art. 11) e sem dark patterns.
 */
@Service
public class LgpdService {
    private static final Logger log = LoggerFactory.getLogger(LgpdService.class);

    private final UserRepository users;
    private final UserLgpdPreferencesRepository prefs;
    private final ConsentAuditLogRepository consents;
    private final TermsAcceptanceRepository terms;
    private final DigitalTwinRepository twins;
    private final WeeklyScoreSnapshotRepository snapshots;
    private final AthleteBadgeRepository badges;
    private final InjuryRecordRepository injuries;
    private final PostRepository posts;
    private final PostReactionRepository reactions;
    private final TechnicalEvaluationRepository evaluations;
    private final TrainingSummaryRepository training;
    private final DataExportRequestRepository exports;
    private final RedisSessionService sessions;
    private final StorageService storage;
    private final ObjectMapper mapper;
    private final AppProperties props;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public LgpdService(UserRepository users, UserLgpdPreferencesRepository prefs, ConsentAuditLogRepository consents, TermsAcceptanceRepository terms,
                       DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, AthleteBadgeRepository badges, InjuryRecordRepository injuries,
                       PostRepository posts, PostReactionRepository reactions, TechnicalEvaluationRepository evaluations, TrainingSummaryRepository training,
                       DataExportRequestRepository exports, RedisSessionService sessions, StorageService storage, ObjectMapper mapper, AppProperties props) {
        this(users, prefs, consents, terms, twins, snapshots, badges, injuries, posts, reactions, evaluations, training, exports, sessions, storage, mapper, props, Clock.systemUTC());
    }

    LgpdService(UserRepository users, UserLgpdPreferencesRepository prefs, ConsentAuditLogRepository consents, TermsAcceptanceRepository terms,
                DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, AthleteBadgeRepository badges, InjuryRecordRepository injuries,
                PostRepository posts, PostReactionRepository reactions, TechnicalEvaluationRepository evaluations, TrainingSummaryRepository training,
                DataExportRequestRepository exports, RedisSessionService sessions, StorageService storage, ObjectMapper mapper, AppProperties props, Clock clock) {
        this.users = users; this.prefs = prefs; this.consents = consents; this.terms = terms; this.twins = twins; this.snapshots = snapshots; this.badges = badges;
        this.injuries = injuries; this.posts = posts; this.reactions = reactions; this.evaluations = evaluations; this.training = training; this.exports = exports;
        this.sessions = sessions; this.storage = storage; this.mapper = mapper; this.props = props; this.clock = clock;
    }

    /* ---------------- privacidade ---------------- */

    public PrivacyCenterDTO center(AuthPrincipal p) {
        User u = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        UserLgpdPreferences pr = preferences(u.getId());
        List<PreferenceDTO> list = new ArrayList<>();
        for (LgpdPreference k : LgpdPreference.values())
            list.add(new PreferenceDTO(k.name(), pr.get(k), k.article().name(), k.article().label(), k.sensitive(), k.sensitive()));
        return new PrivacyCenterDTO(u.getId(), u.getStatus().name(), u.getVisibility().name(), list, u.getDeletionRequestedAt(), u.getScheduledPurgeAt(), sessions.listSessions(u.getId()));
    }

    UserLgpdPreferences preferences(UUID userId) { return prefs.findById(userId).orElseGet(() -> prefs.save(new UserLgpdPreferences(userId))); }

    /** Ativar preferência sensível (Art. 11) exige reautenticação (step-up). Revogar é sempre livre (Art. 8º, §5º). */
    @Transactional
    public PreferenceDTO updatePreference(AuthPrincipal p, LgpdPreference key, boolean enabled, String stepUpToken, String ip, String ua) {
        if (enabled && key.sensitive() && !sessions.verifyStepUpToken(p.userId(), stepUpToken))
            throw ApiException.forbidden("STEP_UP_REQUIRED", "Confirme sua senha para ativar o compartilhamento de dado sensível (Art. 11)");
        UserLgpdPreferences pr = preferences(p.userId());
        boolean old = pr.get(key);
        if (old != enabled) {
            Instant now = clock.instant();
            pr.set(key, enabled, now); prefs.save(pr);
            consents.save(new ConsentAuditLog(p.userId(), key, old, enabled, ip, ua, now));
        }
        return new PreferenceDTO(key.name(), enabled, key.article().name(), key.article().label(), key.sensitive(), key.sensitive());
    }

    public List<ConsentEntryDTO> consentHistory(AuthPrincipal p) {
        return consents.findAllByUserIdOrderByChangedAtDesc(p.userId()).stream()
                .map(c -> new ConsentEntryDTO(c.getPreference().name(), c.isOldValue(), c.isNewValue(), c.getLgpdArticle().name(), c.getLgpdArticle().label(), c.getChangedAt())).toList();
    }

    /* ---------------- exportação (Art. 18, V) ---------------- */

    @Transactional
    public ExportDTO requestExport(AuthPrincipal p) {
        User u = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        DataExportRequest req = exports.save(new DataExportRequest(u.getId(), clock.instant()));
        req.setStatus(ExportStatus.PROCESSING);
        try {
            byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exportPayload(u));
            String key = "exports/" + u.getId() + "/" + req.getId() + ".json";
            storage.putExport(key, json);
            req.markReady(key, json.length, clock.instant(), props.lgpd().exportTtlDays());
        } catch (Exception e) {
            log.error("Exportação LGPD falhou para {}: {}", u.getId(), e.getMessage());
            req.markFailed(e.getClass().getSimpleName());
        }
        return toDto(exports.save(req));
    }

    public List<ExportDTO> listExports(AuthPrincipal p) { return exports.findAllByUserIdOrderByRequestedAtDesc(p.userId()).stream().map(this::toDto).toList(); }

    private ExportDTO toDto(DataExportRequest r) {
        String url = null;
        if (r.getStatus() == ExportStatus.READY && !r.isExpired(clock.instant())) {
            try { url = storage.presignExport(r.getS3ObjectKey(), Duration.between(clock.instant(), r.getExpiresAt())); } catch (RuntimeException e) { log.warn("presign falhou: {}", e.getMessage()); }
        }
        return new ExportDTO(r.getId(), r.getStatus().name(), r.getRequestedAt(), r.getReadyAt(), r.getExpiresAt(), url, r.getSizeBytes(), r.getFailureReason());
    }

    /** Payload completo e legível por máquina — chaves de topo conforme docs/ENTITY_TAXONOMY.md §7.4. */
    Map<String, Object> exportPayload(User u) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", u.getId()); profile.put("email", u.getEmail()); profile.put("username", u.getUsername()); profile.put("displayName", u.getDisplayName());
        profile.put("role", u.getRole()); profile.put("birthDate", u.getBirthDate()); profile.put("bio", u.getBio()); profile.put("city", u.getCity());
        profile.put("stateCode", u.getStateCode()); profile.put("countryCode", u.getCountryCode()); profile.put("visibility", u.getVisibility());
        profile.put("status", u.getStatus()); profile.put("createdAt", u.getCreatedAt());
        out.put("profile", profile);
        out.put("lgpdPreferences", preferences(u.getId()).asMap());
        out.put("consentHistory", consents.findAllByUserIdOrderByChangedAtDesc(u.getId()).stream().map(c -> Map.of("preference", c.getPreference(), "from", c.isOldValue(), "to", c.isNewValue(), "article", c.getLgpdArticle().label(), "at", c.getChangedAt())).toList());
        out.put("termsAcceptances", terms.findAllByUserIdOrderByAcceptedAtDesc(u.getId()).stream().map(t -> Map.of("version", t.getTermsVersion(), "acceptedAt", t.getAcceptedAt(), "ip", t.getIpAddress() == null ? "" : t.getIpAddress())).toList());
        out.put("sessions", sessions.listSessions(u.getId()));
        Optional<DigitalTwin> twin = twins.findByUserId(u.getId());
        out.put("digitalTwin", twin.map(t -> Map.of("status", t.getStatus(), "physicalStatus", t.getPhysicalStatus(), "scoreTotal", t.getScoreTotal(), "rarityTier", t.getRarityTier(), "lastCalculatedWeek", String.valueOf(t.getLastCalculatedWeek()))).orElse(null));
        out.put("weeklyScores", twin.map(t -> snapshots.findAllByTwinIdOrderByIsoWeekAsc(t.getId()).stream().map(s -> Map.of("isoWeek", s.getIsoWeek(), "physical", s.getPhysicalScore(), "technical", s.getTechnicalScore(), "match", s.getMatchScore(), "bonus", s.getBadgeBonus(), "scoreTotal", s.getScoreTotal(), "tier", s.getRarityTier())).toList()).orElse(List.of()));
        out.put("badges", badges.findAllByAthleteId(u.getId()).stream().map(b -> Map.of("type", b.getBadgeType(), "counter", b.getCounter(), "season", b.getSeason(), "awardedAt", b.getAwardedAt(), "active", b.isActive())).toList());
        out.put("injuries", injuries.findAllByAthleteId(u.getId()).stream().map(i -> Map.of("type", i.getInjuryType(), "bodyPart", String.valueOf(i.getBodyPart()), "severity", i.getSeverity(), "startedAt", i.getStartedAt(), "expectedReturn", String.valueOf(i.getExpectedReturnDate()), "clearedAt", String.valueOf(i.getClearedAt()))).toList());
        out.put("telemetry", training.findAllByAthleteIdOrderByIsoWeekDesc(u.getId()).stream().map(t -> Map.of("isoWeek", t.getIsoWeek(), "sessionsPlanned", t.getSessionsPlanned(), "sessionsDone", t.getSessionsDone(), "loadAu", t.getLoadAu(), "acwr", t.acwr(), "readiness", t.getReadiness())).toList());
        out.put("posts", posts.findAllByAuthorId(u.getId()).stream().map(p -> Map.of("id", p.getId(), "contentType", p.getContentType(), "window", p.getWindow(), "title", p.getTitle(), "subtitle", String.valueOf(p.getSubtitle()), "content", String.valueOf(p.getContent()), "publishedAt", p.getPublishedAt(), "likes", p.getLikeCount())).toList());
        out.put("evaluationsReceived", evaluations.findAllByAthleteId(u.getId()).stream().map(e -> Map.of("dimension", e.getDimension(), "rating", e.getRating(), "weight", e.getWeight(), "weekOf", e.getWeekOf(), "evaluatorRole", e.getEvaluatorRole())).toList());
        out.put("generatedAt", clock.instant());
        out.put("format", "application/json");
        out.put("legalBasis", "LGPD Art. 18, V — portabilidade");
        return out;
    }

    /* ---------------- exclusão (Art. 18, VI) ---------------- */

    @Transactional
    public DeletionDTO requestDeletion(AuthPrincipal p, String stepUpToken) {
        if (!sessions.verifyStepUpToken(p.userId(), stepUpToken)) throw ApiException.forbidden("STEP_UP_REQUIRED", "Confirme sua senha para solicitar a exclusão");
        User u = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        if (u.getStatus() == AccountStatus.PENDING_DELETE) return deletionDto(u, "Exclusão já agendada");
        int grace = props.lgpd().deletionGraceDays();
        u.requestDeletion(clock.instant(), grace);
        users.save(u);
        twins.findByUserId(u.getId()).ifPresent(t -> { if (t.getStatus().canTransitionTo(TwinStatus.PENDING_DELETE)) { t.transitionTo(TwinStatus.PENDING_DELETE, clock.instant()); twins.save(t); } });
        sessions.revokeAllSessions(u.getId());
        return deletionDto(u, "Conta desativada. Você tem " + grace + " dias para cancelar fazendo login; depois os dados serão anonimizados (Art. 18, VI)");
    }

    @Transactional
    public DeletionDTO cancelDeletion(AuthPrincipal p) {
        User u = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        if (u.getStatus() != AccountStatus.PENDING_DELETE) throw ApiException.conflict("NO_PENDING_DELETION", "Não há exclusão pendente");
        u.cancelDeletion(); users.save(u);
        twins.findByUserId(u.getId()).ifPresent(t -> { if (t.getStatus() == TwinStatus.PENDING_DELETE) { t.transitionTo(TwinStatus.ACTIVE, clock.instant()); twins.save(t); } });
        return deletionDto(u, "Exclusão cancelada — conta reativada");
    }

    private DeletionDTO deletionDto(User u, String msg) { return new DeletionDTO(u.getStatus().name(), u.getDeletionRequestedAt(), u.getScheduledPurgeAt(), props.lgpd().deletionGraceDays(), msg); }

    /** Job diário: anonimiza contas cuja graça de 30 dias venceu. Nada é apagado fisicamente antes disso. */
    @Scheduled(cron = "${app.lgpd.anonymization-cron:0 30 3 * * *}")
    public void anonymizationJob() {
        Instant now = clock.instant();
        int n = 0;
        for (User u : users.findAllByStatusAndScheduledPurgeAtBefore(AccountStatus.PENDING_DELETE, now)) {
            try { anonymize(u.getId()); n++; } catch (RuntimeException e) { log.error("Anonimização falhou para {}: {}", u.getId(), e.getMessage()); }
        }
        if (n > 0) log.info("LGPD: {} contas anonimizadas", n);
        expireExports(now);
    }

    @Transactional
    public void anonymize(UUID userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("Usuário"));
        if (!u.isPurgeDue(clock.instant())) throw ApiException.conflict("PURGE_NOT_DUE", "Graça de 30 dias ainda em curso");
        Instant now = clock.instant();
        for (Post p : posts.findAllByAuthorId(userId)) { p.anonymizeContent(); posts.save(p); }
        reactions.deleteAllByUserId(userId);
        injuries.deleteAllByAthleteId(userId);              // dado de saúde: remoção física
        training.deleteAll(training.findAllByAthleteIdOrderByIsoWeekDesc(userId));
        preferences(userId).revokeAll(now);
        twins.findByUserId(userId).ifPresent(t -> { if (t.getStatus().canTransitionTo(TwinStatus.ANONYMIZED)) t.transitionTo(TwinStatus.ANONYMIZED, now); twins.save(t); });
        for (DataExportRequest r : exports.findAllByUserIdOrderByRequestedAtDesc(userId)) { safeDelete(r); }
        if (u.getAvatarKey() != null) { try { storage.deleteMedia(u.getAvatarKey()); } catch (RuntimeException ignored) { } }
        sessions.revokeAllSessions(userId);
        u.anonymize(now); users.save(u);
        // ConsentAuditLog e TermsAcceptance permanecem (accountability, Art. 6º, X) ligados ao id pseudonimizado.
    }

    void expireExports(Instant now) {
        for (DataExportRequest r : exports.findAllByStatusAndExpiresAtBefore(ExportStatus.READY, now)) safeDelete(r);
    }

    private void safeDelete(DataExportRequest r) {
        try { if (r.getS3ObjectKey() != null) storage.deleteExport(r.getS3ObjectKey()); } catch (RuntimeException e) { log.warn("delete export falhou: {}", e.getMessage()); }
        r.markExpired(); exports.save(r);
    }

    /* ---------------- step-up ---------------- */

    public StepUpResponse stepUp(AuthPrincipal p, boolean passwordOk) {
        if (!passwordOk) throw ApiException.unauthorized("BAD_CREDENTIALS", "Senha incorreta");
        return new StepUpResponse(sessions.issueStepUpToken(p.userId()), 300);
    }
}
