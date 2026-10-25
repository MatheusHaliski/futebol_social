package com.footballai.social.model.enums;

/** Badges "Athletic Persona" (RF4.1). Prioridade menor = mais importante no topo dos 3 do card. */
public enum BadgeType {
    // Estatísticas da temporada (quantitativas)
    ARTILHEIRO(BadgeCategory.SEASON_STATS, "⚽", "#f5c542", 30, true, "Chuteira de Ouro"),
    GARCOM(BadgeCategory.SEASON_STATS, "🎯", "#00d47a", 31, true, "Rei das Assistências"),
    PAREDAO(BadgeCategory.SEASON_STATS, "🧱", "#3b82f6", 32, true, "Paredão"),
    XERIFAO(BadgeCategory.SEASON_STATS, "🧱", "#3b82f6", 32, true, "Xerifão"),
    MOTORZINHO(BadgeCategory.SEASON_STATS, "⏱️", "#95a89d", 40, true, "Motorzinho"),
    // Condição física (mutuamente exclusivos)
    PRONTO_PRA_JOGO(BadgeCategory.PHYSICAL_STATUS, "⚡", "#00d47a", 10, false, "Pronto pra jogo"),
    DEPARTAMENTO_MEDICO(BadgeCategory.PHYSICAL_STATUS, "🩹", "#ff5d5d", 1, false, "Departamento médico"),
    RISCO_FADIGA(BadgeCategory.PHYSICAL_STATUS, "⚠️", "#f7c948", 5, false, "Risco de fadiga"),
    // Sociais, táticos e gamificados
    MVP(BadgeCategory.SOCIAL_TACTICAL, "🏆", "#f5c542", 20, true, "MVP"),
    MESTRE_TATICO(BadgeCategory.SOCIAL_TACTICAL, "🧠", "#a06bff", 25, false, "Mestre tático"),
    EM_CHAMAS(BadgeCategory.SOCIAL_TACTICAL, "🔥", "#ff9f43", 21, true, "Em chamas"),
    LIDER_CAPITAO(BadgeCategory.SOCIAL_TACTICAL, "👑", "#f5c542", 26, false, "Líder / Capitão"),
    FAIR_PLAY(BadgeCategory.SOCIAL_TACTICAL, "🛡️", "#2dd4bf", 35, false, "Fair play");

    private final BadgeCategory category;
    private final String emoji;
    private final String color;
    private final int priority;
    private final boolean quantitative;
    private final String label;

    BadgeType(BadgeCategory category, String emoji, String color, int priority, boolean quantitative, String label) {
        this.category = category; this.emoji = emoji; this.color = color; this.priority = priority;
        this.quantitative = quantitative; this.label = label;
    }

    public BadgeCategory category() { return category; }
    public String emoji() { return emoji; }
    public String color() { return color; }
    public int priority() { return priority; }
    public boolean quantitative() { return quantitative; }
    public String label() { return label; }
    public boolean isPhysical() { return category == BadgeCategory.PHYSICAL_STATUS; }
}
