package com.footballai.social.model.enums;

/** Família tipográfica da interface (títulos, texto e números). Todas auto-hospedadas sob SIL OFL 1.1. */
public enum UiFont {
    PADRAO("Padrão", "Space Grotesk + Inter"), LEGIVEL("Legível", "Atkinson Hyperlegible"), PLACAR("Placar", "Barlow Condensed + Barlow"),
    EDITORIAL("Editorial", "Source Serif 4 + Source Sans 3"), SISTEMA("Sistema", "Fonte do aparelho");
    private final String label, family;
    UiFont(String label, String family) { this.label = label; this.family = family; }
    public String label() { return label; }
    public String family() { return family; }
}
