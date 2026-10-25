package com.footballai.social.model.enums;

/** Estado físico do Gêmeo, derivado de telemetria + registro médico (dado sensível, Art. 11). */
public enum PhysicalStatus {
    READY(BadgeType.PRONTO_PRA_JOGO, "PRONTO PRA JOGO", "⚡", "ok"),
    FATIGUE_RISK(BadgeType.RISCO_FADIGA, "RISCO DE FADIGA", "⚠️", "warn"),
    INJURED(BadgeType.DEPARTAMENTO_MEDICO, "DEPARTAMENTO MÉDICO", "🩹", "bad");

    private final BadgeType badge;
    private final String label;
    private final String emoji;
    private final String cssClass;

    PhysicalStatus(BadgeType badge, String label, String emoji, String cssClass) {
        this.badge = badge; this.label = label; this.emoji = emoji; this.cssClass = cssClass;
    }

    public BadgeType badge() { return badge; }
    public String label() { return label; }
    public String emoji() { return emoji; }
    public String cssClass() { return cssClass; }
    public boolean blocksCallUp() { return this == INJURED; }
}
