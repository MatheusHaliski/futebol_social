/* =====================================================================
   Football AI · Social — SPA mock
   - roteamento por hash: #/{janela}/{sub}?{filtros}
   - 4 janelas (PERFIL · GEMEO · SCOUTING · ALERTA), cada uma com painel de
     filtros próprio que vira query string do endpoint GET /v1/feeds/{WINDOW}
   - bottom nav com hide-on-scroll, sidebar por papel, composer + Copilot,
     modal LGPD, FLAIR, Explorar (globo SVG)
   ===================================================================== */
(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];

  /* ---------- dados mock (pessoais como <PREENCHER_*>) ---------- */
  const ROLE = document.body.dataset.role || 'ATLETA';
  const RING = { ATLETA:'atleta', STAFF:'staff', STAFF_SAUDE:'saude', CLUBE:'clube', SCOUT:'scout', AGENTE:'agente', MARCA:'marca', IMPRENSA:'imprensa', OFICIAL_LIGA:'liga', TORCEDOR:'torcedor', AGENTE_IA:'ia', ARBITRO:'arbitro', TORCIDA_ORGANIZADA:'organizada' };
  const CT = { TREINO:['treino','TREINO'], PARTIDA_MOMENTO:['partida','PARTIDA / MOMENTO'], RELATORIO_TATICO:['tatico','RELATÓRIO TÁTICO'], INSTITUCIONAL:['inst','INSTITUCIONAL'], LESAO_ALERTA:['lesao','LESÃO / ALERTA'] };
  const SEV = { BAIXA:'var(--sev-baixa)', MEDIA:'var(--sev-media)', ALTA:'var(--sev-alta)', CRITICA:'var(--sev-critica)' };

  const athletes = [
    { id:'ath-1', name:'<PREENCHER_ATLETA_1>', ini:'A1', role:'ATLETA', pos:'MEIA', age:22, club:'<PREENCHER_CLUBE_1>', status:'CONTRATADO', tier:'LEGENDARY', score:1650, s:[82,88,76], badges:[['⚡','Pronto','100% físico','green'],['⚽ 14','Artilheiro','temporada',''],['🏆 3','MVP','acumulado','']] },
    { id:'ath-2', name:'<PREENCHER_ATLETA_2>', ini:'A2', role:'ATLETA', pos:'ATACANTE', age:19, club:'<PREENCHER_CLUBE_2>', status:'AGENTE_LIVRE', tier:'MYTHIC', score:2480, s:[91,93,89], badges:[['🏆 7','MVP','acumulado',''],['⚽ 21','Artilheiro','temporada',''],['🔥 6','Em chamas','streak','']], mvp:true },
    { id:'ath-3', name:'<PREENCHER_ATLETA_3>', ini:'A3', role:'ATLETA', pos:'ZAGUEIRO', age:27, club:'<PREENCHER_CLUBE_1>', status:'CONTRATADO', tier:'EPIC', score:1120, s:[78,80,70], badges:[['⚠️','Risco fadiga','ACWR 1,6','yellow'],['🧱 9','Xerifão','desarmes',''],['🛡️','Fair play','0 vermelhos','']] },
    { id:'ath-4', name:'<PREENCHER_ATLETA_4>', ini:'A4', role:'ATLETA', pos:'GOLEIRO', age:31, club:'<PREENCHER_CLUBE_3>', status:'CONTRATADO', tier:'RARE', score:640, s:[60,72,55], badges:[['🧱 5','Paredão','clean sheets',''],['⏱️ 1.980','Motorzinho','minutos',''],['👑','Capitão','liderança','']] },
    { id:'ath-5', name:'<PREENCHER_ATLETA_5>', ini:'A5', role:'ATLETA', pos:'LATERAL', age:17, club:'<PREENCHER_CLUBE_2>', status:'AGENTE_LIVRE', tier:'COMMON', score:310, s:[52,44,38], badges:[['⚡','Pronto','100% físico','green'],['⏱️ 420','Motorzinho','minutos','']] },
  ];
  const posts = {
    perfil: [
      { role:'ATLETA', name:'<PREENCHER_ATLETA_2>', ct:'PARTIDA_MOMENTO', text:'Gol aos 78\' — finalização de fora da área. Momento #0412 disponível para coleção.', stats:['❤️ 241','💬 18','↗ 9'] },
      { role:'IMPRENSA', name:'<PREENCHER_VEICULO>', ct:'PARTIDA_MOMENTO', text:'Análise: a jovem promessa sub-20 que saltou de EPIC para LEGENDARY em 3 semanas.', stats:['❤️ 97','💬 12'] },
      { role:'SCOUT', name:'<PREENCHER_SCOUT>', ct:'RELATORIO_TATICO', text:'Shortlist da rodada: 5 laterais com passe > 85 e idade ≤ 21. Relatório homologado.', stats:['❤️ 54','🔖 31'] },
    ],
    gemeo: [
      { role:'ATLETA', name:'<PREENCHER_NOME>', ct:'TREINO', text:'Sessão de 72 min · 8,4 km · RPE 7 · carga 504 AU · velocidade máx. 31,2 km/h', stats:['Física','Treino','W43'] },
      { role:'STAFF', name:'<PREENCHER_PREPARADOR>', ct:'TREINO', text:'Treino coletivo: 6 × 4 min de jogo reduzido, foco em transição defensiva.', stats:['Tática','Treino'] },
      { role:'ATLETA', name:'<PREENCHER_NOME>', ct:'PARTIDA_MOMENTO', text:'Partida oficial · 90 min · nota 7,8 · 1 assistência · 11,2 km percorridos', stats:['Técnica','Partida'] },
    ],
    scouting: [
      { role:'STAFF', name:'<PREENCHER_TECNICO>', ct:'RELATORIO_TATICO', text:'4-3-3 com pressão alta — drill de saída de bola sob pressão (prancheta anexa).', stats:['Homologado','12 execuções'] },
      { role:'SCOUT', name:'<PREENCHER_SCOUT>', ct:'RELATORIO_TATICO', text:'Observação: velocidade 84 · passe 88 · tomada de decisão acima da média para a idade.', stats:['Homologado','Velocidade > 80'] },
      { role:'TORCEDOR', name:'<PREENCHER_TORCEDOR>', ct:'RELATORIO_TATICO', text:'Opinião da arquibancada: o time cresce quando o meia recua para construir.', stats:['Comunidade · opinativa'] },
    ],
    alerta: [
      { role:'CLUBE', name:'<PREENCHER_CLUBE_1>', ct:'LESAO_ALERTA', text:'Boletim médico: atleta #10 — lesão grau 2 na coxa, retorno previsto em 3 semanas.', sev:'ALTA', type:'LESAO' },
      { role:'OFICIAL_LIGA', name:'<PREENCHER_FEDERACAO>', ct:'INSTITUCIONAL', text:'Punição: suspensão de 2 jogos após expulsão na rodada 31.', sev:'MEDIA', type:'PUNICAO' },
      { role:'IMPRENSA', name:'<PREENCHER_VEICULO>', ct:'INSTITUCIONAL', text:'Transferência: proposta oficial recebida por lateral sub-20 (janela fecha em 9 dias).', sev:'BAIXA', type:'TRANSFERENCIA' },
      { role:'CLUBE', name:'<PREENCHER_CLUBE_1>', ct:'INSTITUCIONAL', text:'Comunicado oficial: abertura de peneira sub-20 — inscrições até sexta.', sev:'BAIXA', type:'INSTITUCIONAL' },
      { role:'AGENTE_IA', name:'Resumo semanal · IA', ct:'INSTITUCIONAL', text:'3 atletas do seu clube mudaram de tier esta semana (2 ↑, 1 ↓). Operado por <PREENCHER_STAFF>.', sev:'BAIXA', type:'INSTITUCIONAL', ai:true },
    ],
    mural: [
      { role:'CLUBE', name:'<PREENCHER_CLUBE>', ct:'INSTITUCIONAL', text:'📌 Fixado · Convocação para o amistoso de sábado — lista no vestiário digital.', stats:['Elenco','📌'] },
      { role:'CLUBE', name:'<PREENCHER_CLUBE>', ct:'INSTITUCIONAL', text:'Enquete para sócios: nova terceira camisa — vote até domingo.', stats:['Sócios','🗳️ 1.204 votos'] },
      { role:'STAFF', name:'<PREENCHER_TECNICO>', ct:'RELATORIO_TATICO', text:'Resumo tático da semana para o elenco.', stats:['Elenco'] },
    ],
    saude: [ { role:'STAFF_SAUDE', name:'<PREENCHER_MEDICO>', ct:'LESAO_ALERTA', text:'🔒 Só você e o departamento médico veem isto. Reavaliação da coxa direita agendada.', stats:['Privado · Art. 11'] } ],
    autopilot: [
      { role:'AGENTE_IA', name:'Autopilot · rascunho', ct:'TREINO', text:'Rascunho do wearable: 64 min · 7,9 km · RPE estimado 6. Publicar como TREINO? (sem dados de saúde)', stats:['Rascunho privado','Revisar'], ai:true },
      { role:'AGENTE_IA', name:'Autopilot · rascunho', ct:'PARTIDA_MOMENTO', text:'Rascunho da súmula: 90 min jogados · 1 assistência · nota 7,8.', stats:['Rascunho privado','Revisar'], ai:true },
    ],
    momentos: [ { role:'ATLETA', name:'<PREENCHER_ATLETA_2>', ct:'PARTIDA_MOMENTO', text:'🎬 Momento #0412 · edição 1/50 · gol de fora da área', stats:['Coleção','FLAIR'] } ],
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
    ['shareMedicalData','Status de saúde e lesões','Badges 🚑/⚠️ e boletins visíveis a terceiros','Art. 11 · Dado sensível',true],
    ['shareBiometricData','Reconhecimento facial em vídeos (Lens)','Permite identificar seu rosto em análises de vídeo','Art. 11 · Dado sensível',true],
    ['locationHistory','Histórico de localização (GPS de treinos)','Grava o trajeto das sessões','Art. 7º, I · Consentimento revogável',false],
    ['shareWithBrands','Compartilhar com marcas parceiras','Ativações e cupons de patrocinadores','Art. 18, IX · Revogação',false],
    ['personalizedAds','Anúncios personalizados','Usa seu histórico para escolher anúncios','Art. 7º, I · Consentimento revogável',false],
    ['aiAgentProcessing','Processamento por agentes de IA','Copilot, Autopilot e resumos automáticos','Art. 7º, I · Art. 20 · revisão humana',false],
  ];

  /* ---------- templates ---------- */
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' }[c]));
  const tierPill = t => `<span class="tier-pill tier-${t.toLowerCase()}-pill">${t}</span>`;
  const postCard = p => {
    const [cls, label] = CT[p.ct]; const r = RING[p.role] || 'atleta';
    return `<article class="post ${cls}">
      <div class="meta"><span class="avatar ring ring-${r}" data-initials="${esc(p.name.replace(/[<>_]/g,'').slice(0,2).toUpperCase())}"></span>
        <span class="who"><b>${esc(p.name)}</b><small><i class="dot dot-${r}"></i>${p.role.replace('_',' ')}${p.ai ? ' <span class="ai">IA</span>' : ''}</small></span>
        ${p.sev ? `<span class="sev" style="--sevc:${SEV[p.sev]}">${p.sev}</span>` : ''}<span class="ct">${label}</span></div>
      <p>${esc(p.text)}</p>${p.stats ? `<div class="stats">${p.stats.map(s=>`<span>${esc(s)}</span>`).join('')}</div>` : ''}</article>`;
  };
  const athleteCard = a => `<article class="acard tier-${a.tier.toLowerCase()}" style="--ring:var(--ring-${RING[a.role]})" data-id="${a.id}">
      ${a.mvp ? '<div class="mvp">★ MVP DA RODADA ★</div>' : ''}
      <div class="head"><span class="avatar ring ring-${RING[a.role]}" data-initials="${a.ini}"></span>
        <div class="who"><b>${esc(a.name)}</b><small><i class="dot dot-${RING[a.role]}"></i>${a.role} · ${a.pos} · ${a.age} anos · ${esc(a.club)}</small><small>${tierPill(a.tier)} <span>${a.status==='AGENTE_LIVRE'?'Agente livre':'Contratado'}</span></small></div>
        <div class="score"><b>${a.score.toLocaleString('pt-BR')}</b><small>SCORE SEMANAL</small></div></div>
      <div class="badges">${a.badges.map(b=>`<button class="badge ${b[3]}" data-badge="${esc(b[1])}"><b>${b[0]}</b>${b[1]}<small>${b[2]}</small></button>`).join('')}</div>
      <div class="stats3"><div class="stat"><small>Físico 30%</small><b>${a.s[0]}</b><i style="--w:${a.s[0]}%"></i></div><div class="stat"><small>Técnico 40%</small><b>${a.s[1]}</b><i style="--w:${a.s[1]}%"></i></div><div class="stat"><small>Partidas 30%</small><b>${a.s[2]}</b><i style="--w:${a.s[2]}%"></i></div></div>
      <div class="actions"><a class="btn primary" href="#/gemeo?athleteId=${a.id}">Ver Gêmeo</a><a class="btn" href="#/scouting/comparar?compare=${a.id},ath-2">Comparar</a><button class="btn">Seguir</button></div></article>`;

  /* ---------- estado e roteamento ---------- */
  const WINDOWS = ['perfil','gemeo','scouting','alerta'];
  const state = { win:'perfil', sub:{ perfil:'feed', gemeo:'telemetria', scouting:'pranchetas', alerta:'tudo' }, params:{} };
  const DEFAULT_SUB = { perfil:'feed', gemeo:'telemetria', scouting:'pranchetas', alerta:'tudo' };

  function parseHash() {
    const h = location.hash.replace(/^#\/?/, '') || 'perfil';
    const [path, qs] = h.split('?');
    const [win, sub] = path.split('/');
    return { win, sub, params: Object.fromEntries(new URLSearchParams(qs || '')) };
  }
  function route() {
    const { win, sub, params } = parseHash();
    state.params = params;
    $$('.window').forEach(w => w.hidden = true);
    $('#sidebar').classList.remove('open');
    if (WINDOWS.includes(win)) {
      state.win = win; state.sub[win] = sub || DEFAULT_SUB[win];
      $(`#win-${win}`).hidden = false;
      $$(`.segments[data-for="${win}"] a`).forEach(a => a.classList.toggle('active', a.dataset.sub === state.sub[win]));
      $$(`#win-${win} .subview`).forEach(v => v.hidden = v.dataset.sub !== state.sub[win]);
      $(`#win-${win} .win-body`).classList.toggle('no-filters', !['feed','telemetria','evolucao','pranchetas','relatorios','tudo','mural','ranking'].includes(state.sub[win]));
      applyParamsToForm(win, params); updatePreview(win); renderSub(win, state.sub[win]);
    } else if (win === 'flair') { $('#win-flair').hidden = false; renderFlair(params.mode); }
    else if (win === 'novo') { openComposer(params.copilot === '1'); location.hash = '#/perfil'; return; }
    else if (win === 'configuracoes') { $('#win-perfil').hidden = false; openLgpd(); }
    else { $('#win-misc').hidden = false; renderMisc(win, sub, params); }
    $$('.bottom-nav a').forEach(a => a.classList.toggle('active', a.dataset.win === win));
    const navKey = `${win}/${sub || DEFAULT_SUB[win] || ''}`.replace(/\/$/, '');
    $$('.sidebar a').forEach(a => a.classList.toggle('active', a.dataset.nav === navKey || a.dataset.nav === win));
    window.scrollTo({ top: 0 });
  }

  /* ---------- filtros → query string ---------- */
  function formToParams(win) {
    const fd = new FormData($(`#form-${win}`)); const p = new URLSearchParams();
    const multi = {};
    for (const [k, v] of fd.entries()) {
      if (v === '' || k.startsWith('op-')) continue;
      if (k.startsWith('attr-')) { const op = fd.get('op-' + k.slice(5)) || 'gt'; p.append(`attr[${k.slice(5)}]`, `${op}:${v}`); continue; }
      (multi[k] ||= []).push(v);
    }
    if (win === 'scouting' && fd.get('compareA') && fd.get('compareB')) { p.set('compare', `${fd.get('compareA')},${fd.get('compareB')}`); delete multi.compareA; delete multi.compareB; }
    delete multi.compareA; delete multi.compareB;
    Object.entries(multi).forEach(([k, vs]) => p.set(k, vs.join(',')));
    return p;
  }
  function updatePreview(win) {
    const qs = formToParams(win).toString();
    $(`[data-preview="${win}"]`).textContent = `GET /v1/feeds/${win.toUpperCase()}${qs ? '?' + decodeURIComponent(qs) : ''}`;
  }
  function applyParamsToForm(win, params) {
    const f = $(`#form-${win}`); if (!f) return;
    Object.entries(params).forEach(([k, v]) => {
      if (k === 'compare') { const [a, b] = v.split(','); if (f.compareA) { f.compareA.value = a || ''; f.compareB.value = b || ''; } return; }
      const m = k.match(/^attr\[(\w+)\]$/); if (m) { const [op, val] = v.split(':'); if (f[`attr-${m[1]}`]) { f[`attr-${m[1]}`].value = val; f[`op-${m[1]}`].value = op; } return; }
      const els = f.querySelectorAll(`[name="${k}"]`); if (!els.length) return;
      const vals = v.split(',');
      els.forEach(el => { if (el.type === 'checkbox' || el.type === 'radio') el.checked = vals.includes(el.value); else el.value = v; });
    });
  }
  WINDOWS.forEach(win => {
    const f = $(`#form-${win}`);
    f.addEventListener('input', () => updatePreview(win));
    f.addEventListener('submit', e => { e.preventDefault(); const qs = formToParams(win).toString(); location.hash = `#/${win}/${state.sub[win]}${qs ? '?' + qs : ''}`; toast(`Filtros aplicados · ${win.toUpperCase()}`); });
    f.addEventListener('reset', () => setTimeout(() => { updatePreview(win); location.hash = `#/${win}/${state.sub[win]}`; }, 0));
  });
  function setFilters(win, open) {
    const b = $(`.filters-toggle[data-toggle="${win}"]`); b.setAttribute('aria-expanded', String(open));
    $(`#filters-${win}`).classList.toggle('collapsed', !open);
    $(`#win-${win} .win-body`).classList.toggle('filters-collapsed', !open);
  }
  $$('.filters-toggle').forEach(b => b.addEventListener('click', () => setFilters(b.dataset.toggle, b.getAttribute('aria-expanded') !== 'true')));
  // mobile: filtros nascem recolhidos para maximizar a área de leitura
  if (window.matchMedia('(max-width: 860px)').matches) WINDOWS.forEach(w => setFilters(w, false));

  /* ---------- render por janela/sub-aba ---------- */
  function filteredAthletes(p) {
    return athletes.filter(a => (!p.tier || p.tier.split(',').includes(a.tier)) && (!p.position || a.pos === p.position)
      && (!p.ageMin || a.age >= +p.ageMin) && (!p.ageMax || a.age <= +p.ageMax) && (!p.contractStatus || a.status === p.contractStatus));
  }
  function renderSub(win, sub) {
    const p = state.params;
    if (win === 'perfil') {
      if (sub === 'feed') { const list = filteredAthletes(p); $('#feed-perfil').innerHTML = list.map(athleteCard).join('') + posts.perfil.map(postCard).join('') || '<p class="hint">Nenhum atleta com esses filtros.</p>'; }
      if (sub === 'ranking') renderRanking('atletas');
      if (sub === 'explorar') { drawGlobe(+$('#globeRot').value); renderClubs(); }
      if (sub === 'momentos') $('#feed-momentos').innerHTML = posts.momentos.map(postCard).join('');
    }
    if (win === 'gemeo') {
      if (sub === 'telemetria') { const ev = p.eventType; $('#feed-gemeo').innerHTML = posts.gemeo.filter(x => !ev || (ev === 'TREINO' ? x.ct === 'TREINO' : x.ct === 'PARTIDA_MOMENTO')).map(postCard).join(''); }
      if (sub === 'evolucao') drawEvolution();
      if (sub === 'carga') { drawGauge(78); drawLoad(); }
      if (sub === 'dna') drawRadar([88, 84, 72, 55, 80]);
      if (sub === 'comparador') $('#compareTwin').innerHTML = [athletes[0], athletes[1]].map(athleteCard).join('');
      if (sub === 'provador') renderProvador();
      if (sub === 'autopilot') $('#feed-autopilot').innerHTML = posts.autopilot.map(postCard).join('');
    }
    if (win === 'scouting') {
      if (sub === 'pranchetas') { const src = p.source; $('#feed-scouting').innerHTML = posts.scouting.filter(x => !src || (src === 'HOMOLOGADO' ? x.role !== 'TORCEDOR' : x.role === 'TORCEDOR')).map(postCard).join(''); }
      if (sub === 'comparar') { const ids = (p.compare || 'ath-1,ath-2').split(','); $('#compareScout').innerHTML = ids.map(id => athletes.find(a => a.id === id)).filter(Boolean).map(athleteCard).join(''); }
      if (sub === 'relatorios') $('#feed-relatorios').innerHTML = posts.scouting.map(postCard).join('');
    }
    if (win === 'alerta') {
      if (sub === 'tudo') { const sev = p.severity?.split(','), ty = p.alertType?.split(','), so = p.source?.split(','); $('#feed-alerta').innerHTML = posts.alerta.filter(x => (!sev || sev.includes(x.sev)) && (!ty || ty.includes(x.type)) && (!so || so.includes(x.role))).map(postCard).join('') || '<p class="hint">Nenhum alerta com esses filtros.</p>'; }
      if (sub === 'mural') $('#feed-mural').innerHTML = posts.mural.map(postCard).join('');
      if (sub === 'saude') $('#feed-saude').innerHTML = posts.saude.map(postCard).join('');
      if (sub === 'dm') $('#dmList').innerHTML = ['<PREENCHER_TECNICO>|Convocação confirmada para sábado?|staff', '<PREENCHER_SCOUT>|Posso incluir você na shortlist sub-23?|scout', '<PREENCHER_CLUBE>|Renovação: proposta enviada.|clube'].map(s => { const [n, t, r] = s.split('|'); return `<li><span class="avatar ring ring-${r}" data-initials="${n.replace(/[<>_]/g,'').slice(0,2)}"></span><div><b>${esc(n)}</b><small>${esc(t)}</small></div><small>agora</small></li>`; }).join('');
    }
  }
  function renderRanking(kind) {
    const rows = kind === 'atletas' ? [...athletes].sort((a, b) => b.score - a.score).map((a, i) => ({ i, n:a.name, ini:a.ini, r:'atleta', meta:`${a.pos} · ${a.age} · ${a.club}`, score:a.score, tier:a.tier, d:[+2,0,-1,+1,0][i] }))
      : clubs.slice(0, 6).map((c, i) => ({ i, n:c.n, ini:'C' + (i + 1), r:'clube', meta:`${c.c} · Divisão ${c.d}`, score:[2310,1980,1650,1210,880,420][i], tier:c.tier, d:[0,+1,-1,+2,0,-2][i] }));
    $('#rankingTable').innerHTML = `<tr><th>#</th><th>${kind === 'atletas' ? 'Jogador' : 'Clube'}</th><th>Tier</th><th>Score</th><th>Δ</th></tr>` + rows.map(r => `<tr><td class="pos">${r.i + 1}</td><td><div class="who"><span class="avatar ring ring-${r.r}" data-initials="${r.ini}"></span><div><b>${esc(r.n)}</b><br><small>${esc(r.meta)}</small></div></div></td><td>${tierPill(r.tier)}</td><td><b>${r.score.toLocaleString('pt-BR')}</b></td><td class="delta ${r.d > 0 ? 'up' : r.d < 0 ? 'down' : ''}">${r.d > 0 ? '▲' + r.d : r.d < 0 ? '▼' + -r.d : '—'}</td></tr>`).join('');
    $$('.seg-mini button').forEach(b => b.classList.toggle('active', b.dataset.rank === kind));
  }
  $$('.seg-mini button').forEach(b => b.addEventListener('click', () => renderRanking(b.dataset.rank)));

  /* ---------- Explorar: globo ortográfico em SVG ---------- */
  const TIER_COLOR = { COMMON:'#9AA3B2', RARE:'#3B82F6', EPIC:'#A855F7', LEGENDARY:'#F5C542', MYTHIC:'#FF4D6D' };
  function project(lat, lng, rot, R = 190, cx = 210, cy = 210) {
    const φ = lat * Math.PI / 180, λ = (lng + rot) * Math.PI / 180, φ0 = -15 * Math.PI / 180;
    const cosc = Math.sin(φ0) * Math.sin(φ) + Math.cos(φ0) * Math.cos(φ) * Math.cos(λ);
    if (cosc < 0) return null;
    return { x: cx + R * Math.cos(φ) * Math.sin(λ), y: cy - R * (Math.cos(φ0) * Math.sin(φ) - Math.sin(φ0) * Math.cos(φ) * Math.cos(λ)) };
  }
  function drawGlobe(rot) {
    let g = `<defs><radialGradient id="gg" cx="35%" cy="30%"><stop offset="0" stop-color="#2f4a7a"/><stop offset="1" stop-color="#0b1326"/></radialGradient></defs><circle cx="210" cy="210" r="190" fill="url(#gg)" stroke="#3b4f8a"/>`;
    for (let lat = -60; lat <= 60; lat += 30) { let d = ''; for (let lng = -180; lng <= 180; lng += 5) { const p = project(lat, lng, rot); if (p) d += (d ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1); else d += ' '; } g += `<path d="${d.replace(/ +M/g,'M')}" fill="none" stroke="rgba(255,255,255,.08)"/>`; }
    for (let lng = -180; lng < 180; lng += 30) { let d = ''; for (let lat = -90; lat <= 90; lat += 5) { const p = project(lat, lng, rot); if (p) d += (d ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1); } g += `<path d="${d}" fill="none" stroke="rgba(255,255,255,.08)"/>`; }
    clubs.forEach(c => { const p = project(c.lat, c.lng, rot); if (!p) return; g += `<g class="club"><circle cx="${p.x}" cy="${p.y}" r="9" fill="${TIER_COLOR[c.tier]}" opacity=".25"/><circle cx="${p.x}" cy="${p.y}" r="4.5" fill="${TIER_COLOR[c.tier]}" stroke="#fff" stroke-width="1"/><text x="${p.x + 8}" y="${p.y + 4}" font-size="9" fill="#E8EAF0">${c.c}</text></g>`; });
    $('#globe').innerHTML = g;
  }
  $('#globeRot').addEventListener('input', e => drawGlobe(+e.target.value));
  function renderClubs() {
    const co = $('#exCountry').value, dv = $('#exDiv').value, tm = $('#exTier').value; const order = ['COMMON','RARE','EPIC','LEGENDARY','MYTHIC'];
    $('#clubList').innerHTML = clubs.filter(c => (!co || c.c === co) && (!dv || String(c.d) === dv) && (!tm || order.indexOf(c.tier) >= order.indexOf(tm.replace('+', '')))).map((c, i) => `<li><span class="avatar ring ring-clube" data-initials="C${i + 1}"></span><div><b>${esc(c.n)}</b><small>${c.c} · Divisão ${c.d} · plantel ${tierPill(c.tier)}</small></div></li>`).join('') || '<li>Nenhum clube.</li>';
  }
  ['#exCountry', '#exDiv', '#exTier'].forEach(s => $(s).addEventListener('change', renderClubs));

  /* ---------- gráficos do Gêmeo ---------- */
  const line = (pts, color, w = 2.5) => `<polyline points="${pts.map(p => p.join(',')).join(' ')}" fill="none" stroke="${color}" stroke-width="${w}" stroke-linejoin="round"/>`;
  function drawEvolution() {
    const scores = [820, 900, 980, 1040, 1180, 1210, 1300, 1420, 1500, 1580, 1610, 1650]; const W = 640, H = 220, pad = 30;
    const x = i => pad + i * (W - 2 * pad) / 11, y = v => H - pad - (v / 3000) * (H - 2 * pad);
    let s = '';
    [[0,400,'#9AA3B2'],[401,900,'#3B82F6'],[901,1600,'#A855F7'],[1601,2400,'#F5C542'],[2401,3000,'#FF4D6D']].forEach(([a, b, c]) => { s += `<rect x="${pad}" y="${y(b)}" width="${W - 2 * pad}" height="${y(a) - y(b)}" fill="${c}" opacity=".08"/><text x="${W - pad + 2}" y="${y(a) - 2}" font-size="9" fill="${c}">${b === 3000 ? 'MYTHIC' : b === 2400 ? 'LEGEND.' : b === 1600 ? 'EPIC' : b === 900 ? 'RARE' : 'COMMON'}</text>`; });
    s += line(scores.map((v, i) => [x(i), y(v)]), '#F5C542') + scores.map((v, i) => `<circle cx="${x(i)}" cy="${y(v)}" r="3.5" fill="#F5C542"/><text x="${x(i)}" y="${H - 10}" font-size="9" text-anchor="middle" fill="#9AA3B2">W${32 + i}</text>`).join('');
    s += `<g><circle cx="${x(7)}" cy="${y(1420)}" r="8" fill="none" stroke="#A855F7"/><text x="${x(7)}" y="${y(1420) - 12}" font-size="9" text-anchor="middle" fill="#D8B4FE">EPIC → LEG. (W39)</text></g>`;
    $('#evoChart').innerHTML = s;
    $('#milestones').innerHTML = ['W35 · RARE → EPIC', 'W37 · 🏆 MVP', 'W39 · EPIC → LEGENDARY', 'W41 · 🔥 streak 5', 'W43 · nota do treinador: "melhor mês"'].map(m => `<li>${m}</li>`).join('');
  }
  function drawGauge(v) {
    const a = Math.PI * (1 - v / 100), x = 60 + 50 * Math.cos(a), y = 62 - 50 * Math.sin(a);
    $('#readiness').innerHTML = `<path d="M10 62 A50 50 0 0 1 110 62" fill="none" stroke="#2A2F3A" stroke-width="10" stroke-linecap="round"/><path d="M10 62 A50 50 0 0 1 ${x} ${y}" fill="none" stroke="#15805C" stroke-width="10" stroke-linecap="round"/>`;
  }
  function drawLoad() {
    const acute = [420, 510, 380, 600, 450, 520, 504], chronic = 470; const W = 640, H = 160, pad = 24, bw = 56;
    $('#loadChart').innerHTML = acute.map((v, i) => `<rect x="${pad + i * 86}" y="${H - pad - v / 5}" width="${bw}" height="${v / 5}" rx="6" fill="${v > chronic * 1.5 ? '#C0376B' : '#15805C'}" opacity=".85"/><text x="${pad + i * 86 + bw / 2}" y="${H - 8}" font-size="9" text-anchor="middle" fill="#9AA3B2">${['S','T','Q','Q','S','S','D'][i]}</text>`).join('')
      + `<line x1="${pad}" x2="${W - pad}" y1="${H - pad - chronic / 5}" y2="${H - pad - chronic / 5}" stroke="#F5C542" stroke-dasharray="4 4"/><text x="${W - pad}" y="${H - pad - chronic / 5 - 4}" font-size="9" text-anchor="end" fill="#F5C542">carga crônica 28 d</text>`;
  }
  function drawRadar(vals) {
    const labels = ['Ataque', 'Técnica', 'Tática', 'Defesa', 'Criatividade']; const cx = 160, cy = 160, R = 110; const pt = (i, r) => [cx + r * Math.sin(i * 2 * Math.PI / 5), cy - r * Math.cos(i * 2 * Math.PI / 5)];
    let s = ''; [25, 50, 75, 100].forEach(r => s += `<polygon points="${[0,1,2,3,4].map(i => pt(i, R * r / 100).join(',')).join(' ')}" fill="none" stroke="#2A2F3A"/>`);
    s += `<polygon points="${vals.map((v, i) => pt(i, R * v / 100).join(',')).join(' ')}" fill="rgba(21,128,92,.35)" stroke="#15805C" stroke-width="2"/>`;
    labels.forEach((l, i) => { const [x, y] = pt(i, R + 22); s += `<text x="${x}" y="${y}" font-size="11" text-anchor="middle" fill="#E8EAF0">${l} <tspan fill="#9AA3B2">${vals[i]}</tspan></text>`; });
    $('#radar').innerHTML = s;
  }
  function renderProvador() {
    const pos = $('#provPos').value, form = $('#provForm').value; const fit = { MEIA:87, PONTA:74, VOLANTE:69, ATACANTE:61 }[pos] - (form === '3-5-2' ? 4 : 0);
    $('#fitPct').textContent = fit + '%'; $('#fitPct').nextElementSibling.textContent = `fit para ${pos} no ${form}`;
    $('#gaps').innerHTML = [['Ataque', 88, 80], ['Técnica', 84, 85], ['Tática', 72, 80], ['Defesa', 55, pos === 'VOLANTE' ? 75 : 50], ['Criatividade', 80, 85]].map(([l, v, req]) => `<li><span>${l}</span><i style="--w:${v}%"></i><span style="color:${v >= req ? '#6EE7B7' : '#F9A8D4'}">${v}/${req}</span></li>`).join('');
  }
  ['#provPos', '#provForm'].forEach(s => $(s).addEventListener('change', renderProvador));

  /* ---------- FLAIR ---------- */
  const fcard = (a, cap) => { const mult = { COMMON:1, RARE:1.05, EPIC:1.1, LEGENDARY:1.2, MYTHIC:1.3 }[a.tier]; const power = Math.round(a.score * mult * (cap ? 1.2 : 1)); return `<div class="fcard tier-${a.tier.toLowerCase()}">${cap ? '<span class="cap">C</span>' : ''}<span class="avatar ring ring-atleta" data-initials="${a.ini}"></span><b>${esc(a.name)}</b><small style="text-align:center">${a.pos} · ${tierPill(a.tier)}</small><div class="axes3"><span>F ${a.s[0]}</span><span>T ${a.s[1]}</span><span>P ${a.s[2]}</span></div><div class="power">${power.toLocaleString('pt-BR')}</div></div>`; };
  function renderFlair() { $('#deckMe').innerHTML = [athletes[0], athletes[2], athletes[3]].map((a, i) => fcard(a, i === 0)).join(''); $('#deckOp').innerHTML = [athletes[1], athletes[4], athletes[3]].map((a, i) => fcard(a, i === 0)).join(''); }
  $$('.axes button').forEach(b => b.addEventListener('click', () => { $$('.axes button').forEach(x => x.classList.toggle('primary', x === b)); toast(`Eixo escolhido: ${b.textContent}`); }));
  $('#snapBtn').addEventListener('click', () => toast('⚡ Snap! Pontos em disputa dobrados'));

  /* ---------- páginas simples ---------- */
  function renderMisc(win, sub, params) {
    const pages = {
      buscar: ['Buscar', `<label>Buscar atletas, clubes, ligas <input type="search" value="${esc(params.q || '')}" placeholder="Ex.: lateral sub-20 Série B"></label><p class="hint">Resultados consolidam PERFIL (cards), Clubes &amp; Ligas e Momentos.</p>`],
      cards: ['Meus cards', '<p>Seus posts publicados, agrupados por tipo (borda) e janela.</p>' + posts.gemeo.map(postCard).join('')],
      me: ['Meu perfil', athleteCard(athletes[0]) + '<p class="hint">Visibilidade atual: <b>SOMENTE EU</b> (Privacy by Default). Altere em Privacidade (LGPD).</p>'],
      'me/historico': ['Histórico de atividades', '<ul class="milestones"><li>Hoje · login (Chrome · <PREENCHER_CIDADE>)</li><li>Ontem · publicou TREINO</li><li>W42 · consentimento shareTelemetryWithClubs: false → true</li><li>W41 · exportação de dados concluída</li></ul><span class="art">Art. 18, I e II</span>'],
      cupons: ['Cupons', '<p class="hint">Disponíveis apenas com <code>shareWithBrands = true</code>. Nenhum cupom ativo.</p>'],
      pontos: ['Pontos', '<div class="kpis"><div class="kpi"><small>Saldo</small><b>1.240</b></div><div class="kpi"><small>Esta semana</small><b>+180</b></div></div><p class="hint">Ganhos por treinos publicados, avaliações e assiduidade. Não compráveis com dinheiro.</p>'],
      'gestao/liga': ['Minha liga', '<p>Homologação de scouts, tabela, súmulas e punições — só <b>OFICIAL_LIGA</b>.</p>'],
      admin: ['Administração', '<p>Moderação, versões de termos Velion, auditoria LGPD e registro de papéis por agentes.</p>'],
    };
    const key = sub ? `${win}/${sub}` : win; const [t, body] = pages[key] || pages[win] || ['Página', '<p>Em construção.</p>'];
    $('#miscPanel').innerHTML = `<h3>${t}</h3>${body}`;
  }

  /* ---------- composer + Copilot ---------- */
  function openComposer(withCopilot) {
    $('#composer').hidden = false;
    $$('.type-opt[data-roles]').forEach(l => l.hidden = !l.dataset.roles.split(',').includes(ROLE));
    const cp = $('#copilot'); cp.hidden = !withCopilot && !document.body.dataset.copilot;
    $('#copilotBody').innerHTML = `<div>Tipo sugerido: <b>TREINO</b> (borda verde) — detectei uma sessão de 72 min no seu wearable.</div><div>Texto: "Sessão de 72 min · 8,4 km · RPE 7 · carga 504 AU. Semana 43 fechando forte 💪"</div><div>Badges: ⚡ Pronto · 🔥 streak 5</div><small>Gerado por IA · você revisa antes de publicar (LGPD art. 20). Sem dados de saúde.</small>`;
  }
  $('#fabBtn').addEventListener('click', () => openComposer(true));
  $$('[data-close]').forEach(b => b.addEventListener('click', () => $('#' + b.dataset.close).hidden = true));
  $('#cpAccept').addEventListener('click', () => { $('#composerText').value = 'Sessão de 72 min · 8,4 km · RPE 7 · carga 504 AU. Semana 43 fechando forte 💪'; toast('Sugestão aceita'); });
  $('#cpEdit').addEventListener('click', () => { $('#composerText').value = 'Sessão de 72 min · 8,4 km · RPE 7 · carga 504 AU.'; $('#composerText').focus(); });
  $('#cpDismiss').addEventListener('click', () => $('#copilot').hidden = true);
  $('#publishBtn').addEventListener('click', () => { const ct = $('input[name="ct"]:checked').value; posts.gemeo.unshift({ role:ROLE, name:'<PREENCHER_NOME>', ct, text:$('#composerText').value || '(sem texto)', stats:['agora'] }); $('#composer').hidden = true; location.hash = ct === 'TREINO' ? '#/gemeo' : '#/perfil'; renderSub(state.win, state.sub[state.win]); toast('Card publicado · borda ' + CT[ct][1]); });

  /* ---------- LGPD ---------- */
  function openLgpd() { $('#lgpdModal').hidden = false; }
  $('#btnLgpd').addEventListener('click', openLgpd); $('#navLgpd').addEventListener('click', e => { e.preventDefault(); openLgpd(); });
  $$('.lgpd-nav button').forEach(b => b.addEventListener('click', () => { $$('.lgpd-nav button').forEach(x => x.classList.toggle('active', x === b)); $$('.lgpd-panel').forEach(p => p.hidden = p.dataset.lgpd !== b.dataset.lgpd); }));
  $('#lgpdToggles').innerHTML = lgpdToggles.map(([k, t, d, art, s]) => `<li class="toggle-row"><span><b>${t}</b><small>${d}</small><span class="art ${s ? 's' : ''}">${art}</span></span><input type="checkbox" class="sw" data-pref="${k}"></li>`).join('');
  $$('#lgpdToggles .sw').forEach(sw => sw.addEventListener('change', () => toast(`${sw.dataset.pref}: ${!sw.checked} → ${sw.checked} · ConsentAuditLog gravado`)));
  $('#darkToggle').addEventListener('change', e => document.documentElement.dataset.theme = e.target.checked ? 'dark' : 'light');
  $('#exportBtn').addEventListener('click', () => toast('Exportação solicitada (202) · JSON pronto em instantes, link válido por 7 dias'));
  $('#delConfirm').addEventListener('input', e => $('#deleteBtn').disabled = e.target.value !== 'EXCLUIR');
  $('#deleteBtn').addEventListener('click', () => toast('Reautentique-se · conta entrará em PENDING_DELETE por 30 dias'));

  /* ---------- sidebar por papel, menu, hide-on-scroll ---------- */
  $$('.sidebar a[data-roles]').forEach(a => a.hidden = !a.dataset.roles.split(',').includes(ROLE) && ROLE !== 'ADMIN');
  $('#btnMore').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
  $('#searchForm').addEventListener('submit', e => { e.preventDefault(); location.hash = '#/buscar?q=' + encodeURIComponent($('#searchInput').value); });
  let lastY = 0; window.addEventListener('scroll', () => { const y = window.scrollY; $('#bottomNav').classList.toggle('hidden', y > lastY + 24 && y > 80); if (y < lastY - 4 || y < 40) $('#bottomNav').classList.remove('hidden'); lastY = y; }, { passive: true });
  // swipe entre janelas (mobile)
  let x0 = null; document.addEventListener('touchstart', e => x0 = e.touches[0].clientX, { passive: true });
  document.addEventListener('touchend', e => { if (x0 === null) return; const dx = e.changedTouches[0].clientX - x0; x0 = null; if (Math.abs(dx) < 70 || !WINDOWS.includes(state.win)) return; const i = WINDOWS.indexOf(state.win) + (dx < 0 ? 1 : -1); if (WINDOWS[i]) location.hash = '#/' + WINDOWS[i]; });
  document.addEventListener('click', e => { const b = e.target.closest('.badge[data-badge]'); if (b) toast(`Tap to expand · histórico de "${b.dataset.badge}" (GET /v1/athletes/{id}/badges/{type}/history)`); });

  function toast(msg) { const t = $('#toast'); t.textContent = msg; t.hidden = false; clearTimeout(t._t); t._t = setTimeout(() => t.hidden = true, 2600); }

  window.addEventListener('hashchange', route);
  route();
})();
