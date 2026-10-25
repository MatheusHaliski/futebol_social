// Variações de aparência (material · gradiente · fonte · densidade · contraste), backgrounds dos cards e correção arte × foto.
// Uso: BASE=http://localhost:8080/ OUT=./out node variations.js   (API no ar com a conta semente atleta2@exemplo.dev)
// Atenção: altera dados — publica um card com fotografia e define a foto do perfil da conta semente.
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
const base = process.env.BASE || 'http://localhost:8080/';
const out = (process.env.OUT || path.join(__dirname, 'out')) + path.sep; fs.mkdirSync(out, { recursive: true });
const PHOTO = process.env.PHOTO || path.join(__dirname, '..', '..', 'gallery', 'scripts', 'foto_golaco.jpg');
const ACCOUNT = process.env.ACCOUNT || 'atleta2@exemplo.dev'; const PASSWORD = process.env.PASSWORD || 'Senha@Forte123';
const errs = [], report = {};
const step = async (label, fn) => { try { await fn(); } catch (e) { const m = label + ': ' + String(e.message).split('\n')[0]; errs.push(m); console.log('FAIL', m); } };
async function settle(p, ms = 500) {
  await p.waitForTimeout(ms);
  await p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  try { await p.waitForFunction(() => [...document.images].every(i => i.complete) && document.fonts.status === 'loaded', null, { timeout: 5000 }); } catch {}
}
async function shot(p, name, opts = {}) { await settle(p); await p.screenshot({ path: out + name + '.png', ...opts }); console.log(name, JSON.stringify(await p.evaluate(() => ({ ...document.documentElement.dataset })))); }
async function login(p) {
  await p.goto(base + '#/perfil'); await p.waitForTimeout(900);
  await p.fill('#loginForm input[name=email]', ACCOUNT); await p.fill('#loginForm input[name=password]', PASSWORD);
  await p.click('#loginForm button[type=submit]'); await p.waitForTimeout(1500);
}
// o criador abre pela rota #/novo (o botão ＋ flutuante só aparece no celular)
const openCreator = async p => { await p.goto(base + '#/gemeo'); await p.waitForTimeout(500); await p.evaluate(() => { location.hash = '#/novo'; }); await p.waitForSelector('#composer:not([hidden])', { timeout: 8000 }); await p.waitForTimeout(300); };
const openAp = async p => { await p.click('#btnLgpd'); await p.waitForTimeout(350); await p.click('.lgpd-nav [data-lgpd=aparencia]'); await p.waitForTimeout(250); };
const closeAp = async p => { await p.click('[data-close=lgpdModal]'); await p.waitForTimeout(300); };
const tap = async (p, sel) => { const el = p.locator(sel).first(); await el.scrollIntoViewIfNeeded(); await el.click({ force: true }); await p.waitForTimeout(280); };
async function apply(p, picks) {   // picks: [['preset','GRAMADO'], ['material','VIDRO'], ['theme','light'], ...]
  await openAp(p);
  for (const [k, v] of picks) await step(`${k} ${v}`, async () => {
    if (k === 'preset') return tap(p, `[data-preset=${v}]`);
    if (k === 'reset') return tap(p, '#apReset');
    if (k === 'theme') { const dark = await p.isChecked('#darkToggle'); if ((v === 'light') === dark) { await p.click('#darkToggle', { force: true }); await p.waitForTimeout(300); } return; }
    if (k === 'contrast') { const on = await p.isChecked('#contrastToggle'); if ((v === 'ALTO') !== on) { await p.click('#contrastToggle', { force: true }); await p.waitForTimeout(300); } return; }
    return tap(p, `[data-ap=${k}][data-val=${v}]`);
  });
  await closeAp(p);
}
// 20 combinações de background de card (gradiente · material · estilo · layout · cor principal · segunda cor)
const ART = [
  ['SUAVE','FOSCO','GRAFITE','CLASSICO','#00d47a'], ['SOLIDO','MALHA','GRAMADO','CLASSICO','#00d47a'], ['DEGRADE','CETIM','LISTRAS','HERO','#a06bff','#eaf2ee'], ['RADIAL','VIDRO','REDE','HERO','#3b82f6','#22d3ee'],
  ['DIAGONAL','PRISMA','MOSAICO','HERO','#f5c542','#ff9f43'], ['SUAVE','CARBONO','PRANCHETA','SPLIT','#ff9f43'], ['DEGRADE','PAPEL','ARQUIBANCADA','MINIMAL','#ff9f43','#ff5d5d'], ['DIAGONAL','CROMO','FAIXA','HERO','#ff5d5d','#16181a'],
  ['SOLIDO','HOLOGRAFICO','NEON','CLASSICO','#2dd4bf'], ['RADIAL','METALICO','OURO','HERO','#f5c542','#f5c542'], ['SUAVE','FOSCO','XADREZ','CLASSICO','#eaf2ee'], ['DEGRADE','VIDRO','VITRAL','SPLIT','#22d3ee','#a06bff'],
  ['SUAVE','PAPEL','RETRO','MINIMAL','#f7c948'], ['DIAGONAL','MALHA','LISTRAS','CLASSICO','#f7c948','#00d47a'], ['RADIAL','CETIM','ARQUIBANCADA','HERO','#f472b6','#a06bff'], ['SOLIDO','FOSCO','GRAFITE','MINIMAL','#ff5d5d'],
  ['DEGRADE','CROMO','REDE','SPLIT','#3b82f6','#eaf2ee'], ['SUAVE','HOLOGRAFICO','MOSAICO','CLASSICO','#a06bff','#22d3ee'], ['DIAGONAL','CARBONO','FAIXA','SPLIT','#00d47a','#16181a'], ['RADIAL','PRISMA','NEON','HERO','#e11d48','#f5c542']];
async function artGrid(p, name) {
  await p.goto(base + '#/perfil'); await p.waitForSelector('.pcard', { timeout: 8000 }); await p.waitForTimeout(600);
  await p.evaluate(art => {
    document.getElementById('artGrid')?.remove();
    const src = document.querySelector('.pcard'); const wrap = document.createElement('div'); wrap.id = 'artGrid';
    wrap.style.cssText = 'position:absolute;top:0;left:0;right:0;z-index:100;background:var(--ink);padding:18px;display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;align-content:start';
    art.forEach(([g, m, s, l, c, c2]) => {
      const n = src.cloneNode(true); n.classList.add('has-art'); n.querySelector('.p-media')?.remove();
      Object.assign(n.dataset, { gradient:g, material:m, style:s, layout:l }); n.style.setProperty('--ac', c); n.style.setProperty('--ac2', c2 || c);
      n.querySelector('.p-title').textContent = `${g} · ${m} · ${s}`;
      let sub = n.querySelector('.p-sub'); if (!sub) { sub = document.createElement('div'); sub.className = 'p-sub'; n.querySelector('.p-title').after(sub); }
      sub.textContent = `layout ${l} · ${c}${c2 && c2 !== c ? ' + ' + c2 : ''} · teaser e metadados com contraste AA`;
      wrap.appendChild(n);
    });
    document.body.appendChild(wrap); window.scrollTo(0, 0);
  }, ART);
  await settle(p, 700); await p.locator('#artGrid').screenshot({ path: out + name + '.png' }); console.log(name);
  await p.evaluate(() => document.getElementById('artGrid')?.remove());
}
// diferença média (0–255) entre duas capturas PNG do mesmo elemento, calculada no navegador
async function meanDiff(p, a, b) {
  return p.evaluate(async ([a, b]) => {
    const load = src => new Promise(r => { const i = new Image(); i.onload = () => r(i); i.src = 'data:image/png;base64,' + src; });
    const [ia, ib] = await Promise.all([load(a), load(b)]); const w = Math.min(ia.width, ib.width), h = Math.min(ia.height, ib.height);
    const px = img => { const c = document.createElement('canvas'); c.width = w; c.height = h; const x = c.getContext('2d'); x.drawImage(img, 0, 0); return x.getImageData(0, 0, w, h).data; };
    const da = px(ia), db = px(ib); let s = 0, mx = 0; for (let i = 0; i < da.length; i += 4) for (let k = 0; k < 3; k++) { const d = Math.abs(da[i + k] - db[i + k]); s += d; if (d > mx) mx = d; }
    return { mean: +(s / (w * h * 3)).toFixed(3), max: mx, w, h };
  }, [a.toString('base64'), b.toString('base64')]);
}
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 1440, height: 980 }, deviceScaleFactor: 1.25 }); const p = await ctx.newPage();
  p.on('pageerror', e => errs.push('pageerror: ' + e.message));

  /* L · modal de entrada: só o formulário de login aparece (o cadastro fica na outra aba) */
  await p.goto(base); await p.waitForTimeout(1200);
  report.authModal = await p.evaluate(() => ({ loginVisible: !!document.querySelector('#loginForm').offsetParent, registerVisible: !!document.querySelector('#registerForm').offsetParent }));
  await shot(p, 'L01_entrar_sem_cadastro_sobreposto');
  await login(p);
  await apply(p, [['reset']]);

  /* V · combinações prontas e combinações livres (material · gradiente · fonte · densidade · contraste) */
  const V = [
    ['V01_noite_de_jogo_vidro_holofote', [['preset','NOITE_DE_JOGO']], '#/gemeo'],
    ['V02_gramado_placar_arquibancada', [['preset','GRAMADO']], '#/perfil'],
    ['V03_prancheta_clara_editorial', [['preset','PRANCHETA_CLARA']], '#/scouting'],
    ['V04_alta_legibilidade_contraste_alto', [['preset','ALTA_LEGIBILIDADE']], '#/alerta'],
    ['V05_carbono_ciano_crepusculo_compacta', [['preset','CARBONO']], '#/gemeo'],
    ['V06_metalico_prata_vinho_editorial', [['reset'], ['accent','PRATA'], ['surface','VINHO'], ['material','METALICO'], ['gradient','CREPUSCULO'], ['font','EDITORIAL']], '#/perfil'],
    ['V07_claro_marinho_vidro_legivel', [['reset'], ['theme','light'], ['accent','BLUE'], ['surface','NAVY'], ['material','VIDRO'], ['font','LEGIVEL'], ['gradient','HOLOFOTE']], '#/gemeo'],
    ['V08_claro_floresta_gramado_espacosa', [['reset'], ['theme','light'], ['accent','GREEN'], ['surface','FOREST'], ['material','GRAMADO'], ['gradient','ARQUIBANCADA'], ['density','ESPACOSA'], ['font','PLACAR']], '#/perfil'],
    ['V09_sistema_ouro_carvao_carbono_fundo', [['reset'], ['accent','GOLD'], ['surface','CARVAO'], ['material','CARBONO'], ['font','SISTEMA'], ['wallpaper','OURO']], '#/flair'],
  ];
  for (const [name, picks, hash] of V) { await p.goto(base + hash); await p.waitForTimeout(800); await apply(p, picks); await shot(p, name); }

  /* P · o painel Aparência (combinações prontas, prévia, material, gradiente, fonte, densidade) */
  await apply(p, [['reset']]); await p.goto(base + '#/gemeo'); await p.waitForTimeout(600);
  await step('painel', async () => {
    await openAp(p); await settle(p); await p.screenshot({ path: out + 'P01_painel_combinacoes_e_previa.png' });
    await p.locator('#apMaterials').scrollIntoViewIfNeeded(); await settle(p, 300); await p.screenshot({ path: out + 'P02_painel_material_gradiente.png' });
    await p.locator('#apFonts').scrollIntoViewIfNeeded(); await settle(p, 300); await p.screenshot({ path: out + 'P03_painel_fonte_densidade.png' });
    await tap(p, '[data-ap=material][data-val=VIDRO]'); await tap(p, '[data-ap=gradient][data-val=CREPUSCULO]'); await tap(p, '[data-ap=font][data-val=PLACAR]');
    await p.locator('#apMock').scrollIntoViewIfNeeded(); await settle(p, 300); await p.screenshot({ path: out + 'P04_painel_previa_viva.png' });
    report.savedHint = await p.textContent('#apSaved'); await closeAp(p);
  });
  await step('persistência', async () => { await p.reload(); await p.waitForTimeout(1500); report.afterReload = await p.evaluate(() => ({ nav: document.documentElement.dataset.nav, amb: document.documentElement.dataset.amb, font: document.documentElement.dataset.font })); });
  await apply(p, [['reset']]);

  /* C · backgrounds dos cards: 20 combinações, tema escuro e claro */
  await artGrid(p, 'C01_backgrounds_cards_escuro');
  await apply(p, [['theme','light']]); await artGrid(p, 'C02_backgrounds_cards_claro'); await apply(p, [['theme','dark']]);
  await apply(p, [['contrast','ALTO']]); await artGrid(p, 'C03_backgrounds_cards_contraste_alto'); await apply(p, [['contrast','PADRAO']]);

  /* W · criador de cards, etapa Arte: paletas, gradiente bicolor, miniaturas e regra sóbria dos alertas */
  await step('criador · arte', async () => {
    await openCreator(p);
    await p.click('.mode-opt[data-mode=MANUAL]'); await p.click('#wzNext'); await p.waitForTimeout(300);
    await p.check('input[name=ct][value=PARTIDA_MOMENTO]', { force: true }); await p.fill('#composerTitle', 'Virada no último minuto da final regional'); await p.fill('#composerSub', 'O lance que mudou a temporada');
    await p.click('#wzNext'); await p.waitForTimeout(400);
    await tap(p, '.optchip[data-key=palette][data-val="Rubro-negro"]'); await tap(p, '.optchip[data-key=style][data-val=FAIXA]'); await tap(p, '.optchip[data-key=material][data-val=CROMO]'); await tap(p, '.optchip[data-key=layout][data-val=HERO]');
    await p.locator('#artPalettes').scrollIntoViewIfNeeded(); await settle(p); await p.screenshot({ path: out + 'W01_arte_paleta_rubro_negro_faixa.png' });
    await tap(p, '.optchip[data-key=palette][data-val=Celeste]'); await tap(p, '.optchip[data-key=gradient][data-val=RADIAL]'); await tap(p, '.optchip[data-key=style][data-val=REDE]'); await tap(p, '.optchip[data-key=material][data-val=PRISMA]');
    await p.locator('#artGradients').scrollIntoViewIfNeeded(); await settle(p); await p.screenshot({ path: out + 'W02_arte_radial_prisma_rede_miniaturas.png' });
    await p.click('#wzBack'); await p.waitForTimeout(300); await p.check('input[name=ct][value=LESAO_ALERTA]', { force: true }); await p.fill('#composerTitle', 'Atualização sobre a lesão no tornozelo direito');
    await p.click('#wzNext'); await p.waitForTimeout(400);
    report.soberOptions = await p.evaluate(() => ({ gradients: [...document.querySelectorAll('#artGradients .optchip')].map(x => x.dataset.val), materials: [...document.querySelectorAll('#artMaterials .optchip')].map(x => x.dataset.val), styles: [...document.querySelectorAll('#artStyles .optchip')].map(x => x.dataset.val), palettesHidden: document.getElementById('artPalettes').hidden }));
    await p.locator('#artSoberHint').scrollIntoViewIfNeeded(); await settle(p); await p.screenshot({ path: out + 'W03_arte_sobria_alerta_lesao.png' });
    await p.click('[data-close=composer]').catch(async () => { await p.keyboard.press('Escape'); }); await p.waitForTimeout(300);
  });

  /* F · fotografia × arte: publica com foto e a arte mais forte (vitral holográfico) e mede os pixels da foto */
  await step('fotografia', async () => {
    await openCreator(p);
    await p.click('.mode-opt[data-mode=MANUAL]'); await p.click('#wzNext'); await p.waitForTimeout(300);
    await p.check('input[name=ct][value=PARTIDA_MOMENTO]', { force: true });
    await p.fill('#composerTitle', 'Golaço de cabeça aos 93 minutos na final'); await p.fill('#composerText', 'Cruzamento da esquerda, cabeceio no canto. 2 a 1 aos 93 minutos.');
    await p.setInputFiles('#composerPhoto', PHOTO); await p.waitForFunction(() => /enviada|prévia/i.test(document.getElementById('photoStatus').textContent), null, { timeout: 10000 }).catch(() => {});
    await p.click('#wzNext'); await p.waitForTimeout(400);
    await tap(p, '.optchip[data-key=palette][data-val=Canarinho]'); await tap(p, '.optchip[data-key=gradient][data-val=RADIAL]'); await tap(p, '.optchip[data-key=material][data-val=HOLOGRAFICO]'); await tap(p, '.optchip[data-key=style][data-val=VITRAL]'); await tap(p, '.optchip[data-key=layout][data-val=HERO]');
    await settle(p); await p.screenshot({ path: out + 'F01_arte_forte_com_foto_previa.png' });
    await p.click('#wzNext'); await p.waitForTimeout(400); await p.click('#publishBtn'); await p.waitForTimeout(1800);
  });
  await step('fotografia · feed e pixels', async () => {
    await p.goto(base + '#/perfil/momentos'); await p.waitForTimeout(1500);
    const card = p.locator('.pcard', { hasText: 'Golaço de cabeça aos 93' }).first(); await card.scrollIntoViewIfNeeded(); await settle(p, 800);
    await card.screenshot({ path: out + 'F02_card_publicado_foto_intacta.png' });
    const img = card.locator('.p-media.photo img');
    report.photoStyle = await img.evaluate(el => { const s = getComputedStyle(el), m = getComputedStyle(el.parentElement); return { filter: s.filter, blend: s.mixBlendMode, opacity: s.opacity, frameZ: m.zIndex, frameIsolation: m.isolation }; });
    const before = await img.screenshot({ animations: 'disabled' });
    await card.evaluate(el => { el.dataset.gradient = 'SOLIDO'; el.dataset.material = 'FOSCO'; el.dataset.style = 'NONE'; el.dataset.layout = 'CLASSICO'; el.style.setProperty('--ac', '#5c6b62'); el.style.setProperty('--ac2', '#5c6b62'); });
    await p.waitForTimeout(300); const after = await img.screenshot({ animations: 'disabled' });
    await card.evaluate(el => el.classList.remove('has-art')); await p.waitForTimeout(300); const none = await img.screenshot({ animations: 'disabled' });
    report.photoPixels = { holoVitralVsNeutral: await meanDiff(p, before, after), holoVitralVsNoArt: await meanDiff(p, before, none) };
    await p.reload(); await p.waitForTimeout(1500);
    const c2 = p.locator('.pcard', { hasText: 'Golaço de cabeça aos 93' }).first(); await c2.locator('.p-title').first().click(); await p.waitForTimeout(1000); await settle(p);
    await p.screenshot({ path: out + 'F03_postview_foto_inteira.png' }); await p.click('[data-close=postViewModal]').catch(() => p.keyboard.press('Escape'));
  });

  /* K · foto do perfil (Conta → Foto do perfil) no hero do card de perfil, com o brilho do tier por baixo do conteúdo */
  await step('foto do perfil', async () => {
    await p.goto(base + '#/me'); await p.waitForTimeout(800);
    await p.click('#btnLgpd'); await p.waitForTimeout(350); await p.click('.lgpd-nav [data-lgpd=conta]'); await p.waitForTimeout(200);
    await p.setInputFiles('#avatarFile', PHOTO); await p.waitForFunction(() => /atualizada|falha/.test(document.getElementById('avatarStatus').textContent), null, { timeout: 10000 });
    report.avatarStatus = await p.textContent('#avatarStatus');
    await p.locator('.avatar-row').scrollIntoViewIfNeeded(); await settle(p); await p.screenshot({ path: out + 'K01_conta_foto_do_perfil.png' });
    await p.click('[data-close=lgpdModal]'); await p.goto(base + '#/gemeo'); await p.goto(base + '#/me'); await p.waitForTimeout(1500); await settle(p);
    await p.screenshot({ path: out + 'K02_card_perfil_com_foto.png' });
    report.vcardPhoto = await p.evaluate(() => { const i = document.querySelector('.vcard .imgblock.photo img'); return i ? { natural: i.naturalWidth > 0, filter: getComputedStyle(i).filter } : null; });
  });

  /* M · mobile: barra inferior no material escolhido */
  await apply(p, [['preset','GRAMADO']]);
  const mctx = await b.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true }); const m = await mctx.newPage();
  m.on('pageerror', e => errs.push('mobile pageerror: ' + e.message));
  await login(m); await m.goto(base + '#/gemeo'); await m.waitForTimeout(1300); await settle(m); await m.screenshot({ path: out + 'M01_mobile_gramado_placar.png' });
  await mctx.close();
  await apply(p, [['preset','NOITE_DE_JOGO']]);
  const m2ctx = await b.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true }); const m2 = await m2ctx.newPage();
  await login(m2); await m2.goto(base + '#/perfil'); await m2.waitForTimeout(1300); await settle(m2); await m2.screenshot({ path: out + 'M02_mobile_vidro_holofote.png' });
  await m2ctx.close();
  await apply(p, [['reset']]);
  fs.writeFileSync(out + 'report.json', JSON.stringify(report, null, 2));
  await b.close(); console.log(JSON.stringify(report, null, 2)); console.log(errs.length ? 'ISSUES:\n' + errs.join('\n') : 'no page errors');
})();
