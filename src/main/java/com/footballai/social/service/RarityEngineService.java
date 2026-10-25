package com.footballai.social.service;

import com.footballai.social.config.AppProperties;
import com.footballai.social.model.entity.DigitalTwin;
import com.footballai.social.model.entity.WeeklyScoreSnapshot;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.model.enums.TwinStatus;
import com.footballai.social.repository.DigitalTwinRepository;
import com.footballai.social.repository.WeeklyScoreSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import com.footballai.social.model.enums.NotificationType;

/**
 * CORE — Rarity Engine. Cálculo determinístico e semanal do Score_Total (0–3000+):
 * 30 % telemetria física + assiduidade · 40 % avaliações técnicas · 30 % partidas, + bônus de badges.
 * Sem Random, sem relógio dentro do cálculo; BigDecimal HALF_EVEN; idempotente por (twin, isoWeek, inputsHash).
 */
@Service
public class RarityEngineService {
    private static final Logger log = LoggerFactory.getLogger(RarityEngineService.class);
    static final BigDecimal W_PHYSICAL = new BigDecimal("0.30"), W_TECHNICAL = new BigDecimal("0.40"), W_MATCH = new BigDecimal("0.30");
    static final BigDecimal SCALE = new BigDecimal("3000"), MAX_BONUS = new BigDecimal("300");
    private static final int P = 4; // casas decimais internas

    public record RarityInputs(String isoWeek, int sessionsPlanned, int sessionsDone, BigDecimal acwr, int readiness,
                               BigDecimal weightedTechnicalRating, BigDecimal technicalWeightSum,
                               int matches, int minutesPlayed, BigDecimal avgMatchRating, int goalContributions,
                               BigDecimal badgeBonus) {
        public String canonical() {
            return String.join("|", isoWeek, s(sessionsPlanned), s(sessionsDone), s(acwr), s(readiness), s(weightedTechnicalRating),
                    s(technicalWeightSum), s(matches), s(minutesPlayed), s(avgMatchRating), s(goalContributions), s(badgeBonus));
        }
        private static String s(int i) { return Integer.toString(i); }
        private static String s(BigDecimal b) { return b == null ? "0" : b.stripTrailingZeros().toPlainString(); }
    }

    public record RarityResult(BigDecimal physicalScore, BigDecimal technicalScore, BigDecimal matchScore, BigDecimal badgeBonus,
                               int scoreTotal, RarityTier tier, String inputsHash) { }

    private final DigitalTwinRepository twins;
    private final WeeklyScoreSnapshotRepository snapshots;
    private final RarityInputProvider inputs;
    private final BadgeAwardService badges;
    private final NotificationService notifications;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RarityEngineService(DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, RarityInputProvider inputs,
                               BadgeAwardService badges, NotificationService notifications, AppProperties props) {
        this(twins, snapshots, inputs, badges, notifications, Clock.systemUTC());
    }

    RarityEngineService(DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, RarityInputProvider inputs, BadgeAwardService badges, NotificationService notifications, Clock clock) {
        this.twins = twins; this.snapshots = snapshots; this.inputs = inputs; this.badges = badges; this.notifications = notifications; this.clock = clock;
    }

    /* ---------------- cálculo puro ---------------- */

    public RarityResult calculate(RarityInputs in) {
        Objects.requireNonNull(in, "inputs");
        BigDecimal physical = physicalScore(in);
        BigDecimal technical = technicalScore(in);
        BigDecimal match = matchScore(in);
        BigDecimal bonus = clamp(in.badgeBonus() == null ? BigDecimal.ZERO : in.badgeBonus(), BigDecimal.ZERO, MAX_BONUS).setScale(P, RoundingMode.HALF_EVEN);
        BigDecimal weighted = W_PHYSICAL.multiply(physical).add(W_TECHNICAL.multiply(technical)).add(W_MATCH.multiply(match));
        int total = weighted.multiply(SCALE).add(bonus).setScale(0, RoundingMode.HALF_EVEN).intValueExact();
        return new RarityResult(physical, technical, match, bonus, total, RarityTier.fromScore(total), sha256(in.canonical()));
    }

    /** 0–1: 60 % assiduidade (feitos/planejados) + 40 % qualidade de carga (prontidão × penalidade de ACWR). */
    BigDecimal physicalScore(RarityInputs in) {
        BigDecimal attendance = in.sessionsPlanned() <= 0 ? BigDecimal.ZERO
                : clamp(bd(in.sessionsDone()).divide(bd(in.sessionsPlanned()), P, RoundingMode.HALF_EVEN), BigDecimal.ZERO, BigDecimal.ONE);
        BigDecimal readiness = clamp(bd(in.readiness()).divide(bd(100), P, RoundingMode.HALF_EVEN), BigDecimal.ZERO, BigDecimal.ONE);
        BigDecimal acwr = in.acwr() == null ? BigDecimal.ONE : in.acwr();
        BigDecimal penalty = acwr.compareTo(new BigDecimal("0.8")) >= 0 && acwr.compareTo(new BigDecimal("1.3")) <= 0 ? BigDecimal.ONE
                : acwr.compareTo(new BigDecimal("1.5")) <= 0 && acwr.compareTo(new BigDecimal("0.5")) >= 0 ? new BigDecimal("0.8") : new BigDecimal("0.5");
        return new BigDecimal("0.6").multiply(attendance).add(new BigDecimal("0.4").multiply(readiness).multiply(penalty)).setScale(P, RoundingMode.HALF_EVEN);
    }

    /** 0–1: média ponderada das avaliações que contam (comissão 1.0 · scout homologado 0.8 · comunidade 0.5), dividida por 10. */
    BigDecimal technicalScore(RarityInputs in) {
        if (in.technicalWeightSum() == null || in.technicalWeightSum().signum() == 0) return BigDecimal.ZERO.setScale(P);
        BigDecimal avg = in.weightedTechnicalRating().divide(in.technicalWeightSum(), P, RoundingMode.HALF_EVEN);
        return clamp(avg.divide(BigDecimal.TEN, P, RoundingMode.HALF_EVEN), BigDecimal.ZERO, BigDecimal.ONE);
    }

    /** 0–1: 50 % minutagem (min / jogos×90) + 40 % nota média/10 + 10 % participações em gol (cap 10 na semana). */
    BigDecimal matchScore(RarityInputs in) {
        if (in.matches() <= 0) return BigDecimal.ZERO.setScale(P);
        BigDecimal minutes = clamp(bd(in.minutesPlayed()).divide(bd(in.matches() * 90), P, RoundingMode.HALF_EVEN), BigDecimal.ZERO, BigDecimal.ONE);
        BigDecimal rating = clamp((in.avgMatchRating() == null ? BigDecimal.ZERO : in.avgMatchRating()).divide(BigDecimal.TEN, P, RoundingMode.HALF_EVEN), BigDecimal.ZERO, BigDecimal.ONE);
        BigDecimal contrib = clamp(bd(in.goalContributions()).divide(BigDecimal.TEN, P, RoundingMode.HALF_EVEN), BigDecimal.ZERO, BigDecimal.ONE);
        return new BigDecimal("0.5").multiply(minutes).add(new BigDecimal("0.4").multiply(rating)).add(new BigDecimal("0.1").multiply(contrib)).setScale(P, RoundingMode.HALF_EVEN);
    }

    /* ---------------- job semanal ---------------- */

    /** Segunda-feira 03:00 (cron em app.rarity.cron): recalcula a semana ISO anterior para todos os Gêmeos ACTIVE. */
    @Scheduled(cron = "${app.rarity.cron:0 0 3 * * MON}")
    public void weeklyRecalculation() {
        String week = IsoWeeks.previous(LocalDate.now(clock));
        int n = 0;
        for (DigitalTwin twin : twins.findAllByStatus(TwinStatus.ACTIVE)) {
            try { if (recalculate(twin.getId(), week)) n++; }
            catch (RuntimeException e) { log.error("Rarity: falha no twin {} semana {}: {}", twin.getId(), week, e.getMessage()); }
        }
        log.info("Rarity Engine: semana {} recalculada para {} gêmeos", week, n);
    }

    /** Idempotente: mesma entrada ⇒ mesmo snapshot; retorna true quando algo foi gravado. */
    @Transactional
    public boolean recalculate(java.util.UUID twinId, String isoWeek) {
        DigitalTwin twin = twins.findById(twinId).orElseThrow(() -> ApiException.notFound("Gêmeo"));
        if (!twin.getStatus().countsForRanking()) return false;
        RarityInputs in = inputs.inputsFor(twin.getUserId(), isoWeek);
        RarityResult r = calculate(in);
        var existing = snapshots.findByTwinIdAndIsoWeek(twinId, isoWeek);
        if (existing.isPresent() && existing.get().getInputsHash().equals(r.inputsHash())) return false;
        if (existing.isPresent()) {
            existing.get().update(r.physicalScore(), r.technicalScore(), r.matchScore(), r.badgeBonus(), r.scoreTotal(), r.tier(), r.inputsHash(), clock.instant());
            snapshots.save(existing.get());
        } else {
            snapshots.save(new WeeklyScoreSnapshot(twinId, isoWeek, r.physicalScore(), r.technicalScore(), r.matchScore(), r.badgeBonus(),
                    r.scoreTotal(), r.tier(), r.inputsHash(), clock.instant()));
        }
        RarityTier before = twin.getRarityTier();
        twin.applyWeeklyScore(r.scoreTotal(), isoWeek);
        twins.save(twin);
        if (before != twin.getRarityTier())   // o atleta sabe que mudou de tier (sem expor nenhum dado de saúde)
            notifications.notify(twin.getUserId(), NotificationType.TIER_CHANGE, null, "TWIN", twin.getId(),
                    "Seu Gêmeo Digital agora é " + twin.getRarityTier().name(), "Score_Total " + r.scoreTotal() + " · semana " + isoWeek + (twin.getRarityTier().ordinal() > before.ordinal() ? " · subiu de " : " · desceu de ") + before.name());
        badges.onWeeklyScore(twin.getUserId(), isoWeek);
        return true;
    }

    /* ---------------- util ---------------- */

    private static BigDecimal bd(int i) { return BigDecimal.valueOf(i); }
    private static BigDecimal clamp(BigDecimal v, BigDecimal lo, BigDecimal hi) { return v.max(lo).min(hi); }

    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(); for (byte b : d) sb.append(String.format("%02x", b)); return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
