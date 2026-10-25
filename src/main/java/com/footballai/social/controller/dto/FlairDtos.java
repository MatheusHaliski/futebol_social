package com.footballai.social.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** RF6 — Modo FLAIR: cartas dos Gêmeos, escalação, duelo 1×1 por eixo e pontos. Sem dinheiro real. */
public final class FlairDtos {
    private FlairDtos() { }
    public record FlairCardDTO(UUID id, UUID athleteId, String name, String position, String tier, String crestIcon, String color, int pp, int rp, int mp,
                               int scoreTotal, int power, boolean captain, String source, Instant acquiredAt) { }
    public record CollectionDTO(List<FlairCardDTO> cards, DeckDTO deck, long points, boolean freePackAvailable, Map<String, String> packOdds, int matchesToday, int dailyMatchLimit) { }
    public record DeckDTO(List<FlairCardDTO> cards, int captainIndex, boolean complete) { }
    public record SetDeckRequest(@NotNull List<UUID> cardIds, @Min(0) @Max(2) int captainIndex) { }
    public record PackDTO(List<FlairCardDTO> cards, Map<String, String> odds, String nextFreePackAt) { }
    public record StartMatchRequest(UUID opponentId) { }
    public record RoundRequest(@NotNull @Pattern(regexp = "PP|RP|MP") String axis, boolean snap) { }
    public record RoundDTO(int round, String axis, String myCard, int myValue, String theirCard, int theirValue, String winner, boolean snap) { }
    public record MatchDTO(UUID id, String status, String opponent, int round, int myWins, int theirWins, boolean snapUsed, int pointsAwarded,
                           List<RoundDTO> rounds, List<FlairCardDTO> myDeck, List<FlairCardDTO> theirDeck, String result) { }
    public record PointsDTO(long balance, List<Map<String, Object>> history) { }
}
