# Força do clube e "Simular contratação"

## Score por setor e score global

Cada clube tem um **score global** (0–100) e um score por setor: **Goleiro**, **Defesa**, **Meio-campo** e **Ataque**.
Endpoint: `GET /v1/clubs/{id}/strength` (mesma visibilidade do perfil do clube). Aparece na página do clube e na simulação da Vitrine.

1. **Elenco**: atletas com vínculo atual no clube, perfil público e conta ativa (como no ranking de clubes). Menores sem consentimento do responsável ficam de fora.
2. **Nota do jogador no setor** (0–100), com o scouting mais recente:

| Setor | Posições | Pesos |
|---|---|---|
| Goleiro | Goleiro | posicionamento 50 · passe 20 · velocidade 15 · resistência 15 |
| Defesa | Zagueiro, Lateral | desarme 40 · posicionamento 30 · velocidade 15 · passe 15 |
| Meio-campo | Volante, Meia | passe 35 · drible 20 · posicionamento 20 · resistência 15 · desarme 10 |
| Ataque | Ponta, Atacante | chute 40 · drible 25 · velocidade 25 · passe 10 |

3. **Melhor onze**: entre 4-3-3, 4-4-2, 4-2-3-1, 3-5-2, 3-4-3, 5-3-2 e 5-4-1, a formação com o maior score global. Empate fica com a primeira da lista.
4. **Score do setor** = soma das notas dos titulares ÷ titulares de referência (goleiro 1 · defesa 4 · meio 3 · ataque 3), limitado a 100.
   Vaga sem jogador avaliado conta 0. Mais titulares que a referência deixam o setor mais forte; menos, mais fraco.
5. **Score global** = goleiro 10 % · defesa 30 % · meio-campo 30 % · ataque 30 %.

Limitação: o scouting não mede reflexo nem saída do gol, então a nota de goleiro é aproximada.

## Simular contratação (Vitrine → aba Contratação)

`GET /v1/athletes/{id}/vitrine/simulate?clubId=` recalcula o melhor onze com o atleta no elenco e devolve o clube antes e depois,
a variação de cada setor com o motivo, e as listas **Aumenta** e **Diminui**.

- Um setor **aumenta** quando o atleta entra como titular no lugar de alguém mais fraco ou ocupa uma vaga vazia.
- Um setor **diminui** quando o melhor onze muda de formação para encaixar o atleta. Exemplo: um atacante forte leva o 4-4-2 para 3-4-3; o ataque ganha um titular e a defesa perde um.
- Se o atleta não entra no onze, nada muda e a resposta diz que ele seria reserva.

Regras: mesma visibilidade do perfil do atleta (404); análise automatizada para contratação segue a regra do Copilot (atleta adulto que autorizou processamento por IA:
`AI_PROCESSING_OPT_IN`, `ATHLETE_AI_OPT_OUT`, `COPILOT_MINOR`); atleta já do elenco responde `409 ALREADY_IN_SQUAD`; sem posição ou scouting, `status: INSUFFICIENT_DATA`.
Não usa dados de saúde, salário nem entrosamento. Na Vitrine, a simulação é refeita quando os dados do atleta mudam.

O seed cria 10 jogadores de apoio por clube de exemplo (`<PREENCHER_ELENCO_*>`) para os scores fazerem sentido.
