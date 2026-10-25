package com.footballai.social.model.enums;

/** Indicador de tendência do post (views/interações nas últimas 24 h). */
public enum TrendDirection {
    HOT("🔥"), UP("📈"), FLAT("➖"), DOWN("📉");

    private final String emoji;
    TrendDirection(String emoji) { this.emoji = emoji; }
    public String emoji() { return emoji; }

    /** Δ relativo do trendingScore em 24 h. */
    public static TrendDirection fromDelta(double delta) {
        if (delta >= 1.0) return HOT;
        if (delta >= 0.15) return UP;
        if (delta <= -0.15) return DOWN;
        return FLAT;
    }
}
