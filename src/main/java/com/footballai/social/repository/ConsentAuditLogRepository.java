package com.footballai.social.repository;

import com.footballai.social.model.entity.ConsentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsentAuditLogRepository extends JpaRepository<ConsentAuditLog, UUID> {
    List<ConsentAuditLog> findAllByUserIdOrderByChangedAtDesc(UUID userId);
}
