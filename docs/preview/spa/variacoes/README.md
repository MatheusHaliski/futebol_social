# Variações · aparência da navegação e do layout, backgrounds dos cards e arte × foto

Prints capturados na SPA ligada à API real (perfil `local`, MySQL + Redis + S3 compatível, conta semente `atleta2@exemplo.dev`).
Tudo foi aplicado pelos controles reais (**Central de Privacidade → Aparência**, criador de cards, **Conta → Foto do perfil**) e persistido
em `PATCH /v1/me` / `POST /v1/posts`. Script: [`scripts/variations.js`](scripts/variations.js) · medições: [`report.json`](report.json).

## L · entrada

| Print | O que mostra |
|---|---|
| `L01_entrar_sem_cadastro_sobreposto.jpg` | modal com fundo de painel e só o formulário de login (antes, `display:grid` vencia o `hidden` e login + cadastro apareciam juntos) |

## V · material, gradiente, fonte, densidade e contraste

| Print | Tema · destaque · tom | Material da navegação | Gradiente do layout | Fonte | Densidade · contraste |
|---|---|---|---|---|---|
| `V01_noite_de_jogo_vidro_holofote.jpg` | escuro · verde · grafite | vidro | holofote | padrão | confortável |
| `V02_gramado_placar_arquibancada.jpg` | escuro · verde · floresta | gramado | arquibancada | placar | confortável |
| `V03_prancheta_clara_editorial.jpg` | claro · azul · areia | fosco | nenhum | editorial | espaçosa |
| `V04_alta_legibilidade_contraste_alto.jpg` | escuro · ouro · carvão | fosco | nenhum | legível | espaçosa · **alto** |
| `V05_carbono_ciano_crepusculo_compacta.jpg` | escuro · ciano · carvão | carbono | crepúsculo | placar | compacta |
| `V06_metalico_prata_vinho_editorial.jpg` | escuro · prata · vinho | metálico | crepúsculo | editorial | confortável |
| `V07_claro_marinho_vidro_legivel.jpg` | claro · azul · marinho claro | vidro | holofote | legível | confortável |
| `V08_claro_floresta_gramado_espacosa.jpg` | claro · verde · floresta clara | gramado | arquibancada | placar | espaçosa |
| `V09_sistema_ouro_carvao_carbono_fundo.jpg` | escuro · ouro · carvão + fundo ouro | carbono | holofote | sistema | confortável |

V01–V05 são as **combinações prontas** (um toque aplica os oito eixos). As fontes são auto-hospedadas (`/fonts`, SIL OFL 1.1) — nenhuma
requisição a terceiros; a CSP passou a `font-src 'self'`.

## P · o painel Aparência

| Print | O que mostra |
|---|---|
| `P01_painel_combinacoes_e_previa.jpg` | combinações prontas, prévia viva (barras + card com arte), contraste alto |
| `P02_painel_material_gradiente.jpg` | amostras de material (sobre fundo listrado, mostra transparência) e de gradiente |
| `P03_painel_fonte_densidade.jpg` | amostra de cada fonte com "Gol de placa 1×0 · Il1 0O" e densidade segmentada |
| `P04_painel_previa_viva.jpg` | prévia depois de escolher vidro · crepúsculo · placar — salvo na conta (`report.json → afterReload`) |

## C · backgrounds dos cards (20 combinações)

`C01_backgrounds_cards_escuro.jpg`, `C02_backgrounds_cards_claro.jpg`, `C03_backgrounds_cards_contraste_alto.jpg` — o mesmo card com 20
combinações de gradiente (5) · material (10) · padronagem (13) · layout (4) · cor e segunda cor. A tinta tem **luminância travada** em
OKLCH, então título e teaser ficam legíveis com qualquer cor (verificado por `ArtContrastTest`); a borda esquerda continua sempre com a
cor do tipo (PARTIDA = azul).

## W · criador de cards, etapa Arte

| Print | O que mostra |
|---|---|
| `W01_arte_paleta_rubro_negro_faixa.jpg` | paleta de torcida Rubro-negro + faixa diagonal + cromo |
| `W02_arte_radial_prisma_rede_miniaturas.jpg` | opções com **miniatura da camada** na cor escolhida; segunda cor aparece só para gradiente/padronagem bicolor |
| `W03_arte_sobria_alerta_lesao.jpg` | card de lesão: só Suave/Sólido · Fosco/Malha · Grafite, paletas escondidas e aviso (`report.json → soberOptions`) |

## F · fotografia × arte de fundo

| Print | O que mostra |
|---|---|
| `F01_arte_forte_com_foto_previa.jpg` | prévia com a arte mais forte (radial · holográfico · vitral · canarinho) e uma fotografia |
| `F02_card_publicado_foto_intacta.jpg` | card publicado: a foto fica numa moldura neutra, acima de todas as camadas |
| `F03_postview_foto_inteira.jpg` | PostView mostra a foto inteira (`object-fit: contain`) |

Medição (`report.json → photoPixels`): a mesma foto sob radial · holográfico · vitral e sob uma arte neutra difere em média
**0,058/255** por canal (máximo 37 só no antialias da borda arredondada); `filter: none`, `mix-blend-mode: normal`, `opacity: 1`,
moldura com `z-index: 1` e `isolation: isolate`.

## K · foto do perfil

| Print | O que mostra |
|---|---|
| `K01_conta_foto_do_perfil.jpg` | Central → Conta → Foto do perfil (upload próprio; `avatarKey` só aceita mídia da própria conta) |
| `K02_card_perfil_com_foto.jpg` | o card de perfil passa a mostrar a foto no hero; a aura do tier fica **atrás** do conteúdo |

## M · mobile

`M01_mobile_gramado_placar.jpg` e `M02_mobile_vidro_holofote.jpg` — barra superior e barra inferior no material escolhido.
