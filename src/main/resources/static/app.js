/* =====================================================================
   Football AI · Social — SPA mock
   Gramática herdada dos modelos de referência:
     identidade (bolinha por papel) · conteúdo (borda por tipo) ·
     feed (4 blocos segmentados) · raridade (tratamento do card de perfil)
   - roteamento por hash: #/{feed}/{sub}?{filtros}
   - painel de filtros por feed → query string de GET /v1/feeds/{FEED}
   - filterbar de chips mostra os filtros ativos (chip.hot)
   ===================================================================== */
(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];

  /* ---------- camada 1 · identidade (6 papéis canônicos + extensões) ---------- */
  const ROLE = document.body.dataset.role || 'ATLETA';
  const ROLES = {
    ATLETA:{ dc:'atleta', label:'atleta' }, CLUBE:{ dc:'clube', label:'clube formador' }, SCOUT:{ dc:'scout', label:'scout verificado' },
    MARCA:{ dc:'marca', label:'patrocinador' }, IMPRENSA:{ dc:'imprensa', label:'imprensa esportiva' }, OFICIAL_LIGA:{ dc:'oficial', label:'conta oficial' },
    STAFF:{ dc:'staff', label:'staff técnico' }, STAFF_SAUDE:{ dc:'saude', label:'dep. médico' }, AGENTE:{ dc:'agente', label:'agente' },
    TORCEDOR:{ dc:'torcedor', label:'torcedor' }, AGENTE_IA:{ dc:'ia', label:'agente IA' },
  };
  /* ---------- camada 2 · conteúdo (6 bordas) ---------- */
  const CT = {
    TREINO:{ cc:'treino', badge:'TREINO' }, PARTIDA_MOMENTO:{ cc:'partida', badge:'PARTIDA' }, RELATORIO_TATICO:{ cc:'scouting', badge:'SCOUTING' },
    LIGA:{ cc:'liga', badge:'LIGA' }, INSTITUCIONAL:{ cc:'institucional', badge:'INSTITUCIONAL' }, LESAO_ALERTA:{ cc:'institucional', badge:'LESÃO' }, ESPECIAL:{ cc:'especial', badge:'ESPECIAL' },
  };
  /* ---------- camada 4 · raridade ---------- */
  const TIER_MULT = { COMMON:1, RARE:1.05, EPIC:1.1, LEGENDARY:1.2, MYTHIC:1.3 };
  const TIER_COLOR = { COMMON:'#5c6b62', RARE:'#3b82f6', EPIC:'#a06bff', LEGENDARY:'#f5c542', MYTHIC:'#f5c542' };

  /* ---------- dados mock (pessoais como <PREENCHER_*>) ---------- */
  const athletes = [
    { id:'ath-1', name:'<PREENCHER_ATLETA_1>', handle:'@<preencher_1>', ini:'A1', role:'ATLETA', pos:'Meia', age:22, state:'PR', club:'<PREENCHER_CLUBE_1>', status:'CONTRATADO', tier:'LEGENDARY', score:1650, pp:1240, rp:780, s:[82,88,76], sub:'Referência estadual · Top 10% do ranking por posição', badges:[['⚡ PRONTO','green'],['⚽ 14','' ],['🏆 3','']], season:{games:22,goals:14,assists:7,minutes:1980,yellow:3,red:0}, phys:{readiness:78,acwr:1.21,status:'READY'}, likes:241, comments:18, shares:9 },
    { id:'ath-2', name:'<PREENCHER_ATLETA_2>', handle:'@<preencher_2>', ini:'A2', role:'ATLETA', pos:'Atacante', age:19, state:'SP', club:'<PREENCHER_CLUBE_2>', status:'AGENTE_LIVRE', tier:'MYTHIC', score:2480, pp:1480, rp:912, s:[91,93,89], sub:'Referência nacional · Top 1%, candidato à Seleção Football AI', badges:[['🏆 7',''],['⚽ 21',''],['🔥 6','']], momentum:1.6, season:{games:24,goals:21,assists:9,minutes:2110,yellow:2,red:0}, phys:{readiness:91,acwr:1.08,status:'READY'}, likes:1240, comments:96, shares:58 },
    { id:'ath-3', name:'<PREENCHER_ATLETA_3>', handle:'@<preencher_3>', ini:'A3', role:'ATLETA', pos:'Zagueiro', age:27, state:'PR', club:'<PREENCHER_CLUBE_1>', status:'CONTRATADO', tier:'EPIC', score:1120, pp:980, rp:520, s:[78,80,70], sub:'Sequência de atuações de destaque · Momentum 1,4×', badges:[['⚠️ FADIGA','yellow'],['🧱 9',''],['🛡️ FAIR','']], season:{games:20,goals:2,assists:1,minutes:1750,yellow:5,red:0,tackles:9}, phys:{readiness:52,acwr:1.62,status:'FATIGUE'}, likes:88, comments:7, shares:2 },
    { id:'ath-4', name:'<PREENCHER_ATLETA_4>', handle:'@<preencher_4>', ini:'A4', role:'ATLETA', pos:'Goleiro', age:31, state:'RS', club:'<PREENCHER_CLUBE_3>', status:'CONTRATADO', tier:'RARE', score:640, pp:520, rp:210, s:[60,72,55], sub:'Boa consistência ao longo de várias semanas de treino', badges:[['🧱 5',''],['⏱️ 1.980',''],['👑 CAP','']], season:{games:23,goals:0,assists:0,minutes:1980,yellow:1,red:0,cleanSheets:5}, phys:{readiness:66,acwr:1.15,status:'INJURED',injury:{type:'Lesão grau 2 · coxa',ret:'3 sem'}}, likes:54, comments:4, shares:1 },
    { id:'ath-5', name:'<PREENCHER_ATLETA_5>', handle:'@<preencher_5>', ini:'A5', role:'ATLETA', pos:'Lateral', age:17, state:'SP', club:'<PREENCHER_CLUBE_2>', status:'AGENTE_LIVRE', tier:'COMMON', score:310, pp:180, rp:60, s:[52,44,38], sub:'Gêmeo iniciante — poucos dados registrados ainda', badges:[['⚡ PRONTO','green'],['⏱️ 420','']], season:{games:6,goals:1,assists:0,minutes:420,yellow:0,red:0}, phys:{readiness:84,acwr:0.95,status:'READY'}, likes:12, comments:1, shares:0 },
  ];
  const orgs = [
    { id:'club-1', name:'<PREENCHER_CLUBE_1>', handle:'@<preencher_clube>', ini:'CL', role:'CLUBE', tier:'EPIC', score:1210, sub:'Clube formador · Série A · 31 atletas · 3 vagas abertas', meta:'CLUBE FORMADOR', stats:[['PLANTEL','31'],['PRONTOS','27'],['VAGAS','3']], verified:true },
    { id:'scout-1', name:'<PREENCHER_SCOUT>', handle:'@<preencher_scout>', ini:'SC', role:'SCOUT', tier:'RARE', score:0, sub:'Olheira regional · 12 clubes parceiros · verificada', meta:'SCOUT', stats:[['RELATÓRIOS','48'],['CLUBES','12']], verified:true },
  ];
  const posts = {
    perfil: [
      { role:'IMPRENSA', name:'@<preencher_veiculo>', ct:'PARTIDA_MOMENTO', top:'3h', title:'A promessa sub-20 que saltou de Epic para Legendary em 3 semanas', snip:'Análise com dados do Gêmeo Digital e histórico de 90 dias.', trend:'🔥 em alta · 18k views', media:'VÍDEO 16:9', likes:412, comments:37, shares:21 },
      { role:'SCOUT', name:'@<preencher_scout>', ct:'RELATORIO_TATICO', top:'RELATÓRIO', title:'Shortlist da rodada: laterais com passe > 85 e idade ≤ 21', snip:'Relatório homologado · 5 atletas · 3 estados.', trend:'📈 trending' },
    ],
    gemeo: [
      { role:'ATLETA', name:'@<preencher_user>', ct:'TREINO', top:'2h', title:'Sessão de sprints — 25 km/h sustentados', snip:'+38 PP registrados pelo wearable · 72 min · 8,4 km · RPE 7.', trend:'📈 PP em alta', meta:['FÍSICA','TREINO','W43'], likes:138, comments:12, shares:5 },
      { role:'ATLETA', name:'@<preencher_user>', ct:'PARTIDA_MOMENTO', top:'1d', title:'Hat-trick de passes na semifinal', snip:'Minerado como Momento do mês · 90 min · nota 7,8.', trend:'🔥 em alta · 18k views', media:'MOMENTO · 1:1', meta:['TÉCNICA','PARTIDA'], likes:1240, comments:96, shares:58 },
      { role:'STAFF', name:'@<preencher_preparador>', ct:'TREINO', top:'1d', title:'Coletivo: 6 × 4 min de jogo reduzido', snip:'Foco em transição defensiva · carga média 480 AU.', trend:'➖ estável', meta:['TÁTICA','TREINO'] },
      { role:'OFICIAL_LIGA', name:'@footballai', ct:'ESPECIAL', top:'W42', title:'🏅 Prêmio: Gêmeo da Semana — Sub-20', snip:'Conquista especial registrada no card de perfil.', trend:'💠 especial' },
    ],
    scouting: [
      { role:'SCOUT', name:'@<preencher_scout>', ct:'RELATORIO_TATICO', top:'RELATÓRIO', title:'Comparador: Atacantes Sub-20 PR — Top 5', snip:'Ranking por Score_Total das últimas 4 semanas.', meta:['HOMOLOGADO','VELOCIDADE > 80'] },
      { role:'CLUBE', name:'@<preencher_clube>', ct:'RELATORIO_TATICO', top:'RELATÓRIO', title:'Ficha técnica completa — <PREENCHER_ATLETA_1>', snip:'PP, RP, MP e histórico de 90 dias.', meta:['HOMOLOGADO'] },
      { role:'TORCEDOR', name:'@<preencher_torcedor>', ct:'RELATORIO_TATICO', top:'OPINIÃO', title:'Da arquibancada: o time cresce quando o meia recua', snip:'Relatório da comunidade · opinativo · não entra no Rarity.', meta:['COMUNIDADE'] },
    ],
    pranchetas: [
      { role:'STAFF', name:'@<preencher_tecnico>', ct:'RELATORIO_TATICO', top:'PRANCHETA', title:'4-3-3 com pressão alta — saída de bola sob pressão', snip:'Drill de 12 execuções · aderência média 85%.', media:'PRANCHETA · 16:9', meta:['HOMOLOGADO'] },
    ],
    alerta: [
      { role:'CLUBE', name:'@<preencher_clube>', ct:'LESAO_ALERTA', alert:true, alarm:'🩹 LESÃO', top:'AGORA', title:'Atualização médica — afastamento de 3 semanas', snip:'Departamento médico confirma diagnóstico e prazo de retorno.', sev:'ALTA', type:'LESAO', likes:66, comments:14, shares:9 },
      { role:'OFICIAL_LIGA', name:'@footballai', ct:'ESPECIAL', alert:true, alarm:'📣 CONVOCAÇÃO', top:'2h', title:'Seleção Football AI Sub-20 — lista divulgada', snip:'23 atletas convocados para o Torneio Mensal.', sev:'MEDIA', type:'CONVOCACAO', likes:980, comments:120, shares:210 },
      { role:'OFICIAL_LIGA', name:'@<preencher_federacao>', ct:'INSTITUCIONAL', alert:true, alarm:'⚖️ PUNIÇÃO', top:'5h', title:'Suspensão de 2 jogos após expulsão na rodada 31', snip:'Decisão do tribunal desportivo · cabe recurso em 48 h.', sev:'MEDIA', type:'PUNICAO' },
      { role:'IMPRENSA', name:'@<preencher_veiculo>', ct:'INSTITUCIONAL', top:'1d', title:'Proposta oficial recebida por lateral sub-20', snip:'Janela fecha em 9 dias · clube avalia contraproposta.', trend:'📈 trending', sev:'BAIXA', type:'TRANSFERENCIA' },
      { role:'AGENTE_IA', name:'Resumo semanal', ai:true, ct:'INSTITUCIONAL', top:'IA', title:'3 atletas do seu clube mudaram de tier esta semana', snip:'2 ↑ 1 ↓ · operado por @<preencher_staff> · gerado por IA.', sev:'BAIXA', type:'INSTITUCIONAL' },
    ],
    mural: [
      { role:'CLUBE', name:'@<preencher_clube>', ct:'INSTITUCIONAL', top:'📌 FIXADO', title:'Convocação para o amistoso de sábado', snip:'Lista no vestiário digital · concentração 14h.', meta:['ELENCO'] },
      { role:'CLUBE', name:'@<preencher_clube>', ct:'LIGA', top:'ENQUETE', title:'Nova terceira camisa — vote até domingo', snip:'1.204 votos · aberta a sócios.', trend:'🔥 em alta' },
      { role:'STAFF', name:'@<preencher_tecnico>', ct:'RELATORIO_TATICO', top:'1d', title:'Resumo tático da semana para o elenco', snip:'Pressão alta funcionou nos 2 jogos · ajustar bola parada.', meta:['ELENCO'] },
    ],
    saude: [ { role:'STAFF_SAUDE', name:'@<preencher_medico>', ct:'LESAO_ALERTA', alert:true, alarm:'🔒 PRIVADO', top:'HOJE', title:'Reavaliação da coxa direita agendada', snip:'Só você e o departamento médico veem isto (Art. 11).', lock:true } ],
    autopilot: [
      { role:'AGENTE_IA', ai:true, name:'Autopilot · rascunho', ct:'TREINO', top:'RASCUNHO', title:'64 min · 7,9 km · RPE estimado 6', snip:'Fonte: wearable · sem dados de saúde · publicar como TREINO?', meta:['REVISAR'] },
      { role:'AGENTE_IA', ai:true, name:'Autopilot · rascunho', ct:'PARTIDA_MOMENTO', top:'RASCUNHO', title:'90 min jogados · 1 assistência · nota 7,8', snip:'Fonte: súmula oficial · publicar como PARTIDA?', meta:['REVISAR'] },
    ],
    momentos: [ { role:'ATLETA', name:'@<preencher_2>', ct:'ESPECIAL', top:'EDIÇÃO 1/50', title:'🎬 Momento #0412 · gol de fora da área', snip:'Colecionável no FLAIR · borda especial.', trend:'💠 especial · 18k views', media:'MOMENTO · 1:1' } ],
  };
  const clubs = [
    { n:'<PREENCHER_CLUBE_1>', c:'BR', d:1, lat:-22.9, lng:-43.2, tier:'EPIC' }, { n:'<PREENCHER_CLUBE_2>', c:'BR', d:1, lat:-23.5, lng:-46.6, tier:'LEGENDARY' },
    { n:'<PREENCHER_CLUBE_3>', c:'BR', d:2, lat:-30.0, lng:-51.2, tier:'RARE' }, { n:'<PREENCHER_CLUBE_4>', c:'AR', d:1, lat:-34.6, lng:-58.4, tier:'EPIC' },
    { n:'<PREENCHER_CLUBE_5>', c:'ES', d:1, lat:40.4, lng:-3.7, tier:'MYTHIC' }, { n:'<PREENCHER_CLUBE_6>', c:'EN', d:1, lat:53.5, lng:-2.2, tier:'MYTHIC' },
    { n:'<PREENCHER_CLUBE_7>', c:'DE', d:1, lat:48.2, lng:11.6, tier:'LEGENDARY' }, { n:'<PREENCHER_CLUBE_8>', c:'SA', d:1, lat:24.7, lng:46.7, tier:'EPIC' },
    { n:'<PREENCHER_CLUBE_9>', c:'US', d:1, lat:34.0, lng:-118.2, tier:'RARE' }, { n:'<PREENCHER_CLUBE_10>', c:'BR', d:3, lat:-3.7, lng:-38.5, tier:'COMMON' },
  ];
  const lgpdToggles = [
    ['shareTelemetryWithClubs','Compartilhar telemetria com clubes','Treinos, carga e distância visíveis ao clube vinculado','Art. 7º, I · Consentimento revogável',false],
    ['shareTelemetryWithScouts','Compartilhar telemetria com scouts','Scouts verificados veem seu Gêmeo','Art. 7º, I · Consentimento revogável',false],
    ['shareMedicalData','Status de saúde e lesões','Badges ⚡/⚠️/🩹 e boletins visíveis a terceiros','Art. 11 · Dado sensível',true],
    ['shareBiometricData','Reconhecimento facial em vídeos (Lens)','Permite identificar seu rosto em análises de vídeo','Art. 11 · Dado sensível',true],
    ['locationHistory','Histórico de localização (GPS de treinos)','Grava o trajeto das sessões','Art. 7º, I · Consentimento revogável',false],
    ['shareWithBrands','Compartilhar com marcas parceiras','Ativações e cupons de patrocinadores','Art. 18, IX · Revogação',false],
    ['personalizedAds','Anúncios personalizados','Usa seu histórico para escolher anúncios','Art. 7º, I · Consentimento revogável',false],
    ['aiAgentProcessing','Processamento por agentes de IA','Copilot, Autopilot e resumos automáticos','Art. 7º, I · Art. 20 · revisão humana',false],
  ];

  /* ---------- templates ---------- */
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' }[c]));
  const chip = t => `<span class="rarity-chip rc-${t.toLowerCase()}">${t}</span>`;
  const roleOf = r => ROLES[r] || ROLES.ATLETA;

  /* PostCard — borda esquerda = tipo · bolinha = papel · trend por views */
  const postCard = p => {
    const likes = p.likes ?? 0, comments = p.comments ?? 0, shares = p.shares ?? 0;
    const r = roleOf(p.role), c = CT[p.ct] || CT.TREINO;
    const VSL = { treino:'◇', partida:'◇', scouting:'▭', liga:'⬢', institucional:'▭', especial:'✦' };
    const age = p.lock || p.ct === 'LESAO_ALERTA' ? ['age-sens','SENSÍVEL'] : p.age || ['age-safe','SAFE'];
    const topRight = p.alert ? `<span class="alarm">${p.alarm}</span>` : `<span class="badge cc">${c.badge}</span>`;
    let body = `<div class="p-title">${esc(p.title)}</div>`;
    if (p.media) body += `<div class="p-media">${esc(p.media)}</div>`;           // post com imagem: imagem + autor/título, sem subtítulo
    else if (p.snip) body += `<div class="p-sub">${esc(p.snip)}</div>`;          // post de texto: subtítulo teaser (≤ 2 linhas, ~70%)
    if (p.meta) body += `<div class="p-meta">${p.meta.map(m => `<span class="badge">${esc(m)}</span>`).join('')}</div>`;
    if (p.lock) body += `<span class="lock">🔒 DADO SENSÍVEL · ART. 11</span>`;
    return `<article class="pcard cc-${c.cc}${p.alert ? ' alertcard' : ''}">
      <div class="p-top"><span class="time">${esc(p.top || '')}</span><span class="vsl" title="VSL">${VSL[c.cc]}</span>${topRight}<span class="age-chip ${age[0]}">${age[1]}</span></div>
      <div class="p-author"><span class="dot sm dc-${r.dc}"></span><span class="p-name">${esc(p.name)} <span>· ${r.label}</span>${p.ai ? '<span class="ai">IA</span>' : ''}</span></div>
      ${body}
      <div class="actbar">
        <button class="act like" data-act="like"><span class="i">♡</span>Curtir<b>${likes.toLocaleString('pt-BR')}</b></button>
        <button class="act" data-act="comment"><span class="i">💬</span>Comentar<b>${comments.toLocaleString('pt-BR')}</b></button>
        <button class="act" data-act="share"><span class="i">↗</span>Compartilhar<b>${shares.toLocaleString('pt-BR')}</b></button>
        <button class="act save" data-act="save" aria-label="salvar"><span class="i">🔖</span></button>
        ${p.trend ? `<span class="trend${/💠|✦/.test(p.trend) ? ' gold' : /📉|➖/.test(p.trend) ? ' down' : ''}">${esc(p.trend)}</span>` : ''}
      </div>
    </article>`;
  };

  /* Card de perfil / atleta (.vcard) — Common Border dupla + raridade + dados completos */
  const PHYS = { READY:['⚡','PRONTO PRA JOGO','ok'], FATIGUE:['⚠️','RISCO DE FADIGA','warn'], INJURED:['🩹','DEPARTAMENTO MÉDICO','bad'] };
  const vcard = (a, opts = {}) => {
    const r = roleOf(a.role), t = (a.tier || 'COMMON').toLowerCase();
    const cc = a.tier === 'COMMON' || !a.tier ? '#5c6b62' : TIER_COLOR[a.tier];
    const micro = a.badges ? `<div class="micro">${a.badges.slice(0, 3).map(b => `<b class="${b[1]}" data-badge="${esc(b[0])}">${esc(b[0])}</b>`).join('')}</div>` : '';
    const title = a.pos ? `${esc(a.name)} — ${a.pos}` : esc(a.name);
    const sub = a.pos ? `${a.age} anos · ${a.club} · ${a.status === 'AGENTE_LIVRE' ? 'Agente livre' : 'Contratado'} · ${a.sub}` : a.sub;
    let data = '';
    if (a.season) {
      const sz = a.season, ph = a.phys || {}, st = PHYS[ph.status] || PHYS.READY;
      const gk = sz.cleanSheets !== undefined, df = sz.tackles !== undefined;
      data += `<div class="sgrid">
        <div class="stile"><small>JOGOS</small><b>${sz.games}</b></div>
        <div class="stile"><small>${gk ? 'CLEAN SHEETS' : 'GOLS'}</small><b>${gk ? sz.cleanSheets : sz.goals}</b></div>
        <div class="stile"><small>${df ? 'DESARMES' : 'ASSIST.'}</small><b>${df ? sz.tackles : sz.assists}</b></div>
        <div class="stile"><small>MINUTOS</small><b>${sz.minutes.toLocaleString('pt-BR')}</b></div>
        <div class="stile"><small>CARTÕES</small><b><span class="yc">${sz.yellow}</span> <span class="rc">${sz.red}</span></b></div>
        <div class="stile"><small>NOTA MÉDIA</small><b>${(6.2 + (a.s[1] / 40)).toFixed(1)}</b></div>
      </div>
      <div class="phys ${st[2]}">
        <div class="pl"><small>CONDICIONAMENTO</small><div class="rd"><i style="--w:${ph.readiness || 0}%"></i></div><span>prontidão <b>${ph.readiness ?? '—'}%</b> · ACWR <b>${(ph.acwr ?? 0).toLocaleString('pt-BR')}</b></span></div>
        <div class="pst"><span class="i">${st[0]}</span><b>${st[1]}</b>${ph.injury ? `<small>${esc(ph.injury.type)} · retorno ${esc(ph.injury.ret)}</small>` : ''}</div>
      </div>`;
    }
    const stats = a.pp !== undefined
      ? `<span class="pp">PP <b>${a.pp.toLocaleString('pt-BR')}</b></span><span class="rp">RP <b>${a.rp.toLocaleString('pt-BR')}</b></span><span class="mp">MP <b>${a.s ? a.s[2] : '—'}</b></span><span class="st">SCORE <b>${a.score.toLocaleString('pt-BR')}</b></span>`
      : (a.stats || []).map(([k, v]) => `<span class="mp">${k} <b>${v}</b></span>`).join('');
    const actions = opts.actions === false ? '' : `<div class="actbar">
        <button class="act like" data-act="like"><span class="i">♡</span>Curtir<b>${(a.likes ?? 0).toLocaleString('pt-BR')}</b></button>
        <button class="act" data-act="comment"><span class="i">💬</span>Comentar<b>${(a.comments ?? 0).toLocaleString('pt-BR')}</b></button>
        <button class="act" data-act="share"><span class="i">↗</span>Compartilhar<b>${(a.shares ?? 0).toLocaleString('pt-BR')}</b></button>
        <a class="act go" href="#/gemeo?athleteId=${a.id}">Gêmeo →</a>
      </div>`;
    return `<article class="vcard rarity-${t}${opts.wide ? ' wide' : ''}" style="--cc:${cc}" data-id="${a.id}">
      <div class="outer"><div class="inner">
        <div class="ctop"><span class="l"><span class="vsl">${a.pos ? '◇' : '⬢'}</span>${a.pos ? 'GÊMEO DIGITAL' : (a.meta || 'PERFIL')}</span>${a.phys && a.phys.status === 'INJURED' ? '<span class="age-chip age-sens">SENSÍVEL</span>' : '<span class="age-chip age-safe">SAFE</span>'}${chip(a.tier || 'COMMON')}</div>
        <div class="imgblock"><svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/></svg>${micro}</div>
        <div class="author"><span class="dot dc-${r.dc}"></span><span class="handle"><b>${esc(a.handle)}</b> <span>· ${r.label}</span>${a.verified ? '<span class="verified">✦</span>' : ''}</span></div>
        <div class="vtitle">${title}</div>
        <div class="vsub">${esc(sub)}</div>
        ${data}
        <div class="statrow">${stats}</div>
        ${actions}
      </div></div></article>`;
  };

  /* ---------- estado e roteamento ---------- */
  const WINDOWS = ['perfil','gemeo','scouting','alerta'];
  const DEFAULT_SUB = { perfil:'feed', gemeo:'telemetria', scouting:'pranchetas', alerta:'tudo' };
  const state = { win:'perfil', sub:{ ...DEFAULT_SUB }, params:{} };
  const FILTERED_SUBS = ['feed','telemetria','pranchetas','tudo'];

  function parseHash() {
    const h = location.hash.replace(/^#\/?/, '') || 'perfil';
    const [path, qs] = h.split('?'); const [win, sub] = path.split('/');
    return { win, sub, params: Object.fromEntries(new URLSearchParams(qs || '')) };
  }
  function route() {
    const { win, sub, params } = parseHash(); state.params = params;
    $$('.window').forEach(w => w.hidden = true); $('#sidebar').classList.remove('open');
    if (WINDOWS.includes(win)) {
      state.win = win; state.sub[win] = sub || DEFAULT_SUB[win];
      $(`#win-${win}`).hidden = false;
      $$(`.segments[data-for="${win}"] a`).forEach(a => a.classList.toggle('active', a.dataset.sub === state.sub[win]));
      $$(`#win-${win} .subview`).forEach(v => v.hidden = v.dataset.sub !== state.sub[win]);
      $(`#win-${win} .win-body`).classList.toggle('no-filters', !FILTERED_SUBS.includes(state.sub[win]));
      applyParamsToForm(win, params); updatePreview(win); renderSub(win, state.sub[win]);
    } else if (win === 'flair') { $('#win-flair').hidden = false; renderFlair(); }
    else if (win === 'novo') { openComposer(params.copilot === '1'); location.hash = '#/perfil'; return; }
    else if (win === 'configuracoes') { $('#win-perfil').hidden = false; openLgpd(); }
    else { $('#win-misc').hidden = false; renderMisc(win, sub, params); }
    $$('.bottom-nav a, .switcher a').forEach(a => a.classList.toggle(a.closest('.switcher') ? 'on' : 'active', a.dataset.win === win));
    const navKey = `${win}/${sub || DEFAULT_SUB[win] || ''}`.replace(/\/$/, '');
    $$('.sidebar a').forEach(a => a.classList.toggle('active', a.dataset.nav === navKey || a.dataset.nav === win));
    window.scrollTo({ top: 0 });
  }

  /* ---------- filtros → query string + filterbar ---------- */
  function formToParams(win) {
    const fd = new FormData($(`#form-${win}`)); const p = new URLSearchParams(); const multi = {};
    for (const [k, v] of fd.entries()) {
      if (v === '' || k.startsWith('op-') || k === 'compareA' || k === 'compareB') continue;
      if (k.startsWith('attr-')) { p.append(`attr[${k.slice(5)}]`, `${fd.get('op-' + k.slice(5)) || 'gt'}:${v}`); continue; }
      (multi[k] ||= []).push(v);
    }
    if (fd.get('compareA') && fd.get('compareB')) p.set('compare', `${fd.get('compareA')},${fd.get('compareB')}`);
    Object.entries(multi).forEach(([k, vs]) => p.set(k, vs.join(',')));
    return p;
  }
  const LABEL = { tier:'Tier', role:'Papel', position:'Posição', ageMin:'Idade ≥', ageMax:'Idade ≤', state:'Estado', clubId:'Clube', contractStatus:'Status', period:'Período', metricType:'Métrica', eventType:'Evento', contentType:'Tipo de post', athleteId:'Atleta', source:'Origem', compare:'Comparar', severity:'Urgência', alertType:'Tipo' };
  const AXES = { perfil:['Papel','Posição','Estado','Clube','Tier','Nome'], gemeo:['Papel','Tipo de post','Período','Métrica','Evento','Nome'], scouting:['Posição','Estado','Tier','Clube','Idade','Nome'], alerta:['Papel','Tipo','Estado','Clube','Urgência','Nome'] };
  function updatePreview(win) {
    const p = formToParams(win); const qs = p.toString();
    $(`[data-preview="${win}"]`).textContent = `GET /v1/feeds/${win.toUpperCase()}${qs ? '?' + decodeURIComponent(qs) : ''}`;
    const bar = $(`.filterbar[data-bar="${win}"]`); if (!bar) return;
    const active = {}; for (const [k, v] of p.entries()) { const key = k.startsWith('attr[') ? k.slice(5, -1) : k; active[LABEL[key] || key] = v; }
    bar.innerHTML = AXES[win].map(ax => { const hot = Object.keys(active).find(k => k.toLowerCase().startsWith(ax.toLowerCase().slice(0, 4))); return `<span class="fchip${hot ? ' hot' : ''}">${ax}${hot ? ` <b>${esc(active[hot])}</b>` : ''} <span class="ar">${ax === 'Nome' ? '🔍' : '▾'}</span></span>`; }).join('')
      + Object.entries(active).filter(([k]) => !AXES[win].some(ax => k.toLowerCase().startsWith(ax.toLowerCase().slice(0, 4)))).map(([k, v]) => `<span class="fchip hot">${esc(k)} <b>${esc(v)}</b></span>`).join('');
  }
  function applyParamsToForm(win, params) {
    const f = $(`#form-${win}`); if (!f) return; f.reset();
    Object.entries(params).forEach(([k, v]) => {
      if (k === 'compare') { const [a, b] = v.split(','); if (f.compareA) { f.compareA.value = a || ''; f.compareB.value = b || ''; } return; }
      const m = k.match(/^attr\[(\w+)\]$/); if (m) { const [op, val] = v.split(':'); if (f[`attr-${m[1]}`]) { f[`attr-${m[1]}`].value = val; f[`op-${m[1]}`].value = op; } return; }
      const els = f.querySelectorAll(`[name="${k}"]`); if (!els.length) return; const vals = v.split(',');
      els.forEach(el => { if (el.type === 'checkbox' || el.type === 'radio') el.checked = vals.includes(el.value); else el.value = v; });
    });
  }
  WINDOWS.forEach(win => {
    const f = $(`#form-${win}`);
    f.addEventListener('input', () => updatePreview(win));
    f.addEventListener('submit', e => { e.preventDefault(); const qs = formToParams(win).toString(); location.hash = `#/${win}/${state.sub[win]}${qs ? '?' + qs : ''}`; toast(`FILTROS APLICADOS · ${win.toUpperCase()}`); });
    f.addEventListener('reset', () => setTimeout(() => { updatePreview(win); location.hash = `#/${win}/${state.sub[win]}`; }, 0));
  });
  function setFilters(win, open) {
    const b = $(`.filters-toggle[data-toggle="${win}"]`); b.setAttribute('aria-expanded', String(open)); b.textContent = (open ? '◧ ' : '◨ ') + 'filtros';
    $(`#filters-${win}`).classList.toggle('collapsed', !open); $(`#win-${win} .win-body`).classList.toggle('filters-collapsed', !open);
  }
  $$('.filters-toggle').forEach(b => b.addEventListener('click', () => setFilters(b.dataset.toggle, b.getAttribute('aria-expanded') !== 'true')));
  if (window.matchMedia('(max-width: 860px)').matches) WINDOWS.forEach(w => setFilters(w, false));

  /* ---------- render por feed / sub-aba ---------- */
  const filteredAthletes = p => athletes.filter(a => (!p.tier || p.tier.split(',').includes(a.tier)) && (!p.position || a.pos.toUpperCase() === p.position)
    && (!p.ageMin || a.age >= +p.ageMin) && (!p.ageMax || a.age <= +p.ageMax) && (!p.state || a.state === p.state) && (!p.contractStatus || a.status === p.contractStatus) && (!p.role || p.role.split(',').includes(a.role)));
  function renderSub(win, sub) {
    const p = state.params;
    if (win === 'perfil') {
      if (sub === 'feed') { const roles = p.role ? p.role.split(',') : null; const list = [...filteredAthletes(p), ...orgs.filter(o => (!roles || roles.includes(o.role)) && !p.position && !p.tier)]; $('#feed-perfil').innerHTML = list.map(a => vcard(a)).join('') || '<p class="hint">Nenhum perfil com esses filtros.</p>'; $('#feed-perfil-posts').innerHTML = posts.perfil.map(postCard).join(''); }
      if (sub === 'ranking') renderRanking('atletas');
      if (sub === 'explorar') { drawGlobe(+$('#globeRot').value); renderClubs(); }
      if (sub === 'momentos') $('#feed-momentos').innerHTML = posts.momentos.map(postCard).join('');
    }
    if (win === 'gemeo') {
      if (sub === 'telemetria') { const ev = p.eventType, cts = p.contentType?.split(','); $('#feed-gemeo').innerHTML = posts.gemeo.filter(x => (!ev || (ev === 'TREINO' ? x.ct === 'TREINO' : x.ct === 'PARTIDA_MOMENTO')) && (!cts || cts.includes(x.ct))).map(postCard).join('') || '<p class="hint">Nada por aqui com esses filtros.</p>'; }
      if (sub === 'evolucao') drawEvolution();
      if (sub === 'carga') { drawGauge(78); drawLoad(); }
      if (sub === 'dna') drawRadar([88, 84, 72, 55, 80]);
      if (sub === 'comparador') $('#compareTwin').innerHTML = [athletes[0], athletes[1]].map(a => vcard(a, { wide:true })).join('');
      if (sub === 'provador') renderProvador();
      if (sub === 'autopilot') $('#feed-autopilot').innerHTML = posts.autopilot.map(postCard).join('');
    }
    if (win === 'scouting') {
      if (sub === 'pranchetas') { const src = p.source; $('#feed-scouting').innerHTML = posts.scouting.filter(x => !src || (src === 'HOMOLOGADO' ? x.role !== 'TORCEDOR' : x.role === 'TORCEDOR')).map(postCard).join(''); }
      if (sub === 'comparar') { const ids = (p.compare || 'ath-1,ath-2').split(','); $('#compareScout').innerHTML = ids.map(id => athletes.find(a => a.id === id)).filter(Boolean).map(a => vcard(a, { wide:true })).join(''); }
      if (sub === 'relatorios') $('#feed-relatorios').innerHTML = posts.pranchetas.map(postCard).join('');
    }
    if (win === 'alerta') {
      if (sub === 'tudo') { const sev = p.severity?.split(','), ty = p.alertType?.split(','), so = p.source?.split(','); $('#feed-alerta').innerHTML = posts.alerta.filter(x => (!sev || sev.includes(x.sev)) && (!ty || ty.includes(x.type)) && (!so || so.includes(x.role))).map(postCard).join('') || '<p class="hint">Nenhum alerta com esses filtros.</p>'; }
      if (sub === 'mural') $('#feed-mural').innerHTML = posts.mural.map(postCard).join('');
      if (sub === 'saude') $('#feed-saude').innerHTML = posts.saude.map(postCard).join('');
      if (sub === 'dm') $('#dmList').innerHTML = ['@<preencher_tecnico>|Convocação confirmada para sábado?|staff', '@<preencher_scout>|Posso incluir você na shortlist sub-23?|scout', '@<preencher_clube>|Renovação: proposta enviada.|clube'].map(s => { const [n, t, r] = s.split('|'); return `<li><span class="avatar" data-initials="${n.replace(/[@<>_]/g,'').slice(0,2).toUpperCase()}"><i class="dot dc-${r}"></i></span><div><b>${esc(n)}</b><small>${esc(t)}</small></div><small class="mono">AGORA</small></li>`; }).join('');
    }
  }
  function renderRanking(kind) {
    const rows = kind === 'atletas' ? [...athletes].sort((a, b) => b.score - a.score).map((a, i) => ({ i, n:a.handle, ini:a.ini, r:'atleta', meta:`${a.pos} · ${a.age} · ${a.club}`, score:a.score, tier:a.tier, d:[+2,0,-1,+1,0][i] }))
      : clubs.slice(0, 6).map((c, i) => ({ i, n:c.n, ini:'C' + (i + 1), r:'clube', meta:`${c.c} · Divisão ${c.d}`, score:[2310,1980,1650,1210,880,420][i], tier:c.tier, d:[0,+1,-1,+2,0,-2][i] }));
    $('#rankingTable').innerHTML = `<tr><th>#</th><th>${kind === 'atletas' ? 'Jogador' : 'Clube'}</th><th>Tier</th><th>Score_Total</th><th>Δ</th></tr>` + rows.map(r => `<tr><td class="pos">${r.i + 1}</td><td><div class="who"><span class="avatar" data-initials="${r.ini}"><i class="dot dc-${r.r}"></i></span><div><b>${esc(r.n)}</b><br><small>${esc(r.meta)}</small></div></div></td><td>${chip(r.tier)}</td><td><b class="mono">${r.score.toLocaleString('pt-BR')}</b></td><td class="delta ${r.d > 0 ? 'up' : r.d < 0 ? 'down' : ''}">${r.d > 0 ? '▲' + r.d : r.d < 0 ? '▼' + -r.d : '—'}</td></tr>`).join('');
    $$('.seg-mini button').forEach(b => b.classList.toggle('active', b.dataset.rank === kind));
  }
  $$('.seg-mini button').forEach(b => b.addEventListener('click', () => renderRanking(b.dataset.rank)));

  /* ---------- Explorar: globo ortográfico em SVG ---------- */
  function project(lat, lng, rot, R = 190, cx = 210, cy = 210) {
    const φ = lat * Math.PI / 180, λ = (lng + rot) * Math.PI / 180, φ0 = -15 * Math.PI / 180;
    const cosc = Math.sin(φ0) * Math.sin(φ) + Math.cos(φ0) * Math.cos(φ) * Math.cos(λ); if (cosc < 0) return null;
    return { x: cx + R * Math.cos(φ) * Math.sin(λ), y: cy - R * (Math.cos(φ0) * Math.sin(φ) - Math.sin(φ0) * Math.cos(φ) * Math.cos(λ)) };
  }
  function drawGlobe(rot) {
    let g = `<defs><radialGradient id="gg" cx="35%" cy="30%"><stop offset="0" stop-color="#1b3a2c"/><stop offset="1" stop-color="#0a0f0c"/></radialGradient></defs><circle cx="210" cy="210" r="190" fill="url(#gg)" stroke="#26332b"/>`;
    for (let lat = -60; lat <= 60; lat += 30) { let d = ''; for (let lng = -180; lng <= 180; lng += 5) { const p = project(lat, lng, rot); if (p) d += (d ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1); } g += `<path d="${d}" fill="none" stroke="rgba(0,212,122,.12)"/>`; }
    for (let lng = -180; lng < 180; lng += 30) { let d = ''; for (let lat = -90; lat <= 90; lat += 5) { const p = project(lat, lng, rot); if (p) d += (d ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1); } g += `<path d="${d}" fill="none" stroke="rgba(0,212,122,.12)"/>`; }
    clubs.forEach(c => { const p = project(c.lat, c.lng, rot); if (!p) return; const col = TIER_COLOR[c.tier]; g += `<g><circle cx="${p.x}" cy="${p.y}" r="9" fill="${col}" opacity=".25"/><circle cx="${p.x}" cy="${p.y}" r="4.5" fill="${col}" stroke="#eaf2ee" stroke-width="1"/><text x="${p.x + 8}" y="${p.y + 4}" font-size="9" font-family="JetBrains Mono,monospace" fill="#eaf2ee">${c.c}</text></g>`; });
    $('#globe').innerHTML = g;
  }
  $('#globeRot').addEventListener('input', e => drawGlobe(+e.target.value));
  function renderClubs() {
    const co = $('#exCountry').value, dv = $('#exDiv').value, tm = $('#exTier').value; const order = ['COMMON','RARE','EPIC','LEGENDARY','MYTHIC'];
    $('#clubList').innerHTML = clubs.filter(c => (!co || c.c === co) && (!dv || String(c.d) === dv) && (!tm || order.indexOf(c.tier) >= order.indexOf(tm.replace('+', '')))).map((c, i) => `<li><span class="avatar" data-initials="C${i + 1}"><i class="dot dc-clube"></i></span><div><b>${esc(c.n)}</b><small class="mono">${c.c} · DIV ${c.d} · </small>${chip(c.tier)}</div></li>`).join('') || '<li>Nenhum clube.</li>';
  }
  ['#exCountry', '#exDiv', '#exTier'].forEach(s => $(s).addEventListener('change', renderClubs));

  /* ---------- gráficos do Gêmeo ---------- */
  const line = (pts, color, w = 2.5) => `<polyline points="${pts.map(p => p.join(',')).join(' ')}" fill="none" stroke="${color}" stroke-width="${w}" stroke-linejoin="round"/>`;
  function drawEvolution() {
    const scores = [820, 900, 980, 1040, 1180, 1210, 1300, 1420, 1500, 1580, 1610, 1650]; const W = 640, H = 220, pad = 30;
    const x = i => pad + i * (W - 2 * pad) / 11, y = v => H - pad - (v / 3000) * (H - 2 * pad); let s = '';
    [[0,400,'#5c6b62','COMMON'],[401,900,'#3b82f6','RARE'],[901,1600,'#a06bff','EPIC'],[1601,2400,'#f5c542','LEGEND.'],[2401,3000,'#ffe08a','MYTHIC']].forEach(([a, b, c, l]) => { s += `<rect x="${pad}" y="${y(b)}" width="${W - 2 * pad}" height="${y(a) - y(b)}" fill="${c}" opacity=".08"/><text x="${W - pad + 2}" y="${y(a) - 2}" font-size="8" font-family="JetBrains Mono,monospace" fill="${c}">${l}</text>`; });
    s += line(scores.map((v, i) => [x(i), y(v)]), '#f5c542') + scores.map((v, i) => `<circle cx="${x(i)}" cy="${y(v)}" r="3.5" fill="#f5c542"/><text x="${x(i)}" y="${H - 10}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#63756a">W${32 + i}</text>`).join('');
    s += `<circle cx="${x(7)}" cy="${y(1420)}" r="8" fill="none" stroke="#a06bff"/><text x="${x(7)}" y="${y(1420) - 12}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#e2d3ff">EPIC → LEGENDARY (W39)</text>`;
    $('#evoChart').innerHTML = s;
    $('#milestones').innerHTML = ['W35 · RARE → EPIC', 'W37 · 🏆 MVP', 'W39 · EPIC → LEGENDARY', 'W41 · 🔥 STREAK 5', 'W43 · NOTA DO TREINADOR'].map(m => `<li>${m}</li>`).join('');
  }
  function drawGauge(v) { const a = Math.PI * (1 - v / 100), x = 60 + 50 * Math.cos(a), y = 62 - 50 * Math.sin(a); $('#readiness').innerHTML = `<path d="M10 62 A50 50 0 0 1 110 62" fill="none" stroke="#26332b" stroke-width="10" stroke-linecap="round"/><path d="M10 62 A50 50 0 0 1 ${x} ${y}" fill="none" stroke="#00d47a" stroke-width="10" stroke-linecap="round"/>`; }
  function drawLoad() {
    const acute = [420, 510, 380, 600, 450, 520, 504], chronic = 470; const W = 640, H = 160, pad = 24, bw = 56;
    $('#loadChart').innerHTML = acute.map((v, i) => `<rect x="${pad + i * 86}" y="${H - pad - v / 5}" width="${bw}" height="${v / 5}" rx="6" fill="${v > chronic * 1.5 ? '#ff5d5d' : '#00d47a'}" opacity=".8"/><text x="${pad + i * 86 + bw / 2}" y="${H - 8}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#63756a">${['S','T','Q','Q','S','S','D'][i]}</text>`).join('')
      + `<line x1="${pad}" x2="${W - pad}" y1="${H - pad - chronic / 5}" y2="${H - pad - chronic / 5}" stroke="#f5c542" stroke-dasharray="4 4"/><text x="${W - pad}" y="${H - pad - chronic / 5 - 4}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="end" fill="#f5c542">CARGA CRÔNICA 28 D</text>`;
  }
  function drawRadar(vals) {
    const labels = ['ATAQUE', 'TÉCNICA', 'TÁTICA', 'DEFESA', 'CRIATIV.']; const cx = 160, cy = 160, R = 110; const pt = (i, r) => [cx + r * Math.sin(i * 2 * Math.PI / 5), cy - r * Math.cos(i * 2 * Math.PI / 5)];
    let s = ''; [25, 50, 75, 100].forEach(r => s += `<polygon points="${[0,1,2,3,4].map(i => pt(i, R * r / 100).join(',')).join(' ')}" fill="none" stroke="#26332b"/>`);
    s += `<polygon points="${vals.map((v, i) => pt(i, R * v / 100).join(',')).join(' ')}" fill="rgba(0,212,122,.3)" stroke="#00d47a" stroke-width="2"/>`;
    labels.forEach((l, i) => { const [x, y] = pt(i, R + 22); s += `<text x="${x}" y="${y}" font-size="9" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#eaf2ee">${l} <tspan fill="#95a89d">${vals[i]}</tspan></text>`; });
    $('#radar').innerHTML = s;
  }
  function renderProvador() {
    const pos = $('#provPos').value, form = $('#provForm').value; const fit = { MEIA:87, PONTA:74, VOLANTE:69, ATACANTE:61 }[pos] - (form === '3-5-2' ? 4 : 0);
    $('#fitPct').textContent = fit + '%'; $('#fitPct').nextElementSibling.textContent = `fit para ${pos} no ${form}`;
    $('#gaps').innerHTML = [['Ataque', 88, 80], ['Técnica', 84, 85], ['Tática', 72, 80], ['Defesa', 55, pos === 'VOLANTE' ? 75 : 50], ['Criatividade', 80, 85]].map(([l, v, req]) => `<li><span>${l}</span><i style="--w:${v}%"></i><span class="mono" style="color:${v >= req ? '#6cffc0' : '#ff5d5d'}">${v}/${req}</span></li>`).join('');
  }
  ['#provPos', '#provForm'].forEach(s => $(s).addEventListener('change', renderProvador));

  /* ---------- FLAIR ---------- */
  const fcard = (a, cap) => { const power = Math.round(a.score * TIER_MULT[a.tier] * (cap ? 1.2 : 1)); return `<div class="fcard rarity-${a.tier.toLowerCase()}" style="--cc:${TIER_COLOR[a.tier]}"><div class="fin">${cap ? '<span class="cap">C</span>' : ''}<span class="avatar" data-initials="${a.ini}"><i class="dot dc-atleta"></i></span><b class="n">${esc(a.name)}</b><small style="text-align:center">${a.pos} · ${chip(a.tier)}</small><div class="axes3"><span>PP ${a.s[0]}</span><span>RP ${a.s[1]}</span><span>MP ${a.s[2]}</span></div><div class="power">${power.toLocaleString('pt-BR')}</div></div></div>`; };
  function renderFlair() { $('#deckMe').innerHTML = [athletes[0], athletes[2], athletes[3]].map((a, i) => fcard(a, i === 0)).join(''); $('#deckOp').innerHTML = [athletes[1], athletes[4], athletes[3]].map((a, i) => fcard(a, i === 0)).join(''); }
  $$('.axes button').forEach(b => b.addEventListener('click', () => { $$('.axes button').forEach(x => x.classList.toggle('primary', x === b)); toast(`EIXO: ${b.textContent}`); }));
  $('#snapBtn').addEventListener('click', () => toast('⚡ SNAP! PONTOS EM DISPUTA DOBRADOS'));

  /* ---------- páginas simples ---------- */
  function renderMisc(win, sub, params) {
    const pages = {
      buscar: ['Buscar', `<label class="filter-form">Buscar atletas, clubes, ligas <input type="search" value="${esc(params.q || '')}" placeholder="Ex.: lateral sub-20 Série B"></label><p class="hint">Eixo "Nome" do sistema de filtros — cruza Perfil, Clubes &amp; Ligas e Momentos.</p>`],
      cards: ['Meus cards', '<p class="hint">Seus posts publicados, agrupados por tipo (borda) e feed.</p><div class="samples">' + posts.gemeo.map(postCard).join('') + '</div>'],
      me: ['Meu perfil', vcard(athletes[0]) + '<p class="hint" style="margin-top:12px">Visibilidade atual: <b>SOMENTE EU</b> (Privacy by Default). Altere em Privacidade (LGPD).</p>'],
      'me/historico': ['Histórico de atividades', '<ul class="milestones"><li>HOJE · LOGIN (CHROME · &lt;PREENCHER_CIDADE&gt;)</li><li>ONTEM · PUBLICOU TREINO</li><li>W42 · shareTelemetryWithClubs: false → true</li><li>W41 · EXPORTAÇÃO CONCLUÍDA</li></ul><span class="art">Art. 18, I e II</span>'],
      cupons: ['Cupons', '<p class="hint">Disponíveis apenas com <code>shareWithBrands = true</code>. Nenhum cupom ativo.</p>'],
      pontos: ['Pontos', '<div class="kpis"><div class="kpi"><small>Saldo</small><b>1.240</b></div><div class="kpi"><small>Esta semana</small><b>+180</b></div></div><p class="hint">Ganhos por treinos publicados, avaliações e assiduidade. Não compráveis com dinheiro.</p>'],
      'gestao/liga': ['Minha liga', '<p class="hint">Homologação de scouts, tabela, súmulas e punições — só <b>OFICIAL_LIGA</b>.</p>'],
      admin: ['Administração', '<p class="hint">Moderação, versões de termos Velion, auditoria LGPD e registro de papéis por agentes.</p>'],
    };
    const key = sub ? `${win}/${sub}` : win; const [t, body] = pages[key] || pages[win] || ['Página', '<p>Em construção.</p>'];
    $('#miscPanel').innerHTML = `<h3>${t}</h3>${body}`;
  }

  /* ---------- composer + Copilot ---------- */
  function openComposer(withCopilot) {
    $('#composer').hidden = false;
    $$('.type-opt[data-roles]').forEach(l => l.hidden = !l.dataset.roles.split(',').includes(ROLE));
    $('#copilot').hidden = !withCopilot;
    $('#copilotBody').innerHTML = `<div>Tipo sugerido: <b>TREINO</b> (borda verde) — detectei uma sessão de 72 min no seu wearable.</div><div>Título: "Sessão de sprints — 25 km/h sustentados"</div><div>Snippet: "+38 PP registrados pelo wearable · 72 min · 8,4 km · RPE 7."</div><small class="mono">GERADO POR IA · VOCÊ REVISA ANTES DE PUBLICAR (LGPD ART. 20) · SEM DADOS DE SAÚDE</small>`;
  }
  $('#fabBtn').addEventListener('click', () => openComposer(true));
  $$('[data-close]').forEach(b => b.addEventListener('click', () => $('#' + b.dataset.close).hidden = true));
  $('#cpAccept').addEventListener('click', () => { $('#composerTitle').value = 'Sessão de sprints — 25 km/h sustentados'; $('#composerSub').value = '+38 PP registrados pelo wearable · 72 min · 8,4 km · RPE 7.'; toast('SUGESTÃO ACEITA'); });
  $('#cpEdit').addEventListener('click', () => { $('#composerTitle').value = 'Sessão de sprints — 25 km/h sustentados'; $('#composerTitle').focus(); });
  $('#cpDismiss').addEventListener('click', () => $('#copilot').hidden = true);
  $('#publishBtn').addEventListener('click', () => { const ct = $('input[name="ct"]:checked').value; const title = $('#composerTitle').value || '(sem título)', snip = $('#composerSub').value; if (!$('#composerTitle').value) { toast('TÍTULO É OBRIGATÓRIO'); return; } posts.gemeo.unshift({ role:ROLE, name:'@<preencher_user>', ct, top:'AGORA', title, snip, trend:'➖ novo' }); $('#composer').hidden = true; location.hash = ct === 'TREINO' || ct === 'PARTIDA_MOMENTO' ? '#/gemeo' : '#/perfil'; renderSub(state.win, state.sub[state.win]); toast('CARD PUBLICADO · BORDA ' + (CT[ct] || CT.TREINO).badge); });

  /* ---------- LGPD ---------- */
  function openLgpd() { $('#lgpdModal').hidden = false; }
  $('#btnLgpd').addEventListener('click', openLgpd); $('#navLgpd').addEventListener('click', e => { e.preventDefault(); openLgpd(); });
  $$('.lgpd-nav button').forEach(b => b.addEventListener('click', () => { $$('.lgpd-nav button').forEach(x => x.classList.toggle('active', x === b)); $$('.lgpd-panel').forEach(p => p.hidden = p.dataset.lgpd !== b.dataset.lgpd); }));
  $('#lgpdToggles').innerHTML = lgpdToggles.map(([k, t, d, art, s]) => `<li class="toggle-row"><span><b>${t}</b><small>${d}</small><span class="art ${s ? 's' : ''}">${art}</span></span><input type="checkbox" class="sw" data-pref="${k}"></li>`).join('');
  $$('#lgpdToggles .sw').forEach(sw => sw.addEventListener('change', () => toast(`${sw.dataset.pref}: ${!sw.checked} → ${sw.checked} · CONSENT AUDIT LOG`)));
  $('#darkToggle').addEventListener('change', e => document.documentElement.dataset.theme = e.target.checked ? 'dark' : 'light');
  $('#exportBtn').addEventListener('click', () => toast('EXPORTAÇÃO SOLICITADA (202) · LINK VÁLIDO POR 7 DIAS'));
  $('#delConfirm').addEventListener('input', e => $('#deleteBtn').disabled = e.target.value !== 'EXCLUIR');
  $('#deleteBtn').addEventListener('click', () => toast('REAUTENTIQUE-SE · PENDING_DELETE POR 30 DIAS'));

  /* ---------- sidebar por papel, menu, hide-on-scroll, swipe ---------- */
  $$('.sidebar a[data-roles]').forEach(a => a.hidden = !a.dataset.roles.split(',').includes(ROLE) && ROLE !== 'ADMIN');
  $('#btnMore').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
  $('#searchForm').addEventListener('submit', e => { e.preventDefault(); location.hash = '#/buscar?q=' + encodeURIComponent($('#searchInput').value); });
  let lastY = 0; window.addEventListener('scroll', () => { const y = window.scrollY; $('#bottomNav').classList.toggle('hidden', y > lastY + 24 && y > 80); if (y < lastY - 4 || y < 40) $('#bottomNav').classList.remove('hidden'); lastY = y; }, { passive: true });
  let x0 = null; document.addEventListener('touchstart', e => x0 = e.touches[0].clientX, { passive: true });
  document.addEventListener('touchend', e => { if (x0 === null) return; const dx = e.changedTouches[0].clientX - x0; x0 = null; if (Math.abs(dx) < 70 || !WINDOWS.includes(state.win)) return; const i = WINDOWS.indexOf(state.win) + (dx < 0 ? 1 : -1); if (WINDOWS[i]) location.hash = '#/' + WINDOWS[i]; });
  document.addEventListener('click', e => {
    const act = e.target.closest('.act[data-act]');
    if (act) {
      const n = act.querySelector('b'); const kind = act.dataset.act;
      if (kind === 'like') { act.classList.toggle('on'); if (n) n.textContent = (parseInt(n.textContent.replace(/\./g, '')) + (act.classList.contains('on') ? 1 : -1)).toLocaleString('pt-BR'); }
      else if (kind === 'comment') toast('ABRIR COMENTÁRIOS · POST VIEW');
      else if (kind === 'share') { if (n) n.textContent = (parseInt(n.textContent.replace(/\./g, '')) + 1).toLocaleString('pt-BR'); toast('COMPARTILHADO · LINK COPIADO'); }
      else if (kind === 'save') { act.classList.toggle('on'); toast(act.classList.contains('on') ? 'SALVO' : 'REMOVIDO DOS SALVOS'); }
      return;
    }
    const b = e.target.closest('.micro b[data-badge]'); if (b) toast(`TAP TO EXPAND · ${b.dataset.badge} · GET /v1/athletes/{id}/badges/{type}/history`); });

  $$('.sorttabs').forEach(st => st.addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; $$('button', st).forEach(x => x.classList.toggle('on', x === b)); toast(`ORDENAR · ${b.textContent.toUpperCase()}`); }));

  function toast(msg) { const t = $('#toast'); t.textContent = msg; t.hidden = false; clearTimeout(t._t); t._t = setTimeout(() => t.hidden = true, 2600); }

  window.addEventListener('hashchange', route);
  route();
})();
