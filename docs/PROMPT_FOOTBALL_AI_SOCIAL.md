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

| UserRole | Anel (token / hex · contraste WCAG sobre `--ink #0a0f0c`) | Foco | Sub-tipo / verificação |
|---|---|---|---|
| `ATLETA` | Verde `--c-green #00d47a` · 9,85 | Telemetria, treino diário, currículo, Gêmeo | `categoria`, `menorDeIdade` (Art. 14) |
| `STAFF` (ex-`TECNICO_STAFF`) | Azul claro `#7fb6ff` · 9,24 | Pranchetas, treinos, gestão de elenco e esportiva | `StaffSpecialty`: TREINADOR, AUXILIAR, TREINADOR_GOLEIROS, PREPARADOR_FISICO, FISIOLOGISTA, ANALISTA_DESEMPENHO, GESTAO (diretor esportivo), OUTRO |
| `STAFF_SAUDE` | Carmim `#e11d48` · 4,11 | Status médico, reabilitação, nutrição, psicologia | `HealthSpecialty`: MEDICO (CRM), FISIOTERAPEUTA (CREFITO), NUTRICIONISTA (CRN), PSICOLOGO (CRP), MASSAGISTA — único papel além do atleta que origina `InjuryRecord`/`LESAO_ALERTA` |
| `CLUBE` | Azul `--c-blue #3b82f6` · 5,25 | Institucional, plantel, oportunidades, mural | `ClubeTipo`: PROFISSIONAL, FORMADOR_CCF, ESCOLINHA_ACADEMIA, AMADOR |
| `SCOUT` | Amarelo `--c-yellow #f7c948` · 12,34 | Observação, relatórios, shortlists | `VerificationLevel`: COMUNIDADE, VERIFICADO, HOMOLOGADO — só VERIFICADO+ conta no Rarity Engine |
| `AGENTE` | Teal `#2dd4bf` · 10,38 | Representação, mercado, oportunidades | `fifaLicenseId` (RNAF/CBF); representações com consentimento do atleta |
| `MARCA` | Roxo `--c-purple #a06bff` · 5,56 | Patrocínio e ativação comercial | `MarcaCategoria`: PATROCINADOR, MATERIAL, AGENCIA, APOSTAS (18+, advertência, exclui atletas/staff/árbitros/liga, feature flag) |
| `IMPRENSA` | Laranja `--c-orange #ff9f43` · 9,47 | Cobertura, comunicados, criação de conteúdo | `MidiaTipo`: VEICULO_CREDENCIADO, JORNALISTA, CRIADOR_INDEPENDENTE, CANAL_PODCAST |
| `OFICIAL_LIGA` | Elite Gold `--gold #f5c542` · 11,92 (dourado reservado a contas oficiais e a Legendary/Mythic) | Entidades organizadoras (sem arbitragem) | `EntidadeTipo`: CONFEDERACAO, FEDERACAO_ESTADUAL, LIGA, ORGANIZADOR_TORNEIO |
| `TORCEDOR` | Rosa `#f472b6` · 7,30 | Acompanhar, reagir, colecionar cards (FLAIR) | `clubeDoCoracao`; sem Gêmeo/Rarity |
| `AGENTE_IA` | Ciano `#22d3ee` · 10,69 | Automação transparente (resumos, alertas, análises) | `operatedBy`, `modelId`, `purpose`, `disclosureVersion`; toda publicação rotulada "IA" |
| `ARBITRO` *(via registry)* | Ardósia `#95a89d` · 7,70 | Arbitragem, súmulas | `quadro`: ESTADUAL, RENAF, FIFA; `funcao`: ARBITRO, ASSISTENTE, VAR |
| `TORCIDA_ORGANIZADA` *(via registry)* | Magenta `#ec4899` · 5,48 | Conta coletiva de torcida | CNPJ / cadastro de membros (Lei 14.597/2023, art. 178) |

- **Bolinha (dot) do autor**: 13 px + halo de 3 px na cor do papel (`.dot.dc-*`), sempre à esquerda do `@handle · papel`; `✦` dourado = conta verificada. Seis papéis canônicos na UI (atleta verde · clube azul · scout amarelo · marca roxo · imprensa laranja · oficial dourado) e extensões com as cores acima.
- **11 papéis core + 2 reservados** (cores no enum, comportamento no `RoleRegistry`). Arquitetura **extensível** (Strategy + polimorfismo JPA + `RoleCapability(role, subtype, verificationLevel)`), para que **Agentes Inteligentes** registrem novos papéis sem alterar o core. Fundamentação e benchmark em [`docs/research/01_tipos_de_usuarios.md`](research/01_tipos_de_usuarios.md).
- Regras-chave do `RoleCapability`: `LESAO_ALERTA` só por ATLETA (próprio) ou STAFF_SAUDE verificado; `RELATORIO_TATICO` de SCOUT.COMUNIDADE/IMPRENSA é `OPINATIVA` e não entra nos 40 % do Rarity; `INSTITUCIONAL` só para contas de organização e STAFF.GESTAO/AGENTE verificados; `AGENTE_IA` sempre `aiGenerated = true`.

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
- **Painel de filtros segmentado por janela** (`<aside class="filters-panel">` dentro de cada aba; o endpoint de cada janela recebe os mesmos parâmetros):

| Janela | Filtros (query params de `GET /v1/feeds/{window}`) |
|---|---|
| **PERFIL** (Descoberta) | `tier[]` (COMMON…MYTHIC) · `position` · `ageMin`/`ageMax` · `clubId` · `contractStatus` (AGENTE_LIVRE \| CONTRATADO) |
| **GEMEO** (Telemetria) | `period` (WEEK \| MONTH \| SEASON) · `metricType` (FISICA \| TECNICA \| TATICA) · `eventType` (TREINO \| PARTIDA) · `athleteId` |
| **SCOUTING** (Análise) | `attr[velocidade]=gt:80`, `attr[passe]=gt:85`… (repetível) · `compare=idA,idB` (side-by-side) · `source` (HOMOLOGADO \| COMUNIDADE) |
| **ALERTA** (Comunicados) | `severity` (BAIXA \| MEDIA \| ALTA \| CRITICA) · `alertType` (TRANSFERENCIA \| LESAO \| PUNICAO \| INSTITUCIONAL) · `source` (IMPRENSA \| CLUBE \| OFICIAL_LIGA) |

- **Sub-abas (segment control) por janela** — nenhuma novidade vira aba primária ([`docs/research/02_novas_abas_e_features.md`](research/02_novas_abas_e_features.md)):
  - PERFIL: `Feed` · `Ranking` (clubes e jogadores por tier/score, posição, liga, idade) · `Explorar` (globo MapLibre com clubes do mundo via Wikidata/openfootball + busca de ligas/clubes) · `Momentos`
  - GEMEO: `Telemetria` · `Evolução` (linha do tempo de score/tier) · `Carga & Recuperação` (ACWR 7:28, prontidão 1–100, privado por padrão) · `DNA de jogo` (radar 5 eixos + estilos) · `Comparador` (≤ 3 atletas) · `Provador tático` (fit % por posição/esquema) · `Avatar 3D` · `Rascunhos Autopilot`
  - SCOUTING: `Pranchetas` · `Comparar` · `Relatórios` · `Lens` (scouting por vídeo; rostos exigem `shareBiometricData`)
  - ALERTA: `Tudo` · `Mural do Clube` · `Saúde` · `DM`
- **FAB → Copilot**: sugere tipo, texto e badges do card; humano aceita/edita. **Autopilot** (opt-in por fonte: wearable, súmula) gera só rascunhos privados, nunca com dados de saúde sem `shareMedicalData`, com revisão humana (LGPD Art. 20); desabilitado para sub-18.
- **Sidebar desktop (7 seções, 27 itens)**: DESCOBRIR (Feed, Buscar, Explorar, Clubes & Ligas, Lens) · MEU GÊMEO (7 sub-abas acima) · CARDS (Meus cards, Criar card, Copilot, Autopiloto) · PERFIL (Meu perfil, Histórico, Privacidade LGPD) · JOGAR E GANHAR (Momentos, FLAIR, Destaques/Rankings, Cupons) · LOJA (Pontos) · GESTÃO (Meu clube, Minha liga, Administração). Cada item declara os papéis que o veem; "☰ Mais" no mobile abriga JOGAR E GANHAR, LOJA, PERFIL e GESTÃO — nunca ações de criação.
- **Classificação estrita de Cards** = *Bolinha do Autor* (`UserRole`) + *Borda de Conteúdo* (`PostContentType`):

| PostContentType | Borda (`borderColor`) | VSL (`vslGlyph`) | Janela padrão | Conteúdo |
|---|---|---|---|---|
| `TREINO` | Verde `#00d47a` | ◇ identidade | GEMEO | Carga/telemetria do atleta |
| `PARTIDA_MOMENTO` | Azul `#3b82f6` | ◇ identidade | PERFIL | Lances e atuações de jogo |
| `RELATORIO_TATICO` (Scouting Report) | Laranja `#ff9f43` | ▭ instituição | SCOUTING | Relatórios, comparadores, pranchetas, análises |
| `LIGA` | Roxo `#a06bff` | ⬢ comunidade | PERFIL | Tabelas, rodadas, convocações e enquetes de liga/clube |
| `INSTITUCIONAL` | Vermelho `#ff5d5d` | ▭ instituição | ALERTA | Comunicados oficiais, punições, transferências |
| `LESAO_ALERTA` (sub-tipo de `INSTITUCIONAL`, `sensitive = true`) | Vermelho `#ff5d5d` + listra de risco (*hazard*) e badge 🩹 | ▭ instituição | ALERTA | Status médico e urgências (**dado sensível, Art. 11**) |
| `ESPECIAL` | Elite Gold `#f5c542` | ✦ valor | PERFIL | Conquistas, subidas de tier, MVP, momentos oficiais |

- **Common Border dupla**: `outer` com gradiente de 2 px na cor da borda + `inner` com fundo `--panel`; cantos 18 px (card) / 11 px (blocos internos); posts de alerta recebem `hazard` (listra diagonal vermelha).
- **Anatomia obrigatória do post (`.pcard`)** — *o feed nunca mostra texto corrido*:
  - `p-top`: tempo · glifo VSL · badge do tipo (na cor da borda) · `age-chip` de classificação (`AgeRating`: `SAFE` · `KIDS` · `12+` · `16+` · `18+` · `SENSÍVEL`).
  - `author`: bolinha do papel + `@handle · papel` (+ `✦` verificado, rótulo `IA` quando `aiGenerated`).
  - `title` **obrigatório** (5–12 palavras, ≤ 2 linhas, `clamp`); `subtitle` *teaser* opcional (≤ 2 linhas, 70 % de opacidade); o `content` completo só aparece em `PostView`; posts com mídia não exibem subtítulo (mídia 136 px).
  - `meta`: tags do contexto (`HOMOLOGADO`, `VELOCIDADE > 80`, clube, rodada…).
  - `actbar` em **todo** post: **Curtir · Comentar · Compartilhar · Salvar**, com contadores, mais o indicador de tendência (`TrendDirection`: 🔥 HOT · 📈 UP · ➖ FLAT · 📉 DOWN, por visualizações/interações em 24 h).
- **Card de atleta/perfil (`.vcard`)**: `ctop` (VSL + contexto + `age-chip` + **brasão de raridade**) · `imgblock` **hero limpo** (108 px, só foto/avatar — nunca stats, títulos ou badges sobrepostos) · `author` · `title` (`Nome — Posição`) · `vsub` · `sgrid` da temporada (Jogos · Gols ou Clean sheets · Assistências ou Desarmes · Minutos · Cartões · Nota média) · `phys` (prontidão, ACWR 7:28, status ⚡/⚠️/🩹 com tipo de lesão e retorno) · `bdgrow` (`BADGES` + até 3 *Athletic Persona*) · `statrow` (PP · RP · MP · SCORE) · `actbar` (Curtir · Comentar · Compartilhar · **Gêmeo →**). Anatomia completa em [`docs/CARD_ANATOMY.md`](CARD_ANATOMY.md).
- **Raridade nunca é escrita em texto na UI**: cada `RarityTier` tem um **brasão único do Football Social** (`crestIcon = ic-tier-{tier}`, derivado do logo: escudo tingido na cor do tier com gema — Common ◇, Rare, Epic ✦ — ou coroa — Legendary, Mythic com raios e aro branco); o nome do tier vai só em `aria-label`/tooltip e no JSON.
- **Relevância e filtros do feed**: abas `Para você · Trending · Recentes · Seguindo` (`sort=FOR_YOU|TRENDING|RECENT|FOLLOWING`) + `filterbar` com chips por eixo (ativo = `.hot`); todos os endpoints aceitam ainda `role[]`, `state`, `clubId`, `q` (nome), `cursor`, `size ≤ 50`.
- **Design tokens** (fonte: modelo *Football AI · Social*): `--ink #0a0f0c · --ink-2 #0f1613 · --panel #131b17 · --panel-2 #19231d · --line #26332b · --line-soft #1e2921 · --text #eaf2ee · --text-2 #95a89d · --text-3 #63756a · --green #00d47a / --green-hi #6cffc0 / --green-deep #00874e · --gold #f5c542 / --gold-hi #ffe08a / --gold-deep #b8902c · --c-green/--c-blue/--c-yellow/--c-purple/--c-orange/--c-red/--c-slate`; fontes Space Grotesk (display) · Inter (corpo) · JetBrains Mono (rótulos, números). Ícones da navegação = mini-brasões do logo oficial (`static/icons.svg`).

### RF4 — Core Domain · Rarity Engine & Gêmeo Digital (Digital Twin)
- Cálculo **determinístico e semanal** (job agendado, idempotente por `userId + isoWeek`) do `Score_Total` (0 a 3000+):
  - 30% Telemetria física + assiduidade nos treinos.
  - 40% Avaliações técnicas da comissão/comunidade.
  - 30% Desempenho e minutagem em partidas oficiais.
- Mesma entrada ⇒ mesma saída (sem `Random`, sem relógio dentro do cálculo; `BigDecimal` com `RoundingMode.HALF_EVEN`).
- **Tiers de Raridade** aplicados ao Card do Perfil/Gêmeo (`RarityTier`, com `minScore`, `maxScore`, `visualEffect`):

| Tier | Faixa | Efeito visual no card (`visualEffect`) | Brasão (`crestIcon`) |
|---|---|---|---|
| `COMMON` | 0–400 | Borda padrão simples (`--c-slate`) | `ic-tier-common` — escudo ardósia, gema ◇ |
| `RARE` | 401–900 | Glow azul suave | `ic-tier-rare` — escudo azul, gema facetada |
| `EPIC` | 901–1600 | Partículas roxas (`driftParticles`) + destaque de estatística | `ic-tier-epic` — escudo roxo, gema + ✦ |
| `LEGENDARY` | 1601–2400 | Moldura Branco + Dourado | `ic-tier-legendary` — escudo dourado, coroa, aro ouro |
| `MYTHIC` | 2401+ | Aura dinâmica reativa (`mythicAura`, `mythicPulse`) + badges de MVP | `ic-tier-mythic` — escudo dourado, coroa com raios, aro branco |

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
| | `DEPARTAMENTO_MEDICO` | 🩹 vermelho | Em tratamento: `injuryType`, `expectedReturnDate`; **bloqueia convocações automáticas** |
| | `RISCO_FADIGA` | ⚠️ amarelo | RPE × carga dos últimos treinos indicam sobrecarga (ACWR > 1.5) |
| **Sociais, Táticos e Gamificados** | `MVP` | 🏆 | Acumulativo; eleito melhor da partida pela comissão ou grupo |
| | `MESTRE_TATICO` | 🧠 | Atribuído pelo treinador por execução precisa da prancheta |
| | `EM_CHAMAS` (Streak) | 🔥 animado | Sequência de notas altas ou presença consecutiva em treinos |
| | `LIDER_CAPITAO` | 👑 | Capitães e alta avaliação positiva entre colegas |
| | `FAIR_PLAY` | 🛡️ | Baixíssimo histórico de cartões e alta disciplina |

- **Status físico** é um estado do Gêmeo (`PhysicalStatus`: `READY`, `INJURED`, `FATIGUE_RISK`) derivado da telemetria e do registro médico (`InjuryRecord`); o badge correspondente é projetado a partir dele, nunca editado manualmente. Dados médicos são **sensíveis (Art. 11)**: só aparecem para terceiros se `shareMedicalData = true`.
- **Badges na área inferior do Card** (`bdgrow`, nunca sobre a imagem hero): `AthleteCardDTO.topBadges` retorna até **3 badges prioritários** (ordenação: status médico ativo > `priority` > `counter`), exibidos como pills abaixo do bloco de condicionamento; gols/assistências/minutos já aparecem no `sgrid` e não se repetem no hero.
- **Tap to Expand**: `GET /v1/athletes/{id}/badges/{badgeType}/history` alimenta o *Bottom Sheet* com o histórico completo (série temporal de gols, evolução de minutagem, histórico médico), paginado.
- Os badges alimentam o **Rarity Engine** (RF4) como entradas das parcelas de avaliação técnica (40%) e desempenho em partidas (30%); `BadgeAwardService` é determinístico e auditável (`BadgeAuditLog`).

### RF6 — Modo FLAIR: jogo de cartas com os Cards do app (Jogar e Ganhar)
- Cada Card de Atleta/Clube vira **carta jogável** congelada por semana: `Card(subjectType, subjectId, snapshotWeek, tier, power)`.
- **Poder determinístico**: `Power = Score_Total(semana) × tierMult × (1 + Σ bonusBadges)` — `tierMult` COMMON 1,00 · RARE 1,05 · EPIC 1,10 · LEGENDARY 1,20 · MYTHIC 1,30; bônus `MVP` +3 %, `EM_CHAMAS` +2 %, `FAIR_PLAY` +1 % (cap 10 %); capitão +20 %. Cards de atletas `INJURED`/`PENDING_DELETE` não pontuam e o estado médico **nunca** aparece (Art. 11).
- **Loop**: coleção (pacotes grátis a cada 12 h + conquistas, pool e probabilidades visíveis) → escalação (`Deck` de 5 + capitão, restrição por tier) → duelo 1×1 em 3 lances (atacante escolhe o eixo Físico/Técnico/Partidas; "Snap" dobra) ou liga semanal de 8 → recompensas (`Pontos`, `Pack`, `Cupom` de marca com `shareWithBrands`, `Momento`, cosmético).
- **Regulatório (Brasil)**: sem dinheiro real, sem inscrição paga, sem prêmio monetário, **nenhum pacote pago aleatório** (ECA Digital, Lei 15.211/2025, art. 20); enquadra-se como fantasy sport gratuito (Lei 14.790/2023); sub-13 sem modo competitivo público; consentimento parental para menores (Art. 14); anti-vício (limite diário, sem "quase ganhou", desligável em 1 toque).
- Entidades: `Card`, `CardOwnership`, `Deck`, `Pack`, `Match`, `Reward`, `Season`, `Moment`, `PointsLedger`. Entra no roadmap após Rankings e Evolução (usa seus snapshots).

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
config/        SecurityConfig (JWT stateless + CORS), JwtAuthenticationFilter, RateLimitFilter (Redis),
               RedisConfig, CassandraConfig (perfil docker), S3Config (MinIO/AWS), JpaAuditingConfig
model/         enums com cores/tokens (UserRole, PostContentType, FeedWindow, RarityTier, AgeRating, VslGlyph,
               TrendDirection, TwinStatus, PhysicalStatus, BadgeType…) e entidades JPA
               (User JOINED + subclasses, TermsAcceptance, UserLgpdPreferences, ConsentAuditLog, DigitalTwin,
               WeeklyScoreSnapshot, AthleteBadge, InjuryRecord, Post, DataExportRequest) + modelos Cassandra
repository/    Spring Data JPA / Cassandra
service/       AuthService, JwtTokenService, RedisSessionService, RarityEngineService (@Scheduled, determinístico),
               BadgeAwardService, FeedWindowService, LgpdService
controller/    AuthController, UserController, FeedWindowController (filtros por janela), LgpdController,
               GlobalExceptionHandler; DTOs em `controller/dto` (records: PostCardDTO, AthleteCardDTO…)
```
Scripts auxiliares em `scripts/` (Python): `trello_setup.py`, `ecosystem_discovery_agent.py`, `enrichment_agent.py`.

### 2.3 Código crítico implementado (Java 17)
- Entidades JPA e Enums completos (Hibernate 6 / Jakarta Persistence), `records` para DTOs.
- **`RarityEngineService.java`** — algoritmo determinístico 30/40/30, normalização 0–3000+, mapeamento `RarityTier.fromScore(int)`, snapshot semanal idempotente.
- **`LgpdService.java`** — exportação JSON (Art. 18, V), solicitação/cancelamento de exclusão com soft-delete de 30 dias, job de anonimização (Art. 18, VI), auditoria de consentimento.
- **`JwtTokenService` + `RedisSessionService`** — emissão/rotação de tokens e invalidação de sessão em tempo real.
- **`BadgeAwardService.java`** — concessão determinística de badges a partir de estatísticas/telemetria, derivação de `PhysicalStatus` (exclusivo), bloqueio de convocação quando `INJURED`, projeção dos 3 `topBadges` e histórico para o *Bottom Sheet*.
- **`V1__init_schema.sql`** (Flyway, MySQL 8, `utf8mb4`) — tabelas `users`, `user_athletes`…, `terms_acceptances`, `user_lgpd_preferences` (todos os booleanos `DEFAULT FALSE`), `consent_audit_log`, `digital_twins`, `weekly_score_snapshots`, `athlete_badges`, `injury_records`, `badge_audit_log`, `posts`, `tactical_drills`, `data_export_requests`, índices e FKs.
- `application.yml` com perfis `local`/`docker`, `docker-compose.yml` (MySQL 8, Redis 7, Cassandra 4, MinIO) e `README.md` com instruções de execução.

### 2.4 Critérios de aceite
- `mvn -q verify` passa; diagramas renderizam sem erro.
- Novo usuário nasce com visibilidade `SOMENTE_EU` e todos os consentimentos `FALSE`.
- Recalcular o mesmo conjunto de entradas produz o mesmo `Score_Total` e o mesmo `RarityTier`.
- `DELETE /v1/me` não apaga fisicamente nada antes de 30 dias e o login no período de graça reativa a conta.
- `AthleteCardDTO.topBadges` nunca excede 3 itens; um atleta `INJURED` exibe 🩹 em primeiro lugar e é recusado em convocações automáticas.
- Dados de lesão/status médico só aparecem no card para terceiros quando `shareMedicalData = true`.
