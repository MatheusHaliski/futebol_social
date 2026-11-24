package com.footballai.social.controller.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Força do clube (score global + score por setor: goleiro, defesa, meio-campo, ataque) e a simulação de contratação da Vitrine.
 * Regras em docs/CLUB_STRENGTH.md.
 */
public final class ClubStrengthDtos {
    private ClubStrengthDtos() { }

    /** Titular escolhido no melhor onze (só perfis públicos entram no cálculo). rating = nota do jogador no setor (0–100). */
    public record StarterDTO(UUID athleteId, String handle, String name, String position, int rating) { }

    /** Setor: score 0–100 = soma das notas dos titulares ÷ titulares de referência (1 · 4 · 3 · 3), limitado a 100. depth = jogadores avaliados no setor. */
    public record SectorDTO(String key, String label, int score, int starters, int reference, int depth, List<StarterDTO> lineup) { }

    /** Score global 0–100 = média ponderada dos setores (goleiro 10 % · defesa, meio-campo e ataque 30 % cada) no melhor onze entre as formações. */
    public record ClubStrengthDTO(UUID clubId, String clubName, int globalScore, String formation, List<SectorDTO> sectors,
                                  int squadSize, int ratedPlayers, List<String> notes, String algorithm, Instant calculatedAt) { }

    /** Variação de um setor na simulação; reason explica a mudança (entrada do atleta, troca de formação). */
    public record SectorDeltaDTO(String key, String label, int before, int after, int delta, String trend, String reason) { }

    /**
     * "Simular contratação": o clube antes e depois de receber o atleta. increases/decreases listam só os setores que mudam.
     * status: SIMULATED · INSUFFICIENT_DATA (atleta sem posição ou sem scouting).
     */
    public record SimulationDTO(UUID athleteId, UUID clubId, String clubName, String status, String athleteSector, Integer athleteRating,
                                String role, StarterDTO displaced, ClubStrengthDTO before, ClubStrengthDTO after, int globalDelta,
                                List<SectorDeltaDTO> sectors, List<SectorDeltaDTO> increases, List<SectorDeltaDTO> decreases,
                                String summary, String disclaimer) { }
}
