package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.SocialDtos.MarkReadRequest;
import com.footballai.social.controller.dto.SocialDtos.NotificationPageDTO;
import com.footballai.social.model.entity.Notification;
import com.footballai.social.model.enums.NotificationType;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.NotificationRepository;
import com.footballai.social.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** RF3 — notificações: sem autonotificação, corpo truncado, marcar lidas (todas ou por id). */
class NotificationServiceTest {
    private final Instant now = Instant.parse("2026-10-25T12:00:00Z");
    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationService svc = new NotificationService(repo, users, Clock.fixed(now, ZoneOffset.UTC));
    private final UUID me = UUID.randomUUID();

    @Test
    void ownActionsDoNotNotifyAndBodyIsTruncated() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(svc.notify(me, NotificationType.LIKE, me, "POST", UUID.randomUUID(), "eu mesmo", null)).isEmpty();
        verify(repo, never()).save(any());
        String longBody = "x".repeat(400);
        Notification n = svc.notify(me, NotificationType.COMMENT, UUID.randomUUID(), "POST", UUID.randomUUID(), "t", longBody).orElseThrow();
        assertThat(n.getBody()).hasSize(NotificationService.MAX_BODY).endsWith("…");
        assertThat(n.isUnread()).isTrue();
        assertThat(n.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void markReadAllUsesBulkUpdateAndByIdsTouchesOnlyMine() {
        AuthPrincipal p = new AuthPrincipal(me, UserRole.ATLETA, "s", "j", Instant.MAX);
        when(repo.findTop50ByUserIdOrderByCreatedAtDesc(me)).thenReturn(List.of());
        svc.markRead(p, new MarkReadRequest(null, true));
        verify(repo).markAllRead(me, now);

        Notification mine = new Notification(me, NotificationType.FOLLOW, null, "USER", UUID.randomUUID(), "t", null, now.minusSeconds(60));
        UUID id = UUID.randomUUID();
        when(repo.findAllByUserIdAndIdIn(me, List.of(id))).thenReturn(List.of(mine));
        when(repo.countByUserIdAndReadAtIsNull(me)).thenReturn(0L);
        NotificationPageDTO page = svc.markRead(p, new MarkReadRequest(List.of(id), false));
        assertThat(mine.getReadAt()).isEqualTo(now);
        assertThat(page.unreadCount()).isZero();
        verify(repo, times(1)).markAllRead(eq(me), any());
    }

    @Test
    void forgetDeletesInboxAndScrubsActorSnapshotsInOthersInboxes() {
        UUID actor = UUID.randomUUID(), other = UUID.randomUUID();
        Notification c = new Notification(other, NotificationType.COMMENT, actor, "POST", UUID.randomUUID(), "@fulano comentou no seu card", "\"texto do fulano\" · Título", now);
        Notification f = new Notification(other, NotificationType.FOLLOW, actor, "USER", actor, "@fulano começou a seguir você", null, now);
        Notification l = new Notification(other, NotificationType.LIKE, actor, "POST", UUID.randomUUID(), "@fulano curtiu seu card", "Título do card", now);
        when(repo.findAllByActorId(actor)).thenReturn(List.of(c, f, l));
        svc.forget(actor);
        verify(repo).deleteAllByUserId(actor);
        assertThat(c.getActorId()).isNull(); assertThat(c.getTitle()).doesNotContain("fulano"); assertThat(c.getBody()).isNull(); assertThat(c.getRefType()).isEqualTo("POST");
        assertThat(f.getTitle()).isEqualTo("Alguém começou a seguir você"); assertThat(f.getRefId()).isNull(); assertThat(f.getRefType()).isNull();
        assertThat(l.getTitle()).isEqualTo("Alguém curtiu seu card"); assertThat(l.getBody()).isEqualTo("Título do card");   // título do card do destinatário não é dado do ator
    }

    @Test
    void listResolvesActorsAndRelativeTime() {
        AuthPrincipal p = new AuthPrincipal(me, UserRole.ATLETA, "s", "j", Instant.MAX);
        Notification n = new Notification(me, NotificationType.TIER_CHANGE, null, "TWIN", UUID.randomUUID(), "Seu Gêmeo Digital agora é EPIC", "Score_Total 950", now.minusSeconds(3600));
        when(repo.findTop50ByUserIdOrderByCreatedAtDesc(me)).thenReturn(List.of(n));
        when(repo.countByUserIdAndReadAtIsNull(me)).thenReturn(1L);
        NotificationPageDTO page = svc.list(p, false);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).actor()).isNull();
        assertThat(page.items().get(0).glyph()).isEqualTo("◈");
        assertThat(page.items().get(0).timeLabel()).isNotBlank();
        assertThat(page.unreadCount()).isEqualTo(1);
        verify(users, never()).findAllById(any());
    }
}
