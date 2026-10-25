package com.footballai.social.repository;

import com.footballai.social.model.entity.MatchPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MatchPerformanceRepository extends JpaRepository<MatchPerformance, UUID> {
    List<MatchPerformance> findAllByAthleteIdAndMatchDateBetween(UUID athleteId, LocalDate from, LocalDate to);
    List<MatchPerformance> findAllByAthleteIdAndSeasonOrderByMatchDateAsc(UUID athleteId, String season);
    List<MatchPerformance> findAllByAthleteId(UUID athleteId);
}
