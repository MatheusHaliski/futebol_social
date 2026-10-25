package com.footballai.social.model.enums;

/** Tipos de notificação pessoal (central do sino). Nunca carregam dado sensível (Art. 11). */
public enum NotificationType {
    LIKE("♡"), COMMENT("💬"), REPLY("↩"), FOLLOW("＋"), FLAIR_RESULT("⚔️"), TIER_CHANGE("◈"), SYSTEM("ℹ");

    private final String glyph;
    NotificationType(String glyph) { this.glyph = glyph; }
    public String glyph() { return glyph; }
}
