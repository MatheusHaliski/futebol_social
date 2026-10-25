package com.footballai.social.model.enums;

/**
 * Borda de conteúdo do card (RF3). Cor, classe CSS, glifo VSL, janela padrão e sensibilidade (Art. 11).
 */
public enum PostContentType {
    TREINO("#00d47a", "cc-treino", VslGlyph.IDENTIDADE, FeedWindow.GEMEO, "TREINO", false),
    PARTIDA_MOMENTO("#3b82f6", "cc-partida", VslGlyph.IDENTIDADE, FeedWindow.PERFIL, "PARTIDA", false),
    RELATORIO_TATICO("#ff9f43", "cc-scouting", VslGlyph.INSTITUICAO, FeedWindow.SCOUTING, "SCOUTING", false),
    LIGA("#a06bff", "cc-liga", VslGlyph.COMUNIDADE, FeedWindow.PERFIL, "LIGA", false),
    INSTITUCIONAL("#ff5d5d", "cc-institucional", VslGlyph.INSTITUICAO, FeedWindow.ALERTA, "INSTITUCIONAL", false),
    LESAO_ALERTA("#ff5d5d", "cc-institucional", VslGlyph.INSTITUICAO, FeedWindow.ALERTA, "LESÃO", true),
    ESPECIAL("#f5c542", "cc-especial", VslGlyph.VALOR, FeedWindow.PERFIL, "ESPECIAL", false);

    private final String borderColor;
    private final String cssClass;
    private final VslGlyph vslGlyph;
    private final FeedWindow defaultWindow;
    private final String badgeLabel;
    private final boolean sensitive;

    PostContentType(String borderColor, String cssClass, VslGlyph vslGlyph, FeedWindow defaultWindow, String badgeLabel, boolean sensitive) {
        this.borderColor = borderColor; this.cssClass = cssClass; this.vslGlyph = vslGlyph;
        this.defaultWindow = defaultWindow; this.badgeLabel = badgeLabel; this.sensitive = sensitive;
    }

    public String borderColor() { return borderColor; }
    public String cssClass() { return cssClass; }
    public VslGlyph vslGlyph() { return vslGlyph; }
    public FeedWindow defaultWindow() { return defaultWindow; }
    public String badgeLabel() { return badgeLabel; }
    public boolean sensitive() { return sensitive; }
    /** Alertas recebem a listra de risco (hazard) no card. */
    public boolean hazard() { return defaultWindow == FeedWindow.ALERTA; }
}
