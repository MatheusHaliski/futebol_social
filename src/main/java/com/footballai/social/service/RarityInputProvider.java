package com.footballai.social.service;

import java.util.UUID;

/** Porta de entrada do Rarity Engine — permite trocar a fonte (JPA, Cassandra, mocks) sem tocar no cálculo. */
public interface RarityInputProvider {
    RarityEngineService.RarityInputs inputsFor(UUID athleteId, String isoWeek);
}
