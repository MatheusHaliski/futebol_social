package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.VitrineDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.ContractStatus;
import com.footballai.social.model.enums.Position;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.model.enums.TwinStatus;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aba <b>Vitrine do Jogador</b>: painel dinâmico com o histórico do atleta (partidas, gols, assistências, passes e dribles certos),
 * os atributos do momento (velocidade, chute, passe…) e o Copilot que responde "Por que contratar este jogador?".
 * <ul>
 *   <li><b>Acesso</b>: a mesma regra do perfil e do Gêmeo ({@link AccessPolicy#requireProfile}) — senão 404. O card embutido segue
 *       {@link CardAssembler} (condicionamento só para quem pode ver saúde).</li>
 *   <li><b>Atributos do momento</b>: valor do scouting da semana mais recente + ajuste de forma (±{@value #MAX_FORM_ADJUSTMENT}) das últimas
 *       {@value #RECENT} partidas contra as anteriores, só onde a súmula traz evidência (passe, drible, chute, desarme). Cada número vem com o motivo.</li>
 *   <li><b>Copilot</b>: regras determinísticas (sem LLM externo) sobre dados abertos; nunca lê saúde (treino, prontidão, lesões — Art. 11);
 *       exige que o <i>atleta</i> tenha ligado "Processamento por agentes de IA" (Art. 7º, I · Art. 20) e não analisa menores de 18 (Art. 14).</li>
 * </ul>
 */
@Service
public class VitrineService {
    static final int RECENT = 5;
    static final int MIN_FORM_SAMPLE = 3;
    static final int MAX_FORM_ADJUSTMENT = 5;
    static final int HISTORY_MATCHES = 10;
    static final int MAX_REASONS = 6;
    static final int REFRESH_SECONDS = 30;
    static final String QUESTION = "Por que contratar este jogador?";
    static final BigDecimal FORM_THRESHOLD = new BigDecimal("0.30");
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    /** Atributo do scouting exibido na Vitrine (ordem da tela). A finalização aparece como "Chute". */
    record AttrSpec(String key, String label, Function<ScoutingAttribute, BigDecimal> get) { }
    static final List<AttrSpec> ATTRS = List.of(
            new AttrSpec("velocidade", "Velocidade", ScoutingAttribute::getVelocidade),
            new AttrSpec("finalizacao", "Chute", ScoutingAttribute::getFinalizacao),
            new AttrSpec("passe", "Passe", ScoutingAttribute::getPasse),
            new AttrSpec("drible", "Drible", ScoutingAttribute::getDrible),
            new AttrSpec("desarme", "Desarme", ScoutingAttribute::getDesarme),
            new AttrSpec("posicionamento", "Posicionamento", ScoutingAttribute::getPosicionamento),
            new AttrSpec("resistencia", "Resistência", ScoutingAttribute::getResistencia));

    private final MatchPerformanceRepository matches;
    private final ScoutingAttributeRepository scouting;
    private final TechnicalEvaluationRepository evaluations;
    private final DigitalTwinRepository twins;
    private final WeeklyScoreSnapshotRepository snapshots;
    private final UserLgpdPreferencesRepository prefs;
    private final CardAssembler assembler;
    private final AccessPolicy access;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public VitrineService(MatchPerformanceRepository matches, ScoutingAttributeRepository scouting, TechnicalEvaluationRepository evaluations, DigitalTwinRepository twins,
                          WeeklyScoreSnapshotRepository snapshots, UserLgpdPreferencesRepository prefs, CardAssembler assembler, AccessPolicy access) {
        this(matches, scouting, evaluations, twins, snapshots, prefs, assembler, access, Clock.systemUTC());
    }

    VitrineService(MatchPerformanceRepository matches, ScoutingAttributeRepository scouting, TechnicalEvaluationRepository evaluations, DigitalTwinRepository twins,
                   WeeklyScoreSnapshotRepository snapshots, UserLgpdPreferencesRepository prefs, CardAssembler assembler, AccessPolicy access, Clock clock) {
        this.matches = matches; this.scouting = scouting; this.evaluations = evaluations; this.twins = twins; this.snapshots = snapshots;
        this.prefs = prefs; this.assembler = assembler; this.access = access; this.clock = clock;
    }

    /* ======================= painel ======================= */

    public VitrineDTO vitrine(AuthPrincipal viewer, UUID athleteId) {
        Athlete a = requireAthlete(viewer, athleteId);
        Snapshot s = snapshot(a);
        return new VitrineDTO(a.getId(), assembler.athleteCard(a, viewer), s.career(), s.season(), s.seasonLabel(), s.recent(), s.seasons(), s.recentMatches(),
                s.form(), s.attributes(), s.attributesWeek(), copilotStatus(viewer, a), s.revision(), clock.instant(), REFRESH_SECONDS);
    }

    /** Tudo o que o painel e o Copilot leem — nada de saúde (treinos, prontidão, lesões ficam de fora). */
    record Snapshot(TotalsDTO career, TotalsDTO season, String seasonLabel, TotalsDTO recent, TotalsDTO prior,
                    List<SeasonLineDTO> seasons, List<MatchLineDTO> recentMatches, FormDTO form, List<LiveAttributeDTO> attributes,
                    String attributesWeek, DigitalTwin twin, String revision) { }

    Snapshot snapshot(Athlete a) {
        String seasonLabel = IsoWeeks.season(LocalDate.now(clock));
        List<MatchPerformance> all = matches.findAllByAthleteIdOrderByMatchDateAsc(a.getId());
        int split = Math.max(0, all.size() - RECENT);
        TotalsDTO prior = totals(all.subList(0, split)), recent = totals(all.subList(split, all.size())), career = totals(all);
        TotalsDTO season = totals(all.stream().filter(m -> seasonLabel.equals(m.getSeason())).toList());
        Map<String, List<MatchPerformance>> bySeason = new TreeMap<>(Comparator.reverseOrder());
        all.forEach(m -> bySeason.computeIfAbsent(m.getSeason(), k -> new ArrayList<>()).add(m));
        List<SeasonLineDTO> seasons = bySeason.entrySet().stream().map(e -> new SeasonLineDTO(e.getKey(), totals(e.getValue()))).toList();
        List<MatchLineDTO> recentMatches = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && recentMatches.size() < HISTORY_MATCHES; i--) recentMatches.add(line(all.get(i)));
        List<ScoutingAttribute> weeks = new ArrayList<>(scouting.findTop12ByAthleteIdOrderByIsoWeekDesc(a.getId()));
        Collections.reverse(weeks);
        List<LiveAttributeDTO> attrs = liveAttributes(weeks, prior, recent);
        String week = weeks.isEmpty() ? null : weeks.get(weeks.size() - 1).getIsoWeek();
        DigitalTwin twin = twins.findByUserId(a.getId()).orElse(null);
        String revision = Integer.toHexString(Objects.hash(career, attrs, week, twin == null ? null : twin.getScoreTotal(), twin == null ? null : twin.getRarityTier()));
        return new Snapshot(career, season, seasonLabel, recent, prior, seasons, recentMatches, form(all), attrs, week, twin, revision);
    }

    static TotalsDTO totals(List<MatchPerformance> ms) {
        int official = 0, minutes = 0, goals = 0, assists = 0, pc = 0, pa = 0, dc = 0, da = 0, tackles = 0, cs = 0, mvps = 0, y = 0, r = 0, rated = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (MatchPerformance m : ms) {
            if (m.isOfficial()) official++;
            minutes += m.getMinutesPlayed(); goals += m.getGoals(); assists += m.getAssists();
            pc += m.getPassesCompleted(); pa += m.getPassesAttempted(); dc += m.getDribblesCompleted(); da += m.getDribblesAttempted();
            tackles += m.getTackles(); if (m.isCleanSheet()) cs++; if (m.isMvp()) mvps++; y += m.getYellowCards(); r += m.getRedCards();
            if (m.getRating() != null) { sum = sum.add(m.getRating()); rated++; }
        }
        return new TotalsDTO(ms.size(), official, minutes, goals, assists, pc, pa, pct(pc, pa), dc, da, pct(dc, da), tackles, cs, mvps, y, r,
                rated == 0 ? null : sum.divide(BigDecimal.valueOf(rated), 2, RoundingMode.HALF_EVEN), per90(goals, minutes), per90(assists, minutes), per90(tackles, minutes));
    }

    static BigDecimal pct(int part, int whole) { return whole <= 0 ? null : BigDecimal.valueOf(part * 100L).divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_EVEN); }
    static BigDecimal per90(int n, int minutes) { return minutes <= 0 ? null : BigDecimal.valueOf(n * 90L).divide(BigDecimal.valueOf(minutes), 2, RoundingMode.HALF_EVEN); }

    private static MatchLineDTO line(MatchPerformance m) {
        return new MatchLineDTO(m.getMatchDate(), m.getCompetition(), m.isOfficial(), m.getMinutesPlayed(), m.getGoals(), m.getAssists(),
                m.getPassesCompleted(), m.getPassesAttempted(), m.getDribblesCompleted(), m.getDribblesAttempted(), m.getRating(), m.isMvp());
    }

    /** Nota média das últimas {@value #RECENT} partidas com nota vs. a média das anteriores: ±0,30 muda a tendência. */
    static FormDTO form(List<MatchPerformance> chronological) {
        List<BigDecimal> rated = chronological.stream().map(MatchPerformance::getRating).filter(Objects::nonNull).toList();
        if (rated.isEmpty()) return new FormDTO("FLAT", "Sem partidas com nota", null, null, 0, List.of());
        int split = Math.max(0, rated.size() - RECENT);
        List<BigDecimal> last = List.copyOf(rated.subList(split, rated.size())), before = rated.subList(0, split);
        BigDecimal recentAvg = avg(last);
        if (before.isEmpty()) return new FormDTO("FLAT", "Sem partidas anteriores para comparar", recentAvg, null, last.size(), last);
        BigDecimal ref = avg(before), diff = recentAvg.subtract(ref);
        String trend = diff.compareTo(FORM_THRESHOLD) >= 0 ? "UP" : diff.compareTo(FORM_THRESHOLD.negate()) <= 0 ? "DOWN" : "FLAT";
        return new FormDTO(trend, switch (trend) { case "UP" -> "Em alta"; case "DOWN" -> "Em queda"; default -> "Estável"; }, recentAvg, ref, last.size(), last);
    }

    private static BigDecimal avg(List<BigDecimal> xs) {
        return xs.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(xs.size()), 2, RoundingMode.HALF_EVEN);
    }

    /* ---------- atributos do momento ---------- */

    /** Evidência de forma para um atributo: ajuste (±5) e o texto que o explica. */
    record FormEvidence(int adjustment, String text) {
        static final FormEvidence NONE = new FormEvidence(0, null);
    }

    /**
     * Atributos do momento a partir das semanas de scouting (ordem cronológica) e das partidas: as últimas {@value #RECENT} ({@code recent})
     * contra as anteriores ({@code prior}). Sem {@value #MIN_FORM_SAMPLE} partidas de cada lado, vale só o scouting.
     */
    static List<LiveAttributeDTO> liveAttributes(List<ScoutingAttribute> weeks, TotalsDTO prior, TotalsDTO recent) {
        if (weeks.isEmpty()) return List.of();
        ScoutingAttribute now = weeks.get(weeks.size() - 1), prev = weeks.size() > 1 ? weeks.get(weeks.size() - 2) : null;
        boolean formReady = recent.games() >= MIN_FORM_SAMPLE && prior.games() >= MIN_FORM_SAMPLE;
        List<LiveAttributeDTO> out = new ArrayList<>();
        for (AttrSpec spec : ATTRS) {
            int base = round(spec.get().apply(now));
            Integer previous = prev == null ? null : round(spec.get().apply(prev));
            int weekly = previous == null ? 0 : base - previous;
            FormEvidence f = formReady ? formEvidence(spec.key(), prior, recent) : FormEvidence.NONE;
            int value = Math.max(0, Math.min(100, base + f.adjustment()));
            int adjustment = value - base, change = weekly + adjustment;
            List<String> why = new ArrayList<>();
            why.add(previous == null ? "scouting " + weekLabel(now.getIsoWeek())
                    : weekly == 0 ? "estável no scouting (" + weekLabel(prev.getIsoWeek()) + " → " + weekLabel(now.getIsoWeek()) + ")"
                    : signed(weekly) + " no scouting (" + weekLabel(prev.getIsoWeek()) + " → " + weekLabel(now.getIsoWeek()) + ")");
            if (f.text() != null) why.add((adjustment == 0 ? "forma neutra: " : "forma " + signed(adjustment) + ": ") + f.text());
            out.add(new LiveAttributeDTO(spec.key(), spec.label(), value, base, previous, weekly, adjustment, change > 0 ? "UP" : change < 0 ? "DOWN" : "FLAT",
                    String.join(" · ", why), weeks.stream().map(w -> round(spec.get().apply(w))).toList()));
        }
        return out;
    }

    /** Passe e drible: Δ de aproveitamento (p.p.) ÷ 2 · chute: Δ gols/90 × 10 · desarme: Δ desarmes/90 × 2 — sempre limitado a ±5. */
    static FormEvidence formEvidence(String key, TotalsDTO prior, TotalsDTO recent) {
        String window = " nas últimas " + recent.games() + " vs ";
        return switch (key) {
            case "passe" -> recent.passesAttempted() < 10 || prior.passAccuracy() == null ? FormEvidence.NONE
                    : new FormEvidence(clampAdj(recent.passAccuracy().subtract(prior.passAccuracy()).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_EVEN)),
                    "passes certos " + pctText(recent.passAccuracy()) + window + pctText(prior.passAccuracy()) + " antes");
            case "drible" -> recent.dribblesAttempted() < 5 || prior.dribbleSuccess() == null ? FormEvidence.NONE
                    : new FormEvidence(clampAdj(recent.dribbleSuccess().subtract(prior.dribbleSuccess()).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_EVEN)),
                    "dribles certos " + pctText(recent.dribbleSuccess()) + window + pctText(prior.dribbleSuccess()) + " antes");
            case "finalizacao" -> recent.goalsPer90() == null || prior.goalsPer90() == null ? FormEvidence.NONE
                    : new FormEvidence(clampAdj(recent.goalsPer90().subtract(prior.goalsPer90()).multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_EVEN)),
                    "gols por 90 min " + dec(recent.goalsPer90(), 2) + window + dec(prior.goalsPer90(), 2) + " antes");
            case "desarme" -> recent.tacklesPer90() == null || prior.tacklesPer90() == null ? FormEvidence.NONE
                    : new FormEvidence(clampAdj(recent.tacklesPer90().subtract(prior.tacklesPer90()).multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_EVEN)),
                    "desarmes por 90 min " + dec(recent.tacklesPer90(), 2) + window + dec(prior.tacklesPer90(), 2) + " antes");
            default -> FormEvidence.NONE;   // velocidade, posicionamento, resistência: a súmula não mede — vale o scouting da semana
        };
    }

    private static int clampAdj(BigDecimal v) { return Math.max(-MAX_FORM_ADJUSTMENT, Math.min(MAX_FORM_ADJUSTMENT, v.intValue())); }
    private static int round(BigDecimal v) { return v == null ? 0 : v.setScale(0, RoundingMode.HALF_EVEN).intValue(); }

    /* ======================= Copilot: "Por que contratar este jogador?" ======================= */

    public PitchDTO pitch(AuthPrincipal viewer, UUID athleteId) {
        Athlete a = requireAthlete(viewer, athleteId);
        CopilotStatusDTO status = copilotStatus(viewer, a);
        if (!status.available()) throw ApiException.forbidden(status.code(), status.reason());
        Snapshot s = snapshot(a);
        DigitalTwin twin = s.twin();
        LocalDate today = LocalDate.now(clock);
        List<TechnicalEvaluation> evs = evaluations.findAllByAthleteIdAndWeekOfBetween(a.getId(), today.minusWeeks(12), today).stream()
                .filter(TechnicalEvaluation::isCountsForRarity).toList();   // só avaliações verificadas (comissão, scout homologado)
        Integer scoreDelta = null;
        if (twin != null) {
            List<WeeklyScoreSnapshot> weeks = snapshots.findTop12ByTwinIdOrderByIsoWeekDesc(twin.getId());
            if (weeks.size() >= 2) scoreDelta = weeks.get(0).getScoreTotal() - weeks.get(weeks.size() - 1).getScoreTotal();
        }
        return buildPitch(new PitchInput(a.getId(), a.getDisplayName(), a.getPosition(), a.age(), a.getContractStatus(), s.career(), s.season(), s.seasonLabel(),
                s.form(), s.attributes(), s.attributesWeek(), twin == null ? RarityTier.COMMON : twin.getRarityTier(), twin == null ? 0 : twin.getScoreTotal(),
                topPercent(twin), scoreDelta, weightedAverage(evs), evs.size(), s.revision(), clock.instant()));
    }

    /** O atleta é o titular dos dados analisados: o Copilot depende do consentimento <i>dele</i>; menores ficam de fora (ECA Digital). */
    CopilotStatusDTO copilotStatus(AuthPrincipal viewer, Athlete a) {
        if (a.isMinor()) return new CopilotStatusDTO(false, "COPILOT_MINOR", "O Copilot de contratação não analisa atletas menores de 18 anos (LGPD Art. 14 · ECA Digital)");
        if (prefs.findById(a.getId()).map(UserLgpdPreferences::isAiAgentProcessing).orElse(false)) return new CopilotStatusDTO(true, null, null);
        boolean owner = viewer != null && viewer.userId().equals(a.getId());
        return owner
                ? new CopilotStatusDTO(false, "AI_PROCESSING_OPT_IN", "Ative 'Processamento por agentes de IA' na Central de Privacidade para o Copilot explicar por que contratar você")
                : new CopilotStatusDTO(false, "ATHLETE_AI_OPT_OUT", "Este atleta não autorizou a análise dos dados dele por agentes de IA (LGPD Art. 7º, I · Art. 20)");
    }

    /** Mesma faixa de percentil do card (Top 1 · 5 · 10 · 25 %), só com ≥ 10 gêmeos ativos. */
    Integer topPercent(DigitalTwin twin) {
        if (twin == null || twin.getStatus() != TwinStatus.ACTIVE) return null;
        long total = twins.countByStatus(TwinStatus.ACTIVE);
        if (total < 10) return null;
        long above = twins.countByStatusAndScoreTotalGreaterThan(TwinStatus.ACTIVE, twin.getScoreTotal());
        int pct = (int) Math.max(1, Math.ceil(100.0 * (above + 1) / total));
        return pct <= 1 ? 1 : pct <= 5 ? 5 : pct <= 10 ? 10 : pct <= 25 ? 25 : null;
    }

    static BigDecimal weightedAverage(List<TechnicalEvaluation> evs) {
        BigDecimal sum = BigDecimal.ZERO, weights = BigDecimal.ZERO;
        for (TechnicalEvaluation e : evs) { sum = sum.add(e.getRating().multiply(e.getWeight())); weights = weights.add(e.getWeight()); }
        return weights.signum() == 0 ? null : sum.divide(weights, 2, RoundingMode.HALF_EVEN);
    }

    /** Entradas do Copilot (todas de dados abertos do Gêmeo). */
    record PitchInput(UUID athleteId, String name, Position position, Integer age, ContractStatus contract, TotalsDTO career, TotalsDTO season, String seasonLabel,
                      FormDTO form, List<LiveAttributeDTO> attributes, String attributesWeek, RarityTier tier, int score, Integer topPercent, Integer scoreDelta,
                      BigDecimal evaluationAvg, int evaluationCount, String revision, Instant now) { }

    /** Argumento interno: {@code hook} é a forma curta usada no título e na resposta ("com …", "reúne …"). */
    private record Reason(String icon, String title, String evidence, String category, String hook) {
        ReasonDTO dto() { return new ReasonDTO(icon, title, evidence, category); }
    }

    static PitchDTO buildPitch(PitchInput in) {
        TotalsDTO c = in.career();
        boolean hasSeason = in.season().games() > 0;
        TotalsDTO base = hasSeason ? in.season() : c;   // produção e minutagem: temporada atual quando houver, senão a carreira registrada
        String scope = hasSeason ? "na temporada " + in.seasonLabel() : "na carreira registrada";
        Map<String, LiveAttributeDTO> attr = in.attributes().stream().collect(Collectors.toMap(LiveAttributeDTO::key, x -> x));
        Position pos = in.position();
        boolean gk = pos == Position.GOLEIRO, def = pos == Position.ZAGUEIRO || pos == Position.LATERAL || pos == Position.VOLANTE;
        FormDTO f = in.form();

        List<Reason> rs = new ArrayList<>();
        if (gk) {
            if (base.cleanSheets() > 0) rs.add(new Reason("🧱", "Segurança no gol", n(base.cleanSheets()) + " jogos sem sofrer gol em " + n(base.games()) + " " + scope, "PRODUCAO",
                    n(base.cleanSheets()) + " jogos sem sofrer gol " + scope));
        } else {
            int ga = base.goals() + base.assists();
            BigDecimal ga90 = per90(ga, base.minutes());
            if (ga >= 3 || (ga > 0 && ga90 != null && ga90.compareTo(new BigDecimal("0.30")) >= 0))
                rs.add(new Reason("⚽", "Produção ofensiva", n(base.goals()) + " gols e " + n(base.assists()) + " assistências em " + n(base.games()) + " jogos " + scope
                        + (ga90 == null ? "" : " · " + dec(ga90, 2) + " participações em gol por 90 min"), "PRODUCAO", n(ga) + " participações em gol " + scope));
        }
        if (def && base.tacklesPer90() != null && base.tacklesPer90().compareTo(new BigDecimal("1.50")) >= 0)
            rs.add(new Reason("🛡️", "Poder de desarme", n(base.tackles()) + " desarmes " + scope + " · " + dec(base.tacklesPer90(), 2) + " por 90 min", "DEFESA",
                    dec(base.tacklesPer90(), 1) + " desarmes por 90 min"));
        if (c.passAccuracy() != null && c.passesAttempted() >= 50 && c.passAccuracy().compareTo(BigDecimal.valueOf(80)) >= 0)
            rs.add(new Reason("🎯", "Passe confiável", pctText(c.passAccuracy()) + " de passes certos (" + n(c.passesCompleted()) + " de " + n(c.passesAttempted()) + ")", "PASSE",
                    pctText(c.passAccuracy()) + " de passes certos"));
        if (c.dribbleSuccess() != null && c.dribblesAttempted() >= 15 && c.dribbleSuccess().compareTo(BigDecimal.valueOf(55)) >= 0)
            rs.add(new Reason("🌀", "Drible que desequilibra", pctText(c.dribbleSuccess()) + " de dribles certos (" + n(c.dribblesCompleted()) + " de " + n(c.dribblesAttempted()) + ")", "DRIBLE",
                    pctText(c.dribbleSuccess()) + " de dribles certos"));
        List<LiveAttributeDTO> elite = in.attributes().stream().filter(x -> x.value() >= 75).sorted(Comparator.comparingInt(LiveAttributeDTO::value).reversed()).limit(3).toList();
        if (!elite.isEmpty())
            rs.add(new Reason("⚡", "Atributos de elite no momento", elite.stream().map(x -> x.label() + " " + x.value()).collect(Collectors.joining(" · "))
                    + " (scouting " + weekLabel(in.attributesWeek()) + " + forma recente)", "ATRIBUTOS", elite.get(0).label().toLowerCase(BR) + " " + elite.get(0).value() + " no momento"));
        if ("UP".equals(f.trend()))
            rs.add(new Reason("🔥", "Momento em alta", "nota média " + dec(f.recentAvgRating(), 2) + " nas últimas " + f.sample() + " partidas vs " + dec(f.referenceAvgRating(), 2) + " antes",
                    "FORMA", "nota " + dec(f.recentAvgRating(), 1) + " nas últimas " + f.sample() + " partidas"));
        List<LiveAttributeDTO> rising = in.attributes().stream().filter(x -> x.weeklyDelta() + x.formAdjustment() > 0)
                .sorted(Comparator.comparingInt((LiveAttributeDTO x) -> x.weeklyDelta() + x.formAdjustment()).reversed()).limit(2).toList();
        if (!rising.isEmpty())
            rs.add(new Reason("📈", "Em evolução agora", rising.stream().map(x -> x.label() + " " + signed(x.weeklyDelta() + x.formAdjustment())).collect(Collectors.joining(" · "))
                    + " entre a última semana de scouting e a forma recente", "EVOLUCAO", rising.get(0).label().toLowerCase(BR) + " em evolução"));
        if (in.topPercent() != null)
            rs.add(new Reason("✦", "Entre os melhores do ranking", "Top " + in.topPercent() + "% do Score_Total (" + n(in.score()) + ")", "RANKING", "lugar no Top " + in.topPercent() + "% do ranking"));
        if (in.evaluationAvg() != null && in.evaluationAvg().compareTo(BigDecimal.valueOf(7)) >= 0)
            rs.add(new Reason("🧠", "Aprovado na avaliação técnica", "média ponderada " + dec(in.evaluationAvg(), 1) + "/10 em " + in.evaluationCount()
                    + (in.evaluationCount() == 1 ? " avaliação verificada" : " avaliações verificadas") + " nas últimas 12 semanas", "AVALIACAO",
                    "avaliação técnica " + dec(in.evaluationAvg(), 1) + "/10"));
        if (in.scoreDelta() != null && in.scoreDelta() >= 100)
            rs.add(new Reason("🚀", "Curva de evolução", "+" + n(in.scoreDelta()) + " pontos de Score_Total nas últimas 12 semanas", "EVOLUCAO",
                    "+" + n(in.scoreDelta()) + " pontos de Score_Total em 12 semanas"));
        if (c.mvps() > 0)
            rs.add(new Reason("🏆", "Decide jogos", n(c.mvps()) + (c.mvps() == 1 ? " prêmio" : " prêmios") + " de melhor em campo", "DECISIVO",
                    n(c.mvps()) + (c.mvps() == 1 ? " prêmio" : " prêmios") + " de melhor em campo"));
        if (c.avgRating() != null && c.games() >= 5 && c.avgRating().compareTo(BigDecimal.valueOf(7)) >= 0)
            rs.add(new Reason("📊", "Regularidade", "nota média " + dec(c.avgRating(), 2) + " em " + n(c.games()) + " jogos", "REGULARIDADE", "nota média " + dec(c.avgRating(), 1)));
        if (in.contract() == ContractStatus.AGENTE_LIVRE)
            rs.add(new Reason("🤝", "Agente livre", "sem custo de transferência: negociação direta com o atleta", "CONTRATO", "custo zero de transferência"));
        if (base.games() >= 5 && base.minutes() * 10L >= base.games() * 90L * 8)
            rs.add(new Reason("⏱️", "Minutagem de titular", n(base.minutes()) + " min " + scope + " · " + Math.round(100.0 * base.minutes() / (base.games() * 90.0)) + "% dos minutos possíveis",
                    "MINUTAGEM", "minutagem de titular"));
        if (in.age() != null && in.age() >= 18 && in.age() <= 23)
            rs.add(new Reason("🌱", "Margem de evolução", in.age() + " anos, ainda no início da curva de desenvolvimento", "IDADE", "margem de evolução aos " + in.age() + " anos"));
        if (c.games() >= 10 && c.yellow() <= 2 && c.red() == 0)
            rs.add(new Reason("🟩", "Disciplina", n(c.yellow()) + (c.yellow() == 1 ? " amarelo" : " amarelos") + " e nenhum vermelho em " + n(c.games()) + " jogos", "DISCIPLINA",
                    "zero cartão vermelho em " + n(c.games()) + " jogos"));
        List<Reason> top = rs.stream().limit(MAX_REASONS).toList();

        List<String> attention = new ArrayList<>();
        if (c.games() < 5) attention.add("Amostra pequena: " + c.games() + (c.games() == 1 ? " partida registrada." : " partidas registradas."));
        if (c.games() > 0 && c.officialGames() * 2 < c.games())
            attention.add(Math.round(100.0 * (c.games() - c.officialGames()) / c.games()) + "% das partidas são autorrelato (não oficiais): confirme pela súmula.");
        if ("DOWN".equals(f.trend()))
            attention.add("Forma em queda: nota " + dec(f.recentAvgRating(), 2) + " nas últimas " + f.sample() + " partidas vs " + dec(f.referenceAvgRating(), 2) + " antes.");
        in.attributes().stream().filter(x -> x.weeklyDelta() + x.formAdjustment() <= -3)
                .forEach(x -> attention.add("Queda recente em " + x.label() + " (" + signed(x.weeklyDelta() + x.formAdjustment()) + ")."));
        if (c.red() > 0 || (c.games() >= 5 && c.yellow() * 10 >= c.games() * 3))
            attention.add("Disciplina: " + n(c.yellow()) + (c.yellow() == 1 ? " amarelo" : " amarelos") + " e " + n(c.red()) + (c.red() == 1 ? " vermelho" : " vermelhos") + " em " + n(c.games()) + " jogos.");
        if (c.passAccuracy() != null && c.passesAttempted() >= 50 && c.passAccuracy().compareTo(BigDecimal.valueOf(70)) < 0)
            attention.add("Passe abaixo de 70% de acerto (" + pctText(c.passAccuracy()) + ").");
        if (c.games() > 0 && c.passesAttempted() == 0 && c.dribblesAttempted() == 0)
            attention.add("Súmulas sem passes e dribles registrados: o aproveitamento ainda não pode ser calculado.");
        if (in.attributes().isEmpty()) attention.add("Sem atributos de scouting registrados.");

        List<String> fit = new ArrayList<>();
        Function<String, Integer> v = k -> attr.containsKey(k) ? attr.get(k).value() : 0;
        if (v.apply("velocidade") >= 80 && (pos == Position.PONTA || pos == Position.ATACANTE || pos == Position.LATERAL)) fit.add("Transição rápida e ataque à profundidade");
        if (v.apply("finalizacao") >= 80) fit.add("Referência de área na finalização");
        if (v.apply("drible") >= 80) fit.add("Um contra um no último terço");
        if (v.apply("passe") >= 80) fit.add("Construção e controle da posse");
        if (v.apply("desarme") >= 75) fit.add("Pressão alta e recuperação de bola");
        if (v.apply("resistencia") >= 80) fit.add("Modelos de alta intensidade");
        if (v.apply("posicionamento") >= 80) fit.add("Jogo posicional e coberturas");
        if (fit.isEmpty() && pos != null) fit.add("Opção de elenco para " + positionLabel(pos).toLowerCase(BR));
        if (fit.size() > 3) fit = fit.subList(0, 3);

        boolean officialMajority = c.games() > 0 && c.officialGames() * 10 >= c.games() * 6;
        String confidence = c.games() >= 10 && officialMajority && !in.attributes().isEmpty() ? "ALTA" : c.games() >= 5 ? "MEDIA" : "BAIXA";

        String who = (pos == null ? "Atleta" : positionLabel(pos)) + (in.age() == null ? "" : " de " + in.age() + " anos")
                + (in.contract() == ContractStatus.AGENTE_LIVRE ? ", agente livre," : "");
        Reason lead = top.stream().filter(r -> !r.category().equals("CONTRATO") && !r.category().equals("IDADE")).findFirst().orElse(null);
        String headline = who + (lead == null ? " ainda construindo o histórico na plataforma" : " com " + lead.hook());

        StringBuilder answer = new StringBuilder();
        List<String> hooks = top.stream().filter(r -> !r.category().equals("IDADE")).limit(3).map(Reason::hook).toList();
        if (hooks.isEmpty()) answer.append("Ainda não há dados suficientes para recomendar a contratação de ").append(in.name()).append(": registre partidas oficiais e semanas de scouting.");
        else answer.append("Porque ").append(in.name()).append(" reúne ").append(joinPt(hooks)).append('.');
        if (!fit.isEmpty()) answer.append(" Encaixe ideal: ").append(joinPt(fit.stream().map(VitrineService::lowerFirst).toList())).append('.');
        if (!attention.isEmpty()) answer.append(" Atenção: ").append(lowerFirst(attention.get(0)));
        answer.append(' ').append(switch (confidence) {
            case "ALTA" -> "Os números vêm em maioria de súmulas oficiais: base sólida para avançar à observação presencial.";
            case "MEDIA" -> "Base razoável de dados: confirme com observação presencial antes de propor.";
            default -> "Ainda há poucos dados: trate como indicação inicial e acompanhe as próximas partidas.";
        });

        List<String> sources = new ArrayList<>();
        sources.add(c.officialGames() == 1 ? "1 súmula oficial" : n(c.officialGames()) + " súmulas oficiais");
        int self = c.games() - c.officialGames();
        if (self > 0) sources.add(self == 1 ? "1 partida autorrelatada" : n(self) + " partidas autorrelatadas");
        if (in.attributesWeek() != null) sources.add("Scouting " + weekLabel(in.attributesWeek()));
        if (in.evaluationCount() > 0) sources.add(in.evaluationCount() == 1 ? "1 avaliação técnica verificada" : in.evaluationCount() + " avaliações técnicas verificadas");
        sources.add("Rarity Engine (Score_Total semanal)");

        return new PitchDTO(in.athleteId(), QUESTION, headline, answer.toString(), top.stream().map(Reason::dto).toList(), attention, fit, confidence,
                switch (confidence) { case "ALTA" -> "Confiança alta"; case "MEDIA" -> "Confiança média"; default -> "Confiança baixa"; },
                sources, in.tier().name(), in.tier().crestIcon(), in.revision(),
                "Gerado por IA com regras determinísticas sobre os dados abertos do Gêmeo (sem LLM externo). Não usa dados de saúde (LGPD Art. 11): "
                        + "peça a avaliação do departamento médico. A decisão de contratar é humana (LGPD Art. 20).",
                "copilot", true, in.now());
    }

    /* ---------- util ---------- */

    private Athlete requireAthlete(AuthPrincipal viewer, UUID athleteId) {
        User u = access.requireProfile(viewer, athleteId, "Atleta");   // mesma regra do perfil: 404 para "não existe" e "não pode ver"
        if (!(u instanceof Athlete a)) throw ApiException.notFound("Atleta");
        return a;
    }

    static String weekLabel(String isoWeek) { return isoWeek == null ? "—" : isoWeek.contains("-W") ? "W" + isoWeek.split("-W")[1] : isoWeek; }
    static String positionLabel(Position p) { return p.name().charAt(0) + p.name().substring(1).toLowerCase(BR); }
    private static String n(long v) { return String.format(BR, "%,d", v); }
    private static String signed(int v) { return v > 0 ? "+" + v : String.valueOf(v); }
    private static String dec(BigDecimal v, int scale) { return v == null ? "—" : v.setScale(scale, RoundingMode.HALF_EVEN).toPlainString().replace('.', ','); }
    static String pctText(BigDecimal v) { return v == null ? "—" : v.stripTrailingZeros().toPlainString().replace('.', ',') + "%"; }
    private static String lowerFirst(String s) { return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1); }
    private static String joinPt(List<String> xs) {
        if (xs.size() <= 1) return String.join("", xs);
        return String.join(", ", xs.subList(0, xs.size() - 1)) + " e " + xs.get(xs.size() - 1);
    }
}
