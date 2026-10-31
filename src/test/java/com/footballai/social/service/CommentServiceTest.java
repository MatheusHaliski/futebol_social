package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.SocialDtos.CommentDTO;
import com.footballai.social.controller.dto.SocialDtos.CreateCommentRequest;
import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.entity.Post;
import com.footballai.social.model.entity.PostComment;
import com.footballai.social.model.entity.Supporter;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.PostCommentRepository;
import com.footballai.social.repository.PostRepository;
import com.footballai.social.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** RF3 — comentários: regras de idade (ECA Digital), contador do card, notificações e permissão de remoção. */
class CommentServiceTest {
    private final Instant now = Instant.parse("2026-10-25T12:00:00Z");
    private final PostCommentRepository comments = mock(PostCommentRepository.class);
    private final PostRepository posts = mock(PostRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final AccessPolicy access = mock(AccessPolicy.class);
    private CommentService svc;

    private final UUID postId = UUID.randomUUID(), ownerId = UUID.randomUUID(), fanId = UUID.randomUUID();
    private Post post;

    @BeforeEach
    void setUp() {
        when(access.canSeePost(any(), any())).thenReturn(true);
        svc = new CommentService(comments, posts, users, notifications, access, Clock.fixed(now, ZoneOffset.UTC));
        post = new Post(); ReflectionTestUtils.setField(post, "id", postId);
        post.setAuthorId(ownerId); post.setStatus(PostStatus.PUBLISHED); post.setTitle("Hat-trick de passes decisivos na semifinal"); post.setAgeRating(AgeRating.SAFE); post.setContentType(PostContentType.PARTIDA_MOMENTO);
        when(posts.findById(postId)).thenReturn(Optional.of(post));
        when(posts.save(any())).thenAnswer(i -> i.getArgument(0));
        when(comments.save(any())).thenAnswer(i -> { PostComment c = i.getArgument(0); if (c.getId() == null) ReflectionTestUtils.setField(c, "id", UUID.randomUUID()); return c; });
        when(users.findAllById(any())).thenReturn(List.of(user(fanId, 1999), user(ownerId, 2004)));
    }

    private static Supporter user(UUID id, int birthYear) {
        Supporter u = new Supporter(); ReflectionTestUtils.setField(u, "id", id);
        u.setUsername("u" + id.toString().substring(0, 6)); u.setDisplayName("<PREENCHER>"); u.setBirthDate(LocalDate.of(birthYear, 1, 1));
        return u;
    }
    private static AuthPrincipal principal(UUID id, UserRole role) { return new AuthPrincipal(id, role, "s", "j", Instant.MAX); }

    @Test
    void createIncrementsCounterAndNotifiesOwner() {
        when(users.findById(fanId)).thenReturn(Optional.of(user(fanId, 1999)));
        CommentDTO c = svc.create(principal(fanId, UserRole.TORCEDOR), postId, new CreateCommentRequest("  Que lance!  ", null));
        assertThat(c.body()).isEqualTo("Que lance!");
        assertThat(c.mine()).isTrue();
        assertThat(post.getCommentCount()).isEqualTo(1);
        verify(notifications).notify(eq(ownerId), eq(NotificationType.COMMENT), eq(fanId), eq("POST"), eq(postId), any(), any());
    }

    @Test
    void under13CannotComment() {
        when(users.findById(fanId)).thenReturn(Optional.of(user(fanId, 2016)));
        assertThatThrownBy(() -> svc.create(principal(fanId, UserRole.TORCEDOR), postId, new CreateCommentRequest("oi", null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("13");
        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    void minorCannotCommentOnAdultRatedCard() {
        post.setAgeRating(AgeRating.AGE_18);
        when(users.findById(fanId)).thenReturn(Optional.of(user(fanId, 2010)));
        assertThatThrownBy(() -> svc.create(principal(fanId, UserRole.TORCEDOR), postId, new CreateCommentRequest("oi", null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("Classificação");
    }

    @Test
    void replyToReplyHangsOnRootAndNotifiesParentAuthor() {
        UUID rootId = UUID.randomUUID(), replyId = UUID.randomUUID(), otherId = UUID.randomUUID();
        PostComment root = new PostComment(postId, otherId, null, "raiz", now); ReflectionTestUtils.setField(root, "id", rootId);
        PostComment reply = new PostComment(postId, otherId, rootId, "resposta", now); ReflectionTestUtils.setField(reply, "id", replyId);
        when(comments.findById(replyId)).thenReturn(Optional.of(reply));
        when(users.findById(fanId)).thenReturn(Optional.of(user(fanId, 1999)));
        CommentDTO c = svc.create(principal(fanId, UserRole.TORCEDOR), postId, new CreateCommentRequest("tréplica", replyId));
        assertThat(c.parentId()).isEqualTo(rootId);
        verify(notifications).notify(eq(otherId), eq(NotificationType.REPLY), eq(fanId), eq("POST"), eq(postId), any(), any());
    }

    @Test
    void deleteAllowedForOwnerOfCardAndLeagueButNotStrangers() {
        UUID cid = UUID.randomUUID();
        PostComment c = new PostComment(postId, fanId, null, "x", now); ReflectionTestUtils.setField(c, "id", cid);
        when(comments.findById(cid)).thenReturn(Optional.of(c));
        post.comment(1);
        assertThatThrownBy(() -> svc.delete(principal(UUID.randomUUID(), UserRole.ATLETA), postId, cid)).isInstanceOf(ApiException.class);
        PostComment reply = new PostComment(postId, UUID.randomUUID(), cid, "resposta", now); ReflectionTestUtils.setField(reply, "id", UUID.randomUUID());
        when(comments.findAllByParentIdAndStatus(cid, CommentStatus.VISIBLE)).thenReturn(List.of(reply)); post.comment(1);
        svc.delete(principal(ownerId, UserRole.ATLETA), postId, cid);
        assertThat(reply.getStatus()).isEqualTo(CommentStatus.REMOVED);   // o fio sai junto com a raiz
        assertThat(c.getStatus()).isEqualTo(CommentStatus.REMOVED);
        assertThat(c.getBody()).isEmpty();
        assertThat(post.getCommentCount()).isZero();
        assertThat(CommentService.canDelete(principal(UUID.randomUUID(), UserRole.OFICIAL_LIGA), c, post)).isTrue();
    }

    @Test
    void draftsAreNotCommentable() {
        post.setStatus(PostStatus.DRAFT);
        when(users.findById(fanId)).thenReturn(Optional.of(user(fanId, 1999)));
        assertThatThrownBy(() -> svc.create(principal(fanId, UserRole.TORCEDOR), postId, new CreateCommentRequest("oi", null))).isInstanceOf(ApiException.class);
    }

    @Test
    void athleteAuthorIsAllowedToo() {
        Athlete a = new Athlete(); ReflectionTestUtils.setField(a, "id", fanId); a.setUsername("ath"); a.setDisplayName("<PREENCHER>"); a.setBirthDate(LocalDate.of(2000, 5, 5));
        when(users.findById(fanId)).thenReturn(Optional.of(a));
        assertThat(svc.create(principal(fanId, UserRole.ATLETA), postId, new CreateCommentRequest("bola na rede", null)).author()).isNotNull();
    }
}
