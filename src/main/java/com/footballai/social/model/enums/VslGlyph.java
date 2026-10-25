package com.footballai.social.model.enums;

/** Velion Symbol Language — glifo exibido no topo do card. */
public enum VslGlyph {
    IDENTIDADE("◇"), VALOR("✦"), COMUNIDADE("⬢"), INSTITUICAO("▭");

    private final String glyph;
    VslGlyph(String glyph) { this.glyph = glyph; }
    public String glyph() { return glyph; }
}
