package com.footballai.social.repository;

import com.footballai.social.model.entity.DataExportRequest;
import com.footballai.social.model.enums.ExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DataExportRequestRepository extends JpaRepository<DataExportRequest, UUID> {
    List<DataExportRequest> findAllByUserIdOrderByRequestedAtDesc(UUID userId);
    List<DataExportRequest> findAllByStatusAndExpiresAtBefore(ExportStatus status, Instant now);
}
