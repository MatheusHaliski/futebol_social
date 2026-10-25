package com.footballai.social.model.enums;

/** Preferências de consentimento (todas DEFAULT FALSE — opt-in explícito). */
public enum LgpdPreference {
    SHARE_TELEMETRY_WITH_CLUBS(LgpdArticle.ART_7_I, false),
    SHARE_TELEMETRY_WITH_SCOUTS(LgpdArticle.ART_7_I, false),
    SHARE_MEDICAL_DATA(LgpdArticle.ART_11, true),
    SHARE_BIOMETRIC_DATA(LgpdArticle.ART_11, true),
    LOCATION_HISTORY(LgpdArticle.ART_7_I, true),
    SHARE_WITH_BRANDS(LgpdArticle.ART_7_I, false),
    PERSONALIZED_ADS(LgpdArticle.ART_7_I, false),
    AI_AGENT_PROCESSING(LgpdArticle.ART_20, false);

    private final LgpdArticle article;
    private final boolean sensitive;
    LgpdPreference(LgpdArticle article, boolean sensitive) { this.article = article; this.sensitive = sensitive; }
    public LgpdArticle article() { return article; }
    /** Preferências sensíveis exigem reautenticação (step-up) para ativar. */
    public boolean sensitive() { return sensitive; }
}
