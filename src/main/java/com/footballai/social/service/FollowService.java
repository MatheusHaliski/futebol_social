package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.AuthorDTO;
import com.footballai.social.controller.dto.SocialDtos.FollowDTO;
import com.footballai.social.controller.dto.SocialDtos.FollowSummaryDTO;
import com.footballai.social.model.entity.Follow;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.FollowTargetType;
import com.footballai.social.model.enums.NotificationType;
import com.footballai.social.repository.FollowRepository;
import com.footballai.social.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seguir (RF3): alimenta a aba "Seguindo" dos feeds e define "conexão" (seguir mútuo), usada pela visibilidade {@code CONEXOES}.
 * Só contas ATIVAS podem ser seguidas; ninguém segue a si mesmo; o alvo recebe notificação FOLLOW.
 */
@Service
public class FollowService {

    private final FollowRepository follows;
    private final UserRepository users;
    private final NotificationService notifications;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public FollowService(FollowRepository follows, UserRepository users, NotificationService notifications) { this(follows, users, notifications, Clock.systemUTC()); }

    FollowService(FollowRepository follows, UserRepository users, NotificationService notifications, Clock clock) {
        this.follows = follows; this.users = users; this.notifications = notifications; this.clock = clock;
    }

    /** Alterna seguir/deixar de seguir e devolve o estado resultante. */
    @Transactional
    public FollowDTO toggle(AuthPrincipal p, UUID targetId) {
        if (p.userId().equals(targetId)) throw ApiException.badRequest("SELF_FOLLOW", "Não dá para seguir a si mesmo");
        User target = users.findById(targetId).filter(u -> u.getStatus() == AccountStatus.ACTIVE).orElseThrow(() -> ApiException.notFound("Perfil"));
        Optional<Follow> existing = follows.findByFollowerIdAndTargetId(p.userId(), targetId);
        if (existing.isPresent()) follows.delete(existing.get());
        else {
            follows.save(new Follow(p.userId(), targetId, FollowTargetType.of(target.getRole()), clock.instant()));
            User me = users.findById(p.userId()).orElse(null);
            notifications.notify(targetId, NotificationType.FOLLOW, p.userId(), "USER", p.userId(),
                    (me == null ? "Alguém" : "@" + me.getUsername()) + " começou a seguir você", null);
        }
        return state(p, targetId);
    }

    public FollowDTO state(AuthPrincipal p, UUID targetId) {
        User target = users.findById(targetId).orElseThrow(() -> ApiException.notFound("Perfil"));
        boolean following = p != null && follows.existsByFollowerIdAndTargetId(p.userId(), targetId);
        boolean followsMe = p != null && follows.existsByFollowerIdAndTargetId(targetId, p.userId());
        return new FollowDTO(targetId, FollowTargetType.of(target.getRole()).name(), following, followsMe, following && followsMe,
                follows.countByTargetId(targetId), follows.countByFollowerId(targetId));
    }

    /** Quem eu sigo. */
    public List<FollowSummaryDTO> following(AuthPrincipal p) { return summaries(follows.findAllByFollowerIdOrderBySinceDesc(p.userId()), Follow::getTargetId, p.userId()); }

    /** Quem me segue. */
    public List<FollowSummaryDTO> followers(AuthPrincipal p) { return summaries(follows.findAllByTargetIdOrderBySinceDesc(p.userId()), Follow::getFollowerId, p.userId()); }

    private List<FollowSummaryDTO> summaries(List<Follow> list, Function<Follow, UUID> other, UUID me) {
        List<UUID> ids = list.stream().map(other).toList();
        Map<UUID, User> byId = ids.isEmpty() ? Map.of() : users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Set<UUID> connections = new HashSet<>(follows.findConnectionIds(me));
        return list.stream().filter(f -> byId.containsKey(other.apply(f))).map(f -> {
            User u = byId.get(other.apply(f));
            return new FollowSummaryDTO(AuthorDTO.of(u, false), FollowTargetType.of(u.getRole()).name(), f.getSince(), connections.contains(u.getId()));
        }).toList();
    }

    /** Ids seguidos pelo usuário (autores cujos cards entram na aba "Seguindo"). */
    public Set<UUID> followedIds(UUID viewer) { return viewer == null ? Set.of() : new HashSet<>(follows.findTargetIds(viewer)); }

    /** Conexões do usuário: seguir mútuo. */
    public Set<UUID> connectionIds(UUID viewer) { return viewer == null ? Set.of() : new HashSet<>(follows.findConnectionIds(viewer)); }

    public boolean isConnection(UUID viewer, UUID target) {
        return viewer != null && follows.existsByFollowerIdAndTargetId(viewer, target) && follows.existsByFollowerIdAndTargetId(target, viewer);
    }

    /* ---------------- LGPD ---------------- */

    @Transactional
    public void forget(UUID userId) { follows.deleteAllByFollowerIdOrTargetId(userId, userId); }

    public Map<String, Object> export(UUID userId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("following", follows.findAllByFollowerIdOrderBySinceDesc(userId).stream().map(f -> Map.of("targetId", f.getTargetId(), "targetType", f.getTargetType(), "since", f.getSince())).toList());
        m.put("followers", follows.findAllByTargetIdOrderBySinceDesc(userId).stream().map(f -> Map.of("followerId", f.getFollowerId(), "since", f.getSince())).toList());
        return m;
    }
}
