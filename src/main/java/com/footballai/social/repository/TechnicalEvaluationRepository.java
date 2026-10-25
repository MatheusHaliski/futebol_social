package com.footballai.social.repository;

import com.footballai.social.model.entity.TechnicalEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TechnicalEvaluationRepository extends JpaRepository<TechnicalEvaluation, UUID> {
    List<TechnicalEvaluation> findAllByAthleteIdAndWeekOfBetween(UUID athleteId, LocalDate from, LocalDate to);
    List<TechnicalEvaluation> findAllByAthleteId(UUID athleteId);
}
