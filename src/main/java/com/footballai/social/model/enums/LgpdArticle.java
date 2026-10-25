package com.footballai.social.model.enums;

/** Base legal exibida nas pills da Central de Privacidade. */
public enum LgpdArticle {
    ART_6_III("Art. 6º, III — necessidade"), ART_6_IV("Art. 6º, IV — livre acesso"), ART_6_VI("Art. 6º, VI — transparência"),
    ART_6_X("Art. 6º, X — responsabilização"), ART_7_I("Art. 7º, I — consentimento"), ART_7_V("Art. 7º, V — execução de contrato"),
    ART_8_P5("Art. 8º, §5º — revogação"), ART_11("Art. 11 — dado sensível"), ART_14("Art. 14 — crianças e adolescentes"),
    ART_18_I_II("Art. 18, I e II — confirmação e acesso"), ART_18_III("Art. 18, III — correção"), ART_18_V("Art. 18, V — portabilidade"),
    ART_18_VI("Art. 18, VI — eliminação"), ART_18_VII("Art. 18, VII — compartilhamento"), ART_18_IX("Art. 18, IX — revogação"),
    ART_20("Art. 20 — revisão de decisão automatizada");

    private final String label;
    LgpdArticle(String label) { this.label = label; }
    public String label() { return label; }
}
