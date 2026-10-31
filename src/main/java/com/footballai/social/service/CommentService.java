package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.AuthorDTO;
import com.footballai.social.controller.dto.SocialDtos.CommentDTO;
import com.footballai.social.controller.dto.SocialDtos.CommentPageDTO;
import com.footballai.social.controller.dto.SocialDtos.CreateCommentRequest;
import com.footballai.social.model.entity.Post;
import com.footballai.social.model.entity.PostComment;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.CommentStatus;
import com.footballai.social.model.enums.NotificationType;
import com.footballai.social.model.enums.PostStatus;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.PostCommentRepository;
import com.footballai.social.repository.PostRepository;
import com.footballai.social.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Comentários em cards (RF3). Regras:
 * <ul>
 *   <li>só em card PUBLICADO; um nível de resposta (responder a uma resposta pendura na raiz);</li>
 *   <li>ECA Digital: menores de 13 não comentam; menor não comenta em card cuja classificação exige idade maior que a dele;</li>
 *   <li>texto 1–500 caracteres, sem HTML (a SPA escapa na renderização);</li>
 *   <li>remoção lógica pelo autor do comentário, pelo dono do card ou por OFICIAL_LIGA (moderação); remover a raiz remove o fio; o contador do card acompanha;</li>
 *   <li>dono do card recebe COMMENT; autor do comentário-pai recebe REPLY.</li>
 * </ul>
 */
@Service
public class CommentService {

    static final int MAX_BODY = 500;
    static final int MIN_AGE_TO_COMMENT = 13;
    static final int MAX_PAGE = 50;

    private final PostCommentRepository comments;
    private final PostRepository posts;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AccessPolicy access;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CommentService(PostCommentRepository comments, PostRepository posts, UserRepository users, NotificationService notifications, AccessPolicy access) {
        this(comments, posts, users, notifications, access, Clock.systemUTC());
    }

    CommentService(PostCommentRepository comments, PostRepository posts, UserRepository users, NotificationService notifications, AccessPolicy access, Clock clock) {
        this.comments = comments; this.posts = posts; this.users = users; this.notifications = notifications; this.access = access; this.clock = clock;
    }

    public CommentPageDTO list(AuthPrincipal p, UUID postId, String cursor, Integer size) {
        Post post = visiblePost(p, postId);
        int n = size == null ? 20 : Math.max(1, Math.min(MAX_PAGE, size));
        Instant after = cursor == null ? Instant.EPOCH : FeedWindowService.decodeTimeCursor(cursor);
        List<PostComment> found = comments.findAllByPostIdAndStatusAndCreatedAtAfterOrderByCreatedAtAsc(postId, CommentStatus.VISIBLE, after, PageRequest.of(0, n + 1));
        boolean more = found.size() > n;
        List<PostComment> page = more ? found.subList(0, n) : found;
        String next = more ? FeedWindowService.encodeTimeCursor(page.get(page.size() - 1).getCreatedAt()) : null;
        return new CommentPageDTO(postId, toDtos(page, post, p), next, comments.countByPostIdAndStatus(postId, CommentStatus.VISIBLE));
    }

    @Transactional
    public CommentDTO create(AuthPrincipal p, UUID postId, CreateCommentRequest r) {
        Post post = visiblePost(p, postId);
        User author = users.findById(p.userId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        checkCanComment(author, post);
        String body = r.body() == null ? "" : r.body().trim();
        if (body.isEmpty() || body.length() > MAX_BODY) throw ApiException.badRequest("COMMENT_BODY", "Comentário deve ter entre 1 e " + MAX_BODY + " caracteres");
        UUID parentId = null; PostComment parent = null;
        if (r.parentId() != null) {
            parent = comments.findById(r.parentId()).filter(c -> c.getPostId().equals(postId) && c.getStatus() == CommentStatus.VISIBLE)
                    .orElseThrow(() -> ApiException.notFound("Comentário"));
            parentId = parent.getParentId() != null ? parent.getParentId() : parent.getId();   // um nível: resposta de resposta pendura na raiz
        }
        PostComment c = comments.save(new PostComment(postId, p.userId(), parentId, body, clock.instant()));
        post.comment(1); posts.save(post);
        String handle = "@" + author.getUsername();
        notifications.notify(post.getAuthorId(), NotificationType.COMMENT, p.userId(), "POST", postId, handle + " comentou no seu card", excerpt(post.getTitle(), body));
        if (parent != null && !parent.getAuthorId().equals(post.getAuthorId()))
            notifications.notify(parent.getAuthorId(), NotificationType.REPLY, p.userId(), "POST", postId, handle + " respondeu seu comentário", excerpt(post.getTitle(), body));
        return toDtos(List.of(c), post, p).get(0);
    }

    @Transactional
    public void delete(AuthPrincipal p, UUID postId, UUID commentId) {
        PostComment c = comments.findById(commentId).filter(x -> x.getPostId().equals(postId) && x.getStatus() == CommentStatus.VISIBLE).orElseThrow(() -> ApiException.notFound("Comentário"));
        Post post = posts.findById(postId).orElseThrow(() -> ApiException.notFound("Post"));
        if (!canDelete(p, c, post)) throw ApiException.forbidden("NOT_ALLOWED", "Só o autor do comentário, o dono do card ou a liga removem comentários");
        Instant now = clock.instant();
        c.remove(now); comments.save(c);
        int removed = 1;
        if (c.getParentId() == null) for (PostComment reply : comments.findAllByParentIdAndStatus(c.getId(), CommentStatus.VISIBLE)) { reply.remove(now); comments.save(reply); removed++; }   // o fio sai junto com a raiz
        post.comment(-removed); posts.save(post);
    }

    /* ---------------- regras ---------------- */

    static void checkCanComment(User author, Post post) {
        Integer age = author.age();
        if (age != null && age < MIN_AGE_TO_COMMENT) throw ApiException.forbidden("MINOR_RESTRICTED", "Menores de 13 anos não comentam (ECA Digital)");
        if (age != null && age < post.getAgeRating().minAge()) throw ApiException.forbidden("AGE_RESTRICTED", "Classificação " + post.getAgeRating().label() + " acima da idade do usuário");
    }

    static boolean canDelete(AuthPrincipal p, PostComment c, Post post) {
        return p != null && (c.getAuthorId().equals(p.userId()) || post.getAuthorId().equals(p.userId()) || p.role() == UserRole.OFICIAL_LIGA);
    }

    static String excerpt(String title, String body) {
        String b = body.length() > 90 ? body.substring(0, 89) + "…" : body;
        return title == null ? b : "\"" + b + "\" · " + title;
    }

    /** Comentários de um boletim de lesão são tão sensíveis quanto o boletim: mesma regra de acesso (Art. 11). */
    Post visiblePost(AuthPrincipal viewer, UUID postId) {
        return posts.findById(postId).filter(x -> x.getStatus() == PostStatus.PUBLISHED && access.canSeePost(viewer, x)).orElseThrow(() -> ApiException.notFound("Post"));
    }

    List<CommentDTO> toDtos(List<PostComment> list, Post post, AuthPrincipal viewer) {
        List<UUID> ids = list.stream().map(PostComment::getAuthorId).distinct().toList();
        Map<UUID, User> authors = ids.isEmpty() ? Map.of() : users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Instant now = clock.instant();
        return list.stream().map(c -> {
            User a = authors.get(c.getAuthorId());
            boolean mine = viewer != null && c.getAuthorId().equals(viewer.userId());
            return new CommentDTO(c.getId(), c.getPostId(), c.getParentId(), a == null ? null : AuthorDTO.of(a, a.getRole() == UserRole.AGENTE_IA), c.getBody(),
                    c.getCreatedAt(), CardAssembler.timeLabel(c.getCreatedAt(), now), mine, canDelete(viewer, c, post));
        }).toList();
    }

    /* ---------------- LGPD ---------------- */

    /** Anonimização do autor: comentários somem do feed e os contadores dos cards acompanham. */
    @Transactional
    public void forget(UUID userId) {
        Instant now = clock.instant();
        for (PostComment c : comments.findAllByAuthorIdAndStatus(userId, CommentStatus.VISIBLE)) {
            c.remove(now); comments.save(c);
            posts.findById(c.getPostId()).ifPresent(post -> { post.comment(-1); posts.save(post); });
        }
    }

    public List<Map<String, Object>> export(UUID userId) {
        return comments.findAllByAuthorIdOrderByCreatedAtDesc(userId).stream().<Map<String, Object>>map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("postId", c.getPostId()); m.put("body", c.getBody()); m.put("status", c.getStatus()); m.put("createdAt", c.getCreatedAt());
            return m;
        }).toList();
    }
}
