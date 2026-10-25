package com.footballai.social.repository;

import com.footballai.social.model.entity.ScoutingAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScoutingAttributeRepository extends JpaRepository<ScoutingAttribute, UUID> {
    Optional<ScoutingAttribute> findFirstByAthleteIdOrderByIsoWeekDesc(UUID athleteId);
}
