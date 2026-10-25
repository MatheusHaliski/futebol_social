package com.footballai.social.repository;

import com.footballai.social.model.entity.BadgeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BadgeAuditLogRepository extends JpaRepository<BadgeAuditLog, UUID> { }
