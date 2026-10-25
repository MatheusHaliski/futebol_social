package com.footballai.social.model.enums;

import java.util.EnumSet;
import java.util.Set;

/** Máquina de estados do Gêmeo Digital: DRAFT → ACTIVE → SUSPENDED → PENDING_DELETE → ANONYMIZED | DELETED. */
public enum TwinStatus {
    DRAFT, ACTIVE, SUSPENDED, PENDING_DELETE, ANONYMIZED, DELETED;

    public Set<TwinStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> EnumSet.of(ACTIVE, DELETED);
            case ACTIVE -> EnumSet.of(SUSPENDED, PENDING_DELETE);
            case SUSPENDED -> EnumSet.of(ACTIVE, PENDING_DELETE);
            case PENDING_DELETE -> EnumSet.of(ACTIVE, ANONYMIZED, DELETED); // ACTIVE = cancelamento na graça de 30 dias
            case ANONYMIZED, DELETED -> EnumSet.noneOf(TwinStatus.class);
        };
    }

    public boolean canTransitionTo(TwinStatus next) { return allowedNext().contains(next); }
    public boolean isTerminal() { return allowedNext().isEmpty(); }
    public boolean countsForRanking() { return this == ACTIVE; }
}
