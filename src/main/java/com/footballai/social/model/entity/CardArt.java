package com.footballai.social.model.entity;

import com.footballai.social.model.enums.CardLayout;
import com.footballai.social.model.enums.CardMaterial;
import com.footballai.social.model.enums.CardStyle;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Arte de fundo do card: cor (hex), material, layout e estilo — etapa "Arte" do criador de cards. */
@Embeddable
public class CardArt {
    @Column(name = "art_color", length = 7) private String color;
    @Enumerated(EnumType.STRING) @Column(name = "art_material", length = 16) private CardMaterial material;
    @Enumerated(EnumType.STRING) @Column(name = "art_layout", length = 16) private CardLayout layout;
    @Enumerated(EnumType.STRING) @Column(name = "art_style", length = 16) private CardStyle style;

    public CardArt() { }
    public CardArt(String color, CardMaterial material, CardLayout layout, CardStyle style) {
        this.color = normalize(color); this.material = material; this.layout = layout; this.style = style;
    }

    public static String normalize(String hex) {
        if (hex == null) return null;
        String h = hex.trim().toLowerCase();
        if (!h.matches("^#[0-9a-f]{6}$")) throw new IllegalArgumentException("Cor da arte deve ser hex #rrggbb");
        return h;
    }

    public boolean isEmpty() { return color == null && material == null && layout == null && style == null; }
    public String getColor() { return color; }
    public CardMaterial getMaterial() { return material; }
    public CardLayout getLayout() { return layout; }
    public CardStyle getStyle() { return style; }
}
