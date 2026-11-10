/* Perfil do usuário Manager. Dados de carreira e de contrato permanecem distintos. */
(() => {
  'use strict';
  const labels = { HEAD_COACH:'Treinador principal', SPORTING_DIRECTOR:'Gestor esportivo', ASSISTANT:'Assistente técnico' };
  const transferLabels = {TRANSFER_LIST:'Na lista de transferências',NOT_FOR_SALE:'Não está à venda',LOAN_LIST:'Na lista de empréstimos',SWAP_LIST:'Na lista de trocas',FREE_AGENT:'Agente livre'};
  let revision = 0;
  async function render(params, API, user, esc) {
    const run = ++revision;
    const panel = document.querySelector('#miscPanel');
    const id = params.managerId || params.id || (user && user.role === 'MANAGER' ? user.id : '');
    if (!id) { panel.innerHTML = '<h2>Manager</h2><p>Selecione um manager na busca global.</p>'; return; }
    panel.innerHTML = '<h2>Manager</h2><p role="status">Carregando perfil…</p>';
    try {
      const d = await API.call('/v1/managers/' + encodeURIComponent(id));
      if (run !== revision || !location.hash.startsWith('#/manager')) return;
      const text = v => esc(v == null || v === '' ? 'Não informado' : String(v));
      const date = v => v ? text(v.split('-').reverse().join('/')) : 'Não informado';
      const club = t => `<a href="#/clube?clubId=${encodeURIComponent(t.clubId)}">${text(t.clubName)}</a>`;
      const money = (value,currency) => value == null ? 'Não divulgado' : text(currency || '') + ' ' + text(Number(value).toLocaleString('pt-BR'));
      const input = (name,label,value,type='text',extra='') => `<label>${label}<input name="${name}" type="${type}" value="${esc(value == null ? '' : String(value))}" ${extra}></label>`;
      const opts = (list,value) => list.map(([v,l])=>`<option value="${v}"${String(value)===v?' selected':''}>${l}</option>`).join('');
      const status = d.hasCurrentClub === false ? 'Manager sem clube' : d.hasCurrentClub === true ? 'Com vínculo de clube' : 'Vínculo não informado';
      panel.innerHTML = `<style>.dc-manager{--dc:#38bdf8}.manager-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:1rem}.manager-grid label{display:grid;gap:.3rem}.manager-career{overflow-x:auto}.manager-career table{width:100%;text-align:left}.manager-career td,.manager-career th{padding:.5rem}</style>
        <h2>${text(d.name)} · Manager</h2><p>${text(d.handle)} · ${text(labels[d.function] || d.function)} · <strong>${status}</strong></p>
        <p><strong>Situação de transferência:</strong> ${text(transferLabels[d.transferSituation] || d.transferSituation)}</p><p>${text(d.bio)}</p><dl class="manager-grid"><div><dt>Nacionalidades declaradas</dt><dd>${text(d.nationalities)}</dd></div><div><dt>País de residência</dt><dd>${text(d.country)}</dd></div><div><dt>Idiomas</dt><dd>${text(d.languages)}</dd></div><div><dt>Licença</dt><dd>${text(d.licenseName)} · ${text(d.licenseLevel)}</dd></div><div><dt>Formação preferida</dt><dd>${text(d.preferredFormation)}</dd></div><div><dt>Modelo de jogo</dt><dd>${text(d.playingStyle)}</dd></div></dl>
        <h3>Clube atual e contrato</h3>${d.currentClubs.length ? d.currentClubs.map(t=>`<p>${club(t)} · ${text(labels[t.function] || t.function)}${t.interim?' · Interino':''}<br>Início: ${date(t.startDate)} · Expiração contratual: ${date(t.contractUntil)}<br>Taxa de transferência/compensação: ${money(t.transferFee,t.currency)}</p>`).join('') : '<p>Nenhum mandato vigente vinculado. Situação: '+status+'.</p>'}
        <h3>Posição na liga</h3>${d.leaguePositions.length?d.leaguePositions.map(p=>`<p>${text(p.competition)} · ${text(p.season)} · ${text(p.stage)} · ${p.position==null?'Sem colocação informada':text(p.position)+'º'} · referência ${date(p.asOf)}</p>`).join(''):'<p>Classificação não informada para as equipes vinculadas.</p>'}
        <h3>Histórico de clubes</h3><div class="manager-career"><table><thead><tr><th>Clube</th><th>Função</th><th>Início</th><th>Término efetivo</th><th>Contrato até</th></tr></thead><tbody>${d.history.map(t=>`<tr><td>${club(t)}</td><td>${text(labels[t.function] || t.function)}</td><td>${date(t.startDate)}</td><td>${t.endDate?date(t.endDate):'Em aberto'}</td><td>${date(t.contractUntil)}</td></tr>`).join('') || '<tr><td colspan="5">Sem histórico vinculado.</td></tr>'}</tbody></table></div>
        <h3>Clubes com mais passagens</h3><p>${d.mostSpellsClubs.map(c=>`${text(c.clubName)}: ${text(c.spells)} passagem(ns)`).join(' · ')||'Não informado'}</p>
        <h3>Maior tempo de atuação</h3><p>${d.longestServiceClubs.map(c=>`${text(c.clubName)}: ${text(c.serviceDays)} dia(s)`).join(' · ')||'Não informado'}</p><p class="hint">Não há contagem de jogos por manager registrada; passagens e duração são métricas diferentes de partidas.</p>
        <h3>Títulos na carreira</h3><ul>${d.titles.map(t=>`<li>${text(t.competition)} · ${text(t.season)} · ${text(t.clubName)} · ${date(t.awardedOn)} · declarado pelo titular · fonte: ${text(t.source)}${d.editable?` <button class="btn ghost" data-remove-title="${esc(t.id)}">Remover título</button>`:''}</li>`).join('')||'<li>Nenhum título informado.</li>'}</ul>
        ${d.editable?`<details><summary>Editar perfil de Manager</summary><form id="managerEdit" class="manager-grid">
          <label>Função<select name="function">${opts(Object.entries(labels),d.function)}</select></label>
          ${input('nationalities','Nacionalidades (códigos, ex.: BR,PT)',d.nationalities,'text','maxlength="120" pattern="([A-Za-z]{2}(,[A-Za-z]{2})*)?"')}
          ${input('languages','Idiomas (ex.: PT,EN)',d.languages,'text','maxlength="120" pattern="([A-Za-z]{2,3}(,[A-Za-z]{2,3})*)?"')}
          ${input('licenseName','Licença',d.licenseName,'text','maxlength="120"')}${input('licenseLevel','Nível da licença',d.licenseLevel,'text','maxlength="40"')}
          ${input('preferredFormation','Formação preferida',d.preferredFormation,'text','maxlength="40"')}${input('playingStyle','Modelo de jogo',d.playingStyle,'text','maxlength="120"')}
          ${input('careerStartedOn','Início da carreira',d.careerStartedOn,'date')}
          <label>Situação de transferência<select name="transferSituation">${opts(Object.entries(transferLabels),d.transferSituation || 'NOT_FOR_SALE')}</select></label>
          <label>Vínculo declarado<select name="hasCurrentClub">${opts([['','Não informado'],['false','Manager sem clube'],['true','Com vínculo de clube']],d.hasCurrentClub==null?'':String(d.hasCurrentClub))}</select></label>
          ${input('clubStatusSource','Origem da situação de vínculo',d.clubStatusSource,'text','maxlength="240"')}${input('clubStatusAsOf','Data da informação de vínculo',d.clubStatusAsOf,'date')}
          <button class="btn primary" type="submit">Salvar perfil</button></form></details>
          <details><summary>Adicionar título declarado</summary><form id="managerTitle" class="manager-grid">${input('clubName','Clube',null,'text','required maxlength="120"')}${input('competition','Competição',null,'text','required maxlength="120"')}${input('season','Temporada',null,'text','required maxlength="30"')}${input('category','Categoria da equipe',null,'text','maxlength="40"')}${input('awardedOn','Data do título',null,'date','required')}${input('source','Fonte',null,'text','required maxlength="240"')}<button class="btn primary" type="submit">Registrar título</button></form></details>`:''}
        <p role="status" id="managerStatus"></p>`;
      const statusEl = panel.querySelector('#managerStatus');
      const edit = panel.querySelector('#managerEdit');
      if (edit) edit.addEventListener('submit',async e=>{
        e.preventDefault(); const body = Object.fromEntries(new FormData(edit));
        for(const k of ['careerStartedOn','clubStatusAsOf']) if(!body[k]) body[k]=null;
        body.hasCurrentClub=body.hasCurrentClub===''?null:body.hasCurrentClub==='true';
        try { await API.call('/v1/managers/'+encodeURIComponent(id),{method:'PUT',body}); await render(params,API,user,esc); }
        catch(err) { statusEl.textContent=err.message; }
      });
      const title = panel.querySelector('#managerTitle');
      if(title) title.addEventListener('submit',async e=>{
        e.preventDefault(); try {await API.call('/v1/managers/'+encodeURIComponent(id)+'/titles',{method:'POST',body:Object.fromEntries(new FormData(title))});await render(params,API,user,esc);}catch(err){statusEl.textContent=err.message;}
      });
      panel.querySelectorAll('[data-remove-title]').forEach(b=>b.addEventListener('click',async()=>{
        try{await API.call('/v1/managers/'+encodeURIComponent(id)+'/titles/'+encodeURIComponent(b.dataset.removeTitle),{method:'DELETE'});await render(params,API,user,esc);}catch(err){statusEl.textContent=err.message;}
      }));
    } catch(err) { if(run===revision) panel.innerHTML='<h2>Manager</h2><p role="alert">'+esc(err.message)+'</p>'; }
  }
  window.ManagerUI={render};
})();
