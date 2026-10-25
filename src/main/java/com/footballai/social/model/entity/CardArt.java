package com.footballai.social.model.entity;

import com.footballai.social.model.enums.CardGradient;
import com.footballai.social.model.enums.CardLayout;
import com.footballai.social.model.enums.CardMaterial;
import com.footballai.social.model.enums.CardStyle;
import com.footballai.social.model.enums.PostContentType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Set;

/**
 * Arte de fundo do card: cor principal e segunda cor (hex), gradiente, material, layout e estilo — etapa "Arte" do criador de cards.
 * A arte é só fundo: nunca tinge a fotografia do usuário nem substitui a borda do tipo de conteúdo.
 */
@Embeddable
public class CardArt {
    /** Cards de alerta (lesão, comunicado institucional) só aceitam arte sóbria: sem brilho, holografia nem padronagem festiva. */
    public static final Set<CardGradient> SOBER_GRADIENTS = Set.of(CardGradient.SUAVE, CardGradient.SOLIDO);
    public static final Set<CardMaterial> SOBER_MATERIALS = Set.of(CardMaterial.FOSCO, CardMaterial.MALHA);
    public static final Set<CardStyle> SOBER_STYLES = Set.of(CardStyle.GRAFITE);

    @Column(name = "art_color", length = 7) private String color;
    @Column(name = "art_color2", length = 7) private String color2;
    @Enumerated(EnumType.STRING) @Column(name = "art_gradient", length = 16) private CardGradient gradient;
    @Enumerated(EnumType.STRING) @Column(name = "art_material", length = 16) private CardMaterial material;
    @Enumerated(EnumType.STRING) @Column(name = "art_layout", length = 16) private CardLayout layout;
    @Enumerated(EnumType.STRING) @Column(name = "art_style", length = 16) private CardStyle style;

    public CardArt() { }
    public CardArt(String color, CardMaterial material, CardLayout layout, CardStyle style) {
        this(color, null, null, material, layout, style);
    }
    public CardArt(String color, String color2, CardGradient gradient, CardMaterial material, CardLayout layout, CardStyle style) {
        this.color = normalize(color); this.color2 = normalize(color2); this.gradient = gradient;
        this.material = material; this.layout = layout; this.style = style;
        // Gradientes de duas cores e padronagens bicolores sem segunda cor usam a principal (monocromático), nunca uma cor inventada.
        if (this.color2 == null && needsColor2()) this.color2 = this.color;
    }

    public static String normalize(String hex) {
        if (hex == null || hex.isBlank()) return null;
        String h = hex.trim().toLowerCase();
        if (!h.matches("^#[0-9a-f]{6}$")) throw new IllegalArgumentException("Cor da arte deve ser hex #rrggbb");
        return h;
    }

    public static boolean soberType(PostContentType type) {
        return type == PostContentType.LESAO_ALERTA || type == PostContentType.INSTITUCIONAL;
    }

    /** Valida a arte para o tipo do card; lança IllegalArgumentException com a regra violada. */
    public CardArt validateFor(PostContentType type) {
        if (!soberType(type)) return this;
        if (gradient != null && !SOBER_GRADIENTS.contains(gradient)) throw new IllegalArgumentException("Cards de alerta usam gradiente Suave ou Sólido");
        if (material != null && !SOBER_MATERIALS.contains(material)) throw new IllegalArgumentException("Cards de alerta usam material Fosco ou Malha");
        if (style != null && !SOBER_STYLES.contains(style)) throw new IllegalArgumentException("Cards de alerta usam o estilo Grafite");
        return this;
    }

    /** Versão sóbria para exibição (cards de alerta antigos com arte festiva não aparecem com brilho no feed). */
    public CardArt soberFor(PostContentType type) {
        if (!soberType(type) || isEmpty()) return this;
        CardArt a = new CardArt();
        a.color = color; a.color2 = color2; a.layout = layout;
        a.gradient = gradient == null || SOBER_GRADIENTS.contains(gradient) ? gradient : CardGradient.SOLIDO;
        a.material = material == null || SOBER_MATERIALS.contains(material) ? material : CardMaterial.FOSCO;
        a.style = style == null || SOBER_STYLES.contains(style) ? style : CardStyle.GRAFITE;
        return a;
    }

    public boolean needsColor2() {
        return (gradient != null && gradient.needsColor2()) || (style != null && style.needsColor2());
    }

    public boolean isEmpty() { return color == null && material == null && layout == null && style == null && gradient == null; }
    public String getColor() { return color; }
    public String getColor2() { return color2; }
    public CardGradient getGradient() { return gradient; }
    public CardMaterial getMaterial() { return material; }
    public CardLayout getLayout() { return layout; }
    public CardStyle getStyle() { return style; }
}
