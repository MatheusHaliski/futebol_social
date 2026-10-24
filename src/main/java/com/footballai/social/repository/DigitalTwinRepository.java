package com.footballai.social.repository;

import com.footballai.social.model.entity.DigitalTwin;
import com.footballai.social.model.enums.TwinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DigitalTwinRepository extends JpaRepository<DigitalTwin, UUID> {
    Optional<DigitalTwin> findByUserId(UUID userId);
    List<DigitalTwin> findAllByStatus(TwinStatus status);
    List<DigitalTwin> findAllByUserIdIn(java.util.Collection<UUID> userIds);
    long countByStatus(TwinStatus status);
    long countByStatusAndScoreTotalGreaterThan(TwinStatus status, int score);
    @Query("select t from DigitalTwin t where t.status = com.footballai.social.model.enums.TwinStatus.ACTIVE order by t.scoreTotal desc")
    List<DigitalTwin> ranking(org.springframework.data.domain.Pageable pageable);
}
