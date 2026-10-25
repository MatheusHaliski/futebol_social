package com.footballai.social.config;

import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import com.footballai.social.service.BadgeAwardService;
import com.footballai.social.service.IsoWeeks;
import com.footballai.social.service.RarityEngineService;
import com.footballai.social.service.RedisSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Dados de exemplo para os perfis local/docker (app.seed.enabled=true). Nomes/e-mails são placeholders
 * {@code <PREENCHER_*>} / {@code *@exemplo.dev}; senha de todas as contas de exemplo: {@value #PASSWORD}.
 * Roda uma única vez (quando não há usuários).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String PASSWORD = "Senha@Forte123";

    private final UserRepository users; private final AthleteRepository athletes; private final TermsAcceptanceRepository terms;
    private final UserLgpdPreferencesRepository prefs; private final DigitalTwinRepository twins; private final WeeklyScoreSnapshotRepository snapshots;
    private final TrainingSummaryRepository training; private final MatchPerformanceRepository matches; private final TechnicalEvaluationRepository evaluations;
    private final ScoutingAttributeRepository scouting; private final InjuryRecordRepository injuries; private final PostRepository posts;
    private final BadgeAwardService badges; private final RarityEngineService rarity; private final PasswordEncoder encoder; private final AppProperties props;

    public DataSeeder(UserRepository users, AthleteRepository athletes, TermsAcceptanceRepository terms, UserLgpdPreferencesRepository prefs, DigitalTwinRepository twins,
                      WeeklyScoreSnapshotRepository snapshots, TrainingSummaryRepository training, MatchPerformanceRepository matches, TechnicalEvaluationRepository evaluations,
                      ScoutingAttributeRepository scouting, InjuryRecordRepository injuries, PostRepository posts, BadgeAwardService badges, RarityEngineService rarity,
                      PasswordEncoder encoder, AppProperties props) {
        this.users = users; this.athletes = athletes; this.terms = terms; this.prefs = prefs; this.twins = twins; this.snapshots = snapshots; this.training = training;
        this.matches = matches; this.evaluations = evaluations; this.scouting = scouting; this.injuries = injuries; this.posts = posts; this.badges = badges; this.rarity = rarity;
        this.encoder = encoder; this.props = props;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;
        log.info("Seed: criando dados de exemplo (<PREENCHER_*>) — senha das contas: {}", PASSWORD);
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        String season = IsoWeeks.season(today);
        String hash = encoder.encode(PASSWORD);

        // ---- organizações e papéis ----
        LeagueOfficial liga = base(new LeagueOfficial(), "oficial@exemplo.dev", "footballai", "Football AI · Oficial", hash, null, "SP");
        liga.setEntityName("<PREENCHER_FEDERACAO>"); liga.setEntidadeTipo(EntidadeTipo.LIGA); liga.setVerified(true); liga.setVisibility(ProfileVisibility.PUBLICO); liga = users.save(liga);
        Club c1 = club("clube1@exemplo.dev", "preencher_clube_1", "<PREENCHER_CLUBE_1>", 1, "PR", "-25.43", "-49.27", hash);
        Club c2 = club("clube2@exemplo.dev", "preencher_clube_2", "<PREENCHER_CLUBE_2>", 1, "SP", "-23.55", "-46.63", hash);
        Club c3 = club("clube3@exemplo.dev", "preencher_clube_3", "<PREENCHER_CLUBE_3>", 2, "RS", "-30.03", "-51.23", hash);
        Scout scout = base(new Scout(), "scout@exemplo.dev", "preencher_scout", "<PREENCHER_SCOUT>", hash, LocalDate.of(1988, 3, 9), "PR");
        scout.setVerificationLevel(VerificationLevel.HOMOLOGADO); scout.setAgency("<PREENCHER_AGENCIA>"); scout.setVerified(true); scout.setVisibility(ProfileVisibility.PUBLICO); scout = users.save(scout);
        Staff staff = base(new Staff(), "tecnico@exemplo.dev", "preencher_tecnico", "<PREENCHER_TECNICO>", hash, LocalDate.of(1979, 7, 2), "PR");
        staff.setSpecialty(StaffSpecialty.TREINADOR); staff.setHeadCoach(true); staff.setCurrentClubId(c1.getId()); staff.setVisibility(ProfileVisibility.PUBLICO); staff = users.save(staff);
        HealthStaff medico = base(new HealthStaff(), "medico@exemplo.dev", "preencher_medico", "<PREENCHER_MEDICO>", hash, LocalDate.of(1984, 1, 20), "RS");
        medico.setSpecialty(HealthSpecialty.MEDICO); medico.setVerificationLevel(VerificationLevel.VERIFICADO); medico.setCurrentClubId(c3.getId()); medico.setProfessionalRegistry("<PREENCHER_CRM>"); medico = users.save(medico);
        Press press = base(new Press(), "imprensa@exemplo.dev", "preencher_veiculo", "<PREENCHER_VEICULO>", hash, LocalDate.of(1990, 10, 1), "SP");
        press.setOutlet("<PREENCHER_VEICULO>"); press.setMidiaTipo(MidiaTipo.VEICULO_CREDENCIADO); press.setVerified(true); press.setVisibility(ProfileVisibility.PUBLICO); press = users.save(press);
        Supporter torcedor = base(new Supporter(), "torcedor@exemplo.dev", "preencher_torcedor", "<PREENCHER_TORCEDOR>", hash, LocalDate.of(1995, 5, 5), "SP");
        torcedor.setFavoriteClubId(c2.getId()); torcedor.setVisibility(ProfileVisibility.PUBLICO); torcedor = users.save(torcedor);
        for (User u : List.of(liga, c1, c2, c3, scout, staff, medico, press, torcedor)) finishUser(u, now, false);

        // ---- atletas (espelham o mockup da SPA) ----
        record A(String email, String user, String name, Position pos, int age, String state, Club club, ContractStatus cs, String foot, int height, int targetScore, boolean captain, boolean medical) { }
        List<A> spec = List.of(
                new A("atleta1@exemplo.dev", "preencher_1", "<PREENCHER_ATLETA_1>", Position.MEIA, 22, "PR", c1, ContractStatus.CONTRATADO, "D", 176, 1650, false, true),
                new A("atleta2@exemplo.dev", "preencher_2", "<PREENCHER_ATLETA_2>", Position.ATACANTE, 19, "SP", c2, ContractStatus.AGENTE_LIVRE, "E", 181, 2480, true, true),
                new A("atleta3@exemplo.dev", "preencher_3", "<PREENCHER_ATLETA_3>", Position.ZAGUEIRO, 27, "PR", c1, ContractStatus.CONTRATADO, "D", 189, 1120, false, true),
                new A("atleta4@exemplo.dev", "preencher_4", "<PREENCHER_ATLETA_4>", Position.GOLEIRO, 31, "RS", c3, ContractStatus.CONTRATADO, "D", 192, 640, true, true),
                new A("atleta5@exemplo.dev", "preencher_5", "<PREENCHER_ATLETA_5>", Position.LATERAL, 17, "SP", c2, ContractStatus.AGENTE_LIVRE, "E", 172, 310, false, false));
        List<Athlete> created = new ArrayList<>();
        int idx = 0;
        for (A a : spec) {
            idx++;
            Athlete at = base(new Athlete(), a.email(), a.user(), a.name(), hash, today.minusYears(a.age()).minusDays(40L * idx), a.state());
            at.setPosition(a.pos()); at.setDominantFoot(a.foot()); at.setHeightCm(a.height()); at.setCurrentClubId(a.club().getId()); at.setCurrentClubName(a.club().getDisplayName());
            at.setContractStatus(a.cs()); at.setCaptain(a.captain()); at.setVisibility(ProfileVisibility.PUBLICO); at.setCategoria(a.age() < 20 ? "SUB20" : "PROFISSIONAL");
            if (a.age() < 18) at.setGuardianConsentAt(now);
            at = users.save(at);
            finishUser(at, now, a.medical());
            DigitalTwin twin = new DigitalTwin(at.getId()); twin.transitionTo(TwinStatus.ACTIVE, now); twin = twins.save(twin);
            seedHistory(at, twin, a.targetScore(), idx, today, season);
            created.add(at);
        }
        // lesão do goleiro (dado sensível, visível porque shareMedicalData = true no seed)
        Athlete gk = created.get(3);
        InjuryRecord inj = new InjuryRecord(); inj.setAthleteId(gk.getId()); inj.setInjuryType("Lesão grau 2 · coxa"); inj.setBodyPart("coxa"); inj.setSeverity("MODERADA");
        inj.setStartedAt(today.minusDays(6)); inj.setExpectedReturnDate(today.plusWeeks(3)); inj.setReportedBy(medico.getId()); injuries.save(inj);

        // ---- Rarity Engine real sobre a semana anterior + badges ----
        String prevWeek = IsoWeeks.previous(today);
        for (Athlete at : created) { twins.findByUserId(at.getId()).ifPresent(t -> rarity.recalculate(t.getId(), prevWeek)); badges.onWeeklyScore(at.getId(), prevWeek); }

        // ---- posts por janela (título 5–12 palavras, teaser, arte) ----
        post(press, PostContentType.PARTIDA_MOMENTO, FeedWindow.PERFIL, "A promessa sub-20 que saltou de Epic para Legendary", "Análise com dados do Gêmeo Digital e histórico de 90 dias.", null, null, 412, 37, 21, "#3b82f6", CardMaterial.VIDRO, CardLayout.HERO, CardStyle.NEON, now.minusSeconds(3 * 3600));
        post(scout, PostContentType.RELATORIO_TATICO, FeedWindow.PERFIL, "Shortlist da rodada: laterais com passe acima de 85", "Relatório homologado · 5 atletas · 3 estados.", null, null, 96, 12, 8, "#ff9f43", CardMaterial.CARBONO, CardLayout.SPLIT, CardStyle.GRAFITE, now.minusSeconds(5 * 3600));
        post(created.get(0), PostContentType.TREINO, FeedWindow.GEMEO, "Sessão de sprints com 25 km/h sustentados hoje", "+38 PP registrados pelo wearable · 72 min · 8,4 km · RPE 7.", null, null, 138, 12, 5, "#00d47a", CardMaterial.FOSCO, CardLayout.CLASSICO, CardStyle.NEON, now.minusSeconds(2 * 3600));
        post(created.get(1), PostContentType.PARTIDA_MOMENTO, FeedWindow.GEMEO, "Hat-trick de passes decisivos na semifinal estadual", "Minerado como Momento do mês · 90 min · nota 7,8.", null, null, 1240, 96, 58, "#f5c542", CardMaterial.HOLOGRAFICO, CardLayout.HERO, CardStyle.OURO, now.minusSeconds(26 * 3600));
        post(staff, PostContentType.TREINO, FeedWindow.GEMEO, "Coletivo com seis blocos de jogo reduzido", "Foco em transição defensiva · carga média 480 AU.", null, null, 40, 3, 1, "#00d47a", CardMaterial.FOSCO, CardLayout.MINIMAL, CardStyle.GRAFITE, now.minusSeconds(27 * 3600));
        post(liga, PostContentType.ESPECIAL, FeedWindow.PERFIL, "Prêmio Gêmeo da Semana Sub-20 anunciado hoje", "Conquista especial registrada no card de perfil.", null, null, 980, 120, 210, "#f5c542", CardMaterial.METALICO, CardLayout.HERO, CardStyle.OURO, now.minusSeconds(40 * 3600));
        post(scout, PostContentType.RELATORIO_TATICO, FeedWindow.SCOUTING, "Comparador: atacantes Sub-20 do Paraná, top cinco", "Ranking por Score_Total das últimas 4 semanas.", null, null, 64, 9, 4, "#ff9f43", CardMaterial.CARBONO, CardLayout.SPLIT, CardStyle.GRAFITE, now.minusSeconds(8 * 3600));
        post(c1, PostContentType.RELATORIO_TATICO, FeedWindow.SCOUTING, "Ficha técnica completa do meia titular do clube", "PP, RP, MP e histórico de 90 dias.", null, null, 31, 2, 3, "#ff9f43", CardMaterial.VIDRO, CardLayout.SPLIT, CardStyle.VITRAL, now.minusSeconds(9 * 3600));
        post(torcedor, PostContentType.RELATORIO_TATICO, FeedWindow.SCOUTING, "Da arquibancada: o time cresce quando o meia recua", "Relatório da comunidade · opinativo · não entra no Rarity.", null, null, 22, 5, 1, "#ff9f43", CardMaterial.FOSCO, CardLayout.MINIMAL, CardStyle.RETRO, now.minusSeconds(10 * 3600));
        post(staff, PostContentType.RELATORIO_TATICO, FeedWindow.SCOUTING, "Prancheta 4-3-3 com pressão alta e saída sob pressão", "Drill de 12 execuções · aderência média 85%.", null, null, 57, 6, 9, "#ff9f43", CardMaterial.CARBONO, CardLayout.CLASSICO, CardStyle.GRAFITE, now.minusSeconds(30 * 3600));
        post(medico, PostContentType.LESAO_ALERTA, FeedWindow.ALERTA, "Atualização médica: afastamento de três semanas confirmado", "Departamento médico confirma diagnóstico e prazo de retorno.", AlertSeverity.ALTA, AlertType.LESAO, 66, 14, 9, "#ff5d5d", CardMaterial.FOSCO, CardLayout.MINIMAL, CardStyle.GRAFITE, now.minusSeconds(600));
        post(liga, PostContentType.INSTITUCIONAL, FeedWindow.ALERTA, "Seleção Football AI Sub-20 tem lista divulgada", "23 atletas convocados para o Torneio Mensal.", AlertSeverity.MEDIA, AlertType.INSTITUCIONAL, 980, 120, 210, "#ff5d5d", CardMaterial.METALICO, CardLayout.HERO, CardStyle.OURO, now.minusSeconds(2 * 3600));
        post(liga, PostContentType.INSTITUCIONAL, FeedWindow.ALERTA, "Suspensão de dois jogos após expulsão na rodada", "Decisão do tribunal desportivo · cabe recurso em 48 h.", AlertSeverity.MEDIA, AlertType.PUNICAO, 120, 30, 12, "#ff5d5d", CardMaterial.FOSCO, CardLayout.MINIMAL, CardStyle.GRAFITE, now.minusSeconds(5 * 3600));
        post(press, PostContentType.INSTITUCIONAL, FeedWindow.ALERTA, "Proposta oficial recebida por lateral sub-20 paulista", "Janela fecha em 9 dias · clube avalia contraproposta.", AlertSeverity.BAIXA, AlertType.TRANSFERENCIA, 210, 44, 31, "#ff5d5d", CardMaterial.VIDRO, CardLayout.SPLIT, CardStyle.NEON, now.minusSeconds(24 * 3600));
        post(c2, PostContentType.LIGA, FeedWindow.PERFIL, "Nova terceira camisa: vote até domingo no mural", "1.204 votos · aberta a sócios.", null, null, 1204, 88, 40, "#a06bff", CardMaterial.HOLOGRAFICO, CardLayout.HERO, CardStyle.VITRAL, now.minusSeconds(12 * 3600));
        log.info("Seed concluído: {} usuários, {} posts", users.count(), posts.count());
    }

    private <T extends User> T base(T u, String email, String username, String name, String hash, LocalDate birth, String state) {
        u.setEmail(email); u.setEmailHash(RedisSessionService.hash(email)); u.setPasswordHash(hash); u.setUsername(username); u.setDisplayName(name);
        u.setBirthDate(birth); u.setStateCode(state); u.setStatus(AccountStatus.ACTIVE); u.setCity("<PREENCHER_CIDADE>");
        return u;
    }

    private Club club(String email, String username, String name, int division, String state, String lat, String lng, String hash) {
        Club c = base(new Club(), email, username, name, hash, null, state);
        c.setLegalName(name); c.setClubeTipo(ClubeTipo.FORMADOR_CCF); c.setDivision(division); c.setLat(new BigDecimal(lat)); c.setLng(new BigDecimal(lng));
        c.setVerified(true); c.setVisibility(ProfileVisibility.PUBLICO); c.setFoundedYear(1950 + division * 7);
        return users.save(c);
    }

    private void finishUser(User u, Instant now, boolean shareMedical) {
        terms.save(new TermsAcceptance(u.getId(), props.terms().currentVersion(), now, "127.0.0.1", "seed"));
        UserLgpdPreferences p = new UserLgpdPreferences(u.getId());
        if (shareMedical) { p.set(LgpdPreference.SHARE_MEDICAL_DATA, true, now); p.set(LgpdPreference.SHARE_TELEMETRY_WITH_CLUBS, true, now); p.set(LgpdPreference.AI_AGENT_PROCESSING, true, now); }
        prefs.save(p);
    }

    /** 12 semanas de histórico convergindo para o score-alvo + temporada de partidas + treinos + scouting + avaliações. */
    private void seedHistory(Athlete at, DigitalTwin twin, int target, int idx, LocalDate today, String season) {
        int start = Math.max(120, target / 2);
        for (int w = 11; w >= 0; w--) {
            LocalDate d = today.minusWeeks(w + 1);
            int score = start + (target - start) * (11 - w) / 11;
            RarityTier tier = RarityTier.fromScore(score);
            BigDecimal f = BigDecimal.valueOf(score).divide(BigDecimal.valueOf(3000), 4, java.math.RoundingMode.HALF_EVEN);
            snapshots.save(new WeeklyScoreSnapshot(twin.getId(), IsoWeeks.of(d), f, f.add(new BigDecimal("0.05")).min(BigDecimal.ONE), f.subtract(new BigDecimal("0.03")).max(BigDecimal.ZERO), BigDecimal.ZERO, score, tier, "seed-" + w, Instant.now()));
            twin.applyWeeklyScore(score, IsoWeeks.of(d));
            TrainingSummary ts = new TrainingSummary(); ts.setAthleteId(at.getId()); ts.setIsoWeek(IsoWeeks.of(d)); ts.setSessionsPlanned(5); ts.setSessionsDone(Math.min(5, 3 + (w + idx) % 3));
            ts.setLoadAu(BigDecimal.valueOf(420 + ((w * 37 + idx * 11) % 160))); ts.setChronicLoadAu(BigDecimal.valueOf(470)); ts.setAvgRpe(new BigDecimal("6.5")); ts.setDistanceKm(new BigDecimal("31.2"));
            ts.setReadiness(idx == 3 ? 52 : 70 + (w + idx) % 25); training.save(ts);
            ScoutingAttribute sa = new ScoutingAttribute(); sa.setAthleteId(at.getId()); sa.setIsoWeek(IsoWeeks.of(d));
            int b = 50 + target / 60;
            sa.setVelocidade(BigDecimal.valueOf(Math.min(99, b + 12))); sa.setPasse(BigDecimal.valueOf(Math.min(99, b + 8))); sa.setFinalizacao(BigDecimal.valueOf(Math.min(99, b + (at.getPosition() == Position.ATACANTE ? 15 : 0))));
            sa.setDrible(BigDecimal.valueOf(Math.min(99, b + 5))); sa.setDesarme(BigDecimal.valueOf(Math.min(99, b + (at.getPosition() == Position.ZAGUEIRO ? 20 : -10)))); sa.setPosicionamento(BigDecimal.valueOf(Math.min(99, b + 3))); sa.setResistencia(BigDecimal.valueOf(Math.min(99, b + 6)));
            scouting.save(sa);
        }
        twins.save(twin);
        int games = 6 + idx * 4;
        for (int g = 0; g < games; g++) {
            MatchPerformance m = new MatchPerformance(); m.setAthleteId(at.getId()); m.setMatchDate(today.minusDays(3L + g * 7)); m.setCompetition("<PREENCHER_COMPETICAO>"); m.setOfficial(true);
            m.setMinutesPlayed(g % 5 == 0 ? 45 : 90); m.setRating(new BigDecimal(6.2 + ((g * 7 + idx) % 25) / 10.0).setScale(2, java.math.RoundingMode.HALF_EVEN)); m.setSeason(season);
            m.setGoals(at.getPosition() == Position.ATACANTE ? (g % 2) : at.getPosition() == Position.MEIA ? (g % 3 == 0 ? 1 : 0) : 0);
            m.setAssists(at.getPosition() == Position.MEIA || at.getPosition() == Position.LATERAL ? (g % 3 == 1 ? 1 : 0) : 0);
            m.setCleanSheet(at.getPosition() == Position.GOLEIRO && g % 4 == 0); m.setTackles(at.getPosition() == Position.ZAGUEIRO ? 3 : 1);
            m.setYellowCards(g % 7 == 0 ? 1 : 0); m.setMvp(idx == 2 && g % 4 == 0); matches.save(m);
        }
        TechnicalEvaluation e = new TechnicalEvaluation(); e.setAthleteId(at.getId()); e.setEvaluatorId(twin.getId()); e.setEvaluatorRole(UserRole.STAFF);
        e.setRating(new BigDecimal(Math.min(9.5, 5.5 + target / 700.0)).setScale(2, java.math.RoundingMode.HALF_EVEN)); e.setWeight(BigDecimal.ONE); e.setDimension(MetricType.TECNICA); e.setWeekOf(today.minusWeeks(1)); evaluations.save(e);
    }

    private void post(User author, PostContentType type, FeedWindow window, String title, String subtitle, AlertSeverity sev, AlertType at, int likes, int comments, int shares,
                      String color, CardMaterial material, CardLayout layout, CardStyle style, Instant publishedAt) {
        Post p = new Post();
        p.setAuthorId(author.getId()); p.setAuthorRole(author.getRole()); p.setContentType(type); p.setWindow(window); p.setTitle(title); p.setSubtitle(subtitle);
        p.setSeverity(sev); p.setAlertType(at); p.setStateCode(author.getStateCode()); p.setPublishedAt(publishedAt); p.setAiGenerated(author.getRole().isAutomated());
        p.setArt(new CardArt(color, material, layout, style)); p.setGenerationMode(GenerationMode.MANUAL);
        for (int i = 0; i < likes; i++) p.like(1); for (int i = 0; i < shares; i++) p.share();
        p.setTrendingScore(BigDecimal.valueOf(likes + shares * 3L)); p.setTrendingDelta(BigDecimal.valueOf(likes > 500 ? 1.5 : likes > 100 ? 0.3 : 0));
        posts.save(p);
    }
}
