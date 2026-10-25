# Anatomia dos Cards — Post, Atleta, Staff e Clube

> Reescrito a partir dos modelos de referência da Velion/Orange Social/Football AI · Social
> (`velion_sistema_identificacao_visual`, `orange_social_spec`, `football-ai-social`) e do estudo em
> [`research/03_anatomia_cards_redes_sociais.md`](research/03_anatomia_cards_redes_sociais.md).
> Implementação: `src/main/resources/static/{styles.css,app.js}` · prints em [`preview/spa/`](preview/spa/).

## 1. Princípios herdados

| # | Princípio | Origem | Consequência no card |
|---|---|---|---|
| 1 | **Forma não decora — forma é gramática. Cor não embeleza — cor é categoria.** | Velion | Cada sinal visual responde a uma pergunta; nenhum enfeite sem significado. |
| 2 | **Quatro camadas ortogonais** | Velion + FAS | Bolinha (*quem*) · Borda (*o quê*) · Feed (*onde*) · Raridade (*quanto o Gêmeo evoluiu*). |
| 3 | **Classificação de público** | Velion | `age-chip`: `SAFE` · `KIDS` · `12+` · `16+` · `18+` · `SENSÍVEL` (dado médico, Art. 11). |
| 4 | **O feed nunca exibe texto corrido** | Velion regra 01 | Título obrigatório (5–12 palavras, ≤ 2 linhas) · subtítulo teaser opcional (≤ 2 linhas, 70 % opacidade, só em post de texto) · texto completo só no PostView. |
| 5 | **Ações sociais em toda postagem** | pedido do produto | Barra com **Curtir · Comentar · Compartilhar · Salvar**, com contadores, em posts **e** cards de perfil. Trending (🔥 📈 ➖ 📉) à direita, discreto, nunca compete com o título. |
| 6 | **Common Border dupla** | Velion §08 | `outer` (gradiente da cor de contexto, 2 px) + `inner` (painel, 1 px), cantos sempre arredondados. |
| 7 | **VSL — alfabeto geométrico** | Velion §04 | ◇ identidade (treino, partida, atleta) · ⬢ sistema/comunidade (clube, liga) · ▭ instituição (scouting report, institucional) · ✦ valor/especial (prêmio, momento). O glifo aparece no topo do card. |
| 8 | **Elite Gold reservado** | FAS | Dourado só em conta oficial e em Legendary/Mythic — nunca como decoração. |

## 2. Tokens

| Token | Valor | Uso |
|---|---|---|
| `--ink / --ink-2 / --panel / --panel-2` | `#0a0f0c · #0f1613 · #131b17 · #19231d` | base pitch green-black |
| `--line / --line-soft` | `#26332b · #1e2921` | contornos |
| `--text / --text-2 / --text-3` | `#eaf2ee · #95a89d · #63756a` | hierarquia de texto |
| `--green / --green-hi / --green-deep` | `#00d47a · #6cffc0 · #00874e` | assinatura Pitch Green |
| `--gold / --gold-hi / --gold-deep` | `#f5c542 · #ffe08a · #b8902c` | Elite Gold |
| `--c-green · --c-blue · --c-yellow · --c-purple · --c-orange · --c-red · --c-gold · --c-slate` | `#00d47a · #3b82f6 · #f7c948 · #a06bff · #ff9f43 · #ff5d5d · #f5c542 · #5c6b62` | paleta de categoria |
| `--radius / --radius-sm` | `18px / 11px` | cards / posts |
| Fontes | Space Grotesk (display) · Inter (corpo) · JetBrains Mono (rótulos, chips, números) | |

## 3. Camada 1 · Identidade — a bolinha (`.dot`)

13 px, halo de 3 px a 22 % + glow 12 px. Papéis canônicos do modelo e extensões da pesquisa 01:

| Papel | Cor | | Extensão | Cor |
|---|---|---|---|---|
| Atleta | `--c-green` | | Staff técnico | `#7fb6ff` |
| Clube / Instituição | `--c-blue` | | Staff saúde | `#e11d48` |
| Scout | `--c-yellow` | | Agente | `#2dd4bf` |
| Marca | `--c-purple` | | Torcedor | `#f472b6` |
| Imprensa | `--c-orange` | | Agente IA | `#22d3ee` (sempre com rótulo **IA**) |
| Conta oficial | `--gold` | | | |

## 4. Camada 2 · Conteúdo — a borda (`.cc-*`)

| Tipo | Cor | VSL | Feed padrão |
|---|---|---|---|
| `TREINO` | verde | ◇ | GEMEO |
| `PARTIDA_MOMENTO` | azul | ◇ | GEMEO / PERFIL |
| `RELATORIO_TATICO` (Scouting Report) | laranja | ▭ | SCOUTING |
| `LIGA` (Liga / Comunidade) | roxo | ⬢ | ALERTA · Mural |
| `INSTITUCIONAL` (inclui `LESAO_ALERTA`, com alarme 🩹) | vermelho | ▭ | ALERTA |
| `ESPECIAL` (prêmio, momento) | dourado | ✦ | GEMEO / Momentos |

## 5. PostCard (`.pcard`)

```
┌─ borda esquerda 3 px = tipo ──────────────────────────────────────┐
│ 2h            ◇  [TREINO]  [SAFE]                 ← p-top (mono .6rem) │
│ ● @handle · papel  [IA]                           ← p-author (dot 11 px)│
│ Título em Space Grotesk 600 · ≤ 2 linhas          ← p-title            │
│ Subtítulo teaser · 70 % · ≤ 2 linhas  |ou| bloco de mídia 136 px        │
│ [FÍSICA] [TREINO] [W43]                           ← p-meta (opcional)  │
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│ (♡ Curtir 138) (💬 Comentar 12) (↗ Compartilhar 5) (🔖)   📈 PP em alta │
└───────────────────────────────────────────────────────────────────┘
```

| Zona | Medidas | Regras |
|---|---|---|
| Container | `--ink-2`, raio 11, padding 15, `border-left: 3px var(--cc)` | Variante **alerta**: gradiente da cor de contexto no topo + `.alarm` (🩹 LESÃO · 📣 CONVOCAÇÃO · ⚖️ PUNIÇÃO). |
| `p-top` | mono .6 rem | tempo à esquerda · VSL · badge do tipo (cor da borda) · `age-chip`. |
| `p-author` | dot 11 px + handle .8 rem 600 | papel em `--text-3`; posts de IA recebem rótulo **IA** (AI Act art. 50). |
| `p-title` | Space Grotesk .98 rem 600, `line-clamp: 2` | obrigatório. |
| `p-sub` / `p-media` | .8 rem a 70 % · mídia 136 px raio 9 | post com imagem não mostra subtítulo. |
| `actbar` | botões 34 px de altura, pill, mono .62 rem | ordem fixa: Curtir → Comentar → Compartilhar → Salvar; curtir alterna estado (vermelho); trending à direita. |

## 6. Card de perfil / atleta (`.vcard`)

```
┌ outer: gradiente da cor de contexto (raridade) 2 px ───────────────────┐
│┌ inner ─────────────────────────────────────────────────────────────┐ │
││ ◇ GÊMEO DIGITAL                            [SAFE] (brasão tier)   │ │ ← ctop
││ ┌ imgblock 108 px ─────────────────────────────────────────────┐   │ │
││ │                        (foto / avatar)                       │   │ │
││ │              hero limpo — sem stats nem badges sobrepostos    │   │ │
││ └──────────────────────────────────────────────────────────────┘   │ │
││ ● @handle · atleta ✦                                               │ │ ← author
││ Nome                                                               │ │ ← vtitle (só o nome)
││ ( TEMPORADA )( ATRIBUTOS )                                         │ │ ← vtabs (janela segmentada)
││ ┌JOGOS 22┐┌GOLS 14┐┌ASSIST. 7┐┌MINUTOS 1.980┐┌CARTÕES 3 0┐┌NOTA 8.4┐│ │ ← sgrid (aba Temporada)
││ ┌ CONDICIONAMENTO ▬▬▬▬▬▬▬▬▬░░ prontidão 78% · ACWR 1,21  ⚡ PRONTO ┐│ │ ← phys (aba Temporada)
││ (◎ Meia)(22 anos)(⬢ Clube)(Contratado)(PR)(Pé D)(176 cm)           │ │ ← attrs (aba Atributos)
││ (✦ Referência estadual)(✦ Top 10% do ranking por posição)          │ │   lista de badges, nunca texto corrido
││ BADGES (⚡ PRONTO) (⚽ 14) (🏆 3)                 ← Athletic Persona ≤ 3  │ │ ← bdgrow
││ ─ ─ PP 1.240 · RP 780 · MP 76 · SCORE 1.650 ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│ │ ← statrow
││ (♡ Curtir 241) (💬 Comentar 18) (↗ Compartilhar 9)     (Gêmeo →)   │ │ ← actbar
│└────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

| Zona | Conteúdo | Regras |
|---|---|---|
| `ctop` | VSL + contexto (`GÊMEO DIGITAL`, `CLUBE FORMADOR`, `SCOUT`) · `age-chip` · **brasão de raridade** (`ic-tier-*`) | `age-chip` vira `SENSÍVEL` quando o card expõe dado médico. O tier **nunca é escrito** (sem “LEGENDARY/MYTHIC” em texto): é um ícone único do Football Social — brasão do logo tingido na cor do tier com gema (Common ◇, Rare, Epic ✦) ou coroa (Legendary, Mythic com raios e aro branco). O nome do tier fica só em `aria-label`/tooltip. |
| `imgblock` | 108 px, gradiente da cor de contexto; **hero limpo** (foto/avatar apenas) | nada sobreposto à imagem: nem stats, nem títulos, nem badges — evita repetir gols/títulos que já estão no `sgrid`. |
| **`bdgrow` (badges)** | rótulo `BADGES` + até 3 pills *Athletic Persona* (status físico → `priority` → contador), na área inferior de dados, logo abaixo de `phys` | toque abre o histórico (*tap to expand*); nunca sobre a imagem. |
| `author` | bolinha 13 px + `@handle · papel`; ✦ dourado = verificado | |
| `vtitle` | **só o nome** do atleta (a posição é um badge da lista de atributos) | |
| **`vtabs` (janela segmentada)** | `Temporada` · `Atributos` — segment control dentro do card | evita excesso de informação: uma aba por vez; default `Temporada`. |
| **`attrs` (aba Atributos)** | lista de itens/badges: `◎ posição` · `idade` · `⬢ clube` · `Contratado`/`Agente livre` · UF · pé dominante · altura · `✦ referência` · `✦ ranking` | **atributos nunca em texto corrido**: cada atributo é um pill (`.attrs li`), com cor por semântica (verde contratado, azul agente livre, dourado referência). |
| **`sgrid` (aba Temporada)** | Jogos · Gols (goleiro: Clean sheets) · Assistências (defensor: Desarmes) · Minutos · Cartões (🟨 🟥) · Nota média | tiles mono .5 rem + Space Grotesk 1.05 rem. |
| **`phys` (aba Temporada)** | barra de prontidão 1–100 · ACWR 7:28 · status `⚡ PRONTO PRA JOGO` / `⚠️ RISCO DE FADIGA` / `🩹 DEPARTAMENTO MÉDICO` + tipo da lesão e retorno previsto | **privado por padrão**: só aparece para terceiros com `shareMedicalData = true`; a variante lesão pinta o bloco de vermelho. |
| `statrow` | `PP` (verde) · `RP` (azul) · `MP` (amarelo) · `SCORE` (dourado) | fórmula do Rarity Engine; tiers de recompensa (Bronze→Diamante) correm em paralelo. |
| `actbar` | Curtir · Comentar · Compartilhar · **Gêmeo →** | Staff: `Pranchetas →` · Clube: `Plantel →`. |

### 6.1 Raridade (moldura — só no card de perfil, nunca no post)

| Tier | Faixa | Tratamento |
|---|---|---|
| Common | 0–400 | flat, slate, 1–2 px |
| Rare | 401–900 | borda azul + glow 18 px |
| Epic | 901–1600 | borda roxa + glow + partículas (`driftParticles`) |
| Legendary | 1601–2400 | gradiente branco → dourado + glow 26 px |
| Mythic | 2401+ | branco · dourado · branco + `mythicPulse` + aura cônica (`mythicAura`) |

`prefers-reduced-motion` desliga todas as animações.

### 6.2 Variantes por papel

| Papel | `ctop` | `sgrid` | `phys` | Ação principal |
|---|---|---|---|---|
| **Atleta** | ◇ GÊMEO DIGITAL | temporada (acima) | sim | Gêmeo → |
| **Staff técnico** | ◇ STAFF · licença | Aproveitamento · Aderência tática · Atletas · Pranchetas · Jogos · Cartões/jogo | não (exibe elenco pronto %) | Pranchetas → |
| **Staff saúde** | ◇ DEP. MÉDICO | Atletas em tratamento · Altas · Dias médios | — | Boletins → |
| **Clube** | ⬢ CLUBE FORMADOR ✦ | Plantel · Prontos · Vagas · Média de tier · Comunicados · Sócios | agregado (nunca nominal) | Plantel → |
| **Scout** | ▭ SCOUT · verificação | Relatórios · Clubes · Shortlists | — | Relatórios → |
| **Conta oficial** | ✦ OFICIAL | Competições · Convocações · Súmulas | — | Comunicados → |

## 7. Medidas e responsividade

| | Mobile (≤ 860 px) | Desktop |
|---|---|---|
| Largura do `.vcard` | 100 % − 28 px | grade `minmax(300px, 1fr)`, máx. 360 px |
| Largura do `.pcard` | 100 % | 2 colunas no `feed-panel` |
| Alvos de toque | ≥ 34 px (botões da `actbar`), ≥ 44 px (bottom nav) | |
| Contraste | texto ≥ 4.5:1 · bolinhas/bordas ≥ 3:1 sobre `#131b17` | |

## 8. Mapeamento para o DTO

```json
{
  "card": "ATHLETE",
  "identity": { "role": "ATLETA", "dot": "#00d47a", "handle": "@<preencher>", "verified": false },
  "classification": { "ageRating": "SAFE", "sensitive": false },
  "rarity": { "tier": "LEGENDARY", "effect": "frame-white-gold", "score": 1650, "pp": 1240, "rp": 780, "mp": 76 },
  "season": { "games": 22, "goals": 14, "assists": 7, "minutes": 1980, "yellow": 3, "red": 0, "rating": 8.4 },
  "physical": { "readiness": 78, "acwr": 1.21, "status": "READY", "injury": null },
  "topBadges": [ { "type": "PRONTO_PRA_JOGO" }, { "type": "ARTILHEIRO", "counter": 14 }, { "type": "MVP", "counter": 3 } ],
  "social": { "likes": 241, "comments": 18, "shares": 9, "saved": false, "trend": "📈" },
  "vsl": "◇"
}
```

`physical` e qualquer badge de saúde são omitidos quando o viewer não é o dono e `shareMedicalData = false`; nesse caso `classification.sensitive` continua `false` e o `age-chip` permanece `SAFE`.

---

## 9. Arte de fundo, fotografia e criador de cards (4 etapas)

O card é criado num assistente único (FAB / “Criar card”): **1 · Modo** (Manual · Com IA/Copilot) → **2 · Conteúdo** (tipo, título 5–12 palavras, teaser, texto, **fotografia** e classificação) → **3 · Arte de fundo** → **4 · Revisar e salvar** (rascunho ou publicar).

**Dois campos distintos, nunca confundidos:**

| Campo | O que é | Quem define | Onde aparece |
|---|---|---|---|
| **Fotografia** (`mediaKeys`) | imagem ou vídeo enviado pelo usuário (`POST /v1/media`, JPEG/PNG/WebP/GIF/MP4 ≤ 25 MB, bucket `fas-media`, URL pré-assinada 1 h) | upload do autor | bloco `p-media.photo` abaixo do título (posts com fotografia não exibem teaser) |
| **Arte de fundo** (`CardArt`) | cor, material, layout e estilo que **tingem o fundo do próprio card** (`.pcard.has-art`) — não alteram a fotografia | etapa 3 do criador (ou sugestão do Copilot por tier) | fundo, borda e brilho do card inteiro, atrás de título, teaser e barra de ações |

| Dimensão da arte | Valores | Render no fundo do `.pcard` |
|---|---|---|
| Cor | hex dos tokens (`--c-*`, `--gold`) ou personalizada | `--ac` misturada ao `--panel` (gradiente de 28 % → 0 %) e à borda |
| Material | Fosco · Metálico · Vidro · Carbono · Holográfico | gradiente plano · faixas de brilho · translúcido com blur · trama diagonal · conic-gradient animado em `::before` |
| Layout | Clássico · Hero · Split · Minimal | tinta uniforme · banda forte no terço superior · faixa lateral esquerda · só um fio de 4 px no topo |
| Estilo | Neon · Vitral · Grafite · Ouro · Retrô | glow externo/interno · facetas do logo · véu grafite · aro Elite Gold · scanlines sépia |

Regras: a arte **nunca substitui** a borda de conteúdo nem a bolinha do autor; o rótulo `material · layout · estilo` fica discreto no canto inferior do card; o Copilot sugere arte por tier (Common fosco/grafite … Mythic holográfico/ouro) e tipo (partida/especial = hero; relatório = split; institucional/lesão = minimal); cards gerados com IA exibem a pill “GERADO COM IA · REVISADO”.

**Estúdio de edição**: o autor reabre o próprio card (`✎ Editar`) no mesmo assistente, a partir da etapa Conteúdo, com tipo/borda travados; `PATCH /v1/posts/{id}` salva título, teaser, texto, fotografia, classificação e arte. Por isso os cards de um feed nunca são todos da mesma cor: cada um traz a arte definida pelo autor.
