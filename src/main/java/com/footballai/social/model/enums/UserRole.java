package com.footballai.social.model.enums;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Papel do usuário (RF2). Cada papel carrega a cor do anel/bolinha de identidade —
 * tokens do modelo visual Football AI · Social (contraste WCAG medido sobre {@code --ink #0a0f0c}).
 */
public enum UserRole {
    ATLETA("#00d47a", "dc-atleta", "atleta", Kind.HUMAN),
    @JsonAlias("TECNICO_STAFF")
    STAFF("#7fb6ff", "dc-staff", "comissão técnica", Kind.HUMAN),
    STAFF_SAUDE("#e11d48", "dc-saude", "staff de saúde", Kind.HUMAN),
    CLUBE("#3b82f6", "dc-clube", "clube", Kind.ORGANIZATION),
    SCOUT("#f7c948", "dc-scout", "scout", Kind.HUMAN),
    AGENTE("#2dd4bf", "dc-agente", "agente", Kind.HUMAN),
    MARCA("#a06bff", "dc-marca", "marca", Kind.ORGANIZATION),
    IMPRENSA("#ff9f43", "dc-imprensa", "imprensa", Kind.HUMAN),
    OFICIAL_LIGA("#f5c542", "dc-oficial", "oficial de liga", Kind.ORGANIZATION),
    TORCEDOR("#f472b6", "dc-torcedor", "torcedor", Kind.HUMAN),
    AGENTE_IA("#22d3ee", "dc-ia", "agente de IA", Kind.AUTOMATED),
    /** Reservado — comportamento registrado via RoleRegistry. */
    ARBITRO("#95a89d", "dc-arbitro", "árbitro", Kind.HUMAN),
    /** Reservado — conta coletiva de torcida (Lei 14.597/2023). */
    TORCIDA_ORGANIZADA("#ec4899", "dc-torcida", "torcida organizada", Kind.ORGANIZATION);

    private enum Kind { HUMAN, ORGANIZATION, AUTOMATED }

    private final String ringColor;
    private final String dotClass;
    private final String label;
    private final Kind kind;

    UserRole(String ringColor, String dotClass, String label, Kind kind) {
        this.ringColor = ringColor;
        this.dotClass = dotClass;
        this.label = label;
        this.kind = kind;
    }

    public String ringColor() { return ringColor; }
    public String dotClass() { return dotClass; }
    public String label() { return label; }
    public boolean isOrganization() { return kind == Kind.ORGANIZATION; }
    public boolean isHuman() { return kind == Kind.HUMAN; }
    public boolean isAutomated() { return kind == Kind.AUTOMATED; }
    /** Só atletas possuem Gêmeo Digital e entram no Rarity Engine. */
    public boolean hasDigitalTwin() { return this == ATLETA; }

    /** Tipos de conteúdo que o papel pode publicar (RoleCapability simplificado). */
    public boolean canPublish(PostContentType type, VerificationLevel level) {
        return switch (type) {
            case TREINO, PARTIDA_MOMENTO -> this == ATLETA || this == STAFF || this == CLUBE;
            case RELATORIO_TATICO -> this == SCOUT || this == STAFF || this == IMPRENSA || this == AGENTE_IA;
            case LIGA -> this == OFICIAL_LIGA || this == CLUBE || this == IMPRENSA;
            case INSTITUCIONAL -> isOrganization() || this == STAFF || this == AGENTE;
            case LESAO_ALERTA -> this == ATLETA || (this == STAFF_SAUDE && level != null && level.countsForRarity());
            case ESPECIAL -> this == OFICIAL_LIGA || this == CLUBE || this == AGENTE_IA;
        };
    }
}
