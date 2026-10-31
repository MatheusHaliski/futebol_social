package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.AthleteCardDTO;
import com.footballai.social.controller.dto.PerformanceDtos.DnaDTO;
import com.footballai.social.controller.dto.PerformanceDtos.EvolutionDTO;
import com.footballai.social.controller.dto.PerformanceDtos.PointDTO;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Sub-abas do Gêmeo: evolução (linha do tempo de score/tier), DNA de jogo (radar) e comparador. */
@Service
public class TwinService {
    private final DigitalTwinRepository twins;
    private final WeeklyScoreSnapshotRepository snapshots;
    private final AthleteBadgeRepository badges;
    private final ScoutingAttributeRepository scouting;
    private final AthleteRepository athletes;
    private final CardAssembler assembler;
    private final AccessPolicy access;

    public TwinService(DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, AthleteBadgeRepository badges, ScoutingAttributeRepository scouting, AthleteRepository athletes,
                       CardAssembler assembler, AccessPolicy access) {
        this.twins = twins; this.snapshots = snapshots; this.badges = badges; this.scouting = scouting; this.athletes = athletes; this.assembler = assembler; this.access = access;
    }

    /** Evolução, DNA e comparador seguem a mesma visibilidade do perfil (dono, público, conexão; menor só com consentimento) — senão 404. */
    public EvolutionDTO evolution(AuthPrincipal viewer, UUID athleteId) {
        access.requireProfile(viewer, athleteId, "Gêmeo");
        DigitalTwin t = twins.findByUserId(athleteId).orElseThrow(() -> ApiException.notFound("Gêmeo"));
        List<WeeklyScoreSnapshot> all = snapshots.findAllByTwinIdOrderByIsoWeekAsc(t.getId());
        List<PointDTO> pts = all.stream().map(s -> new PointDTO(s.getIsoWeek(), s.getScoreTotal(), s.getRarityTier().name(), s.getPhysicalScore(), s.getTechnicalScore(), s.getMatchScore())).toList();
        List<String> milestones = new ArrayList<>();
        RarityTier prev = null;
        for (WeeklyScoreSnapshot s : all) {
            if (prev != null && s.getRarityTier() != prev) milestones.add(s.getIsoWeek().replace("20", "").replace("-W", "W") + " · " + prev + " → " + s.getRarityTier());
            prev = s.getRarityTier();
        }
        badges.findAllByAthleteIdAndActiveTrue(athleteId).stream().filter(b -> !b.getBadgeType().isPhysical()).limit(3)
                .forEach(b -> milestones.add(b.getSeason() + " · " + b.getBadgeType().emoji() + " " + b.getBadgeType().label().toUpperCase()));
        return new EvolutionDTO(athleteId, t.getRarityTier().name(), t.getRarityTier().crestIcon(), t.getScoreTotal(), pts, milestones);
    }

    /** Radar 5 eixos (0–100) a partir dos atributos de scouting mais recentes. */
    public DnaDTO dna(AuthPrincipal viewer, UUID athleteId) {
        access.requireProfile(viewer, athleteId, "Gêmeo");
        ScoutingAttribute s = scouting.findFirstByAthleteIdOrderByIsoWeekDesc(athleteId).orElse(null);
        List<String> axes = List.of("ATAQUE", "TÉCNICA", "TÁTICA", "DEFESA", "CRIATIVIDADE");
        if (s == null) return new DnaDTO(athleteId, axes, List.of(0, 0, 0, 0, 0), List.of(), null);
        int ataque = avg(s.getFinalizacao(), s.getVelocidade()), tecnica = avg(s.getPasse(), s.getDrible()), tatica = avg(s.getPosicionamento(), s.getResistencia());
        int defesa = s.getDesarme().intValue(), criat = avg(s.getDrible(), s.getPasse());
        List<String> styles = new ArrayList<>();
        if (ataque >= 75) styles.add("Finalizador"); if (tecnica >= 75) styles.add("Técnico"); if (tatica >= 75) styles.add("Cerebral"); if (defesa >= 75) styles.add("Muralha"); if (s.getVelocidade().intValue() >= 80) styles.add("Veloz");
        return new DnaDTO(athleteId, axes, List.of(ataque, tecnica, tatica, defesa, criat), styles, s.getIsoWeek());
    }

    private static int avg(java.math.BigDecimal a, java.math.BigDecimal b) { return a.add(b).divide(java.math.BigDecimal.valueOf(2), 0, java.math.RoundingMode.HALF_EVEN).intValue(); }

    public List<AthleteCardDTO> compare(AuthPrincipal viewer, List<UUID> ids) {
        if (ids.size() < 2 || ids.size() > 3) throw ApiException.badRequest("COMPARE_SIZE", "Compare 2 ou 3 atletas");
        List<Athlete> found = athletes.findAllById(ids);
        if (found.size() != ids.size() || !found.stream().allMatch(a -> access.canViewProfile(viewer, a))) throw ApiException.notFound("Atleta");
        return assembler.athleteCards(found, viewer);
    }
}
