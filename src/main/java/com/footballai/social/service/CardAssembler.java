package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta os DTOs dos cards (.pcard e .vcard) aplicando as regras de privacidade:
 * dados médicos só para o próprio atleta ou com shareMedicalData = true; atributos sempre como lista de badges.
 */
@Component
public class CardAssembler {

    private final UserRepository users;
    private final DigitalTwinRepository twins;
    private final WeeklyScoreSnapshotRepository snapshots;
    private final MatchPerformanceRepository matches;
    private final TrainingSummaryRepository training;
    private final InjuryRecordRepository injuries;
    private final UserLgpdPreferencesRepository prefs;
    private final AthleteBadgeRepository badges;
    private final PostReactionRepository reactions;
    private final StorageService storage;
    private final Clock clock;

    public CardAssembler(UserRepository users, DigitalTwinRepository twins, WeeklyScoreSnapshotRepository snapshots, MatchPerformanceRepository matches,
                         TrainingSummaryRepository training, InjuryRecordRepository injuries, UserLgpdPreferencesRepository prefs, AthleteBadgeRepository badges,
                         PostReactionRepository reactions, StorageService storage) {
        this.users = users; this.twins = twins; this.snapshots = snapshots; this.matches = matches; this.training = training; this.injuries = injuries;
        this.prefs = prefs; this.badges = badges; this.reactions = reactions; this.storage = storage; this.clock = Clock.systemUTC();
    }

    /* ---------------- posts ---------------- */

    public List<PostCardDTO> postCards(List<Post> posts, AuthPrincipal viewer) {
        if (posts.isEmpty()) return List.of();
        Map<UUID, User> authors = users.findAllById(posts.stream().map(Post::getAuthorId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Set<UUID> liked = new HashSet<>(), saved = new HashSet<>();
        if (viewer != null) {
            for (PostReaction r : reactions.findAllByUserIdAndPostIdIn(viewer.userId(), posts.stream().map(Post::getId).toList()))
                (r.getKind() == PostReaction.Kind.LIKE ? liked : saved).add(r.getPostId());
        }
        Instant now = clock.instant();
        return posts.stream().map(p -> postCard(p, authors.get(p.getAuthorId()), liked.contains(p.getId()), saved.contains(p.getId()), now)).toList();
    }

    public PostCardDTO postCard(Post p, User author, boolean liked, boolean saved, Instant now) {
        PostContentType t = p.getContentType();
        AuthorDTO a = author == null ? AuthorDTO.unknown(p.getAuthorId(), p.getAuthorRole()) : AuthorDTO.of(author, p.isAiGenerated());
        MediaDTO media = null;
        if (p.hasMedia()) {
            String key = p.getMediaKeys().replaceAll("[\\[\\]\"\\s]", "").split(",")[0];
            media = new MediaDTO(key, storage.mediaUrl(key), key.matches("(?i).*\\.(mp4|mov|webm)$") ? "video" : "image");
        }
        List<String> meta = new ArrayList<>();
        if (p.getSeverity() != null) meta.add("URGÊNCIA " + p.getSeverity());
        if (p.getAlertType() != null) meta.add(p.getAlertType().name());
        if (p.getStateCode() != null) meta.add(p.getStateCode());
        if (p.isAiGenerated()) meta.add("GERADO POR IA");
        return new PostCardDTO(p.getId(), p.getPublishedAt(), timeLabel(p.getPublishedAt(), now), a,
                t.name(), t.cssClass(), t.borderColor(), t.badgeLabel(), t.vslGlyph().glyph(), t.hazard(),
                AgeRatingDTO.of(p.getAgeRating()), p.getTitle(), media == null ? p.getSubtitle() : null, media, meta,
                new SocialCountersDTO(p.getLikeCount(), p.getCommentCount(), p.getShareCount(), p.getSaveCount(), liked, saved),
                new TrendDTO(p.trend().name(), p.trend().emoji()), p.getWindow().name(),
                p.getSeverity() == null ? null : p.getSeverity().name(), p.getAlertType() == null ? null : p.getAlertType().name(),
                p.getGenerationMode().name(), CardArtDTO.of(p.getArt()));
    }

    static String timeLabel(Instant at, Instant now) {
        Duration d = Duration.between(at, now);
        if (d.toMinutes() < 1) return "AGORA";
        if (d.toHours() < 1) return d.toMinutes() + "MIN";
        if (d.toDays() < 1) return d.toHours() + "H";
        if (d.toDays() < 7) return d.toDays() + "D";
        return (d.toDays() / 7) + "SEM";
    }

    /* ---------------- atletas ---------------- */

    public List<AthleteCardDTO> athleteCards(List<Athlete> athletes, AuthPrincipal viewer) {
        return athletes.stream().map(a -> athleteCard(a, viewer)).toList();
    }

    public AthleteCardDTO athleteCard(Athlete a, AuthPrincipal viewer) {
        boolean owner = viewer != null && viewer.userId().equals(a.getId());
        boolean canSeeMedical = owner || prefs.findById(a.getId()).map(UserLgpdPreferences::isShareMedicalData).orElse(false);
        DigitalTwin twin = twins.findByUserId(a.getId()).orElse(null);
        RarityTier tier = twin == null ? RarityTier.COMMON : twin.getRarityTier();
        int score = twin == null ? 0 : twin.getScoreTotal();
        int pp = 0, rp = 0, mp = 0;
        if (twin != null) {
            var last = snapshots.findTop12ByTwinIdOrderByIsoWeekDesc(twin.getId());
            if (!last.isEmpty()) {
                WeeklyScoreSnapshot s = last.get(0);
                pp = s.getPhysicalScore().multiply(new BigDecimal("1500")).setScale(0, RoundingMode.HALF_EVEN).intValue();
                rp = s.getTechnicalScore().multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_EVEN).intValue();
                mp = s.getMatchScore().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_EVEN).intValue();
            }
        }
        String season = IsoWeeks.season(LocalDate.now(clock));
        BadgeAwardService.SeasonStats st = BadgeAwardService.aggregate(matches.findAllByAthleteIdAndSeasonOrderByMatchDateAsc(a.getId(), season));
        boolean gk = a.getPosition() == Position.GOLEIRO;
        boolean def = a.getPosition() == Position.ZAGUEIRO || a.getPosition() == Position.LATERAL || a.getPosition() == Position.VOLANTE;
        SeasonStatsDTO seasonDto = new SeasonStatsDTO(season, st.games(), st.goals(), st.assists(), st.minutes(), st.yellow(), st.red(),
                gk ? st.cleanSheets() : null, def ? st.tackles() : null, st.avgRating());

        PhysicalDTO physical = null; boolean sensitive = false;
        if (canSeeMedical) {
            TrainingSummary ts = training.findFirstByAthleteIdOrderByIsoWeekDesc(a.getId()).orElse(null);
            PhysicalStatus ps = twin == null ? PhysicalStatus.READY : twin.getPhysicalStatus();
            InjuryDTO inj = injuries.findAllByAthleteIdAndClearedAtIsNull(a.getId()).stream().findFirst().map(i -> new InjuryDTO(i.getInjuryType(), i.getExpectedReturnDate())).orElse(null);
            physical = new PhysicalDTO(ts == null ? 100 : ts.getReadiness(), ts == null ? BigDecimal.ONE : ts.acwr(), ps.name(), ps.label(), ps.emoji(), ps.cssClass(), inj);
            sensitive = ps == PhysicalStatus.INJURED;
        }
        List<BadgeDTO> top = BadgeAwardService.top3(badges.findAllByAthleteIdAndActiveTrue(a.getId()), canSeeMedical).stream().map(BadgeAwardService::toDto).toList();
        AuthorDTO author = AuthorDTO.of(a, false);
        MediaDTO hero = a.getAvatarKey() == null ? null : new MediaDTO(a.getAvatarKey(), storage.mediaUrl(a.getAvatarKey()), "image");
        int likes = (int) Math.min(Integer.MAX_VALUE, score / 2L); // placeholder determinístico até existir grafo social
        return new AthleteCardDTO(a.getId(), "@" + a.getUsername(), a.getDisplayName(), author, "GÊMEO DIGITAL", VslGlyph.IDENTIDADE.glyph(),
                RarityDTO.of(tier, score, pp, rp, mp), attributes(a, twin), seasonDto, physical, top,
                new ClassificationDTO(AgeRatingDTO.of(sensitive ? AgeRating.SENSITIVE : AgeRating.SAFE), sensitive),
                new SocialCountersDTO(likes, 0, 0, 0, false, false), hero);
    }

    /** Lista de atributos como badges (nunca texto corrido): posição · idade · clube · contrato · UF · pé · altura · referência · ranking. */
    public List<AttributeChipDTO> attributes(Athlete a, DigitalTwin twin) {
        List<AttributeChipDTO> out = new ArrayList<>();
        if (a.getPosition() != null) out.add(new AttributeChipDTO("◎", capitalize(a.getPosition().name()), "pos"));
        if (a.age() != null) out.add(new AttributeChipDTO("", a.age() + " anos", "age"));
        if (a.getCurrentClubName() != null) out.add(new AttributeChipDTO("⬢", a.getCurrentClubName(), "club"));
        boolean free = a.getContractStatus() == ContractStatus.AGENTE_LIVRE;
        out.add(new AttributeChipDTO("", free ? "Agente livre" : "Contratado", free ? "free" : "signed"));
        if (a.getStateCode() != null) out.add(new AttributeChipDTO("", a.getStateCode(), "state"));
        if (a.getDominantFoot() != null) out.add(new AttributeChipDTO("", "Pé " + a.getDominantFoot(), "foot"));
        if (a.getHeightCm() != null) out.add(new AttributeChipDTO("", a.getHeightCm() + " cm", "height"));
        if (a.getCategoria() != null) out.add(new AttributeChipDTO("", a.getCategoria(), "category"));
        if (twin != null && twin.getStatus().countsForRanking()) {
            switch (twin.getRarityTier()) {
                case MYTHIC -> out.add(new AttributeChipDTO("✦", "Referência nacional", "ref"));
                case LEGENDARY -> out.add(new AttributeChipDTO("✦", "Referência estadual", "ref"));
                case EPIC -> out.add(new AttributeChipDTO("✦", "Destaque regional", "ref"));
                default -> { }
            }
            long total = twins.countByStatus(TwinStatus.ACTIVE);
            if (total >= 10) {
                long above = twins.countByStatusAndScoreTotalGreaterThan(TwinStatus.ACTIVE, twin.getScoreTotal());
                int pct = (int) Math.max(1, Math.ceil(100.0 * (above + 1) / total));
                if (pct <= 25) out.add(new AttributeChipDTO("✦", "Top " + (pct <= 1 ? 1 : pct <= 5 ? 5 : pct <= 10 ? 10 : 25) + "% do ranking", "rank"));
            }
        }
        return out;
    }

    private static String capitalize(String s) { return s.charAt(0) + s.substring(1).toLowerCase(); }
}
