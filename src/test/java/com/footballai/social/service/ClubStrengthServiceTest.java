package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ClubStrengthDtos.*;
import com.footballai.social.controller.dto.VitrineDtos.CopilotStatusDTO;
import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.entity.Club;
import com.footballai.social.model.entity.ScoutingAttribute;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.Position;
import com.footballai.social.model.enums.ProfileVisibility;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.AthleteRepository;
import com.footballai.social.repository.ClubRepository;
import com.footballai.social.repository.ScoutingAttributeRepository;
import com.footballai.social.service.ClubStrengthService.Player;
import com.footballai.social.service.ClubStrengthService.Sector;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Força do clube por setor e "Simular contratação": o que aumenta, o que diminui e por quê. */
class ClubStrengthServiceTest {
    private final ClubRepository clubs = mock(ClubRepository.class);
    private final AthleteRepository athletes = mock(AthleteRepository.class);
    private final ScoutingAttributeRepository scouting = mock(ScoutingAttributeRepository.class);
    private final AccessPolicy access = mock(AccessPolicy.class);
    private final VitrineService vitrine = mock(VitrineService.class);
    private final ClubStrengthService service = new ClubStrengthService(clubs, athletes, scouting, access, vitrine,
            Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC));
    private final AuthPrincipal scout = new AuthPrincipal(UUID.randomUUID(), UserRole.SCOUT, "s", "j", Instant.MAX);
    private final Club club = club();

    private static Club club() {
        Club c = new Club(); ReflectionTestUtils.setField(c, "id", UUID.randomUUID()); c.setDisplayName("<PREENCHER_CLUBE>");
        return c;
    }

    private static Player p(Position pos, int rating) {
        UUID id = UUID.randomUUID();
        return new Player(id, "@j" + id.toString().substring(0, 4), "<PREENCHER_" + pos + "_" + rating + ">", pos, ClubStrengthService.sectorOf(pos), rating);
    }

    /** Goleiro, 4 defensores, 4 meio-campistas e 2 atacantes, todos com nota 70: o melhor onze é 4-4-2. */
    private static List<Player> squad442() {
        List<Player> s = new ArrayList<>(List.of(p(Position.GOLEIRO, 70)));
        for (int i = 0; i < 2; i++) { s.add(p(Position.ZAGUEIRO, 70)); s.add(p(Position.LATERAL, 70)); s.add(p(Position.VOLANTE, 70)); s.add(p(Position.MEIA, 70)); }
        s.add(p(Position.ATACANTE, 70)); s.add(p(Position.PONTA, 70));
        return s;
    }

    @Test
    void sectorRatingUsesSectorWeights() {
        ScoutingAttribute a = new ScoutingAttribute();
        a.setVelocidade(BigDecimal.valueOf(90)); a.setFinalizacao(BigDecimal.valueOf(80)); a.setPasse(BigDecimal.valueOf(70)); a.setDrible(BigDecimal.valueOf(60));
        a.setDesarme(BigDecimal.valueOf(40)); a.setPosicionamento(BigDecimal.valueOf(50)); a.setResistencia(BigDecimal.valueOf(60));
        assertThat(ClubStrengthService.rating(Sector.ATAQUE, a)).isEqualTo(77);      // .40·80 + .25·60 + .25·90 + .10·70 = 76,5
        assertThat(ClubStrengthService.rating(Sector.DEFESA, a)).isEqualTo(55);      // .40·40 + .30·50 + .15·90 + .15·70
        assertThat(ClubStrengthService.sectorOf(Position.VOLANTE)).isEqualTo(Sector.MEIO_CAMPO);
        assertThat(ClubStrengthService.sectorOf(Position.LATERAL)).isEqualTo(Sector.DEFESA);
    }

    @Test
    void bestElevenPicksTheStrongestFormationAndScoresEachSector() {
        var l = ClubStrengthService.best(squad442());
        assertThat(l.formation().name()).isEqualTo("4-4-2");
        assertThat(l.scores().get(Sector.GOLEIRO)).isEqualTo(70);
        assertThat(l.scores().get(Sector.DEFESA)).isEqualTo(70);
        assertThat(l.scores().get(Sector.MEIO_CAMPO)).isEqualTo(93);   // 4 titulares para referência 3
        assertThat(l.scores().get(Sector.ATAQUE)).isEqualTo(47);       // 2 titulares para referência 3
        assertThat(l.global()).isEqualTo(70);
    }

    @Test
    void emptySlotsCountZeroAndUnratedPlayersAreReported() {
        List<Player> squad = List.of(p(Position.GOLEIRO, 80), new Player(UUID.randomUUID(), "@x", "x", Position.MEIA, Sector.MEIO_CAMPO, -1));
        var l = ClubStrengthService.best(squad);
        assertThat(l.scores().get(Sector.DEFESA)).isZero();
        assertThat(l.global()).isEqualTo(8);
        assertThat(ClubStrengthService.notes(squad)).anyMatch(n -> n.contains("1 jogador sem posição ou scouting"))
                .anyMatch(n -> n.startsWith("Defesa: 4 vagas"));
    }

    @Test
    void simulationShowsWhatGoesUpAndWhatGoesDown() {
        Player striker = p(Position.ATACANTE, 90);
        SimulationDTO s = service.simulate(club, squad442(), striker, List.of(), "<PREENCHER_ATACANTE>");
        assertThat(s.status()).isEqualTo("SIMULATED");
        assertThat(s.before().formation()).isEqualTo("4-4-2");
        assertThat(s.after().formation()).isEqualTo("3-4-3");          // abrir mais um atacante vale mais que o quarto defensor
        assertThat(s.increases()).extracting(SectorDeltaDTO::key).containsExactly("ATAQUE");
        assertThat(s.decreases()).extracting(SectorDeltaDTO::key).containsExactly("DEFESA");
        SectorDeltaDTO ataque = s.increases().get(0), defesa = s.decreases().get(0);
        assertThat(ataque.before()).isEqualTo(47); assertThat(ataque.after()).isEqualTo(77);
        assertThat(defesa.before()).isEqualTo(70); assertThat(defesa.after()).isEqualTo(53);
        assertThat(defesa.reason()).contains("4-4-2 → 3-4-3").contains("perde 1 titular");
        assertThat(s.globalDelta()).isEqualTo(4);
        assertThat(s.summary()).contains("Aumenta: Ataque +30").contains("Diminui: Defesa -17");
    }

    @Test
    void weakerSigningStaysOnTheBenchAndChangesNothing() {
        SimulationDTO s = service.simulate(club, squad442(), p(Position.ZAGUEIRO, 50), List.of(), "x");
        assertThat(s.role()).startsWith("Entra como reserva");
        assertThat(s.increases()).isEmpty();
        assertThat(s.decreases()).isEmpty();
        assertThat(s.globalDelta()).isZero();
    }

    @Test
    void strongerPlayerReplacesTheWeakestStarter() {
        List<Player> squad = squad442();
        Player weak = p(Position.ZAGUEIRO, 55); squad.set(1, weak);
        SimulationDTO s = service.simulate(club, squad, p(Position.ZAGUEIRO, 85), List.of(), "x");
        assertThat(s.displaced().athleteId()).isEqualTo(weak.id());
        assertThat(s.role()).contains("no lugar de " + weak.handle()).contains("55 → 85");
        assertThat(s.increases()).extracting(SectorDeltaDTO::key).contains("DEFESA");
    }

    @Test
    void athleteWithoutScoutingCannotBeSimulated() {
        SimulationDTO s = service.simulate(club, squad442(), new Player(UUID.randomUUID(), "@y", "y", Position.MEIA, Sector.MEIO_CAMPO, -1), List.of(), "y");
        assertThat(s.status()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(s.after()).isEqualTo(s.before());
    }

    @Test
    @SuppressWarnings("unchecked")
    void apiChecksProfileConsentAndCurrentClub() {
        Athlete a = new Athlete(); ReflectionTestUtils.setField(a, "id", UUID.randomUUID()); a.setPosition(Position.ATACANTE); a.setBirthDate(LocalDate.of(2000, 1, 1));
        when(access.requireProfile(any(), eq(a.getId()), any())).thenReturn(a);
        when(clubs.findById(club.getId())).thenReturn(Optional.of(club));
        when(access.canViewProfile(any(), eq(club))).thenReturn(true);
        when(athletes.findAll(any(Specification.class))).thenReturn(List.of());
        when(vitrine.copilotStatus(any(), eq(a))).thenReturn(new CopilotStatusDTO(false, "ATHLETE_AI_OPT_OUT", "sem consentimento"));
        assertThatThrownBy(() -> service.simulate(scout, a.getId(), club.getId())).isInstanceOf(ApiException.class).extracting("code").isEqualTo("ATHLETE_AI_OPT_OUT");

        when(vitrine.copilotStatus(any(), eq(a))).thenReturn(new CopilotStatusDTO(true, null, null));
        a.setCurrentClubId(club.getId());
        assertThatThrownBy(() -> service.simulate(scout, a.getId(), club.getId())).isInstanceOf(ApiException.class).extracting("code").isEqualTo("ALREADY_IN_SQUAD");

        when(access.canViewProfile(any(), eq(club))).thenReturn(false);
        assertThatThrownBy(() -> service.strength(scout, club.getId())).isInstanceOf(ApiException.class).extracting("code").isEqualTo("NOT_FOUND");
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyPublicActiveAthletesCountForTheClub() {
        Athlete pub = athlete(Position.GOLEIRO, ProfileVisibility.PUBLICO), priv = athlete(Position.ATACANTE, ProfileVisibility.SOMENTE_EU);
        when(clubs.findById(club.getId())).thenReturn(Optional.of(club));
        when(access.canViewProfile(any(), eq(club))).thenReturn(true);
        when(athletes.findAll(any(Specification.class))).thenReturn(List.of(pub, priv));
        ScoutingAttribute old = scout(pub.getId(), "2026-W40", 50), now = scout(pub.getId(), "2026-W47", 80);
        when(scouting.findAllByAthleteIdIn(anyCollection())).thenReturn(List.of(old, now));
        ClubStrengthDTO d = service.strength(scout, club.getId());
        assertThat(d.squadSize()).isEqualTo(1);
        assertThat(d.sectors().get(0).score()).isEqualTo(80);          // semana mais recente
        assertThat(d.sectors()).extracting(SectorDTO::key).containsExactly("GOLEIRO", "DEFESA", "MEIO_CAMPO", "ATAQUE");
    }

    private static Athlete athlete(Position pos, ProfileVisibility v) {
        Athlete a = new Athlete(); ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        a.setPosition(pos); a.setVisibility(v); a.setStatus(AccountStatus.ACTIVE); a.setBirthDate(LocalDate.of(1995, 1, 1)); a.setUsername("u"); a.setDisplayName("n");
        return a;
    }

    private static ScoutingAttribute scout(UUID id, String week, int v) {
        ScoutingAttribute s = new ScoutingAttribute(); s.setAthleteId(id); s.setIsoWeek(week);
        BigDecimal b = BigDecimal.valueOf(v);
        s.setVelocidade(b); s.setPasse(b); s.setFinalizacao(b); s.setDrible(b); s.setDesarme(b); s.setPosicionamento(b); s.setResistencia(b);
        return s;
    }
}
