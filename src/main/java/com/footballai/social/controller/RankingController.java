package com.footballai.social.controller;

import com.footballai.social.model.enums.Position;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.service.RankingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Rankings semanais (jogadores e clubes) e clubes para o globo Explorar. */
@RestController
@RequestMapping("/v1")
public class RankingController {
    private final RankingService ranking;
    public RankingController(RankingService ranking) { this.ranking = ranking; }

    @GetMapping("/rankings/athletes")
    public List<RankingService.AthleteRow> athletes(@RequestParam(required = false) Position position, @RequestParam(required = false) String state,
                                                    @RequestParam(required = false) RarityTier minTier, @RequestParam(defaultValue = "20") int limit) {
        return ranking.athletes(position, state, minTier, Math.max(1, Math.min(100, limit)));
    }

    @GetMapping("/rankings/clubs")
    public List<RankingService.ClubRow> clubRanking(@RequestParam(required = false) String country, @RequestParam(required = false) Integer division, @RequestParam(required = false) RarityTier minTier) {
        return ranking.clubs(country, division, minTier);
    }

    @GetMapping("/clubs")
    public List<RankingService.ClubRow> clubs(@RequestParam(required = false) String country, @RequestParam(required = false) Integer division, @RequestParam(required = false) RarityTier minTier) {
        return ranking.clubs(country, division, minTier);
    }
}
