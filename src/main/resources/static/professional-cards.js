/* Sporting evidence first. Compact scanning and accessible inline expansion share one projection. */
(() => {
  'use strict';
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const $=(s,r=document)=>r.querySelector(s);
  const roles={ATLETA:'Jogador',CLUBE:'Clube',MANAGER:'Manager'};
  const market={TRANSFER_LIST:'Na lista de transferências',NOT_FOR_SALE:'Não está à venda',LOAN_LIST:'Na lista de empréstimos',SWAP_LIST:'Na lista de trocas',FREE_AGENT:'Agente livre'};
  let serial=0;
  const date=v=>v?new Intl.DateTimeFormat('pt-BR',{timeZone:'UTC'}).format(new Date(v+'T12:00:00Z')):'Não informado';
  const val=v=>v==null||v===''?'Não informado':String(v);
  const number=v=>v==null?'—':new Intl.NumberFormat('pt-BR',{maximumFractionDigits:2}).format(Number(v));
  function facts(items=[]){return `<dl class="pro-facts">${items.map(f=>`<div><dt>${esc(f.label)}</dt><dd>${esc(f.key==='contract'?date(f.value):val(f.value))}</dd>${f.note?`<small>${esc(f.note)}</small>`:''}</div>`).join('')}</dl>`;}
  function metrics(items=[]){return items.length?`<dl class="pro-metrics">${items.map(f=>`<div><dt>${esc(f.label)}</dt><dd>${esc(number(f.value))}</dd>${f.note?`<small>${esc(f.note)}</small>`:''}</div>`).join('')}</dl>`:'<p class="pro-empty">Sem indicadores registrados para este recorte.</p>';}
  function context(c){if(!c)return '<p class="pro-context">Competição e referência ainda não informadas.</p>';return `<div class="pro-context">${c.competition?`<b>${esc(c.competition)}</b>`:'Competição não informada'}${c.teamName?`<span>Equipe: ${esc(c.teamName)}</span>`:''}${c.season?`<span>Temporada ${esc(c.season)}</span>`:''}${c.stage?`<span>${esc(c.stage)}</span>`:''}${c.position!=null?`<span>${esc(c.position)}º na classificação informada</span>`:''}${c.asOf?`<span>Referência: ${date(c.asOf)}</span>`:''}</div>`;}
  function card(c,{expand=true}={}){
    const id='pro-'+(++serial),initials=c.name?.trim().split(/\s+/).slice(0,2).map(s=>s[0]).join('').toUpperCase()||'•';
    const important=c.facts.filter(f=>['position','age','category','foot','contract','function','formation','location','type','manager'].includes(f.key));
    const status=c.transferSituation?market[c.transferSituation]:c.membership;
    return `<article class="pro-card cm-result pro-${esc(c.role.toLowerCase())}" data-profile="${esc(c.id)}" data-profile-role="${esc(c.role)}">
      <header class="pro-identity"><span class="pro-avatar">${c.photoUrl?`<img src="${esc(c.photoUrl)}" alt="" loading="lazy">`:esc(initials)}</span><div><span class="pro-eyebrow">${esc(roles[c.role]||c.role)}${c.verified?' · Conta verificada':''}</span><h3><a href="${esc(c.href)}">${esc(c.name)}</a></h3><p>${esc(c.headline||'Função não informada')}</p></div></header>
      <div class="pro-affiliation">${esc(c.affiliation?(c.role==='CLUBE'?'Treinador: ':'')+c.affiliation:c.membership||'Vínculo não informado')}${c.affiliation&&c.membership?`<small>${esc(c.membership)}</small>`:''}</div>
      ${status?`<p class="pro-market${c.transferSituation==='FREE_AGENT'?' is-free':''}">${esc(status)}</p>`:''}
      <ul class="pro-chips">${important.slice(0,4).map(f=>`<li title="${esc(f.label)}">${esc(f.key==='contract'?'Contrato '+date(f.value):f.value)}</li>`).join('')}</ul>
      ${metrics(c.metrics.slice(0,3))}
      ${context(c.context)}
      <p class="pro-evidence">${esc(c.evidence||'Dados do perfil')}${c.context?.source?` · ${esc(c.context.source)}`:''}</p>
      <div class="pro-supplementary">${facts(c.facts.filter(f=>!important.slice(0,4).includes(f)))}</div>
      <footer class="pro-actions"><a class="btn primary" href="${esc(c.href)}">${c.role==='ATLETA'?'Ver vitrine':'Ver perfil'}</a>${c.role==='ATLETA'?`<button type="button" class="btn ghost" data-pro-compare="${esc(c.id)}" data-pro-name="${esc(c.name)}" aria-pressed="false">Comparar</button>`:''}</footer>
      ${expand?`<details class="pro-expansion"><summary aria-controls="${id}"><span class="pro-expand-label">Expandir ficha profissional</span><span class="pro-collapse-label">Recolher ficha</span></summary><div id="${id}" class="pro-expanded" data-expanded-content></div></details>`:''}
    </article>`;
  }
  function tabs(root,{initial,groups,urlKey,label='Seções do perfil'}={}){
    if(!root)return;root.dataset.sportsTabsRoot='';
    const panes=[...root.querySelectorAll('[data-sports-panel]')].filter(p=>p.closest('[data-sports-tabs-root]')===root);
    const available=groups.filter(g=>panes.some(p=>p.dataset.sportsPanel===g[0]));
    if(!available.length)return;
    let nav=$('[data-sports-tabs]',root);if(!nav){nav=document.createElement('nav');nav.dataset.sportsTabs='';root.prepend(nav);}
    const cleanNav=nav.cloneNode(false);nav.replaceWith(cleanNav);nav=cleanNav;
    nav.className='sports-tabs';nav.setAttribute('role','tablist');nav.setAttribute('aria-label',label);const prefix='st-'+(++serial);
    nav.innerHTML=available.map(([key,title])=>`<button type="button" id="${prefix}-${key}" role="tab" aria-controls="${prefix}-panel-${key}" data-sports-tab="${key}">${esc(title)}</button>`).join('');
    const choose=(key,{focus=false,write=false}={})=>{if(!available.some(g=>g[0]===key))key=available[0][0];
      for(const p of panes){const on=p.dataset.sportsPanel===key;p.hidden=!on;p.id=prefix+'-panel-'+p.dataset.sportsPanel;p.setAttribute('role','tabpanel');p.setAttribute('aria-labelledby',prefix+'-'+p.dataset.sportsPanel);p.tabIndex=0;}
      for(const b of nav.children){const on=b.dataset.sportsTab===key;b.setAttribute('aria-selected',String(on));b.tabIndex=on?0:-1;if(on&&focus)b.focus();}
      if(write&&urlKey){const [path,search='']=location.hash.split('?'),q=new URLSearchParams(search);q.set(urlKey,key);history.replaceState(null,'',path+'?'+q);}
    };
    nav.addEventListener('click',e=>{const b=e.target.closest('[data-sports-tab]');if(b)choose(b.dataset.sportsTab,{write:true});});
    nav.addEventListener('keydown',e=>{if(!['ArrowLeft','ArrowRight','Home','End'].includes(e.key))return;e.preventDefault();const buttons=[...nav.children],current=buttons.indexOf(document.activeElement);const index=e.key==='Home'?0:e.key==='End'?buttons.length-1:(current+(e.key==='ArrowRight'?1:-1)+buttons.length)%buttons.length;choose(buttons[index].dataset.sportsTab,{focus:true,write:true});});
    choose(initial||available[0][0]);return {choose};
  }
  function table(headers,rows,empty='Nenhum registro informado.') {return rows.length?`<div class="pro-table-wrap"><table class="pro-table"><thead><tr>${headers.map(h=>`<th scope="col">${esc(h)}</th>`).join('')}</tr></thead><tbody>${rows.map(r=>`<tr>${r.map(x=>`<td>${esc(val(x))}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`:`<p class="pro-empty">${esc(empty)}</p>`;}
  function athleteExpansion(d){
    const total=d.season,rows=d.recentMatches||[];
    return `<div data-sports-tabs-root><nav data-sports-tabs></nav><section data-sports-panel="overview"><h4>Temporada ${esc(d.seasonLabel||'atual')}</h4>${total?.games>0?metrics([{label:'Jogos',value:total.games},{label:'Minutos',value:total.minutes},{label:'Assistências',value:total.assists},{label:'Gols',value:total.goals},{label:'Nota média',value:total.avgRating}]):'<p class="pro-empty">Sem partidas registradas nesta temporada.</p>'}<p>${esc(d.form?.label||'Forma recente sem amostra suficiente.')}</p><p class="hint">Confira a temporada e o volume de minutos antes de comparar atletas.</p></section>
      <section data-sports-panel="matches"><h4>Últimas partidas registradas</h4>${table(['Data','Competição','Min.','Gols','Assist.','Origem'],rows.map(m=>[date(m.date),m.competition,m.minutes,m.goals,m.assists,m.official?'Oficial':'Autorrelato']))}</section>
      <section data-sports-panel="attributes"><h4>Atributos do momento</h4>${facts((d.attributes||[]).map(a=>({key:a.key,label:a.label,value:a.value==null?null:a.value+' / 100',note:a.reason})))}<p class="hint">Atributos disponíveis no perfil. Saúde e prontidão não entram nesta ficha pública.</p></section></div>`;
  }
  function clubExpansion(d){const teams=new Map(d.teams.map(t=>[t.id,t.name]));return `<div data-sports-tabs-root><nav data-sports-tabs></nav>
    <section data-sports-panel="overview"><h4>Estrutura esportiva</h4>${table(['Equipe','Categoria','Posições procuradas','Vagas'],d.teams.map(t=>[t.name,t.category,t.wantedPositions,t.vacancies]))}</section>
    <section data-sports-panel="squad"><h4>Elenco visível</h4><ul class="pro-links">${d.squad.map(a=>`<li><a href="#/vitrine?athleteId=${esc(a.athleteId)}">${esc(a.name)}</a><small>${esc(a.attributes?.find(x=>x.kind==='pos')?.label||'Posição não informada')}</small></li>`).join('')||'<li>Nenhum jogador visível vinculado.</li>'}</ul></section>
    <section data-sports-panel="competitions"><h4>Competições e referências</h4>${table(['Equipe','Competição','Temporada','Fase','Posição','Referência'],[...d.competitions].sort((a,b)=>b.asOf.localeCompare(a.asOf)).slice(0,12).map(e=>[teams.get(e.teamId),e.competition,e.season,e.stage,e.position==null?'—':e.position+'º',date(e.asOf)]))}</section></div>`;}
  function managerExpansion(d){return `<div data-sports-tabs-root><nav data-sports-tabs></nav>
    <section data-sports-panel="overview"><h4>Perfil técnico</h4>${facts([{label:'Função',value:d.function==='HEAD_COACH'?'Treinador principal':d.function==='SPORTING_DIRECTOR'?'Gestor esportivo':'Assistente técnico'},{label:'Formação preferida',value:d.preferredFormation},{label:'Modelo de jogo',value:d.playingStyle},{label:'Licença',value:d.licenseName,note:d.licenseLevel},{label:'Idiomas',value:d.languages}])}</section>
    <section data-sports-panel="career"><h4>Trajetória registrada</h4>${table(['Clube','Início','Saída efetiva','Contrato até'],d.history.map(t=>[t.clubName,date(t.startDate),t.endDate?date(t.endDate):'Em aberto',t.contractUntil?date(t.contractUntil):'Não informado']))}</section>
    <section data-sports-panel="titles"><h4>Títulos declarados</h4>${table(['Conquista','Temporada','Clube','Fonte'],d.titles.map(t=>[t.competition,t.season,t.clubName,t.source]))}<p class="hint">Declarações do titular; não equivalem a uma verificação independente de títulos.</p></section></div>`;}
  let compare=[];try{const saved=JSON.parse(sessionStorage.getItem('sports.compare')||'[]');if(Array.isArray(saved))compare=saved.filter(x=>typeof x.id==='string'&&typeof x.name==='string').slice(0,2);}catch{}
  function paintCompare(){
    let tray=$('#proCompareTray');if(!tray){tray=document.createElement('aside');tray.id='proCompareTray';tray.className='pro-compare-tray';tray.setAttribute('aria-label','Comparação de jogadores');document.body.append(tray);}
    tray.hidden=!compare.length;tray.innerHTML=`<div><b>Comparar jogadores</b><span>${compare.map(c=>esc(c.name)).join(' × ')}${compare.length===1?' · selecione mais um jogador':''}</span></div>${compare.length===2?`<a class="btn primary" href="#/scouting/comparar?compare=${compare.map(c=>encodeURIComponent(c.id)).join(',')}">Abrir comparação</a>`:''}<button type="button" class="btn ghost" data-clear-compare>Limpar seleção</button>`;
    document.querySelectorAll('[data-pro-compare]').forEach(b=>{const selected=compare.some(c=>c.id===b.dataset.proCompare);b.setAttribute('aria-pressed',String(selected));b.textContent=selected?'Selecionado para comparar':'Comparar';});
    tray.querySelector('[data-clear-compare]')?.addEventListener('click',()=>{compare=[];sessionStorage.removeItem('sports.compare');paintCompare();});
  }
  document.addEventListener('click',e=>{const b=e.target.closest('[data-pro-compare]');if(!b)return;const id=b.dataset.proCompare,index=compare.findIndex(c=>c.id===id);if(index>=0)compare.splice(index,1);else if(compare.length<2)compare.push({id,name:b.dataset.proName});else{b.textContent='Compare até dois jogadores';return;}sessionStorage.setItem('sports.compare',JSON.stringify(compare));paintCompare();});
  function mount(root,cards,API,{density='compact',toolbar=true}={}){
    if(!root)return;root.classList.add('pro-card-collection');root.dataset.density=density;
    root.innerHTML=`${toolbar?`<div class="pro-collection-head"><span class="pro-card-legend">Fichas profissionais</span><div role="group" aria-label="Densidade dos cards"><button type="button" class="btn ghost" data-pro-density="compact" aria-pressed="${density==='compact'}">Compactos</button><button type="button" class="btn ghost" data-pro-density="detailed" aria-pressed="${density==='detailed'}">Com detalhes</button></div></div>`:''}<div class="pro-grid">${cards.map(c=>card(c)).join('')||'<p class="pro-empty">Nenhum perfil visível com estes filtros.</p>'}</div>`;
    root.querySelectorAll('[data-pro-density]').forEach(b=>b.addEventListener('click',()=>{root.dataset.density=b.dataset.proDensity;root.querySelectorAll('[data-pro-density]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));try{localStorage.setItem('sports.cardDensity',b.dataset.proDensity);}catch{}}));
    root.querySelectorAll('.pro-expansion').forEach(detail=>detail.addEventListener('toggle',async()=>{
      if(!detail.open||detail.dataset.loaded)return;const article=detail.closest('[data-profile]'),content=$('[data-expanded-content]',detail);const c=cards.find(c=>c.id===article.dataset.profile);if(!c)return;
      detail.dataset.loaded='loading';content.innerHTML='<p role="status">Carregando a ficha…</p>';
      const path=c.role==='ATLETA'?'/v1/athletes/'+c.id+'/vitrine':c.role==='CLUBE'?'/v1/clubs/'+c.id:c.role==='MANAGER'?'/v1/managers/'+c.id:null;
      if(!path){content.innerHTML=facts(c.facts);return;}
      try{const d=await API.call(path);if(!detail.isConnected)return;content.innerHTML=c.role==='ATLETA'?athleteExpansion(d):c.role==='CLUBE'?clubExpansion(d):managerExpansion(d);const host=$('[data-sports-tabs-root]',content);tabs(host,{groups:c.role==='ATLETA'?[['overview','Temporada'],['matches','Partidas'],['attributes','Atributos']]:c.role==='CLUBE'?[['overview','Equipes'],['squad','Elenco'],['competitions','Competições']]:[['overview','Perfil técnico'],['career','Carreira'],['titles','Títulos']]});detail.dataset.loaded='ready';}
      catch(e){detail.dataset.loaded='';content.innerHTML=`<p role="alert">${esc(e.message)}</p><p>Recolha e expanda para tentar novamente.</p>`;}
    }));paintCompare();
  }
  async function load(root,ids,API,options){const requestId=String(++serial);root.dataset.proRequest=requestId;const unique=[...new Set(ids)].slice(0,60);if(!unique.length){mount(root,[],API,options);return;}root.innerHTML='<p role="status">Carregando fichas profissionais…</p>';try{const d=await API.call('/v1/discovery/cards?ids='+unique.map(encodeURIComponent).join(','));if(root.isConnected&&root.dataset.proRequest===requestId)mount(root,d,API,options);}catch(e){if(root.isConnected&&root.dataset.proRequest===requestId)root.innerHTML=`<p role="alert">${esc(e.message)}</p>`;}}
  function fromAthleteDto(d){const find=k=>d.attributes?.find(a=>a.kind===k)?.label,s=d.season,metrics=[];if(s?.games>0){metrics.push({key:'minutes',label:'Minutos',value:s.minutes},{key:'games',label:'Partidas',value:s.games},{key:'goals',label:s.cleanSheets!=null?'Jogos sem sofrer gol':s.tackles!=null?'Desarmes':'Gols',value:s.cleanSheets??s.tackles??s.goals});}return {id:d.athleteId,role:'ATLETA',name:d.name,handle:d.handle,verified:d.author?.verified,photoUrl:d.heroImage?.url,headline:find('pos')||'Posição não informada',affiliation:find('club'),membership:find('without-club')||find('free')||find('signed'),transferSituation:null,facts:(d.attributes||[]).filter(a=>['pos','age','category','foot','height','state'].includes(a.kind)).map(a=>({key:a.kind==='pos'?'position':a.kind,label:({pos:'Posição',age:'Faixa etária',category:'Categoria',foot:'Pé',height:'Altura',state:'Estado'})[a.kind],value:a.label})),metrics,context:{season:s?.season},evidence:s?.games?'Indicadores das partidas registradas na temporada':'Sem partidas registradas nesta temporada',href:'#/vitrine?athleteId='+d.athleteId};}
  window.SportsLayout={tabs};
  window.ProfessionalCards={card,mount,load,fromAthleteDto};
})();
