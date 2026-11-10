# Clubes, Managers, mercado e busca global

## Uso na SPA

- **Buscar** (`#/buscar`): catálogo global; selecione Clubes, Jogadores ou Managers, adicione filtros e compartilhe a URL. Campos de uma entidade específica restringem os resultados àquela entidade. Valores de um mesmo filtro são alternativas; filtros diferentes se combinam. Intervalos usam dois filtros, mínimo e máximo. A consulta, a contagem e a paginação são feitas na API.
- **Clubes** e **Managers** no menu abrem os respectivos catálogos. Os resultados levam aos perfis. Existem atalhos para jogadores e Managers sem clube.
- **Meu clube** (`#/clube`): o titular edita identidade, equipes, comissão, competições, instalações e dados financeiros; seleciona registros para edição e adiciona snapshots para preservar outras temporadas/datas.
- **Meu perfil esportivo e mercado** (`#/mercado`): o jogador declara vínculo externo ou ausência de clube, situação contratual, situação de transferência e atributos esportivos. Convites para elenco são aceitos ou recusados nesta tela.
- **Meu perfil de Manager** (`#/manager`): o titular edita função, nacionalidades declaradas, idiomas, licença, formação, modelo de jogo, disponibilidade de clube e situação de transferência; pode cadastrar títulos declarados. Histórico e clube atual vêm das nomeações que o clube cadastra para o perfil de Manager.
- **Vitrine**: além do painel e Copilot existentes, escolha clube e equipe para calcular “Por que devo contratar este jogador?”. A análise exige atleta adulto, perfil visível e consentimento de processamento por IA.

## Modelo e significado dos dados

`Club` mantém a identidade institucional. As relações `ClubTeam`, `ClubManagerTenure`, `ClubCompetitionEntry`, `ClubFinanceSnapshot`, `ClubVenue` e `ClubMembership` armazenam equipes, mandatos, participações/classificações, finanças, estádios/instalações e vínculos de jogadores. Os registros têm fonte, data de criação/alteração e versão de concorrência.

O Manager é um usuário `MANAGER`, distinto de `STAFF` e de agente de jogadores. Suas funções são treinador principal, gestor esportivo ou assistente. `hasCurrentClub` tem três estados: `false` = sem clube declarado; `true` = vínculo declarado; `null` = não informado. Um mandato vigente prevalece sobre a declaração de sem clube. Contrato até (`contractUntil`) é diferente do término efetivo do mandato (`endDate`). Clube mais frequente é mostrado por passagens e por dias de atuação (com empates e sem contar períodos simultâneos duas vezes); não há contagem oficial de partidas por manager.

A classificação da liga é contextualizada por equipe, competição, temporada, fase/grupo e data. Não equivale ao ranking de raridade da plataforma. Em copas, fase e situação podem ser informadas sem inventar uma colocação numérica. Registre uma nova linha para preservar outra temporada ou outra data de classificação.

Folha salarial, valor estimado do elenco, orçamento e taxa de contratação são métricas distintas; sempre registre moeda, referência temporal e fonte. Finanças e taxas não são divulgadas por padrão. Não há importação automática de classificações, salários ou preços de mercado.

### Elenco e mercado

O campo `Athlete.currentClubId` é atualizado apenas por confirmação do convite do clube. O cadastro não aceita vincular um ID de clube arbitrariamente. Aceitar outro convite encerra o vínculo anterior; encerrar o vínculo atual remove o jogador do elenco e preserva o histórico. Vínculos com data final ultrapassada são reconciliados automaticamente a cada minuto. A descoberta e os elencos respeitam a visibilidade dos jogadores e o consentimento do responsável para menores.

`affiliationStatus` distingue `UNKNOWN`, `ATTACHED` e `WITHOUT_CLUB`; nome de clube externo e último clube têm sentidos diferentes. Um ID local ausente não basta para afirmar que o jogador está sem clube. O perfil tem situação contratual e declaração de transferência sem taxa separadas.

Jogadores e Managers têm `transferSituation`:

| Valor da API | Interface |
|---|---|
| `TRANSFER_LIST` | Na lista de transferências |
| `NOT_FOR_SALE` | Não está à venda |
| `LOAN_LIST` | Na lista de empréstimos |
| `SWAP_LIST` | Na lista de trocas |
| `FREE_AGENT` | Agente livre |

`FREE_AGENT` exige declaração de sem clube e ausência de vínculo ativo. No jogador, exige também `AGENTE_LIVRE` na situação contratual. Ser agente livre não garante ausência de comissão, compensação de formação, custo salarial ou restrição de registro.

### Score de afinidade

Versão `club-fit-v1`: posição procurada 35%, categoria 20%, modelo de jogo 20%, idiomas 10%, pé preferido 10%, país do perfil 2%, estado 1% e cidade 2%. Critérios informados têm correspondência 0 ou 100; um item de uma lista basta para corresponder. O score é a média ponderada somente dos critérios avaliáveis. A cobertura mostra o percentual do modelo que tinha dados disponíveis; abaixo de 50% retorna `score: null` e `INSUFFICIENT_DATA`. Um score válido zero é diferente de dado ausente.

O score sempre identifica clube/equipe, fatores, pesos, cobertura, data e versão. O painel recalcula uma avaliação já solicitada nos ciclos de atualização da Vitrine. País do perfil não é tratado como nacionalidade; dados médicos, credenciais, aparência da interface e finanças privadas não entram na fórmula. É um auxílio contextual à avaliação humana.

## API

Todas as rotas abaixo exigem autenticação. Perfis sem acesso retornam 404; alterações exigem o titular da conta correta.

| Rota | Uso |
|---|---|
| `GET /v1/discovery/fields` | Catálogo dos campos e operadores permitidos |
| `GET /v1/discovery?entity=ALL&q=...&filter=...&page=0&size=20&order=name` | Busca global paginada |
| `GET /v1/clubs/{id}` / `PUT /v1/clubs/{id}` | Perfil e identidade do clube |
| `POST /v1/clubs/{id}/{teams,managers,competitions,finances,venues}` | Adicionar registros |
| `PUT /v1/clubs/{id}/{recurso}/{registro}` | Editar registro do próprio clube |
| `POST /v1/clubs/{id}/memberships` | Convidar jogador para a equipe |
| `GET /v1/me/club-invitations` | Convites pendentes do jogador |
| `POST /v1/me/club-invitations/{id}/{accept,reject}` | Responder convite |
| `POST /v1/clubs/{id}/memberships/{registro}/end` | Encerrar vínculo pelo clube ou jogador |
| `GET /v1/me/market` / `PUT /v1/me/market` | Perfil esportivo e mercado do próprio jogador |
| `GET /v1/managers/{id}` / `PUT /v1/managers/{id}` | Perfil e edição de Manager |
| `POST /v1/managers/{id}/titles` / `DELETE /v1/managers/{id}/titles/{titulo}` | Títulos declarados pelo Manager |
| `GET /v1/athletes/{id}/vitrine/compatibility?clubId=...&teamId=...` | Afinidade contextual |

Filtros são parâmetros repetidos `filter=campo:operador:valor`, codificados como query string. `eq` aceita vírgulas; operadores disponíveis dependem do campo: `eq`, `contains`, `gte`, `lte`, `isnull`. Exemplo:

```text
/v1/discovery?entity=ATLETA&filter=athlete.affiliationStatus:eq:WITHOUT_CLUB&filter=athlete.transferSituation:eq:FREE_AGENT
/v1/discovery?entity=MANAGER&filter=manager.hasCurrentClub:eq:false
/v1/discovery?entity=CLUBE&filter=competition.season:eq:2026&filter=competition.position:lte:5
```

Filtros de uma mesma relação precisam corresponder ao mesmo registro; não cruzam uma posição de 2025 com uma temporada de 2026. Dados privados não podem ser descobertos por contagens, filtros ou ordenação. Campos de autenticação e de saúde não fazem parte do catálogo.

## Migrações e validação

Flyway V8 adiciona os perfis relacionais e o mercado do jogador; V9 adiciona usuário Manager e títulos. Ambas são aditivas, preservando as migrações anteriores. Inicie com o perfil `local` e MySQL/Redis como no README.

```bash
mvn -B verify
python3 scripts/smoke_club_market.py --base http://localhost:8080 --session-file /tmp/club-session.json
node scripts/smoke_club_market_browser.cjs /tmp/club-session.json
```

No ambiente cloud preparado, `/workspace/.setup/mvn` configura o Maven e o proxy da sessão. Os smokes criam contas de desenvolvimento com prefixo aleatório `smoke_`; use banco de validação descartável, não produção. O arquivo opcional de sessão tem permissão 0600, contém tokens temporários e deve ser removido após o teste. O teste de navegador requer Playwright e Chromium (caminho `/usr/bin/chromium`). Screenshots são escritos em `/tmp`; não publique o arquivo de sessão nem seus valores.
