# Cards e navegação para avaliação profissional

A descoberta usa a função esportiva, o vínculo e a evidência disponível como hierarquia principal. Popularidade, raridade e ações sociais não são indicadores de capacidade profissional. O novo card não deriva curtidas ou notas a partir de scores.

## O que aparece primeiro

| Perfil | Card compacto | Expansão e perfil completo |
|---|---|---|
| Jogador | Posição, clube/situação de vínculo, disponibilidade no mercado, categoria, idade/categoria etária, pé dominante; três métricas da temporada | Temporada, partidas e origem oficial/autorrelatada, atributos; Vitrine com abas Visão geral, Desempenho e Contratação |
| Clube | Identidade, localização, treinador informado, jogadores visíveis, equipes e vagas declaradas; competição e data de referência | Equipes, elenco visível e histórico de competições; perfil com abas Visão geral, Elenco e equipes, Competições, Comissão, Estrutura e Finanças |
| Manager | Função, clube/vínculo, disponibilidade, modelo de jogo e contrato quando informado; títulos declarados, clubes no histórico visível e mandatos vigentes | Perfil técnico, carreira e títulos; perfil com abas Visão geral, Carreira, Títulos e Perfil técnico e edição |

Para goleiros, o resumo esportivo prioriza jogos sem sofrer gol; para zagueiros, laterais e volantes, desarmes; para outras posições, gols. Todos trazem minutos e volume de partidas para contextualizar a comparação. Outras métricas ficam na expansão e na Vitrine.

Sem partidas com minutos registrados, o card mostra ausência de indicadores; não inventa uma nota e não apresenta ausência de dados como produção zero. Um zero registrado numa amostra real permanece zero. O período e a origem dos dados acompanham os números. Vagas e vínculos não informados não são inferidos.

A classificação de liga/copa é sempre acompanhada de competição, temporada, fase quando informada e data de referência. Ela não é o ranking de raridade da plataforma. Informações antigas continuam identificadas pela data e não ganham um selo de atualização automática.

## Compacto, detalhado e expandido

- **Compactos**: identidade, vínculo/mercado, atributos prioritários e até três indicadores.
- **Com detalhes**: adiciona os demais fatos disponíveis do perfil na mesma grade, sem mais requisições por card. A densidade é lembrada localmente.
- **Expandir ficha profissional**: carrega, sob demanda, seções específicas do perfil e suas abas. Não remove o usuário do feed. O botão principal leva à Vitrine ou ao perfil completo.
- **Comparar**: permite selecionar dois jogadores, mostra a seleção numa barra e abre o comparador existente. A seleção é local à sessão, não é uma recomendação de contratação.

As abas usam botões, estados ARIA, painéis identificados e navegação por setas, Home e End. As abas dos perfis e da Vitrine preservam o parâmetro `tab` na URL. Em telas pequenas, a navegação de segmentos pode rolar horizontalmente dentro da própria barra; o conteúdo da página não exige rolagem horizontal.

## Feed segmentado

A aba Descoberta oferece:

- **Panorama**: fichas profissionais e uma seção separada de publicações.
- **Jogadores**: descoberta com filtros esportivos.
- **Mercado**: transferências, empréstimos, trocas e agentes livres; atalhos preservam o filtro na URL.
- **Clubes**: contexto competitivo e oportunidades declaradas por equipe.
- **Managers**: carreira, função e disponibilidade.
- **Publicações**: posts sociais, relatórios, partidas e atualizações, sem misturar a listagem de perfis.

Ranking, Explorar e Momentos continuam disponíveis. Os segmentos de perfis e a busca global usam o mesmo catálogo autorizado de campos e a mesma consulta. Filtros avançados ficam recolhidos; os critérios ativos permanecem visíveis acima dos resultados.

## Projeção de dados e privacidade

`GET /v1/discovery/cards?ids=id1,id2,...` aceita até 60 IDs, elimina duplicatas e preserva a ordem pedida. Cada perfil passa novamente pela política de visibilidade. O serviço busca partidas, mandatos, competições, equipes e títulos em lotes, sem disparar uma requisição HTTP por card compacto.

A projeção não contém credenciais, dados médicos, prontidão, folha salarial ou taxas privadas. O número de jogadores se refere apenas aos perfis visíveis ao leitor. Uma conta verificada não transforma os títulos declarados ou os registros de partidas em uma verificação independente. A fonte e a amostra são apresentadas separadamente.

A expansão reutiliza os endpoints de perfil já protegidos. A Vitrine mantém seus consentimentos e seu score contextual de compatibilidade; a reorganização em abas não altera essas regras.

## Verificação

```bash
mvn -B verify
python3 scripts/smoke_club_market.py --base http://localhost:8080 --session-file /tmp/professional-session.json
node scripts/smoke_club_market_browser.cjs /tmp/professional-session.json
node scripts/smoke_professional_cards_browser.cjs /tmp/professional-session.json
```

Os smokes criam contas de teste e devem rodar em banco de desenvolvimento descartável. Os testes do navegador precisam de Playwright e Chromium. Remova o arquivo temporário de sessões após o uso; ele contém tokens e não deve ser versionado. Capturas de tela ficam em `/tmp`.
