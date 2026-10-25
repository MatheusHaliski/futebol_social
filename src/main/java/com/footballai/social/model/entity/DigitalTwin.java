package com.footballai.social.model.entity;

import com.footballai.social.model.enums.PhysicalStatus;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.model.enums.RewardTier;
import com.footballai.social.model.enums.TwinStatus;
import com.footballai.social.model.InvalidTwinTransitionException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Gêmeo Digital do atleta (RF4): status (máquina de estados), score semanal e tier de raridade. */
@Entity
@Table(name = "digital_twins")
public class DigitalTwin {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "BINARY(16)") private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private TwinStatus status = TwinStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(name = "physical_status", nullable = false, length = 16) private PhysicalStatus physicalStatus = PhysicalStatus.READY;
    @Column(name = "score_total", nullable = false) private int scoreTotal = 0;
    @Enumerated(EnumType.STRING) @Column(name = "rarity_tier", nullable = false, length = 12) private RarityTier rarityTier = RarityTier.COMMON;
    @Enumerated(EnumType.STRING) @Column(name = "reward_tier", nullable = false, length = 12) private RewardTier rewardTier = RewardTier.BRONZE;
    @Column(name = "last_calculated_week", length = 8) private String lastCalculatedWeek;
    @Column(name = "activated_at") private Instant activatedAt;
    @Column(name = "suspended_at") private Instant suspendedAt;

    protected DigitalTwin() { }
    public DigitalTwin(UUID userId) { this.userId = userId; }

    /** Transição validada pela máquina de estados; lança {@link InvalidTwinTransitionException}. */
    public void transitionTo(TwinStatus next, Instant now) {
        if (!status.canTransitionTo(next)) throw new InvalidTwinTransitionException(status, next);
        this.status = next;
        if (next == TwinStatus.ACTIVE && activatedAt == null) activatedAt = now;
        if (next == TwinStatus.SUSPENDED) suspendedAt = now;
    }

    public void applyWeeklyScore(int scoreTotal, String isoWeek) {
        this.scoreTotal = scoreTotal;
        this.rarityTier = RarityTier.fromScore(scoreTotal);
        this.lastCalculatedWeek = isoWeek;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public TwinStatus getStatus() { return status; }
    public PhysicalStatus getPhysicalStatus() { return physicalStatus; }
    public void setPhysicalStatus(PhysicalStatus physicalStatus) { this.physicalStatus = physicalStatus; }
    public int getScoreTotal() { return scoreTotal; }
    public RarityTier getRarityTier() { return rarityTier; }
    public RewardTier getRewardTier() { return rewardTier; }
    public void setRewardTier(RewardTier rewardTier) { this.rewardTier = rewardTier; }
    public String getLastCalculatedWeek() { return lastCalculatedWeek; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getSuspendedAt() { return suspendedAt; }
}
