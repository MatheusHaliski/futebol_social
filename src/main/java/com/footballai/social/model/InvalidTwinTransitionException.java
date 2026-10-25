package com.footballai.social.model;

import com.footballai.social.model.enums.TwinStatus;

/** Transição inválida na máquina de estados do Gêmeo Digital. */
public class InvalidTwinTransitionException extends RuntimeException {
    private final TwinStatus from;
    private final TwinStatus to;

    public InvalidTwinTransitionException(TwinStatus from, TwinStatus to) {
        super("Transição inválida do Gêmeo: " + from + " → " + to + " (permitidas: " + from.allowedNext() + ")");
        this.from = from; this.to = to;
    }
    public TwinStatus from() { return from; }
    public TwinStatus to() { return to; }
}
