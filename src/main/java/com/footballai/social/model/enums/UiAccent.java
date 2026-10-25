package com.footballai.social.model.enums;

/** Cor de destaque da interface (barras ativas, botões, brilhos). Não altera as cores de identidade dos papéis. */
public enum UiAccent {
    GREEN("Verde campo", "#00d47a"), BLUE("Azul", "#3b82f6"), GOLD("Ouro", "#f5c542"), PURPLE("Roxo", "#a06bff"), ORANGE("Laranja", "#ff9f43"),
    CIANO("Ciano", "#22d3ee"), PRATA("Prata", "#c7d2cc");
    private final String label, hex;
    UiAccent(String label, String hex) { this.label = label; this.hex = hex; }
    public String label() { return label; }
    public String hex() { return hex; }
}
