package com.footballai.social.model.enums;

/** Material das barras de navegação (topo, lateral, barra inferior): opacidade, desfoque e textura. */
public enum UiMaterial {
    FOSCO("Fosco", "Sólido, sem transparência"), VIDRO("Vidro", "Translúcido com desfoque"), METALICO("Metálico", "Brilho escovado"),
    CARBONO("Carbono", "Trama de fibra discreta"), GRAMADO("Gramado", "Faixas de corte do campo");
    private final String label, hint;
    UiMaterial(String label, String hint) { this.label = label; this.hint = hint; }
    public String label() { return label; }
    public String hint() { return hint; }
}
