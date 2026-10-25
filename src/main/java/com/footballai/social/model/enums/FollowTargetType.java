package com.footballai.social.model.enums;

/** Alvo de um "seguir": pessoa/perfil, clube ou liga (todos são usuários; o tipo guia fan-out e rótulos). */
public enum FollowTargetType {
    USER, CLUBE, LIGA;

    public static FollowTargetType of(UserRole role) {
        return switch (role) { case CLUBE -> CLUBE; case OFICIAL_LIGA -> LIGA; default -> USER; };
    }
}
