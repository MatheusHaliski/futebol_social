package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.SocialDtos.FollowDTO;
import com.footballai.social.model.entity.Club;
import com.footballai.social.model.entity.Follow;
import com.footballai.social.model.entity.Supporter;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.FollowTargetType;
import com.footballai.social.model.enums.NotificationType;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.FollowRepository;
import com.footballai.social.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** RF3 — seguir: alternância, tipo do alvo, conexão (seguir mútuo) e notificação. */
class FollowServiceTest {
    private final FollowRepository follows = mock(FollowRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final FollowService svc = new FollowService(follows, users, notifications, Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC));
    private final UUID me = UUID.randomUUID(), other = UUID.randomUUID();

    private static <T extends User> T user(T u, UUID id, String handle) {
        ReflectionTestUtils.setField(u, "id", id); u.setUsername(handle); u.setDisplayName("<PREENCHER>"); u.setStatus(AccountStatus.ACTIVE); return u;
    }
    private AuthPrincipal p() { return new AuthPrincipal(me, UserRole.TORCEDOR, "s", "j", Instant.MAX); }

    @BeforeEach
    void setUp() {
        when(users.findById(me)).thenReturn(Optional.of(user(new Supporter(), me, "eu")));
        when(users.findById(other)).thenReturn(Optional.of(user(new Club(), other, "clube")));
        when(follows.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void toggleFollowsThenUnfollowsAndNotifiesOnce() {
        when(follows.findByFollowerIdAndTargetId(me, other)).thenReturn(Optional.empty());
        when(follows.existsByFollowerIdAndTargetId(me, other)).thenReturn(true);
        FollowDTO on = svc.toggle(p(), other);
        assertThat(on.following()).isTrue();
        assertThat(on.targetType()).isEqualTo(FollowTargetType.CLUBE.name());
        verify(notifications).notify(eq(other), eq(NotificationType.FOLLOW), eq(me), eq("USER"), eq(me), eq("@eu começou a seguir você"), any());

        Follow existing = new Follow(me, other, FollowTargetType.CLUBE, Instant.EPOCH);
        when(follows.findByFollowerIdAndTargetId(me, other)).thenReturn(Optional.of(existing));
        when(follows.existsByFollowerIdAndTargetId(me, other)).thenReturn(false);
        assertThat(svc.toggle(p(), other).following()).isFalse();
        verify(follows).delete(existing);
        verifyNoMoreInteractions(notifications);
    }

    @Test
    void cannotFollowSelfOrInactiveAccount() {
        assertThatThrownBy(() -> svc.toggle(p(), me)).isInstanceOf(ApiException.class).hasMessageContaining("si mesmo");
        UUID gone = UUID.randomUUID(); Supporter s = user(new Supporter(), gone, "gone"); s.setStatus(AccountStatus.PENDING_DELETE);
        when(users.findById(gone)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> svc.toggle(p(), gone)).isInstanceOf(ApiException.class);
    }

    @Test
    void connectionRequiresMutualFollow() {
        when(follows.existsByFollowerIdAndTargetId(me, other)).thenReturn(true);
        when(follows.existsByFollowerIdAndTargetId(other, me)).thenReturn(false);
        assertThat(svc.isConnection(me, other)).isFalse();
        assertThat(svc.state(p(), other).connection()).isFalse();
        when(follows.existsByFollowerIdAndTargetId(other, me)).thenReturn(true);
        assertThat(svc.isConnection(me, other)).isTrue();
        assertThat(svc.state(p(), other).connection()).isTrue();
    }

    @Test
    void followedIdsAreEmptyForAnonymousAndTypeFollowsRole() {
        assertThat(svc.followedIds(null)).isEmpty();
        when(follows.findTargetIds(me)).thenReturn(List.of(other));
        assertThat(svc.followedIds(me)).containsExactly(other);
        assertThat(FollowTargetType.of(UserRole.OFICIAL_LIGA)).isEqualTo(FollowTargetType.LIGA);
        assertThat(FollowTargetType.of(UserRole.ATLETA)).isEqualTo(FollowTargetType.USER);
    }
}
