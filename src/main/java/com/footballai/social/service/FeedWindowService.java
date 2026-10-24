package com.footballai.social.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.AthleteCardDTO;
import com.footballai.social.controller.dto.CardDtos.FeedPageDTO;
import com.footballai.social.controller.dto.CardDtos.PostCardDTO;
import com.footballai.social.model.cassandra.FeedEntry;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.AthleteRepository;
import com.footballai.social.repository.PostRepository;
import com.footballai.social.repository.cassandra.FeedEntryRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RF3 — as 4 janelas independentes de feed com filtros próprios, paginação por cursor e cache curto em Redis.
 * Fonte: Cassandra (fan-out {@code feed_entries}) quando habilitado; MySQL (Specification) como fallback/local.
 */
@Service
public class FeedWindowService {
    private static final Logger log = LoggerFactory.getLogger(FeedWindowService.class);
    /** Partição global por janela (fan-out sem grafo de seguidores). */
    public static final UUID GLOBAL_FEED_USER = new UUID(0L, 0L);
    private static final Duration CACHE_TTL = Duration.ofSeconds(20);

    private final PostRepository posts;
    private final AthleteRepository athletes;
    private final ObjectProvider<FeedEntryRepository> feedEntries;
    private final CardAssembler assembler;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public FeedWindowService(PostRepository posts, AthleteRepository athletes, ObjectProvider<FeedEntryRepository> feedEntries, CardAssembler assembler,
                             StringRedisTemplate redis, ObjectMapper mapper) {
        this.posts = posts; this.athletes = athletes; this.feedEntries = feedEntries; this.assembler = assembler; this.redis = redis; this.mapper = mapper;
    }

    public FeedPageDTO page(FeedQuery q, AuthPrincipal viewer) {
        String key = q.cacheKey(viewer == null ? null : viewer.userId());
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return mapper.readValue(cached, FeedPageDTO.class);
        } catch (Exception e) { log.debug("cache miss/erro {}: {}", key, e.getMessage()); }

        FeedEntryRepository cass = feedEntries.getIfAvailable();
        FeedPageDTO page = cass != null ? fromCassandra(cass, q, viewer) : fromMysql(q, viewer);
        try { redis.opsForValue().set(key, mapper.writeValueAsString(page), CACHE_TTL); } catch (Exception e) { log.debug("cache write falhou: {}", e.getMessage()); }
        return page;
    }

    /* ---------------- MySQL (Specification) ---------------- */

    FeedPageDTO fromMysql(FeedQuery q, AuthPrincipal viewer) {
        Instant before = decodeTimeCursor(q.cursor);
        Specification<Post> spec = postSpec(q, before);
        Sort sort = q.sort == FeedSort.TRENDING ? Sort.by(Sort.Order.desc("trendingScore"), Sort.Order.desc("publishedAt")) : Sort.by(Sort.Order.desc("publishedAt"));
        List<Post> found = posts.findAll(spec, PageRequest.of(0, q.size + 1, sort)).getContent();
        boolean more = found.size() > q.size;
        List<Post> pagePosts = more ? found.subList(0, q.size) : found;
        String next = more ? encodeTimeCursor(pagePosts.get(pagePosts.size() - 1).getPublishedAt()) : null;
        List<PostCardDTO> cards = assembler.postCards(pagePosts, viewer);
        List<AthleteCardDTO> ath = athleteCards(q, viewer);
        return new FeedPageDTO(q.window.name(), q.sort.name(), cards, ath, next, q.applied(), "mysql");
    }

    Specification<Post> postSpec(FeedQuery q, Instant before) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), PostStatus.PUBLISHED));
            ps.add(cb.equal(root.get("window"), q.window));
            if (before != null) ps.add(cb.lessThan(root.get("publishedAt"), before));
            if (!q.roles.isEmpty()) ps.add(root.get("authorRole").in(q.roles));
            if (q.state != null) ps.add(cb.equal(root.get("stateCode"), q.state));
            if (q.clubId != null) ps.add(cb.equal(root.get("clubId"), q.clubId));
            if (q.q != null) ps.add(cb.like(cb.lower(root.get("title")), "%" + q.q.toLowerCase() + "%"));
            if (q.athleteId != null) ps.add(cb.equal(root.get("authorId"), q.athleteId));
            if (q.periodDuration() != null) ps.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), clock.instant().minus(q.periodDuration())));
            if (!q.eventTypes.isEmpty()) {
                List<PostContentType> types = new ArrayList<>();
                for (EventType e : q.eventTypes) types.add(e == EventType.TREINO ? PostContentType.TREINO : PostContentType.PARTIDA_MOMENTO);
                ps.add(root.get("contentType").in(types));
            }
            if (!q.severities.isEmpty()) ps.add(root.get("severity").in(q.severities));
            if (!q.alertTypes.isEmpty()) ps.add(root.get("alertType").in(q.alertTypes));
            if (!q.sources.isEmpty()) ps.add(root.get("authorRole").in(q.sources));
            if (q.window == FeedWindow.SCOUTING && q.source == ReportSource.HOMOLOGADO)
                ps.add(root.get("authorRole").in(List.of(UserRole.SCOUT, UserRole.STAFF, UserRole.CLUBE)));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Cards de atleta: PERFIL (descoberta) sempre; SCOUTING quando há atributos ou comparação. */
    List<AthleteCardDTO> athleteCards(FeedQuery q, AuthPrincipal viewer) {
        boolean wants = q.window == FeedWindow.PERFIL || (q.window == FeedWindow.SCOUTING && (!q.attributes.isEmpty() || !q.compare.isEmpty()));
        if (!wants || q.cursor != null) return List.of();
        if (!q.roles.isEmpty() && !q.roles.contains(UserRole.ATLETA)) return List.of();
        List<Athlete> found = athletes.findAll(athleteSpec(q, viewer), PageRequest.of(0, Math.min(q.size, 12), Sort.by(Sort.Order.asc("displayName")))).getContent();
        return assembler.athleteCards(found, viewer);
    }

    Specification<Athlete> athleteSpec(FeedQuery q, AuthPrincipal viewer) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), AccountStatus.ACTIVE));
            Predicate visible = cb.equal(root.get("visibility"), ProfileVisibility.PUBLICO);
            ps.add(viewer == null ? visible : cb.or(visible, cb.equal(root.get("id"), viewer.userId())));
            if (q.position != null) ps.add(cb.equal(root.get("position"), q.position));
            if (q.state != null) ps.add(cb.equal(root.get("stateCode"), q.state));
            if (q.clubId != null) ps.add(cb.equal(root.get("currentClubId"), q.clubId));
            if (q.contractStatus != null) ps.add(cb.equal(root.get("contractStatus"), q.contractStatus));
            LocalDate today = LocalDate.now(clock);
            if (q.ageMin != null) ps.add(cb.lessThanOrEqualTo(root.get("birthDate"), today.minusYears(q.ageMin)));
            if (q.ageMax != null) ps.add(cb.greaterThan(root.get("birthDate"), today.minusYears(q.ageMax + 1L)));
            if (q.q != null) { String like = "%" + q.q.toLowerCase().replace("@", "") + "%"; ps.add(cb.or(cb.like(cb.lower(root.get("username")), like), cb.like(cb.lower(root.get("displayName")), like))); }
            if (!q.compare.isEmpty()) ps.add(root.get("id").in(q.compare));
            if (!q.tiers.isEmpty()) {
                Subquery<UUID> sq = cq.subquery(UUID.class);
                var t = sq.from(DigitalTwin.class);
                sq.select(t.get("userId")).where(t.get("rarityTier").in(q.tiers), cb.equal(t.get("status"), TwinStatus.ACTIVE));
                ps.add(root.get("id").in(sq));
            }
            for (FeedQuery.AttributeFilter f : q.attributes) {
                if (!ScoutingAttribute.NAMES.contains(f.name())) continue;
                Subquery<UUID> sq = cq.subquery(UUID.class);
                var s = sq.from(ScoutingAttribute.class);
                var col = s.<java.math.BigDecimal>get(f.name());
                java.math.BigDecimal v = java.math.BigDecimal.valueOf(f.value());
                Predicate cmp = switch (f.op()) {
                    case "gt" -> cb.greaterThan(col, v); case "gte" -> cb.greaterThanOrEqualTo(col, v);
                    case "lt" -> cb.lessThan(col, v); case "lte" -> cb.lessThanOrEqualTo(col, v); default -> cb.equal(col, v);
                };
                sq.select(s.get("athleteId")).where(cmp);
                ps.add(root.get("id").in(sq));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /* ---------------- Cassandra (fan-out) ---------------- */

    FeedPageDTO fromCassandra(FeedEntryRepository repo, FeedQuery q, AuthPrincipal viewer) {
        CassandraPageRequest pr = q.cursor == null ? CassandraPageRequest.first(q.size * 2)
                : CassandraPageRequest.of(PageRequest.of(0, q.size * 2), ByteBuffer.wrap(Base64.getUrlDecoder().decode(q.cursor)));
        Slice<FeedEntry> slice = repo.findByKeyUserIdAndKeyFeedWindow(GLOBAL_FEED_USER, q.window.name(), pr);
        List<FeedEntry> entries = slice.getContent().stream().filter(e -> matches(e, q)).limit(q.size).toList();
        Map<UUID, Post> byId = posts.findAllByIdIn(entries.stream().map(FeedEntry::getPostId).toList()).stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> ordered = entries.stream().map(e -> byId.get(e.getPostId())).filter(Objects::nonNull).filter(p -> p.getStatus() == PostStatus.PUBLISHED).toList();
        String next = null;
        if (slice.hasNext() && slice.nextPageable() instanceof CassandraPageRequest n && n.getPagingState() != null) {
            ByteBuffer st = n.getPagingState(); byte[] b = new byte[st.remaining()]; st.duplicate().get(b);
            next = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        }
        return new FeedPageDTO(q.window.name(), q.sort.name(), assembler.postCards(ordered, viewer), athleteCards(q, viewer), next, q.applied(), "cassandra");
    }

    static boolean matches(FeedEntry e, FeedQuery q) {
        if (!q.roles.isEmpty() && q.roles.stream().noneMatch(r -> r.name().equals(e.getAuthorRole()))) return false;
        if (!q.severities.isEmpty() && q.severities.stream().noneMatch(s -> s.name().equals(e.getSeverity()))) return false;
        if (!q.alertTypes.isEmpty() && q.alertTypes.stream().noneMatch(a -> a.name().equals(e.getAlertType()))) return false;
        if (!q.sources.isEmpty() && q.sources.stream().noneMatch(s -> s.name().equals(e.getSourceRole()))) return false;
        if (q.athleteId != null && !q.athleteId.equals(e.getAuthorId())) return false;
        if (!q.eventTypes.isEmpty()) {
            boolean ok = q.eventTypes.stream().anyMatch(ev -> (ev == EventType.TREINO ? "TREINO" : "PARTIDA_MOMENTO").equals(e.getContentType()));
            if (!ok) return false;
        }
        return q.q == null || (e.getTitle() != null && e.getTitle().toLowerCase().contains(q.q.toLowerCase()));
    }

    /* ---------------- cursores ---------------- */

    static String encodeTimeCursor(Instant t) { return Base64.getUrlEncoder().withoutPadding().encodeToString(Long.toString(t.toEpochMilli()).getBytes()); }
    static Instant decodeTimeCursor(String c) {
        if (c == null) return null;
        try { return Instant.ofEpochMilli(Long.parseLong(new String(Base64.getUrlDecoder().decode(c)))); } catch (RuntimeException e) { throw ApiException.badRequest("CURSOR_INVALID", "Cursor inválido"); }
    }
}
