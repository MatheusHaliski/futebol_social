package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.AuthorDTO;
import com.footballai.social.controller.dto.SocialDtos.MarkReadRequest;
import com.footballai.social.controller.dto.SocialDtos.NotificationDTO;
import com.footballai.social.controller.dto.SocialDtos.NotificationPageDTO;
import com.footballai.social.model.entity.Notification;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.NotificationType;
import com.footballai.social.repository.NotificationRepository;
import com.footballai.social.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Notificações pessoais (RF3, "sino"): curtidas, comentários, respostas, novos seguidores, resultado FLAIR e mudança de tier.
 * Regras: ninguém é notificado da própria ação; título/corpo nunca carregam dado sensível (Art. 11); o destinatário pode ler e marcar como lidas.
 */
@Service
public class NotificationService {

    static final int MAX_BODY = 280;

    private final NotificationRepository repo;
    private final UserRepository users;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationService(NotificationRepository repo, UserRepository users) { this(repo, users, Clock.systemUTC()); }

    NotificationService(NotificationRepository repo, UserRepository users, Clock clock) { this.repo = repo; this.users = users; this.clock = clock; }

    /** Cria a notificação; ações do próprio destinatário são ignoradas. */
    @Transactional
    public Optional<Notification> notify(UUID userId, NotificationType type, UUID actorId, String refType, UUID refId, String title, String body) {
        if (userId == null || (actorId != null && actorId.equals(userId))) return Optional.empty();
        String b = body == null ? null : body.length() > MAX_BODY ? body.substring(0, MAX_BODY - 1) + "…" : body;
        return Optional.of(repo.save(new Notification(userId, type, actorId, refType, refId, title.length() > 120 ? title.substring(0, 119) + "…" : title, b, clock.instant())));
    }

    public NotificationPageDTO list(AuthPrincipal p, boolean unreadOnly) {
        List<Notification> items = unreadOnly ? repo.findTop50ByUserIdAndReadAtIsNullOrderByCreatedAtDesc(p.userId()) : repo.findTop50ByUserIdOrderByCreatedAtDesc(p.userId());
        return new NotificationPageDTO(toDtos(items), repo.countByUserIdAndReadAtIsNull(p.userId()));
    }

    public long unreadCount(UUID userId) { return repo.countByUserIdAndReadAtIsNull(userId); }

    /** Marca como lidas (todas ou por id); só as do próprio usuário. */
    @Transactional
    public NotificationPageDTO markRead(AuthPrincipal p, MarkReadRequest r) {
        Instant now = clock.instant();
        if (r == null || Boolean.TRUE.equals(r.all()) || r.ids() == null || r.ids().isEmpty()) repo.markAllRead(p.userId(), now);
        else for (Notification n : repo.findAllByUserIdAndIdIn(p.userId(), r.ids())) { n.markRead(now); repo.save(n); }
        return list(p, false);
    }

    List<NotificationDTO> toDtos(List<Notification> items) {
        List<UUID> actorIds = items.stream().map(Notification::getActorId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, User> actors = actorIds.isEmpty() ? Map.of() : users.findAllById(actorIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Instant now = clock.instant();
        return items.stream().map(n -> {
            User actor = n.getActorId() == null ? null : actors.get(n.getActorId());
            return new NotificationDTO(n.getId(), n.getType().name(), n.getType().glyph(), n.getTitle(), n.getBody(), actor == null ? null : AuthorDTO.of(actor, false),
                    n.getRefType(), n.getRefId(), n.getCreatedAt(), CardAssembler.timeLabel(n.getCreatedAt(), now), n.isUnread());
        }).toList();
    }

    /* ---------------- LGPD ---------------- */

    /** Exclusão do destinatário apaga a caixa; como ator, as notificações alheias perdem id, @handle no título, trecho de comentário e referência ao perfil. */
    @Transactional
    public void forget(UUID userId) {
        repo.deleteAllByUserId(userId);
        for (Notification n : repo.findAllByActorId(userId)) { n.anonymizeActor(); repo.save(n); }
    }

    public List<Map<String, Object>> export(UUID userId) {
        return repo.findAllByUserIdOrderByCreatedAtDesc(userId).stream().<Map<String, Object>>map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", n.getType()); m.put("title", n.getTitle()); m.put("body", String.valueOf(n.getBody())); m.put("createdAt", n.getCreatedAt()); m.put("readAt", String.valueOf(n.getReadAt()));
            return m;
        }).toList();
    }
}
