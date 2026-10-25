package com.footballai.social.model.enums;

/** Nível de verificação de scouts e profissionais (só VERIFICADO+ entra nos 40 % do Rarity). */
public enum VerificationLevel {
    COMUNIDADE, VERIFICADO, HOMOLOGADO;

    public boolean countsForRarity() { return this != COMUNIDADE; }
}
