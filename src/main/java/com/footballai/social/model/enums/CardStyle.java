package com.footballai.social.model.enums;

/** Estilo gráfico da arte — coerente com o sistema visual (vitral do logo, neon verde, ouro elite). */
public enum CardStyle {
    NEON("Neon"), VITRAL("Vitral"), GRAFITE("Grafite"), OURO("Ouro"), RETRO("Retrô");
    private final String label;
    CardStyle(String label) { this.label = label; }
    public String label() { return label; }
}
