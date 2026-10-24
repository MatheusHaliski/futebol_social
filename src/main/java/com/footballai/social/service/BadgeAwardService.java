package com.footballai.social.service;

import com.footballai.social.controller.dto.CardDtos.BadgeDTO;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.BadgeType;
import com.footballai.social.model.enums.PhysicalStatus;
import com.footballai.social.model.enums.Position;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * RF4.1 — Athletic Persona. Concessão determinística e auditável de badges a partir de estatísticas e telemetria;
 * derivação exclusiva do PhysicalStatus; bloqueio de convocação quando INJURED; projeção dos 3 topBadges.
 */
@Service
public class BadgeAwardService {

    public record SeasonStats(int games, int goals, int assists, int minutes, int yellow, int red, int cleanSheets, int tackles, int mvp,
                              int bestStreak, BigDecimal avgRating) { }

    private final AthleteBadgeRepository badges;
    private final BadgeAuditLogRepository audit;
    private final MatchPerformanceRepository matches;
    private final TrainingSummaryRepository training;
    private final InjuryRecordRepository injuries;
    private final DigitalTwinRepository twins;
    private final AthleteRepository athletes;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public BadgeAwardService(AthleteBadgeRepository badges, BadgeAuditLogRepository audit, MatchPerformanceRepository matches,
                             TrainingSummaryRepository training, InjuryRecordRepository injuries, DigitalTwinRepository twins, AthleteRepository athletes) {
        this(badges, audit, matches, training, injuries, twins, athletes, Clock.systemUTC());
    }

    BadgeAwardService(AthleteBadgeRepository badges, BadgeAuditLogRepository audit, MatchPerformanceRepository matches, TrainingSummaryRepository training,
                      InjuryRecordRepository injuries, DigitalTwinRepository twins, AthleteRepository athletes, Clock clock) {
        this.badges = badges; this.audit = audit; this.matches = matches; this.training = training; this.injuries = injuries;
        this.twins = twins; this.athletes = athletes; this.clock = clock;
    }

    /* ---------------- regras puras ---------------- */

    /** Estados mutuamente exclusivos: lesão ativa > ACWR > 1.5 ou prontidão < 60 > pronto. */
    public static PhysicalStatus derivePhysicalStatus(boolean activeInjury, BigDecimal acwr, int readiness) {
        if (activeInjury) return PhysicalStatus.INJURED;
        if ((acwr != null && acwr.compareTo(new BigDecimal("1.5")) > 0) || readiness < 60) return PhysicalStatus.FATIGUE_RISK;
        return PhysicalStatus.READY;
    }

    public static SeasonStats aggregate(List<MatchPerformance> ms) {
        int games = ms.size(), goals = 0, assists = 0, minutes = 0, yellow = 0, red = 0, cs = 0, tackles = 0, mvp = 0, streak = 0, best = 0;
        BigDecimal sum = BigDecimal.ZERO; int rated = 0;
        for (MatchPerformance m : ms) {
            goals += m.getGoals(); assists += m.getAssists(); minutes += m.getMinutesPlayed(); yellow += m.getYellowCards(); red += m.getRedCards();
            if (m.isCleanSheet()) cs++; tackles += m.getTackles(); if (m.isMvp()) mvp++;
            if (m.getRating() != null) {
                sum = sum.add(m.getRating()); rated++;
                streak = m.getRating().compareTo(new BigDecimal("7.5")) >= 0 ? streak + 1 : 0; best = Math.max(best, streak);
            }
        }
        BigDecimal avg = rated == 0 ? null : sum.divide(BigDecimal.valueOf(rated), 2, java.math.RoundingMode.HALF_EVEN);
        return new SeasonStats(games, goals, assists, minutes, yellow, red, cs, tackles, mvp, best, avg);
    }

    /** Badges da temporada que o atleta faz jus (determinístico): tipo → contador. */
    public static Map<BadgeType, Integer> seasonBadges(SeasonStats s, Position pos, boolean captain) {
        Map<BadgeType, Integer> out = new EnumMap<>(BadgeType.class);
        if (s.goals() > 0) out.put(BadgeType.ARTILHEIRO, s.goals());
        if (s.assists() > 0) out.put(BadgeType.GARCOM, s.assists());
        if (pos == Position.GOLEIRO && s.cleanSheets() > 0) out.put(BadgeType.PAREDAO, s.cleanSheets());
        if ((pos == Position.ZAGUEIRO || pos == Position.LATERAL || pos == Position.VOLANTE) && s.tackles() >= 20) out.put(BadgeType.XERIFAO, s.tackles());
        if (s.minutes() >= 900) out.put(BadgeType.MOTORZINHO, s.minutes());
        if (s.mvp() > 0) out.put(BadgeType.MVP, s.mvp());
        if (s.bestStreak() >= 3) out.put(BadgeType.EM_CHAMAS, s.bestStreak());
        if (s.games() >= 10 && s.yellow() <= 1 && s.red() == 0) out.put(BadgeType.FAIR_PLAY, 0);
        if (captain) out.put(BadgeType.LIDER_CAPITAO, 0);
        return out;
    }

    /** Bônus do Rarity (cap 300): MVP 25/un · Em chamas 10/un · Mestre tático 30 · Líder 20 · Fair play 15. */
    public BigDecimal badgeBonus(List<AthleteBadge> active) {
        int b = 0;
        for (AthleteBadge x : active) {
            switch (x.getBadgeType()) {
                case MVP -> b += 25 * Math.max(1, x.getCounter());
                case EM_CHAMAS -> b += 10 * Math.max(1, x.getCounter());
                case MESTRE_TATICO -> b += 30;
                case LIDER_CAPITAO -> b += 20;
                case FAIR_PLAY -> b += 15;
                default -> { }
            }
        }
        return BigDecimal.valueOf(Math.min(300, b));
    }

    /** Top 3: status médico ativo primeiro, depois priority (asc) e counter (desc). Badges de saúde só com permissão. */
    public static List<AthleteBadge> top3(List<AthleteBadge> active, boolean canSeeMedical) {
        return active.stream()
                .filter(AthleteBadge::isActive)
                .filter(b -> canSeeMedical || !b.getBadgeType().isPhysical())
                .sorted(Comparator.<AthleteBadge>comparingInt(b -> b.getBadgeType() == BadgeType.DEPARTAMENTO_MEDICO ? 0 : 1)
                        .thenComparingInt(b -> b.getBadgeType().priority())
                        .thenComparing(Comparator.comparingInt(AthleteBadge::getCounter).reversed()))
                .limit(3).toList();
    }

    public static BadgeDTO toDto(AthleteBadge b) {
        BadgeType t = b.getBadgeType();
        String label = t.quantitative() && b.getCounter() > 0 ? t.emoji() + " " + b.getCounter() : t.emoji() + " " + shortLabel(t);
        return new BadgeDTO(t.name(), label, t.label(), t.emoji(), t.color(), t.category().name(), b.getCounter(), b.getSeason(), b.getAwardedAt());
    }

    private static String shortLabel(BadgeType t) {
        return switch (t) {
            case PRONTO_PRA_JOGO -> "PRONTO"; case RISCO_FADIGA -> "FADIGA"; case DEPARTAMENTO_MEDICO -> "DM";
            case LIDER_CAPITAO -> "CAP"; case FAIR_PLAY -> "FAIR"; case MESTRE_TATICO -> "TÁTICO"; default -> t.label().toUpperCase();
        };
    }

    /* ---------------- persistência ---------------- */

    /** Recalcula status físico e badges do atleta após o score semanal (chamado pelo Rarity Engine). */
    @Transactional
    public void onWeeklyScore(UUID athleteId, String isoWeek) {
        Instant now = clock.instant();
        String season = IsoWeeks.season(IsoWeeks.range(isoWeek)[0]);
        Athlete athlete = athletes.findById(athleteId).orElse(null);
        Position pos = athlete == null ? null : athlete.getPosition();
        boolean captain = athlete != null && athlete.isCaptain();
        SeasonStats stats = aggregate(matches.findAllByAthleteIdAndSeasonOrderByMatchDateAsc(athleteId, season));
        syncSeasonBadges(athleteId, season, seasonBadges(stats, pos, captain), now);
        TrainingSummary ts = training.findByAthleteIdAndIsoWeek(athleteId, isoWeek).orElse(null);
        boolean injured = !injuries.findAllByAthleteIdAndClearedAtIsNull(athleteId).isEmpty();
        PhysicalStatus status = derivePhysicalStatus(injured, ts == null ? null : ts.acwr(), ts == null ? 100 : ts.getReadiness());
        syncPhysicalStatus(athleteId, season, status, now);
    }

    @Transactional
    public void syncSeasonBadges(UUID athleteId, String season, Map<BadgeType, Integer> due, Instant now) {
        for (var e : due.entrySet()) {
            var existing = badges.findByAthleteIdAndBadgeTypeAndSeason(athleteId, e.getKey(), season);
            if (existing.isEmpty()) {
                badges.save(new AthleteBadge(athleteId, e.getKey(), e.getValue(), season, now, null));
                audit.save(new BadgeAuditLog(athleteId, e.getKey(), "AWARD", "temporada " + season, now));
            } else if (existing.get().getCounter() != e.getValue() || !existing.get().isActive()) {
                existing.get().setCounter(e.getValue()); existing.get().setActive(true); badges.save(existing.get());
                audit.save(new BadgeAuditLog(athleteId, e.getKey(), "UPDATE_COUNTER", "counter=" + e.getValue(), now));
            }
        }
    }

    @Transactional
    public void syncPhysicalStatus(UUID athleteId, String season, PhysicalStatus status, Instant now) {
        twins.findByUserId(athleteId).ifPresent(t -> { t.setPhysicalStatus(status); twins.save(t); });
        for (AthleteBadge b : badges.findAllByAthleteIdAndActiveTrue(athleteId)) {
            if (b.getBadgeType().isPhysical() && b.getBadgeType() != status.badge()) {
                b.setActive(false); badges.save(b);
                audit.save(new BadgeAuditLog(athleteId, b.getBadgeType(), "DEACTIVATE", "status=" + status, now));
            }
        }
        var current = badges.findByAthleteIdAndBadgeTypeAndSeason(athleteId, status.badge(), season);
        if (current.isEmpty()) {
            badges.save(new AthleteBadge(athleteId, status.badge(), 0, season, now, null));
            audit.save(new BadgeAuditLog(athleteId, status.badge(), "AWARD", "status=" + status, now));
        } else if (!current.get().isActive()) { current.get().setActive(true); badges.save(current.get()); }
    }

    /** Convocação automática bloqueada para atletas INJURED. */
    public void assertCallUpAllowed(UUID athleteId) {
        PhysicalStatus st = twins.findByUserId(athleteId).map(DigitalTwin::getPhysicalStatus).orElse(PhysicalStatus.READY);
        if (st.blocksCallUp()) throw ApiException.conflict("CALL_UP_BLOCKED", "Atleta no departamento médico — convocação automática bloqueada");
    }

    public List<BadgeDTO> topBadges(UUID athleteId, boolean canSeeMedical) {
        return top3(badges.findAllByAthleteIdAndActiveTrue(athleteId), canSeeMedical).stream().map(BadgeAwardService::toDto).toList();
    }

    /** Tap to expand: histórico do badge (série temporal) para o bottom sheet. */
    public List<BadgeDTO> history(UUID athleteId, BadgeType type, boolean canSeeMedical) {
        if (type.isPhysical() && !canSeeMedical) throw ApiException.forbidden("MEDICAL_DATA_PRIVATE", "Dados de saúde não compartilhados (Art. 11)");
        return badges.findAllByAthleteIdAndBadgeTypeOrderByAwardedAtDesc(athleteId, type).stream().map(BadgeAwardService::toDto).toList();
    }

    public LocalDate today() { return LocalDate.now(clock); }
}
