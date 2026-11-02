package com.footballai.social.repository;

import com.footballai.social.model.entity.ScoutingAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoutingAttributeRepository extends JpaRepository<ScoutingAttribute, UUID> {
    Optional<ScoutingAttribute> findFirstByAthleteIdOrderByIsoWeekDesc(UUID athleteId);
    /** Últimas 12 semanas (mais recente primeiro): tendência dos atributos na Vitrine. */
    List<ScoutingAttribute> findTop12ByAthleteIdOrderByIsoWeekDesc(UUID athleteId);
}
