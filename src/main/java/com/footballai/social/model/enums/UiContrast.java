package com.footballai.social.model.enums;

/** Contraste da interface. ALTO reforça textos secundários, bordas e foco (WCAG AAA onde possível). */
public enum UiContrast {
    PADRAO("Padrão"), ALTO("Alto");
    private final String label;
    UiContrast(String label) { this.label = label; }
    public String label() { return label; }
}
