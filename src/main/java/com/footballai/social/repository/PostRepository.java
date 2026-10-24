package com.footballai.social.repository;

import com.footballai.social.model.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    List<Post> findAllByAuthorId(UUID authorId);
    List<Post> findAllByIdIn(Collection<UUID> ids);
}
