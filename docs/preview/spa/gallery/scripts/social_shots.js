// Prints da camada social: botão Seguir no card, aba Seguindo, PostView com comentários, sino de notificações
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path'); const base = process.env.BASE || 'http://localhost:8080/'; const out = (process.env.OUT || path.join(__dirname, 'out')) + path.sep; fs.mkdirSync(out, { recursive: true });
const errs = [];
async function settle(p, ms = 600) { await p.waitForTimeout(ms); await p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; }); try { await p.waitForFunction(() => [...document.images].every(i => i.complete), null, { timeout: 4000 }); } catch {} }
async function login(p, email) { await p.goto(base + '#/perfil'); await p.waitForTimeout(800); await p.fill('#loginForm input[name=email]', email); await p.fill('#loginForm input[name=password]', 'Senha@Forte123'); await p.click('#loginForm button[type=submit]'); await p.waitForTimeout(1500); }
(async () => {
  const b = await chromium.launch(); const ctx = await b.newContext({ viewport: { width: 1440, height: 980 }, deviceScaleFactor: 1.25 }); const p = await ctx.newPage();
  p.on('pageerror', e => errs.push('pageerror: ' + e.message));
  await login(p, 'torcedor@exemplo.dev');
  // G01 — cards de atleta com botão Seguir / Seguindo (torcedor segue atleta2 no seed)
  await p.goto(base + '#/perfil'); await p.waitForTimeout(1500); await settle(p); await p.screenshot({ path: out + 'G01_cards_seguir.png' });
  console.log('botões seguir:', await p.evaluate(() => [...document.querySelectorAll('.vcard .act.follow')].map(b => b.textContent.trim())));
  // G02 — aba Seguindo no Gêmeo (só autores seguidos)
  await p.goto(base + '#/gemeo'); await p.waitForTimeout(900);
  await p.locator('#win-gemeo .sorttabs button', { hasText: 'Seguindo' }).first().click(); await p.waitForTimeout(1500); await settle(p);
  console.log('autores na aba Seguindo:', await p.evaluate(() => [...new Set([...document.querySelectorAll('#win-gemeo .pcard .p-name')].map(e => e.textContent.trim().split(' ')[0]))]));
  await p.screenshot({ path: out + 'G02_aba_seguindo.png' });
  // G03 — PostView com comentários (Hat-trick) + responder
  await p.locator('#win-gemeo .pcard', { hasText: 'Hat-trick' }).first().locator('.act[data-act=comment]').click(); await p.waitForTimeout(1500); await settle(p);
  console.log('comentários visíveis:', await p.evaluate(() => document.querySelectorAll('#cmList .cm').length), '· total:', await p.evaluate(() => document.getElementById('cmTotal')?.textContent));
  await p.screenshot({ path: out + 'G03_postview_comentarios.png' });
  await p.fill('#cmBody', 'Esse time joga muito! Partida da temporada.'); await p.click('#cmForm button[type=submit]'); await p.waitForTimeout(1500); await settle(p);
  console.log('após comentar:', await p.evaluate(() => document.querySelectorAll('#cmList .cm').length), '· total:', await p.evaluate(() => document.getElementById('cmTotal')?.textContent));
  await p.screenshot({ path: out + 'G04_comentario_publicado.png' });
  await p.click('[data-close=postViewModal]'); await p.waitForTimeout(300);
  // G05 — sino do dono do card (atleta2): notificações não lidas e leitura
  const ctx2 = await b.newContext({ viewport: { width: 1440, height: 980 }, deviceScaleFactor: 1.25 }); const q = await ctx2.newPage(); q.on('pageerror', e => errs.push('pageerror(atleta2): ' + e.message));
  await login(q, 'atleta2@exemplo.dev'); await q.goto(base + '#/gemeo'); await q.waitForTimeout(1500); await settle(q);
  console.log('contador do sino:', await q.evaluate(() => ({ topbar: document.getElementById('notifCount')?.textContent, hidden: document.getElementById('notifCount')?.hidden, nav: document.getElementById('navAlertCount')?.textContent })));
  await q.click('#btnNotif'); await q.waitForTimeout(1200); await settle(q); await q.screenshot({ path: out + 'G05_notificacoes.png' });
  console.log('itens no sino:', await q.evaluate(() => [...document.querySelectorAll('#notifList .notif b')].map(e => e.textContent).slice(0, 6)));
  await q.locator('#notifList .notif.unread').first().click(); await q.waitForTimeout(1500); await settle(q); await q.screenshot({ path: out + 'G06_notificacao_abre_card.png' });
  // G07 — mobile: sino e contador na barra inferior
  const mctx = await b.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true }); const m = await mctx.newPage();
  await login(m, 'atleta1@exemplo.dev'); await m.goto(base + '#/gemeo'); await m.waitForTimeout(1500); await settle(m); await m.screenshot({ path: out + 'G07_mobile_contador_alerta.png' });
  await b.close(); console.log(errs.length ? 'ISSUES:\n' + errs.join('\n') : 'no page errors');
})();
