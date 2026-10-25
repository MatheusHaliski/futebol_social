package com.footballai.social.repository;

import com.footballai.social.model.entity.FlairCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FlairCardRepository extends JpaRepository<FlairCard, UUID> {
    List<FlairCard> findAllByOwnerIdOrderByScoreTotalDesc(UUID ownerId);
    long countByOwnerIdAndSourceAndAcquiredAtAfter(UUID ownerId, String source, Instant after);
}
