package com.footballai.social.repository;

import com.footballai.social.model.entity.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostReactionRepository extends JpaRepository<PostReaction, UUID> {
    Optional<PostReaction> findByPostIdAndUserIdAndKind(UUID postId, UUID userId, PostReaction.Kind kind);
    List<PostReaction> findAllByUserIdAndPostIdIn(UUID userId, Collection<UUID> postIds);
    void deleteAllByUserId(UUID userId);
}
