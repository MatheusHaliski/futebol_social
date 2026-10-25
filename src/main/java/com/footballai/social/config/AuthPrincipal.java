package com.footballai.social.config;

import com.footballai.social.model.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

/** Principal autenticado extraído do access JWT (sub, role, sid, jti, exp). Nunca contém e-mail ou nome. */
public record AuthPrincipal(UUID userId, UserRole role, String sessionId, String jti, Instant expiresAt) { }
