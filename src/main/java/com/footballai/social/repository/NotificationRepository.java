package com.footballai.social.repository;

import com.footballai.social.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Notification> findTop50ByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndReadAtIsNull(UUID userId);
    List<Notification> findAllByUserIdAndIdIn(UUID userId, List<UUID> ids);
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);

    void deleteAllByUserId(UUID userId);
    List<Notification> findAllByActorId(UUID actorId);
}
