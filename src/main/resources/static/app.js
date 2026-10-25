/* =====================================================================
   Football AI · Social — SPA
   Camadas: identidade (bolinha por papel) · conteúdo (borda por tipo) ·
   feed (4 janelas segmentadas) · raridade (brasão por tier, nunca texto)
   - roteamento por hash: #/{janela}/{sub}?{filtros}
   - painel de filtros por janela → GET /v1/feeds/{JANELA}?…
   - fonte de dados: API real (JWT + refresh) com fallback DEMO (mock) quando offline
   - criador de cards em 4 etapas: modo → conteúdo → arte → revisar
   ===================================================================== */
(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];

  /* ---------- camada 1 · identidade ---------- */
  let ROLE = document.body.dataset.role || 'ATLETA';
  const ROLES = {
    ATLETA:{ dc:'atleta', label:'atleta' }, CLUBE:{ dc:'clube', label:'clube formador' }, SCOUT:{ dc:'scout', label:'scout verificado' },
    MARCA:{ dc:'marca', label:'patrocinador' }, IMPRENSA:{ dc:'imprensa', label:'imprensa esportiva' }, OFICIAL_LIGA:{ dc:'oficial', label:'conta oficial' },
    STAFF:{ dc:'staff', label:'staff técnico' }, STAFF_SAUDE:{ dc:'saude', label:'dep. médico' }, AGENTE:{ dc:'agente', label:'agente' },
    TORCEDOR:{ dc:'torcedor', label:'torcedor' }, AGENTE_IA:{ dc:'ia', label:'agente IA' }, ARBITRO:{ dc:'arbitro', label:'árbitro' }, TORCIDA_ORGANIZADA:{ dc:'torcida', label:'torcida' },
  };
  /* ---------- camada 2 · conteúdo (7 bordas) ---------- */
  const CT = {
    TREINO:{ cc:'treino', badge:'TREINO' }, PARTIDA_MOMENTO:{ cc:'partida', badge:'PARTIDA' }, RELATORIO_TATICO:{ cc:'scouting', badge:'SCOUTING' },
    LIGA:{ cc:'liga', badge:'LIGA' }, INSTITUCIONAL:{ cc:'institucional', badge:'INSTITUCIONAL' }, LESAO_ALERTA:{ cc:'institucional', badge:'LESÃO' }, ESPECIAL:{ cc:'especial', badge:'ESPECIAL' },
  };
  /* ---------- camada 4 · raridade ---------- */
  const TIER_MULT = { COMMON:1, RARE:1.05, EPIC:1.1, LEGENDARY:1.2, MYTHIC:1.3 };
  const TIER_COLOR = { COMMON:'#5c6b62', RARE:'#3b82f6', EPIC:'#a06bff', LEGENDARY:'#f5c542', MYTHIC:'#f5c542' };
  const TIER_LABEL = { COMMON:'Common', RARE:'Rare', EPIC:'Epic', LEGENDARY:'Legendary', MYTHIC:'Mythic' };

  /* =====================================================================
     API client — JWT access (15 min) + refresh (7 d) com rotação; fallback DEMO
     ===================================================================== */
  const API = {
    base: location.protocol.startsWith('http') ? '' : 'http://localhost:8080',
    online: false, user: null, terms: null,
    get access() { return sessionStorage.getItem('fas.access'); },
    get refresh() { return sessionStorage.getItem('fas.refresh'); },
    setTokens(a, r) { sessionStorage.setItem('fas.access', a); if (r) sessionStorage.setItem('fas.refresh', r); },
    clear() { sessionStorage.removeItem('fas.access'); sessionStorage.removeItem('fas.refresh'); this.user = null; },
    async raw(path, { method = 'GET', headers = {}, body } = {}) {
      const ctl = new AbortController(); const t = setTimeout(() => ctl.abort(), 8000);
      try { return await fetch(this.base + path, { method, headers, body, signal: ctl.signal }); } finally { clearTimeout(t); }
    },
    async call(path, { method = 'GET', body, headers = {}, auth = true, retry = true } = {}) {
      const h = { Accept: 'application/json', ...headers };
      if (body !== undefined) h['Content-Type'] = 'application/json';
      if (auth && this.access) h.Authorization = 'Bearer ' + this.access;
      const res = await this.raw(path, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
      if (res.status === 401 && auth && retry && this.refresh && (await this.refreshTokens())) return this.call(path, { method, body, headers, auth, retry: false });
      if (res.status === 204) return null;
      const text = await res.text(); let data = null;
      try { data = text ? JSON.parse(text) : null; } catch { data = { detail: text }; }
      if (!res.ok) { const e = new Error((data && (data.detail || data.title)) || ('HTTP ' + res.status)); e.status = res.status; e.code = data && data.code; e.data = data; throw e; }
      return data;
    },
    async refreshTokens() {
      try { const d = await this.call('/v1/auth/refresh', { method: 'POST', body: { refreshToken: this.refresh }, auth: false, retry: false }); this.setTokens(d.accessToken, d.refreshToken); return true; }
      catch { this.clear(); return false; }
    },
    async probe() { try { const r = await this.raw('/v1/ui/navigation'); this.online = r.ok; } catch { this.online = false; } return this.online; },
  };
  const live = () => API.online && !!API.user;                 // dados reais?
  const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' }[c]));
  const fmt = n => (n ?? 0).toLocaleString('pt-BR');
  const ini = n => String(n || '??').replace(/[<>_]/g, ' ').trim().split(/\s+/).map(w => w[0]).join('').slice(0, 2).toUpperCase() || '??';

  /* ---------- dados mock (pessoais como <PREENCHER_*>) ---------- */
  const athletes = [
    { id:'ath-1', name:'<PREENCHER_ATLETA_1>', handle:'@<preencher_1>', ini:'A1', role:'ATLETA', pos:'Meia', age:22, foot:'D', height:176, state:'PR', club:'<PREENCHER_CLUBE_1>', status:'CONTRATADO', tier:'LEGENDARY', score:1650, pp:1240, rp:780, s:[82,88,76], sub:'Referência estadual · Top 10% do ranking por posição', badges:[['⚡ PRONTO','green'],['⚽ 14','' ],['🏆 3','']], season:{games:22,goals:14,assists:7,minutes:1980,yellow:3,red:0}, phys:{readiness:78,acwr:1.21,status:'READY'}, likes:241, comments:18, shares:9 },
    { id:'ath-2', name:'<PREENCHER_ATLETA_2>', handle:'@<preencher_2>', ini:'A2', role:'ATLETA', pos:'Atacante', age:19, foot:'E', height:181, state:'SP', club:'<PREENCHER_CLUBE_2>', status:'AGENTE_LIVRE', tier:'MYTHIC', score:2480, pp:1480, rp:912, s:[91,93,89], sub:'Referência nacional · Top 1%, candidato à Seleção Football AI', badges:[['🏆 7',''],['⚽ 21',''],['🔥 6','']], momentum:1.6, season:{games:24,goals:21,assists:9,minutes:2110,yellow:2,red:0}, phys:{readiness:91,acwr:1.08,status:'READY'}, likes:1240, comments:96, shares:58 },
    { id:'ath-3', name:'<PREENCHER_ATLETA_3>', handle:'@<preencher_3>', ini:'A3', role:'ATLETA', pos:'Zagueiro', age:27, foot:'D', height:189, state:'PR', club:'<PREENCHER_CLUBE_1>', status:'CONTRATADO', tier:'EPIC', score:1120, pp:980, rp:520, s:[78,80,70], sub:'Sequência de atuações de destaque · Momentum 1,4×', badges:[['⚠️ FADIGA','yellow'],['🧱 9',''],['🛡️ FAIR','']], season:{games:20,goals:2,assists:1,minutes:1750,yellow:5,red:0,tackles:9}, phys:{readiness:52,acwr:1.62,status:'FATIGUE'}, likes:88, comments:7, shares:2 },
    { id:'ath-4', name:'<PREENCHER_ATLETA_4>', handle:'@<preencher_4>', ini:'A4', role:'ATLETA', pos:'Goleiro', age:31, foot:'D', height:192, state:'RS', club:'<PREENCHER_CLUBE_3>', status:'CONTRATADO', tier:'RARE', score:640, pp:520, rp:210, s:[60,72,55], sub:'Boa consistência ao longo de várias semanas de treino', badges:[['🧱 5',''],['⏱️ 1.980',''],['👑 CAP','']], season:{games:23,goals:0,assists:0,minutes:1980,yellow:1,red:0,cleanSheets:5}, phys:{readiness:66,acwr:1.15,status:'INJURED',injury:{type:'Lesão grau 2 · coxa',ret:'3 sem'}}, likes:54, comments:4, shares:1 },
    { id:'ath-5', name:'<PREENCHER_ATLETA_5>', handle:'@<preencher_5>', ini:'A5', role:'ATLETA', pos:'Lateral', age:17, foot:'E', height:172, state:'SP', club:'<PREENCHER_CLUBE_2>', status:'AGENTE_LIVRE', tier:'COMMON', score:310, pp:180, rp:60, s:[52,44,38], sub:'Gêmeo iniciante — poucos dados registrados ainda', badges:[['⚡ PRONTO','green'],['⏱️ 420','']], season:{games:6,goals:1,assists:0,minutes:420,yellow:0,red:0}, phys:{readiness:84,acwr:0.95,status:'READY'}, likes:12, comments:1, shares:0 },
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


  /* ---------- adaptadores DTO → forma usada pelos templates ---------- */
  const TREND = { HOT:'🔥 em alta', UP:'📈 trending', FLAT:null, DOWN:'📉 em queda' };
  const fromPost = d => {
    const alert = !!d.hazard;
    const alarm = d.contentType === 'LESAO_ALERTA' ? '🩹 LESÃO' : d.alertType === 'PUNICAO' ? '⚖️ PUNIÇÃO' : d.alertType === 'TRANSFERENCIA' ? '🔁 TRANSFERÊNCIA' : d.alertType === 'LESAO' ? '🩹 LESÃO' : '📣 ' + (d.badgeLabel || 'ALERTA');
    return { id:d.id, authorId:d.author.id, mine: !!(API.user && API.user.id === d.author.id), role:d.author.role, name:d.author.handle, ai:d.author.aiGenerated, verified:d.author.verified, ct:d.contentType, top:d.timeLabel, title:d.title, snip:d.subtitle,
      media:d.media ? (d.media.kind === 'video' ? 'VÍDEO' : 'IMAGEM') : null, mediaUrl:d.media && d.media.url, mediaKind:d.media && d.media.kind, meta:d.meta && d.meta.length ? d.meta : null, likes:d.social.likes, comments:d.social.comments, shares:d.social.shares,
      liked:d.social.likedByMe, saved:d.social.savedByMe, trend:d.trend ? TREND[d.trend.direction] : null, alert, alarm: alert ? alarm : null, sev:d.severity, type:d.alertType,
      age:[d.ageRating.cssClass, d.ageRating.label], art:d.art, gen:d.generationMode, window:d.window };
  };
  const PHYS_MAP = { READY:'READY', FATIGUE_RISK:'FATIGUE', INJURED:'INJURED' };
  const fromAthlete = d => {
    const r = d.rarity || {}, ph = d.physical, attrs = d.attributes || [];
    const find = k => (attrs.find(a => a.kind === k) || {}).label;
    return { id:d.athleteId, name:d.name, handle:d.handle, ini:ini(d.name), role:'ATLETA', pos:find('pos') || '—', age:parseInt(find('age')) || null, club:find('club') || '', state:find('state'),
      status:attrs.some(a => a.kind === 'free') ? 'AGENTE_LIVRE' : 'CONTRATADO', tier:r.tier || 'COMMON', score:r.score || 0, pp:r.pp || 0, rp:r.rp || 0, s:[Math.round((r.pp || 0) / 15), Math.round((r.rp || 0) / 10), r.mp || 0],
      attrs:attrs.map(a => [a.icon, a.label, a.kind]), badges:(d.topBadges || []).map(b => [b.label, b.type === 'PRONTO_PRA_JOGO' ? 'green' : b.type === 'DEPARTAMENTO_MEDICO' ? 'red' : b.type === 'RISCO_FADIGA' ? 'yellow' : '']),
      season:d.season, avgRating:d.season && d.season.avgRating, phys: ph ? { readiness:ph.readiness, acwr:ph.acwr, status:PHYS_MAP[ph.status] || 'READY', injury: ph.injury ? { type:ph.injury.type, ret: ph.injury.expectedReturn || '—' } : null } : null,
      physPrivate: !ph, verified:d.author && d.author.verified, likes:d.social.likes, comments:d.social.comments, shares:d.social.shares, sub:'' };
  };
  const fromFlairCard = c => ({ id:c.id, name:c.name, pos:c.position ? c.position[0] + c.position.slice(1).toLowerCase() : '—', ini:ini(c.name), tier:c.tier, score:c.scoreTotal, s:[c.pp, c.rp, c.mp], power:c.power, captain:c.captain });

  /* ---------- templates ---------- */
  const chip = t => { const k = String(t || 'COMMON').toUpperCase(); return `<svg class="ic tier t-${k.toLowerCase()}" role="img" aria-label="Tier ${TIER_LABEL[k] || k}" data-tier="${k}"><title>Tier ${TIER_LABEL[k] || k}</title><use href="#ic-tier-${k.toLowerCase()}"/></svg>`; };
  const roleOf = r => ROLES[r] || ROLES.ATLETA;
  /* arte = atributos do PRÓPRIO card (tinge o fundo); a fotografia é outro campo (upload do usuário) */
  const artAttrs = art => art && (art.color || art.material) ? ` data-material="${esc(art.material || 'FOSCO')}" data-style="${esc(art.style || 'GRAFITE')}" data-layout="${esc(art.layout || 'CLASSICO')}" data-art-label="${esc([art.materialLabel || art.material, art.layoutLabel || art.layout, art.styleLabel || art.style].filter(Boolean).join(' · '))}" style="--ac:${esc(art.color || '#00d47a')}"` : '';
  const mediaBlock = p => p.mediaUrl ? `<div class="p-media photo">${p.mediaKind === 'video' ? `<video src="${esc(p.mediaUrl)}" muted playsinline></video>` : `<img src="${esc(p.mediaUrl)}" alt="" loading="lazy">`}</div>` : p.media ? `<div class="p-media">${esc(p.media)}</div>` : '';

  /* PostCard — borda esquerda = tipo · bolinha = papel · arte opcional · ações sociais sempre */
  const postCard = p => {
    const likes = p.likes ?? 0, comments = p.comments ?? 0, shares = p.shares ?? 0;
    const r = roleOf(p.role), c = CT[p.ct] || CT.TREINO;
    const VSL = { treino:'◇', partida:'◇', scouting:'▭', liga:'⬢', institucional:'▭', especial:'✦' };
    const age = p.lock || p.ct === 'LESAO_ALERTA' ? ['age-sens','SENSÍVEL'] : p.age || ['age-safe','SAFE'];
    const topRight = p.alert ? `<span class="alarm">${esc(p.alarm || '📣 ALERTA')}</span>` : `<span class="badge cc">${c.badge}</span>`;
    const hasArt = !!(p.art && (p.art.color || p.art.material));
    let body = `<div class="p-title">${esc(p.title)}</div>`;
    if (p.media || p.mediaUrl) body += mediaBlock(p);            // post com fotografia: imagem + autor/título, sem subtítulo
    else if (p.snip) body += `<div class="p-sub">${esc(p.snip)}</div>`;  // post de texto: teaser (≤ 2 linhas)
    if (p.meta) body += `<div class="p-meta">${p.meta.map(m => `<span class="badge">${esc(m)}</span>`).join('')}</div>`;
    if (p.gen === 'AI' || p.ai) body += `<div class="p-meta"><span class="badge">GERADO COM IA · REVISADO</span></div>`;
    if (p.lock) body += `<span class="lock">🔒 DADO SENSÍVEL · ART. 11</span>`;
    const top = `<div class="p-top"><span class="time">${esc(p.top || '')}</span><span class="vsl" title="VSL">${VSL[c.cc]}</span>${topRight}<span class="age-chip ${age[0]}">${age[1]}</span></div>`;
    return `<article class="pcard cc-${c.cc}${p.alert ? ' alertcard' : ''}${hasArt ? ' has-art' : ''}" data-id="${esc(p.id || '')}"${hasArt ? artAttrs(p.art) : ''}>
      ${top}
      <div class="p-author"><span class="dot sm dc-${r.dc}"></span><span class="p-name">${esc(p.name)} <span>· ${r.label}</span>${p.verified ? '<span class="verified">✦</span>' : ''}${p.ai ? '<span class="ai">IA</span>' : ''}</span></div>
      ${body}
      <div class="actbar">
        <button class="act like${p.liked ? ' on' : ''}" data-act="like"><span class="i">♡</span>Curtir<b>${fmt(likes)}</b></button>
        <button class="act" data-act="comment"><span class="i">💬</span>Comentar<b>${fmt(comments)}</b></button>
        <button class="act" data-act="share"><span class="i">↗</span>Compartilhar<b>${fmt(shares)}</b></button>
        <button class="act save${p.saved ? ' on' : ''}" data-act="save" aria-label="salvar"><span class="i">🔖</span></button>
        ${p.mine ? '<button class="act" data-act="edit" title="Editar no estúdio"><span class="i">✎</span>Editar</button>' : ''}
        ${p.trend ? `<span class="trend${/💠|✦/.test(p.trend) ? ' gold' : /📉|➖/.test(p.trend) ? ' down' : ''}">${esc(p.trend)}</span>` : ''}
      </div>
    </article>`;
  };

  /* Card de perfil / atleta (.vcard) — hero limpo · janela Temporada · Atributos · badges na área inferior */
  const PHYS = { READY:['⚡','PRONTO PRA JOGO','ok'], FATIGUE:['⚠️','RISCO DE FADIGA','warn'], INJURED:['🩹','DEPARTAMENTO MÉDICO','bad'] };
  const vcard = (a, opts = {}) => {
    const r = roleOf(a.role), t = (a.tier || 'COMMON').toLowerCase();
    const cc = a.tier === 'COMMON' || !a.tier ? '#5c6b62' : TIER_COLOR[a.tier];
    const bdg = a.badges && a.badges.length ? `<div class="bdgrow"><small>BADGES</small>${a.badges.slice(0, 3).map(b => `<b class="${b[1]}" data-badge="${esc(b[0])}">${esc(b[0])}</b>`).join('')}</div>` : '';
    const title = esc(a.name);
    const refs = String(a.sub || '').split(' · ').map(x => x.trim()).filter(Boolean);
    const attrItems = a.attrs ? a.attrs : a.pos ? [
      ['◎', a.pos, 'pos'], ['', `${a.age} anos`, ''], ['⬢', a.club, 'club'],
      ['', a.status === 'AGENTE_LIVRE' ? 'Agente livre' : 'Contratado', a.status === 'AGENTE_LIVRE' ? 'free' : 'signed'],
      a.state ? ['', a.state, ''] : null, a.foot ? ['', `Pé ${a.foot}`, ''] : null, a.height ? ['', `${a.height} cm`, ''] : null,
      ...refs.map(x => ['✦', x, 'ref']),
    ].filter(Boolean) : refs.map(x => ['', x, '']);
    const attrs = `<ul class="attrs">${attrItems.map(([i, x, c]) => `<li class="${c || ''}">${i ? `<span class="i">${i}</span>` : ''}${esc(x)}</li>`).join('')}</ul>`;
    let data = '';
    if (a.season) {
      const sz = a.season, ph = a.phys || {}, st = PHYS[ph.status] || PHYS.READY;
      const gk = sz.cleanSheets !== undefined && sz.cleanSheets !== null, df = sz.tackles !== undefined && sz.tackles !== null;
      const nota = a.avgRating != null ? Number(a.avgRating).toFixed(1) : a.s ? (6.2 + (a.s[1] / 40)).toFixed(1) : '—';
      const physBlock = a.physPrivate ? `<div class="phys priv"><div class="pl"><small>CONDICIONAMENTO</small><span>🔒 privado por padrão · visível só com <b>shareMedicalData</b> (Art. 11)</span></div></div>`
        : `<div class="phys ${st[2]}">
        <div class="pl"><small>CONDICIONAMENTO</small><div class="rd"><i style="--w:${ph.readiness || 0}%"></i></div><span>prontidão <b>${ph.readiness ?? '—'}%</b> · ACWR <b>${Number(ph.acwr ?? 0).toLocaleString('pt-BR')}</b></span></div>
        <div class="pst"><span class="i">${st[0]}</span><b>${st[1]}</b>${ph.injury ? `<small>${esc(ph.injury.type)} · retorno ${esc(ph.injury.ret)}</small>` : ''}</div>
      </div>`;
      data += `<div class="vtabs" role="tablist"><button type="button" class="vt on" role="tab" data-vt="temporada">Temporada</button><button type="button" class="vt" role="tab" data-vt="atributos">Atributos</button></div>
      <div class="vpane" data-vt="temporada"><div class="sgrid">
        <div class="stile"><small>JOGOS</small><b>${sz.games}</b></div>
        <div class="stile"><small>${gk ? 'CLEAN SHEETS' : 'GOLS'}</small><b>${gk ? sz.cleanSheets : sz.goals}</b></div>
        <div class="stile"><small>${df ? 'DESARMES' : 'ASSIST.'}</small><b>${df ? sz.tackles : sz.assists}</b></div>
        <div class="stile"><small>MINUTOS</small><b>${fmt(sz.minutes)}</b></div>
        <div class="stile"><small>CARTÕES</small><b><span class="yc">${sz.yellow}</span> <span class="rc">${sz.red}</span></b></div>
        <div class="stile"><small>NOTA MÉDIA</small><b>${nota}</b></div>
      </div>${physBlock}</div>
      <div class="vpane" data-vt="atributos" hidden>${attrs}</div>`;
    } else data = attrs;
    const stats = a.pp !== undefined
      ? `<span class="pp">PP <b>${fmt(a.pp)}</b></span><span class="rp">RP <b>${fmt(a.rp)}</b></span><span class="mp">MP <b>${a.s ? a.s[2] : '—'}</b></span><span class="st">SCORE <b>${fmt(a.score)}</b></span>`
      : (a.stats || []).map(([k, v]) => `<span class="mp">${k} <b>${v}</b></span>`).join('');
    const actions = opts.actions === false ? '' : `<div class="actbar">
        <button class="act like" data-act="like"><span class="i">♡</span>Curtir<b>${fmt(a.likes)}</b></button>
        <button class="act" data-act="comment"><span class="i">💬</span>Comentar<b>${fmt(a.comments)}</b></button>
        <button class="act" data-act="share"><span class="i">↗</span>Compartilhar<b>${fmt(a.shares)}</b></button>
        <a class="act go" href="#/gemeo/evolucao?athleteId=${esc(a.id)}">Gêmeo →</a>
      </div>`;
    const sens = a.phys && a.phys.status === 'INJURED';
    return `<article class="vcard rarity-${t}${opts.wide ? ' wide' : ''}" style="--cc:${cc}" data-id="${esc(a.id)}" data-athlete="1">
      <div class="outer"><div class="inner">
        <div class="ctop"><span class="l"><span class="vsl">${a.pos ? '◇' : '⬢'}</span>${a.pos ? 'GÊMEO DIGITAL' : (a.meta || 'PERFIL')}</span>${sens ? '<span class="age-chip age-sens">SENSÍVEL</span>' : '<span class="age-chip age-safe">SAFE</span>'}${chip(a.tier || 'COMMON')}</div>
        <div class="imgblock"><svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/></svg></div>
        <div class="author"><span class="dot dc-${r.dc}"></span><span class="handle"><b>${esc(a.handle)}</b> <span>· ${r.label}</span>${a.verified ? '<span class="verified">✦</span>' : ''}</span></div>
        <div class="vtitle">${title}</div>
        ${data}
        ${bdg}
        <div class="statrow">${stats}</div>
        ${actions}
      </div></div></article>`;
  };

  /* ---------- estado e roteamento ---------- */
  const WINDOWS = ['perfil','gemeo','scouting','alerta'];
  const DEFAULT_SUB = { perfil:'feed', gemeo:'telemetria', scouting:'pranchetas', alerta:'tudo' };
  const state = { win:'perfil', sub:{ ...DEFAULT_SUB }, params:{} };
  const FILTERED_SUBS = ['feed','telemetria','pranchetas','tudo'];
  const toast = msg => { const t = $('#toast'); t.textContent = msg; t.hidden = false; clearTimeout(t._t); t._t = setTimeout(() => t.hidden = true, 3200); };

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
    } else if (win === 'flair') { $('#win-flair').hidden = false; renderFlair(params); }
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
  const LABEL = { tier:'Tier', role:'Papel', position:'Posição', ageMin:'Idade ≥', ageMax:'Idade ≤', state:'Estado', clubId:'Clube', contractStatus:'Status', period:'Período', metricType:'Métrica', eventType:'Evento', contentType:'Tipo de post', athleteId:'Atleta', source:'Origem', compare:'Comparar', severity:'Urgência', alertType:'Tipo', sort:'Ordem', q:'Nome' };
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

  /* =====================================================================
     fonte de dados — API (quando autenticado) com fallback DEMO (mock)
     ===================================================================== */
  const SORT = { 'Para você':'FOR_YOU', 'Trending':'TRENDING', 'Recentes':'RECENT', 'Seguindo':'FOLLOWING' };
  let sortBy = 'FOR_YOU';
  async function feed(win, params = {}, extra = {}) {
    const p = new URLSearchParams(params); Object.entries(extra).forEach(([k, v]) => v != null && p.set(k, v)); p.set('sort', sortBy);
    const d = await API.call(`/v1/feeds/${win.toUpperCase()}?${p}`);
    return { posts: (d.posts || []).map(fromPost), athletes: (d.athletes || []).map(fromAthlete), next: d.nextCursor, source: d.source };
  }
  const filteredAthletes = p => athletes.filter(a => (!p.tier || p.tier.split(',').includes(a.tier)) && (!p.position || a.pos.toUpperCase() === p.position)
    && (!p.ageMin || a.age >= +p.ageMin) && (!p.ageMax || a.age <= +p.ageMax) && (!p.state || a.state === p.state) && (!p.contractStatus || a.status === p.contractStatus) && (!p.role || p.role.split(',').includes(a.role)));
  const empty = (msg = 'Nada por aqui com esses filtros.') => `<p class="hint">${msg}</p>`;
  const cards = (list, fn = postCard, msg) => list.length ? list.map(x => fn(x)).join('') : empty(msg);
  async function tryApi(fn, fallback) {
    if (!live()) return fallback();
    try { return await fn(); } catch (e) { console.warn('API', e); toast(`API: ${e.message}`); return fallback(); }
  }

  async function renderSub(win, sub) {
    const p = state.params;
    if (win === 'perfil') {
      if (sub === 'feed') {
        const roles = p.role ? p.role.split(',') : null;
        const { athletes: ath, posts: ps } = await tryApi(() => feed('perfil', p), () => ({ athletes: [...filteredAthletes(p), ...orgs.filter(o => (!roles || roles.includes(o.role)) && !p.position && !p.tier)], posts: posts.perfil }));
        $('#feed-perfil').innerHTML = cards(ath, vcard, 'Nenhum perfil com esses filtros.'); $('#feed-perfil-posts').innerHTML = cards(ps);
      }
      if (sub === 'ranking') renderRanking('atletas');
      if (sub === 'explorar') { await loadClubs(); drawGlobe(+$('#globeRot').value); renderClubs(); }
      if (sub === 'momentos') { const { posts: ps } = await tryApi(() => feed('perfil', {}, { contentType: 'ESPECIAL,PARTIDA_MOMENTO' }), () => ({ posts: posts.momentos })); $('#feed-momentos').innerHTML = cards(ps); }
    }
    if (win === 'gemeo') {
      if (sub === 'telemetria') {
        const ev = p.eventType, cts = p.contentType?.split(',');
        const { posts: ps } = await tryApi(() => feed('gemeo', p), () => ({ posts: posts.gemeo.filter(x => (!ev || (ev === 'TREINO' ? x.ct === 'TREINO' : x.ct === 'PARTIDA_MOMENTO')) && (!cts || cts.includes(x.ct))) }));
        $('#feed-gemeo').innerHTML = cards(ps);
      }
      if (sub === 'evolucao') await drawEvolution(p.athleteId);
      if (sub === 'carga') await drawLoadPanel(p.athleteId);
      if (sub === 'dna') await drawDna(p.athleteId);
      if (sub === 'comparador') { const list = await tryApi(async () => { const ids = await twoIds(p.compare); return (await API.call(`/v1/twins/compare?ids=${ids.join(',')}`)).map(fromAthlete); }, () => [athletes[0], athletes[1]]); $('#compareTwin').innerHTML = list.map(a => vcard(a, { wide:true })).join(''); }
      if (sub === 'provador') renderProvador();
      if (sub === 'autopilot') {
        const list = await tryApi(async () => (await API.call('/v1/autopilot/drafts')).map(s => ({ role:'AGENTE_IA', ai:true, name:'Autopilot · rascunho', ct:s.contentType, top:'RASCUNHO', title:s.title, snip:s.subtitle, meta:['REVISAR', s.rationale ? s.rationale.slice(0, 40) : 'IA'], art:s.art, gen:'AI' })), () => posts.autopilot);
        $('#feed-autopilot').innerHTML = cards(list, postCard, 'Ative "Processamento por agentes de IA" na Central de Privacidade para receber rascunhos.');
      }
    }
    if (win === 'scouting') {
      if (sub === 'pranchetas') { const src = p.source; const { posts: ps } = await tryApi(() => feed('scouting', p), () => ({ posts: posts.scouting.filter(x => !src || (src === 'HOMOLOGADO' ? x.role !== 'TORCEDOR' : x.role === 'TORCEDOR')) })); $('#feed-scouting').innerHTML = cards(ps); }
      if (sub === 'comparar') { const list = await tryApi(async () => { const ids = await twoIds(p.compare); return (await API.call(`/v1/twins/compare?ids=${ids.join(',')}`)).map(fromAthlete); }, () => (p.compare || 'ath-1,ath-2').split(',').map(id => athletes.find(a => a.id === id)).filter(Boolean)); $('#compareScout').innerHTML = list.map(a => vcard(a, { wide:true })).join(''); }
      if (sub === 'relatorios') { const { posts: ps } = await tryApi(() => feed('scouting', {}, { role: 'SCOUT,CLUBE,IMPRENSA' }), () => ({ posts: posts.pranchetas })); $('#feed-relatorios').innerHTML = cards(ps); }
    }
    if (win === 'alerta') {
      if (sub === 'tudo') { const sev = p.severity?.split(','), ty = p.alertType?.split(','), so = p.source?.split(','); const { posts: ps } = await tryApi(() => feed('alerta', p), () => ({ posts: posts.alerta.filter(x => (!sev || sev.includes(x.sev)) && (!ty || ty.includes(x.type)) && (!so || so.includes(x.role))) })); $('#feed-alerta').innerHTML = cards(ps, postCard, 'Nenhum alerta com esses filtros.'); }
      if (sub === 'mural') { const list = await tryApi(async () => { const [a, b] = await Promise.all([feed('alerta', {}, { source: 'CLUBE' }), feed('perfil', {}, { role: 'CLUBE,STAFF' })]); return [...a.posts, ...b.posts]; }, () => posts.mural); $('#feed-mural').innerHTML = cards(list, postCard, 'Seu clube ainda não publicou no mural.'); }
      if (sub === 'saude') { const { posts: ps } = await tryApi(() => feed('alerta', {}, { alertType: 'LESAO' }), () => ({ posts: posts.saude })); $('#feed-saude').innerHTML = cards(ps.map(x => ({ ...x, lock: true })), postCard, 'Sem boletins de saúde.'); }
      if (sub === 'dm') $('#dmList').innerHTML = ['@<preencher_tecnico>|Convocação confirmada para sábado?|staff', '@<preencher_scout>|Posso incluir você na shortlist sub-23?|scout', '@<preencher_clube>|Renovação: proposta enviada.|clube'].map(s => { const [n, t, r] = s.split('|'); return `<li><span class="avatar" data-initials="${n.replace(/[@<>_]/g,'').slice(0,2).toUpperCase()}"><i class="dot dc-${r}"></i></span><div><b>${esc(n)}</b><small>${esc(t)}</small></div><small class="mono">AGORA</small></li>`; }).join('') + '<li class="hint">Mensagens diretas: protótipo (sem backend de mensageria).</li>';
    }
  }
  async function twoIds(compare) {
    if (compare && compare.includes(',')) return compare.split(',').slice(0, 3);
    const rows = await API.call('/v1/rankings/athletes?limit=3'); return rows.slice(0, 2).map(r => r.athleteId);
  }
  async function myAthleteId(explicit) {
    if (explicit) return explicit;
    if (API.user && API.user.role === 'ATLETA') return API.user.id;
    const rows = await API.call('/v1/rankings/athletes?limit=1'); return rows.length ? rows[0].athleteId : API.user.id;
  }

  /* ---------- ranking ---------- */
  async function renderRanking(kind) {
    const rows = await tryApi(async () => kind === 'atletas'
        ? (await API.call('/v1/rankings/athletes?limit=20')).map((r, i) => ({ i, n:r.handle, ini:ini(r.name), r:'atleta', meta:`${r.positionLabel || '—'} · ${r.age ?? '—'} · ${r.club || '—'}`, score:r.scoreTotal, tier:r.tier, d:r.delta }))
        : (await API.call('/v1/rankings/clubs')).map((c, i) => ({ i, n:c.name, ini:'C' + (i + 1), r:'clube', meta:`${c.country} · Divisão ${c.division ?? '—'} · ${c.athletes} atletas`, score:c.avgScore, tier:c.avgTier, d:0 })),
      () => kind === 'atletas' ? [...athletes].sort((a, b) => b.score - a.score).map((a, i) => ({ i, n:a.handle, ini:a.ini, r:'atleta', meta:`${a.pos} · ${a.age} · ${a.club}`, score:a.score, tier:a.tier, d:[+2,0,-1,+1,0][i] }))
        : clubs.slice(0, 6).map((c, i) => ({ i, n:c.n, ini:'C' + (i + 1), r:'clube', meta:`${c.c} · Divisão ${c.d}`, score:[2310,1980,1650,1210,880,420][i], tier:c.tier, d:[0,+1,-1,+2,0,-2][i] })));
    $('#rankingTable').innerHTML = `<tr><th>#</th><th>${kind === 'atletas' ? 'Jogador' : 'Clube'}</th><th>Tier</th><th>Score_Total</th><th>Δ</th></tr>` + rows.map(r => `<tr><td class="pos">${r.i + 1}</td><td><div class="who"><span class="avatar" data-initials="${r.ini}"><i class="dot dc-${r.r}"></i></span><div><b>${esc(r.n)}</b><br><small>${esc(r.meta)}</small></div></div></td><td>${chip(r.tier)}</td><td><b class="mono">${fmt(r.score)}</b></td><td class="delta ${r.d > 0 ? 'up' : r.d < 0 ? 'down' : ''}">${r.d > 0 ? '▲' + r.d : r.d < 0 ? '▼' + -r.d : '—'}</td></tr>`).join('');
    $$('.seg-mini button[data-rank]').forEach(b => b.classList.toggle('active', b.dataset.rank === kind));
  }
  $$('.seg-mini button[data-rank]').forEach(b => b.addEventListener('click', () => renderRanking(b.dataset.rank)));

  /* ---------- Explorar: globo ortográfico em SVG ---------- */
  let clubData = clubs;
  async function loadClubs() { clubData = await tryApi(async () => (await API.call('/v1/clubs')).filter(c => c.lat != null).map(c => ({ n:c.name, c:c.country, d:c.division || 1, lat:c.lat, lng:c.lng, tier:c.avgTier })), () => clubs); if (!clubData.length) clubData = clubs; }
  function project(lat, lng, rot, R = 190, cx = 210, cy = 210) {
    const φ = lat * Math.PI / 180, λ = (lng + rot) * Math.PI / 180, φ0 = -15 * Math.PI / 180;
    const cosc = Math.sin(φ0) * Math.sin(φ) + Math.cos(φ0) * Math.cos(φ) * Math.cos(λ); if (cosc < 0) return null;
    return { x: cx + R * Math.cos(φ) * Math.sin(λ), y: cy - R * (Math.cos(φ0) * Math.sin(φ) - Math.sin(φ0) * Math.cos(φ) * Math.cos(λ)) };
  }
  function drawGlobe(rot) {
    let g = `<defs><radialGradient id="gg" cx="35%" cy="30%"><stop offset="0" stop-color="#1b3a2c"/><stop offset="1" stop-color="#0a0f0c"/></radialGradient></defs><circle cx="210" cy="210" r="190" fill="url(#gg)" stroke="#26332b"/>`;
    for (let lat = -60; lat <= 60; lat += 30) { let d = ''; for (let lng = -180; lng <= 180; lng += 5) { const p = project(lat, lng, rot); if (p) d += (d ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1); } g += `<path d="${d}" fill="none" stroke="rgba(0,212,122,.12)"/>`; }
    for (let lng = -180; lng < 180; lng += 30) { let d = ''; for (let lat = -90; lat <= 90; lat += 5) { const p = project(lat, lng, rot); if (p) d += (d ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1); } g += `<path d="${d}" fill="none" stroke="rgba(0,212,122,.12)"/>`; }
    clubData.forEach(c => { const p = project(c.lat, c.lng, rot); if (!p) return; const col = TIER_COLOR[c.tier] || '#5c6b62'; g += `<g><circle cx="${p.x}" cy="${p.y}" r="9" fill="${col}" opacity=".25"/><circle cx="${p.x}" cy="${p.y}" r="4.5" fill="${col}" stroke="#eaf2ee" stroke-width="1"/><text x="${p.x + 8}" y="${p.y + 4}" font-size="9" font-family="JetBrains Mono,monospace" fill="#eaf2ee">${esc(c.c)}</text></g>`; });
    $('#globe').innerHTML = g;
  }
  $('#globeRot').addEventListener('input', e => drawGlobe(+e.target.value));
  function renderClubs() {
    const co = $('#exCountry').value, dv = $('#exDiv').value, tm = $('#exTier').value; const order = ['COMMON','RARE','EPIC','LEGENDARY','MYTHIC'];
    $('#clubList').innerHTML = clubData.filter(c => (!co || c.c === co) && (!dv || String(c.d) === dv) && (!tm || order.indexOf(c.tier) >= order.indexOf(tm.replace('+', '')))).map((c, i) => `<li><span class="avatar" data-initials="C${i + 1}"><i class="dot dc-clube"></i></span><div><b>${esc(c.n)}</b><small class="mono">${esc(c.c)} · DIV ${c.d} · </small>${chip(c.tier)}</div></li>`).join('') || '<li>Nenhum clube.</li>';
  }
  ['#exCountry', '#exDiv', '#exTier'].forEach(s => $(s).addEventListener('change', renderClubs));

  /* ---------- gráficos do Gêmeo ---------- */
  const line = (pts, color, w = 2.5) => `<polyline points="${pts.map(p => p.join(',')).join(' ')}" fill="none" stroke="${color}" stroke-width="${w}" stroke-linejoin="round"/>`;
  async function drawEvolution(athleteId) {
    const data = await tryApi(async () => { const d = await API.call(`/v1/twins/${await myAthleteId(athleteId)}/evolution`); return { scores: d.points.map(x => x.scoreTotal), labels: d.points.map(x => x.isoWeek.split('-W')[1]), tiers: d.points.map(x => x.tier), milestones: d.milestones }; },
      () => ({ scores:[820, 900, 980, 1040, 1180, 1210, 1300, 1420, 1500, 1580, 1610, 1650], labels:[...Array(12)].map((_, i) => String(32 + i)), tiers:[], milestones:['W35 · RARE → EPIC', 'W37 · 🏆 MVP', 'W39 · EPIC → LEGENDARY', 'W41 · 🔥 STREAK 5', 'W43 · NOTA DO TREINADOR'] }));
    const scores = data.scores.length ? data.scores : [0]; const n = Math.max(scores.length - 1, 1); const W = 640, H = 220, pad = 30;
    const x = i => pad + i * (W - 2 * pad) / n, y = v => H - pad - (Math.min(v, 3000) / 3000) * (H - 2 * pad); let s = '';
    [[0,400,'#5c6b62','common'],[401,900,'#3b82f6','rare'],[901,1600,'#a06bff','epic'],[1601,2400,'#f5c542','legendary'],[2401,3000,'#ffe08a','mythic']].forEach(([a, b, c, l]) => { const ih = Math.min(22, y(a) - y(b) - 2); s += `<rect x="${pad}" y="${y(b)}" width="${W - 2 * pad}" height="${y(a) - y(b)}" fill="${c}" opacity=".08"/><use href="#ic-tier-${l}" x="${W - pad + 3}" y="${(y(a) + y(b)) / 2 - ih / 2}" width="${ih}" height="${ih}"><title>Tier ${l}</title></use>`; });
    s += line(scores.map((v, i) => [x(i), y(v)]), '#f5c542') + scores.map((v, i) => `<circle cx="${x(i)}" cy="${y(v)}" r="3.5" fill="#f5c542"><title>${v}</title></circle><text x="${x(i)}" y="${H - 10}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#63756a">W${data.labels[i] || i}</text>`).join('');
    for (let i = 1; i < data.tiers.length; i++) if (data.tiers[i] !== data.tiers[i - 1]) s += `<circle cx="${x(i)}" cy="${y(scores[i])}" r="8" fill="none" stroke="#a06bff"/><text x="${x(i)}" y="${y(scores[i]) - 12}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#e2d3ff">${data.tiers[i - 1]} → ${data.tiers[i]}</text>`;
    $('#evoChart').innerHTML = s;
    $('#milestones').innerHTML = data.milestones.map(m => `<li>${esc(m)}</li>`).join('') || '<li>Sem marcos ainda.</li>';
  }
  function drawGauge(v) { const a = Math.PI * (1 - v / 100), x = 60 + 50 * Math.cos(a), y = 62 - 50 * Math.sin(a); $('#readiness').innerHTML = `<path d="M10 62 A50 50 0 0 1 110 62" fill="none" stroke="#26332b" stroke-width="10" stroke-linecap="round"/><path d="M10 62 A50 50 0 0 1 ${x} ${y}" fill="none" stroke="${v < 60 ? '#ff5d5d' : '#00d47a'}" stroke-width="10" stroke-linecap="round"/><text x="60" y="58" text-anchor="middle" font-size="16" font-family="Space Grotesk,sans-serif" fill="#eaf2ee">${v}%</text>`; }
  async function drawLoadPanel(athleteId) {
    const d = await tryApi(async () => { const weeks = (await API.call(`/v1/twins/${await myAthleteId(athleteId)}/load`)).reverse(); return { readiness: weeks.length ? weeks[weeks.length - 1].readiness : 100, acute: weeks.map(w => +w.loadAu), chronic: weeks.length ? +weeks[weeks.length - 1].chronicLoadAu : 470, labels: weeks.map(w => 'W' + w.isoWeek.split('-W')[1]), priv: false }; },
      () => ({ readiness: 78, acute: [420, 510, 380, 600, 450, 520, 504], chronic: 470, labels: ['S','T','Q','Q','S','S','D'], priv: false }));
    drawGauge(d.readiness);
    const W = 640, H = 160, pad = 24, n = Math.max(d.acute.length, 1), bw = Math.min(56, (W - 2 * pad) / n - 16), step = (W - 2 * pad) / n, max = Math.max(...d.acute, d.chronic * 1.6, 1);
    const sc = v => (H - 2 * pad) * v / max;
    $('#loadChart').innerHTML = d.acute.map((v, i) => `<rect x="${pad + i * step}" y="${H - pad - sc(v)}" width="${bw}" height="${sc(v)}" rx="6" fill="${v > d.chronic * 1.5 ? '#ff5d5d' : '#00d47a'}" opacity=".8"><title>${v} AU</title></rect><text x="${pad + i * step + bw / 2}" y="${H - 8}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#63756a">${d.labels[i]}</text>`).join('')
      + `<line x1="${pad}" x2="${W - pad}" y1="${H - pad - sc(d.chronic)}" y2="${H - pad - sc(d.chronic)}" stroke="#f5c542" stroke-dasharray="4 4"/><text x="${W - pad}" y="${H - pad - sc(d.chronic) - 4}" font-size="8" font-family="JetBrains Mono,monospace" text-anchor="end" fill="#f5c542">CARGA CRÔNICA 28 D</text>`;
  }
  let dnaVals = [88, 84, 72, 55, 80];
  async function drawDna(athleteId) {
    dnaVals = await tryApi(async () => (await API.call(`/v1/twins/${await myAthleteId(athleteId)}/dna`)).values, () => [88, 84, 72, 55, 80]);
    drawRadar(dnaVals);
  }
  function drawRadar(vals) {
    const labels = ['ATAQUE', 'TÉCNICA', 'TÁTICA', 'DEFESA', 'CRIATIV.']; const cx = 160, cy = 160, R = 110; const pt = (i, r) => [cx + r * Math.sin(i * 2 * Math.PI / 5), cy - r * Math.cos(i * 2 * Math.PI / 5)];
    let s = ''; [25, 50, 75, 100].forEach(r => s += `<polygon points="${[0,1,2,3,4].map(i => pt(i, R * r / 100).join(',')).join(' ')}" fill="none" stroke="#26332b"/>`);
    s += `<polygon points="${vals.map((v, i) => pt(i, R * v / 100).join(',')).join(' ')}" fill="rgba(0,212,122,.3)" stroke="#00d47a" stroke-width="2"/>`;
    labels.forEach((l, i) => { const [x, y] = pt(i, R + 22); s += `<text x="${x}" y="${y}" font-size="9" font-family="JetBrains Mono,monospace" text-anchor="middle" fill="#eaf2ee">${l} <tspan fill="#95a89d">${vals[i]}</tspan></text>`; });
    $('#radar').innerHTML = s;
  }
  function renderProvador() {
    const pos = $('#provPos').value, form = $('#provForm').value; const req = { MEIA:[80,85,80,50,85], PONTA:[85,80,70,45,85], VOLANTE:[60,80,85,75,70], ATACANTE:[90,80,70,40,75] }[pos] || [80,80,80,50,80];
    const v = dnaVals; const fit = Math.max(0, Math.round(100 - req.reduce((acc, r, i) => acc + Math.max(0, r - v[i]), 0) / 2 - (form === '3-5-2' ? 4 : 0)));
    $('#fitPct').textContent = fit + '%'; $('#fitPct').nextElementSibling.textContent = `fit para ${pos} no ${form}`;
    $('#gaps').innerHTML = ['Ataque', 'Técnica', 'Tática', 'Defesa', 'Criatividade'].map((l, i) => `<li><span>${l}</span><i style="--w:${v[i]}%"></i><span class="mono" style="color:${v[i] >= req[i] ? '#6cffc0' : '#ff5d5d'}">${v[i]}/${req[i]}</span></li>`).join('');
  }
  ['#provPos', '#provForm'].forEach(s => $(s).addEventListener('change', renderProvador));

  /* =====================================================================
     FLAIR — coleção, pacote diário, escalação e duelo 1×1 (API) · demo (mock)
     ===================================================================== */
  const F = { col:null, match:null, snap:false };
  const fcard = (a, cap) => { const power = a.power ?? Math.round(a.score * TIER_MULT[a.tier] * (cap ? 1.2 : 1)); return `<div class="fcard rarity-${a.tier.toLowerCase()}" style="--cc:${TIER_COLOR[a.tier]}"><div class="fin">${cap ? '<span class="cap">C</span>' : ''}<span class="avatar" data-initials="${a.ini}"><i class="dot dc-atleta"></i></span><b class="n">${esc(a.name)}</b><small style="text-align:center">${esc(a.pos)} · ${chip(a.tier)}</small><div class="axes3"><span>PP ${a.s[0]}</span><span>RP ${a.s[1]}</span><span>MP ${a.s[2]}</span></div><div class="power">${fmt(power)}</div></div></div>`; };
  async function renderFlair() {
    if (!live()) { $('#deckMe').innerHTML = [athletes[0], athletes[2], athletes[3]].map((a, i) => fcard(a, i === 0)).join(''); $('#deckOp').innerHTML = [athletes[1], athletes[4], athletes[3]].map((a, i) => fcard(a, i === 0)).join(''); $('#flairLog').innerHTML = '<li class="hint">Modo demonstração — entre para jogar com cartas reais.</li>'; return; }
    try {
      F.col = await API.call('/v1/flair/collection');
      $('#flairPoints').textContent = `🪙 ${fmt(F.col.points)} PONTOS`;
      $('#packBtn').disabled = !F.col.freePackAvailable; $('#packBtn').textContent = F.col.freePackAvailable ? '🎁 Abrir pacote do dia' : '🎁 Pacote de hoje já aberto';
      $('#flairOdds').textContent = 'odds · ' + Object.entries(F.col.packOdds).map(([k, v]) => `${k} ${v}`).join(' · ') + ` · partidas hoje ${F.col.matchesToday}/${F.col.dailyMatchLimit}`;
      const deck = F.col.deck.cards.length ? F.col.deck.cards : F.col.cards.slice(0, 3).map((c, i) => ({ ...c, captain: i === 0 }));
      $('#deckMe').innerHTML = deck.length ? deck.map(c => fcard(fromFlairCard(c), c.captain)).join('') : '<p class="hint">Abra seu primeiro pacote para montar a escalação.</p>';
      if (!F.match) { $('#deckOp').innerHTML = '<p class="hint">Inicie um duelo para ver o oponente.</p>'; $('#scoreLine').textContent = '0 × 0'; $('#roundLabel').textContent = 'LANCE 1 DE 3 · VOCÊ ESCOLHE O EIXO'; $('#flairLog').innerHTML = ''; }
      else renderMatch(F.match);
    } catch (e) { toast('FLAIR: ' + e.message); }
  }
  function renderMatch(m) {
    $('#deckMe').innerHTML = m.myDeck.map(c => fcard(fromFlairCard(c), c.captain)).join('');
    $('#deckOp').innerHTML = m.theirDeck.map(c => fcard(fromFlairCard(c), c.captain)).join('');
    $('#deckOpName').textContent = m.opponent; $('#scoreLine').textContent = `${m.myWins} × ${m.theirWins}`;
    $('#roundLabel').textContent = m.status === 'FINISHED' ? `FIM · ${m.result === 'WIN' ? 'VITÓRIA' : m.result === 'LOSS' ? 'DERROTA' : 'EMPATE'} · ${m.pointsAwarded >= 0 ? '+' : ''}${m.pointsAwarded} PONTOS` : `LANCE ${m.round} DE 3 · VOCÊ ESCOLHE O EIXO`;
    $('#flairLog').innerHTML = m.rounds.map(r => `<li class="${r.winner === 'THEM' ? 'lost' : ''}"><b>L${r.round} ${r.axis}</b> ${esc(r.myCard)} ${r.myValue} × ${r.theirValue} ${esc(r.theirCard)} → ${r.winner === 'ME' ? 'você' : r.winner === 'THEM' ? 'oponente' : 'empate'}${r.snap ? ' ⚡' : ''}</li>`).join('');
  }
  $('#packBtn').addEventListener('click', async () => { if (!live()) { toast('ENTRE PARA ABRIR PACOTES'); return; } try { const pk = await API.call('/v1/flair/packs/open', { method: 'POST' }); toast('PACOTE: ' + pk.cards.map(c => `${c.name} · ${TIER_LABEL[c.tier]}`).join(' · ')); F.match = null; renderFlair(); } catch (e) { toast(e.message); } });
  $('#flairStart').addEventListener('click', async () => {
    if (!live()) { toast('ENTRE PARA DUELAR'); return; }
    try {
      if (!F.col) F.col = await API.call('/v1/flair/collection');
      if (!F.col.deck.complete) { if (F.col.cards.length < 3) { toast('ABRA PACOTES ATÉ TER 3 CARTAS'); return; } await API.call('/v1/flair/deck', { method: 'PUT', body: { cardIds: F.col.cards.slice(0, 3).map(c => c.id), captainIndex: 0 } }); }
      F.match = await API.call('/v1/flair/matches', { method: 'POST', body: {} }); F.snap = false; $('#snapBtn').setAttribute('aria-pressed', 'false'); renderMatch(F.match); toast('DUELO INICIADO · ESCOLHA O EIXO DO LANCE 1');
    } catch (e) { toast(e.message); }
  });
  $$('.axes button').forEach(b => b.addEventListener('click', async () => {
    $$('.axes button').forEach(x => x.classList.toggle('primary', x === b));
    if (!live() || !F.match || F.match.status === 'FINISHED') { toast(live() ? 'INICIE UM NOVO DUELO' : `EIXO: ${b.textContent}`); return; }
    try { F.match = await API.call(`/v1/flair/matches/${F.match.id}/rounds`, { method: 'POST', body: { axis: b.dataset.axis, snap: F.snap } }); F.snap = false; $('#snapBtn').setAttribute('aria-pressed', 'false'); renderMatch(F.match); if (F.match.status === 'FINISHED') { toast(`${F.match.result} · ${F.match.pointsAwarded} PONTOS`); F.col = null; const pts = await API.call('/v1/flair/points'); $('#flairPoints').textContent = `🪙 ${fmt(pts.balance)} PONTOS`; } } catch (e) { toast(e.message); }
  }));
  $('#snapBtn').addEventListener('click', () => { F.snap = !F.snap; $('#snapBtn').setAttribute('aria-pressed', String(F.snap)); toast(F.snap ? '⚡ SNAP ARMADO · PONTOS EM DISPUTA DOBRADOS' : 'SNAP DESARMADO'); });

  /* ---------- páginas simples ---------- */
  async function renderMisc(win, sub, params = {}) {
    const mine = win === 'cards' ? await tryApi(async () => (await API.call('/v1/posts/mine')).map(fromPost), () => posts.gemeo) : [];
    const me = win === 'me' ? await tryApi(async () => API.user.role === 'ATLETA' ? fromAthlete(await API.call(`/v1/athletes/${API.user.id}/card`)) : null, () => athletes[0]) : null;
    const pages = {
      buscar: ['Buscar', `<label class="filter-form">Buscar atletas, clubes, ligas <input type="search" value="${esc(params.q || '')}" placeholder="Ex.: lateral sub-20 Série B"></label><p class="hint">Eixo "Nome" do sistema de filtros — cruza Perfil, Clubes &amp; Ligas e Momentos.</p>` + (params.q ? `<p class="hint"><a href="#/perfil?q=${encodeURIComponent(params.q)}">Ver resultados em Perfil →</a></p>` : '')],
      cards: ['Meus cards', '<p class="hint">Seus cards publicados e rascunhos, agrupados por tipo (borda) e feed.</p><div class="samples">' + cards(mine, postCard, 'Você ainda não criou cards — use o botão ＋.') + '</div>'],
      me: ['Meu perfil', (me ? vcard(me) : `<p class="hint">Perfil de ${esc(roleOf(ROLE).label)} — cards de atleta só para ATLETA.</p>`) + `<p class="hint" style="margin-top:12px">Visibilidade atual: <b>${API.user ? API.user.visibility.replace('_', ' ') : 'SOMENTE EU'}</b> (Privacy by Default). Altere em Privacidade (LGPD).</p>`],
      'me/historico': ['Histórico de atividades', '<ul class="milestones" id="histList"><li>carregando…</li></ul><span class="art">Art. 18, I e II</span>'],
      cupons: ['Cupons', '<p class="hint">Disponíveis apenas com <code>shareWithBrands = true</code>. Nenhum cupom ativo.</p>'],
      pontos: ['Pontos', '<div class="kpis"><div class="kpi"><small>Saldo</small><b id="ptsBal">—</b></div><div class="kpi"><small>Origem</small><b>FLAIR</b></div></div><p class="hint">Ganhos por duelos FLAIR, treinos publicados e assiduidade. Não compráveis com dinheiro.</p>'],
      'gestao/liga': ['Minha liga', '<p class="hint">Homologação de scouts, tabela, súmulas e punições — só <b>OFICIAL_LIGA</b>.</p><button class="btn ghost" id="recalcBtn">Recalcular Rarity da semana anterior</button>'],
      admin: ['Administração', '<p class="hint">Moderação, versões de termos Velion, auditoria LGPD e registro de papéis por agentes.</p>'],
    };
    const key = sub ? `${win}/${sub}` : win; const [t, body] = pages[key] || pages[win] || ['Página', '<p>Em construção.</p>'];
    $('#miscPanel').innerHTML = `<h3>${t}</h3>${body}`;
    if (key === 'me/historico') $('#histList').innerHTML = (await tryApi(async () => (await API.call('/v1/me/consent-history')).map(c => `${c.changedAt.slice(0, 10)} · ${c.preference}: ${c.oldValue} → ${c.newValue} · ${esc(c.articleLabel)}`), () => ['HOJE · LOGIN', 'W42 · shareTelemetryWithClubs: false → true'])).map(x => `<li>${x}</li>`).join('') || '<li>Sem eventos.</li>';
    if (key === 'pontos') $('#ptsBal').textContent = fmt(await tryApi(async () => (await API.call('/v1/flair/points')).balance, () => 1240));
    if (key === 'gestao/liga') $('#recalcBtn').addEventListener('click', async () => { try { const r = await API.call('/v1/admin/rarity/recalculate', { method: 'POST' }); toast(`RARITY ${r.week}: ${r.recalculated} GÊMEOS RECALCULADOS`); } catch (e) { toast(e.message); } });
  }

  /* =====================================================================
     CRIADOR DE CARDS — 4 etapas: modo → conteúdo → arte → revisar e salvar
     ===================================================================== */
  const ART_DEFAULT = { colors:['#00d47a','#3b82f6','#f7c948','#a06bff','#ff9f43','#ff5d5d','#f5c542','#2dd4bf','#f472b6','#22d3ee','#5c6b62','#eaf2ee'],
    materials:[['FOSCO','Fosco'],['METALICO','Metálico'],['VIDRO','Vidro'],['CARBONO','Carbono'],['HOLOGRAFICO','Holográfico']],
    layouts:[['CLASSICO','Clássico'],['HERO','Hero'],['SPLIT','Split'],['MINIMAL','Minimal']], styles:[['NEON','Neon'],['VITRAL','Vitral'],['GRAFITE','Grafite'],['OURO','Ouro'],['RETRO','Retrô']] };
  const CT_COLOR = { TREINO:'#00d47a', PARTIDA_MOMENTO:'#3b82f6', RELATORIO_TATICO:'#ff9f43', LIGA:'#a06bff', INSTITUCIONAL:'#ff5d5d', LESAO_ALERTA:'#ff5d5d', ESPECIAL:'#f5c542' };
  const WZ = { step:1, mode:'MANUAL', art:{ color:'#00d47a', material:'FOSCO', layout:'CLASSICO', style:'NEON' }, opts:ART_DEFAULT, suggestion:null, busy:false, photo:null, editId:null };
  const words = s => String(s || '').trim().split(/\s+/).filter(Boolean).length;
  const labelOf = (list, v) => (list.find(x => x[0] === v) || [v, v])[1];
  function openComposer(withCopilot, edit) {
    WZ.step = 1; WZ.mode = withCopilot ? 'AI' : 'MANUAL'; WZ.suggestion = null; WZ.art = { color:'#00d47a', material:'FOSCO', layout:'CLASSICO', style:'NEON' }; WZ.editId = edit ? edit.id : null;
    $('#wzTitle').textContent = edit ? 'Editar card · estúdio' : 'Criar card';
    ['#composerTitle', '#composerSub', '#composerText'].forEach(s => $(s).value = ''); $('#composerAge').value = 'SAFE'; $('#copilotBody').textContent = 'Gerando sugestão…'; clearPhoto();
    $$('.mode-opt').forEach(b => b.classList.toggle('on', b.dataset.mode === WZ.mode));
    $$('.type-opt[data-roles]').forEach(l => l.hidden = !l.dataset.roles.split(',').includes(ROLE));
    const first = $$('.type-opt').find(l => !l.hidden); if (first) first.querySelector('input').checked = true;
    $('#wzMeta').innerHTML = `<i class="dot xs dc-${roleOf(ROLE).dc}"></i> AUTOR · ${esc(roleOf(ROLE).label.toUpperCase())} · <span class="age-chip age-safe">SAFE</span>`;
    buildArtOptions(); goStep(1); $('#composer').hidden = false; $('#titleCount').textContent = '0 palavras';
    if (edit) {   // estúdio de edição: campos preenchidos, tipo/borda travados, começa no conteúdo
      const radio = $(`input[name="ct"][value="${edit.ct}"]`); if (radio) { radio.checked = true; } $$('input[name="ct"]').forEach(r => r.disabled = true);
      $('#composerTitle').value = edit.title || ''; $('#composerSub').value = edit.snip || ''; $('#composerText').value = edit.content || ''; $('#titleCount').textContent = `${words(edit.title)} palavras`;
      if (edit.art && edit.art.color) WZ.art = { color: edit.art.color, material: edit.art.material || 'FOSCO', layout: edit.art.layout || 'CLASSICO', style: edit.art.style || 'NEON', touched: true };
      if (edit.mediaUrl) { WZ.photo = { key: edit.mediaKey || null, url: edit.mediaUrl, kind: edit.mediaKind || 'image', keep: true }; $('#photoPreview').innerHTML = `<img src="${esc(edit.mediaUrl)}" alt="">`; $('#photoPreview').hidden = false; $('#photoClear').hidden = false; $('#photoStatus').textContent = 'fotografia atual · envie outra para substituir'; }
      buildArtOptions(); goStep(2);
    } else $$('input[name="ct"]').forEach(r => r.disabled = false);
    if (live()) API.call('/v1/cards/art-options').then(o => { WZ.opts = { colors:o.colors, materials:o.materials.map(m => [m.value, m.label]), layouts:o.layouts.map(m => [m.value, m.label]), styles:o.styles.map(m => [m.value, m.label]) }; buildArtOptions(); }).catch(() => {});
  }
  function buildArtOptions() {
    $('#artColors').innerHTML = WZ.opts.colors.map(c => `<button type="button" class="swatch${c === WZ.art.color ? ' on' : ''}" data-color="${c}" style="--sw:${c}" aria-label="${c}"></button>`).join('');
    const chips = (id, list, key) => $(id).innerHTML = list.map(([v, l]) => `<button type="button" class="optchip${WZ.art[key] === v ? ' on' : ''}" data-key="${key}" data-val="${v}">${l}</button>`).join('');
    chips('#artMaterials', WZ.opts.materials, 'material'); chips('#artLayouts', WZ.opts.layouts, 'layout'); chips('#artStyles', WZ.opts.styles, 'style');
    $('#artColorHex').textContent = WZ.art.color; $('#artCustom').value = WZ.art.color; renderArtPreview();
  }
  const currentCt = () => ($('input[name="ct"]:checked') || {}).value || 'TREINO';
  const draftPost = () => { const ct = currentCt(); const ageSel = $('#composerAge').value; return { id:'', role:ROLE, name:API.user ? '@' + API.user.username : '@<preencher_user>', ct, top:'AGORA', title:$('#composerTitle').value || '(título do card · 5 a 12 palavras)', snip:$('#composerSub').value, meta:null, mediaUrl: WZ.photo ? WZ.photo.url : null, mediaKind: WZ.photo ? WZ.photo.kind : null,
    art:{ ...WZ.art, materialLabel:labelOf(WZ.opts.materials, WZ.art.material), styleLabel:labelOf(WZ.opts.styles, WZ.art.style) }, gen:WZ.mode, age: ct === 'LESAO_ALERTA' ? ['age-sens','SENSÍVEL'] : [{ SAFE:'age-safe', KIDS:'age-kids', AGE_12:'age-12', AGE_16:'age-16', AGE_18:'age-18' }[ageSel] || 'age-safe', { SAFE:'SAFE', KIDS:'KIDS', AGE_12:'12+', AGE_16:'16+', AGE_18:'18+' }[ageSel] || 'SAFE'], alert: ct === 'LESAO_ALERTA' || ct === 'INSTITUCIONAL', alarm: ct === 'LESAO_ALERTA' ? '🩹 LESÃO' : '📣 INSTITUCIONAL', verified: API.user && API.user.verified }; };
  function renderArtPreview() { $('#artPreview').innerHTML = postCard(draftPost()); }
  function renderReview() {
    const d = draftPost(); $('#reviewCard').innerHTML = postCard(d);
    const rows = [['Modo', WZ.mode === 'AI' ? 'Com IA · Copilot (revisado)' : 'Manual'], ['Tipo · borda', `${(CT[d.ct] || CT.TREINO).badge} · ${CT_COLOR[d.ct]}`], ['Janela', { TREINO:'GÊMEO', PARTIDA_MOMENTO:'PERFIL', RELATORIO_TATICO:'SCOUTING', LIGA:'PERFIL', INSTITUCIONAL:'ALERTA', LESAO_ALERTA:'ALERTA', ESPECIAL:'PERFIL' }[d.ct]],
      ['Título', `${words($('#composerTitle').value)} palavras`], ['Teaser', $('#composerSub').value ? 'sim' : '—'], ['Texto completo', $('#composerText').value ? `${$('#composerText').value.length} caracteres (só no PostView)` : '—'],
      ['Fotografia', WZ.photo ? (WZ.photo.key ? 'enviada · ' + WZ.photo.key.split('/').pop() : 'prévia local (demo)') : '— (só arte de fundo)'], ['Classificação', d.age[1]], ['Cor de fundo', WZ.art.color], ['Material', labelOf(WZ.opts.materials, WZ.art.material)], ['Layout', labelOf(WZ.opts.layouts, WZ.art.layout)], ['Estilo', labelOf(WZ.opts.styles, WZ.art.style)], ['Fonte', live() ? (WZ.editId ? 'API · PATCH /v1/posts/{id}' : 'API · POST /v1/posts') : 'demo (local)']];
    $('#reviewSummary').innerHTML = rows.map(([k, v]) => `<li><span>${k}</span><b>${esc(v)}</b></li>`).join('');
  }
  function goStep(n) {
    if (n === 3 && WZ.step === 2) { const w = words($('#composerTitle').value); if (w < 5 || w > 12) { toast('TÍTULO DEVE TER ENTRE 5 E 12 PALAVRAS'); $('#composerTitle').focus(); return; } }
    WZ.step = Math.max(1, Math.min(4, n));
    $$('#wzSteps li').forEach(li => { const s = +li.dataset.step; li.classList.toggle('on', s === WZ.step); li.classList.toggle('done', s < WZ.step); });
    $$('.wz-pane').forEach(p => p.hidden = +p.dataset.step !== WZ.step);
    $('#wzBack').disabled = WZ.step === 1; $('#wzNext').hidden = WZ.step === 4; $('#publishBtn').hidden = WZ.step !== 4; $('#saveDraftBtn').hidden = WZ.step !== 4;
    $('#wzNext').textContent = WZ.step === 3 ? 'Revisar →' : 'Avançar →';
    if (WZ.step === 2) { $('#copilot').hidden = WZ.mode !== 'AI'; if (WZ.mode === 'AI' && !WZ.suggestion) fetchSuggestion(); }
    if (WZ.step === 3) { if (!WZ.art.touched) { WZ.art.color = CT_COLOR[currentCt()] || WZ.art.color; buildArtOptions(); } renderArtPreview(); }
    if (WZ.step === 4) renderReview();
  }
  async function fetchSuggestion() {
    $('#copilotBody').textContent = 'Gerando sugestão a partir dos seus dados…';
    const s = await tryApi(() => API.call('/v1/copilot/suggest', { method: 'POST', body: { draft: $('#composerText').value || $('#composerTitle').value || null, contentType: $('input[name="ct"]:checked') ? currentCt() : null } }),
      () => ({ contentType:'TREINO', title:'Sessão de sprints com 25 km/h sustentados hoje', subtitle:'+38 PP registrados pelo wearable · 72 min · 8,4 km · RPE 7.', badges:['⚡ PRONTO'], ageRating:'SAFE', art:{ color:'#00d47a', material:'VIDRO', materialLabel:'Vidro', layout:'CLASSICO', layoutLabel:'Clássico', style:'NEON', styleLabel:'Neon' }, rationale:'Detectei uma sessão de 72 min no seu wearable (demo).' }));
    WZ.suggestion = s;
    $('#copilotBody').innerHTML = `<div>Tipo sugerido: <b>${esc((CT[s.contentType] || CT.TREINO).badge)}</b> (borda ${esc(CT_COLOR[s.contentType] || '')})</div><div>Título: "${esc(s.title)}"</div><div>Teaser: "${esc(s.subtitle || '—')}"</div>${s.badges && s.badges.length ? `<div>Badges: ${s.badges.map(esc).join(' · ')}</div>` : ''}<div>Arte: ${esc(s.art ? `${s.art.materialLabel} · ${s.art.layoutLabel} · ${s.art.styleLabel} · ${s.art.color}` : '—')}</div><small class="mono">${esc(s.rationale || '')} · GERADO POR IA · VOCÊ REVISA ANTES DE PUBLICAR (LGPD ART. 20) · SEM DADOS DE SAÚDE</small>`;
  }
  function acceptSuggestion() {
    const s = WZ.suggestion; if (!s) return;
    const radio = $(`input[name="ct"][value="${s.contentType}"]`); if (radio && !radio.closest('.type-opt').hidden) radio.checked = true;
    $('#composerTitle').value = s.title; $('#composerSub').value = s.subtitle || ''; if (s.content && !$('#composerText').value) $('#composerText').value = s.content;
    $('#titleCount').textContent = `${words(s.title)} palavras`;
    if (s.art) { WZ.art = { color:s.art.color, material:s.art.material, layout:s.art.layout, style:s.art.style, touched:true }; buildArtOptions(); }
    toast('SUGESTÃO ACEITA · REVISE E AVANCE');
  }
  async function publish(draft) {
    if (WZ.busy) return; WZ.busy = true;
    const ct = currentCt(), title = $('#composerTitle').value.trim(), sub = $('#composerSub').value.trim(), text = $('#composerText').value.trim();
    try {
      if (live() && WZ.editId) {
        const body = { title, subtitle: sub, content: text, ageRating: $('#composerAge').value, mediaKeys: WZ.photo && WZ.photo.key ? [WZ.photo.key] : null, clearMedia: !WZ.photo, art: { color: WZ.art.color, material: WZ.art.material, layout: WZ.art.layout, style: WZ.art.style } };
        await API.call(`/v1/posts/${WZ.editId}`, { method: 'PATCH', body });
        toast('CARD ATUALIZADO · ESTÚDIO'); $('#composer').hidden = true; renderSub(state.win, state.sub[state.win]); if (location.hash.startsWith('#/cards')) renderMisc('cards'); return;
      }
      if (live()) {
        const body = { contentType: ct, title, subtitle: sub || null, content: text || null, ageRating: $('#composerAge').value, generationMode: WZ.mode, draft, mediaKeys: WZ.photo && WZ.photo.key ? [WZ.photo.key] : null, art: { color: WZ.art.color, material: WZ.art.material, layout: WZ.art.layout, style: WZ.art.style } };
        const created = await API.call('/v1/posts', { method: 'POST', body });
        toast(draft ? 'RASCUNHO SALVO · MEUS CARDS' : `CARD PUBLICADO · ${created.badgeLabel} · ${created.window}`);
        $('#composer').hidden = true; location.hash = draft ? '#/cards' : '#/' + created.window.toLowerCase(); renderSub(state.win, state.sub[state.win]);
      } else {
        const p = { ...draftPost(), title, snip: sub, trend: '➖ novo' }; const bucket = ct === 'TREINO' || ct === 'PARTIDA_MOMENTO' ? 'gemeo' : ct === 'RELATORIO_TATICO' ? 'scouting' : ct === 'INSTITUCIONAL' || ct === 'LESAO_ALERTA' ? 'alerta' : 'perfil';
        if (!draft) posts[bucket].unshift(p); else posts.autopilot.unshift({ ...p, top:'RASCUNHO' });
        $('#composer').hidden = true; location.hash = '#/' + bucket; renderSub(state.win, state.sub[state.win]); toast(draft ? 'RASCUNHO SALVO (DEMO)' : 'CARD PUBLICADO (DEMO) · BORDA ' + (CT[ct] || CT.TREINO).badge);
      }
    } catch (e) { toast(e.message); } finally { WZ.busy = false; }
  }
  /* fotografia: campo de upload do usuário (POST /v1/media multipart) — independente da arte de fundo */
  function clearPhoto() { WZ.photo = null; $('#composerPhoto').value = ''; $('#photoPreview').hidden = true; $('#photoPreview').innerHTML = ''; $('#photoClear').hidden = true; $('#photoStatus').textContent = 'sem fotografia — o card usa só a arte de fundo'; }
  $('#composerPhoto').addEventListener('change', async e => {
    const f = e.target.files && e.target.files[0]; if (!f) { clearPhoto(); return; }
    if (f.size > 25 * 1024 * 1024) { toast('ARQUIVO ACIMA DE 25 MB'); clearPhoto(); return; }
    const kind = f.type.startsWith('video') ? 'video' : 'image'; const local = URL.createObjectURL(f);
    $('#photoPreview').innerHTML = kind === 'video' ? `<video src="${local}" muted playsinline></video>` : `<img src="${local}" alt="prévia da fotografia">`; $('#photoPreview').hidden = false; $('#photoClear').hidden = false;
    if (!live()) { WZ.photo = { key: null, url: local, kind }; $('#photoStatus').textContent = 'prévia local (demo) · ' + f.name; return; }
    $('#photoStatus').textContent = 'enviando…';
    try {
      const fd = new FormData(); fd.append('file', f);
      const res = await API.raw('/v1/media', { method: 'POST', headers: { Authorization: 'Bearer ' + API.access }, body: fd });
      const d = await res.json(); if (!res.ok) throw new Error(d.detail || 'upload falhou');
      WZ.photo = { key: d.key, url: d.url, kind: d.kind }; $('#photoStatus').textContent = `enviada · ${d.key.split('/').pop()} · ${(d.sizeBytes / 1024).toFixed(0)} KB`;
    } catch (err) { toast('FOTO: ' + err.message); WZ.photo = { key: null, url: local, kind }; $('#photoStatus').textContent = 'falha no upload — prévia local'; }
  });
  $('#photoClear').addEventListener('click', clearPhoto);
  $('#fabBtn').addEventListener('click', () => openComposer(false));
  $$('.mode-opt').forEach(b => b.addEventListener('click', () => { WZ.mode = b.dataset.mode; $$('.mode-opt').forEach(x => x.classList.toggle('on', x === b)); }));
  $('#wzNext').addEventListener('click', () => goStep(WZ.step + 1)); $('#wzBack').addEventListener('click', () => goStep(WZ.step - 1));
  $$('#wzSteps li').forEach(li => li.addEventListener('click', () => { if (+li.dataset.step < WZ.step) goStep(+li.dataset.step); }));
  $('#composerTitle').addEventListener('input', e => { const w = words(e.target.value); const el = $('#titleCount'); el.textContent = `${w} palavras`; el.className = w >= 5 && w <= 12 ? 'mono ok' : 'mono bad'; });
  $$('input[name="ct"]').forEach(r => r.addEventListener('change', () => { if (!WZ.art.touched) WZ.art.color = CT_COLOR[r.value]; }));
  $('#cpAccept').addEventListener('click', acceptSuggestion); $('#cpRefresh').addEventListener('click', fetchSuggestion); $('#cpDismiss').addEventListener('click', () => $('#copilot').hidden = true);
  document.addEventListener('click', e => {
    const sw = e.target.closest('.swatch'); if (sw) { WZ.art.color = sw.dataset.color; WZ.art.touched = true; buildArtOptions(); return; }
    const oc = e.target.closest('.optchip'); if (oc) { WZ.art[oc.dataset.key] = oc.dataset.val; WZ.art.touched = true; buildArtOptions(); }
  });
  $('#artCustom').addEventListener('input', e => { WZ.art.color = e.target.value; WZ.art.touched = true; buildArtOptions(); });
  $('#publishBtn').addEventListener('click', () => publish(false)); $('#saveDraftBtn').addEventListener('click', () => publish(true));
  $$('[data-close]').forEach(b => b.addEventListener('click', () => $('#' + b.dataset.close).hidden = true));

  /* =====================================================================
     LGPD — Central de Privacidade ligada à API (opt-in, step-up, export, exclusão)
     ===================================================================== */
  const PREF_KEY = k => k.replace(/[A-Z]/g, c => '_' + c).toUpperCase();   // shareMedicalData → SHARE_MEDICAL_DATA
  let stepUpToken = null, pending = null;
  $('#lgpdToggles').innerHTML = lgpdToggles.map(([k, t, d, art, s]) => `<li class="toggle-row"><span><b>${t}</b><small>${d}</small><span class="art ${s ? 's' : ''}">${art}</span></span><input type="checkbox" class="sw" data-pref="${PREF_KEY(k)}" data-sensitive="${s}"></li>`).join('');
  async function openLgpd(panel) {
    $('#lgpdModal').hidden = false;
    if (panel) $(`.lgpd-nav [data-lgpd="${panel}"]`).click();
    if (!live()) return;
    try {
      const c = await API.call('/v1/me/privacy');
      c.preferences.forEach(p => { const sw = $(`#lgpdToggles .sw[data-pref="${p.key}"]`); if (sw) sw.checked = p.enabled; });
      const vis = $(`input[name="vis"][value="${c.visibility}"]`); if (vis) vis.checked = true;
      $('#sessionList').innerHTML = (c.sessions || []).map(s => `<li>${esc(s['device.userAgent'] || 'dispositivo').slice(0, 40)} · ${esc(s['device.ip'])} · ${esc((s.lastSeenAt || '').slice(0, 16).replace('T', ' '))}</li>`).join('');
      $('#accName').value = API.user.displayName; $('#langSelect').value = API.user.locale || 'pt-BR';
      const ex = await API.call('/v1/me/exports'); $('#exportList').innerHTML = ex.slice(0, 3).map(x => `<li>${x.requestedAt.slice(0, 16).replace('T', ' ')} · ${x.status}${x.downloadUrl ? ` · <a href="${x.downloadUrl}" target="_blank">baixar (7 d)</a>` : x.failureReason ? ' · ' + esc(x.failureReason) : ''}</li>`).join('');
    } catch (e) { toast('LGPD: ' + e.message); }
  }
  $('#btnLgpd').addEventListener('click', () => openLgpd()); $('#navLgpd').addEventListener('click', e => { e.preventDefault(); openLgpd(); });
  $$('.lgpd-nav button').forEach(b => b.addEventListener('click', () => { $$('.lgpd-nav button').forEach(x => x.classList.toggle('active', x === b)); $$('.lgpd-panel').forEach(p => p.hidden = p.dataset.lgpd !== b.dataset.lgpd); }));
  async function setPref(key, enabled, sw) {
    try {
      const d = await API.call(`/v1/me/privacy/${key}`, { method: 'PUT', body: { enabled }, headers: stepUpToken ? { 'X-Step-Up-Token': stepUpToken } : {} });
      sw.checked = d.enabled; toast(`${key}: ${!enabled} → ${enabled} · CONSENT AUDIT LOG · ${d.articleLabel}`); pending = null; $('#stepUpBox').hidden = true;
    } catch (e) {
      sw.checked = !enabled;
      if (e.code === 'STEP_UP_REQUIRED') { pending = { key, enabled, sw }; $('#stepUpBox').hidden = false; $('#stepUpPass').focus(); toast('CONFIRME SUA SENHA PARA ATIVAR DADO SENSÍVEL (ART. 11)'); } else toast(e.message);
    }
  }
  $$('#lgpdToggles .sw').forEach(sw => sw.addEventListener('change', () => { if (!live()) { toast(`${sw.dataset.pref}: ${!sw.checked} → ${sw.checked} · CONSENT AUDIT LOG (DEMO)`); return; } setPref(sw.dataset.pref, sw.checked, sw); }));
  async function stepUp(password) { const d = await API.call('/v1/me/step-up', { method: 'POST', body: { password } }); stepUpToken = d.stepUpToken; setTimeout(() => stepUpToken = null, (d.expiresInSeconds - 5) * 1000); return stepUpToken; }
  $('#stepUpBtn').addEventListener('click', async () => { try { await stepUp($('#stepUpPass').value); $('#stepUpPass').value = ''; toast('REAUTENTICADO · 5 MIN'); if (pending) { const p = pending; p.sw.checked = p.enabled; setPref(p.key, p.enabled, p.sw); } else if (pending === 'delete') doDelete(); $('#stepUpBox').hidden = true; } catch (e) { toast(e.message); } });
  $$('input[name="vis"]').forEach(r => r.addEventListener('change', async () => { if (!live()) { toast('VISIBILIDADE: ' + r.value + ' (DEMO)'); return; } try { API.user = await API.call('/v1/me', { method: 'PATCH', body: { visibility: r.value } }); toast('VISIBILIDADE · ' + r.value); } catch (e) { toast(e.message); } }));
  $('#accSave').addEventListener('click', async () => { if (!live()) { toast('SALVO (DEMO)'); return; } try { API.user = await API.call('/v1/me', { method: 'PATCH', body: { displayName: $('#accName').value, bio: $('#accBio').value || null, stateCode: $('#accState').value || null, city: $('#accCity').value || null } }); setUser(API.user); toast('PERFIL ATUALIZADO · ART. 18, III'); } catch (e) { toast(e.message); } });
  $('#darkToggle').addEventListener('change', e => saveAppearance({ theme: e.target.checked ? 'dark' : 'light' }));
  $('#fontScale').addEventListener('change', e => saveAppearance({ fontScale: +e.target.value }));
  $('#langSelect').addEventListener('change', e => { if (live()) API.call('/v1/me', { method: 'PATCH', body: { locale: e.target.value } }).then(() => toast('IDIOMA · ' + e.target.value)).catch(err => toast(err.message)); else toast('IDIOMA · ' + e.target.value + ' (DEMO)'); });
  $('#exportBtn').addEventListener('click', async () => { if (!live()) { toast('EXPORTAÇÃO SOLICITADA (202) · LINK VÁLIDO POR 7 DIAS (DEMO)'); return; } try { const x = await API.call('/v1/me/export', { method: 'POST' }); toast(`EXPORTAÇÃO ${x.status}${x.downloadUrl ? ' · LINK PRONTO (7 DIAS)' : x.failureReason ? ' · ' + x.failureReason : ''}`); openLgpd('dados'); } catch (e) { toast(e.message); } });
  $('#historyBtn').addEventListener('click', async () => { const ul = $('#consentHistory'); ul.hidden = !ul.hidden; if (ul.hidden) return; ul.innerHTML = (await tryApi(async () => (await API.call('/v1/me/consent-history')).map(c => `${c.changedAt.slice(0, 16).replace('T', ' ')} · ${c.preference} ${c.oldValue} → ${c.newValue} · ${esc(c.articleLabel)}`), () => ['W42 · SHARE_TELEMETRY_WITH_CLUBS false → true · Art. 7º, I'])).map(x => `<li>${x}</li>`).join('') || '<li>Sem alterações registradas.</li>'; });
  $('#delConfirm').addEventListener('input', e => $('#deleteBtn').disabled = e.target.value !== 'EXCLUIR');
  async function doDelete() { try { const d = await API.call('/v1/me', { method: 'DELETE', headers: stepUpToken ? { 'X-Step-Up-Token': stepUpToken } : {} }); toast(d.message); API.clear(); setTimeout(() => location.reload(), 1800); } catch (e) { if (e.code === 'STEP_UP_REQUIRED') { pending = 'delete'; $(`.lgpd-nav [data-lgpd="privacidade"]`).click(); $('#stepUpBox').hidden = false; $('#stepUpPass').focus(); toast('REAUTENTIQUE-SE PARA EXCLUIR'); } else toast(e.message); } }
  $('#deleteBtn').addEventListener('click', () => { if (!live()) { toast('REAUTENTIQUE-SE · PENDING_DELETE POR 30 DIAS (DEMO)'); return; } doDelete(); });
  $('#logoutBtn').addEventListener('click', async () => { try { if (live()) await API.call('/v1/auth/logout', { method: 'POST' }); } catch { } API.clear(); location.reload(); });
  $('#logoutAllBtn').addEventListener('click', async () => { try { if (live()) await API.call('/v1/auth/logout-all', { method: 'POST' }); } catch { } API.clear(); location.reload(); });

  /* =====================================================================
     AUTENTICAÇÃO — login / cadastro polimórfico / modo demo
     ===================================================================== */
  /* ---------- Aparência: tema · destaque · superfície · tela de fundo (aplicado no <html>, salvo local + API) ---------- */
  const AP_DEFAULT = { accents:[['GREEN','Verde campo','#00d47a'],['BLUE','Azul','#3b82f6'],['GOLD','Ouro','#f5c542'],['PURPLE','Roxo','#a06bff'],['ORANGE','Laranja','#ff9f43']],
    surfaces:[['GRAPHITE','Grafite','#131b17'],['FOREST','Floresta','#0f241a'],['NAVY','Marinho','#0f192b'],['PLUM','Ameixa','#1f1529'],['SAND','Areia','#fbf7ee']],
    wallpapers:[['NONE','Sem fundo',null],['VERDE','Verde','brand/wallpapers/verde.svg'],['CIANO','Ciano','brand/wallpapers/ciano.svg'],['OURO','Ouro','brand/wallpapers/ouro.svg'],['VERDE_CLARO','Verde-claro','brand/wallpapers/verde-claro.svg'],['VERDE_VIVO','Verde-vivo','brand/wallpapers/verde-vivo.svg']] };
  let AP = Object.assign({ theme:'dark', accent:'GREEN', surface:'GRAPHITE', wallpaper:'NONE', wallpaperUrl:null, fontScale:1 }, JSON.parse(localStorage.getItem('fas.appearance') || '{}'));
  function applyAppearance(save = true) {
    const root = document.documentElement;
    root.dataset.theme = AP.theme; root.dataset.accent = AP.accent; root.dataset.surface = AP.surface; root.dataset.wallpaper = AP.wallpaper;
    const wp = AP.wallpaper === 'NONE' ? null : AP.wallpaperUrl || (AP_DEFAULT.wallpapers.find(w => w[0] === AP.wallpaper) || [])[2];
    root.style.setProperty('--wallpaper-img', wp ? `url("${wp}")` : 'none'); root.style.fontSize = (16 * (AP.fontScale || 1)) + 'px';
    if (save) try { localStorage.setItem('fas.appearance', JSON.stringify(AP)); } catch { }
    renderAppearancePanel();
  }
  function renderAppearancePanel() {
    $('#apAccents').innerHTML = AP_DEFAULT.accents.map(([v, l, hex]) => `<button type="button" class="ap-swatch${AP.accent === v ? ' on' : ''}" data-ap="accent" data-val="${v}" style="--sw:${hex}" title="${l}" aria-label="${l}"></button>`).join('');
    $('#apSurfaces').innerHTML = AP_DEFAULT.surfaces.map(([v, l, hex]) => `<button type="button" class="ap-swatch${AP.surface === v ? ' on' : ''}" data-ap="surface" data-val="${v}" style="--sw:${hex}" title="${l}" aria-label="${l}"></button>`).join('');
    $('#apWalls').innerHTML = AP_DEFAULT.wallpapers.map(([v, l, url]) => `<button type="button" class="ap-wall${AP.wallpaper === v ? ' on' : ''}" data-ap="wallpaper" data-val="${v}"><div class="thumb${url ? '' : ' none'}" style="${url ? `--wp:url('${url}')` : ''}">${url ? '' : 'SEM FUNDO'}</div><span>${l}</span></button>`).join('')
      + `<label class="ap-wall custom${AP.wallpaper === 'CUSTOM' ? ' on' : ''}"><div class="thumb${AP.wallpaper === 'CUSTOM' && AP.wallpaperUrl ? '' : ' none'}" style="${AP.wallpaper === 'CUSTOM' && AP.wallpaperUrl ? `--wp:url('${AP.wallpaperUrl}')` : ''}">${AP.wallpaper === 'CUSTOM' && AP.wallpaperUrl ? '' : 'SEU ARQUIVO'}</div><span>Personalizado</span><input type="file" id="apCustom" accept="image/jpeg,image/png,image/webp"></label>`;
    $('#darkToggle').checked = AP.theme !== 'light'; $('#fontScale').value = AP.fontScale || 1;
  }
  async function saveAppearance(patch) {
    Object.assign(AP, patch); applyAppearance(true);
    if (!live()) return;
    const body = {}; if (patch.theme !== undefined) body.darkMode = patch.theme !== 'light'; if (patch.accent) body.uiAccent = patch.accent; if (patch.surface) body.uiSurface = patch.surface;
    if (patch.wallpaper) body.wallpaper = patch.wallpaper; if (patch.wallpaperKey) body.wallpaperKey = patch.wallpaperKey; if (patch.fontScale) body.fontScale = patch.fontScale;
    try { API.user = await API.call('/v1/me', { method: 'PATCH', body }); } catch (e) { toast(e.message); }
  }
  document.addEventListener('click', e => { const b = e.target.closest('[data-ap]'); if (!b) return; const patch = {}; patch[b.dataset.ap] = b.dataset.val; if (b.dataset.ap === 'wallpaper') patch.wallpaperUrl = null; saveAppearance(patch); toast(`APARÊNCIA · ${b.dataset.ap.toUpperCase()} ${b.dataset.val}`); });
  document.addEventListener('change', async e => {
    if (e.target.id !== 'apCustom') return; const f = e.target.files && e.target.files[0]; if (!f) return;
    const local = URL.createObjectURL(f);
    if (!live()) { saveAppearance({ wallpaper: 'CUSTOM', wallpaperUrl: local }); toast('FUNDO PERSONALIZADO (DEMO · SÓ NESTA SESSÃO)'); return; }
    try { const fd = new FormData(); fd.append('file', f); const res = await API.raw('/v1/media', { method: 'POST', headers: { Authorization: 'Bearer ' + API.access }, body: fd }); const d = await res.json(); if (!res.ok) throw new Error(d.detail || 'upload falhou');
      saveAppearance({ wallpaper: 'CUSTOM', wallpaperUrl: d.url, wallpaperKey: d.key }); toast('FUNDO PERSONALIZADO SALVO'); } catch (err) { toast('FUNDO: ' + err.message); }
  });
  applyAppearance(false);

  function setUser(u) {
    API.user = u; ROLE = u.role; document.body.dataset.role = u.role; document.body.dataset.tier = u.tier || 'COMMON';
    Object.assign(AP, { theme: u.darkMode === false ? 'light' : 'dark', accent: u.uiAccent || 'GREEN', surface: u.uiSurface || 'GRAPHITE', wallpaper: u.wallpaper || 'NONE', wallpaperUrl: u.wallpaperUrl || null, fontScale: +u.fontScale || 1 }); applyAppearance(true);
    $('#meName').textContent = u.displayName; $('#meHandle').textContent = '@' + u.username; $('#meRole').textContent = roleOf(u.role).label;
    $('#meTier').innerHTML = u.tier ? chip(u.tier) : ''; const av = $('#meBtn .avatar'); av.dataset.initials = ini(u.displayName); av.innerHTML = `<i class="dot dc-${roleOf(u.role).dc}"></i>`;
    $('#srcBadge').textContent = 'API'; $('#srcBadge').classList.add('api');
    $$('.sidebar a[data-roles]').forEach(a => a.hidden = !a.dataset.roles.split(',').includes(ROLE));
  }
  function openAuth() { $('#authModal').hidden = false; $('#authError').hidden = true; if (API.online && !API.terms) API.call('/v1/terms/current', { auth: false }).then(t => { API.terms = t; $('#termsVersion').textContent = `versão ${t.version} · ${t.owner}`; $('#termsLink').href = t.documentUrl || '#'; }).catch(() => {}); }
  $$('.auth-tabs button').forEach(b => b.addEventListener('click', () => { $$('.auth-tabs button').forEach(x => x.classList.toggle('active', x === b)); $('#loginForm').hidden = b.dataset.auth !== 'login'; $('#registerForm').hidden = b.dataset.auth !== 'register'; }));
  $('#regRole').addEventListener('change', e => $('#regAthlete').hidden = e.target.value !== 'ATLETA');
  const authFail = e => { const el = $('#authError'); el.hidden = false; el.textContent = e.code === 'TERMS_REQUIRED' ? e.message + ' — aceite os termos para continuar.' : e.status === 429 ? 'Muitas tentativas — aguarde um minuto.' : e.message; };
  $('#loginForm').addEventListener('submit', async e => {
    e.preventDefault(); const fd = new FormData(e.target);
    try { const d = await API.call('/v1/auth/login', { method: 'POST', body: { email: fd.get('email'), password: fd.get('password') }, auth: false }); API.setTokens(d.accessToken, d.refreshToken); setUser(d.user); $('#authModal').hidden = true; sessionStorage.removeItem('fas.demo'); toast(`BEM-VINDO · ${d.user.displayName}`); route(); } catch (err) { authFail(err); }
  });
  $('#registerForm').addEventListener('submit', async e => {
    e.preventDefault(); const fd = new FormData(e.target);
    const body = { role: fd.get('role'), email: fd.get('email'), password: fd.get('password'), username: fd.get('username'), displayName: fd.get('displayName'), birthDate: fd.get('birthDate') || null, acceptTerms: !!fd.get('acceptTerms'), termsVersion: API.terms ? API.terms.version : '2026.10', profile: { position: fd.get('position') || null, clubName: fd.get('clubName') || null } };
    try { const d = await API.call('/v1/auth/register', { method: 'POST', body, auth: false }); API.setTokens(d.accessToken, d.refreshToken); setUser(d.user); $('#authModal').hidden = true; toast('CONTA CRIADA · VISIBILIDADE SOMENTE EU · CONSENTIMENTOS DESLIGADOS'); route(); } catch (err) { authFail(err); }
  });
  $('#demoBtn').addEventListener('click', () => { sessionStorage.setItem('fas.demo', '1'); $('#authModal').hidden = true; toast('MODO DEMONSTRAÇÃO · DADOS FICTÍCIOS'); });
  $('#meBtn').addEventListener('click', () => API.user ? openLgpd('conta') : openAuth());

  /* ---------- post view (texto completo só aqui) ---------- */
  async function openPostView(id, fallbackCard) {
    const m = $('#postViewModal'); m.hidden = false;
    $('#postViewBody').innerHTML = fallbackCard || '';
    if (!live() || !id) { $('#postViewBody').insertAdjacentHTML('beforeend', '<p class="content hint">Texto completo disponível no PostView (demo).</p>'); return; }
    try { const v = await API.call(`/v1/posts/${id}`); $('#postViewBody').innerHTML = postCard(fromPost(v.card)) + `<div class="content">${esc(v.content || '(sem texto completo)')}</div>`; } catch (e) { toast(e.message); }
  }

  /* ---------- sidebar por papel, menu, hide-on-scroll, swipe, ações sociais ---------- */
  $$('.sidebar a[data-roles]').forEach(a => a.hidden = !a.dataset.roles.split(',').includes(ROLE) && ROLE !== 'ADMIN');
  $('#btnMore').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
  $('#searchForm').addEventListener('submit', e => { e.preventDefault(); location.hash = '#/perfil?q=' + encodeURIComponent($('#searchInput').value); });
  let lastY = 0; window.addEventListener('scroll', () => { const y = window.scrollY; $('#bottomNav').classList.toggle('hidden', y > lastY + 24 && y > 80); if (y < lastY - 4 || y < 40) $('#bottomNav').classList.remove('hidden'); lastY = y; }, { passive: true });
  let x0 = null; document.addEventListener('touchstart', e => x0 = e.touches[0].clientX, { passive: true });
  document.addEventListener('touchend', e => { if (x0 === null) return; const dx = e.changedTouches[0].clientX - x0; x0 = null; if (Math.abs(dx) < 70 || !WINDOWS.includes(state.win)) return; const i = WINDOWS.indexOf(state.win) + (dx < 0 ? 1 : -1); if (WINDOWS[i]) location.hash = '#/' + WINDOWS[i]; });
  document.addEventListener('click', async e => {
    const act = e.target.closest('.act[data-act]');
    if (act) {
      const card = act.closest('article'); const id = card && card.dataset.id; const isPost = card && !card.dataset.athlete; const n = act.querySelector('b'); const kind = act.dataset.act;
      const bump = d => { if (n) n.textContent = fmt(parseInt(n.textContent.replace(/\./g, '')) + d); };
      if (kind === 'comment') { openPostView(isPost ? id : null, card ? card.outerHTML : ''); return; }
      if (kind === 'edit') {
        if (!live() || !id) { toast('EDIÇÃO DISPONÍVEL COM A API'); return; }
        try { const v = await API.call(`/v1/posts/${id}`); const d = fromPost(v.card); openComposer(false, { ...d, content: v.content, mediaKey: v.card.media && v.card.media.key }); } catch (err) { toast(err.message); }
        return;
      }
      if (live() && isPost && id) {
        try { const r = await API.call(`/v1/posts/${id}/${kind}`, { method: 'POST' }); if (kind === 'like') { act.classList.toggle('on', r.liked); if (n) n.textContent = fmt(r.likes); } if (kind === 'save') { act.classList.toggle('on', r.saved); toast(r.saved ? 'SALVO' : 'REMOVIDO DOS SALVOS'); } if (kind === 'share') { if (n) n.textContent = fmt(r.shares); toast('COMPARTILHADO · LINK COPIADO'); try { navigator.clipboard.writeText(location.origin + '/#/post/' + id); } catch { } } } catch (err) { toast(err.message); }
        return;
      }
      if (kind === 'like') { act.classList.toggle('on'); bump(act.classList.contains('on') ? 1 : -1); }
      else if (kind === 'share') { bump(1); toast('COMPARTILHADO · LINK COPIADO'); }
      else if (kind === 'save') { act.classList.toggle('on'); toast(act.classList.contains('on') ? 'SALVO' : 'REMOVIDO DOS SALVOS'); }
      return;
    }
    const vt = e.target.closest('.vtabs .vt'); if (vt) { const card = vt.closest('.vcard'); card.querySelectorAll('.vtabs .vt').forEach(x => x.classList.toggle('on', x === vt)); card.querySelectorAll('.vpane').forEach(pn => pn.hidden = pn.dataset.vt !== vt.dataset.vt); return; }
    const b = e.target.closest('.bdgrow b[data-badge]'); if (b) toast(`TAP TO EXPAND · ${b.dataset.badge} · GET /v1/athletes/{id}/badges/{type}/history`); });
  $$('.sorttabs').forEach(st => st.addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; $$('button', st).forEach(x => x.classList.toggle('on', x === b)); sortBy = SORT[b.textContent.trim()] || 'FOR_YOU'; toast(`ORDENAR · ${b.textContent.toUpperCase()}`); renderSub(state.win, state.sub[state.win]); }));

  /* ---------- boot ---------- */
  async function boot() {
    await API.probe();
    if (API.online && API.access) { try { setUser(await API.call('/v1/me')); } catch { API.clear(); } }
    if (!API.online) { $('#srcBadge').textContent = 'DEMO · API OFFLINE'; }
    if (API.online && !API.user && !sessionStorage.getItem('fas.demo')) openAuth();
    window.addEventListener('hashchange', route);
    route();
  }
  boot();
})();
