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

| Entidade / tabela | Atributos específicos | LGPD |
|---|---|---|
| `Athlete` · `user_athletes` | `position` (GOLEIRO, ZAGUEIRO, LATERAL, VOLANTE, MEIA, PONTA, ATACANTE) · `dominantFoot` (D/E/AMBOS) · `heightCm` · `weightKg` · `currentClubId` (FK opcional) · `contractStatus` (`AGENTE_LIVRE` \| `CONTRATADO`) · `contractUntil` · `shirtNumber` · `isCaptain` | P (altura/peso = dado de saúde → tratar como **S**) |
| `TechnicalStaff` · `user_technical_staff` | `specialty` (TREINADOR, AUXILIAR, PREP_FISICO, ANALISTA, …) · `licenseCode` · `licenseLevel` (CBF A/B/C, UEFA Pro…) · `currentClubId` | P |
| `Club` · `user_clubs` | `legalName` · `federationCode` · `division` · `foundedYear` · `stadium` · `crestKey` · `verifiedBy` (FK `OFICIAL_LIGA`) · `lat`/`lng` (para o globo Explorar) | N (pessoa jurídica) |
| `Scout` · `user_scouts` | `agency` · `regionsOfInterest` (JSON) · `accredited` (homologado por clube/liga) · `accreditedBy` | P |
| `Brand` · `user_brands` | `tradeName` · `segment` · `website` · `cnpj` | N |
| `Press` · `user_press` | `outlet` · `pressCardId` · `beat` (clube/liga que cobre) | P |
| `LeagueOfficial` · `user_league_officials` | `entityName` (CBF, FPF, CONMEBOL…) · `officialRole` (ORGANIZADOR, ARBITRO, DELEGADO) · `jurisdiction` | P |

> Novos papéis mapeados por agentes (ex.: `DIRETOR_ESPORTIVO`, `FISIOLOGISTA`, `AGENTE_FIFA`) entram como novas subclasses + `RoleCapability`, sem alterar `users`.

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
| `text` | String(2000) | — | P |
| `mediaKeys` | JSON array | chaves S3 | P |
| `status` | `PostStatus` | default `PUBLISHED` | N |
| `severity` | `AlertSeverity` | só para janela ALERTA | N |
| `alertType` | `AlertType` | só para janela ALERTA | N |
| `clubId` | UUID | opcional (mural do clube) | N |
| `drillId` | UUID | FK opcional → `TacticalDrill` | — |
| `publishedAt` | Instant | — | — |
| `likeCount` / `commentCount` / `shareCount` / `saveCount` | int | contadores denormalizados | D |

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

### 5.4 Filtros por janela (RF3) — parâmetros aceitos por `GET /v1/feeds/{window}`

| Janela | Query params | Fonte do atributo |
|---|---|---|
| **PERFIL** | `tier` (lista `RarityTier`), `position`, `ageMin`/`ageMax`, `clubId`, `contractStatus` | `DigitalTwin.rarityTier`, `Athlete.*` |
| **GEMEO** | `period` (`WEEK` \| `MONTH` \| `SEASON`), `metricType` (`FISICA` \| `TECNICA` \| `TATICA`), `eventType` (`TREINO` \| `PARTIDA`), `athleteId` | `TelemetryData`, `WeeklyScoreSnapshot` |
| **SCOUTING** | `attr[velocidade]=gt:80`, `attr[passe]=gt:85` (repetível), `compare=idA,idB`, `source` (`HOMOLOGADO` \| `COMUNIDADE`) | `ScoutingAttribute`, `TacticalDrill.sourceKind`, `Scout.accredited` |
| **ALERTA** | `severity` (`BAIXA` \| `MEDIA` \| `ALTA` \| `CRITICA`), `alertType` (`TRANSFERENCIA` \| `LESAO` \| `PUNICAO` \| `INSTITUCIONAL`), `source` (`IMPRENSA` \| `CLUBE` \| `OFICIAL_LIGA`) | `Post.severity`, `Post.alertType`, `Post.authorRole` |
| todas | `cursor`, `size` (≤ 50) | `FeedEntry.published_at` |

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
| `UserRole` | `ATLETA` · `TECNICO_STAFF` · `CLUBE` · `SCOUT` · `MARCA` · `IMPRENSA` · `OFICIAL_LIGA` | `ringColor` (#15805C · #5BA3E6 · #1E3A8A · #D4A017 · #7C3AED · #EA580C · #C9A227), `label` |
| `AccountStatus` | `PENDING_VERIFICATION` · `ACTIVE` · `SUSPENDED` · `PENDING_DELETE` · `ANONYMIZED` | — |
| `ProfileVisibility` | `SOMENTE_EU` (default) · `CONEXOES` · `PUBLICO` | — |
| `ContractStatus` | `AGENTE_LIVRE` · `CONTRATADO` | — |
| `Position` | `GOLEIRO` · `ZAGUEIRO` · `LATERAL` · `VOLANTE` · `MEIA` · `PONTA` · `ATACANTE` | — |
| `FeedWindow` | `PERFIL`(1) · `GEMEO`(2) · `SCOUTING`(3) · `ALERTA`(4) | `order`, `icon`, `route`, `label` |
| `PostContentType` | `TREINO` · `PARTIDA_MOMENTO` · `RELATORIO_TATICO` · `INSTITUCIONAL` · `LESAO_ALERTA` | `borderColor` (#15805C · #2B5CB8 · #D4A017 · #6B7280 · #C0376B), `defaultWindow`, `sensitive` |
| `PostStatus` | `DRAFT` · `PUBLISHED` · `HIDDEN` · `REMOVED` | — |
| `AlertSeverity` | `BAIXA` · `MEDIA` · `ALTA` · `CRITICA` | — |
| `AlertType` | `TRANSFERENCIA` · `LESAO` · `PUNICAO` · `INSTITUCIONAL` | — |
| `MetricType` | `FISICA` · `TECNICA` · `TATICA` | — |
| `EventType` | `TREINO` · `PARTIDA` | — |
| `Period` | `WEEK` · `MONTH` · `SEASON` | — |
| `ReportSource` | `HOMOLOGADO` · `COMUNIDADE` | — |
| `RarityTier` | `COMMON`(0–400) · `RARE`(401–900) · `EPIC`(901–1600) · `LEGENDARY`(1601–2400) · `MYTHIC`(2401+) | `minScore`, `maxScore`, `visualEffect`, `fromScore()` |
| `RewardTier` | `BRONZE` · `SILVER` · `GOLD` · `PLATINUM` | — |
| `TwinStatus` | `DRAFT` · `ACTIVE` · `SUSPENDED` · `PENDING_DELETE` · `ANONYMIZED` · `DELETED` | `allowedNext()` |
| `PhysicalStatus` | `READY` · `FATIGUE_RISK` · `INJURED` | `badge()`, `blocksCallUp()` |
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

## 11. Regras de anonimização (job diário, após 30 dias)

| Entidade | Ação |
|---|---|
| `User` | `email_enc`, `username`, `displayName`, `bio`, `city`, `avatarKey` → hash/`anon_{shortId}`; `status = ANONYMIZED` |
| Subclasses | campos identificadores (licença, CNPJ, press card) → nulos |
| `Session`, `rt:*`, `stepup:*` | removidos do Redis |
| `Post` | `text` → `[conteúdo removido]`, `mediaKeys` apagados do S3, autor → pseudônimo |
| `TelemetryData` | `gps_track` apagado; métricas agregadas mantidas sem vínculo nominal |
| `InjuryRecord` | removido fisicamente |
| `WeeklyScoreSnapshot`, `AthleteBadge` | mantidos como agregados (`D`) ligados ao pseudônimo |
| `ConsentAuditLog`, `TermsAcceptance` | mantidos (obrigação legal de accountability) com `userId` pseudonimizado |
| `DataExportRequest` | objetos S3 apagados; registro marcado `EXPIRED` |
