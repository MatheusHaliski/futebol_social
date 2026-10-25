package com.footballai.social.repository;

import com.footballai.social.model.entity.WeeklyScoreSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyScoreSnapshotRepository extends JpaRepository<WeeklyScoreSnapshot, UUID> {
    Optional<WeeklyScoreSnapshot> findByTwinIdAndIsoWeek(UUID twinId, String isoWeek);
    List<WeeklyScoreSnapshot> findTop12ByTwinIdOrderByIsoWeekDesc(UUID twinId);
    List<WeeklyScoreSnapshot> findAllByTwinIdOrderByIsoWeekAsc(UUID twinId);
}
