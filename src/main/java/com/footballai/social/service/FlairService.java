package com.footballai.social.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.FlairDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/**
 * RF6 — FLAIR. Regras: sem dinheiro real nem prêmio monetário (ECA Digital art. 20 · Lei 14.790/2023); pacotes gratuitos
 * com probabilidades visíveis e sorteio determinístico por (usuário, dia); cartas só de atletas ACTIVE com perfil PUBLICO;
 * atletas INJURED/PENDING_DELETE não pontuam; sub-13 só joga contra o sistema; limite diário de partidas; opt-out em 1 toque.
 */
@Service
public class FlairService {
    static final Map<RarityTier, Integer> ODDS = new EnumMap<>(Map.of(RarityTier.COMMON, 55, RarityTier.RARE, 25, RarityTier.EPIC, 12, RarityTier.LEGENDARY, 6, RarityTier.MYTHIC, 2));
    static final int DAILY_MATCH_LIMIT = 10, PACK_SIZE = 3, WIN_POINTS = 100, LOSS_POINTS = 0, SNAP_WIN = 200, SNAP_LOSS = -100;

    private final FlairCardRepository cards;
    private final FlairDeckRepository decks;
    private final FlairMatchRepository matches;
    private final PointsLedgerRepository points;
    private final AthleteRepository athletes;
    private final DigitalTwinRepository twins;
    private final WeeklyScoreSnapshotRepository snapshots;
    private final UserRepository users;
    private final ObjectMapper mapper;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public FlairService(FlairCardRepository cards, FlairDeckRepository decks, FlairMatchRepository matches, PointsLedgerRepository points, AthleteRepository athletes,
                        DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, UserRepository users, ObjectMapper mapper) {
        this(cards, decks, matches, points, athletes, twins, snapshots, users, mapper, Clock.systemUTC());
    }

    FlairService(FlairCardRepository cards, FlairDeckRepository decks, FlairMatchRepository matches, PointsLedgerRepository points, AthleteRepository athletes,
                 DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, UserRepository users, ObjectMapper mapper, Clock clock) {
        this.cards = cards; this.decks = decks; this.matches = matches; this.points = points; this.athletes = athletes; this.twins = twins;
        this.snapshots = snapshots; this.users = users; this.mapper = mapper; this.clock = clock;
    }

    /* ---------------- coleção / pacotes ---------------- */

    public CollectionDTO collection(AuthPrincipal p) {
        List<FlairCard> mine = cards.findAllByOwnerIdOrderByScoreTotalDesc(p.userId());
        FlairDeck deck = decks.findById(p.userId()).orElse(null);
        Instant dayStart = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).toInstant();
        boolean free = cards.countByOwnerIdAndSourceAndAcquiredAtAfter(p.userId(), "PACK", dayStart) == 0;
        int today = (int) matches.countByChallengerIdAndCreatedAtAfter(p.userId(), dayStart);
        return new CollectionDTO(mine.stream().map(c -> toDto(c, false)).toList(), deckDto(deck, mine), points.balance(p.userId()), free, oddsLabels(), today, DAILY_MATCH_LIMIT);
    }

    static Map<String, String> oddsLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        for (RarityTier t : RarityTier.values()) m.put(t.name(), ODDS.get(t) + "%");
        return m;
    }

    /** Um pacote gratuito por dia; sorteio determinístico por SHA-256(userId + dia) — auditável e sem viés. */
    @Transactional
    public PackDTO openPack(AuthPrincipal p) {
        LocalDate today = LocalDate.now(clock);
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        if (cards.countByOwnerIdAndSourceAndAcquiredAtAfter(p.userId(), "PACK", dayStart) > 0)
            throw ApiException.conflict("PACK_ALREADY_OPENED", "Pacote gratuito de hoje já aberto — próximo em " + today.plusDays(1) + "T00:00Z");
        List<DigitalTwin> pool = twins.findAllByStatus(TwinStatus.ACTIVE);
        Map<UUID, Athlete> byId = new HashMap<>();
        for (Athlete a : athletes.findAllById(pool.stream().map(DigitalTwin::getUserId).toList()))
            if (a.getVisibility() == ProfileVisibility.PUBLICO && a.getStatus() == AccountStatus.ACTIVE) byId.put(a.getId(), a);
        List<DigitalTwin> eligible = pool.stream().filter(t -> byId.containsKey(t.getUserId())).sorted(Comparator.comparing(DigitalTwin::getUserId)).toList();
        if (eligible.isEmpty()) throw ApiException.conflict("NO_PUBLIC_ATHLETES", "Ainda não há Gêmeos públicos para gerar cartas");
        Random rng = new Random(seed(p.userId() + "|" + today));
        List<FlairCard> drawn = new ArrayList<>();
        Instant now = clock.instant();
        for (int i = 0; i < PACK_SIZE; i++) {
            RarityTier target = drawTier(rng.nextInt(100));
            List<DigitalTwin> ofTier = eligible.stream().filter(t -> t.getRarityTier() == target).toList();
            if (ofTier.isEmpty()) ofTier = eligible; // sem atleta no tier sorteado: cai para o pool
            DigitalTwin t = ofTier.get(rng.nextInt(ofTier.size()));
            drawn.add(cards.save(snapshotCard(p.userId(), byId.get(t.getUserId()), t, "PACK", now)));
        }
        return new PackDTO(drawn.stream().map(c -> toDto(c, false)).toList(), oddsLabels(), today.plusDays(1) + "T00:00:00Z");
    }

    static RarityTier drawTier(int roll) {
        int acc = 0;
        for (RarityTier t : RarityTier.values()) { acc += ODDS.get(t); if (roll < acc) return t; }
        return RarityTier.COMMON;
    }

    static long seed(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            long v = 0; for (int i = 0; i < 8; i++) v = (v << 8) | (d[i] & 0xff); return v;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    FlairCard snapshotCard(UUID owner, Athlete a, DigitalTwin t, String source, Instant now) {
        int pp = 0, rp = 0, mp = 0;
        var last = snapshots.findTop12ByTwinIdOrderByIsoWeekDesc(t.getId());
        if (!last.isEmpty()) {
            WeeklyScoreSnapshot s = last.get(0);
            pp = s.getPhysicalScore().multiply(java.math.BigDecimal.valueOf(100)).intValue();
            rp = s.getTechnicalScore().multiply(java.math.BigDecimal.valueOf(100)).intValue();
            mp = s.getMatchScore().multiply(java.math.BigDecimal.valueOf(100)).intValue();
        }
        int score = t.getPhysicalStatus() == PhysicalStatus.INJURED || !t.getStatus().countsForRanking() ? 0 : t.getScoreTotal(); // lesionado/pendente não pontua
        return new FlairCard(owner, a.getId(), a.getDisplayName(), a.getPosition() == null ? null : a.getPosition().name(), t.getRarityTier(), pp, rp, mp, score,
                IsoWeeks.season(LocalDate.now(clock)), source, now);
    }

    /* ---------------- escalação ---------------- */

    @Transactional
    public DeckDTO setDeck(AuthPrincipal p, SetDeckRequest r) {
        if (r.cardIds().size() != 3 || new HashSet<>(r.cardIds()).size() != 3) throw ApiException.badRequest("DECK_SIZE", "Escale exatamente 3 cartas distintas");
        List<FlairCard> mine = cards.findAllById(r.cardIds());
        if (mine.size() != 3 || mine.stream().anyMatch(c -> !c.getOwnerId().equals(p.userId()))) throw ApiException.forbidden("NOT_OWNER", "Só cartas da sua coleção");
        FlairDeck deck = decks.findById(p.userId()).orElseGet(() -> new FlairDeck(p.userId()));
        deck.set(r.cardIds().get(0), r.cardIds().get(1), r.cardIds().get(2), r.captainIndex(), clock.instant());
        decks.save(deck);
        return deckDto(deck, mine);
    }

    DeckDTO deckDto(FlairDeck deck, List<FlairCard> owned) {
        if (deck == null) return new DeckDTO(List.of(), 0, false);
        Map<UUID, FlairCard> byId = new HashMap<>(); owned.forEach(c -> byId.put(c.getId(), c));
        List<FlairCardDTO> out = new ArrayList<>();
        UUID[] ids = deck.cards();
        for (int i = 0; i < 3; i++) { FlairCard c = ids[i] == null ? null : byId.getOrDefault(ids[i], cards.findById(ids[i]).orElse(null)); if (c != null) out.add(toDto(c, i == deck.getCaptainIndex())); }
        return new DeckDTO(out, deck.getCaptainIndex(), deck.isComplete() && out.size() == 3);
    }

    /* ---------------- duelo ---------------- */

    record Side(List<FlairCardDTO> deck) { }

    @Transactional
    public MatchDTO startMatch(AuthPrincipal p, StartMatchRequest r) {
        User me = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        Instant dayStart = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (matches.countByChallengerIdAndCreatedAtAfter(p.userId(), dayStart) >= DAILY_MATCH_LIMIT) throw ApiException.conflict("DAILY_LIMIT", "Limite diário de " + DAILY_MATCH_LIMIT + " partidas atingido");
        FlairDeck mine = decks.findById(p.userId()).filter(FlairDeck::isComplete).orElseThrow(() -> ApiException.conflict("DECK_INCOMPLETE", "Escale 3 cartas antes de jogar"));
        List<FlairCardDTO> myDeck = deckDto(mine, cards.findAllByOwnerIdOrderByScoreTotalDesc(p.userId())).cards();
        UUID opponentId = r == null ? null : r.opponentId();
        String label; List<FlairCardDTO> theirDeck;
        Integer age = me.age();
        if (opponentId != null) {
            if (age != null && age < 13) throw ApiException.forbidden("UNDER_13", "Sub-13 joga apenas contra o sistema (sem modo competitivo público)");
            FlairDeck theirs = decks.findById(opponentId).filter(FlairDeck::isComplete).orElseThrow(() -> ApiException.conflict("OPPONENT_DECK", "Oponente sem escalação completa"));
            theirDeck = deckDto(theirs, cards.findAllByOwnerIdOrderByScoreTotalDesc(opponentId)).cards();
            label = users.findById(opponentId).map(u -> "@" + u.getUsername()).orElse("oponente");
        } else {
            theirDeck = systemDeck(p.userId(), myDeck);
            label = "Sistema · Football AI";
        }
        String deckJson;
        try { deckJson = mapper.writeValueAsString(Map.of("mine", myDeck, "theirs", theirDeck)); } catch (Exception e) { throw new IllegalStateException(e); }
        FlairMatch m = matches.save(new FlairMatch(p.userId(), opponentId, label, deckJson, clock.instant()));
        return toDto(m, myDeck, theirDeck, List.of());
    }

    /** Oponente do sistema: 3 cartas virtuais com os mesmos atletas públicos, sorteio determinístico por partida/dia. */
    List<FlairCardDTO> systemDeck(UUID userId, List<FlairCardDTO> myDeck) {
        List<DigitalTwin> pool = twins.findAllByStatus(TwinStatus.ACTIVE).stream().sorted(Comparator.comparing(DigitalTwin::getUserId)).toList();
        Map<UUID, Athlete> byId = new HashMap<>();
        for (Athlete a : athletes.findAllById(pool.stream().map(DigitalTwin::getUserId).toList())) if (a.getVisibility() == ProfileVisibility.PUBLICO) byId.put(a.getId(), a);
        List<DigitalTwin> eligible = pool.stream().filter(t -> byId.containsKey(t.getUserId())).toList();
        Random rng = new Random(seed(userId + "|sys|" + LocalDate.now(clock) + "|" + matches.countByChallengerIdAndCreatedAtAfter(userId, Instant.EPOCH)));
        List<FlairCardDTO> out = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (eligible.isEmpty()) { FlairCardDTO c = myDeck.get(i); out.add(new FlairCardDTO(null, c.athleteId(), c.name(), c.position(), c.tier(), c.crestIcon(), c.color(), c.pp(), c.rp(), c.mp(), c.scoreTotal(), c.power(), i == 0, "SYSTEM", clock.instant())); continue; }
            DigitalTwin t = eligible.get(rng.nextInt(eligible.size()));
            FlairCard c = snapshotCard(userId, byId.get(t.getUserId()), t, "SYSTEM", clock.instant());
            out.add(toDto(c, i == 0));
        }
        return out;
    }

    @Transactional
    public MatchDTO playRound(AuthPrincipal p, UUID matchId, RoundRequest r) {
        FlairMatch m = matches.findByIdForUpdate(matchId).orElseThrow(() -> ApiException.notFound("Partida")); // lock: dois lances simultâneos não jogam o mesmo round
        if (!m.getChallengerId().equals(p.userId())) throw ApiException.forbidden("NOT_PLAYER", "Partida de outro jogador");
        if (m.getStatus() == FlairMatch.Status.FINISHED) throw ApiException.conflict("MATCH_FINISHED", "Partida encerrada");
        Map<String, List<FlairCardDTO>> deck = readDeck(m);
        List<RoundDTO> rounds = readRounds(m);
        int idx = m.getRoundsPlayed();
        FlairCardDTO mine = deck.get("mine").get(idx), theirs = deck.get("theirs").get(idx);
        int myValue = axisValue(mine, r.axis()), theirValue = axisValue(theirs, r.axis());
        String winner = myValue > theirValue ? "ME" : myValue < theirValue ? "THEM" : "DRAW";
        if (r.snap() && !m.isSnapUsed()) m.setSnapUsed(true);
        rounds.add(new RoundDTO(idx + 1, r.axis(), mine.name(), myValue, theirs.name(), theirValue, winner, r.snap() && m.isSnapUsed()));
        if ("ME".equals(winner)) m.setChallengerWins(m.getChallengerWins() + 1); else if ("THEM".equals(winner)) m.setOpponentWins(m.getOpponentWins() + 1);
        m.setRoundsPlayed(idx + 1);
        try { m.setRoundsJson(mapper.writeValueAsString(rounds)); } catch (Exception e) { throw new IllegalStateException(e); }
        if (m.getRoundsPlayed() >= 3) finish(m);
        matches.save(m);
        return toDto(m, deck.get("mine"), deck.get("theirs"), rounds);
    }

    /** Valor do lance: eixo da carta × multiplicador do tier (× 1,2 capitão); carta com score 0 (lesionado) vale 0. */
    static int axisValue(FlairCardDTO c, String axis) {
        if (c.scoreTotal() == 0) return 0;
        int base = switch (axis) { case "PP" -> c.pp(); case "RP" -> c.rp(); default -> c.mp(); };
        return RarityTier.valueOf(c.tier()).flairMultiplier().multiply(java.math.BigDecimal.valueOf(base)).multiply(c.captain() ? new java.math.BigDecimal("1.2") : java.math.BigDecimal.ONE)
                .setScale(0, java.math.RoundingMode.HALF_EVEN).intValue();
    }

    private void finish(FlairMatch m) {
        boolean won = m.getChallengerWins() > m.getOpponentWins();
        boolean draw = m.getChallengerWins() == m.getOpponentWins();
        int pts = draw ? 25 : won ? (m.isSnapUsed() ? SNAP_WIN : WIN_POINTS) : (m.isSnapUsed() ? SNAP_LOSS : LOSS_POINTS);
        long balance = points.balance(m.getChallengerId());
        if (pts < 0 && balance + pts < 0) pts = (int) -balance; // saldo nunca negativo
        m.setPointsAwarded(pts);
        m.setStatus(FlairMatch.Status.FINISHED);
        m.setFinishedAt(clock.instant());
        if (pts != 0) points.save(new PointsLedger(m.getChallengerId(), pts, "FLAIR_REWARD", m.getId(), clock.instant()));
    }

    public MatchDTO match(AuthPrincipal p, UUID id) {
        FlairMatch m = matches.findById(id).orElseThrow(() -> ApiException.notFound("Partida"));
        if (!m.getChallengerId().equals(p.userId())) throw ApiException.forbidden("NOT_PLAYER", "Partida de outro jogador");
        Map<String, List<FlairCardDTO>> deck = readDeck(m);
        return toDto(m, deck.get("mine"), deck.get("theirs"), readRounds(m));
    }

    public PointsDTO points(AuthPrincipal p) {
        return new PointsDTO(points.balance(p.userId()), points.findTop20ByUserIdOrderByAtDesc(p.userId()).stream()
                .map(l -> Map.<String, Object>of("delta", l.getDelta(), "reason", l.getReason(), "at", l.getAt())).toList());
    }

    /* ---------------- util ---------------- */

    private Map<String, List<FlairCardDTO>> readDeck(FlairMatch m) {
        try { return mapper.readValue(m.getDeckJson(), new TypeReference<Map<String, List<FlairCardDTO>>>() { }); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private List<RoundDTO> readRounds(FlairMatch m) {
        try { return new ArrayList<>(mapper.readValue(m.getRoundsJson(), new TypeReference<List<RoundDTO>>() { })); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private MatchDTO toDto(FlairMatch m, List<FlairCardDTO> mine, List<FlairCardDTO> theirs, List<RoundDTO> rounds) {
        String result = m.getStatus() == FlairMatch.Status.FINISHED ? (m.getChallengerWins() > m.getOpponentWins() ? "WIN" : m.getChallengerWins() < m.getOpponentWins() ? "LOSS" : "DRAW") : null;
        return new MatchDTO(m.getId(), m.getStatus().name(), m.getOpponentLabel(), Math.min(3, m.getRoundsPlayed() + 1), m.getChallengerWins(), m.getOpponentWins(),
                m.isSnapUsed(), m.getPointsAwarded(), rounds, mine, theirs, result);
    }
    static FlairCardDTO toDto(FlairCard c, boolean captain) {
        return new FlairCardDTO(c.getId(), c.getAthleteId(), c.getAthleteName(), c.getAthletePosition(), c.getTier().name(), c.getTier().crestIcon(), c.getTier().color(),
                c.getPp(), c.getRp(), c.getMp(), c.getScoreTotal(), c.power(captain), captain, c.getSource(), c.getAcquiredAt());
    }
}
