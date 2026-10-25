package com.footballai.social.controller.dto;

import com.footballai.social.controller.dto.CardDtos.AuthorDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Camada social (RF3): seguir, comentários e notificações. */
public final class SocialDtos {
    private SocialDtos() { }

    /** Estado de "seguir" entre o usuário autenticado e um alvo. */
    public record FollowDTO(UUID targetId, String targetType, boolean following, boolean followsMe, boolean connection, long followers, long followingCount) { }
    public record FollowSummaryDTO(AuthorDTO user, String targetType, Instant since, boolean mutual) { }

    public record CreateCommentRequest(@NotBlank @Size(max = 500) String body, UUID parentId) { }
    public record CommentDTO(UUID id, UUID postId, UUID parentId, AuthorDTO author, String body, Instant createdAt, String timeLabel, boolean mine, boolean canDelete) { }
    public record CommentPageDTO(UUID postId, List<CommentDTO> items, String nextCursor, long total) { }

    public record NotificationDTO(UUID id, String type, String glyph, String title, String body, AuthorDTO actor, String refType, UUID refId,
                                  Instant createdAt, String timeLabel, boolean unread) { }
    public record NotificationPageDTO(List<NotificationDTO> items, long unreadCount) { }
    public record MarkReadRequest(List<UUID> ids, Boolean all) { }
}
