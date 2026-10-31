# Sprint 0 · defeitos P0 corrigidos

Prints da SPA ligada à API real (perfil `local`, seed), com o atleta 1 depois de revogar `shareMedicalData`.
Script: [`scripts/sprint0.js`](scripts/sprint0.js) · resultados: [`report.json`](report.json). Itens e arquivos de origem em
[`docs/estudos/ESTUDO_ABAS_2026-10.md`](../../../estudos/ESTUDO_ABAS_2026-10.md) §2.

| Print | P0 | O que mostra |
|---|---|---|
| `S01_gemeo_de_menor_nao_encontrado.jpg` | #2 · #4 · #5 | torcedor abre a evolução do Gêmeo de um atleta de 17 anos sem consentimento do responsável: estado "Não encontrado" (404), sem gráfico fictício |
| `S02_carga_privada_sem_acesso.jpg` | #2 | carga e recuperação de quem não compartilha saúde: "Sem acesso" (403 `MEDICAL_DATA_PRIVATE`) com atalho para a Central e "Tentar de novo" — antes aparecia "prontidão 78 % · PRONTO PRA JOGO" inventado com o selo "API" |
| `S03_copilot_sem_consentimento.jpg` | #7 | Copilot sem "Processamento por agentes de IA": 403 `AI_PROCESSING_OPT_IN`, nenhuma sugestão fictícia e "Aceitar" desabilitado |
| `S04_flair_odds_reais.jpg` | #6 | odds exibidas = odds do sorteio. No seed só há LEGENDARY e MYTHIC no pool público, então aparece 75 % / 25 % (antes: 55/25/12/6/2 com o sorteio caindo no pool inteiro) |
| `S05_boletim_com_atleta_vinculado.jpg` | #3 | médico verificado escolhe o atleta do boletim de lesão; o texto explica quem poderá ler |
| `S06_menor_categoria_no_card.jpg` | #5 | o atleta de 17 anos vê "SUB-20" no próprio card — a idade exata não é exibida |
| `S07_menor_publico_exige_responsavel.jpg` | #5 | tentar ficar "Público" responde 403 `GUARDIAN_CONSENT_REQUIRED` |

Verificações pela API (mesma sessão):

- **#1 rate limit:** 13 logins com `X-Forwarded-For` diferente a cada vez → `401 ×10` e depois `429`, num único bucket (`127.0.0.1`). A causa
  raiz incluía `server.forward-headers-strategy: framework`, que aplicava `X-Forwarded-*` de qualquer cliente antes do filtro.
- **#3 boletim de lesão:** com o consentimento revogado, o torcedor recebe 404 no PostView, nos comentários, nas reações e no feed; o
  próprio atleta e o médico verificado continuam vendo.
