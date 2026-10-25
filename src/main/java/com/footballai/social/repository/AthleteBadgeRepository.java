package com.footballai.social.repository;

import com.footballai.social.model.entity.AthleteBadge;
import com.footballai.social.model.enums.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AthleteBadgeRepository extends JpaRepository<AthleteBadge, UUID> {
    List<AthleteBadge> findAllByAthleteIdAndActiveTrue(UUID athleteId);
    List<AthleteBadge> findAllByAthleteId(UUID athleteId);
    List<AthleteBadge> findAllByAthleteIdAndBadgeTypeOrderByAwardedAtDesc(UUID athleteId, BadgeType badgeType);
    Optional<AthleteBadge> findByAthleteIdAndBadgeTypeAndSeason(UUID athleteId, BadgeType badgeType, String season);
    List<AthleteBadge> findAllByAthleteIdInAndActiveTrue(java.util.Collection<UUID> athleteIds);
}
