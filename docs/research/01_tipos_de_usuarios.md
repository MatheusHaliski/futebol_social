# 01 · Tipos de usuários do ecossistema do futebol — pesquisa e taxonomia recomendada

**Projeto:** Football AI · Social (Java/Spring) · **Documento-base:** `docs/PROMPT_FOOTBALL_AI_SOCIAL.md` (RF2, RF3)
**Data:** 2026-10-24 · **Status:** proposta para decisão (não implementada; sem commit)

---

## 1. Resumo executivo

1. Os 7 papéis atuais cobrem bem o "núcleo transacional" (atleta, comissão, clube, scout, marca, imprensa, entidade), mas deixam de fora atores com **status legal/regulatório próprio** no Brasil: **agente licenciado FIFA** (RNAF/CBF 2023), **torcedor** (Lei 14.597/2023, art. 178), **árbitro** (RENAF/Comissão de Arbitragem) e **departamento de saúde** (CRM/CREFITO; dado sensível LGPD art. 11).
2. `TECNICO_STAFF` agrupa funções com direitos de publicação muito diferentes: o licenciamento de clubes (FPF 2026, baseado no da CBF) exige treinador, auxiliar, treinador de goleiros, preparador físico, fisioterapeuta e massagista, e recomenda fisiologista, analista de desempenho, nutricionista e psicólogo. Recomenda-se **STAFF com `StaffSpecialty`** + um papel separado **`STAFF_SAUDE`** (único, além do próprio atleta, autorizado a originar `LESAO_ALERTA`).
3. `OFICIAL_LIGA` mistura **organizador** (confederação/federação/liga) com **arbitragem**; devem ser separados — a arbitragem é indicada pelas federações, mas é uma carreira própria (RENAF, árbitros profissionais previstos para 2027).
4. **Scout homologado vs. comunidade** não deve virar dois enums: o modelo Transfermarkt (voluntários) e o Wyscout/BeSoccer Pro (B2B) convivem; usar `VerificationLevel` no mesmo papel `SCOUT`.
5. **Casas de apostas não devem ser um papel**: são `MARCA` com `categoria = APOSTAS` e capacidades restritas (Anexo X CONAR, Lei 14.790/2023, PL 3.563/2024 em tramitação, e o licenciamento FPF 2026 que obriga autoexclusão de toda a comissão técnica).
6. **Criador de conteúdo** é sub-tipo de `IMPRENSA` (credenciada vs. independente), não papel próprio — mercados estão justamente diferenciando credenciamento (Wimbledon, US Open, Boca Juniors), e no Brasil 37% do conteúdo da Copa 2026 veio de criadores.
7. **Agentes de IA** precisam de identidade própria (`AGENTE_IA`) com rótulo obrigatório — EU AI Act art. 50 em vigor desde 02/08/2026 — e nunca herdam o anel de um humano.
8. Achado colateral: o anel `CLUBE #1E3A8A` tem contraste **1,68:1** sobre `#171A21` (abaixo do 3:1 exigido no RF3) e `MARCA #7C3AED` fica em 3,06:1 (limítrofe). Sugere-se ajuste de `CLUBE` para `#4C6EF5` (4,03:1) ou, no mínimo, um halo claro.
9. Taxonomia final proposta: **11 papéis no core** (7 atuais, 1 renomeado, + `STAFF_SAUDE`, `AGENTE`, `TORCEDOR`, `AGENTE_IA`) e **2 registráveis por agente** (`ARBITRO`, `TORCIDA_ORGANIZADA`), com `ACADEMIA/FORMADOR` como sub-tipo de `CLUBE`.
10. Impacto de código: enum `UserRole` + enums de sub-tipo (`StaffSpecialty`, `ClubeTipo`, `MidiaTipo`, `EntidadeTipo`, `MarcaCategoria`), 4 novas subclasses JPA JOINED, e `RoleCapability` passando a ter dimensão de **verificação** além de papel.

---

## 2. Achados com fontes

Legenda: **[A]** = página aberta e lida nesta pesquisa (WebFetch); **[B]** = visto apenas em resultado de busca (página não pôde ser aberta — 404/503); tratar como indicação, não como fonte verificada.

### 2.1 Atores previstos em regulação brasileira (CBF/Lei)

| Ator | O que a fonte estabelece | Fonte |
|---|---|---|
| **Agente de futebol (intermediário)** | O RNAF (CBF, 04/10/2023) revogou o Regulamento de Intermediários; a atividade passou a ser "privativa de pessoas físicas que obtenham licença junto à FIFA"; transição até 30/06/2024; contratos com atleta/treinador ≤ 2 anos, sem renovação automática; dupla representação só com consentimento escrito; atleta e treinador podem se autorrepresentar; parentes/advogados isentos (Lei 14.597 art. 95 §1º). | [A] https://www.bicharaemotta.com.br/cbf-publica-o-novo-regulamento-nacional-de-agentes-de-futebol-com-impactos-significativos-para-o-mercado-brasileiro/ |
| **Agente licenciado FIFA** | FIFA mantém o diretório público de agentes licenciados ("click here to access all Football Agents licensed by FIFA" → agents.fifa.com) e trata das licenças "to become and remain a FIFA-licensed Football Agent". Implicação: **verificável por número de licença**. | [A] https://inside.fifa.com/legal/football-regulatory/agents |
| **Torcedor / torcida organizada** | Lei 14.597/2023, art. 178: "Torcedor é toda pessoa que aprecia, apoia ou se associa a qualquer organização esportiva"; §2º torcida organizada é pessoa jurídica de direito privado ou existente de fato; §4º obrigação de "manter cadastro atualizado de seus associados" (nome, foto, CPF etc.); §§5º–6º responsabilidade civil objetiva e solidária. | [A] https://modeloinicial.com.br/lei/L-14597-2023/torcedor-@___III_II (texto consolidado; o Planalto retornou 503 no momento da pesquisa) |
| **Clube formador / categoria de base** | CCF emitido pela CBF "com o apoio das Federações Estaduais" (RDP 01/2019), válido por 1 ano; exige relação de técnicos e preparadores físicos da base, assistência médica (CRM), reabilitação sob fisioterapeuta (CREFITO), "assistência nutricional, odontológica e psicológica", assistência educacional; apenas 38 clubes certificados à época da apresentação (2021). | [A] https://www2.camara.leg.br/atividade-legislativa/comissoes/comissoes-temporarias/especiais/56a-legislatura/pl-1153-19-direito-aos-atletas-de-base-lei-pele/apresentacoes-em-eventos/CBF19.08.21.pdf |
| **Comissão técnica, diretoria e saúde (licenciamento de clubes)** | Regulamento de Licenciamento do Paulista 2026 (FPF; declara seguir o "Regulamento de Licença de Clubes da CBF"): cargos **obrigatórios** no organograma — Diretor Geral, Financeiro, Administrativo, de Comunicação, de Marketing, de Futebol de Base, Oficial de Operação e Segurança, Oficial de Infraestrutura, Ouvidor; comissão **obrigatória** — Treinador, Auxiliar técnico, Treinador de goleiros, Preparador físico, Fisioterapeuta, Massagista; médico responsável com CRM; **recomendados** — Fisiologista, Analista de desempenho, Nutricionista, Psicólogo; recomenda Licença CBF (PRO/A/B) para preparador físico e analista; exige **autoexclusão de apostas** (Portal da Secretaria de Prêmios e Apostas) de todo o organograma e comissão técnica até 27/11/2026. | [A] https://futebolpaulista.com.br/Repositorio/Institucional/Resolucao/3684/3684_639258522594600654.pdf |
| **Formação/licenças por função** | Catálogo CBF Academy lista "Licença A/B/C – Treinador de Futebol", "Licença B – Analista de Desempenho", "Fisioterapia no Futebol", "Gestão de Futebol", "Governança e Compliance no Futebol", "Workshop de Intermediação no Futebol", "Psicologia do Futebol", pós em Direito Esportivo e em Liderança e Gestão. Em resultado de busca, páginas de "Licença A/B – Preparador Físico" e Licença PRO de analista. | [A] https://plataforma.cbfacademy.com.br/pt-br/cursos · [B] https://cbfacademy.com.br/escola-tecnica/licenca-a-preparador-fisico/ |
| **Árbitro** | Resultados de busca: federações estaduais indicam anualmente árbitros, assistentes e assessores para a RENAF (Relação Nacional de Árbitros); CBF criou Diretoria de Arbitragem e planeja árbitros profissionais em 2027; o Portal de Governança da CBF lista árbitros por federação/cargo/categoria. | [B] https://portaldegovernanca.cbf.com.br/relacao-arbitros (503) · [B] https://br.soccerway.com/noticias/futebol-brasileirao-betano-cbf-busca-novos-talentos-e-planeja-arbitros-profissionais-no-brasil-em-2027/06B8I6rA |
| **Executivo de futebol / diretor esportivo** | Resultados de busca: ABEX (Associação Brasileira dos Executivos de Futebol) busca regulamentação da profissão; CBF Academy mantém "Programa de Formação de Executivos de Futebol". Página da ABEX retornou 503. | [B] https://www.abexfutebol.com.br/cursos/ · [B] https://fia.com.br/blog/gestao-esportiva-como-e-o-curso-e-o-que-faz-um-executivo-de-clubes/ |

### 2.2 Patrocínio e casas de apostas (atenção regulatória)

| Ponto | Conteúdo | Fonte |
|---|---|---|
| Anexo X do CONAR (dez/2023, pós Lei 14.790/2023) | Publicidade de apostas "exclusivamente destinada ao público adulto"; em redes sociais, só perfis/influenciadores com público adulto; quem aparece apostando deve "ser e parecer maior de 21 anos"; publicidade de influenciadores/afiliados deve ser claramente comercial; advertência obrigatória ("jogue com responsabilidade"); vedado prometer ganhos, sugerir investimento ou oferecer crédito. | [A] https://www.demarest.com.br/conar-regulamenta-a-publicidade-de-apostas-esportivas/ |
| PL 3.563/2024 (Senado) | Aprovado na CCT em 04/02/2026; proíbe propaganda, patrocínio e promoção de bets "em todas as modalidades" — incluindo logos em uniformes/placas de clubes, federações e estádios e "promoção de jogos por celebridades" e criadores; segue para CCJ/Plenário/Câmara. **Risco regulatório alto e em movimento.** | [A] https://www12.senado.leg.br/noticias/materias/2026/02/05/projeto-que-veda-propaganda-e-patrocinio-de-bets-avanca-no-senado |
| Autoexclusão de profissionais de clube | Licenciamento FPF 2026 exige comprovação de autoexclusão de apostas de dirigentes e comissão técnica (ver 2.1). Implicação: **o produto não pode direcionar conteúdo/ofertas de bets a `ATLETA`, `STAFF`, `ARBITRO`, `OFICIAL_LIGA`.** | [A] FPF (link acima) |
| Mercado de dados vende para bets | PlayMakerAI lista "Betting companies & sportsbooks" como segmento ao lado de clubes, mídia, agentes e fãs. | [A] https://www.playmaker.ai/football/who-we-help |

### 2.3 Imprensa vs. criadores de conteúdo

- Eventos estão separando credenciamento: Wimbledon "não terá categoria de credenciamento para influenciador"; US Open dobrou programa de criadores (54 → ~100) e revogou credenciais; Boca Juniors proíbe gravação/lives em setores de sócios sem autorização. No Brasil, "37% de tudo que foi produzido sobre a Copa 2026 veio de criadores digitais" (Inflr) e 30% dos torcedores preferem se informar com influenciadores (Wake Creators, 2025). Conclusão da matéria: "o modelo pensado para imprensa tradicional está mostrando que precisa de ajuste para esse tipo de perfil". [A] https://maquinadoesporte.com.br/midia/us-open-wimbledon-e-boca-juniors-por-que-eventos-esportivos-estao-revendo-as-regras-para-influenciadores-no-esporte/
- Credenciamento de jogos de Brasileirão/Copa do Brasil é feito pela CBF em sistema próprio; estaduais pelas federações (página de imprensa do Palmeiras, vista em busca). [B] https://www.palmeiras.com.br/imprensa/

### 2.4 Agentes de IA como usuários

- EU AI Act, art. 50: desde **02/08/2026**, provedores de sistemas que interagem com pessoas devem garantir que o usuário saiba que lida com IA ("know they are dealing with AI, not a human"); conteúdo sintético deve ter marcação legível por máquina (50(2)); texto de interesse público gerado por IA deve ser rotulado salvo revisão editorial humana (50(4)); informação "clear and distinguishable" na primeira interação. Sistemas apenas de backend ficam fora. [A] https://www.legal500.com/intelligence/united-kingdom/privacy/is-it-a-bot-eu-ai-act-transparency-rules-take-effect-2-august-2026
- Implicação de produto: um agente de IA que publica cards ou responde DMs no feed **interage com pessoas** → precisa de papel próprio, rótulo visual e "principal" humano/organização responsável (`operatedBy`).

### 2.5 Como as plataformas adjacentes classificam usuários

| Plataforma | Classificação declarada | Fonte |
|---|---|---|
| **Hudl Wyscout** | "built for soccer professionals involved in recruitment and scouting, including **scouts, agents, analysts and technical directors**"; "recruitment teams"; clubes (Tag on Demand); público B2B. Artigo de terceiros acrescenta "independent agents and small representative firms", "established player agent agencies", "professional football clubs", "scouts and video analysts", "youth development directors". | [A] https://www.hudl.com/products/wyscout/faq · [A] https://fifa.sportsagentinstitute.com/en/blog/wyscout |
| **Hudl Instat** | "College and pro coaches", "scouts and recruiters", "video analysts and coordinators", "front office staff". | [A] https://www.hudl.com/products/instat/faqs |
| **BeSoccer Pro** | "the ultimate solution for **clubs, agencies and media**" para "scouting, content creation and performance analysis". | [A] https://pro.besoccer.com/aboutus |
| **Transfermarkt** | Base alimentada por **voluntários** ("wisdom of the crowd"), com um "godfather" por liga fixando valores; usada por scouts (head scout do NEC: idade, contrato, nome do agente), diretorias (citada em relatórios anuais de Lyon, Schalke, Porto…), agentes e mídia. Modelo de **comunidade não homologada com curadoria**. | [A] https://www.ftm.eu/articles/transfermarkt-volunteers-european-football |
| **Fieldoo** | Grupos: **players, agents, scouts, clubs**; "market posts" (Trial/Camp/Academy, Need/Offer player, Agent partnership). Site atual: "Whether you're a player, a scout, a club or an investor" e postura crítica a agentes ("No agents taking 30% for doing nothing"). | [A] https://en.wikipedia.org/wiki/Fieldoo · [A] https://www.fieldoo.com/ |
| **Tonsser** | Em busca: FAQ cita "three different roles on Tonsser: **Player, Coach and Supporter**"; clubes e scouts pagam assinatura pelo banco de dados (não são "papéis" no app). Página do FAQ retornou 404; perfil no The Hub só cita players (1,5 M jogadores jovens; modelo "Marketplace"). | [B] https://tonsser.com/faq · [A] https://thehub.io/startups/tonsser |
| **PLAYMAKER.pro (PL)** | **Players** (perfil, PM Score, "express interest in the club") e **clubs/scouts** ("You publish who you're looking for. Players apply."); rodapé lista **coaches, officials, fans**. | [A] https://www.playmaker.pro/en |
| **PlayMakerAI** | Segmentos: **Clubs (and academies), Betting companies & sportsbooks, Media (broadcasters, publishers, creators), Agents, Fans & analytical enthusiasts**. | [A] https://www.playmaker.ai/football/who-we-help |
| **Footure Pro (BR)** | Consultoria para "**CLUBES, AGENTES e JOGADORES**": scouting, relatórios comparativos, performance individual/coletiva, relatórios de adversários, assessoria técnica a atletas e prospecção para agentes. | [A] https://footure.com.br/footure-pro/ |

Observação: "Playmaker" é nome ambíguo (PLAYMAKER.pro polonês, PlayMakerAI, Playermaker — sensor vestível). As duas primeiras foram as consideradas.

---

## 3. Avaliação dos papéis atuais

| Papel atual | Veredito | Justificativa |
|---|---|---|
| `ATLETA` | **Manter** | Único produtor de telemetria (`TREINO`) e sujeito do Gêmeo Digital. Acrescentar atributo `categoria` (BASE / PROFISSIONAL / AMADOR / FUTSAL / FEMININO) e `menorDeIdade` (RNAF permite representação a partir de 15,5 anos com consentimento; LGPD art. 14). |
| `TECNICO_STAFF` | **Renomear → `STAFF` + `StaffSpecialty`; extrair `STAFF_SAUDE`** | O licenciamento FPF/CBF lista ≥ 10 funções distintas. Treinador, auxiliar, treinador de goleiros, preparador físico, fisiologista e analista compartilham o foco "prancheta/treino" (`RELATORIO_TATICO`, `TREINO` do grupo). Médico, fisioterapeuta, nutricionista e psicólogo lidam com **dado de saúde (LGPD art. 11)** e são a origem legítima de `LESAO_ALERTA`/`InjuryRecord` — merecem papel, anel e `RoleCapability` próprios. Diretor esportivo/executivo entra como `StaffSpecialty.GESTAO` (foca em `INSTITUCIONAL`/oportunidades), vinculado ao `CLUBE`. |
| `CLUBE` | **Manter como conta de organização; adicionar `ClubeTipo`** | `PROFISSIONAL`, `FORMADOR_CCF` (certificado CBF, válido 1 ano, verificável na lista pública), `ESCOLINHA_ACADEMIA`, `AMADOR_VARZEA`. Escolinhas/academias não são clubes formadores (Lei Pelé art. 29) mas são "entidades de prática" e o maior funil de atletas de base. **Corrigir cor**: `#1E3A8A` tem 1,68:1 sobre `#171A21`. |
| `SCOUT` | **Manter; adicionar `VerificationLevel`** | Wyscout/BeSoccer Pro atendem scouts de clube/agência (homologados); Transfermarkt prova valor da comunidade. Um único enum com `COMUNIDADE → VERIFICADO (vínculo com CLUBE/AGENTE) → HOMOLOGADO (licença CBF Academy/ certificação)`. Olheiro independente = `SCOUT` + `COMUNIDADE`. Relatórios de comunidade não alimentam os 40% de avaliação técnica do Rarity Engine; só os de nível `VERIFICADO+`. |
| `MARCA` | **Manter; adicionar `MarcaCategoria`** | `PATROCINADOR`, `MATERIAL_ESPORTIVO`, `APOSTAS` (regulada), `AGENCIA_MARKETING`. `APOSTAS` recebe capacidades restritas: só usuários ≥ 18 verificados, advertência obrigatória no card, nunca segmentar para `ATLETA`/`STAFF`/`ARBITRO`/`OFICIAL_LIGA` (autoexclusão exigida no licenciamento), `kill switch` para quando/se PL 3.563 for sancionado. Contraste 3,06:1 — limítrofe; opcional elevar para `#8B5CF6` (4,11:1). |
| `IMPRENSA` | **Manter nome; adicionar `MidiaTipo`** | `VEICULO_CREDENCIADO`, `JORNALISTA`, `CRIADOR_INDEPENDENTE`, `PODCAST/CANAL`. Mesmo anel (laranja) + selo de credenciamento. Criador de conteúdo **não** vira papel: a diferença real é credenciamento/verificação, exatamente o que o mercado está ajustando. |
| `OFICIAL_LIGA` | **Dividir: `OFICIAL_LIGA` (organizador) e `ARBITRO`** | Organizador = CBF, federações estaduais, ligas (Libra/LFU), CONMEBOL; publica `INSTITUCIONAL` (tabelas, punições, convocações). Árbitro = carreira própria (RENAF, indicada pelas federações; profissionalização 2027). Misturá-los confunde quem "fala pela competição" com quem "apita". `ARBITRO` nasce por agente (volume menor), mas com anel reservado desde já. |
| *(ausente)* `AGENTE` | **Criar no core** | Licença FIFA obrigatória no Brasil desde 30/06/2024; verificável no diretório FIFA; é o terceiro vértice de toda transação (Fieldoo, Wyscout, PlayMakerAI, Footure Pro o tratam como segmento próprio). "Empresário" não licenciado = `AGENTE` com `VerificationLevel.PENDENTE` e sem direito a publicar oportunidades/representação. |
| *(ausente)* `TORCEDOR` | **Criar no core (viewer+)** | Definido em lei (art. 178), maior funil de aquisição, e consumidor de `PARTIDA_MOMENTO`/`PERFIL`. Não é apenas "viewer": publica em `PERFIL`, reage, segue; **não** publica `RELATORIO_TATICO`, `TREINO`, `LESAO_ALERTA`, `INSTITUCIONAL`. `TORCIDA_ORGANIZADA` é conta de organização derivada (CNPJ/cadastro de membros), registrada por agente. |
| *(ausente)* `AGENTE_IA` | **Criar no core (identidade reservada)** | O RF2 já prevê agentes registrando papéis; mas o **agente que publica/interage** é ele próprio um ator do feed e o art. 50 do AI Act exige transparência. Campos: `operatedBy` (User/Org), `modelId`, `purpose`, rótulo "IA" sempre visível, sem Rarity/Gêmeo. |
| *(ausente)* `FEDERACAO_ESTADUAL` | **Não criar** — sub-tipo de `OFICIAL_LIGA` (`EntidadeTipo.FEDERACAO_ESTADUAL`) | Mesma natureza (entidade de administração). |
| *(ausente)* `CASA_APOSTAS` | **Não criar** — `MarcaCategoria.APOSTAS` | Ver `MARCA`. |

---

## 4. Taxonomia recomendada

Fundo de referência para contraste: `#171A21`. Razões calculadas pela fórmula WCAG 2.1 (luminância relativa). Janelas: `PERFIL`, `GEMEO`, `SCOUTING`, `ALERTA`. Tipos: `TREINO`, `PARTIDA_MOMENTO`, `RELATORIO_TATICO`, `INSTITUCIONAL`, `LESAO_ALERTA`.

| `UserRole` | Anel (hex) | Contraste | Foco | Pode publicar (`PostContentType`) | Janelas principais | Origem | Sub-tipo / verificação |
|---|---|---|---|---|---|---|---|
| `ATLETA` | `#15805C` (atual) | 3,54 | Telemetria, treino, currículo, Gêmeo | `TREINO`, `PARTIDA_MOMENTO`, `LESAO_ALERTA` (própria, opt-in `shareMedicalData`) | GEMEO, PERFIL, ALERTA | Core | `categoria`, `menorDeIdade` |
| `STAFF` (ex-`TECNICO_STAFF`) | `#5BA3E6` (atual) | 6,49 | Prancheta, treinos, gestão de elenco, gestão esportiva | `RELATORIO_TATICO`, `TREINO` (coletivo), `PARTIDA_MOMENTO`, `INSTITUCIONAL` (se `GESTAO` e vinculado a `CLUBE`) | SCOUTING, GEMEO, ALERTA | Core | `StaffSpecialty`: TREINADOR, AUXILIAR, TREINADOR_GOLEIROS, PREPARADOR_FISICO, FISIOLOGISTA, ANALISTA_DESEMPENHO, GESTAO (diretor esportivo/executivo), OUTRO; licença CBF opcional |
| `STAFF_SAUDE` | `#E11D48` | 3,71 | Status médico, reabilitação, nutrição, psicologia | `LESAO_ALERTA` (origem de `InjuryRecord`), `TREINO` (prescrição), `INSTITUCIONAL` (boletim médico via clube) | ALERTA, GEMEO | Core | `HealthSpecialty`: MEDICO (CRM), FISIOTERAPEUTA (CREFITO), NUTRICIONISTA (CRN), PSICOLOGO (CRP), MASSAGISTA; registro profissional obrigatório para `VERIFICADO` |
| `CLUBE` | `#1E3A8A` (atual) → **sugerido `#4C6EF5`** | 1,68 → 4,03 | Institucional, plantel, oportunidades | `INSTITUCIONAL`, `PARTIDA_MOMENTO`, `RELATORIO_TATICO` (via staff vinculado) | ALERTA, PERFIL, SCOUTING | Core | `ClubeTipo`: PROFISSIONAL, FORMADOR_CCF, ESCOLINHA_ACADEMIA, AMADOR; membros via `OrgMembership` |
| `SCOUT` | `#D4A017` (atual) | 7,33 | Observação, relatórios, shortlists | `RELATORIO_TATICO`, `PARTIDA_MOMENTO` | SCOUTING, PERFIL | Core | `VerificationLevel`: COMUNIDADE, VERIFICADO (vínculo), HOMOLOGADO (licença) — só VERIFICADO+ conta no Rarity |
| `AGENTE` | `#0EA5A4` | 5,75 | Representação, mercado, oportunidades | `INSTITUCIONAL` (oportunidade/representação, apenas `VERIFICADO`), `PARTIDA_MOMENTO` | PERFIL, ALERTA | Core | `fifaLicenseId` (diretório FIFA); PENDENTE sem licença; `representa[]` com consentimento do atleta/treinador |
| `MARCA` | `#7C3AED` (atual; opcional `#8B5CF6`) | 3,06 (4,11) | Patrocínio e ativação | `INSTITUCIONAL`, `PARTIDA_MOMENTO` (conteúdo patrocinado rotulado) | PERFIL, ALERTA | Core | `MarcaCategoria`: PATROCINADOR, MATERIAL, AGENCIA, **APOSTAS** (18+, advertência, exclusões de audiência, kill switch) |
| `IMPRENSA` | `#EA580C` (atual) | 4,89 | Cobertura, comunicados, criação de conteúdo | `PARTIDA_MOMENTO`, `INSTITUCIONAL` (notícia/comunicado), `RELATORIO_TATICO` (análise opinativa, rotulada) | PERFIL, SCOUTING | Core | `MidiaTipo`: VEICULO_CREDENCIADO, JORNALISTA, CRIADOR_INDEPENDENTE, CANAL_PODCAST; selo de credenciamento (CBF/federação) |
| `OFICIAL_LIGA` | `#C9A227` (atual) | 7,20 | Entidades organizadoras | `INSTITUCIONAL` (tabelas, súmulas, punições, convocações), `PARTIDA_MOMENTO` oficial | ALERTA, PERFIL | Core | `EntidadeTipo`: CONFEDERACAO, FEDERACAO_ESTADUAL, LIGA, ORGANIZADOR_TORNEIO |
| `TORCEDOR` | `#F472B6` | 6,57 | Acompanhar, reagir, colecionar cards | `PARTIDA_MOMENTO` (somente em PERFIL) | PERFIL | Core (viewer+) | padrão de cadastro; `clubeDoCoracao`; sem Gêmeo/Rarity |
| `AGENTE_IA` | `#22D3EE` | 9,63 | Automação transparente (resumos, alertas, análises) | Qualquer tipo **delegado** pelo `operatedBy`, sempre rotulado "IA"; nunca `LESAO_ALERTA` sem humano responsável | conforme delegação | Core (identidade) / instâncias por agente | `operatedBy`, `modelId`, `purpose`, `disclosureVersion` (AI Act art. 50) |
| `ARBITRO` | `#94A3B8` | 6,79 | Arbitragem, súmulas, formação | `PARTIDA_MOMENTO`, `INSTITUCIONAL` (escala/súmula se vinculado à federação) | PERFIL, ALERTA | **Registrado por agente** | `quadro`: ESTADUAL, RENAF, FIFA; `funcao`: ARBITRO, ASSISTENTE, VAR, ASSESSOR |
| `TORCIDA_ORGANIZADA` | `#EC4899` | 4,94 | Conta coletiva de torcida | `INSTITUCIONAL` (avisos de caravana/ação social), `PARTIDA_MOMENTO` | PERFIL, ALERTA | **Registrado por agente** | `cnpj`/cadastro de membros (art. 178 §4º); responsável legal |

Notas de cor: as cinco novas cores foram escolhidas para não colidir com as atuais em matiz ou luminosidade — vermelho-carmim (saúde) vs. laranja (imprensa); teal (agente) vs. verde (atleta) e azul-claro (staff); rosa (torcedor) e magenta (organizada) vs. roxo (marca); ciano (IA) vs. azul-claro (staff); cinza-ardósia (árbitro) é o único neutro. A borda cinza de `INSTITUCIONAL` é elemento distinto do anel, mas vale testar `ARBITRO` com halo. Todas ≥ 3:1; `STAFF_SAUDE` e `ATLETA` ficam na faixa 3,5–3,7, então evitar anéis muito finos (< 2 px) nesses dois.

---

## 5. Impacto no código

### 5.1 Enum `UserRole`

```java
public enum UserRole {
    ATLETA("#15805C"), STAFF("#5BA3E6"), STAFF_SAUDE("#E11D48"),
    CLUBE("#4C6EF5"), SCOUT("#D4A017"), AGENTE("#0EA5A4"),
    MARCA("#7C3AED"), IMPRENSA("#EA580C"), OFICIAL_LIGA("#C9A227"),
    TORCEDOR("#F472B6"), AGENTE_IA("#22D3EE"),
    // reservados; ativados via RoleRegistry por agentes
    ARBITRO("#94A3B8"), TORCIDA_ORGANIZADA("#EC4899");

    private final String ringHex;
    // ringHex, isOrganization(), isHuman(), isAutomated()
}
```

- `TECNICO_STAFF` → `STAFF`: migração Flyway `UPDATE users SET role='STAFF' WHERE role='TECNICO_STAFF'` + `@Deprecated` alias de desserialização (`@JsonAlias("TECNICO_STAFF")`) por 2 versões de API.
- Se a equipe preferir manter o enum estritamente fechado ao core, `ARBITRO`/`TORCIDA_ORGANIZADA` podem viver num `RoleRegistry` (tabela `role_definition` com `code`, `ringHex`, `capabilities`) consultado pelo Strategy — é o mecanismo já previsto no RF2 para `AGENTE_IA`/`PREPARADOR_FISICO`. Recomendação: **cores reservadas no enum**, comportamento no registry.

### 5.2 Subclasses JPA (`@Inheritance(JOINED)` a partir de `User`)

| Subclasse | Campos principais | Observações |
|---|---|---|
| `Athlete` (existente) | `+ categoria`, `+ birthDate`, `+ guardianConsent` | LGPD art. 14 para menores |
| `Staff` (renomear `TechnicalStaff`) | `specialty: StaffSpecialty`, `cbfLicense`, `orgMembership` | Diretor esportivo = `GESTAO` |
| **`HealthStaff`** (nova) | `specialty: HealthSpecialty`, `professionalRegistry` (CRM/CREFITO/CRN/CRP), `registryUf` | Única classe com `canOriginateInjuryRecord()` |
| `Club` (existente) | `+ clubeTipo: ClubeTipo`, `+ ccfValidUntil`, `+ federacao` | CCF validado contra lista pública da CBF |
| `Scout` (existente) | `+ verificationLevel`, `+ affiliation (Club/Agent)` | |
| **`Agent`** (nova) | `fifaLicenseId`, `licenseStatus`, `agencyName`, `representations: List<Representation>` | `Representation` com consentimento e `validUntil ≤ 2 anos` (RNAF) |
| `Brand` (existente) | `+ categoria: MarcaCategoria`, `+ fazendaAuthorizationId` (se APOSTAS) | |
| `Press` (existente) | `+ midiaTipo`, `+ credentialIssuer`, `+ credentialValidUntil` | |
| `LeagueOfficial` (existente) | `+ entidadeTipo` | Remover semântica de arbitragem |
| **`Supporter`** (nova) | `favoriteClub`, `memberSince` | Sem `DigitalTwin` |
| **`AiAgent`** (nova) | `operatedBy (User)`, `modelId`, `purpose`, `disclosureVersion`, `delegatedCapabilities` | `TermsAcceptance` do operador; rótulo obrigatório no `CardDTO.authorBadge = "IA"` |
| `Referee`, `SupporterGroup` | via registry (tabelas satélite) | agentes registram |

### 5.3 `RoleCapability`

Passar de `Map<UserRole, Set<Capability>>` para avaliação **(role, subtype, verificationLevel)**:

```java
public interface RoleCapability {
    boolean canPublish(PostContentType type, UserContext ctx);   // ctx = role + subtype + verification + orgMembership
    Set<FeedWindow> defaultWindows(UserContext ctx);
    boolean canOriginateInjuryRecord(UserContext ctx);           // só STAFF_SAUDE verificado e o próprio ATLETA
    boolean countsForRarity(UserContext ctx);                    // SCOUT ≥ VERIFICADO, STAFF, STAFF_SAUDE
    AudiencePolicy audiencePolicy(UserContext ctx);              // MARCA/APOSTAS: 18+, exclui ATLETA/STAFF/ARBITRO/OFICIAL_LIGA
}
```

Regras-chave a codificar:
1. `LESAO_ALERTA`: autor ∈ {ATLETA (próprio), STAFF_SAUDE verificado}; `AGENTE_IA` só com `operatedBy` ∈ STAFF_SAUDE e revisão humana.
2. `RELATORIO_TATICO` de `SCOUT.COMUNIDADE` e `IMPRENSA` recebe `analysisKind = OPINATIVA` e **não** entra nos 40% do Rarity Engine.
3. `INSTITUCIONAL` só para contas de organização (`CLUBE`, `OFICIAL_LIGA`, `MARCA`, `TORCIDA_ORGANIZADA`) e `STAFF.GESTAO`/`AGENTE` verificados.
4. `MARCA.APOSTAS`: `AudiencePolicy` com `minAge=18`, `mandatoryWarning=true`, `excludedRoles={ATLETA, STAFF, STAFF_SAUDE, ARBITRO, OFICIAL_LIGA}`, `featureFlag=betting.ads.enabled` (desligar se PL 3.563 virar lei).
5. `AGENTE_IA`: toda publicação carrega `aiGenerated=true` → UI exibe rótulo e marca metadados (art. 50(2)/(4)).
6. `TermsAcceptance`: versão de termos **por papel** (agentes de apostas, saúde e IA têm anexos próprios).

### 5.4 API/UI
- `GET /v1/ui/roles` passa a retornar `role`, `ringHex`, `subtypes[]`, `verificationLevels[]` para o SPA montar cadastro polimórfico e legenda de anéis.
- `GET /v1/ui/navigation` por papel: `TORCEDOR` vê PERFIL + ALERTA (DM) por padrão; `STAFF_SAUDE` abre em ALERTA; `SCOUT` em SCOUTING.

### 5.5 Ordem sugerida de implementação
1. Renomear `TECNICO_STAFF → STAFF` + `StaffSpecialty` (baixo risco, alto ganho de clareza).
2. `STAFF_SAUDE` + ajuste de `canOriginateInjuryRecord` (dependência de RF4.1/RF5).
3. `TORCEDOR` (aquisição) e `AGENTE` (mercado).
4. `AGENTE_IA` (transparência) — antes de qualquer agente publicar no feed.
5. Sub-tipos de `CLUBE`, `MARCA` (com política APOSTAS), `IMPRENSA`, `OFICIAL_LIGA`.
6. Registry para `ARBITRO`, `TORCIDA_ORGANIZADA`.
7. Correção de contraste de `CLUBE` (e opcional `MARCA`).

---

## 6. Referências

Páginas abertas e lidas nesta pesquisa (2026-10-24):

1. Bichara e Motta — CBF publica o novo Regulamento Nacional de Agentes de Futebol — https://www.bicharaemotta.com.br/cbf-publica-o-novo-regulamento-nacional-de-agentes-de-futebol-com-impactos-significativos-para-o-mercado-brasileiro/
2. FIFA — Football Agents (legal/football-regulatory) — https://inside.fifa.com/legal/football-regulatory/agents
3. Lei 14.597/2023 (Lei Geral do Esporte), art. 178 "Do Torcedor" (texto consolidado) — https://modeloinicial.com.br/lei/L-14597-2023/torcedor-@___III_II
4. CBF — Certificado de Clube Formador (CCF) e Mecanismo de Solidariedade (apresentação na Câmara, 19/08/2021) — https://www2.camara.leg.br/atividade-legislativa/comissoes/comissoes-temporarias/especiais/56a-legislatura/pl-1153-19-direito-aos-atletas-de-base-lei-pele/apresentacoes-em-eventos/CBF19.08.21.pdf
5. FPF — Regulamento de Licenciamento do Campeonato Paulista (2026) — https://futebolpaulista.com.br/Repositorio/Institucional/Resolucao/3684/3684_639258522594600654.pdf
6. CBF Academy — catálogo de cursos — https://plataforma.cbfacademy.com.br/pt-br/cursos
7. Demarest — CONAR regulamenta a publicidade de apostas esportivas (Anexo X) — https://www.demarest.com.br/conar-regulamenta-a-publicidade-de-apostas-esportivas/
8. Senado Notícias — Projeto que veda propaganda e patrocínio de bets avança no Senado (05/02/2026) — https://www12.senado.leg.br/noticias/materias/2026/02/05/projeto-que-veda-propaganda-e-patrocinio-de-bets-avanca-no-senado
9. Máquina do Esporte — US Open, Wimbledon e Boca Juniors: regras para influenciadores — https://maquinadoesporte.com.br/midia/us-open-wimbledon-e-boca-juniors-por-que-eventos-esportivos-estao-revendo-as-regras-para-influenciadores-no-esporte/
10. Legal 500 — EU AI Act transparency rules take effect 2 August 2026 — https://www.legal500.com/intelligence/united-kingdom/privacy/is-it-a-bot-eu-ai-act-transparency-rules-take-effect-2-august-2026
11. Hudl Wyscout — FAQ — https://www.hudl.com/products/wyscout/faq
12. Sports Agent Institute — Wyscout: An Essential Tool for Football Agents and Professional Scouts — https://fifa.sportsagentinstitute.com/en/blog/wyscout
13. Hudl Instat — FAQs — https://www.hudl.com/products/instat/faqs
14. BeSoccer Pro — About us — https://pro.besoccer.com/aboutus
15. Follow the Money — How the volunteers of Transfermarkt became influential players — https://www.ftm.eu/articles/transfermarkt-volunteers-european-football
16. Wikipedia — Fieldoo — https://en.wikipedia.org/wiki/Fieldoo
17. Fieldoo — site oficial — https://www.fieldoo.com/
18. The Hub — Tonsser — https://thehub.io/startups/tonsser
19. PLAYMAKER.pro — https://www.playmaker.pro/en
20. PlayMakerAI — Who we help — https://www.playmaker.ai/football/who-we-help
21. Footure Pro — https://footure.com.br/footure-pro/

Vistas apenas em resultados de busca (não abertas; confirmar antes de citar formalmente): Tonsser FAQ (https://tonsser.com/faq — 404), Portal de Governança CBF – relação de árbitros (https://portaldegovernanca.cbf.com.br/relacao-arbitros — 503), ABEX – cursos (https://www.abexfutebol.com.br/cursos/ — 503), CBF Academy – Licença A Preparador Físico (https://cbfacademy.com.br/escola-tecnica/licenca-a-preparador-fisico/), CBF Intermediários (https://intermediario.cbf.com.br/ — 503), Planalto – Lei 14.597/2023 (https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2023/lei/l14597.htm — 503), Palmeiras – Informações à imprensa (https://www.palmeiras.com.br/imprensa/), Soccerway – CBF planeja árbitros profissionais em 2027 (https://br.soccerway.com/noticias/futebol-brasileirao-betano-cbf-busca-novos-talentos-e-planeja-arbitros-profissionais-no-brasil-em-2027/06B8I6rA).
