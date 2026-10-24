# Football AI · Social

**Identity · Feed · Rarity System** — rede social e técnica do futebol para atletas, comissões, clubes, scouts, marcas,
imprensa e ligas. Java 17 · Spring Boot 3.3 · MySQL 8 · Redis 7 · Cassandra 4.1 · S3/MinIO · SPA estática.

> Dados pessoais de exemplo aparecem sempre como `<PREENCHER_*>`. Segredos entram **só** por variável de ambiente.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/PROMPT_FOOTBALL_AI_SOCIAL.md`](docs/PROMPT_FOOTBALL_AI_SOCIAL.md) | Especificação completa (RF1–RF6, RF4.1, RF5/RF23, RNF, entregáveis) |
| [`docs/ENTITY_TAXONOMY.md`](docs/ENTITY_TAXONOMY.md) | Entidades, atributos, classes LGPD, chaves Redis, tabelas Cassandra, enums, DTOs dos cards |
| [`docs/CARD_ANATOMY.md`](docs/CARD_ANATOMY.md) | Anatomia dos cards (`.pcard` / `.vcard`): bolinha, borda, age-chip, brasão de raridade, janela Temporada · Atributos |
| [`docs/diagrams/`](docs/diagrams/README.md) | PlantUML por RF (classes, atividades, estados, sequência, componentes) |
| [`docs/preview/spa/`](docs/preview/spa) | Prints da SPA (desktop e mobile) |
| [`docs/research/`](docs/research) | Pesquisas: tipos de usuário, novas abas, FLAIR, Explorar |

## Subindo o ambiente

```bash
# 1) infraestrutura local (MySQL, Redis, Cassandra + keyspace, MinIO + buckets)
docker compose up -d

# 2) API + SPA (perfil local: MySQL + Redis + MinIO; Cassandra desligado → feed via MySQL)
export APP_JWT_SECRET="$(openssl rand -base64 64)"
export APP_CRYPTO_KEY="$(openssl rand -base64 32)"
mvn spring-boot:run

# 3) stack completa com Cassandra (fan-out dos feeds e telemetria)
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

SPA: <http://localhost:8080/> · health: <http://localhost:8080/actuator/health> · MinIO console: <http://localhost:9001>.

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:mysql://localhost:3306/football_ai_social` / `fas` / `fas` | MySQL (Flyway `V1__init_schema.sql`) |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / vazio | sessões, blacklist, rate limit, cache de feed |
| `CASSANDRA_HOST` / `CASSANDRA_DC` | `localhost` / `dc1` | só com `app.cassandra.enabled=true` (perfil `docker`) |
| `S3_ENDPOINT` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` | MinIO local | mídias (`fas-media`) e exportações LGPD (`fas-lgpd-exports`, 7 d) |
| `APP_JWT_SECRET` | *(dev)* | HS512 — use ≥ 64 bytes em produção |
| `APP_CRYPTO_KEY` | *(dev)* | AES-256-GCM (base64 de 32 bytes) para PII em repouso |

## Arquitetura do código (`src/main/java/com/footballai/social`)

```
config/      SecurityConfig (JWT stateless + CORS + CSP), JwtAuthenticationFilter, RateLimitFilter (Redis, 429),
             RedisConfig, CassandraConfig (condicional), S3Config, CryptoConverter (AES-GCM), AppProperties
model/       enums com tokens visuais (UserRole.ringColor, PostContentType.borderColor/vslGlyph, RarityTier.crestIcon,
             AgeRating, TrendDirection…) · entidades JPA (User JOINED + 11 papéis, DigitalTwin, WeeklyScoreSnapshot,
             AthleteBadge, InjuryRecord, Post, UserLgpdPreferences, ConsentAuditLog, DataExportRequest…) · Cassandra (FeedEntry, TelemetryData)
repository/  Spring Data JPA (+ Specification) e Cassandra (repository/cassandra)
service/     AuthService, JwtTokenService, RedisSessionService, RateLimitService, RarityEngineService (@Scheduled),
             BadgeAwardService, FeedWindowService (+ FeedQuery), PostService, CardAssembler, LgpdService, StorageService
controller/  AuthController, UserController, FeedWindowController, PostController, LgpdController, GlobalExceptionHandler, dto/
```

### Endpoints principais

| Método | Rota | RF |
|---|---|---|
| `POST` | `/v1/auth/register` · `/login` · `/refresh` · `/logout` · `/logout-all` · `/terms/accept` — `GET /v1/terms/current` | RF1/RF2 |
| `GET` | `/v1/feeds/{PERFIL\|GEMEO\|SCOUTING\|ALERTA}?…` — filtros segmentados por janela, `sort`, `cursor`, `size` | RF3 |
| `GET` | `/v1/ui/navigation` · `/v1/ui/tokens` — bottom bar (4 abas + FAB) e tokens de design | RF3 |
| `POST` | `/v1/posts` · `/{id}/like` · `/{id}/save` · `/{id}/share` — `GET /v1/posts/{id}` (PostView) | RF3 |
| `GET` | `/v1/me` · `PATCH /v1/me` · `/v1/athletes/{id}/card` · `/v1/athletes/{id}/badges/{type}/history` | RF4/RF4.1 |
| `POST` | `/v1/telemetry` · `PUT /v1/telemetry/plan` · `/v1/evaluations` · `/v1/matches/performances` · `/v1/injuries` · `/v1/injuries/{id}/clear` — `GET /v1/twins/{id}/evolution\|dna\|load` · `/v1/twins/compare?ids=` · `/v1/twins/me` | RF4 |
| `GET` | `/v1/rankings/athletes` · `/v1/rankings/clubs` · `/v1/clubs` (globo Explorar) — `POST /v1/admin/rarity/recalculate` (OFICIAL_LIGA) | RF3/RF4 |
| `POST` | `/v1/copilot/suggest` · `GET /v1/autopilot/drafts` · `GET /v1/cards/art-options` · `GET /v1/posts/mine` · `POST /v1/posts/{id}/publish` | RF3 |
| `GET/POST/PUT` | `/v1/flair/collection` · `/v1/flair/packs/open` · `/v1/flair/deck` · `/v1/flair/matches` · `/v1/flair/matches/{id}/rounds` · `/v1/flair/points` | RF6 |
| `GET/PUT` | `/v1/me/privacy` · `/v1/me/privacy/{preference}` (header `X-Step-Up-Token` para sensíveis) · `/v1/me/consent-history` | RF5 |
| `POST` | `/v1/me/step-up` · `/v1/me/export` · `GET /v1/me/exports` · `DELETE /v1/me` · `POST /v1/me/restore` | RF5 |

Filtros por janela (`GET /v1/feeds/…`): **PERFIL** `tier`, `position`, `ageMin/ageMax`, `clubId`, `contractStatus` ·
**GEMEO** `period`, `metricType`, `eventType`, `athleteId` · **SCOUTING** `attr[velocidade]=gt:80`, `compare=idA,idB`, `source` ·
**ALERTA** `severity`, `alertType`, `source` · comuns `sort=FOR_YOU|TRENDING|RECENT|FOLLOWING`, `role`, `state`, `q`, `cursor`, `size≤50`.

### Regras que o código garante

- **Privacy by default**: usuário nasce `SOMENTE_EU`, todos os consentimentos `FALSE`; dado médico só para o dono ou com `shareMedicalData`.
- **Rarity Engine determinístico**: 30 % físico · 40 % técnico · 30 % partidas (+ bônus de badges ≤ 300), `BigDecimal HALF_EVEN`,
  snapshot idempotente por `(twin, isoWeek, inputsHash)`; tiers `COMMON 0–400 · RARE 401–900 · EPIC 901–1600 · LEGENDARY 1601–2400 · MYTHIC 2401+`.
- **Raridade nunca em texto**: cada tier expõe `crestIcon` (`ic-tier-*`), o brasão único do Football Social.
- **Cards**: título obrigatório (5–12 palavras), teaser opcional, conteúdo só em PostView; atributos do atleta como lista de badges;
  hero limpo; janela segmentada Temporada · Atributos; barra Curtir · Comentar · Compartilhar · Salvar.
- **LGPD**: exclusão = soft delete com 30 dias de graça (login restaura), anonimização diária, exportação JSON com URL de 7 dias,
  step-up por senha para ações sensíveis, trilha de consentimento com base legal.

## Contas de exemplo (seed · perfis `local`/`docker`)

Senha de todas: `Senha@Forte123`. `atleta1@exemplo.dev` (Legendary) · `atleta2@exemplo.dev` (Mythic) · `atleta3`…`atleta5@exemplo.dev` · `scout@exemplo.dev` (homologado) · `tecnico@exemplo.dev` · `medico@exemplo.dev` · `clube1`…`clube3@exemplo.dev` · `imprensa@exemplo.dev` · `torcedor@exemplo.dev` · `oficial@exemplo.dev` (OFICIAL_LIGA / admin). Nomes aparecem como `<PREENCHER_*>`.

A SPA em <http://localhost:8080/> abre o modal de login; sem API alcançável (ou em “modo demonstração”) ela roda com dados fictícios e mostra `DEMO` no chip de identidade.

## Testes

```bash
mvn -q verify     # testes unitários: Rarity Engine, tiers, máquina de estados, badges, regras de post, filtros, JWT, cripto, LGPD, FLAIR, telemetria, Copilot
```

Os testes não exigem banco. Testes de integração com Testcontainers (MySQL/Redis/Cassandra) são o próximo passo.

## Scripts (Python 3.11+)

```bash
pip install -r scripts/requirements.txt
python scripts/trello_setup.py --workspace <id> --dry-run             # Kanban RF1…RF6 + DOCS
python scripts/ecosystem_discovery_agent.py --country BR --limit 300  # clubes (Wikidata) para o globo Explorar
python scripts/enrichment_agent.py --in seed.json --out enriched.json # badges, status físico e prévia de score (mesmas regras do backend)
```

## Front-end

`src/main/resources/static/` — `index.html`, `styles.css`, `app.js`, `icons.svg` (mini-brasões derivados do logo oficial) e `brand/`.
SPA ligada à API (JWT + refresh automático, fallback demo): 4 janelas com painel de filtros, cards `.pcard`/`.vcard`, criador de cards em
4 etapas (modo → conteúdo → arte → revisar), Copilot/Autopilot, FLAIR, rankings, globo, evolução/carga/DNA e Central LGPD.
