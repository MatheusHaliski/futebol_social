package com.footballai.social.model.enums;

/** Gradiente ambiente do layout de tela (fundo atrás do conteúdo), derivado da cor de destaque. */
public enum UiGradient {
    NENHUM("Nenhum", "Fundo liso"), HOLOFOTE("Holofote", "Luz do estádio no topo"), ARQUIBANCADA("Arquibancada", "Faixas diagonais da torcida"),
    CREPUSCULO("Crepúsculo", "Jogo ao entardecer");
    private final String label, hint;
    UiGradient(String label, String hint) { this.label = label; this.hint = hint; }
    public String label() { return label; }
    public String hint() { return hint; }
}
