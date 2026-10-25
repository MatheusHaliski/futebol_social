# Galeria · variações de aparência, estúdio de edição e fotografia

Prints capturados na SPA ligada à API real (`atleta2@exemplo.dev`, `torcedor@exemplo.dev` na seção G, perfil `local`, MySQL + Redis + S3 compatível).
Todas as variações foram aplicadas pelos controles reais de **Central de Privacidade → Aparência** e persistidas em `PATCH /v1/me`.

## A · tema, cor de destaque e tom das barras laterais/painéis

| Print | Tema | Destaque (`uiAccent`) | Tom (`uiSurface`) | Tela |
|---|---|---|---|---|
| `A01_padrao_grafite_verde.jpg` | escuro | verde campo | grafite (padrão) | Gêmeo · Telemetria |
| `A02_claro_azul_grafite.jpg` | **claro** | azul | grafite | Perfil · Feed |
| `A03_escuro_floresta_ouro.jpg` | escuro | ouro | floresta | Alerta |
| `A04_escuro_marinho_roxo.jpg` | escuro | roxo | marinho | Scouting |
| `A05_escuro_ameixa_laranja.jpg` | escuro | laranja | ameixa | Gêmeo |
| `A06_areia_verde.jpg` | escuro + **areia (clara)** | verde | areia | Perfil · Ranking |

O destaque muda barras ativas, botões e brilhos; as cores de identidade dos papéis e as bordas de conteúdo nunca mudam.
Os cards **não** seguem a cor da interface: cada um carrega a arte de fundo escolhida no estúdio (cor · material · layout · estilo).

## B · telas de fundo (`wallpaper`) e persistência

| Print | Fundo | Combinação | Tela |
|---|---|---|---|
| `B01_fundo_verde.jpg` | verde | grafite · verde | Perfil |
| `B02_fundo_ciano_marinho_azul.jpg` | ciano | marinho · azul | Gêmeo |
| `B03_fundo_ouro_flair.jpg` | ouro | grafite · ouro | FLAIR |
| `B04_fundo_verde_claro_floresta.jpg` | verde-claro | floresta · verde | Alerta |
| `B05_fundo_verde_vivo.jpg` | verde-vivo | grafite · verde | Scouting |
| `B06_persistido_apos_reload.jpg` | ciano | marinho · roxo, **após `reload`** — preferência veio do servidor | Gêmeo |
| `C01_painel_aparencia.jpg` | — | o painel: modo escuro, fonte, 5 destaques, 5 tons, 5 fundos + arquivo próprio | Aparência |

Com fundo ativo, topbar, sidebar, painéis, KPIs e a arena FLAIR ficam translúcidos (blur) sobre um véu de 46 % da cor base, para manter o texto legível.

## D · estúdio de edição do próprio card (`PATCH /v1/posts/{id}`, só o autor)

| Print | O que mostra |
|---|---|
| `D00_meus_cards_antes.jpg` | Meus cards antes: metálico·hero·ouro (com foto) e carbono·hero·vitral |
| `D01_estudio_edicao.jpg` | "Editar card · estúdio" aberto a partir do botão ✎ do card |
| `D02_estudio_arte.jpg` | etapa Arte: holográfico · minimal · neon · `#2dd4bf` |
| `D02b_estudio_revisao.jpg` / `D04_estudio_revisao_fonte_patch.jpg` | revisão (fonte `PATCH /v1/posts/{id}`) |
| `D03_meus_cards_depois.jpg` / `D05_meus_cards_apos_salvar.jpg` | Meus cards depois: três cards, três artes distintas |

## E · fotografia (upload do usuário) × arte de fundo

| Print | O que mostra |
|---|---|
| `E01_conteudo_fotografia.jpg` | etapa Conteúdo com o campo de fotografia (`POST /v1/media`) |
| `E02_arte_fundo_com_foto.jpg` | etapa Arte: a cor tinge o **fundo do card**, a foto fica intacta |
| `E02b_revisao_com_foto.jpg` | revisão: fotografia enviada + arte vidro · hero · retrô |
| `E03_publicado_com_foto.jpg` | card publicado no feed Perfil · Momentos |
| `E04_postview_com_foto.jpg` | PostView do card com foto (texto completo só aqui) |

## F · mobile

| Print | O que mostra |
|---|---|
| `F01_mobile_fundo_ouro.jpg` | Gêmeo em 390 px com fundo ouro, KPIs e feed translúcidos |
| `F02_mobile_perfil_fundo_ouro.jpg` | Perfil em 390 px com fundo ouro |

## G · camada social (seguir · comentários · notificações)

| Print | O que mostra |
|---|---|
| `G01_cards_seguir.jpg` | cards de atleta com **＋ Seguir / ✓ Seguindo** (torcedor já segue o atleta 2 no seed; o próprio perfil não mostra o botão) |
| `G02_aba_seguindo.jpg` | aba **Seguindo** do Gêmeo: só cards de quem o usuário segue (`sort=FOLLOWING`) |
| `G03_postview_comentarios.jpg` | PostView com o fio de comentários (resposta indentada), botão Seguir o autor e formulário (1–500 caracteres) |
| `G04_comentario_publicado.jpg` | comentário publicado: lista e contador "Comentar N" do card atualizados |
| `G05_notificacoes.jpg` | sino do dono do card: COMMENT, TIER_CHANGE e FOLLOW, não lidas em destaque, "Marcar todas como lidas" |
| `G06_notificacao_abre_card.jpg` | clicar na notificação marca como lida e abre o card referenciado |
| `G07_mobile_contador_alerta.jpg` | mobile: contador de não lidas na aba Alerta da barra inferior |

Regenerar: `cd docs/preview/spa/gallery/scripts && BASE=http://localhost:8080/ OUT=./out node gallery.js` (Playwright + Chromium; API no ar com a conta semente). `foto_golaco.jpg` é a fotografia ilustrativa usada no upload (gerada por código, sem pessoas reais). As seções D e E alteram dados locais.
