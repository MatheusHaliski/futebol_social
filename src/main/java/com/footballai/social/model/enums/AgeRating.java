package com.footballai.social.model.enums;

/** Classificação de público exibida no age-chip (camada Velion). */
public enum AgeRating {
    SAFE("SAFE", "age-safe", 0),
    KIDS("KIDS", "age-kids", 0),
    AGE_12("12+", "age-12", 12),
    AGE_16("16+", "age-16", 16),
    AGE_18("18+", "age-18", 18),
    SENSITIVE("SENSÍVEL", "age-sens", 0);

    private final String label;
    private final String cssClass;
    private final int minAge;

    AgeRating(String label, String cssClass, int minAge) { this.label = label; this.cssClass = cssClass; this.minAge = minAge; }
    public String label() { return label; }
    public String cssClass() { return cssClass; }
    public int minAge() { return minAge; }
}
