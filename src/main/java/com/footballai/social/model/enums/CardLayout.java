package com.footballai.social.model.enums;

/** Layout da arte: onde o bloco de arte aparece no card. */
public enum CardLayout {
    CLASSICO("Clássico"), HERO("Hero"), SPLIT("Split"), MINIMAL("Minimal");
    private final String label;
    CardLayout(String label) { this.label = label; }
    public String label() { return label; }
}
