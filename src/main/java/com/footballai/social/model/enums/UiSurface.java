package com.footballai.social.model.enums;

/** Tom das barras laterais e painéis. SAND é uma superfície clara. */
public enum UiSurface {
    GRAPHITE("Grafite", "#131b17", false), FOREST("Floresta", "#0f241a", false), NAVY("Marinho", "#0f192b", false), PLUM("Ameixa", "#1f1529", false),
    CARVAO("Carvão", "#16181a", false), VINHO("Vinho", "#24131a", false), SAND("Areia", "#fbf7ee", true);
    private final String label, hex; private final boolean light;
    UiSurface(String label, String hex, boolean light) { this.label = label; this.hex = hex; this.light = light; }
    public String label() { return label; }
    public String hex() { return hex; }
    public boolean light() { return light; }
}
