// Galeria de aparência (controles reais do painel → PATCH /v1/me), estúdio de edição, fotografia e mobile.
// Uso: BASE=http://localhost:8080/ OUT=./out node gallery.js   (exige API no ar com a conta semente atleta2@exemplo.dev)
// Atenção: as seções D e E alteram dados — editam a arte do card "Hat-trick" e publicam um card com fotografia.
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
const base = process.env.BASE || 'http://localhost:8080/';
const out = (process.env.OUT || path.join(__dirname, 'out')) + path.sep; fs.mkdirSync(out, { recursive: true });
const PHOTO = process.env.PHOTO || path.join(__dirname, 'foto_golaco.jpg');
const ACCOUNT = process.env.ACCOUNT || 'atleta2@exemplo.dev'; const PASSWORD = process.env.PASSWORD || 'Senha@Forte123';
const errs = [];
const step = async (label, fn) => { try { await fn(); } catch (e) { const m = label + ': ' + String(e.message).split('\n')[0]; errs.push(m); console.log('FAIL', m); } };
async function settle(p, ms = 500) {
  await p.waitForTimeout(ms);
  await p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  try { await p.waitForFunction(() => [...document.images].every(i => i.complete), null, { timeout: 4000 }); } catch {}
}
async function shot(p, name) {
  await settle(p); await p.screenshot({ path: out + name + '.png' });
  const ds = await p.evaluate(() => ({ theme: document.documentElement.dataset.theme, accent: document.documentElement.dataset.accent, surface: document.documentElement.dataset.surface, wallpaper: document.documentElement.dataset.wallpaper }));
  console.log(name, JSON.stringify(ds));
}
async function login(p) {
  await p.goto(base + '#/perfil'); await p.waitForTimeout(800);
  await p.fill('#loginForm input[name=email]', ACCOUNT); await p.fill('#loginForm input[name=password]', PASSWORD);
  await p.click('#loginForm button[type=submit]'); await p.waitForTimeout(1500);
}
async function pick(p, ap) {
  await step('abrir painel', async () => { await p.click('#btnLgpd'); await p.waitForTimeout(400); await p.click('.lgpd-nav [data-lgpd=aparencia]'); await p.waitForTimeout(250); });
  await step('tema', async () => { const dark = await p.isChecked('#darkToggle'); if ((ap.theme === 'light') === dark) { await p.click('#darkToggle', { force: true }); await p.waitForTimeout(350); } });
  await step('accent ' + ap.accent, async () => { await p.click(`.ap-swatch[data-ap=accent][data-val=${ap.accent}]`, { force: true }); await p.waitForTimeout(300); });
  await step('surface ' + ap.surface, async () => { await p.click(`.ap-swatch[data-ap=surface][data-val=${ap.surface}]`, { force: true }); await p.waitForTimeout(300); });
  await step('wallpaper ' + ap.wallpaper, async () => { const w = p.locator(`.ap-wall[data-ap=wallpaper][data-val=${ap.wallpaper}]`); await w.scrollIntoViewIfNeeded(); await w.click({ force: true }); await p.waitForTimeout(400); });
  await step('fechar painel', async () => { await p.click('[data-close=lgpdModal]'); await p.waitForTimeout(300); });
}
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 1440, height: 980 }, deviceScaleFactor: 1.25 }); const p = await ctx.newPage();
  p.on('pageerror', e => errs.push('pageerror: ' + e.message));
  await login(p);

  /* A/B — variações de aparência: tema, destaque, tom das barras e telas de fundo */
  const combos = [
    ['A02_claro_azul_grafite', { theme:'light', accent:'BLUE', surface:'GRAPHITE', wallpaper:'NONE' }, '#/perfil'],
    ['A03_escuro_floresta_ouro', { theme:'dark', accent:'GOLD', surface:'FOREST', wallpaper:'NONE' }, '#/alerta'],
    ['A04_escuro_marinho_roxo', { theme:'dark', accent:'PURPLE', surface:'NAVY', wallpaper:'NONE' }, '#/scouting'],
    ['A05_escuro_ameixa_laranja', { theme:'dark', accent:'ORANGE', surface:'PLUM', wallpaper:'NONE' }, '#/gemeo'],
    ['A06_areia_verde', { theme:'dark', accent:'GREEN', surface:'SAND', wallpaper:'NONE' }, '#/perfil/ranking'],
    ['B01_fundo_verde', { theme:'dark', accent:'GREEN', surface:'GRAPHITE', wallpaper:'VERDE' }, '#/perfil'],
    ['B02_fundo_ciano_marinho_azul', { theme:'dark', accent:'BLUE', surface:'NAVY', wallpaper:'CIANO' }, '#/gemeo'],
    ['B03_fundo_ouro_flair', { theme:'dark', accent:'GOLD', surface:'GRAPHITE', wallpaper:'OURO' }, '#/flair'],
    ['B04_fundo_verde_claro_floresta', { theme:'dark', accent:'GREEN', surface:'FOREST', wallpaper:'VERDE_CLARO' }, '#/alerta'],
    ['B05_fundo_verde_vivo', { theme:'dark', accent:'GREEN', surface:'GRAPHITE', wallpaper:'VERDE_VIVO' }, '#/scouting'],
    ['A01_padrao_grafite_verde', { theme:'dark', accent:'GREEN', surface:'GRAPHITE', wallpaper:'NONE' }, '#/gemeo'],
  ];
  for (const [name, ap, hash] of combos) { await p.goto(base + hash); await p.waitForTimeout(700); await pick(p, ap); await shot(p, name); }

  /* C — o próprio painel de aparência */
  await p.goto(base + '#/gemeo'); await p.waitForTimeout(500);
  await step('painel C01', async () => { await p.click('#btnLgpd'); await p.waitForTimeout(400); await p.click('.lgpd-nav [data-lgpd=aparencia]'); await p.waitForTimeout(300); await p.screenshot({ path: out + 'C01_painel_aparencia.png' }); await p.click('[data-close=lgpdModal]'); });

  /* B06 — persistência server-side: recarrega e confere que veio da API */
  await pick(p, { theme:'dark', accent:'PURPLE', surface:'NAVY', wallpaper:'CIANO' }); await p.reload(); await p.waitForTimeout(1500);
  await shot(p, 'B06_persistido_apos_reload');
  await pick(p, { theme:'dark', accent:'GREEN', surface:'GRAPHITE', wallpaper:'NONE' });

  /* D — estúdio de edição do próprio card (Hat-trick): a arte de fundo muda; Meus cards antes/depois */
  await p.goto(base + '#/cards'); await p.reload(); await p.waitForTimeout(1200); await shot(p, 'D00_meus_cards_antes');
  await step('estúdio de edição', async () => {
    const card = p.locator('#miscPanel .pcard', { hasText: 'Hat-trick' });
    await card.locator('.act[data-act=edit]').click(); await p.waitForTimeout(1000); await p.screenshot({ path: out + 'D01_estudio_edicao.png' });
    await p.click('#wzNext'); await p.waitForTimeout(300);
    await p.click('.optchip[data-key=material][data-val=HOLOGRAFICO]'); await p.click('.optchip[data-key=layout][data-val=MINIMAL]'); await p.click('.optchip[data-key=style][data-val=NEON]'); await p.click('.swatch[data-color="#2dd4bf"]'); await p.waitForTimeout(400);
    await p.screenshot({ path: out + 'D02_estudio_arte.png' });
    await p.click('#wzNext'); await p.waitForTimeout(400); await p.screenshot({ path: out + 'D02b_estudio_revisao.png' });
    await p.click('#publishBtn'); await p.waitForTimeout(1500);
  });
  await p.goto(base + '#/cards'); await p.reload(); await p.waitForTimeout(1500); await shot(p, 'D03_meus_cards_depois');

  /* E — fotografia (upload do usuário) separada da arte de fundo */
  await step('fluxo de fotografia', async () => {
    await p.goto(base + '#/gemeo'); await p.waitForTimeout(500); await p.evaluate(() => { location.hash = '#/novo'; }); await p.waitForTimeout(700);
    await p.click('.mode-opt[data-mode=MANUAL]'); await p.click('#wzNext'); await p.waitForTimeout(300);
    await p.check('input[name=ct][value=PARTIDA_MOMENTO]', { force: true });
    await p.fill('#composerTitle', 'Virada no último minuto da final regional'); await p.fill('#composerSub', 'O lance que mudou a temporada');
    await p.fill('#composerText', 'Cruzamento da esquerda, cabeceio no canto. 2 a 1 aos 93 minutos.');
    await p.setInputFiles('#composerPhoto', PHOTO); await p.waitForFunction(() => /enviada|prévia/i.test(document.getElementById('photoStatus').textContent), null, { timeout: 8000 }).catch(() => {});
    await p.waitForTimeout(600); await p.screenshot({ path: out + 'E01_conteudo_fotografia.png' });
    await p.click('#wzNext'); await p.waitForTimeout(300);
    await p.click('.swatch[data-color="#ff9f43"]'); await p.click('.optchip[data-key=material][data-val=VIDRO]'); await p.click('.optchip[data-key=layout][data-val=HERO]'); await p.click('.optchip[data-key=style][data-val=RETRO]'); await p.waitForTimeout(400);
    await p.screenshot({ path: out + 'E02_arte_fundo_com_foto.png' });
    await p.click('#wzNext'); await p.waitForTimeout(400); await p.screenshot({ path: out + 'E02b_revisao_com_foto.png' });
    await p.click('#publishBtn'); await p.waitForTimeout(1800);
  });
  await p.goto(base + '#/perfil/momentos'); await p.waitForTimeout(1500); await shot(p, 'E03_publicado_com_foto');
  await step('postview', async () => {
    const card = p.locator('.pcard', { hasText: 'Virada no último minuto' }).first();
    await card.locator('.p-title').first().click(); await p.waitForTimeout(900); await settle(p); await p.screenshot({ path: out + 'E04_postview_com_foto.png' });
  });

  /* F — mobile com tela de fundo (a preferência vem do servidor após o login) */
  await pick(p, { theme:'dark', accent:'GOLD', surface:'GRAPHITE', wallpaper:'OURO' });
  const mctx = await b.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true }); const m = await mctx.newPage();
  await login(m); await m.goto(base + '#/gemeo'); await m.waitForTimeout(1200); await settle(m); await m.screenshot({ path: out + 'F01_mobile_fundo_ouro.png' });
  await m.goto(base + '#/perfil'); await m.waitForTimeout(1200); await settle(m); await m.screenshot({ path: out + 'F02_mobile_perfil_fundo_ouro.png' });
  await mctx.close();
  await pick(p, { theme:'dark', accent:'GREEN', surface:'GRAPHITE', wallpaper:'NONE' });
  await b.close(); console.log(errs.length ? 'ISSUES:\n' + errs.join('\n') : 'no page errors');
})();
