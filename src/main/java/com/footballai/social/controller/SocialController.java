package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.SocialDtos.*;
import com.footballai.social.service.CommentService;
import com.footballai.social.service.FollowService;
import com.footballai.social.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** RF3 — camada social: seguir, comentários em cards e notificações pessoais. */
@RestController
@RequestMapping("/v1")
public class SocialController {

    private final FollowService follows;
    private final CommentService comments;
    private final NotificationService notifications;

    public SocialController(FollowService follows, CommentService comments, NotificationService notifications) {
        this.follows = follows; this.comments = comments; this.notifications = notifications;
    }

    /* ---------- seguir ---------- */

    @PostMapping("/users/{id}/follow")
    public FollowDTO toggleFollow(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return follows.toggle(p, id); }

    @GetMapping("/users/{id}/follow")
    public FollowDTO followState(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return follows.state(p, id); }

    @GetMapping("/me/following")
    public List<FollowSummaryDTO> following(@AuthenticationPrincipal AuthPrincipal p) { return follows.following(p); }

    @GetMapping("/me/followers")
    public List<FollowSummaryDTO> followers(@AuthenticationPrincipal AuthPrincipal p) { return follows.followers(p); }

    /* ---------- comentários ---------- */

    @GetMapping("/posts/{id}/comments")
    public CommentPageDTO comments(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id,
                                   @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
        return comments.list(p, id, cursor, size);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDTO comment(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @Valid @RequestBody CreateCommentRequest r) { return comments.create(p, id, r); }

    @DeleteMapping("/posts/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @PathVariable UUID commentId) { comments.delete(p, id, commentId); }

    /* ---------- notificações ---------- */

    @GetMapping("/me/notifications")
    public NotificationPageDTO notifications(@AuthenticationPrincipal AuthPrincipal p, @RequestParam(defaultValue = "false") boolean unread) { return notifications.list(p, unread); }

    @GetMapping("/me/notifications/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthPrincipal p) { return Map.of("unread", notifications.unreadCount(p.userId())); }

    @PostMapping("/me/notifications/read")
    public NotificationPageDTO markRead(@AuthenticationPrincipal AuthPrincipal p, @RequestBody(required = false) MarkReadRequest r) { return notifications.markRead(p, r); }
}
