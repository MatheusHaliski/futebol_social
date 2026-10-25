package com.footballai.social.model.enums;

/** Estilo gráfico (padronagem) da arte — coerente com o sistema visual (vitral do logo, neon verde, ouro elite, campo). */
public enum CardStyle {
    NEON("Neon", false), VITRAL("Vitral", false), GRAFITE("Grafite", false), OURO("Ouro", false), RETRO("Retrô", false),
    GRAMADO("Gramado", false), REDE("Rede do gol", false), LISTRAS("Listras de camisa", true), FAIXA("Faixa diagonal", true),
    XADREZ("Xadrez", false), PRANCHETA("Prancheta tática", false), ARQUIBANCADA("Arquibancada", false), MOSAICO("Mosaico da torcida", true);
    private final String label; private final boolean needsColor2;
    CardStyle(String label, boolean needsColor2) { this.label = label; this.needsColor2 = needsColor2; }
    public String label() { return label; }
    public boolean needsColor2() { return needsColor2; }
}
