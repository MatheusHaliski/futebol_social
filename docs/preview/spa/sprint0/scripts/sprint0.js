// Sprint 0 (P0): estados honestos com a API ligada, regra de saúde, menor, odds do FLAIR, consentimento do Copilot.
// Uso: BASE=http://localhost:8080/ OUT=./out node sprint0.js   (API no ar com o seed; atleta1 com shareMedicalData desligado)
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
const base = process.env.BASE || 'http://localhost:8080/';
const out = (process.env.OUT || path.join(__dirname, 'out')) + path.sep; fs.mkdirSync(out, { recursive: true });
const PASS = 'Senha@Forte123'; const errs = []; const report = {};
async function api(pathname, token, opts = {}) {
  const r = await fetch(base.replace(/\/$/, '') + pathname, { ...opts, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) } });
  return r.status === 204 ? null : r.json();
}
const tokenOf = async email => (await api('/v1/auth/login', null, { method: 'POST', body: JSON.stringify({ email, password: PASS }) })).accessToken;
async function settle(p, ms = 600) { await p.waitForTimeout(ms); await p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; }); }
async function login(p, email) {
  await p.goto(base + '#/perfil'); await p.waitForTimeout(900);
  await p.fill('#loginForm input[name=email]', email); await p.fill('#loginForm input[name=password]', PASS);
  await p.click('#loginForm button[type=submit]'); await p.waitForTimeout(1500);
}
async function shot(p, name) { await settle(p); await p.screenshot({ path: out + name + '.png' }); console.log(name); }
(async () => {
  const a1 = await api('/v1/me', await tokenOf('atleta1@exemplo.dev')), a5 = await api('/v1/me', await tokenOf('atleta5@exemplo.dev'));
  const b = await chromium.launch(); const mk = async () => { const ctx = await b.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1.25 }); const p = await ctx.newPage(); p.on('pageerror', e => errs.push('pageerror: ' + e.message)); return [ctx, p]; };

  /* torcedor: Gêmeo de um menor (404), carga de quem não compartilha saúde (403), Copilot sem consentimento, odds do FLAIR */
  let [ctx, p] = await mk(); await login(p, 'torcedor@exemplo.dev');
  await p.goto(base + `#/gemeo/evolucao?athleteId=${a5.id}`); await p.waitForTimeout(1500);
  report.minorEvolution = await p.evaluate(() => { const s = document.querySelector('#win-gemeo .subview[data-sub="evolucao"] .api-state'); return s ? s.innerText.split('\n').slice(0, 2) : null; });
  await shot(p, 'S01_gemeo_de_menor_nao_encontrado');
  await p.goto(base + `#/gemeo/carga?athleteId=${a1.id}`); await p.waitForTimeout(1500);
  report.privateLoad = await p.evaluate(() => { const s = document.querySelector('#win-gemeo .subview[data-sub="carga"] .api-state'); return s ? s.innerText.split('\n').slice(0, 2) : null; });
  await shot(p, 'S02_carga_privada_sem_acesso');
  await p.goto(base + '#/gemeo'); await p.waitForTimeout(600); await p.evaluate(() => { location.hash = '#/novo?copilot=1'; }); await p.waitForTimeout(900);
  await p.click('#wzNext'); await p.waitForTimeout(1500);
  report.copilot = await p.evaluate(() => document.getElementById('copilotBody').innerText.split('\n').slice(0, 2));
  await shot(p, 'S03_copilot_sem_consentimento');
  await p.click('[data-close=composer]'); await p.goto(base + '#/flair'); await p.waitForTimeout(1500);
  report.flairOdds = await p.evaluate(() => document.getElementById('flairOdds')?.textContent);
  await shot(p, 'S04_flair_odds_reais');
  await ctx.close();

  /* médico verificado: boletim de lesão com atleta vinculado */
  [ctx, p] = await mk(); await login(p, 'medico@exemplo.dev');
  await p.evaluate(() => { location.hash = '#/novo'; }); await p.waitForTimeout(900);
  await p.click('.mode-opt[data-mode=MANUAL]'); await p.click('#wzNext'); await p.waitForTimeout(300);
  await p.check('input[name=ct][value=LESAO_ALERTA]', { force: true }); await p.waitForTimeout(800);
  await p.fill('#composerTitle', 'Atualização médica do goleiro após o clássico de domingo');
  report.subjectOptions = await p.evaluate(() => document.querySelectorAll('#composerSubject option').length - 1);
  await p.locator('#subjectRow').scrollIntoViewIfNeeded(); await shot(p, 'S05_boletim_com_atleta_vinculado');
  await ctx.close();

  /* menor: o próprio card mostra a categoria, não a idade; tentar ficar público pede o responsável */
  [ctx, p] = await mk(); await login(p, 'atleta5@exemplo.dev');
  await p.goto(base + '#/me'); await p.waitForTimeout(1500);
  report.minorAgeChip = await p.evaluate(() => [...document.querySelectorAll('.vcard .attrs li')].map(li => li.textContent.trim()).filter(t => /SUB-|anos/.test(t)));
  await shot(p, 'S06_menor_categoria_no_card');
  await p.click('#btnLgpd'); await p.waitForTimeout(400); await p.click('.lgpd-nav [data-lgpd=privacidade]'); await p.waitForTimeout(300);
  await p.check('input[name=vis][value=PUBLICO]', { force: true }); await p.waitForTimeout(900);
  report.minorPublicToast = await p.evaluate(() => document.getElementById('toast')?.textContent);
  await p.screenshot({ path: out + 'S07_menor_publico_exige_responsavel.png' }); console.log('S07_menor_publico_exige_responsavel');
  await ctx.close();

  fs.writeFileSync(out + 'report.json', JSON.stringify(report, null, 2));
  await b.close(); console.log(JSON.stringify(report, null, 2)); console.log(errs.length ? 'ISSUES:\n' + errs.join('\n') : 'no page errors');
})();
