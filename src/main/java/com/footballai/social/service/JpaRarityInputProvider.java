package com.footballai.social.service;

import com.footballai.social.model.entity.MatchPerformance;
import com.footballai.social.model.entity.TechnicalEvaluation;
import com.footballai.social.model.entity.TrainingSummary;
import com.footballai.social.repository.AthleteBadgeRepository;
import com.footballai.social.repository.MatchPerformanceRepository;
import com.footballai.social.repository.TechnicalEvaluationRepository;
import com.footballai.social.repository.TrainingSummaryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Agrega os insumos da semana a partir de MySQL (resumo de treinos, avaliações, partidas e badges). */
@Component
public class JpaRarityInputProvider implements RarityInputProvider {
    private final TrainingSummaryRepository training;
    private final TechnicalEvaluationRepository evaluations;
    private final MatchPerformanceRepository matches;
    private final AthleteBadgeRepository badges;
    private final BadgeAwardService badgeService;

    public JpaRarityInputProvider(TrainingSummaryRepository training, TechnicalEvaluationRepository evaluations, MatchPerformanceRepository matches,
                                  AthleteBadgeRepository badges, BadgeAwardService badgeService) {
        this.training = training; this.evaluations = evaluations; this.matches = matches; this.badges = badges; this.badgeService = badgeService;
    }

    @Override
    public RarityEngineService.RarityInputs inputsFor(UUID athleteId, String isoWeek) {
        LocalDate[] r = IsoWeeks.range(isoWeek);
        TrainingSummary ts = training.findByAthleteIdAndIsoWeek(athleteId, isoWeek).orElse(null);
        List<TechnicalEvaluation> evs = evaluations.findAllByAthleteIdAndWeekOfBetween(athleteId, r[0], r[1]).stream().filter(TechnicalEvaluation::isCountsForRarity).toList();
        BigDecimal wsum = BigDecimal.ZERO, wrating = BigDecimal.ZERO;
        for (TechnicalEvaluation e : evs) { wsum = wsum.add(e.getWeight()); wrating = wrating.add(e.getRating().multiply(e.getWeight())); }
        List<MatchPerformance> ms = matches.findAllByAthleteIdAndMatchDateBetween(athleteId, r[0], r[1]);
        int minutes = 0, contrib = 0; BigDecimal ratingSum = BigDecimal.ZERO; int rated = 0;
        for (MatchPerformance m : ms) {
            minutes += m.getMinutesPlayed(); contrib += m.getGoals() + m.getAssists();
            if (m.getRating() != null) { ratingSum = ratingSum.add(m.getRating()); rated++; }
        }
        BigDecimal avgRating = rated == 0 ? BigDecimal.ZERO : ratingSum.divide(BigDecimal.valueOf(rated), 4, RoundingMode.HALF_EVEN);
        BigDecimal bonus = badgeService.badgeBonus(badges.findAllByAthleteIdAndActiveTrue(athleteId));
        return new RarityEngineService.RarityInputs(isoWeek,
                ts == null ? 0 : ts.getSessionsPlanned(), ts == null ? 0 : ts.getSessionsDone(), ts == null ? BigDecimal.ONE : ts.acwr(), ts == null ? 0 : ts.getReadiness(),
                wrating, wsum, ms.size(), minutes, avgRating, contrib, bonus);
    }
}
