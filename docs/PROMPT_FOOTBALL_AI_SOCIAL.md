# PROMPT — Inicialização do repositório `football-ai-social`

Você é um **Engenheiro de Software Principal, Arquiteto de Sistemas Distribuídos e Tech Lead Java/Spring**. Sua missão é inicializar e estruturar o repositório completo do ecossistema **"Football AI · Social"** (`football-ai-social`) com **Java 17, Spring Boot 3.x, Spring Security 6, Spring Data JPA (MySQL 8), Spring Data Redis e Spring Data Cassandra**, seguindo **Arquitetura Limpa / DDD** (Domain → Application → Infrastructure → Web).

A plataforma é uma rede social técnica de alta performance para o futebol, conectando **Atletas, Comissões Técnicas, Clubes, Scouts, Marcas, Imprensa e Entidades Oficiais** em uma experiência rica em dados, telemetria e engajamento. Todo o código deve compilar (`mvn -q verify`) e os diagramas devem renderizar em PlantUML.

---

## 1. REQUISITOS FUNCIONAIS (RF) E DE NEGÓCIO

### RF1 — Autenticação, Sessão e Segurança de Fronteira
- JWT **Access Token (15 min)** + **Refresh Token (7 dias) com rotação** e revogação do token anterior a cada refresh.
- **Sessões no Redis** (`session:{userId}:{sessionId}`), permitindo: limite de sessões simultâneas, listagem de dispositivos e **logout em tempo real** (invalidação imediata em todas as abas, inclusive anônimas, via TTL + blacklist de `jti`).
- **Rate limiting** por IP e por usuário (Bucket4j + Redis) no filtro de borda.
- **Reautenticação obrigatória** (step-up) para alterações sensíveis: e-mail, senha, exclusão de conta.

### RF2 — Cadastro Polimórfico, Termos de Uso e Identidade Visual (Anel/Bolinha de Perfil)
- **Aceite obrigatório e versionado** dos Termos de Uso e da Política de Privacidade da **Velion (proprietária da Orange Social)** — entidade `TermsAcceptance` com `termsVersion`, `acceptedAt`, `ipAddress`, `userAgent`. Login bloqueado enquanto houver versão vigente não aceita.
- **Herança JPA** (`@Inheritance(strategy = JOINED)`) a partir de `User`, com um `UserRole` e uma cor de anel universal:

| UserRole | Anel (cor / hex) | Foco |
|---|---|---|
| `ATLETA` | Verde `#15805C` | Telemetria, treino diário, currículo esportivo |
| `TECNICO_STAFF` | Azul Claro `#5BA3E6` | Pranchetas táticas, treinos, gestão de elenco |
| `CLUBE` | Azul Escuro `#1E3A8A` | Gestão institucional, plantel, oportunidades |
| `SCOUT` | Amarelo `#D4A017` | Relatórios de observação, dados táticos |
| `MARCA` | Roxo `#7C3AED` | Patrocínio e ativação comercial |
| `IMPRENSA` | Laranja `#EA580C` | Cobertura e comunicados oficiais |
| `OFICIAL_LIGA` | Dourado `#C9A227` | Entidades organizadoras e arbitragem |

- Arquitetura **extensível** (Strategy + polimorfismo JPA + `RoleCapability`), para que **Agentes Inteligentes** registrem novos papéis sem alterar o core (ex.: `AGENTE_IA`, `PREPARADOR_FISICO`).

### RF3 — SPA, Arquitetura de Navegação Móvel e Classificação de Cards (UI/UX)
- API REST v1 pronta para SPA/Mobile. Desktop: **Sidebar lateral + Topbar**. Mobile: **Bottom Navigation / Tab Bar** com **limite estrito de 4 abas primárias** na zona de alcance do polegar (*Thumb Zone*), mapeadas 1:1 ao enum `FeedWindow` (`order`, `icon`, `label`, `route`), cada uma com rota própria, paginação por cursor (Cassandra) e cache (Redis):
  1. `PERFIL` — Descoberta & Social Feed (interação aberta, destaques, conquistas).
  2. `GEMEO` — Telemetria & Performance (treinos estilo Strava: carga, RPE, distância, estatísticas acumuladas).
  3. `SCOUTING` — Prancheta Tática & Análises (estilo Gaffer, vídeo, relatórios de observação).
  4. `ALERTA` — Status de Saúde, Lesões, Comunicados & DM (avisos de clube, convocações, mensagens diretas).
- **Regras da Bottom Bar** (documentar em `docs/ui/navigation.md` e expor em `GET /v1/ui/navigation`):
  - A barra abriga **somente caminhos de navegação**; ações de criação só via **Floating Action Button central elevado** (`POST /v1/posts` com `PostContentType` escolhido no FAB).
  - **Ocultação dinâmica**: oculta no scroll para baixo no feed e reaparece no scroll para cima (área útil de leitura).
  - **Navegação por gestos**: swipe lateral entre as 4 janelas independentes, preservando estado/cursor de cada feed.
  - **Acessibilidade (WCAG 2.1 AA)**: contraste mínimo **3:1** em ícones e rótulos da barra; rótulos nunca truncados; alvos ≥ 44×44 pt; animações curtas e respeitando `prefers-reduced-motion`.
- **Classificação estrita de Cards** = *Bolinha do Autor* (`UserRole`) + *Borda de Conteúdo* (`PostContentType`):

| PostContentType | Borda | Janela padrão | Conteúdo |
|---|---|---|---|
| `TREINO` | Verde | GEMEO | Carga/telemetria do atleta |
| `PARTIDA_MOMENTO` | Azul | PERFIL | Lances e atuações de jogo |
| `RELATORIO_TATICO` | Amarela | SCOUTING | Esquemas, drills, análises da comissão |
| `INSTITUCIONAL` | Cinza | ALERTA | Comunicados de clubes/marcas |
| `LESAO_ALERTA` | Vermelha | ALERTA | Status médico e urgências (**dado sensível**) |

### RF4 — Core Domain · Rarity Engine & Gêmeo Digital (Digital Twin)
- Cálculo **determinístico e semanal** (job agendado, idempotente por `userId + isoWeek`) do `Score_Total` (0 a 3000+):
  - 30% Telemetria física + assiduidade nos treinos.
  - 40% Avaliações técnicas da comissão/comunidade.
  - 30% Desempenho e minutagem em partidas oficiais.
- Mesma entrada ⇒ mesma saída (sem `Random`, sem relógio dentro do cálculo; `BigDecimal` com `RoundingMode.HALF_EVEN`).
- **Tiers de Raridade** aplicados ao Card do Perfil/Gêmeo (`RarityTier`, com `minScore`, `maxScore`, `visualEffect`):

| Tier | Faixa | Efeito visual |
|---|---|---|
| `COMMON` | 0–400 | Borda padrão simples |
| `RARE` | 401–900 | Glow azul suave |
| `EPIC` | 901–1600 | Partículas roxas + destaque de estatística |
| `LEGENDARY` | 1601–2400 | Moldura Branco + Dourado |
| `MYTHIC` | 2401+ | Aura dinâmica reativa + badges de MVP |

- **Máquina de Estados** do Gêmeo (`TwinStatus`): `DRAFT → ACTIVE → SUSPENDED → PENDING_DELETE → ANONYMIZED | DELETED`, com transições validadas no domínio (`DigitalTwin.transitionTo()` lança `InvalidTwinTransitionException`).

### RF4.1 — "Athletic Persona": Badges Gamificados e Status Físico no Card do Atleta
Experiência imersiva inspirada em cards de jogos e redes de telemetria esportiva. Modelar `AthleteBadge` (entidade, FK para `User`/`DigitalTwin`, `badgeType`, `counter`, `season`, `awardedAt`, `awardedBy`, `active`) e o enum `BadgeType` com `category`, `emoji`, `color`, `priority`, `quantitative`:

| Categoria | BadgeType | Emoji / cor | Regra de concessão |
|---|---|---|---|
| **Estatísticas da Temporada** (quantitativas) | `ARTILHEIRO` (Chuteira de Ouro) | ⚽ | Contador de gols em oficiais + amistosos |
| | `GARCOM` (Rei das Assistências) | 🎯 | Contador de assistências/passes decisivos |
| | `PAREDAO` (goleiro) / `XERIFAO` (defensor) | 🧱 | Clean sheets / alta taxa de desarmes limpos |
| | `MOTORZINHO` (Minutagem) | ⏱️ | Minutos acumulados em campo na temporada |
| **Condição Física & Status Médico** (telemetria, **mutuamente exclusivos**) | `PRONTO_PRA_JOGO` | ⚡ verde | Rotina de treinos cumprida, capacidade 100% |
| | `DEPARTAMENTO_MEDICO` | 🚑 vermelho | Em tratamento: `injuryType`, `expectedReturnDate`; **bloqueia convocações automáticas** |
| | `RISCO_FADIGA` | ⚠️ amarelo | RPE × carga dos últimos treinos indicam sobrecarga (ACWR > 1.5) |
| **Sociais, Táticos e Gamificados** | `MVP` | 🏆 | Acumulativo; eleito melhor da partida pela comissão ou grupo |
| | `MESTRE_TATICO` | 🧠 | Atribuído pelo treinador por execução precisa da prancheta |
| | `EM_CHAMAS` (Streak) | 🔥 animado | Sequência de notas altas ou presença consecutiva em treinos |
| | `LIDER_CAPITAO` | 👑 | Capitães e alta avaliação positiva entre colegas |
| | `FAIR_PLAY` | 🛡️ | Baixíssimo histórico de cartões e alta disciplina |

- **Status físico** é um estado do Gêmeo (`PhysicalStatus`: `READY`, `INJURED`, `FATIGUE_RISK`) derivado da telemetria e do registro médico (`InjuryRecord`); o badge correspondente é projetado a partir dele, nunca editado manualmente. Dados médicos são **sensíveis (Art. 11)**: só aparecem para terceiros se `shareMedicalData = true`.
- **Micro-Badges no topo do Card**: `AthleteCardDTO.topBadges` retorna até **3 badges prioritários** (ordenação: status médico ativo > `priority` > `counter`), para leitura rápida por scouts e treinadores.
- **Tap to Expand**: `GET /v1/athletes/{id}/badges/{badgeType}/history` alimenta o *Bottom Sheet* com o histórico completo (série temporal de gols, evolução de minutagem, histórico médico), paginado.
- Os badges alimentam o **Rarity Engine** (RF4) como entradas das parcelas de avaliação técnica (40%) e desempenho em partidas (30%); `BadgeAwardService` é determinístico e auditável (`BadgeAuditLog`).

### RF5 — Central de Compliance LGPD / Privacy by Design (Lei nº 13.709/2018)
Modelar a **Central de Privacidade** exatamente como a *Anatomia da Tela* de referência (padrão Meta/Google), com **5 seções** navegáveis e cada controle etiquetado com o artigo da LGPD que cumpre (`lgpdArticle` no DTO):

| Seção (rota SPA) | Controles | Artigo LGPD | Endpoint |
|---|---|---|---|
| **Conta** `/configuracoes/conta` | Nome de exibição, usuário, e-mail (fluxo de verificação), data de nascimento (só verificação de idade), biografia, posição/clube | Art. 18, III (Correção) · Art. 7º, V · Art. 6º, III | `GET/PUT /v1/me/profile` |
| **Privacidade** `/configuracoes/privacidade` | Radio "Quem pode ver seu perfil": `SOMENTE_EU` (padrão) / `CONEXOES` / `PUBLICO`; toggles de consentimento por finalidade | Art. 18, VII · Art. 7º, I · Art. 11 · Art. 18, IX | `GET/PUT /v1/me/privacy` |
| **Aparência** `/configuracoes/aparencia` | Modo escuro, tamanho de fonte (preferência de interface — **não é dado pessoal**) | — | `PUT /v1/me/preferences/ui` |
| **Idioma** `/configuracoes/idioma` | `pt-BR` / `en-US` / `es` persistido no perfil | Art. 6º, IV e VI (transparência no idioma do titular) | `PUT /v1/me/preferences/locale` |
| **Seus dados** `/configuracoes/seus-dados` | "Baixar seus dados" (JSON) · "Histórico de atividades" · Zona de risco "Excluir conta" | Art. 18, I, II, V · Art. 18, VI | `POST /v1/me/data-export` · `DELETE /v1/me` · `POST /v1/me/cancel-deletion` |

- **Toggles de consentimento (`UserLgpdPreferences`) — TODOS `FALSE` por padrão (opt-in)**, um por finalidade, revogáveis no mesmo lugar e com o mesmo esforço (Art. 8º, §5º):
  - `shareTelemetryWithClubs` — compartilhar telemetria com clubes.
  - `shareTelemetryWithScouts` — compartilhar telemetria com scouts.
  - `shareMedicalData` — status de saúde/lesão (**dado sensível, Art. 11**, badge rosa).
  - `shareBiometricData` — reconhecimento facial em vídeos/fotos (**sensível, Art. 11**).
  - `locationHistory` — GPS de treinos/jogos.
  - `shareWithBrands` — ativação comercial com marcas parceiras.
  - `personalizedAds` — anúncios personalizados.
  - `aiAgentProcessing` — processamento por agentes de IA (Rarity Engine avançado).
- Registrar cada mudança em `ConsentAuditLog` (quem, quando, de→para, IP) — princípio da **responsabilização** (Art. 6º, X).
- **Portabilidade (Art. 18, V):** `POST /v1/me/data-export` gera payload JSON completo (perfil, preferências, gêmeo, telemetria, posts, aceites de termos) em formato legível por máquina; export assíncrono com link no S3 e expiração de 7 dias.
- **Exclusão com período de graça (Art. 18, VI):** `DELETE /v1/me` exige **confirmação dupla + reautenticação**, muda `User.status` e `DigitalTwin.status` para `PENDING_DELETE`, grava `deletionRequestedAt` e `scheduledPurgeAt = +30 dias`, revoga todas as sessões. Job diário anonimiza (hash irreversível de PII, remoção de mídias) e marca `ANONYMIZED`; durante os 30 dias o login reativa a conta (`cancel-deletion`).
- **Anti-dark-patterns (obrigatório):** nenhum checkbox pré-marcado; nenhum consentimento agrupado; revogação em 1 clique; padrões mais protetivos; linguagem clara no idioma do usuário; confirmação explícita para ações destrutivas.

### Requisitos Não Funcionais (RNF)
- PII criptografada em repouso (AES-256 via `AttributeConverter`), senhas com BCrypt(12).
- Logs sem PII; auditoria estruturada (JSON).
- Observabilidade: Actuator + Micrometer + OpenTelemetry.
- Testes: JUnit 5 + Testcontainers (MySQL, Redis, Cassandra) para o Rarity Engine e o fluxo LGPD.

---

## 2. ENTREGÁVEIS DO REPOSITÓRIO

### 2.1 Diagramas em `docs/diagrams/` (PlantUML `.puml`)
1. **`class_diagram.puml`** — Modelo de Domínio Rico: `User` (abstrata) e subclasses por papel, `TermsAcceptance`, `UserLgpdPreferences`, `ConsentAuditLog`, `DigitalTwin`, `WeeklyScoreSnapshot`, `AthleteBadge`, `InjuryRecord`, `BadgeAuditLog`, `Post`, `TacticalDrill`, `TelemetryData`, `DataExportRequest`; enums `UserRole` (com `ringColor`), `PostContentType` (com `borderColor`, `defaultWindow`), `FeedWindow` (com `order`, `icon`, `route`), `RarityTier`, `RewardTier`, `TwinStatus`, `BadgeType` (com `category`, `emoji`, `priority`), `BadgeCategory`, `PhysicalStatus`, `ProfileVisibility`, `AccountStatus`.
2. **`twin_state_machine.puml`** — Ciclo de vida completo do Gêmeo Digital, progressão/regressão de Tiers por recálculo semanal e transição para `PENDING_DELETE → ANONYMIZED/DELETED` (com guarda de 30 dias e cancelamento).
3. **`component_diagram.puml`** — Front-end SPA (Sidebar, Topbar, 4 Janelas de Feed, Central de Privacidade), API Gateway (JWT + Rate Limit), Spring Boot Core (módulos Auth, Identity, Feed, Rarity Engine, LGPD Compliance), Agentes de IA em Python (gRPC/REST), MySQL 8 (RDBMS), Redis (sessões/cache/rate limit), Cassandra (feeds e telemetria de alta vazão), S3 (mídias e exports LGPD).
4. **`lgpd_privacy_center.puml`** *(bônus)* — Diagrama de sequência dos fluxos "Exportar dados" e "Excluir conta" (graça de 30 dias).

### 2.2 Estrutura de código (`src/main/java/com/footballai/social/`)
```
domain/          entidades, value objects, enums, serviços de domínio, exceções
  identity/      User + subclasses, TermsAcceptance, UserRole, ProfileVisibility
  twin/          DigitalTwin, RarityTier, TwinStatus, PhysicalStatus, RarityCalculationService
  badge/         AthleteBadge, BadgeType, BadgeCategory, InjuryRecord, BadgeAwardService
  feed/          Post, PostContentType, FeedWindow, TacticalDrill
  telemetry/     TelemetryData (modelo Cassandra)
  lgpd/          UserLgpdPreferences, ConsentAuditLog, DataExportRequest, LgpdComplianceService
application/     casos de uso (use cases), DTOs, ports (interfaces de repositório)
infrastructure/  adaptadores JPA/Cassandra/Redis, Spring Security (JWT, filtros), config, jobs
web/             controllers REST v1, handlers de exceção (@RestControllerAdvice)
```

### 2.3 Código crítico implementado (Java 17)
- Entidades JPA e Enums completos (Hibernate 6 / Jakarta Persistence), `records` para DTOs.
- **`RarityCalculationService.java`** — algoritmo determinístico 30/40/30, normalização 0–3000+, mapeamento `RarityTier.fromScore(int)`, snapshot semanal idempotente.
- **`LgpdComplianceService.java`** — exportação JSON (Art. 18, V), solicitação/cancelamento de exclusão com soft-delete de 30 dias, job de anonimização (Art. 18, VI), auditoria de consentimento.
- **`JwtTokenService` + `RedisSessionService`** — emissão/rotação de tokens e invalidação de sessão em tempo real.
- **`BadgeAwardService.java`** — concessão determinística de badges a partir de estatísticas/telemetria, derivação de `PhysicalStatus` (exclusivo), bloqueio de convocação quando `INJURED`, projeção dos 3 `topBadges` e histórico para o *Bottom Sheet*.
- **`V1__init_schema.sql`** (Flyway, MySQL 8, `utf8mb4`) — tabelas `users`, `user_athletes`…, `terms_acceptances`, `user_lgpd_preferences` (todos os booleanos `DEFAULT FALSE`), `consent_audit_log`, `digital_twins`, `weekly_score_snapshots`, `athlete_badges`, `injury_records`, `badge_audit_log`, `posts`, `tactical_drills`, `data_export_requests`, índices e FKs.
- `application.yml` com perfis `local`/`docker`, `docker-compose.yml` (MySQL 8, Redis 7, Cassandra 4, MinIO) e `README.md` com instruções de execução.

### 2.4 Critérios de aceite
- `mvn -q verify` passa; diagramas renderizam sem erro.
- Novo usuário nasce com visibilidade `SOMENTE_EU` e todos os consentimentos `FALSE`.
- Recalcular o mesmo conjunto de entradas produz o mesmo `Score_Total` e o mesmo `RarityTier`.
- `DELETE /v1/me` não apaga fisicamente nada antes de 30 dias e o login no período de graça reativa a conta.
- `AthleteCardDTO.topBadges` nunca excede 3 itens; um atleta `INJURED` exibe 🚑 em primeiro lugar e é recusado em convocações automáticas.
- Dados de lesão/status médico só aparecem no card para terceiros quando `shareMedicalData = true`.
