package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.CardArtDTO;
import com.footballai.social.controller.dto.CopilotDtos.SuggestRequest;
import com.footballai.social.controller.dto.CopilotDtos.SuggestionDTO;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Copilot: sugere tipo (borda), título 5–12 palavras, teaser, badges e arte a partir dos dados do próprio usuário.
 * Regras determinísticas (sem LLM externo) — o humano sempre revisa antes de publicar. Nunca usa dado de saúde
 * sem shareMedicalData. Autopilot (rascunhos automáticos) exige aiAgentProcessing = true.
 */
@Service
public class CopilotService {
    private final UserRepository users;
    private final TrainingSummaryRepository training;
    private final MatchPerformanceRepository matches;
    private final DigitalTwinRepository twins;
    private final AthleteBadgeRepository badges;
    private final UserLgpdPreferencesRepository prefs;
    private final Clock clock = Clock.systemUTC();

    public CopilotService(UserRepository users, TrainingSummaryRepository training, MatchPerformanceRepository matches, DigitalTwinRepository twins,
                          AthleteBadgeRepository badges, UserLgpdPreferencesRepository prefs) {
        this.users = users; this.training = training; this.matches = matches; this.twins = twins; this.badges = badges; this.prefs = prefs;
    }

    public SuggestionDTO suggest(AuthPrincipal p, SuggestRequest r) {
        User u = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        PostContentType type = r.contentType() != null ? r.contentType() : inferType(u.getRole(), r.draft());
        FeedWindow window = r.window() != null ? r.window() : type.defaultWindow();
        RarityTier tier = twins.findByUserId(u.getId()).map(DigitalTwin::getRarityTier).orElse(RarityTier.COMMON);
        List<String> badgeLabels = badges.findAllByAthleteIdAndActiveTrue(u.getId()).stream().filter(b -> !b.getBadgeType().isPhysical())
                .limit(3).map(b -> BadgeAwardService.toDto(b).label()).toList();
        String draft = r.draft() == null ? "" : r.draft().trim();
        String title, subtitle, rationale;
        switch (type) {
            case TREINO -> {
                TrainingSummary ts = training.findFirstByAthleteIdOrderByIsoWeekDesc(u.getId()).orElse(null);
                if (ts != null) {
                    title = clampWords("Semana de treinos: " + ts.getSessionsDone() + " de " + ts.getSessionsPlanned() + " sessões concluídas");
                    subtitle = "Carga " + ts.getLoadAu().setScale(0, java.math.RoundingMode.HALF_EVEN) + " AU · prontidão " + ts.getReadiness() + "% · semana " + ts.getIsoWeek();
                    rationale = "Detectei o resumo de treinos da semana " + ts.getIsoWeek() + " (sem dados de saúde).";
                } else { title = clampWords(draft.isBlank() ? "Nova sessão de treino registrada no meu Gêmeo Digital" : draft); subtitle = "Telemetria registrada no Gêmeo Digital"; rationale = "Sem resumo de treinos recente — usei o rascunho."; }
            }
            case PARTIDA_MOMENTO -> {
                List<MatchPerformance> ms = matches.findAllByAthleteIdAndSeasonOrderByMatchDateAsc(u.getId(), IsoWeeks.season(LocalDate.now(clock)));
                MatchPerformance last = ms.isEmpty() ? null : ms.get(ms.size() - 1);
                if (last != null) {
                    title = clampWords(last.getMinutesPlayed() + " minutos, " + last.getGoals() + " gols e " + last.getAssists() + " assistências na última partida");
                    subtitle = (last.getCompetition() == null ? "Partida" : last.getCompetition()) + " · nota " + (last.getRating() == null ? "—" : last.getRating()) + (last.isMvp() ? " · 🏆 MVP" : "");
                    rationale = "Última partida registrada em " + last.getMatchDate() + ".";
                } else { title = clampWords(draft.isBlank() ? "Momento da partida registrado no meu Gêmeo Digital" : draft); subtitle = "Lance registrado como momento"; rationale = "Sem partida recente — usei o rascunho."; }
            }
            case RELATORIO_TATICO -> { title = clampWords(draft.isBlank() ? "Relatório tático da rodada com dados abertos do Gêmeo" : draft); subtitle = "Análise com PP, RP e MP das últimas 4 semanas"; rationale = "Scouting report: dados abertos, sem informação médica."; }
            case LESAO_ALERTA -> { title = clampWords(draft.isBlank() ? "Atualização médica com prazo de retorno estimado" : draft); subtitle = "Boletim do departamento médico"; rationale = "Dado sensível (Art. 11): age-chip SENSÍVEL obrigatório."; }
            case INSTITUCIONAL -> { title = clampWords(draft.isBlank() ? "Comunicado oficial publicado para elenco e torcida" : draft); subtitle = "Comunicado institucional"; rationale = "Janela ALERTA com listra de risco."; }
            case LIGA -> { title = clampWords(draft.isBlank() ? "Rodada da liga com tabela e convocações atualizadas" : draft); subtitle = "Comunidade da liga"; rationale = "Borda roxa ⬢ comunidade."; }
            default -> { title = clampWords(draft.isBlank() ? "Conquista especial registrada no card de perfil hoje" : draft); subtitle = "Momento especial ✦"; rationale = "Borda Elite Gold reservada a conquistas."; }
        }
        AgeRating rating = type.sensitive() ? AgeRating.SENSITIVE : AgeRating.SAFE;
        CardArtDTO art = suggestArt(type, tier);
        return new SuggestionDTO(type.name(), window.name(), title, subtitle, draft.isBlank() ? null : draft, badgeLabels, rating.name(), art,
                rationale + " Você revisa antes de publicar (LGPD Art. 20).", "copilot", true);
    }

    /** Arte sugerida: cor da borda do tipo, material pelo tier, layout pelo tipo, estilo pelo tier. */
    static CardArtDTO suggestArt(PostContentType type, RarityTier tier) {
        CardMaterial material = switch (tier) { case MYTHIC -> CardMaterial.HOLOGRAFICO; case LEGENDARY -> CardMaterial.METALICO; case EPIC -> CardMaterial.VIDRO; case RARE -> CardMaterial.CARBONO; default -> CardMaterial.FOSCO; };
        CardLayout layout = switch (type) { case PARTIDA_MOMENTO, ESPECIAL -> CardLayout.HERO; case RELATORIO_TATICO -> CardLayout.SPLIT; case INSTITUCIONAL, LESAO_ALERTA -> CardLayout.MINIMAL; default -> CardLayout.CLASSICO; };
        CardStyle style = switch (tier) { case MYTHIC, LEGENDARY -> CardStyle.OURO; case EPIC -> CardStyle.VITRAL; case RARE -> CardStyle.NEON; default -> CardStyle.GRAFITE; };
        return new CardArtDTO(type.borderColor(), material.name(), material.label(), layout.name(), layout.label(), style.name(), style.label());
    }

    static PostContentType inferType(UserRole role, String draft) {
        String d = draft == null ? "" : draft.toLowerCase(Locale.ROOT);
        if (d.contains("lesão") || d.contains("lesao") || d.contains("médic")) return role.canPublish(PostContentType.LESAO_ALERTA, VerificationLevel.VERIFICADO) ? PostContentType.LESAO_ALERTA : PostContentType.INSTITUCIONAL;
        if (d.contains("gol") || d.contains("partida") || d.contains("jogo")) return PostContentType.PARTIDA_MOMENTO;
        if (d.contains("relat") || d.contains("scout") || d.contains("tátic") || d.contains("tatic")) return PostContentType.RELATORIO_TATICO;
        if (d.contains("tabela") || d.contains("rodada") || d.contains("liga")) return PostContentType.LIGA;
        return switch (role) {
            case ATLETA, STAFF -> PostContentType.TREINO;
            case SCOUT, IMPRENSA, AGENTE_IA -> PostContentType.RELATORIO_TATICO;
            case CLUBE, MARCA, AGENTE, STAFF_SAUDE -> PostContentType.INSTITUCIONAL;
            case OFICIAL_LIGA -> PostContentType.LIGA;
            default -> PostContentType.PARTIDA_MOMENTO;
        };
    }

    /** Garante 5–12 palavras (regra do feed). */
    static String clampWords(String s) {
        List<String> w = new ArrayList<>(List.of(s.trim().split("\\s+")));
        w.removeIf(String::isBlank);
        while (w.size() < PostService.TITLE_MIN_WORDS) w.add(List.of("registrado", "no", "Gêmeo", "Digital", "hoje").get(w.size() % 5));
        if (w.size() > PostService.TITLE_MAX_WORDS) w = w.subList(0, PostService.TITLE_MAX_WORDS);
        String t = String.join(" ", w);
        return t.length() > 90 ? t.substring(0, 87) + "…" : t;
    }

    /** Autopilot: rascunhos privados a partir de fontes opt-in (resumo de treinos, súmula). Nunca saúde sem consentimento. */
    public List<SuggestionDTO> autopilotDrafts(AuthPrincipal p) {
        UserLgpdPreferences pr = prefs.findById(p.userId()).orElse(null);
        if (pr == null || !pr.isAiAgentProcessing()) throw ApiException.forbidden("AI_PROCESSING_OPT_IN", "Ative 'Processamento por agentes de IA' na Central de Privacidade para receber rascunhos");
        User u = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        if (u.isMinor()) throw ApiException.forbidden("AUTOPILOT_MINOR", "Autopilot desabilitado para menores de 18");
        List<SuggestionDTO> out = new ArrayList<>();
        SuggestionDTO t = suggest(p, new SuggestRequest(null, PostContentType.TREINO, null));
        out.add(new SuggestionDTO(t.contentType(), t.window(), t.title(), t.subtitle(), t.content(), t.badges(), t.ageRating(), t.art(), "Fonte: wearable/resumo semanal · sem dados de saúde", "autopilot", true));
        if (!matches.findAllByAthleteIdAndSeasonOrderByMatchDateAsc(u.getId(), IsoWeeks.season(LocalDate.now(clock))).isEmpty()) {
            SuggestionDTO m = suggest(p, new SuggestRequest(null, PostContentType.PARTIDA_MOMENTO, null));
            out.add(new SuggestionDTO(m.contentType(), m.window(), m.title(), m.subtitle(), m.content(), m.badges(), m.ageRating(), m.art(), "Fonte: súmula oficial", "autopilot", true));
        }
        return out;
    }

    public UUID idOf(AuthPrincipal p) { return p.userId(); }
}
