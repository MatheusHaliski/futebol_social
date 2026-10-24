package com.footballai.social.controller.dto;

import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.AgeRating;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.model.enums.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato REST ↔ SPA dos cards (ver docs/ENTITY_TAXONOMY.md §15 e docs/CARD_ANATOMY.md).
 * O front renderiza apenas a partir destes records — nenhuma regra visual é recalculada no cliente.
 */
public final class CardDtos {
    private CardDtos() { }

    /** Bolinha do autor: papel → cor/classe; ✦ verificado; rótulo IA. */
    public record AuthorDTO(UUID id, String handle, String displayName, String role, String roleLabel, String dotClass, String ringColor,
                            boolean verified, boolean aiGenerated) {
        public static AuthorDTO of(User u, boolean aiGenerated) {
            UserRole r = u.getRole();
            return new AuthorDTO(u.getId(), "@" + u.getUsername(), u.getDisplayName(), r.name(), r.label(), r.dotClass(), r.ringColor(), u.isVerified(), aiGenerated || r.isAutomated());
        }
        public static AuthorDTO unknown(UUID id, UserRole r) {
            return new AuthorDTO(id, "@usuario", "Usuário", r.name(), r.label(), r.dotClass(), r.ringColor(), false, r.isAutomated());
        }
    }

    public record AgeRatingDTO(String value, String label, String cssClass) {
        public static AgeRatingDTO of(AgeRating a) { return new AgeRatingDTO(a.name(), a.label(), a.cssClass()); }
    }

    public record SocialCountersDTO(int likes, int comments, int shares, int saves, boolean likedByMe, boolean savedByMe) { }
    public record TrendDTO(String direction, String emoji) { }
    public record MediaDTO(String key, String url, String kind) { }
    /** Arte de fundo escolhida no criador de cards (cor · material · layout · estilo). */
    public record CardArtDTO(String color, String material, String materialLabel, String layout, String layoutLabel, String style, String styleLabel) {
        public static CardArtDTO of(com.footballai.social.model.entity.CardArt a) {
            if (a == null || a.isEmpty()) return null;
            return new CardArtDTO(a.getColor(), a.getMaterial() == null ? null : a.getMaterial().name(), a.getMaterial() == null ? null : a.getMaterial().label(),
                    a.getLayout() == null ? null : a.getLayout().name(), a.getLayout() == null ? null : a.getLayout().label(),
                    a.getStyle() == null ? null : a.getStyle().name(), a.getStyle() == null ? null : a.getStyle().label());
        }
    }

    /** Card de post (.pcard): título obrigatório, teaser, sem texto corrido; barra de ações em todo post. */
    public record PostCardDTO(UUID id, Instant publishedAt, String timeLabel, AuthorDTO author,
                              String contentType, String borderClass, String borderColor, String badgeLabel, String vslGlyph, boolean hazard,
                              AgeRatingDTO ageRating, String title, String subtitle, MediaDTO media, List<String> meta,
                              SocialCountersDTO social, TrendDTO trend, String window, String severity, String alertType,
                              String generationMode, CardArtDTO art) { }

    public record PostViewDTO(PostCardDTO card, String content) { }

    /** Raridade: o tier nunca é texto na UI — vai o brasão (crestIcon). PP/RP/MP são projeções do último snapshot. */
    public record RarityDTO(String tier, String crestIcon, String color, String visualEffect, int score, int pp, int rp, int mp) {
        public static RarityDTO of(RarityTier t, int score, int pp, int rp, int mp) {
            return new RarityDTO(t.name(), t.crestIcon(), t.color(), t.visualEffect(), score, pp, rp, mp);
        }
    }

    public record SeasonStatsDTO(String season, int games, int goals, int assists, int minutes, int yellow, int red, Integer cleanSheets, Integer tackles, BigDecimal avgRating) { }
    public record InjuryDTO(String type, LocalDate expectedReturn) { }
    public record PhysicalDTO(int readiness, BigDecimal acwr, String status, String statusLabel, String emoji, String cssClass, InjuryDTO injury) { }
    public record BadgeDTO(String type, String label, String name, String emoji, String color, String category, int counter, String season, Instant awardedAt) { }

    /** Atributo do card: sempre item/badge (nunca texto corrido). kind: pos · age · club · signed · free · state · foot · height · ref · rank. */
    public record AttributeChipDTO(String icon, String label, String kind) { }
    public record ClassificationDTO(AgeRatingDTO ageRating, boolean sensitive) { }

    /** Card de atleta (.vcard): hero limpo, janela segmentada Temporada · Atributos, badges na área inferior. */
    public record AthleteCardDTO(UUID athleteId, String handle, String name, AuthorDTO author, String context, String vslGlyph,
                                 RarityDTO rarity, List<AttributeChipDTO> attributes, SeasonStatsDTO season, PhysicalDTO physical,
                                 List<BadgeDTO> topBadges, ClassificationDTO classification, SocialCountersDTO social, MediaDTO heroImage) { }

    public record FeedPageDTO(String window, String sort, List<PostCardDTO> posts, List<AthleteCardDTO> athletes, String nextCursor,
                              Map<String, Object> appliedFilters, String source) { }
}
