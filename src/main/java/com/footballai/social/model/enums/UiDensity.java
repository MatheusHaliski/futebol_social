package com.footballai.social.model.enums;

/** Densidade do layout: espaçamentos e raios de cantos. */
public enum UiDensity {
    COMPACTA("Compacta"), CONFORTAVEL("Confortável"), ESPACOSA("Espaçosa");
    private final String label;
    UiDensity(String label) { this.label = label; }
    public String label() { return label; }
}
