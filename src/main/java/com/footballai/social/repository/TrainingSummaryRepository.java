package com.footballai.social.repository;

import com.footballai.social.model.entity.TrainingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSummaryRepository extends JpaRepository<TrainingSummary, UUID> {
    Optional<TrainingSummary> findByAthleteIdAndIsoWeek(UUID athleteId, String isoWeek);
    Optional<TrainingSummary> findFirstByAthleteIdOrderByIsoWeekDesc(UUID athleteId);
    List<TrainingSummary> findAllByAthleteIdOrderByIsoWeekDesc(UUID athleteId);
}
