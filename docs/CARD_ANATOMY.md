# Anatomia dos Cards — Atleta, Técnico/Staff e Clube

> Preview renderizado: [`docs/preview/card_anatomy.html`](preview/card_anatomy.html)
> Gramática visual em três camadas, sempre nesta ordem de leitura: **quem** (anel do autor) → **o quê** (borda do conteúdo) → **quão raro** (efeito do tier).

## 1. Princípios

| Princípio | Regra |
|---|---|
| **Uma cor, um significado** | Cor do **anel** = papel do usuário. Cor da **borda** = tipo de conteúdo. Cor/efeito da **moldura** = tier de raridade. Nunca se misturam. |
| **Cor nunca é o único sinal** (WCAG 1.4.1) | Todo anel vem com o rótulo do papel; toda borda com o rótulo do tipo; todo tier com a pill textual. |
| **≤ 3 micro-badges** | O topo do card mostra no máximo três badges, ordenados por: status médico ativo → `BadgeType.priority` → `counter`. O restante vai para o Bottom Sheet ("tap to expand"). |
| **Privacy by Default** | Dados médicos/biométricos (Art. 11) só aparecem para terceiros se `shareMedicalData = true`. Caso contrário o badge 🚑/⚠️ é removido e o próximo da fila sobe. |
| **Dados pessoais são placeholders** | Nome, clube, cidade, licença etc. ficam como `<PREENCHER_*>` no mockup e nos scripts de enriquecimento. |
| **Thumb-zone** | Ações (zona ⑤) ficam na base do card; alvo mínimo 44 × 44 pt. |

## 2. Zonas comuns a todos os cards

```
┌──────────────────────────────────────────────┐  ← moldura = RarityTier.visualEffect
│ ① (avatar+anel)  Nome               ② 1.650  │
│                  ● PAPEL · meta      SCORE   │
│                  [TIER PILL] status          │
│ ③ [badge 1] [badge 2] [badge 3]              │  ≤ 3, tap-to-expand
│ ④ [stat A ▬▬▬] [stat B ▬▬▬] [stat C ▬▬▬]      │  específicas do papel
│ ─────────────────────────────────────────────│
│ ⑤ [ Ação primária ] [ Secundária ] [ Seguir ]│
└──────────────────────────────────────────────┘
```

| Zona | Conteúdo | Fonte de dados | Regras |
|---|---|---|---|
| **① Header** | Avatar 56 px com **anel de 3 px** na cor do `UserRole`; nome (1 linha, ellipsis); linha de papel com **bolinha** da mesma cor + metadados; pill do tier + status contratual/institucional | `User`, `UserRole.ringColor`, `DigitalTwin.rarityTier` | O anel e a bolinha usam **exatamente** o hex do enum. Nome nunca quebra linha. |
| **② Tier / Score** | `Score_Total` da semana (0–3000+) e rótulo "SCORE SEMANAL" | `WeeklyScoreSnapshot.scoreTotal` | Alinhado à direita, no header (não sobrepõe o nome). Oculto para twin `DRAFT`. |
| **③ Micro-badges** | Até 3 cards 1/3 com emoji, valor e legenda | `AthleteBadge`, `PhysicalStatus` | Badge de status físico tem fundo verde (⚡) ou vermelho (🚑). Toque abre `GET /v1/athletes/{id}/badges/{type}/history`. |
| **④ Estatísticas** | 3 métricas com barra de progresso na cor do anel | varia por papel (abaixo) | Valores normalizados 0–100 ou absolutos com unidade. |
| **⑤ Ações** | 3 botões: primária (cor do anel), secundária, "Seguir" | — | Primária muda por papel. |

## 3. Card de **Atleta** (anel verde `#15805C`)

| Zona | Especificidade |
|---|---|
| ① | Metadados: **Posição · Idade · Clube atual**; status contratual: `Agente Livre` \| `Contratado` (filtro da janela PERFIL). |
| ③ | Prioridade padrão: `PRONTO_PRA_JOGO` ⚡ / `DEPARTAMENTO_MEDICO` 🚑 / `RISCO_FADIGA` ⚠️ → `ARTILHEIRO` ⚽ → `MVP` 🏆 → `GARCOM` 🎯 → `EM_CHAMAS` 🔥 → demais. |
| ④ | Breakdown do Rarity Engine: **Físico (30 %)**, **Técnico (40 %)**, **Partidas (30 %)** — os mesmos pesos do `RarityCalculationService`. |
| ⑤ | `Ver Gêmeo` (abre janela GEMEO filtrada pelo atleta) · `Comparar` (adiciona ao side-by-side da janela SCOUTING) · `Seguir`. |
| Estados | `INJURED`: badge 🚑 em 1º lugar com `injuryType` + retorno previsto; botão `Comparar` continua, `Convocar` (se existir) fica desabilitado. `PENDING_DELETE`: card não é renderizado. |

## 3.1 Variações de anel para os demais papéis

| Papel | Anel | Card próprio? |
|---|---|---|
| `STAFF_SAUDE` | `#E11D48` | Variante do card de Staff com badges de reabilitação; dados médicos só para o clube vinculado |
| `AGENTE` | `#0EA5A4` | Card de representação: atletas representados (com consentimento) e oportunidades |
| `TORCEDOR` | `#F472B6` | Card simplificado: clube do coração, coleção FLAIR, sem tier/score |
| `AGENTE_IA` | `#22D3EE` | Sempre com rótulo "IA" e `operatedBy` visível no header |
| `ARBITRO` / `TORCIDA_ORGANIZADA` | `#94A3B8` / `#EC4899` | Registrados por agente; herdam o layout base |

## 4. Card de **Técnico / Staff** (anel azul-claro `#5BA3E6`)

| Zona | Especificidade |
|---|---|
| ① | Metadados: **Função/Licença · Clube**; status: `Staff principal` \| `Auxiliar` \| `Disponível`. |
| ③ | `MESTRE_TATICO` 🧠 (pranchetas executadas) → evolução média do elenco 📈 → disciplina do elenco 🛡️. Não exibe badges físicos próprios. |
| ④ | **Aproveitamento** (% pontos) · **Aderência tática** média dos atletas às pranchetas · **Atletas** sob gestão. |
| ⑤ | `Pranchetas` (janela SCOUTING, origem = staff homologado) · `Elenco` · `Seguir`. |
| Tier | Derivado do Gêmeo coletivo: média ponderada dos tiers dos atletas sob gestão + avaliações da comissão/clube. |

## 5. Card de **Clube** (anel azul `#4C6EF5` — corrigido de `#1E3A8A`, cujo contraste de 1,68:1 violava o mínimo 3:1)

| Zona | Especificidade |
|---|---|
| ① | Metadados: **Divisão · Cidade**; selo `Verificado` quando `OFICIAL_LIGA` homologou. Pode exibir faixa superior `★ MVP DA RODADA ★` (clube com mais MVPs na rodada). |
| ③ | **Vagas abertas** 🏟️ (oportunidades publicadas) → **Dep. médico** 🚑 (contagem agregada — nunca nomes, respeitando Art. 11) → **Plantel** 👥. |
| ④ | **Média de tier** do plantel · **Prontos para jogo** · **Comunicados** da semana. |
| ⑤ | `Plantel` · `Oportunidades` · `Seguir`. |
| Conteúdo típico | Posts `INSTITUCIONAL` (borda cinza) e `LESAO_ALERTA` (borda vermelha) na janela ALERTA, com `source = CLUBE`. |

## 6. Efeitos de raridade (moldura)

| Tier | Faixa | Classe CSS | Efeito |
|---|---|---|---|
| COMMON | 0–400 | `.tier-common` | Borda 1 px `--line`, sem brilho |
| RARE | 401–900 | `.tier-rare` | Borda azul + `box-shadow` glow suave |
| EPIC | 901–1600 | `.tier-epic` | Borda roxa + glow + partículas (`radial-gradient` em `::before`) + destaque da estatística mais alta |
| LEGENDARY | 1601–2400 | `.tier-legendary` | Moldura gradiente **branco + dourado** (2 px, `border-box`) |
| MYTHIC | 2401+ | `.tier-mythic` | Moldura `conic-gradient` animada (`@keyframes mythicAura`, 6 s), aura dupla dourada/roxa, faixa de MVP |

`prefers-reduced-motion: reduce` desliga `mythicAura` e as partículas; a cor da moldura permanece.

## 7. Cards de post (borda de conteúdo)

Todo post tem **borda esquerda de 5 px** na cor do `PostContentType` e o avatar do autor com o anel do `UserRole`:

| Tipo | Cor | Janela padrão | Autores típicos |
|---|---|---|---|
| `TREINO` | Verde `#15805C` | GEMEO | Atleta |
| `PARTIDA_MOMENTO` | Azul `#2B5CB8` | PERFIL | Atleta, Imprensa |
| `RELATORIO_TATICO` | Amarelo `#D4A017` | SCOUTING | Técnico, Scout |
| `INSTITUCIONAL` | Cinza `#6B7280` | ALERTA | Clube, Marca, Liga |
| `LESAO_ALERTA` | Vermelho `#C0376B` | ALERTA | Clube (dado sensível) |

## 8. Mapeamento para o DTO

```json
{
  "userId": "uuid",
  "displayName": "<PREENCHER_NOME>",
  "role": "ATLETA",
  "ringColor": "#15805C",
  "rarityTier": "LEGENDARY",
  "visualEffect": "frame-white-gold",
  "scoreTotal": 1650,
  "meta": { "position": "Meia", "age": 22, "club": "<PREENCHER_CLUBE>", "contractStatus": "CONTRATADO" },
  "topBadges": [
    { "type": "PRONTO_PRA_JOGO", "emoji": "⚡", "label": "Pronto", "detail": "100% físico", "color": "green" },
    { "type": "ARTILHEIRO", "emoji": "⚽", "counter": 14, "label": "Artilheiro", "detail": "temporada" },
    { "type": "MVP", "emoji": "🏆", "counter": 3, "label": "MVP", "detail": "acumulado" }
  ],
  "stats": [
    { "key": "physical", "label": "Físico 30%", "value": 82 },
    { "key": "technical", "label": "Técnico 40%", "value": 88 },
    { "key": "match", "label": "Partidas 30%", "value": 76 }
  ],
  "actions": ["VIEW_TWIN", "COMPARE", "FOLLOW"],
  "physicalStatus": "READY"
}
```

`physicalStatus` e qualquer badge de `PHYSICAL_STATUS` são **nulos/omitidos** quando o viewer não é o dono e `shareMedicalData = false`.

## 9. Responsividade e acessibilidade

- Largura: 340 px (desktop/grade), 100 % − 32 px (mobile, 1 coluna).
- Contraste mínimo 4.5:1 em texto e 3:1 em anéis/bordas sobre `#171A21`.
- Ordem de foco: header → badges (cada um é `button`) → ações. Badges anunciam `aria-label="Artilheiro, 14 gols na temporada, toque para histórico"`.
- Animações respeitam `prefers-reduced-motion`.
