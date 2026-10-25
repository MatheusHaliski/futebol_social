package com.footballai.social.model.enums;

/** Gradiente da arte de fundo do card. DEGRADE, RADIAL e DIAGONAL combinam a cor principal com a segunda cor. */
public enum CardGradient {
    SUAVE("Suave", false), SOLIDO("Sólido", false), DEGRADE("Degradê", true), RADIAL("Radial", true), DIAGONAL("Diagonal", true);
    private final String label; private final boolean needsColor2;
    CardGradient(String label, boolean needsColor2) { this.label = label; this.needsColor2 = needsColor2; }
    public String label() { return label; }
    public boolean needsColor2() { return needsColor2; }
}
