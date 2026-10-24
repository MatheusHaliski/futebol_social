# Taxonomia de Entidades e Atributos — Football AI · Social

> Fonte de verdade para o modelo de dados. Cada atributo traz **tipo**, **restrições**, **store** (onde persiste) e **classe LGPD**:
> `P` = dado pessoal · `S` = dado pessoal **sensível** (Art. 11) · `N` = não pessoal / preferência de interface · `D` = derivado/agregado (sem PII após anonimização).
> Diagramas correspondentes em [`docs/diagrams/`](diagrams/README.md).

## 0. Mapa de contextos (bounded contexts)

```
┌─────────────┐  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  ┌───────────┐  ┌───────────┐
│  identity   │  │    auth     │  │     twin     │  │     badge     │  │   feed    │  │   lgpd    │
│ User + sub- │  │ Session     │  │ DigitalTwin  │  │ AthleteBadge  │  │ Post      │  │ LgpdPrefs │
│ classes,    │─▶│ RefreshTok. │  │ WeeklyScore  │◀─│ InjuryRecord  │  │ FeedEntry │  │ Consent   │
│ Terms       │  │ (Redis)     │  │ Evaluation   │  │ BadgeAudit    │  │ Drill     │  │ Export    │
└─────────────┘  └─────────────┘  │ MatchPerf.   │  └───────────────┘  └───────────┘  └───────────┘
                                  └──────────────┘        ▲                                ▲
                                        ▲                 │                                │
                                  ┌─────────────┐   ┌───────────┐                    ┌────────────┐
                                  │  telemetry  │──▶│  (eventos) │                    │  storage   │
                                  │ TelemetryData│  └───────────┘                    │ S3 objects │
                                  └─────────────┘                                     └────────────┘
```

| Contexto | Store principal | Agregado raiz | Identidade |
|---|---|---|---|
| identity | MySQL | `User` | `UUID v4` |
| auth | Redis | `Session` | `UUID` por sessão; `jti` por access token |
| twin | MySQL (+ leitura Cassandra) | `DigitalTwin` | `UUID`, 1:1 com `User` |
| badge | MySQL | `AthleteBadge` (por tipo+temporada) | `UUID` |
| feed | MySQL (post) + Cassandra (fan-out) | `Post` | `UUID`; `FeedEntry` por `(user_id, feed_window, published_at)` |
| telemetry | Cassandra | `TelemetryData` | `(athlete_id, year_month, recorded_at)` |
| lgpd | MySQL + S3 | `UserLgpdPreferences` | `user_id` |

Convenções: tabelas `snake_case` plural; PK `id BINARY(16)`; timestamps `TIMESTAMP(6)` em UTC; dinheiro/notas em `DECIMAL`; enums persistidos como `VARCHAR` (`@Enumerated(STRING)`); colunas PII com sufixo `_enc` quando cifradas (AES-256 via `AttributeConverter`).

---

## 1. Contexto `identity`

### 1.1 `User` (abstrata · `users` · herança `JOINED`)

| Atributo | Tipo | Restrições | LGPD |
|---|---|---|---|
| `id` | UUID | PK | — |
| `email` | String(254) | único, cifrado (`email_enc`) + hash para busca (`email_hash`) | P |
| `passwordHash` | String(60) | BCrypt(12), nunca exposto | P |
| `username` | String(30) | único, `^[a-z0-9_.]{3,30}$` | P |
| `displayName` | String(80) | obrigatório | P |
| `birthDate` | LocalDate | ≥ 13 anos; usada só para idade (Art. 6º, III) | P |
| `bio` | String(280) | opcional | P |
| `avatarKey` | String(255) | chave S3 em `fas-media` | P |
| `role` | `UserRole` | imutável após criação | N |
| `status` | `AccountStatus` | default `PENDING_VERIFICATION` | N |
| `visibility` | `ProfileVisibility` | default `SOMENTE_EU` | N |
| `locale` | String(5) | `pt-BR` \| `en-US` \| `es` (preferência de UI) | N |
| `darkMode` | boolean | default `false` (preferência de UI) | N |
| `fontScale` | Decimal(2,1) | 0.8–1.4 | N |
| `countryCode` | String(2) | ISO-3166 | P |
| `city` | String(80) | opcional | P |
| `createdAt` / `updatedAt` | Instant | auditoria | — |
| `deletionRequestedAt` | Instant | nulo até `DELETE /v1/me` | — |
| `scheduledPurgeAt` | Instant | `deletionRequestedAt + 30 d` | — |
| `anonymizedAt` | Instant | preenchido pelo job de anonimização | — |

Métodos de domínio: `ringColor()`, `activate()`, `requestDeletion(now)`, `cancelDeletion()`, `anonymize()`, `hasAcceptedCurrentTerms(version)`.

### 1.2 Subclasses por papel (uma tabela por subclasse, FK = `users.id`)

| Entidade / tabela | Papel | Atributos específicos | LGPD |
|---|---|---|---|
| `Athlete` · `user_athletes` | `ATLETA` | `position` (GOLEIRO, ZAGUEIRO, LATERAL, VOLANTE, MEIA, PONTA, ATACANTE) · `dominantFoot` (D/E/AMBOS) · `heightCm` · `weightKg` · `currentClubId` (FK opcional) · `contractStatus` (`AGENTE_LIVRE` \| `CONTRATADO`) · `contractUntil` · `shirtNumber` · `isCaptain` · `categoria` (SUB15…PROFISSIONAL) · `guardianConsentAt` (menores, Art. 14) | P (altura/peso = dado de saúde → **S**) |
| `Staff` · `user_staff` (ex-`TechnicalStaff`) | `STAFF` | `specialty` (`StaffSpecialty`) · `cbfLicense` · `licenseLevel` (CBF A/B/C, UEFA Pro…) · `currentClubId` · `isHeadCoach` | P |
| `HealthStaff` · `user_health_staff` | `STAFF_SAUDE` | `specialty` (`HealthSpecialty`) · `professionalRegistry` (CRM/CREFITO/CRN/CRP, cifrado) · `registryUf` · `currentClubId` · `verificationLevel` | P — único papel com `canOriginateInjuryRecord()` |
| `Club` · `user_clubs` | `CLUBE` | `legalName` · `federationCode` · `clubeTipo` (`ClubeTipo`) · `division` · `foundedYear` · `stadium` · `crestKey` · `verifiedBy` (FK `OFICIAL_LIGA`) · `ccfValidUntil` · `lat`/`lng` (globo Explorar) · `countryCode` | N (pessoa jurídica) |
| `Scout` · `user_scouts` | `SCOUT` | `agency` · `regionsOfInterest` (JSON) · `verificationLevel` (`VerificationLevel`) · `affiliationId` (clube ou agente) | P |
| `Agent` · `user_agents` | `AGENTE` | `fifaLicenseId` · `licenseStatus` (PENDENTE \| ATIVA \| SUSPENSA) · `agencyName` · `representations` → `representations(agent_id, athlete_id, consented_at, valid_until ≤ 2 anos)` | P |
| `Brand` · `user_brands` | `MARCA` | `tradeName` · `categoria` (`MarcaCategoria`) · `segment` · `website` · `cnpj` · `fazendaAuthorizationId` (só APOSTAS) | N |
| `Press` · `user_press` | `IMPRENSA` | `outlet` · `midiaTipo` (`MidiaTipo`) · `pressCardId` · `credentialIssuer` · `credentialValidUntil` · `beat` | P |
| `LeagueOfficial` · `user_league_officials` | `OFICIAL_LIGA` | `entityName` (CBF, FPF, CONMEBOL…) · `entidadeTipo` (`EntidadeTipo`) · `jurisdiction` | P |
| `Supporter` · `user_supporters` | `TORCEDOR` | `favoriteClubId` · `memberSince` — **sem `DigitalTwin`** | P |
| `AiAgent` · `user_ai_agents` | `AGENTE_IA` | `operatedBy` (FK `users`) · `modelId` · `purpose` · `disclosureVersion` · `delegatedCapabilities` (JSON) | N (identidade), operador = P |
| `Referee` · `user_referees` *(registry)* | `ARBITRO` | `quadro` (ESTADUAL \| RENAF \| FIFA) · `funcao` (ARBITRO \| ASSISTENTE \| VAR \| ASSESSOR) · `federationId` | P |
| `SupporterGroup` · `user_supporter_groups` *(registry)* | `TORCIDA_ORGANIZADA` | `cnpj` · `legalRepresentativeId` · `clubId` · `membersRegistryUrl` | N |

> `ARBITRO` e `TORCIDA_ORGANIZADA` têm cor reservada no enum e comportamento registrado em `role_definitions(code, ring_hex, capabilities_json)` por agentes. Papéis futuros entram como nova subclasse + `RoleCapability`, sem alterar `users`.

### 1.2.1 `OrgMembership` · `org_memberships`

Vínculo pessoa ↔ organização (`Staff`/`HealthStaff`/`Athlete` ↔ `Club`; `LeagueOfficial` ↔ entidade): `id`, `userId`, `orgId`, `roleInOrg` (`STAFF_PRINCIPAL` \| `AUXILIAR` \| `ATLETA` \| `GESTAO`), `since`, `until`, `verifiedBy`. É o que autoriza `INSTITUCIONAL` em nome do clube e o acesso ao Mural/Gestão.

### 1.3 `TermsVersion` · `terms_versions`

| Atributo | Tipo | Restrições | LGPD |
|---|---|---|---|
| `id` | UUID | PK | — |
| `version` | String(20) | único, ex. `2026.10` | N |
| `owner` | String(80) | `Velion (Orange Social)` | N |
| `documentUrl` / `privacyPolicyUrl` | String(255) | — | N |
| `effectiveFrom` | Instant | — | N |
| `current` | boolean | só uma versão `true` | N |

### 1.4 `TermsAcceptance` · `terms_acceptances` (imutável)

| Atributo | Tipo | Restrições | LGPD |
|---|---|---|---|
| `id` | UUID | PK | — |
| `userId` | UUID | FK | P |
| `termsVersion` | String(20) | FK lógico | N |
| `acceptedAt` | Instant | — | P |
| `ipAddress` | String(45) | IPv4/IPv6 | P |
| `userAgent` | String(255) | — | P |

### 1.5 `EmailVerificationToken` · `email_verification_tokens`

`id`, `userId`, `tokenHash` (SHA-256), `expiresAt` (+24 h), `usedAt`.

---

## 2. Contexto `auth` (Redis)

| Chave / estrutura | Campos | TTL | LGPD |
|---|---|---|---|
| `session:{userId}:{sessionId}` (HASH) | `device.userAgent`, `device.platform`, `device.ip`, `createdAt`, `lastSeenAt`, `refreshTokenId`, `state` (`ACTIVE`) | 7 d (renova no refresh) | P |
| `rt:{refreshId}` (HASH) | `sessionId`, `userId`, `issuedAt`, `expiresAt`, `rotatedTo`, `revoked` | 7 d | P |
| `bl:{jti}` (STRING) | `1` | restante do access (≤ 15 min) | — |
| `bucket:ip:{ip}` / `bucket:user:{userId}` (STRING contador) | tokens consumidos | 60 s | P |
| `stepup:{userId}` (STRING) | token de reautenticação | 5 min | — |
| `login_fail:{emailHash}` (STRING) | contador de falhas | 15 min | P |

Access JWT (HS512) — claims: `sub` (userId), `role`, `sid`, `jti`, `iat`, `exp` (15 min). Nunca contém e-mail ou nome.

---

## 3. Contexto `twin` (Rarity Engine)

### 3.1 `DigitalTwin` · `digital_twins`

| Atributo | Tipo | Restrições | LGPD |
|---|---|---|---|
| `id` | UUID | PK | — |
| `userId` | UUID | FK único (1:1) | P |
| `status` | `TwinStatus` | default `DRAFT`; transições validadas | N |
| `physicalStatus` | `PhysicalStatus` | default `READY`; derivado | **S** |
| `scoreTotal` | int | 0–3000+ | D |
| `rarityTier` | `RarityTier` | `fromScore(scoreTotal)` | D |
| `rewardTier` | `RewardTier` | default `BRONZE` | D |
| `lastCalculatedWeek` | String(8) | ISO week `2026-W43` | — |
| `activatedAt` / `suspendedAt` | Instant | — | — |

### 3.2 `WeeklyScoreSnapshot` · `weekly_score_snapshots` · `UNIQUE(twin_id, iso_week)`

| Atributo | Tipo | Restrições | LGPD |
|---|---|---|---|
| `id` | UUID | PK | — |
| `twinId` | UUID | FK | — |
| `isoWeek` | String(8) | — | — |
| `physicalScore` | Decimal(7,4) | 0–1 normalizado (peso 0.30) | D |
| `technicalScore` | Decimal(7,4) | 0–1 (peso 0.40) | D |
| `matchScore` | Decimal(7,4) | 0–1 (peso 0.30) | D |
| `badgeBonus` | Decimal(7,4) | ≥ 0 | D |
| `scoreTotal` | int | `round((Σ pesos) × 3000 + bonus, HALF_EVEN)` | D |
| `rarityTier` | `RarityTier` | — | D |
| `inputsHash` | String(64) | SHA-256 dos inputs canônicos | — |
| `calculatedAt` | Instant | — | — |

### 3.3 `TechnicalEvaluation` · `technical_evaluations`

`id`, `athleteId`, `evaluatorId`, `evaluatorRole` (`UserRole`), `rating` Decimal(4,2) 0–10, `weight` Decimal(3,2) (comissão 1.00 · scout homologado 0.80 · comunidade 0.50), `dimension` (`FISICA` \| `TECNICA` \| `TATICA`), `weekOf` LocalDate, `comment` String(500). LGPD: P.

### 3.4 `MatchPerformance` · `match_performances`

`id`, `athleteId`, `matchId`, `matchDate`, `competition`, `official` boolean, `minutesPlayed` 0–120, `rating` Decimal(4,2), `goals`, `assists`, `cleanSheet`, `tackles`, `yellowCards`, `redCards`, `mvp` boolean. LGPD: P.

---

## 4. Contexto `badge` (Athletic Persona)

### 4.1 `AthleteBadge` · `athlete_badges` · `UNIQUE(athlete_id, badge_type, season)`

| Atributo | Tipo | Restrições | LGPD |
|---|---|---|---|
| `id` | UUID | PK | — |
| `athleteId` | UUID | FK | P |
| `badgeType` | `BadgeType` | — | N (os de `PHYSICAL_STATUS` são **S**) |
| `counter` | int | ≥ 0; só para `quantitative = true` | D |
| `season` | String(9) | `2026` ou `2026/27` | — |
| `awardedAt` | Instant | — | — |
| `awardedBy` | UUID | nulo = sistema; técnico para `MESTRE_TATICO` | P |
| `active` | boolean | — | — |

### 4.2 `InjuryRecord` · `injury_records` — **sensível (Art. 11)**

`id`, `athleteId`, `injuryType` (cifrado), `bodyPart`, `severity` (`LEVE` \| `MODERADA` \| `GRAVE`), `startedAt`, `expectedReturnDate`, `clearedAt`, `reportedBy` (staff médico/clube), `notes` (cifrado). Visível a terceiros apenas com `shareMedicalData = true`.

### 4.3 `BadgeAuditLog` · `badge_audit_log` (append-only)

`id`, `athleteId`, `badgeType`, `action` (`AWARD` \| `UPDATE_COUNTER` \| `DEACTIVATE` \| `ARCHIVE`), `reason`, `at`.

---

## 5. Contexto `feed`

### 5.1 `Post` · `posts`

| Atributo | Tipo | Restrições | LGPD |
|---|---|---|---|
| `id` | UUID | PK | — |
| `authorId` | UUID | FK | P |
| `authorRole` | `UserRole` | denormalizado p/ bolinha | N |
| `contentType` | `PostContentType` | borda; validado por `RoleCapability.allowedContentTypes()` | N |
| `window` | `FeedWindow` | default `contentType.defaultWindow` | N |
| `title` | String(90) | **obrigatório**, 5–12 palavras, ≤ 2 linhas no card | P |
| `subtitle` | String(160) | *teaser* opcional (≤ 2 linhas, 70 %); nulo quando há mídia | P |
| `content` | String(5000) | texto completo — **só em `PostView`**, nunca no feed | P |
| `mediaKeys` | JSON array | chaves S3 (mídia 136 px no card) | P |
| `ageRating` | `AgeRating` | default `SAFE`; `SENSITIVE` forçado quando `contentType.sensitive` | N |
| `vslGlyph` | `VslGlyph` | derivado de `contentType` (não persistido) | N |
| `aiGenerated` | boolean | `true` obrigatório para `AGENTE_IA`/Autopilot | N |
| `badgeLabel` | String(24) | rótulo curto da pill do tipo (`TREINO`, `LESÃO`, `SCOUTING`…) | N |
| `status` | `PostStatus` | default `PUBLISHED` | N |
| `severity` | `AlertSeverity` | só para janela ALERTA | N |
| `alertType` | `AlertType` | só para janela ALERTA | N |
| `clubId` | UUID | opcional (mural do clube) | N |
| `drillId` | UUID | FK opcional → `TacticalDrill` | — |
| `publishedAt` | Instant | — | — |
| `likeCount` / `commentCount` / `shareCount` / `saveCount` | int | contadores denormalizados (Curtir · Comentar · Compartilhar · Salvar) | D |
| `viewCount` | long | visualizações (Orange Social: *trending by views*) | D |
| `trendingScore` | Decimal(8,3) | janela de 24 h (views, likes, shares, decaimento) | D |
| `trend` | `TrendDirection` | projeção de `trendingScore` Δ24h: `HOT` · `UP` · `FLAT` · `DOWN` | D |

### 5.2 `TacticalDrill` · `tactical_drills`

`id`, `authorId`, `title`, `formation` (`4-3-3`…), `boardJson` (prancheta serializada), `videoKey`, `durationMin`, `targetAttributes` (JSON: `{"passe":85,"velocidade":80}`), `sourceKind` (`HOMOLOGADO` \| `COMUNIDADE`).

### 5.3 `FeedEntry` · Cassandra `feed_entries`

| Coluna | Tipo CQL | Papel |
|---|---|---|
| `user_id` | uuid | partição |
| `feed_window` | text | partição |
| `published_at` | timeuuid | clustering DESC (cursor) |
| `post_id`, `author_id` | uuid | — |
| `author_role`, `content_type` | text | filtros no cliente/servidor |
| `severity`, `alert_type`, `source_role` | text | filtros da janela ALERTA |
| `title`, `subtitle`, `age_rating` | text | render do card sem *fan-in* no MySQL |
| `trending_score` | double | ordenação da aba *Trending* |

### 5.4 Filtros por janela (RF3) — parâmetros aceitos por `GET /v1/feeds/{window}`

| Janela | Query params | Fonte do atributo |
|---|---|---|
| **PERFIL** | `tier` (lista `RarityTier`), `position`, `ageMin`/`ageMax`, `clubId`, `contractStatus` | `DigitalTwin.rarityTier`, `Athlete.*` |
| **GEMEO** | `period` (`WEEK` \| `MONTH` \| `SEASON`), `metricType` (`FISICA` \| `TECNICA` \| `TATICA`), `eventType` (`TREINO` \| `PARTIDA`), `athleteId` | `TelemetryData`, `WeeklyScoreSnapshot` |
| **SCOUTING** | `attr[velocidade]=gt:80`, `attr[passe]=gt:85` (repetível), `compare=idA,idB`, `source` (`HOMOLOGADO` \| `COMUNIDADE`) | `ScoutingAttribute`, `TacticalDrill.sourceKind`, `Scout.accredited` |
| **ALERTA** | `severity` (`BAIXA` \| `MEDIA` \| `ALTA` \| `CRITICA`), `alertType` (`TRANSFERENCIA` \| `LESAO` \| `PUNICAO` \| `INSTITUCIONAL`), `source` (`IMPRENSA` \| `CLUBE` \| `OFICIAL_LIGA`) | `Post.severity`, `Post.alertType`, `Post.authorRole` |
| todas | `sort` (`FOR_YOU` \| `TRENDING` \| `RECENT` \| `FOLLOWING`), `role` (lista `UserRole`), `state` (UF), `clubId`, `q` (nome/handle), `cursor`, `size` (≤ 50) | `FeedEntry.*`, `Post.authorRole`, `Athlete.state` |

### 5.5 `ScoutingAttribute` · `scouting_attributes` (view materializada semanal)

`athleteId`, `isoWeek`, `velocidade`, `passe`, `finalizacao`, `drible`, `desarme`, `posicionamento`, `resistencia` — todos `Decimal(5,2)` 0–100, derivados de telemetria + avaliações. LGPD: D.

---

## 6. Contexto `telemetry` · Cassandra `telemetry_data`

| Coluna | Tipo | Restrições | LGPD |
|---|---|---|---|
| `athlete_id` | uuid | partição | P |
| `year_month` | text | partição (`2026-10`) | — |
| `recorded_at` | timestamp | clustering DESC | — |
| `event_type` | text | `TREINO` \| `PARTIDA` | N |
| `metric_type` | text | `FISICA` \| `TECNICA` \| `TATICA` | N |
| `distance_km`, `max_speed_kmh`, `load_au` | decimal | ≥ 0 | **S** (saúde) |
| `duration_min`, `rpe` (1–10) | int | — | **S** |
| `passes_ok` / `passes_total` | int | — | P |
| `gps_track` | text (GeoJSON) | **só com `locationHistory = true`** | **S** |

Métricas derivadas (não persistidas): `acuteLoad7d`, `chronicLoad28d`, `acwr = acute/chronic` (gatilho `RISCO_FADIGA` > 1.5).

---

## 7. Contexto `lgpd`

### 7.1 `UserLgpdPreferences` · `user_lgpd_preferences` — **todas `DEFAULT FALSE`**

| Atributo | Finalidade | Base legal | Sensível |
|---|---|---|---|
| `shareTelemetryWithClubs` | clubes veem telemetria | Art. 7º, I | — |
| `shareTelemetryWithScouts` | scouts veem telemetria | Art. 7º, I | — |
| `shareMedicalData` | status/lesões visíveis a terceiros | Art. 11, I | **S** |
| `shareBiometricData` | reconhecimento facial em vídeos | Art. 11, I | **S** |
| `locationHistory` | GPS de treinos/jogos | Art. 7º, I | **S** |
| `shareWithBrands` | ativação comercial | Art. 7º, I | — |
| `personalizedAds` | anúncios personalizados | Art. 7º, I | — |
| `aiAgentProcessing` | agentes de IA processam o perfil | Art. 7º, I | — |
| `updatedAt` | — | — | — |

### 7.2 `ConsentAuditLog` · `consent_audit_log` (append-only)

`id`, `userId`, `preference`, `oldValue`, `newValue`, `lgpdArticle`, `ipAddress`, `userAgent`, `changedAt`. Retenção: 5 anos após o fim do tratamento (prazo prescricional).

### 7.3 `DataExportRequest` · `data_export_requests`

`id`, `userId`, `status` (`ExportStatus`), `requestedAt`, `readyAt`, `expiresAt` (+7 d), `s3ObjectKey` (`exports/{userId}/{id}.json`), `sizeBytes`, `failureReason`, `attempts` (≤ 3).

### 7.4 Payload de exportação (Art. 18, V) — chaves de topo

`profile`, `lgpdPreferences`, `consentHistory`, `termsAcceptances`, `sessions` (dispositivos), `digitalTwin`, `weeklyScores`, `badges`, `injuries`, `telemetry`, `posts`, `evaluationsReceived`, `generatedAt`, `format: application/json`.

---

## 8. Storage de objetos (S3 / MinIO)

| Bucket | Prefixo | Conteúdo | Retenção | LGPD |
|---|---|---|---|---|
| `fas-media` | `avatars/{userId}/` | foto de perfil | enquanto conta ativa; apagado na anonimização | P (biométrico se usado p/ reconhecimento → S) |
| `fas-media` | `posts/{postId}/` | imagens/vídeos | idem | P |
| `fas-media` | `drills/{drillId}/` | vídeos de prancheta | idem | P |
| `fas-lgpd-exports` | `exports/{userId}/` | JSON de portabilidade | 7 dias (lifecycle) | P |

---

## 9. Enumerações

| Enum | Valores | Atributos extras |
|---|---|---|
| `UserRole` | core: `ATLETA` · `STAFF` · `STAFF_SAUDE` · `CLUBE` · `SCOUT` · `AGENTE` · `MARCA` · `IMPRENSA` · `OFICIAL_LIGA` · `TORCEDOR` · `AGENTE_IA` — reservados (registry): `ARBITRO` · `TORCIDA_ORGANIZADA` | `ringColor` — tokens do modelo visual (#00d47a · #7fb6ff · #e11d48 · #3b82f6 · #f7c948 · #2dd4bf · #a06bff · #ff9f43 · #f5c542 · #f472b6 · #22d3ee · #95a89d · #ec4899), `dotClass` (`dc-atleta`…), `label`, `isOrganization()`, `isHuman()`, `isAutomated()`; `@JsonAlias("TECNICO_STAFF")` em `STAFF` por 2 versões |
| `StaffSpecialty` | `TREINADOR` · `AUXILIAR` · `TREINADOR_GOLEIROS` · `PREPARADOR_FISICO` · `FISIOLOGISTA` · `ANALISTA_DESEMPENHO` · `GESTAO` · `OUTRO` | — |
| `HealthSpecialty` | `MEDICO` · `FISIOTERAPEUTA` · `NUTRICIONISTA` · `PSICOLOGO` · `MASSAGISTA` | `registryCouncil` (CRM/CREFITO/CRN/CRP) |
| `VerificationLevel` | `COMUNIDADE` · `VERIFICADO` · `HOMOLOGADO` | `countsForRarity()` (≥ VERIFICADO) |
| `ClubeTipo` | `PROFISSIONAL` · `FORMADOR_CCF` · `ESCOLINHA_ACADEMIA` · `AMADOR` | — |
| `MarcaCategoria` | `PATROCINADOR` · `MATERIAL` · `AGENCIA` · `APOSTAS` | `audiencePolicy()` — APOSTAS: `minAge=18`, advertência, exclui ATLETA/STAFF/STAFF_SAUDE/ARBITRO/OFICIAL_LIGA, flag `betting.ads.enabled` |
| `MidiaTipo` | `VEICULO_CREDENCIADO` · `JORNALISTA` · `CRIADOR_INDEPENDENTE` · `CANAL_PODCAST` | — |
| `EntidadeTipo` | `CONFEDERACAO` · `FEDERACAO_ESTADUAL` · `LIGA` · `ORGANIZADOR_TORNEIO` | — |
| `AnalysisKind` | `TECNICA` · `OPINATIVA` | `OPINATIVA` (SCOUT.COMUNIDADE, IMPRENSA) não entra nos 40 % do Rarity |
| `AccountStatus` | `PENDING_VERIFICATION` · `ACTIVE` · `SUSPENDED` · `PENDING_DELETE` · `ANONYMIZED` | — |
| `ProfileVisibility` | `SOMENTE_EU` (default) · `CONEXOES` · `PUBLICO` | — |
| `ContractStatus` | `AGENTE_LIVRE` · `CONTRATADO` | — |
| `Position` | `GOLEIRO` · `ZAGUEIRO` · `LATERAL` · `VOLANTE` · `MEIA` · `PONTA` · `ATACANTE` | — |
| `FeedWindow` | `PERFIL`(1) · `GEMEO`(2) · `SCOUTING`(3) · `ALERTA`(4) | `order`, `icon`, `route`, `label` |
| `PostContentType` | `TREINO` · `PARTIDA_MOMENTO` · `RELATORIO_TATICO` · `LIGA` · `INSTITUCIONAL` · `LESAO_ALERTA` · `ESPECIAL` | `borderColor` (#00d47a · #3b82f6 · #ff9f43 · #a06bff · #ff5d5d · #ff5d5d+hazard · #f5c542), `cssClass` (`cc-treino` · `cc-partida` · `cc-scouting` · `cc-liga` · `cc-institucional` · `cc-institucional` · `cc-especial`), `vslGlyph`, `defaultWindow` (GEMEO · PERFIL · SCOUTING · PERFIL · ALERTA · ALERTA · PERFIL), `badgeLabel`, `sensitive` (só `LESAO_ALERTA`) |
| `AgeRating` | `SAFE` · `KIDS` · `AGE_12` · `AGE_16` · `AGE_18` · `SENSITIVE` | `label` (`SAFE` · `KIDS` · `12+` · `16+` · `18+` · `SENSÍVEL`), `cssClass` (`age-safe`…`age-sens`), `minAge` |
| `VslGlyph` | `IDENTIDADE` · `VALOR` · `COMUNIDADE` · `INSTITUICAO` | `glyph` (◇ · ✦ · ⬢ · ▭) — Velion Symbol Language |
| `TrendDirection` | `HOT` · `UP` · `FLAT` · `DOWN` | `emoji` (🔥 · 📈 · ➖ · 📉), `fromDelta()` |
| `FeedSort` | `FOR_YOU` · `TRENDING` · `RECENT` · `FOLLOWING` | abas de relevância (Para você · Trending · Recentes · Seguindo) |
| `PostStatus` | `DRAFT` · `PUBLISHED` · `HIDDEN` · `REMOVED` | — |
| `AlertSeverity` | `BAIXA` · `MEDIA` · `ALTA` · `CRITICA` | — |
| `AlertType` | `TRANSFERENCIA` · `LESAO` · `PUNICAO` · `INSTITUCIONAL` | — |
| `MetricType` | `FISICA` · `TECNICA` · `TATICA` | — |
| `EventType` | `TREINO` · `PARTIDA` | — |
| `Period` | `WEEK` · `MONTH` · `SEASON` | — |
| `ReportSource` | `HOMOLOGADO` · `COMUNIDADE` | — |
| `RarityTier` | `COMMON`(0–400) · `RARE`(401–900) · `EPIC`(901–1600) · `LEGENDARY`(1601–2400) · `MYTHIC`(2401+) | `minScore`, `maxScore`, `visualEffect`, `color` (#5c6b62 · #3b82f6 · #a06bff · #f5c542 · #f5c542), `crestIcon` (`ic-tier-common`…`ic-tier-mythic` — brasão único; o nome do tier nunca é renderizado em texto), `flairMultiplier` (1 · 1,05 · 1,1 · 1,2 · 1,3), `fromScore()` |
| `RewardTier` | `BRONZE` · `SILVER` · `GOLD` · `PLATINUM` | — |
| `TwinStatus` | `DRAFT` · `ACTIVE` · `SUSPENDED` · `PENDING_DELETE` · `ANONYMIZED` · `DELETED` | `allowedNext()` |
| `PhysicalStatus` | `READY` · `FATIGUE_RISK` · `INJURED` | `badge()`, `blocksCallUp()`, `label` (`PRONTO PRA JOGO` · `RISCO DE FADIGA` · `DEPARTAMENTO MÉDICO`), `emoji` (⚡ · ⚠️ · 🩹) |
| `BadgeCategory` | `SEASON_STATS` · `PHYSICAL_STATUS` · `SOCIAL_TACTICAL` | — |
| `BadgeType` | `ARTILHEIRO` · `GARCOM` · `PAREDAO` · `XERIFAO` · `MOTORZINHO` · `PRONTO_PRA_JOGO` · `DEPARTAMENTO_MEDICO` · `RISCO_FADIGA` · `MVP` · `MESTRE_TATICO` · `EM_CHAMAS` · `LIDER_CAPITAO` · `FAIR_PLAY` | `category`, `emoji`, `color`, `priority`, `quantitative` |
| `ExportStatus` | `REQUESTED` · `PROCESSING` · `READY` · `EXPIRED` · `FAILED` | — |
| `SessionState` | `ACTIVE` · `REVOKED` · `EXPIRED` | — |
| `LgpdArticle` | `ART_6_III` · `ART_6_IV` · `ART_6_VI` · `ART_6_X` · `ART_7_I` · `ART_7_V` · `ART_8_P5` · `ART_11` · `ART_18_I_II` · `ART_18_III` · `ART_18_V` · `ART_18_VI` · `ART_18_VII` · `ART_18_IX` | `label` exibido na pill da UI |

---

## 10. Relacionamentos (cardinalidades)

| De | Para | Cardinalidade | Observação |
|---|---|---|---|
| `User` | `UserLgpdPreferences` | 1 : 1 | criada no cadastro, tudo `FALSE` |
| `User` | `DigitalTwin` | 1 : 1 | criada no cadastro em `DRAFT` |
| `User` | `TermsAcceptance` | 1 : N | imutável |
| `User` | `ConsentAuditLog` | 1 : N | append-only |
| `User` | `DataExportRequest` | 1 : N | — |
| `User` | `Post` | 1 : N | autor |
| `Athlete` | `AthleteBadge` | 1 : N | único por tipo+temporada |
| `Athlete` | `InjuryRecord` | 1 : N | sensível |
| `Athlete` | `TechnicalEvaluation` | 1 : N | avaliado |
| `Athlete` | `MatchPerformance` | 1 : N | — |
| `Athlete` | `TelemetryData` | 1 : N | Cassandra |
| `Athlete` | `Club` | N : 1 | `currentClubId`, opcional |
| `DigitalTwin` | `WeeklyScoreSnapshot` | 1 : N | único por semana |
| `Post` | `TacticalDrill` | 0..1 : 0..1 | — |
| `Post` | `FeedEntry` | 1 : N | fan-out por seguidor × janela |
| `Club` | `LeagueOfficial` | N : 1 | `verifiedBy` |

---

## 11. Contexto `twin` — sub-abas do Gêmeo (novas entidades)

| Entidade · tabela | Atributos | Sub-aba | LGPD |
|---|---|---|---|
| `TwinMilestone` · `twin_milestones` | `twinId`, `isoWeek`, `type` (`TIER_UP` \| `TIER_DOWN` \| `BADGE` \| `COACH_NOTE`), `refId`, `note` | Evolução | P |
| `TrainingLoadDaily` · `training_load_daily` | `athleteId`, `date`, `loadAu`, `rpe`, `durationMin`, `sessions` | Carga & Recuperação | **S** |
| `RecoveryDaily` · `recovery_daily` | `athleteId`, `date`, `sleepHours`, `hrv`?, `restingHr`?, `readiness` (1–100), `band` (PRIME ≥95 · HIGH 75–94 · MODERATE 50–74 · LOW 25–49 · POOR ≤24) | Carga & Recuperação | **S** — nunca em FLAIR/rankings |
| `PlayDnaProfile` · `play_dna_profiles` | `userId`, `season`, `axes` JSON `{ataque, tecnica, tatica, defesa, criatividade}` 0–100, `styles[]` (FINALIZADOR, CONSTRUTOR, VELOCISTA, MURALHA, MOTOR, LIDER), `calculatedAt` | DNA de jogo | D |
| `ComparisonSet` · `comparison_sets` | `ownerId`, `memberIds[≤3]`, `createdAt` | Comparador | P |
| `PositionProfile` · `position_profiles` | `position`, `formation`, `requiredAxes` JSON | Provador tático (fit % calculado on-the-fly) | N |
| `AvatarConfig` · `avatar_configs` | `userId`, `baseModel`, `skin`, `kitId`, `tierFxOverride`? | Avatar 3D | P |
| `AutopilotDraft` · `autopilot_drafts` | `userId`, `source` (`WEARABLE` \| `SUMULA` \| `TELEMETRIA`), `suggestedType`, `text`, `status` (`DRAFT` \| `ACCEPTED` \| `DISCARDED`), `aiGenerated = true`, `reviewedAt` | Rascunhos Autopilot | P (sem saúde sem `shareMedicalData`) |

## 12. Contexto `social` — Rankings, Mural do Clube e Explorar

| Entidade · tabela | Atributos | Onde aparece | LGPD |
|---|---|---|---|
| `RankingEntry` · `ranking_entries` (view materializada semanal) | `isoWeek`, `scope` (`GLOBAL` \| `LIGA` \| `POSICAO` \| `IDADE` \| `CLUBE`), `scopeKey`, `subjectType` (`ATLETA` \| `CLUBE`), `subjectId`, `scoreTotal`, `rarityTier`, `rank`, `delta` | PERFIL → Ranking · Destaques | D (só perfis `PUBLICO`/`CONEXOES`) |
| `ClubWallPost` (= `Post` com `clubId` + `INSTITUCIONAL`) | `clubId`, `pinned`, `audience` (`PUBLICO` \| `SOCIOS` \| `ELENCO`) | ALERTA → Mural do Clube · Gestão → Meu clube | N |
| `ClubPoll` · `club_polls` | `clubId`, `question`, `options[]`, `closesAt`, `audience` | Mural do Clube | N |
| `League` · `leagues` | `id`, `name`, `countryCode`, `tier` (divisão), `organizerId` (FK `OFICIAL_LIGA`), `wikidataId`, `season` | Explorar · Clubes & Ligas | N |
| `ClubGeo` (projeção de `Club`) | `clubId`, `lat`, `lng`, `countryCode`, `leagueId`, `avgTier`, `crestKey` | Explorar (globo MapLibre; dados Wikidata CC0 + openfootball) | N |
| `Follow` · `follows` | `followerId`, `targetId`, `targetType` (`USER` \| `CLUBE` \| `LIGA`), `since` | fan-out dos feeds | P |

## 13. Contexto `flair` — jogo de cartas (RF6)

| Entidade · tabela | Atributos | LGPD |
|---|---|---|
| `Card` · `flair_cards` | `id`, `subjectType` (`ATLETA` \| `CLUBE`), `subjectId`, `snapshotWeek`, `tier`, `power` Decimal(9,2), `axes` `{fisico, tecnico, partidas}`, `visualVariant` (`BASE` \| `GOLD`), `badgeBonusPct` | D — só stats consentidas; nunca `PhysicalStatus`/`InjuryRecord` |
| `CardOwnership` · `flair_card_ownerships` | `userId`, `cardId`, `acquiredVia` (`PACK` \| `TRADE` \| `REWARD`), `acquiredAt`, `serial` | P |
| `Deck` · `flair_decks` | `userId`, `season`, `cardIds[5]`, `captainCardId`, `tierCapRule` | P |
| `Pack` · `flair_packs` | `type`, `source` (`TIMER_12H` \| `ACHIEVEMENT` \| `CHALLENGE`), `dropTable` JSON **visível** (probabilidades), `poolRules` — **nunca vendido** | N |
| `Match` · `flair_matches` | `mode` (`DUEL` \| `LEAGUE8` \| `CHALLENGE`), `participantIds[]`, `rounds` JSON (eixo escolhido, valores, vencedor), `snapMultiplier`, `settledAtWeek`, `winnerId` | P |
| `Reward` · `flair_rewards` | `userId`, `type` (`POINTS` \| `PACK` \| `COUPON` \| `MOMENT` \| `COSMETIC`), `value`, `partnerId`? (cupom só com `shareWithBrands`), `grantedAt` | P |
| `Season` · `flair_seasons` | `startWeek`, `endWeek`, `ruleset` JSON, `resetPolicy` | N |
| `Moment` · `flair_moments` | `postId` (`PARTIDA_MOMENTO`), `edition`, `serial`, `ownerId` | P |
| `PointsLedger` · `points_ledger` (append-only) | `userId`, `delta`, `reason` (`TREINO_PUBLICADO` \| `AVALIACAO` \| `ASSIDUIDADE` \| `CONVITE` \| `FLAIR_REWARD` \| `RESGATE`), `at` | P |

Regras: sem dinheiro real nem prêmio monetário (ECA Digital art. 20 · Lei 14.790/2023); sub-13 sem modo competitivo público; cards de atletas `INJURED`/`PENDING_DELETE` não pontuam; perfis `SOMENTE_EU` não geram carta para terceiros; limite diário de partidas e opt-out em 1 toque.

## 14. Regras de anonimização (job diário, após 30 dias)

| Entidade | Ação |
|---|---|
| `User` | `email_enc`, `username`, `displayName`, `bio`, `city`, `avatarKey` → hash/`anon_{shortId}`; `status = ANONYMIZED` |
| Subclasses | campos identificadores (licença, CNPJ, press card) → nulos |
| `Session`, `rt:*`, `stepup:*` | removidos do Redis |
| `Post` | `title` → `[conteúdo removido]`, `subtitle`/`content` → nulos, `mediaKeys` apagados do S3, autor → pseudônimo |
| `TelemetryData` | `gps_track` apagado; métricas agregadas mantidas sem vínculo nominal |
| `InjuryRecord` | removido fisicamente |
| `WeeklyScoreSnapshot`, `AthleteBadge` | mantidos como agregados (`D`) ligados ao pseudônimo |
| `ConsentAuditLog`, `TermsAcceptance` | mantidos (obrigação legal de accountability) com `userId` pseudonimizado |
| `DataExportRequest` | objetos S3 apagados; registro marcado `EXPIRED` |
| `Card`, `CardOwnership`, `Deck`, `Moment` | cards do titular como *sujeito* perdem nome/avatar (viram "Atleta #hash"); cards que o titular *possuía* são devolvidos ao pool; `PointsLedger` pseudonimizado |
| `RankingEntry`, `PlayDnaProfile` | mantidos como agregados ligados ao pseudônimo |
| `RecoveryDaily`, `TrainingLoadDaily`, `AutopilotDraft` | removidos fisicamente (dado de saúde) |

---

## 15. DTOs de card (contrato REST ↔ SPA)

Records Java em `controller/dto`; o front renderiza **apenas** a partir deles (nenhuma regra visual é recalculada no cliente).

### 15.1 `PostCardDTO` — `.pcard`

| Campo | Tipo | Origem | Camada visual |
|---|---|---|---|
| `id`, `publishedAt`, `timeLabel` | UUID, Instant, String (`2h`, `ontem`) | `Post` | `p-top` |
| `author` → `{ handle, role, dotClass, verified, aiGenerated }` | `AuthorDTO` | `User`, `UserRole` | bolinha + `@handle · papel` |
| `contentType`, `borderClass`, `badgeLabel`, `vslGlyph` | `PostContentType` | enum | borda + badge + VSL |
| `ageRating` → `{ value, label, cssClass }` | `AgeRating` | `Post.ageRating` | `age-chip` |
| `title`, `subtitle` | String | `Post` | `p-title` (≤ 2 linhas) · `p-sub` (teaser) |
| `media` → `{ key, url, kind }` | opcional | S3 | `p-media` 136 px (sem subtítulo) |
| `meta` | `List<String>` | regras da janela | tags de contexto |
| `social` → `{ likes, comments, shares, saves, likedByMe, savedByMe }` | `SocialCountersDTO` | contadores | `actbar` |
| `trend` → `{ direction, emoji }` | `TrendDirection` | `trendingScore` | `.trend` |
| `window`, `severity`, `alertType` | enums | `Post` | listra `hazard` quando ALERTA/`LESAO_ALERTA` |

### 15.2 `AthleteCardDTO` — `.vcard`

| Campo | Tipo | Origem | Camada visual |
|---|---|---|---|
| `athleteId`, `handle`, `name`, `position`, `age`, `state`, `club`, `contractStatus` | — | `Athlete` | `author` · `vtitle` (`Nome — Posição`) · `vsub` |
| `rarity` → `{ tier, crestIcon, color, visualEffect, score, pp, rp, mp }` | `RarityDTO` | `DigitalTwin`, `WeeklyScoreSnapshot` | `ctop` (brasão) · borda · `statrow` |
| `season` → `{ games, goals, assists, minutes, yellow, red, cleanSheets?, tackles?, avgRating }` | `SeasonStatsDTO` | `MatchPerformance` | `sgrid` |
| `physical` → `{ readiness, acwr, status, statusLabel, emoji, injury?: { type, expectedReturn } }` | `PhysicalDTO` · **omitido** para terceiros sem `shareMedicalData` | `TelemetryData`, `InjuryRecord` | `phys` |
| `topBadges` (≤ 3) → `{ type, label, emoji, color, counter }` | `List<BadgeDTO>` | `AthleteBadge` | `bdgrow` (área inferior, nunca sobre o hero) |
| `classification` → `{ ageRating, sensitive }` | `AgeRating` | derivado de `physical` | `age-chip` (`SENSÍVEL` quando expõe dado médico) |
| `social` → `{ likes, comments, shares }` | `SocialCountersDTO` | contadores do perfil | `actbar` + `Gêmeo →` |
| `heroImage` → `{ url }` | opcional | S3 | `imgblock` — **só a imagem**, sem overlays |
