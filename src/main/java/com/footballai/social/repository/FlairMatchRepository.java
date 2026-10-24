package com.footballai.social.repository;

import com.footballai.social.model.entity.FlairMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FlairMatchRepository extends JpaRepository<FlairMatch, UUID> {
    long countByChallengerIdAndCreatedAtAfter(UUID challengerId, Instant after);
    List<FlairMatch> findTop10ByChallengerIdOrderByCreatedAtDesc(UUID challengerId);
}
