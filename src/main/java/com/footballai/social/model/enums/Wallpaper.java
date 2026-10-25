package com.footballai.social.model.enums;

/** Tela de fundo: grade de ícones de futebol com holofote, em cinco paletas, ou arquivo do usuário (CUSTOM). */
public enum Wallpaper {
    NONE("Sem fundo", null), VERDE("Verde", "verde.svg"), CIANO("Ciano", "ciano.svg"), OURO("Ouro", "ouro.svg"),
    VERDE_CLARO("Verde-claro", "verde-claro.svg"), VERDE_VIVO("Verde-vivo", "verde-vivo.svg"), CUSTOM("Personalizado", null);
    private final String label, file;
    Wallpaper(String label, String file) { this.label = label; this.file = file; }
    public String label() { return label; }
    /** Caminho público do asset (nulo para NONE/CUSTOM). */
    public String url() { return file == null ? null : "/brand/wallpapers/" + file; }
}
