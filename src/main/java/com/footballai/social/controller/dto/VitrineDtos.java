package com.footballai.social.controller.dto;

import com.footballai.social.controller.dto.CardDtos.AthleteCardDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Aba Vitrine do Jogador: painel dinâmico (histórico de partidas, gols, assistências, passes e dribles certos; atributos do
 * momento) e o Copilot que responde "Por que contratar este jogador?". Contrato REST ↔ SPA (docs/VITRINE.md).
 */
public final class VitrineDtos {
    private VitrineDtos() { }

    /**
     * Totais de um conjunto de partidas. Aproveitamentos (%) e médias por 90 min são {@code null} quando não há base
     * (nenhuma tentativa registrada, nenhum minuto jogado) — nunca 0 % por falta de dado.
     */
    public record TotalsDTO(int games, int officialGames, int minutes, int goals, int assists,
                            int passesCompleted, int passesAttempted, BigDecimal passAccuracy,
                            int dribblesCompleted, int dribblesAttempted, BigDecimal dribbleSuccess,
                            int tackles, int cleanSheets, int mvps, int yellow, int red, BigDecimal avgRating,
                            BigDecimal goalsPer90, BigDecimal assistsPer90, BigDecimal tacklesPer90) { }

    /** Linha do histórico por temporada (mais recente primeiro). */
    public record SeasonLineDTO(String season, TotalsDTO totals) { }

    /** Partida do histórico recente (mais recente primeiro). {@code official = false} = autorrelato do atleta. */
    public record MatchLineDTO(LocalDate date, String competition, boolean official, int minutes, int goals, int assists,
                               int passesCompleted, int passesAttempted, int dribblesCompleted, int dribblesAttempted,
                               BigDecimal rating, boolean mvp) { }

    /**
     * Forma: nota média das últimas partidas vs. a média das partidas anteriores. trend: {@code UP · FLAT · DOWN};
     * lastRatings em ordem cronológica (sparkline).
     */
    public record FormDTO(String trend, String label, BigDecimal recentAvgRating, BigDecimal referenceAvgRating, int sample, List<BigDecimal> lastRatings) { }

    /**
     * Atributo do momento (0–100) = valor do scouting da semana mais recente ({@code base}) + ajuste de forma das últimas
     * partidas ({@code formAdjustment}, ±5, só onde há evidência na súmula). {@code reason} explica o número; {@code history}
     * traz até 12 semanas em ordem cronológica.
     */
    public record LiveAttributeDTO(String key, String label, int value, int base, Integer previous, int weeklyDelta, int formAdjustment,
                                   String trend, String reason, List<Integer> history) { }

    /** Copilot disponível para este atleta? Quando não, o código estável e o motivo (consentimento de IA do atleta, menor de idade). */
    public record CopilotStatusDTO(boolean available, String code, String reason) { }

    /**
     * Painel da Vitrine. {@code revision} muda quando os dados mudam (nova partida, nova semana de scouting, novo score):
     * a SPA consulta a cada {@code refreshSeconds} e só redesenha — e avisa que a resposta do Copilot ficou desatualizada — quando ela muda.
     */
    public record VitrineDTO(UUID athleteId, AthleteCardDTO card, TotalsDTO career, TotalsDTO season, String seasonLabel, TotalsDTO recent,
                             List<SeasonLineDTO> seasons, List<MatchLineDTO> recentMatches, FormDTO form,
                             List<LiveAttributeDTO> attributes, String attributesWeek, CopilotStatusDTO copilot,
                             String revision, Instant updatedAt, int refreshSeconds) { }

    /** Um argumento do Copilot: sempre com a evidência numérica que o sustenta. */
    public record ReasonDTO(String icon, String title, String evidence, String category) { }

    /**
     * Resposta do Copilot à pergunta "Por que contratar este jogador?". Determinística (regras sobre os dados abertos do Gêmeo,
     * sem LLM externo), rotulada como gerada por IA e sem dados de saúde (LGPD Art. 11); a decisão é humana (Art. 20).
     * O tier vai como brasão ({@code crestIcon}), nunca em texto.
     */
    public record PitchDTO(UUID athleteId, String question, String headline, String answer, List<ReasonDTO> reasons, List<String> attention,
                           List<String> bestFit, String confidence, String confidenceLabel, List<String> sources, String tier, String crestIcon,
                           String revision, String disclaimer, String source, boolean aiGenerated, Instant generatedAt) { }
}
