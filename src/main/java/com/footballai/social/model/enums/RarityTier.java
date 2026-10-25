package com.footballai.social.model.enums;

import java.math.BigDecimal;

/**
 * Tiers de raridade do Gêmeo Digital (RF4/CORE). Faixas fixas de Score_Total; o tier nunca é escrito
 * em texto na UI — cada tier tem um brasão único ({@link #crestIcon()}).
 */
public enum RarityTier {
    COMMON(0, 400, "border-default", "#5c6b62", "ic-tier-common", "1.00"),
    RARE(401, 900, "glow-blue", "#3b82f6", "ic-tier-rare", "1.05"),
    EPIC(901, 1600, "particles-purple", "#a06bff", "ic-tier-epic", "1.10"),
    LEGENDARY(1601, 2400, "frame-white-gold", "#f5c542", "ic-tier-legendary", "1.20"),
    MYTHIC(2401, Integer.MAX_VALUE, "aura-dynamic", "#f5c542", "ic-tier-mythic", "1.30");

    private final int minScore;
    private final int maxScore;
    private final String visualEffect;
    private final String color;
    private final String crestIcon;
    private final BigDecimal flairMultiplier;

    RarityTier(int minScore, int maxScore, String visualEffect, String color, String crestIcon, String flairMultiplier) {
        this.minScore = minScore; this.maxScore = maxScore; this.visualEffect = visualEffect;
        this.color = color; this.crestIcon = crestIcon; this.flairMultiplier = new BigDecimal(flairMultiplier);
    }

    public int minScore() { return minScore; }
    public int maxScore() { return maxScore; }
    public String visualEffect() { return visualEffect; }
    public String color() { return color; }
    public String crestIcon() { return crestIcon; }
    public BigDecimal flairMultiplier() { return flairMultiplier; }

    public static RarityTier fromScore(int score) {
        if (score < 0) throw new IllegalArgumentException("Score_Total não pode ser negativo: " + score);
        for (RarityTier t : values()) {
            if (score >= t.minScore && score <= t.maxScore) return t;
        }
        return MYTHIC;
    }
}
