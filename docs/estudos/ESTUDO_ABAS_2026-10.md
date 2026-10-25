# Estudo de melhorias por aba · outubro/2026

Nove agentes de estudo leram o código, chamaram a API local e navegaram a SPA com Playwright (desktop 1440×980 e mobile 390×844). Cada um cobriu uma área: Perfil/Buscar, Gêmeo, Scouting, Alerta, FLAIR, Criar card/Estúdio, Central de Privacidade + Aparência, design de cards e interação social. Nenhum agente alterou o repositório. Os achados P0 abaixo foram conferidos no código antes de entrar aqui.

Evidências visuais: [`docs/preview/estudos/`](../preview/estudos).

## 1. Resumo executivo

- **O produto está largo, mas com fundações frágeis.** Há muitas telas, porém várias sub-abas são fachada (DM, Mural, Pranchetas, Lens, Liga de 8, Coleção, Avatar 3D, Cupons) e quase todas as abas trocam a resposta real por dados fictícios quando a API falha.
- **A maior perda de aprovação vem de confiança, não de falta de função.** Número que não bate com a API, botão que muda contador sem salvar, link que abre "Em construção", odds que não correspondem ao sorteio.
- **Há riscos legais concretos** (LGPD Art. 11 e Art. 14, ECA Digital): boletim de lesão público, perfil de menor exposto com idade exata, consentimentos gravados e nunca lidos, Copilot usando dado de saúde sem opt-in.

## 2. Defeitos P0 (conferidos no código)

| # | Área | Defeito | Onde | Correção proposta |
|---|---|---|---|---|
| 1 | Segurança | Rate limit contornável: `clientIp` confia em qualquer `X-Forwarded-For`, cada IP falso ganha cota nova (inclusive no login) | `config/RateLimitFilter.java:64-67` | Usar `request.getRemoteAddr()` com `RemoteIpValve` só para proxies confiáveis; limitar login por e-mail + IP |
| 2 | Transversal (SPA) | `tryApi` troca qualquer erro (403, 429, 500) por dados fictícios com o selo "API". No Gêmeo, mostra prontidão "78% · PRONTO PRA JOGO" para um menor cujo acesso foi negado | `static/app.js` (`tryApi`) | Mock só no modo demo ou com API fora; estados de carregando, vazio, bloqueado e erro com "tentar de novo" |
| 3 | Alerta / LGPD Art. 11 | Boletim `LESAO_ALERTA` aparece para qualquer usuário, com o selo "🔒 DADO SENSÍVEL" | `service/FeedWindowService.java` (`postSpec` sem filtro médico); `Post` não guarda o atleta-sujeito | `posts.subject_athlete_id` + regra única `MedicalVisibility.canSee` no feed, no PostView e nos comentários |
| 4 | Gêmeo / Perfil / Scouting | `GET /v1/twins/{id}/evolution`, `/dna` e `/compare` não recebem o usuário nem checam visibilidade, status ou consentimento | `controller/TwinController.java:34-43`, `TwinService` | `ProfileAccessPolicy` / `TwinAccessPolicy` com escopos (SCORE, EVOLUTION, DNA, TELEMETRY, LOAD, MEDICAL) |
| 5 | Perfil / ECA Digital | Atleta de 17 anos público com idade exata e UF, sem consentimento do responsável | `config/DataSeeder.java`, `FeedWindowService`, `RankingService` | Menor só `PUBLICO` com `guardianConsentAt`; mostrar categoria (SUB-17), não idade |
| 6 | FLAIR / ECA Digital art. 20 | Odds exibidas 55/25/12/6/2 não correspondem ao sorteio: tier sem atleta cai para o pool inteiro (efetivo ≈ LEGENDARY 80 % · MYTHIC 20 %) | `service/FlairService.java:87-89` | Calcular e exibir odds efetivas do pool (renormalizar tiers vazios); gravar odds por abertura |
| 7 | Criador / LGPD | Copilot não checa `aiAgentProcessing` e coloca "prontidão N%" (dado de saúde) no teaser público | `service/CopilotService.java:39` (só o Autopilot checa, linha 114) | 403 `AI_PROCESSING_OPT_IN`; prontidão e ACWR nunca em texto público sem `shareMedicalData` |
| 8 | Entrada | Login e cadastro aparecem juntos e o cartão é transparente: `.auth-form{display:grid}` anula o atributo `hidden` | `static/styles.css:519-523` | Regra global `[hidden]{display:none!important}` e fundo no `.modal-card.auth` |
| 9 | Central / LGPD Art. 18, VI | Excluir a conta quebra com TypeError depois do step-up: `if (pending)` captura `'delete'` antes do `else if` | `static/app.js` (handler de `#stepUpBtn`) | Testar `pending === 'delete'` primeiro |

## 3. Defeitos P1 mais relevantes

- **Social:** contador de curtidas perde atualizações em concorrência e dá 500 em toque duplo; notificações duplicadas e que não somem ao desfazer; o coração "volta" por até 20 s por causa do cache; link de compartilhar abre "Em construção"; Curtir/Comentar no card de atleta só mudam a tela; qualquer adulto segue menor sem pedido.
- **Alerta:** badge da aba conta curtidas; DM e mural fictícios com a API ligada; filtro "Convocação" devolve tudo; INSTITUCIONAL publicável por papéis não verificados; staff de saúde de outro clube lê e registra lesões.
- **Gêmeo:** KPIs e cabeçalho da Carga fixos no HTML; consentimentos `shareTelemetryWith*` nunca lidos; Autopilot sem publicar/editar; inflação de raridade (todos os atletas do seed entre LEGENDARY e MYTHIC).
- **Scouting:** busca por atributo encontra atletas e a tela descarta; comparador manda ids de demonstração e recebe 500; "Comunidade" não filtra.
- **Perfil/Buscar:** página Buscar sem ação e sem busca no mobile; filtro de clube com ids fictícios; descoberta só traz atletas.
- **FLAIR:** Snap sem risco no último lance; rival revelado inteiro; carta "0" revela lesão; mock de pontos (1.240) com a API ligada.
- **Criador:** rascunho nunca vira publicado; editar card de lesão dá 500; tipos proibidos pré-marcados (403 só na etapa 4); upload falha em silêncio; perda de trabalho ao fechar.
- **Central/Aparência:** contraste abaixo de AA no tema claro e em Areia; tela de fundo deixa chips ilegíveis; anonimização não apaga mídias nem `wallpaperKey`; `wallpaperKey` aceita chave de outro usuário (IDOR).
- **Cards:** a arte substitui a borda de tipo em 100 % das combinações; 100 combinações geram só 73 visuais distintos; o rótulo da arte sobrepõe a actbar; overlays de EPIC/MYTHIC desenhados por cima do conteúdo do `.vcard`.

## 4. Diagnóstico da arte de fundo sobre a fotografia

O agente de cards comparou pixels da foto com e sem arte em 100 combinações de material × estilo × layout, no feed, na prévia, na revisão e no PostView.

- **Hoje nenhuma combinação altera a fotografia no `.pcard`.** A média RGB fica idêntica e o Δ máximo interior é de 5/255, o mesmo ruído que aparece num card sem arte por reamostragem do Chromium.
- **A percepção tem quatro causas reais:**
  1. a foto de teste do card "Título editado no estúdio" era o próprio logo em vitral, exibido com o estilo VITRAL;
  2. tinta forte (38–45 %) em volta da foto cria contraste simultâneo;
  3. o `.vcard` nunca mostra a foto do atleta (`heroImage` é descartado) e exibe um placeholder tingido pela cor do tier;
  4. os efeitos de raridade EPIC (`::after`) e MYTHIC (`::before`) do `.vcard` são desenhados por cima do conteúdo, inclusive do hero.
- **Correção:** isolar `.p-media.photo` numa camada própria acima da arte, sem filtros; levar os overlays de raridade para baixo do conteúdo; mapear `heroImage` no `.vcard`; teste de pixels como critério de aceite.

## 5. Por aba: melhorias de maior impacto na aprovação

| Aba | Top 3 | Esforço |
|---|---|---|
| Perfil/Buscar | Busca unificada `/v1/search` com interpretador pt-BR; filtros confiáveis (chips clicáveis, clube real, `q`/`sort` na URL, paginação); cards multi-papel honestos (clube, scout, marca) | M · S/M · M |
| Gêmeo | Estados honestos + `GET /v1/twins/{id}/summary`; "Registrar treino" em 30 s com ACWR por EWMA; política de acesso por escopo | S · M · M |
| Scouting | Busca de atletas em tabela + correções do comparador; controle LGPD/ECA dos dados de scouting; shortlists com status e notas privadas | S/M · S/M · M |
| Alerta | Bloqueio LGPD de lesões + Saúde por papel; caixa de alertas própria (lido/ciente, badge, preferências); mural do clube real | M · M · M |
| FLAIR | Base de confiança (odds reais, sem mock, retomar partida); pacote honesto com revelação animada; duelo com decisão (rival oculto, atacante alterna, Snap com recuo) | S/M · S/M · M |
| Criar card/Estúdio | Ciclo de rascunho e "perda zero" (autosave, confirmar ao fechar, publicar rascunho, excluir com desfazer); capacidades por papel; Copilot conforme e útil | M · S · M |
| Central/Aparência | Correções P0 da entrada e da exclusão; tokens AA (`--accent-ink`, `--on-accent`, `--text-2/3`); novos eixos de aparência com prévia ao vivo | XS · M · L |
| Cards | Arte em camadas (cada dimensão escreve só a sua camada); tinta com luminância travada (AA garantido); novas variações + 2ª cor | M · S · M |
| Social | Reações íntegras (UPDATE atômico, PUT/DELETE idempotentes); compartilhar que funciona + Salvos; notificações 2.0 (agrupar, preferências, SSE) | M · S/M · M |

## 6. Proposta de variações de aparência (navegação e layout)

Novos atributos em `<html>`, combináveis com os existentes (`data-theme`, `data-accent`, `data-surface`, `data-wallpaper`). Cores de papel e bordas de tipo não mudam.

| Eixo | Opções | Onde se aplica |
|---|---|---|
| Material (`UiMaterial`) | FOSCO · VIDRO · METALICO · CARBONO · GRAMADO | topbar, sidebar, barra inferior, navegação da Central |
| Gradiente (`UiGradient`) | NENHUM · HOLOFOTE · ARQUIBANCADA · CREPUSCULO | fundo ambiente, borda da topbar, item ativo (≤ 12 % atrás de texto) |
| Fonte (`UiFont`) | PADRAO · LEGIVEL · PLACAR · EDITORIAL · SISTEMA | `--font-display/body/mono` |
| Densidade (`UiDensity`) | COMPACTA · CONFORTAVEL · ESPACOSA | altura da topbar, largura da sidebar, ícones, padding |
| Contraste (`UiContrast`) | PADRAO · ALTO | texto, linhas, opacidade; ALTO automático com `prefers-contrast:more` |
| Destaque | + PRATA, com pares fill/ink por tema | botões, barras, títulos |

Predefinições em um clique: Noite de jogo, Gramado, Prancheta clara, Alta legibilidade, Carbono. Protótipos: `docs/preview/estudos/aparencia_proto_*.jpg`.

## 7. Proposta de variações de background dos cards

- **Gradiente (`CardGradient`):** SUAVE · SOLIDO · DEGRADE · RADIAL · DIAGONAL, com segunda cor opcional (`color2`).
- **Materiais novos:** CETIM · MALHA · CROMO · PAPEL · PRISMA.
- **Estilos/padrões novos:** GRAMADO · REDE · LISTRAS · FAIXA · XADREZ · PRANCHETA · ARQUIBANCADA · MOSAICO.
- **Paletas para a 2ª cor:** Alvinegro, Alviverde, Rubro-negro, Tricolor, Celeste, Canarinho.
- **Regras:** tinta com luminância travada para manter AA; padrões com alfa ≤ 0,06; LESAO_ALERTA e INSTITUCIONAL restritos a combinações sóbrias; a borda de tipo é sempre preservada; a foto nunca recebe arte.

## 8. Roteiro sugerido

1. **Sprint 0 — P0 (1 PR):** itens 1–9 da seção 2. Esforço total estimado: M.
2. **Sprint 1 — Aparência e cards (pedido do usuário):** eixos de aparência, tokens AA, arte em camadas, novas variações, isolamento da foto, `heroImage`.
3. **Sprint 2 — Confiança:** estados honestos em todas as abas, reações íntegras, notificações deduplicadas, compartilhar com rota `#/post/:id`, ciclo de rascunho.
4. **Sprint 3 — Funções de maior valor:** busca unificada, registrar treino, shortlists, caixa de alertas, coleção/escalação do FLAIR.
5. **Depois:** DM com salvaguardas para menores, bloquear/denunciar/moderar, tempo real (SSE), Liga de 8, relatório estruturado de scouting, pranchetas.

## 9. Critérios de aceite transversais

1. Com a API online, nenhuma aba mostra dados fictícios; falhas mostram estado de erro com "tentar de novo".
2. Matriz de acesso (papel × consentimento × visibilidade × idade) coberta por teste parametrizado; menor nunca exposto a terceiros sem base legal.
3. Nenhum contador muda sem requisição confirmada; 50 curtidas simultâneas resultam em `like_count = COUNT(reações)`.
4. Contraste AA (texto ≥ 4,5:1, UI ≥ 3:1) em todas as combinações de aparência e de arte de card, verificado por teste.
5. Foto do usuário com Δ ≤ 6/255 com e sem arte em feed, prévia, revisão, PostView e `.vcard`.
6. Mobile 390×844 sem rolagem horizontal, alvos ≥ 44 px e foco gerenciado em modais.
