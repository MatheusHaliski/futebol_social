package com.footballai.social.repository;

import com.footballai.social.model.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerIdAndTargetId(UUID followerId, UUID targetId);
    boolean existsByFollowerIdAndTargetId(UUID followerId, UUID targetId);
    List<Follow> findAllByFollowerIdOrderBySinceDesc(UUID followerId);
    List<Follow> findAllByTargetIdOrderBySinceDesc(UUID targetId);
    long countByTargetId(UUID targetId);
    long countByFollowerId(UUID followerId);

    @Query("select f.targetId from Follow f where f.followerId = :followerId")
    List<UUID> findTargetIds(@Param("followerId") UUID followerId);

    @Query("select f.followerId from Follow f where f.targetId = :targetId")
    List<UUID> findFollowerIds(@Param("targetId") UUID targetId);

    /** Conexões = seguir mútuo: quem o usuário segue e que o segue de volta. */
    @Query("select f.targetId from Follow f where f.followerId = :userId and exists (select 1 from Follow g where g.followerId = f.targetId and g.targetId = :userId)")
    List<UUID> findConnectionIds(@Param("userId") UUID userId);

    void deleteAllByFollowerIdOrTargetId(UUID followerId, UUID targetId);
}
