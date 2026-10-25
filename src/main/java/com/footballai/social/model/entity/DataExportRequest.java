package com.footballai.social.model.entity;

import com.footballai.social.model.enums.ExportStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Pedido de portabilidade (Art. 18, V): JSON gerado para S3 com link válido por 7 dias. */
@Entity
@Table(name = "data_export_requests")
public class DataExportRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private ExportStatus status = ExportStatus.REQUESTED;
    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Column(name = "ready_at") private Instant readyAt;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "s3_object_key") private String s3ObjectKey;
    @Column(name = "size_bytes") private Long sizeBytes;
    @Column(name = "failure_reason", length = 255) private String failureReason;
    @Column(nullable = false) private int attempts = 0;

    protected DataExportRequest() { }
    public DataExportRequest(UUID userId, Instant requestedAt) { this.userId = userId; this.requestedAt = requestedAt; }

    public void markReady(String key, long sizeBytes, Instant now, int ttlDays) {
        this.status = ExportStatus.READY; this.s3ObjectKey = key; this.sizeBytes = sizeBytes; this.readyAt = now;
        this.expiresAt = now.plus(ttlDays, java.time.temporal.ChronoUnit.DAYS);
    }
    public void markFailed(String reason) { this.status = ExportStatus.FAILED; this.failureReason = reason; this.attempts++; }
    public void markExpired() { this.status = ExportStatus.EXPIRED; }
    public boolean isExpired(Instant now) { return expiresAt != null && now.isAfter(expiresAt); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public ExportStatus getStatus() { return status; }
    public void setStatus(ExportStatus status) { this.status = status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReadyAt() { return readyAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getS3ObjectKey() { return s3ObjectKey; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getFailureReason() { return failureReason; }
    public int getAttempts() { return attempts; }
}
