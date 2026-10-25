# Pesquisa 02 — Novas abas e funcionalidades

**Candidatas:** Mural do Clube · Rankings · Copilot · Autopilot · Explorar (globo) · **+** Sub-abas do Gêmeo Digital · Modo FLAIR (jogo de cartas) · Arquitetura de informação inspirada no menu "FashionAI"

> Produto: **Football AI · Social** · Base: `docs/PROMPT_FOOTBALL_AI_SOCIAL.md` (RF2, RF3, RF4, RF4.1, RF5) e `docs/CARD_ANATOMY.md`
> Data: 2026-10-24 · Método: busca web + leitura das páginas listadas em "Referências". Páginas que **não abriram** (403/404/503) estão marcadas e só são citadas via fonte secundária ou resultado de busca. Textos legais brasileiros foram lidos **diretamente no Planalto** via `curl`.

---

## 1. Resumo executivo

1. Nenhuma das candidatas vira aba primária: **as 4 abas (PERFIL, GEMEO, SCOUTING, ALERTA) + FAB permanecem** (RF3). Material Design manda destinos excedentes para "outros locais" (drawer/"Mais"); Apple HIG limita a 5 tabs — a estratégia de sub-abas + Topbar + "Mais" é compatível.
2. **Rankings** (PERFIL → Ranking) e **Mural do Clube** (ALERTA → Mural) são as entregas de maior valor/esforço: reaproveitam o `WeeklyScoreSnapshot` e os posts `INSTITUCIONAL` já existentes. Esforço **M**, prioridade **Alta**.
3. **Copilot** vive no FAB e segue o padrão para o qual o LinkedIn migrou em 2026 — *sugerir e o humano aceita* (Review/Shorten/Clarify). **Autopilot** gera só **rascunhos privados**, opt-in por fonte, nunca dados de saúde sem `shareMedicalData`, com botão de revisão humana (LGPD art. 20).
4. **Explorar**: MapLibre GL JS ≥5 (BSD-3, `setProjection({type:'globe'})`, ~287 kB gz) como globo principal; react-simple-maps `geoOrthographic` (32 kB gz) como modo leve. Dados **CC0**: Wikidata (8.827 clubes com liga + coordenadas; 1.718 brasileiros — verificado via SPARQL) + openfootball/clubs.
5. **Gêmeo Digital** ganha 6 sub-abas (Meu Avatar 3D, Provador tático, DNA de jogo, Evolução, Comparador, Carga & Recuperação) modeladas em Garmin Training Readiness (6 inputs, faixas 1–100), WHOOP (Strain 0–21, Recovery 0–100%), Catapult (Player Load/ACWR), Sofascore Attribute Overview (5 eixos) e EA FC PlayStyles.
6. **FLAIR** (jogo de cartas) usa os Cards de Atleta/Clube como cartas; poder = `Score_Total` 30/40/30 + tier + badges; duelo semanal decidido por **desempenho real** (modelo Sorare). **Sem dinheiro real, sem loot box paga**: ECA Digital (Lei 15.211/2025, art. 20) veda caixas de recompensa em jogos de acesso provável por menores; a Lei 14.790/2023 define fantasy sport (equipes ≥2 pessoas reais, premiação garantida independente da arrecadação) e dispensa autorização.
7. A sidebar desktop é reorganizada em **7 seções** (DESCOBRIR, MEU GÊMEO, CARDS, PERFIL, JOGAR E GANHAR, LOJA, GESTÃO), reduzidas no mobile às 4 abas + FAB + "Mais".
8. Roadmap: Rankings → Mural → Copilot → Gêmeo (Evolução/Comparador/Carga) → Explorar 2D → FLAIR MVP → Autopilot → Avatar 3D/Provador → Explorar 3D.

---

## 2. Proposta de navegação revisada

Princípio: Bottom Bar com **4 destinos + FAB central** (inalterada). Tudo novo entra como (a) **sub-aba/segment control** dentro de uma janela, (b) **entrada da Topbar** (mobile) / **Sidebar** (desktop) ou (c) **camada do FAB**. `FeedWindow` mantém `order`, `icon`, `label`, `route`.

```
MOBILE
┌────────────────────────────────────────────────────────────────────┐
│ TOPBAR  [☰ Mais]           Football AI · Social        [🔍 Explorar]│
├────────────────────────────────────────────────────────────────────┤
│ Segment control da janela ativa (scroll horizontal, estado próprio) │
│                                                                     │
│ PERFIL   : [Feed] [Ranking] [Explorar] [Momentos]                   │
│ GEMEO    : [Telemetria] [Evolução] [Carga & Recup.] [DNA] [Avatar]  │
│            [Provador] [Comparador] [Rascunhos Autopilot ●]          │
│ SCOUTING : [Pranchetas] [Comparar] [Relatórios] [Lens]              │
│ ALERTA   : [Tudo] [Mural do Clube ▾] [Saúde] [DM]                   │
│                                                                     │
│ …cards…                                                             │
├────────────────────────────────────────────────────────────────────┤
│   PERFIL        GEMEO        (＋)        SCOUTING        ALERTA     │
└────────────────────────────────────────────────────────────────────┘
                                 │ FAB → PostContentType
                                 ▼
                     ┌──────────────────────────────┐
                     │ ✨ Copilot: sugere tipo/texto/ │
                     │ badges · [Aceitar][Editar][✗]  │
                     └──────────────────────────────┘

"☰ Mais" → JOGAR E GANHAR (FLAIR · Momentos · Destaques · Cupons)
          → LOJA (Pontos) → GESTÃO (clube/liga/admin, por papel)
          → Rankings completos · Privacidade LGPD · Configurações

DESKTOP — SIDEBAR (7 seções; detalhada na §10)
DESCOBRIR ─ Feed · Buscar · Explorar (globo) · Clubes & Ligas · Lens
MEU GÊMEO ─ Telemetria · Evolução · Carga & Recuperação · DNA de jogo
            · Comparador · Provador tático · Meu Avatar 3D
CARDS ───── Meus cards · Criar card · Copilot · Autopiloto
PERFIL ──── Meu perfil · Histórico · Privacidade (LGPD)
JOGAR E GANHAR ─ Momentos · FLAIR · Destaques/Rankings · Cupons
LOJA ────── Pontos
GESTÃO ──── Meu clube · Minha liga · Administração
```

Mapeamento Sidebar → Bottom Bar: DESCOBRIR→PERFIL · MEU GÊMEO→GEMEO · CARDS→FAB · GESTÃO/ALERTA institucional→ALERTA · SCOUTING mantém Pranchetas/Comparar/Lens · JOGAR E GANHAR, LOJA, PERFIL (config.)→"Mais".

---

## 3. Candidata 1 — Mural do Clube

**O que é.** Feed institucional *por clube*: comunicados, convocações, vagas, agenda, enquetes. Hoje `INSTITUCIONAL` cai em ALERTA misturado com `LESAO_ALERTA` e DM.

**Referências.** **OneFootball "Club Network"** — notícias "diretamente da fonte oficial" (Real Madrid, Man City, Monaco), seguir clubes/ligas/jogadores (App Store). Case **Chivas × OneFootball**: conteúdo editorial do clube com escudo marcando "oficial" e *push segmentado* por grupo. **Socios.com** — enquetes oficiais (até música pós-gol), recompensas, "Find your team" (60+ clubes); o voto ponderado por token não se aplica, o padrão *enquete oficial + recompensa* sim.

**Valor por papel**
| Papel | Valor |
|---|---|
| Atleta | Convocações, agenda, vagas sem ruído |
| Técnico | Canal oficial da comissão; agenda de treinos |
| Clube | Casa institucional; alcance; enquetes; vitrine de vagas |
| Scout | O que o clube promove; lesões agregadas (sem nomes); oportunidades |
| Marca | Ativações com selo "patrocinado" (respeita `shareWithBrands`) |
| Imprensa | Fonte primária verificada (`OFICIAL_LIGA` homologou) |
| Liga | Tabela/regulamento no mural dos afiliados |

**UI.** Sub-aba **ALERTA → Mural do Clube** (clube do usuário; seletor ▾ para seguidos) + rota `/clubes/{id}/mural` a partir do Card de Clube (nova ação `Mural`). Filtros: `Tipo` (Comunicado | Convocação | Vaga | Agenda | Enquete | Patrocinado) · `Período` · `Fixado` · `Só oficiais`. Push segmentado: Elenco / Seguidores / Todos.

**Entidades.** `ClubBoardPost extends Post (pinned, audience, boardType)`, `ClubPoll (options, closesAt, anonymous)`, `PollVote`, `ClubMembership`; Cassandra `club_board_by_club`.

**Esforço M · Prioridade Alta.**

---

## 4. Candidata 2 — Rankings

**Referências.** **Sofascore Rating** — parte de 6.5, cada estatística move a nota, ~60 recálculos por jogo, escala até 10 (artigo oficial; via busca, página 403). **Transfermarkt** — "sabedoria da multidão" moderada por "padrinhos" voluntários por liga, 4 atualizações/ano, 800 mil jogadores, desvio ~60% vs. taxas reais (Follow the Money). **FIFA SUM** — `P = Pbefore + I·(W − We)`, `We = 1/(10^(−dr/600)+1)`, `I` de 5 (amistoso fora de janela) a 60 (Copa a partir das quartas) (PDF oficial). **ClubElo** — Elo de clubes; **API de Fixtures desativada** (verificado via `curl`).

**Lições.** Nosso `Score_Total` já é determinístico, semanal e auditável; ranking útil mostra **Δ semanal** e **percentil por posição/idade**; ranking de clubes pode usar Elo interno (fórmula SUM em `BigDecimal`).

**Valor por papel.** Atleta: posição vs. pares, meta semanal · Técnico: benchmark do elenco · Clube: tier médio, marketing · **Scout: caso principal** (posição × idade × liga × `Agente Livre`) · Marca: ▲ de tier · Imprensa: pauta semanal · Liga: ranking oficial da competição.

**UI.** Sub-aba **PERFIL → Ranking**; rota `/ranking/{jogadores|clubes}`. Filtros jogadores: Posição, Faixa etária (sub-15…master), Liga/Divisão, País/Estado, Tier, Status contratual, Semana ISO. Clubes: Liga, País, Métrica (tier médio | prontos | MVPs | Elo). Linha = mini-card (posição, ▲▼Δ, anel, pill de tier, score, ≤2 badges). Só perfis `PUBLICO`/`CONEXOES`; "sua posição" sempre visível ao dono.

**Entidades.** `RankingSnapshot (isoWeek, scope, filtersHash, rank, refId, scoreTotal, tier, delta)` no job semanal; `ClubEloRating`; Redis sorted sets.

**Esforço M · Prioridade Alta.**

---

## 5. Candidata 3 — Copilot para criar cards

**Referências.** **LinkedIn** removeu em set./2026 o gerador "do zero" e lançou o **Post Proofreader** (Premium): *Review / Shorten / Clarify*, sugestões inline, **nada muda até o usuário aceitar** (Metricool). **Strava Athlete Intelligence** — resumo automático após upload, assinantes, opt-out e thumbs (TechCrunch, Android Central). **Notion AI** — "melhore a escrita ou gere rascunhos com blocos de IA" (página oficial).

**Lição.** Mercado migrou de *gerar por você* para *sugerir e você aceita*: preserva autoria (scout, imprensa) e evita decisão automatizada.

**UI.** Dentro do **FAB → compositor**: botão `✨ Sugerir` com *Classificar* (tipo+janela, chip com a cor da borda, nunca pré-marcado silenciosamente), *Melhorar texto* (Revisar / Encurtar / Clarear), *Destacar badges* (≤3, ordem do RF4.1). Guarda de dado sensível (menção a lesão sem `shareMedicalData` → sugere `LESAO_ALERTA` restrito). Feedback 👍👎. Gate pelo toggle `aiAgentProcessing`.

**Entidades.** `PostSuggestion (draftId, suggestedType, suggestedText, suggestedBadges[], model, promptVersion, accepted, feedback)`; `POST /v1/posts/suggest`, `POST /v1/posts/{id}/suggestion-feedback`.

**Esforço M · Prioridade Média-Alta.**

---

## 6. Candidata 4 — Autopilot para criar cards

**Referências.** **Strava ⇄ Garmin** — visibilidade segue o padrão do destino (Everyone/Followers/Only You), ajustável por atividade; Garmin privado por padrão e revogação em "Connected Apps" (Strava Support; Kaspersky). **Hudl** — highlights automáticos (Game Recap 2 min, Game Highlights 15 s por atleta, End-of-Season) só com jogo completo ≥50 clipes **em playlist pública** e tags de jogador. **WSC Sports** — ">1.000 pacotes de highlights em minutos", personalização por fã, parceiros NBA/LaLiga/DAZN. **WHOOP Coach** — resumos de sleep/strain/recovery (via busca; páginas oficiais 403).

**UI.** Sub-aba **GEMEO → Rascunhos do Autopilot** (badge ●); rota `/autopilot`. Fluxo: evento → rascunho privado → notificação → Publica/Edita/Descarta. "Publicar automaticamente" é um **segundo opt-in**, por fonte e tipo, só `TREINO` e `PARTIDA_MOMENTO`. Selo "Gerado automaticamente" + "Por que gerei isso?". Filtros: Fonte (Wearable | Súmula | Badge | Tier), Tipo, Status, Período.

**LGPD / consentimento (texto da Lei 13.709 lido no Planalto)**
- Telemetria = dado pessoal (art. 7º, I); FC, RPE, lesão, biometria = **sensível** (art. 5º, II) → art. 11, I: consentimento "**de forma específica e destacada, para finalidades específicas**"; art. 11 §4º veda vantagem econômica com dados de saúde → card automático de saúde nunca é patrocinado.
- Art. 8º §5º: revogação "a qualquer momento… por procedimento gratuito e facilitado" → toggles `autopilotDrafts`, `autopilotAutoPublish` (default `false`) em `UserLgpdPreferences`, por `ConnectedSource`, com `ConsentAuditLog`.
- Art. 20: direito a "solicitar a revisão de decisões tomadas unicamente com base em tratamento automatizado… inclusive… perfil… profissional" → botão **Pedir revisão humana** sempre que o Autopilot afetar score/convocação (ex.: `RISCO_FADIGA`). ANPD concluiu Tomada de Subsídios sobre IA (124 contribuições; Nota Técnica 12/2025) com consenso em transparência, explicabilidade e revisão humana em alto risco — regulação em andamento, desenhar pelo cenário mais protetivo.
- Art. 14: menores exigem consentimento específico e destacado de um dos pais → Autopilot **desligado para sub-18** até fluxo parental.
- Riscos: GPS (`locationHistory`), alucinação (caso Strava "atleta consistente" após queda) → humano no loop, poluição de feed (cap diário + agrupamento), terceiros em vídeo (`shareBiometricData`, desfoque).

**Entidades.** `ConnectedSource`, `AutopilotRule (sourceType, postType, mode DRAFT|AUTO)`, `AutopilotDraft (eventRef, payload, explanation, status)`, `AutomatedDecisionReviewRequest`. Fila de eventos idempotente por `eventId`.

**Esforço G · Prioridade Média.**

---

## 7. Candidata 5 — Explorar (globo + busca de ligas e clubes)

**UI.** 🔍 na Topbar + sub-aba **PERFIL → Explorar** + Sidebar DESCOBRIR; rota `/explorar`. Globo full-screen com bottom sheet de resultados. Filtros: País · Divisão/nível · Tier médio do plantel · Vagas abertas · Faixa etária média · Só verificados. Mobile: carrega **2D leve** primeiro; globo WebGL sob demanda; respeita `prefers-reduced-motion` e `saveData`. Card de clube reaproveita a anatomia (anel `#1E3A8A`).

**Valor por papel.** Atleta: clubes com vagas por região · Técnico: ligas-alvo · Clube: ser encontrado · **Scout: mapa de prospecção** · Marca: densidade regional · Imprensa: contexto geográfico · Liga: cobertura territorial.

**Entidades.** `Club` + `wikidataId, lat, lng, countryCode, leagueId, foundedYear, aliases[]`; `League (wikidataId, country, level)`; `ClubGeoAggregate (avgTier, readyCount, openingsCount)` semanal. Pipeline: Wikidata SPARQL → openfootball (aliases) → revisão de duplicatas.

**Esforço G (MVP 2D = M) · Prioridade Média.** Comparativo de libs na §11; fontes de dados na §12.

---

## 8. Frente A — Sub-abas funcionais do Gêmeo Digital

Hoje GEMEO = feed `TREINO`. Proposta: 7 sub-abas (segment control), todas alimentadas por entidades já previstas (`DigitalTwin`, `WeeklyScoreSnapshot`, `AthleteBadge`, `PhysicalStatus`, `InjuryRecord`) + novas abaixo. Dados de saúde só para terceiros com `shareMedicalData`.

| Sub-aba | O que mostra | Referência de mercado (o que copiar) | Dados/entidades novas | Esforço | Prioridade |
|---|---|---|---|---|---|
| **Telemetria** (atual) | Feed de treinos, carga, RPE, distância | Strava (feed de atividades) | — | — | — |
| **Evolução** (histórico) | Linha do tempo semanal de `Score_Total` e tier; marcos de badges; anotações do treinador | Transfermarkt (histórico de valor em curva); Sofascore (nota por partida) | `WeeklyScoreSnapshot` já existe; `TwinMilestone (week, type, ref)` | P | **Alta** |
| **Carga & Recuperação** | ACWR (acute:chronic), carga semanal, sono, prontidão 1–100 com faixas, `PhysicalStatus` | **Garmin Training Readiness**: 6 inputs (sono da noite, recovery time, HRV status, carga aguda, histórico de sono 3 noites, estresse 3 dias), faixas Prime 95–100 / High 75–94 / Moderate 50–74 / Low 25–49 / Poor 1–24 (manual oficial). **WHOOP**: Strain 0–21 (Light 0–9, Moderate 10–13, High 14–17, All Out 18–21), Recovery 0–100% (verde ≥67, amarelo 34–66, vermelho ≤33) com HRV como maior peso (Sleep Foundation). **Catapult**: Player Load, GPS/LPS/FC, 5.500+ equipes, ferramentas de lesão/recuperação; coaches leem ACWR como barras agudas vs. curva crônica. **Zepp**: Readiness a partir de FC de repouso no sono, HRV, respiração e temperatura; Exertion diário ajustado pela prontidão | `TrainingLoadDaily (date, load, rpe, duration)`, `RecoveryDaily (sleepHours, hrv?, restingHr?, readiness 1–100)`, cálculo ACWR 7:28 (já citado no RF4.1: >1,5 = `RISCO_FADIGA`) | M | **Alta** (dá sentido ao badge ⚠️/⚡ já previsto) |
| **DNA de jogo** | Radar de 5 eixos + "estilos" (Finalizador, Construtor, Velocista, Muralha, Motor, Líder) derivados de badges e avaliações | **Sofascore Attribute Overview**: Attacking, Technical, Tactical, Defending, Creativity (goleiro: saves, anticipation, tactical, distribution, aerial) — via busca, páginas 403. **EA FC PlayStyles/PlayStyles+**: traços assinatura baseados em dados Opta, em 6 áreas (Scoring, Passing, Ball Control, Defending, Physical, Goalkeeping); FC 25 tinha 34 estilos (via busca/EA) | `PlayDnaProfile (userId, season, axes{5}, styles[])` recalculado no job semanal; regras determinísticas (ex.: `ARTILHEIRO` + xG → Finalizador) | M | Média-Alta |
| **Comparador** | Side-by-side de 2–3 atletas (radar DNA, 30/40/30, badges, evolução) | **Wyscout**: Player Lists com *compare mode* (melhor/pior por stat), filtros por competição/período, Advanced Search exportável; **SciSports**: comparação de até 3 jogadores por papel (via busca) | Reaproveita ação `Comparar` do card; `ComparisonSet (ownerId, memberIds[≤3])` | P/M | **Alta** (já referenciado no CARD_ANATOMY) |
| **Provador tático** | Simula o atleta em outra posição/esquema: recalcula aderência (eixos DNA × requisitos da posição), mostra "fit" % e lacunas | EA FC (posições alternativas e química); Wyscout/SciSports (role fit); pranchetas da janela SCOUTING | `PositionProfile (position, requiredAxes{5})`, `TacticalFitResult` (cálculo on-the-fly, determinístico) | M | Média |
| **Meu Avatar 3D** | Avatar do atleta com atributos visuais por tier (moldura, aura MYTHIC), uniforme do clube, badges 3D | Ready Player Me (SDK web via WebGL/postMessage; **licença**: gratuito não-comercial, comercial exige parceria; proibido NFT — via busca, docs não resolveram DNS). Alternativa: avatar 2.5D em SVG/Lottie próprio | `AvatarConfig (userId, baseModel, skin, kitId, tierFxOverride?)`; assets por tier reaproveitando `RarityTier.visualEffect` | G | Baixa-Média (valor de marca; validar licença antes) |

Regras transversais: (i) **Carga & Recuperação** e qualquer métrica médica são privadas por padrão e **nunca** entram no FLAIR nem em rankings públicos; (ii) todo cálculo é determinístico/auditável, igual ao Rarity Engine; (iii) `prefers-reduced-motion` desliga animações do avatar.

---

## 9. Frente B — Modo "FLAIR": jogo de cartas com os Cards do app

### 9.1 Conceito
Cada **Card de Atleta/Clube** (tier COMMON→MYTHIC, badges, stats 30/40/30) vira uma **carta jogável**. O diferencial frente a TCGs genéricos: o poder da carta **é o desempenho real** da semana (modelo Sorare), não um número fixo — o Rarity Engine já produz isso.

### 9.2 Mecânicas de referência (o que aproveitar)
| Jogo | Mecânica observada | Aproveitar como |
|---|---|---|
| **Sorare** | 4 escassez (Limited 1.000 / Rare 100 / Super Rare 10 / Unique 1); escalação de 5; pontuação por desempenho real (Opta) incl. ações "all-around"; torneios quinzenais gratuitos; cartas comuns grátis; **18+** (Premier League) | Escalação 5 + capitão; pontuação = `Score_Total` semanal; divisões por tier |
| **EA FC Ultimate Team — Rivals** | Pontos semanais (3 vitória/1 empate), 15 pts = recompensa base, 30 = upgraded; divisões com promoção/rebaixamento; recompensas às quintas; Bounties/Duels (FC 26) (Dexerto; página EA 503) | Liga semanal com dois patamares de recompensa; "Bounties" = desafios de posição |
| **Topps Match Attax** | Duelo carta × carta: atacante escolhe Ataque ou Defesa, maior valor vence o lance (Topps; página 403, via busca) | Duelo 1×1 por "lances": comparar Físico/Técnico/Partidas |
| **Panini Digital Album** (2018) | 2 pacotes grátis/dia, códigos, troca de repetidas, desafios "complete o time", sorteio ao completar | Pacotes **gratuitos** por tempo/atividade; troca de repetidas; álbum por clube/liga |
| **Pokémon TCG Pocket** | 2 pacotes grátis/dia (timer 12 h), *Wonder Pick* (escolher carta do pacote de outro jogador), cartas imersivas (site oficial + busca) | "Wonder Pick" = escolher 1 card de um pacote aberto por um colega de clube |
| **Marvel Snap** | Deck 12, 6 turnos, 3 locais com efeitos, energia crescente, **Snap** dobra os cubos em jogo, retiro limita perda, temporadas mensais (Wikipedia) | Partida curta (≤3 min); "3 locais" = 3 setores do campo com efeitos (ex.: "chuva: −10% técnico"); Snap = apostar pontos do app |
| **Hearthstone Battlegrounds** | 8 jogadores, fases Recruta/Combate, taverna por tiers, ouro não acumula, triplas viram dourada (Blizzard) | Modo 8 jogadores semanal; "tripla" de cards do mesmo atleta → versão dourada |
| **NBA Top Shot** | *Moments* (lances) em pacotes; Challenges queimam Moments por recompensa rara; Top Shot Score dá acesso a drops (site oficial) | Aba **Momentos**: `PARTIDA_MOMENTO` colecionáveis; desafios de coleção |

### 9.3 Loop de jogo
```
COLEÇÃO ──► ESCALAÇÃO ──► DUELO/LIGA SEMANAL ──► RECOMPENSAS ──┐
 ▲  pacotes grátis    5 cards + capitão   poder = Score_Total   pontos, pacotes,  │
 │  (tempo/atividade) restrição por tier  real da semana (job   cupons, Momentos  │
 └──────────────────────────────────────── do Rarity Engine)  ◄─────────────────┘
```
- **Poder da carta** (determinístico, `BigDecimal`): `Power = Score_Total(semana) × tierMult × (1 + Σ bonusBadges)`; `tierMult`: COMMON 1,00 · RARE 1,05 · EPIC 1,10 · LEGENDARY 1,20 · MYTHIC 1,30; bônus: `MVP` +3%, `EM_CHAMAS` +2%, `FAIR_PLAY` +1% (cap 10%). Capitão +20% (Sorare). **Cards de atletas `INJURED`/`PENDING_DELETE` não pontuam** e o estado médico **não aparece** (art. 11).
- **Lances do duelo 1×1** (3 rodadas, estilo Match Attax): quem ataca escolhe o eixo (Físico 30 / Técnico 40 / Partidas 30); maior valor vence o lance; empate → `FAIR_PLAY`. Snap (Marvel Snap) dobra os pontos em disputa.
- **Liga semanal**: 8 escalações (Battlegrounds), pontuação acumulada no fechamento do job semanal; 15/30 pontos → recompensa base/upgraded (Rivals).
- **Desafios de posição** (Bounties): "escale 3 defensores sub-20 do mesmo estado" → `Pack` extra.

### 9.4 Economia (sem dinheiro real, sem aposta)
- Moeda: **Pontos do app** (ganhos por treinos publicados, avaliações feitas, assiduidade, convites, participação em enquetes). **Não comprável** com dinheiro no MVP.
- `Pack`: **grátis** a cada 12 h (Pokémon Pocket) + por conquistas. Conteúdo **não aleatório pago**: o pacote mostra pool e probabilidades e nunca é vendido — evita a definição de *caixa de recompensa* do ECA Digital ("aquisição, mediante pagamento… de itens… ou vantagens aleatórias… sem conhecimento prévio de seu conteúdo").
- `Reward`: Pontos, Pack, Cupom de parceiro (Marca, com `shareWithBrands`), `Momento`, moldura cosmética. **Nunca** dinheiro, cripto ou prêmio cuja soma dependa de arrecadação.
- Troca de repetidas 1:1 entre usuários (Panini); sem mercado secundário monetário.

### 9.5 Regulação brasileira (textos lidos no Planalto)
- **Lei 15.211/2025 (ECA Digital), art. 20**: "São vedadas as caixas de recompensa (loot boxes) oferecidas em jogos eletrônicos direcionados a crianças e a adolescentes ou de acesso provável por eles, nos termos da respectiva classificação indicativa." Definição legal de caixa de recompensa exige **pagamento** + aleatoriedade sem conhecimento prévio → nosso Pack gratuito e com pool exibido fica fora; **qualquer pacote pago aleatório é proibido** enquanto houver sub-18 na plataforma (há: sub-15/17/20). MJSP pediu à ANPD (out./2026) fiscalização de jogos com loot boxes sem verificação de idade; caminhos aceitos: verificar idade e bloquear, versão sem loot box, ou restringir por padrão no Brasil (Fast Company Brasil).
- **Lei 14.790/2023**: define *fantasy sport* ("esporte eletrônico em que ocorrem disputas em ambiente virtual, a partir do desempenho de pessoas reais") com requisitos: equipes virtuais de **≥2 pessoas reais** e desempenho dependente de conhecimento/estatística/estratégia; regras preestabelecidas; **valor garantido da premiação independe do número de participantes ou da arrecadação**; resultados não decorrem de um único evento. Dispensa autorização do poder público. Apostas de quota fixa proíbem menores de 18 e exigem autorização — **FLAIR não pode cobrar inscrição nem pagar prêmio em dinheiro**, mantendo-se como jogo gratuito/fantasy sem premiação monetária.
- **Decreto 13.138/2026** regulamenta o Marco Legal dos Games (Lei 14.852/2024) com diretrizes de proteção a crianças/adolescentes (privacidade, acessibilidade) — fonte secundária (Fast Company Brasil, BNL Data).
- **LGPD**: cartas usam **apenas stats consentidas** (`Score_Total`, badges não médicos); `PhysicalStatus`/`InjuryRecord` nunca entram (art. 11); menores exigem consentimento parental (art. 14) e **sem FLAIR competitivo público** para sub-13; perfis `SOMENTE_EU` não geram carta colecionável por terceiros.
- Anti-vício (coerente com RF5 anti-dark-patterns): sem timers agressivos, sem "quase ganhou", limite diário de partidas, lembretes de tempo de uso, desligável em 1 toque.

### 9.6 Entidades novas
`Card (id, subjectType ATLETA|CLUBE, subjectId, snapshotWeek, tier, power, visualVariant GOLD?)` · `CardOwnership (userId, cardId, acquiredVia PACK|TRADE|REWARD)` · `Deck (userId, cardIds[5], captainId, season)` · `Pack (type, poolRules, dropTable visível, source)` · `Match (mode DUEL|LEAGUE8|CHALLENGE, participants, roundResults, snapMultiplier, settledAtWeek)` · `Reward (type POINTS|PACK|COUPON|MOMENT|COSMETIC, value, partnerId?)` · `Season (startWeek, endWeek, ruleset, resetPolicy)` · `Moment (postId PARTIDA_MOMENTO, edition, serial)` · `PointsLedger (userId, delta, reason)`.

**Esforço G (MVP duelo 1×1 + pacotes grátis = M) · Prioridade Média** (retenção forte; só após Rankings e Evolução existirem, pois usa seus snapshots).

---

## 10. Frente C — Arquitetura de informação inspirada no menu "FashionAI"

Tradução moda → futebol (Feed→Feed; Buscar→Buscar; Explorador→Explorar/globo; Marcas→Clubes & Ligas; Lens→Lens de vídeo; Minhas peças→Telemetria; Adicionar peça→Registrar treino; Meu Quarto→Vestiário/Elenco; Atelier→Prancheta; Provador→Provador tático; Avatar 3D→Avatar 3D; Meus looks→Meus cards; Criar look→Criar card; DNA de Estilo→DNA de jogo; Momentos→Momentos; FLAIR→FLAIR; Destaques→Destaques/Rankings; Cupons→Cupons; FAI Points→Pontos; Administração→Gestão clube/liga/admin).

### 10.1 Sidebar desktop completa
Papéis: **A** Atleta · **T** Técnico/Staff · **C** Clube · **S** Scout · **M** Marca · **I** Imprensa · **L** Oficial de Liga · **Adm** administrador. Ícones: nomes do Material Symbols / Lucide.

| Seção | Item | Ícone sugerido | Rota | Vê |
|---|---|---|---|---|
| **DESCOBRIR** | Feed | `home` | `/feed` | todos |
| | Buscar | `search` | `/buscar` | todos |
| | Explorar (globo) | `public` | `/explorar` | todos |
| | Clubes & Ligas | `shield` | `/clubes`, `/ligas` | todos |
| | Lens (scouting por câmera/vídeo) | `center_focus_strong` | `/lens` | A, T, S, C (gate `shareBiometricData` para rostos) |
| **MEU GÊMEO** | Telemetria | `monitoring` | `/gemeo` | A (dono); T/C/S com `shareTelemetryWith*` |
| | Evolução | `timeline` | `/gemeo/evolucao` | A; T/C/S conforme visibilidade do perfil |
| | Carga & Recuperação | `favorite` | `/gemeo/carga` | A (dono); T/C só com `shareMedicalData` |
| | DNA de jogo | `fingerprint` | `/gemeo/dna` | A; T/S/C (perfil público/conexão) |
| | Comparador | `compare_arrows` | `/gemeo/comparar` | T, S, C, A |
| | Provador tático | `sports_soccer` | `/gemeo/provador` | A, T |
| | Meu Avatar 3D | `view_in_ar` | `/gemeo/avatar` | A (dono); exibição pública se perfil público |
| **CARDS** | Meus cards | `style` | `/cards` | todos (posts próprios) |
| | Criar card | `add_circle` | `/cards/novo` (= FAB) | todos |
| | Copilot | `auto_awesome` | dentro do compositor | todos com `aiAgentProcessing` |
| | Autopiloto | `autorenew` | `/autopilot` | A, T, C, L (fontes: wearable, súmula) — sub-18 desabilitado |
| **PERFIL** | Meu perfil | `person` | `/me` | todos |
| | Histórico | `history` | `/me/historico` | todos (RF5 "Histórico de atividades") |
| | Privacidade (LGPD) | `verified_user` | `/configuracoes/privacidade` | todos (RF5) |
| **JOGAR E GANHAR** | Momentos | `movie` | `/momentos` | todos |
| | FLAIR | `playing_cards` / `casino`* | `/flair` | A, T, S, I, M (≥13; sub-18 sem liga pública); C/L espectadores |
| | Destaques / Rankings | `leaderboard` | `/ranking` | todos |
| | Cupons | `confirmation_number` | `/cupons` | quem tem `shareWithBrands` |
| **LOJA** | Pontos | `toll` | `/pontos` | todos (extrato + resgate; sem compra em dinheiro) |
| **GESTÃO** | Meu clube (Mural, Plantel, Vagas, Enquetes) | `apartment` | `/gestao/clube` | C, T (staff principal) |
| | Minha liga (homologação, tabela, súmulas) | `emoji_events` | `/gestao/liga` | L |
| | Administração (moderação, termos, auditoria LGPD) | `admin_panel_settings` | `/admin` | Adm |

\*evitar ícone de cassino — usar ícone de cartas neutro, coerente com a posição anti-aposta.

### 10.2 Redução para mobile (4 abas + FAB + "Mais")
| Bottom Bar | Sub-abas (segment control) | Vem de |
|---|---|---|
| **PERFIL** | Feed · Ranking · Explorar · Momentos | DESCOBRIR + Destaques + Momentos |
| **GEMEO** | Telemetria · Evolução · Carga & Recup. · DNA · Comparador · Provador · Avatar · Rascunhos Autopilot | MEU GÊMEO + Autopiloto |
| **FAB (＋)** | Criar card + Copilot | CARDS |
| **SCOUTING** | Pranchetas · Comparar · Relatórios · Lens | SCOUTING (RF3) + Lens |
| **ALERTA** | Tudo · Mural do Clube · Saúde · DM | ALERTA (RF3) + GESTÃO→Mural |
| **☰ Mais** (Topbar) | FLAIR · Cupons · Pontos · Meu perfil · Histórico · Privacidade · Gestão (por papel) · Buscar/Clubes & Ligas | JOGAR E GANHAR, LOJA, PERFIL, GESTÃO |

Justificativa: Material (3–5 destinos; excedente em "alternative locations" como drawer) e HIG (≤5 tabs no iPhone) — fontes na §14. "Mais" nunca abriga ações de criação (regra RF3), só navegação.

---

## 11. Comparativo de bibliotecas de globo para SPA

| Lib | Tipo | Tamanho (min+gz) | Licença | Performance mobile | Facilidade | Observações |
|---|---|---|---|---|---|---|
| **MapLibre GL JS ≥5** (`setProjection({type:'globe'})`) | Vector tiles WebGL; globo longe, Mercator perto ("Adaptive Composite") | ~287 kB (v6.15.0, Bundlephobia) | BSD-3-Clause | Boa (engine móvel maduro); precisa de tile server (demotiles/OpenFreeMap/MapTiler) | Alta — 7 exemplos oficiais de globo | Globo **não** existe no MapLibre Native. **Recomendada** |
| **globe.gl / three-globe** | Three.js, data viz (pontos, arcos, labels, HTML, hex bins) | ~546 kB globe.gl (inclui three); three-globe 148 kB | MIT | Média: `pointsMerge`, `pauseAnimation`, desligar `enablePointerInteraction`; sem tiles | Muito alta (`react-globe.gl`) | Visual "wow"; sem zoom a rua |
| **deck.gl GlobeView** | Camadas WebGL sobre `@deck.gl/core` | ~212 kB core (v9.4.0) + camadas | MIT | Incerta: GlobeView **experimental** (zoom >12 impreciso, Heatmap/Contour não funcionam, TileLayer experimental) | Média | Para milhões de pontos; overkill |
| **CesiumJS** | Globo geoespacial completo | ~1,3 MB (v1.146.0) | Apache-2.0 (lib); globo/geocoder padrão usam **Cesium ion** (comercial pago, ~US$149–874/mês, fonte secundária) | Pesada | Baixa | Excesso de funcionalidade e custo |
| **react-simple-maps** (`projection="geoOrthographic"`) | SVG/d3-geo | ~32 kB (v5.0.5) | MIT | Excelente (sem WebGL) | Muito alta | Sem inércia 3D/tiles; 161 issues abertas. **Fallback 2D recomendado** |

---

## 12. Fontes de dados abertas de clubes/ligas

| Fonte | Conteúdo útil | Licença / custo | Coordenadas? | Observações |
|---|---|---|---|---|
| **Wikidata** (SPARQL) | Clubes (Q476028), liga P118, estádio P115, coordenadas P625, país P17 | **CC0** | Sim (via estádio) | Verificado 2026-10-24: **8.827 clubes** com liga + coordenadas; **1.718 brasileiros** |
| **openfootball/clubs** | Clubes do mundo em `.txt`: nome, fundação, estádio, cidade, **aliases** | **CC0-1.0** | Não (cidade) | Ótimo para busca/aliases |
| **openfootball/leagues, /europe** | Ligas/copas, calendários, resultados | Domínio público | Não | `League.level`, temporadas |
| **football-data.org** | 12 competições grátis (incl. Brasileirão A), 10 req/min; €12–199/mês | Free "forever"; uso comercial: consultar daniel@football-data.org | Não | Pricing oficial aberto |
| **API-Football** | Times, ligas, **venues**, jogadores; free 100 req/dia; Pro US$19/mês | Comercial (página 403/502; via guia oficial e secundárias) | Sim | Cache obrigatório |
| **TheSportsDB** | Times, ligas, venues, escudos; key `123`, 30 req/min; Premium US$9/mês | Termos não lidos (403) | Endpoint venue existe | Crowd-sourced; verificar licença de logos |
| **ClubElo** | Elo histórico de clubes | Sem termos; **API Fixtures desativada** (verificado) | Não | Só referência metodológica |

Estratégia: Wikidata + openfootball (CC0) como base canônica; enriquecimento sob demanda por API-Football/football-data.org; escudos via upload dos próprios clubes.

---

## 13. Roadmap sugerido

| Fase | Entrega | Esforço | Depende de |
|---|---|---|---|
| **F1** (sem. 1–4) | **Rankings** (PERFIL → Ranking; `RankingSnapshot`) + **Gêmeo → Evolução** | M + P | Rarity Engine |
| **F2** (sem. 3–8) | **Mural do Clube** (ALERTA → Mural; `ClubBoardPost`, `ClubPoll`, push segmentado) + sidebar 7 seções no desktop | M | Card de Clube |
| **F3** (sem. 6–10) | **Copilot** no FAB (`/v1/posts/suggest`, gate `aiAgentProcessing`) + **Gêmeo → Comparador** | M + P | Compositor |
| **F4** (sem. 8–13) | **Gêmeo → Carga & Recuperação** (ACWR, prontidão 1–100) e **DNA de jogo** (radar 5 eixos) | M | `TrainingLoadDaily`, consentimento médico |
| **F5** (sem. 10–15) | **Explorar MVP 2D** (busca + react-simple-maps; import Wikidata/openfootball BR) | M | `Club.lat/lng` |
| **F6** (sem. 13–20) | **FLAIR MVP** (coleção + pacotes grátis + duelo 1×1 + liga semanal 8) + Momentos + Pontos | G | Rankings, Evolução, parecer jurídico ECA Digital |
| **F7** (sem. 16–24) | **Autopilot** (rascunhos privados; toggles LGPD; art. 20; bloqueio sub-18) | G | Copilot, `ConnectedSource`, RF5 |
| **F8** (sem. 20–28) | **Provador tático** + **Meu Avatar 3D** (após validar licença RPM ou avatar próprio) | M + G | DNA de jogo |
| **F9** (sem. 24–30) | **Explorar 3D** (MapLibre globe) + Elo interno de clubes | G | F5 |

Métricas: % de sugestões do Copilot aceitas sem edição; taxa de publicação de rascunhos do Autopilot; DAU do papel CLUBE após Mural; cliques de scouts a partir de Rankings/Comparador; retenção D7/D30 de jogadores FLAIR; buscas em Explorar que terminam em "Seguir".

---

## 14. Referências (URLs abertas nesta pesquisa; ★ = só via busca/fonte secundária, página direta não abriu)

**Navegação / guidelines**
- Material Components iOS — Bottom Navigation README (3–5 destinos; excedente em drawer): https://github.com/material-components/material-components-ios/blob/c8b8acabf8c78b9e266c33803848e9e67967fe73/components/BottomNavigation/docs/README.md
- ★ Apple HIG — Tab bars: https://developer.apple.com/design/human-interface-guidelines/tab-bars
- UX Planet — Bottom tab bar best practices: https://uxplanet.org/bottom-tab-bar-design-best-practices-ef3ee71de0fc

**Mural do Clube**
- OneFootball — App Store (Club Network): https://apps.apple.com/br/app/onefootball-football-scores/id382002079?l=en-GB
- Chivas × OneFootball: https://www.chivasdecorazon.com.mx/en/noticias/34891/Chivas-une-fuerzas-con-OneFootball
- Socios.com: https://www.socios.com/

**Rankings**
- FIFA — Men's ranking procedure: https://inside.fifa.com/fifa-world-ranking/procedure-men
- FIFA — PDF do método SUM (fórmula e coeficientes I): https://digitalhub.fifa.com/m/f99da4f73212220/original/edbm045h0udbwkqew35a-pdf.pdf
- Follow the Money — Transfermarkt volunteers: https://www.ftm.eu/articles/transfermarkt-volunteers-european-football
- ★ Sofascore — How the rating works: https://www.sofascore.com/news/sofascore-rating
- ClubElo (API Fixtures desativada, via `curl`): https://clubelo.com/API · http://api.clubelo.com/Fixtures

**Copilot / Autopilot**
- Metricool — LinkedIn Post Proofreader (02/10/2026): https://metricool.com/linkedin-post-proofreader/
- Notion AI: https://www.notion.com/product/ai
- TechCrunch — Strava Athlete Intelligence beta: https://techcrunch.com/2024/05/16/strava-taps-ai-to-weed-out-leaderboard-cheats-unveils-family-plan-dark-mode-and-more
- ★ TechRadar — Athlete Intelligence beta pública: https://www.techradar.com/health-fitness/strava-s-powerful-ai-insights-are-here-athlete-intelligence-is-now-available-in-beta
- Android Central — Athlete Intelligence sai do beta: https://www.androidcentral.com/wearables/strava-announces-stable-athlete-intelligence-anti-cheating-and-flyover-sharing
- Strava Support — Activity Privacy Controls: https://support.strava.com/hc/en-us/articles/216919377-Activity-Privacy-Controls
- Kaspersky — privacidade Garmin: https://www.kaspersky.com/blog/garmin-privacy-settings/53920/
- Hudl — auto-generated highlights: https://www.hudl.com/blog/you-dominate-the-gridiron-well-handle-the-highlights
- Hudl Assist — player highlights: https://www.hudl.com/support/diamond-sports-playbook/guides/how-to-use-hudl-assist-to-create-player-highlights
- WSC Sports: https://wsc-sports.com/
- ★ WHOOP Coach (BusinessWire/Wareable 403): https://www.businesswire.com/news/home/20230926899032/en

**Gêmeo Digital (Frente A)**
- Garmin — Training Readiness (manual oficial): https://www8.garmin.com/manuals/webhelp/GUID-31D23DBB-57C2-4DF7-A0C9-8D1A00AB4BE7/EN-US/GUID-C21BE0C8-A08E-4DA1-B6C6-2E0E2DDDB372.html
- Sleep Foundation — WHOOP (Strain/Recovery): https://www.sleepfoundation.org/sleep-trackers/whoop
- ★ WHOOP — The Data: https://www.whoop.com/de/en/the-data/
- Catapult — home e Athlete Monitoring: https://www.catapult.com/ · https://www.catapult.com/solutions/athlete-monitoring
- SimpliFaster — experiência com Catapult (ACWR no dashboard): https://simplifaster.com/articles/catapult-gps/
- Zepp — Technology (PAI, Readiness, PeakBeats): https://ir.zepp.com/technology
- Hudl/Wyscout — Benchmarking & compare mode: http://www.hudl.com/blog/benchmarking-monitor-player-portfolio-with-wyscout
- ★ SciSports — Player comparison tool: https://support.scisports.com/en/articles/4732889-player-comparison-tool
- ★ Sofascore — Attribute Overview: https://www.sofascore.com/news/?p=71132 · https://sofascore.com/news/?p=71965
- ★ EA — PlayStyles guide / deep dive: https://help.ea.com/en/articles/ea-sports-fc/playstyles-guide/ · https://ea.com/news/fc-24-playstyles-deepdive
- ★ Ready Player Me — Licensing & Privacy (DNS não resolveu): https://docs.readyplayer.me/ready-player-me/support/terms-of-use

**FLAIR (Frente B)**
- Premier League — What is Sorare: https://www.premierleague.com/en/news/3041558
- Dexerto — EA FC 26 Rivals format: https://www.dexerto.com/wikis/ea-fc-26-guides-walkthrough-tips/ea-fc-26-rivals-format
- ★ EA Help — Rivals: https://help.ea.com/help/ea-sports-fc/fc-ultimate-team-rivals
- ★ Topps — How to play Match Attax: https://ripped.topps.com/uk/how-to-play-the-match-attax-trading-card-game
- FIFA — Panini Digital Sticker Album 2018: https://inside.fifa.com/news/panini-digital-sticker-album-launched-for-russia-2018
- Pokémon TCG Pocket (site oficial): https://tcgpocket.pokemon.com/en-us/
- ★ ptcgpocket.gg — Wonder Pick: https://ptcgpocket.gg/wonder-pick/
- Wikipedia — Marvel Snap: https://en.wikipedia.org/wiki/Marvel_Snap
- Blizzard — Introducing Hearthstone Battlegrounds: https://hearthstone.blizzard.com/en-us/news/23156373
- NBA Top Shot 101: https://about.nbatopshot.com/nba-top-shot-101

**Regulação brasileira**
- Lei 13.709/2018 (LGPD) — Planalto (lida via `curl`: arts. 8º §5º, 11, 14, 20): https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm
- Lei 15.211/2025 (ECA Digital) — Planalto (art. 20 e definição de caixa de recompensa, via `curl`): https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/lei/L15211.htm
- Lei 14.790/2023 (apostas / fantasy sport) — Planalto (via `curl`): https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2023/lei/L14790.htm
- Fast Company Brasil — decreto do Marco Legal dos Games e loot boxes (out./2026): https://fastcompanybrasil.com/games/brasil-cria-regras-games-preve-apoio-estudios-nacionais/
- BNL Data — Decreto 13.138/2026, ECA Digital e loot boxes (coluna de opinião): https://bnldata.com.br/lula-aposta-nos-games-e-esquece-que-eles-tambem-viciam/
- Migalhas — aspectos da Lei das Bets: https://www.migalhas.com.br/depeso/400124/aspectos-relevantes-da-recemsancionada-lei-das-bets
- LEC — consentimento e dados de saúde (art. 11): https://lec.com.br/lgpd-e-o-mito-do-consentimento-para-tratamento-dos-dados-de-saude
- Convergência Digital — ANPD e decisões automatizadas por IA: https://convergenciadigital.com.br/governo/anpd-avanca-na-regulacao-de-decisoes-automatizadas-por-ia/

**Globo (libs)**
- MapLibre — roadmap Globe View: https://maplibre.org/roadmap/maplibre-gl-js/globe-view
- MapLibre — exemplo globo vetorial: https://maplibre.org/maplibre-gl-js/docs/examples/display-a-globe-with-a-vector-map/
- MapLibre — índice de exemplos: https://maplibre.org/maplibre-gl-js/docs/examples/
- MapLibre — LICENSE (BSD-3): https://github.com/maplibre/maplibre-gl-js/blob/main/LICENSE.txt
- Bundlephobia: https://bundlephobia.com/package/maplibre-gl · https://bundlephobia.com/package/globe.gl · https://bundlephobia.com/package/@deck.gl/core · https://bundlephobia.com/package/cesium · https://bundlephobia.com/package/react-simple-maps
- globe.gl (MIT): https://github.com/vasturiano/globe.gl · ★ three-globe: https://depscope.dev/pkg/npm/three-globe
- deck.gl GlobeView: https://deck.gl/docs/api-reference/core/globe-view · LICENSE: https://github.com/visgl/deck.gl/blob/master/LICENSE
- CesiumJS: https://cesium.com/platform/cesiumjs/
- react-simple-maps — projections / getting started / GitHub: https://www.react-simple-maps.io/docs/projections/ · https://www.react-simple-maps.io/docs/getting-started/ · https://github.com/zcreativelabs/react-simple-maps

**Dados de clubes/ligas**
- Wikidata — Licensing (CC0): https://www.wikidata.org/wiki/Wikidata:Licensing · P118: https://www.wikidata.org/wiki/Property:P118 · Request a query ("Clubs in league"): https://www.wikidata.org/wiki/Wikidata:Request_a_query/Archive/2019/06 · Query Service: https://query.wikidata.org/sparql
- openfootball/clubs (CC0): https://github.com/openfootball/clubs · leagues: https://github.com/openfootball/leagues
- football-data.org — Pricing: https://www.football-data.org/pricing · Sportmonks (secundária): https://www.sportmonks.com/football-api/alternatives/football-data-org/
- ★ API-Football — guia oficial: https://www.api-football.com/news/post/how-to-get-started-with-api-football-the-complete-beginners-guide
- TheSportsDB — Free API / Documentation: https://www.thesportsdb.com/free_sports_api · https://www.thesportsdb.com/documentation

Consulta SPARQL usada (reprodutível):
```sparql
SELECT (COUNT(DISTINCT ?club) AS ?n) WHERE {
  ?club wdt:P31 wd:Q476028 ; wdt:P118 ?league .
  OPTIONAL { ?club wdt:P115 ?venue . ?venue wdt:P625 ?c1 }
  OPTIONAL { ?club wdt:P625 ?c2 }
  FILTER(BOUND(?c1) || BOUND(?c2))
}
```
