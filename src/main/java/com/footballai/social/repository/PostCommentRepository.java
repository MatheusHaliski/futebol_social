package com.footballai.social.repository;

import com.footballai.social.model.entity.PostComment;
import com.footballai.social.model.enums.CommentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
    /** Página por cursor de tempo (mais antigos primeiro, como numa conversa). */
    List<PostComment> findAllByPostIdAndStatusAndCreatedAtAfterOrderByCreatedAtAsc(UUID postId, CommentStatus status, Instant after, Pageable page);
    long countByPostIdAndStatus(UUID postId, CommentStatus status);
    List<PostComment> findAllByAuthorIdOrderByCreatedAtDesc(UUID authorId);
    List<PostComment> findAllByAuthorIdAndStatus(UUID authorId, CommentStatus status);
    List<PostComment> findAllByParentIdAndStatus(UUID parentId, CommentStatus status);
}
