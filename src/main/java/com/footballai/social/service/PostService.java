package com.footballai.social.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.PostCardDTO;
import com.footballai.social.controller.dto.CardDtos.PostViewDTO;
import com.footballai.social.controller.dto.PostDtos.CreatePostRequest;
import com.footballai.social.controller.dto.PostDtos.ReactionDTO;
import com.footballai.social.controller.dto.PostDtos.UpdatePostRequest;
import com.footballai.social.model.cassandra.FeedEntry;
import com.footballai.social.model.entity.Brand;
import com.footballai.social.model.entity.CardArt;
import com.footballai.social.model.entity.Post;
import com.footballai.social.model.entity.PostReaction;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.AgeRating;
import com.footballai.social.model.enums.FeedWindow;
import com.footballai.social.model.enums.PostContentType;
import com.footballai.social.model.enums.PostStatus;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.AthleteRepository;
import com.footballai.social.repository.PostReactionRepository;
import com.footballai.social.repository.PostRepository;
import com.footballai.social.repository.cassandra.FeedEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.footballai.social.model.enums.NotificationType;

/** Publicação via FAB/Copilot com classificação estrita (bolinha + borda), regras do título e fan-out para o feed. */
@Service
public class PostService {
    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    public static final int TITLE_MIN_WORDS = 5, TITLE_MAX_WORDS = 12;

    private final PostRepository posts;
    private final PostReactionRepository reactions;
    private final UserService users;
    private final CardAssembler assembler;
    private final ObjectProvider<FeedEntryRepository> feedEntries;
    private final NotificationService notifications;
    private final AccessPolicy access;
    private final AthleteRepository athletes;
    private final Clock clock = Clock.systemUTC();

    public PostService(PostRepository posts, PostReactionRepository reactions, UserService users, CardAssembler assembler, ObjectProvider<FeedEntryRepository> feedEntries,
                       NotificationService notifications, AccessPolicy access, AthleteRepository athletes) {
        this.posts = posts; this.reactions = reactions; this.users = users; this.assembler = assembler; this.feedEntries = feedEntries; this.notifications = notifications;
        this.access = access; this.athletes = athletes;
    }

    public static int wordCount(String title) { return title == null ? 0 : (int) java.util.Arrays.stream(title.trim().split("\\s+")).filter(w -> !w.isBlank()).count(); }

    @Transactional
    public PostCardDTO create(AuthPrincipal p, CreatePostRequest r) {
        User author = users.require(p.userId());
        UserRole role = author.getRole();
        if (!role.canPublish(r.contentType(), UserService.verificationLevelOf(author)))
            throw ApiException.forbidden("CONTENT_TYPE_NOT_ALLOWED", "O papel " + role.label() + " não publica " + r.contentType());
        int words = wordCount(r.title());
        if (words < TITLE_MIN_WORDS || words > TITLE_MAX_WORDS)
            throw ApiException.badRequest("TITLE_WORDS", "Título deve ter entre " + TITLE_MIN_WORDS + " e " + TITLE_MAX_WORDS + " palavras (o feed não mostra texto corrido)");
        Post post = new Post();
        post.setAuthorId(author.getId()); post.setAuthorRole(role); post.setContentType(r.contentType());
        post.setWindow(r.window() != null ? r.window() : r.contentType().defaultWindow());
        post.setTitle(r.title().trim()); post.setSubtitle(r.subtitle()); post.setContent(r.content());
        if (r.mediaKeys() != null && !r.mediaKeys().isEmpty()) post.setMediaKeys("[" + r.mediaKeys().stream().map(k -> "\"" + k.replace("\"", "") + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]");
        AgeRating rating = r.ageRating() == null ? AgeRating.SAFE : r.ageRating();
        if (author instanceof Brand b && b.isBettingBrand()) rating = AgeRating.AGE_18; // Lei 14.790/2023 — publicidade de apostas 18+
        post.setAgeRating(rating);
        post.setAiGenerated(role.isAutomated());
        post.setSeverity(r.severity()); post.setAlertType(r.alertType()); post.setClubId(r.clubId());
        if (r.contentType() == PostContentType.LESAO_ALERTA) post.setSubjectAthleteId(injurySubject(author, r.subjectAthleteId()));
        post.setStateCode(r.stateCode() == null ? author.getStateCode() : r.stateCode().toUpperCase());
        post.setGenerationMode(r.generationMode());
        if (r.art() != null) {
            try { post.setArt(art(r.art(), post.getContentType())); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("ART_INVALID", e.getMessage()); }
        }
        if (Boolean.TRUE.equals(r.draft())) post.setStatus(PostStatus.DRAFT);
        post.setPublishedAt(clock.instant());
        post = posts.save(post);
        if (post.getStatus() == PostStatus.PUBLISHED) fanOut(post, author);
        return assembler.postCard(post, author, false, false, clock.instant());
    }

    private void fanOut(Post post, User author) {
        FeedEntryRepository repo = feedEntries.getIfAvailable();
        if (repo == null) return;
        try {
            for (UUID target : List.of(FeedWindowService.GLOBAL_FEED_USER, author.getId())) {
                FeedEntry e = new FeedEntry();
                e.setKey(new FeedEntry.Key(target, post.getWindow().name(), Uuids.timeBased()));
                e.setPostId(post.getId()); e.setAuthorId(author.getId()); e.setAuthorRole(author.getRole().name());
                e.setContentType(post.getContentType().name()); e.setSourceRole(author.getRole().name());
                e.setSeverity(post.getSeverity() == null ? null : post.getSeverity().name()); e.setAlertType(post.getAlertType() == null ? null : post.getAlertType().name());
                e.setTitle(post.getTitle()); e.setSubtitle(post.getSubtitle()); e.setAgeRating(post.getAgeRating().name()); e.setTrendingScore(0d);
                repo.save(e);
            }
        } catch (RuntimeException ex) { log.warn("Fan-out Cassandra falhou para post {}: {}", post.getId(), ex.getMessage()); }
    }

    @Transactional
    public PostViewDTO view(AuthPrincipal p, UUID id) {
        Post post = posts.findById(id)
                .filter(x -> (x.getStatus() == PostStatus.PUBLISHED && access.canSeePost(p, x))
                        || (p != null && x.getAuthorId().equals(p.userId()) && x.getStatus() != PostStatus.REMOVED)) // autor abre o próprio rascunho (estúdio)
                .orElseThrow(() -> ApiException.notFound("Post"));
        if (post.getStatus() == PostStatus.PUBLISHED) { post.view(); posts.save(post); }
        return new PostViewDTO(assembler.postCards(List.of(post), p).get(0), post.getContent());
    }

    @Transactional
    public ReactionDTO toggle(AuthPrincipal p, UUID id, PostReaction.Kind kind) {
        Post post = visible(p, id);
        var existing = reactions.findByPostIdAndUserIdAndKind(id, p.userId(), kind);
        boolean on;
        if (existing.isPresent()) { reactions.delete(existing.get()); on = false; }
        else { reactions.save(new PostReaction(id, p.userId(), kind, Instant.now(clock))); on = true; }
        if (kind == PostReaction.Kind.LIKE) post.like(on ? 1 : -1); else post.save(on ? 1 : -1);
        posts.save(post);
        if (kind == PostReaction.Kind.LIKE && on) {   // dono do card sabe quem curtiu (nunca o próprio)
            User me = users.require(p.userId());
            notifications.notify(post.getAuthorId(), NotificationType.LIKE, p.userId(), "POST", id, "@" + me.getUsername() + " curtiu seu card", post.getTitle());
        }
        boolean liked = kind == PostReaction.Kind.LIKE ? on : reactions.findByPostIdAndUserIdAndKind(id, p.userId(), PostReaction.Kind.LIKE).isPresent();
        boolean saved = kind == PostReaction.Kind.SAVE ? on : reactions.findByPostIdAndUserIdAndKind(id, p.userId(), PostReaction.Kind.SAVE).isPresent();
        return new ReactionDTO(id, liked, saved, post.getLikeCount(), post.getSaveCount(), post.getShareCount());
    }

    @Transactional
    public ReactionDTO share(AuthPrincipal p, UUID id) {
        Post post = visible(p, id);
        post.share(); posts.save(post);
        return new ReactionDTO(id, reactions.findByPostIdAndUserIdAndKind(id, p.userId(), PostReaction.Kind.LIKE).isPresent(),
                reactions.findByPostIdAndUserIdAndKind(id, p.userId(), PostReaction.Kind.SAVE).isPresent(), post.getLikeCount(), post.getSaveCount(), post.getShareCount());
    }

    /** Rascunho → publicado (último passo "Revisar e salvar" do criador). */
    @Transactional
    public PostCardDTO publishDraft(AuthPrincipal p, UUID id) {
        Post post = posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
        if (!post.getAuthorId().equals(p.userId())) throw ApiException.forbidden("NOT_AUTHOR", "Só o autor publica o rascunho");
        if (post.getStatus() == PostStatus.DRAFT) { post.setStatus(PostStatus.PUBLISHED); post.setPublishedAt(clock.instant()); posts.save(post); fanOut(post, users.require(p.userId())); }
        return assembler.postCards(List.of(post), p).get(0);
    }

    /** Estúdio de edição: o autor altera título, teaser, texto, fotografia, classificação e arte de fundo. */
    @Transactional
    public PostCardDTO update(AuthPrincipal p, UUID id, UpdatePostRequest r) {
        Post post = posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
        if (!post.getAuthorId().equals(p.userId())) throw ApiException.forbidden("NOT_AUTHOR", "Só o autor edita o card");
        if (r.title() != null) {
            int words = wordCount(r.title());
            if (words < TITLE_MIN_WORDS || words > TITLE_MAX_WORDS) throw ApiException.badRequest("TITLE_WORDS", "Título deve ter entre " + TITLE_MIN_WORDS + " e " + TITLE_MAX_WORDS + " palavras");
            post.setTitle(r.title().trim());
        }
        if (r.subtitle() != null) post.setSubtitle(r.subtitle().isBlank() ? null : r.subtitle());
        if (r.content() != null) post.setContent(r.content().isBlank() ? null : r.content());
        if (Boolean.TRUE.equals(r.clearMedia())) post.setMediaKeys(null);
        else if (r.mediaKeys() != null && !r.mediaKeys().isEmpty()) post.setMediaKeys("[" + r.mediaKeys().stream().map(k -> "\"" + k.replace("\"", "") + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]");
        if (r.ageRating() != null && !post.getContentType().sensitive()) post.setAgeRating(r.ageRating());
        if (users.require(p.userId()) instanceof Brand b && b.isBettingBrand()) post.setAgeRating(AgeRating.AGE_18); // Lei 14.790/2023: a edição não rebaixa a classificação de marca de apostas
        if (r.art() != null) {
            try { post.setArt(art(r.art(), post.getContentType())); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("ART_INVALID", e.getMessage()); }
        }
        if (post.hasMedia()) post.setSubtitle(null);
        post = posts.save(post);
        return assembler.postCards(List.of(post), p).get(0);
    }

    /** Sujeito do boletim de lesão: o atleta autor fala de si; o staff de saúde indica um atleta existente (sem atleta, o boletim fica só com o autor). */
    private UUID injurySubject(User author, UUID requested) {
        if (author.getRole() == UserRole.ATLETA) return author.getId();
        if (requested == null) return null;
        if (!athletes.existsById(requested)) throw ApiException.badRequest("SUBJECT_INVALID", "Atleta do boletim não encontrado");
        return requested;
    }

    /** Post visível ao leitor (boletim de lesão segue a regra de saúde do atleta-sujeito) ou 404. */
    private Post visible(AuthPrincipal p, UUID id) {
        return posts.findById(id).filter(x -> x.getStatus() == PostStatus.PUBLISHED && access.canSeePost(p, x)).orElseThrow(() -> ApiException.notFound("Post"));
    }

    private static CardArt art(com.footballai.social.controller.dto.PostDtos.ArtRequest a, PostContentType type) {
        return new CardArt(a.color(), a.color2(), a.gradient(), a.material(), a.layout(), a.style()).validateFor(type);
    }

    /** "Meus cards": publicados e rascunhos do autor, mais recentes primeiro. */
    public List<PostCardDTO> mine(AuthPrincipal p) {
        List<Post> mine = posts.findAllByAuthorId(p.userId()).stream().filter(x -> x.getStatus() != PostStatus.REMOVED)
                .sorted(java.util.Comparator.comparing(Post::getPublishedAt).reversed()).toList();
        return assembler.postCards(mine, p);
    }

    public FeedWindow defaultWindow(CreatePostRequest r) { return r.window() != null ? r.window() : r.contentType().defaultWindow(); }
}
