package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ClubStrengthDtos.*;
import com.footballai.social.controller.dto.VitrineDtos.CopilotStatusDTO;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.Position;
import com.footballai.social.model.enums.ProfileVisibility;
import com.footballai.social.repository.AthleteRepository;
import com.footballai.social.repository.ClubRepository;
import com.footballai.social.repository.ScoutingAttributeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Força do clube e "Simular contratação" (Vitrine do Jogador).
 * <ul>
 *   <li><b>Nota do jogador no setor</b> (0–100): média ponderada dos atributos de scouting mais recentes, com pesos por setor.</li>
 *   <li><b>Melhor onze</b>: entre formações usuais, a que dá o maior score global; empate fica com a primeira da lista (4-3-3).</li>
 *   <li><b>Score do setor</b>: soma das notas dos titulares ÷ titulares de referência (goleiro 1 · defesa 4 · meio 3 · ataque 3), limitado a 100.
 *       Vaga sem jogador conta 0; um setor com mais titulares que a referência fica mais forte, com menos fica mais fraco.</li>
 *   <li><b>Score global</b>: goleiro 10 % · defesa 30 % · meio-campo 30 % · ataque 30 %.</li>
 *   <li><b>Simulação</b>: recalcula com o atleta no elenco. Ele pode subir um setor e, se o melhor onze mudar de formação, outro setor perde um titular.</li>
 * </ul>
 * Só perfis públicos e ativos entram no elenco (como no ranking de clubes). A simulação segue a regra do perfil (404) e,
 * por ser análise automatizada para contratação, a mesma do Copilot: atleta adulto que autorizou processamento por IA.
 */
@Service
public class ClubStrengthService {
    static final String ALGORITHM = "club-strength-v1";

    public enum Sector {
        GOLEIRO("Goleiro", 1, 10), DEFESA("Defesa", 4, 30), MEIO_CAMPO("Meio-campo", 3, 30), ATAQUE("Ataque", 3, 30);
        final String label; final int reference; final int weight;
        Sector(String label, int reference, int weight) { this.label = label; this.reference = reference; this.weight = weight; }
        public String label() { return label; }
    }

    /** Formação = titulares por setor (goleiro sempre 1). */
    record Formation(String name, int def, int mid, int att) {
        int slots(Sector s) { return switch (s) { case GOLEIRO -> 1; case DEFESA -> def; case MEIO_CAMPO -> mid; case ATAQUE -> att; }; }
    }
    static final List<Formation> FORMATIONS = List.of(new Formation("4-3-3", 4, 3, 3), new Formation("4-4-2", 4, 4, 2), new Formation("4-2-3-1", 4, 5, 1),
            new Formation("3-5-2", 3, 5, 2), new Formation("3-4-3", 3, 4, 3), new Formation("5-3-2", 5, 3, 2), new Formation("5-4-1", 5, 4, 1));

    record Player(UUID id, String handle, String name, Position position, Sector sector, int rating) {
        StarterDTO dto() { return new StarterDTO(id, handle, name, position.name(), rating); }
    }

    record Lineup(Formation formation, Map<Sector, List<Player>> starters, Map<Sector, Integer> scores, Map<Sector, Integer> depth, int global, double exact) { }

    private final ClubRepository clubs;
    private final AthleteRepository athletes;
    private final ScoutingAttributeRepository scouting;
    private final AccessPolicy access;
    private final VitrineService vitrine;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ClubStrengthService(ClubRepository clubs, AthleteRepository athletes, ScoutingAttributeRepository scouting, AccessPolicy access, VitrineService vitrine) {
        this(clubs, athletes, scouting, access, vitrine, Clock.systemUTC());
    }

    ClubStrengthService(ClubRepository clubs, AthleteRepository athletes, ScoutingAttributeRepository scouting, AccessPolicy access, VitrineService vitrine, Clock clock) {
        this.clubs = clubs; this.athletes = athletes; this.scouting = scouting; this.access = access; this.vitrine = vitrine; this.clock = clock;
    }

    /* ======================= API ======================= */

    public ClubStrengthDTO strength(AuthPrincipal viewer, UUID clubId) {
        Club c = visibleClub(viewer, clubId);
        List<Player> squad = squad(c.getId(), null);
        return dto(c, best(squad), squad.size(), notes(squad));
    }

    public SimulationDTO simulate(AuthPrincipal viewer, UUID athleteId, UUID clubId) {
        User u = access.requireProfile(viewer, athleteId, "Atleta");
        if (!(u instanceof Athlete a)) throw ApiException.notFound("Atleta");
        Club c = visibleClub(viewer, clubId);
        CopilotStatusDTO status = vitrine.copilotStatus(viewer, a);
        if (!status.available()) throw ApiException.forbidden(status.code(), status.reason());
        if (clubId.equals(a.getCurrentClubId())) throw ApiException.conflict("ALREADY_IN_SQUAD", "O atleta já faz parte do elenco deste clube");
        List<Player> squad = squad(c.getId(), a.getId());
        Player signing = player(a, latest(List.of(a.getId())).get(a.getId()));
        return simulate(c, squad, signing, notes(squad), a.getDisplayName());
    }

    /* ======================= elenco ======================= */

    private Club visibleClub(AuthPrincipal viewer, UUID clubId) {
        Club c = clubs.findById(clubId).orElseThrow(() -> ApiException.notFound("Clube"));
        if (!access.canViewProfile(viewer, c)) throw ApiException.notFound("Clube");
        return c;
    }

    /** Elenco público e ativo do clube com nota no setor; sem posição ou sem scouting o jogador fica sem nota (fora do onze). */
    private List<Player> squad(UUID clubId, UUID exclude) {
        List<Athlete> list = athletes.findAll((r, q, b) -> b.equal(r.get("currentClubId"), clubId)).stream()
                .filter(x -> !x.getId().equals(exclude))
                .filter(x -> x.getStatus() == AccountStatus.ACTIVE && x.getVisibility() == ProfileVisibility.PUBLICO && AccessPolicy.openToPublic(x)).toList();
        Map<UUID, ScoutingAttribute> attrs = latest(list.stream().map(Athlete::getId).toList());
        List<Player> out = new ArrayList<>();
        for (Athlete x : list) out.add(player(x, attrs.get(x.getId())));
        return out;
    }

    private Map<UUID, ScoutingAttribute> latest(List<UUID> ids) {
        Map<UUID, ScoutingAttribute> out = new HashMap<>();
        if (ids.isEmpty()) return out;
        for (ScoutingAttribute s : scouting.findAllByAthleteIdIn(ids))
            out.merge(s.getAthleteId(), s, (x, y) -> x.getIsoWeek().compareTo(y.getIsoWeek()) >= 0 ? x : y);
        return out;
    }

    static Player player(Athlete a, ScoutingAttribute s) {
        Sector sector = sectorOf(a.getPosition());
        return new Player(a.getId(), "@" + a.getUsername(), a.getDisplayName(), a.getPosition(), sector, sector == null || s == null ? -1 : rating(sector, s));
    }

    static Sector sectorOf(Position p) {
        if (p == null) return null;
        return switch (p) {
            case GOLEIRO -> Sector.GOLEIRO;
            case ZAGUEIRO, LATERAL -> Sector.DEFESA;
            case VOLANTE, MEIA -> Sector.MEIO_CAMPO;
            case PONTA, ATACANTE -> Sector.ATAQUE;
        };
    }

    /**
     * Nota no setor (pesos em %). O scouting não mede reflexo nem saída do gol: o goleiro usa posicionamento, passe, velocidade e resistência.
     */
    static int rating(Sector s, ScoutingAttribute a) {
        double v = switch (s) {
            case GOLEIRO -> 50 * d(a.getPosicionamento()) + 20 * d(a.getPasse()) + 15 * d(a.getVelocidade()) + 15 * d(a.getResistencia());
            case DEFESA -> 40 * d(a.getDesarme()) + 30 * d(a.getPosicionamento()) + 15 * d(a.getVelocidade()) + 15 * d(a.getPasse());
            case MEIO_CAMPO -> 35 * d(a.getPasse()) + 20 * d(a.getDrible()) + 20 * d(a.getPosicionamento()) + 15 * d(a.getResistencia()) + 10 * d(a.getDesarme());
            case ATAQUE -> 40 * d(a.getFinalizacao()) + 25 * d(a.getDrible()) + 25 * d(a.getVelocidade()) + 10 * d(a.getPasse());
        };
        return (int) Math.round(v / 100.0);
    }

    private static double d(BigDecimal v) { return v == null ? 0 : v.doubleValue(); }

    /* ======================= melhor onze ======================= */

    static Lineup best(List<Player> squad) {
        Map<Sector, List<Player>> bySector = new EnumMap<>(Sector.class);
        for (Sector s : Sector.values()) bySector.put(s, new ArrayList<>());
        squad.stream().filter(p -> p.sector() != null && p.rating() >= 0).forEach(p -> bySector.get(p.sector()).add(p));
        bySector.values().forEach(l -> l.sort(Comparator.comparingInt(Player::rating).reversed().thenComparing(Player::name, Comparator.nullsLast(Comparator.naturalOrder()))));
        Lineup best = null;
        for (Formation f : FORMATIONS) {
            Lineup l = lineup(f, bySector);
            if (best == null || l.exact() > best.exact() + 1e-9) best = l;   // compara sem arredondar; empate fica com a formação anterior da lista
        }
        return best;
    }

    private static Lineup lineup(Formation f, Map<Sector, List<Player>> bySector) {
        Map<Sector, List<Player>> starters = new EnumMap<>(Sector.class);
        Map<Sector, Integer> scores = new EnumMap<>(Sector.class), depth = new EnumMap<>(Sector.class);
        double global = 0;
        for (Sector s : Sector.values()) {
            List<Player> all = bySector.get(s), xi = all.subList(0, Math.min(f.slots(s), all.size()));
            int sum = xi.stream().mapToInt(Player::rating).sum();
            int score = (int) Math.min(100, Math.round((double) sum / s.reference));
            starters.put(s, List.copyOf(xi)); scores.put(s, score); depth.put(s, all.size());
            global += score * s.weight / 100.0;
        }
        return new Lineup(f, starters, scores, depth, (int) Math.round(global), global);
    }

    static List<String> notes(List<Player> squad) {
        List<String> notes = new ArrayList<>();
        long unrated = squad.stream().filter(p -> p.rating() < 0).count();
        if (unrated > 0) notes.add(unrated + (unrated == 1 ? " jogador sem posição ou scouting ficou fora do cálculo." : " jogadores sem posição ou scouting ficaram fora do cálculo."));
        Lineup l = best(squad);
        for (Sector s : Sector.values()) {
            int missing = l.formation().slots(s) - l.starters().get(s).size();
            if (missing > 0) notes.add(s.label + ": " + missing + (missing == 1 ? " vaga de titular sem jogador avaliado (conta 0)." : " vagas de titular sem jogador avaliado (contam 0)."));
        }
        notes.add("Só perfis públicos entram no cálculo. O scouting não mede reflexo de goleiro.");
        return notes;
    }

    private ClubStrengthDTO dto(Club c, Lineup l, int squadSize, List<String> notes) {
        List<SectorDTO> sectors = Arrays.stream(Sector.values()).map(s -> new SectorDTO(s.name(), s.label, l.scores().get(s), l.starters().get(s).size(), s.reference,
                l.depth().get(s), l.starters().get(s).stream().map(Player::dto).toList())).toList();
        int rated = l.depth().values().stream().mapToInt(Integer::intValue).sum();
        return new ClubStrengthDTO(c.getId(), c.getDisplayName(), l.global(), l.formation().name(), sectors, squadSize, rated, notes, ALGORITHM, clock.instant());
    }

    /* ======================= simulação ======================= */

    SimulationDTO simulate(Club c, List<Player> squad, Player signing, List<String> notes, String athleteName) {
        Lineup before = best(squad);
        ClubStrengthDTO beforeDto = dto(c, before, squad.size(), notes);
        String disclaimer = "Simulação determinística com o scouting mais recente e só perfis públicos do elenco; não considera dados de saúde, salário nem entrosamento. "
                + "A decisão de contratar é humana (LGPD Art. 20).";
        if (signing.sector() == null || signing.rating() < 0) {
            return new SimulationDTO(signing.id(), c.getId(), c.getDisplayName(), "INSUFFICIENT_DATA", signing.sector() == null ? null : signing.sector().name(), null,
                    "Sem posição ou sem atributos de scouting: não dá para simular.", null, beforeDto, beforeDto, 0, List.of(), List.of(), List.of(),
                    "Registre a posição e uma semana de scouting do atleta para simular a contratação.", disclaimer);
        }
        List<Player> with = new ArrayList<>(squad); with.add(signing);
        Lineup after = best(with);
        ClubStrengthDTO afterDto = dto(c, after, with.size(), notes(with));

        boolean starter = after.starters().get(signing.sector()).stream().anyMatch(p -> p.id().equals(signing.id()));
        Set<UUID> afterIds = after.starters().values().stream().flatMap(List::stream).map(Player::id).collect(Collectors.toSet());
        Player displaced = before.starters().get(signing.sector()).stream().filter(p -> !afterIds.contains(p.id())).min(Comparator.comparingInt(Player::rating)).orElse(null);
        boolean filledGap = starter && displaced == null && before.starters().get(signing.sector()).size() < before.formation().slots(signing.sector());
        String role = !starter ? "Entra como reserva em " + signing.sector().label.toLowerCase(Locale.ROOT) + ": os titulares do setor têm nota maior."
                : displaced != null ? "Titular em " + signing.sector().label.toLowerCase(Locale.ROOT) + " no lugar de " + displaced.handle() + " (" + displaced.rating() + " → " + signing.rating() + ")."
                : filledGap ? "Titular em " + signing.sector().label.toLowerCase(Locale.ROOT) + ", ocupando uma vaga que estava sem jogador avaliado."
                : "Titular em " + signing.sector().label.toLowerCase(Locale.ROOT) + " com a mudança de formação.";

        List<SectorDeltaDTO> deltas = new ArrayList<>();
        for (Sector s : Sector.values()) {
            int b = before.scores().get(s), a = after.scores().get(s), slotsB = before.formation().slots(s), slotsA = after.formation().slots(s);
            List<String> why = new ArrayList<>();
            if (s == signing.sector() && starter) why.add(displaced != null ? "o atleta (" + signing.rating() + ") entra no lugar de " + displaced.handle() + " (" + displaced.rating() + ")"
                    : "o atleta (" + signing.rating() + ") ganha uma vaga de titular");
            if (slotsA != slotsB) why.add("formação " + before.formation().name() + " → " + after.formation().name() + ": "
                    + (slotsA > slotsB ? "ganha " + (slotsA - slotsB) : "perde " + (slotsB - slotsA)) + (Math.abs(slotsA - slotsB) == 1 ? " titular" : " titulares"));
            if (why.isEmpty()) why.add(a == b ? "sem mudança" : "nova escolha de titulares");
            deltas.add(new SectorDeltaDTO(s.name(), s.label, b, a, a - b, a > b ? "UP" : a < b ? "DOWN" : "FLAT", String.join(" · ", why)));
        }
        List<SectorDeltaDTO> up = deltas.stream().filter(x -> x.delta() > 0).sorted(Comparator.comparingInt(SectorDeltaDTO::delta).reversed()).toList();
        List<SectorDeltaDTO> down = deltas.stream().filter(x -> x.delta() < 0).sorted(Comparator.comparingInt(SectorDeltaDTO::delta)).toList();
        int globalDelta = after.global() - before.global();

        StringBuilder sum = new StringBuilder("Com ").append(athleteName).append(", o score global do clube vai de ").append(before.global()).append(" para ").append(after.global())
                .append(" (").append(globalDelta > 0 ? "+" : "").append(globalDelta).append(").");
        if (!up.isEmpty()) sum.append(" Aumenta: ").append(up.stream().map(x -> x.label() + " +" + x.delta()).collect(Collectors.joining(", "))).append('.');
        if (!down.isEmpty()) sum.append(" Diminui: ").append(down.stream().map(x -> x.label() + " " + x.delta()).collect(Collectors.joining(", "))).append('.');
        if (up.isEmpty() && down.isEmpty()) sum.append(" Nenhum setor muda: o atleta não entra no melhor onze.");
        if (!after.formation().name().equals(before.formation().name())) sum.append(" O melhor onze passa de ").append(before.formation().name()).append(" para ").append(after.formation().name()).append('.');

        return new SimulationDTO(signing.id(), c.getId(), c.getDisplayName(), "SIMULATED", signing.sector().name(), signing.rating(), role,
                displaced == null ? null : displaced.dto(), beforeDto, afterDto, globalDelta, deltas, up, down, sum.toString(), disclaimer);
    }
}
