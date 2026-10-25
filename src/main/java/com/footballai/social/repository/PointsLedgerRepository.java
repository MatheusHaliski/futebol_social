package com.footballai.social.repository;

import com.footballai.social.model.entity.PointsLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, UUID> {
    @Query("select coalesce(sum(p.delta), 0) from PointsLedger p where p.userId = :userId")
    long balance(UUID userId);
    List<PointsLedger> findTop20ByUserIdOrderByAtDesc(UUID userId);
    void deleteAllByUserId(UUID userId);
}
