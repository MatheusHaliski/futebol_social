package com.footballai.social.model.enums;

/** Material da arte de fundo do card (etapa 3 do criador). */
public enum CardMaterial {
    FOSCO("Fosco"), METALICO("Metálico"), VIDRO("Vidro"), CARBONO("Carbono"), HOLOGRAFICO("Holográfico");
    private final String label;
    CardMaterial(String label) { this.label = label; }
    public String label() { return label; }
}
