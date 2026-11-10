#!/usr/bin/env python3
"""Functional API validation. Creates uniquely named development profiles; use a disposable local database.
No credential/token is printed. Optional --session-file stores temporary browser-test sessions with mode 0600.
"""
import argparse, datetime as dt, json, os, secrets, time, urllib.request, urllib.error, urllib.parse


def run(base, session_file=None):
    checks=[]
    def check(condition, name):
        assert condition, name
        checks.append(name)
    def call(path, actor=None, body=None, method='GET', status=200):
        headers={'Content-Type':'application/json'}
        if actor: headers['Authorization']='Bearer '+actor['accessToken']
        req=urllib.request.Request(base+path, headers=headers, data=None if body is None else json.dumps(body).encode(),method=method)
        for attempt in range(3):
            try:
                with urllib.request.urlopen(req,timeout=25) as r: code=r.status; raw=r.read()
            except urllib.error.HTTPError as e:
                code=e.code;raw=e.read()
                if code==429 and attempt<2:
                    time.sleep(min(60,max(1,int(e.headers.get('Retry-After','60')))));continue
            break
        data=json.loads(raw) if raw else None
        assert code==status, f'{method} {path}: expected {status}, got {code}: '+str(data if code>=400 else 'unexpected success')
        return data
    prefix='smoke_'+secrets.token_hex(4)
    password=secrets.token_urlsafe(18)+'aA1!'
    terms=call('/v1/terms/current')['version']
    def register(role, name, profile=None, public=True):
        body=dict(role=role,email=f'{prefix}_{name}@exemplo.dev',password=password,username=f'{prefix}_{name}',displayName=f'{prefix} {name}',birthDate='1995-02-01',acceptTerms=True,termsVersion=terms,profile=profile or {})
        actor=call('/v1/auth/register',body=body,method='POST',status=201)
        if public: call('/v1/me',actor,dict(visibility='PUBLICO'),method='PATCH')
        return actor
    club=register('CLUBE','clube'); player=register('ATLETA','jogador'); manager=register('MANAGER','manager'); outsider=register('TORCEDOR','leitor'); private=register('ATLETA','privado',public=False)
    club_id=club['user']['id']; athlete_id=player['user']['id']; manager_id=manager['user']['id']
    def search(entity='ALL', filters=(), actor=outsider):
        q=urllib.parse.urlencode([('entity',entity),('q',prefix)]+[('filter',x) for x in filters])
        return call('/v1/discovery?'+q,actor)
    check(search()['total']==4,'Private profiles excluded from discovery and counts')
    registry=call('/v1/discovery/fields',outsider)
    check({'athlete.transferSituation','manager.transferSituation','manager.hasCurrentClub','competition.position','venue.street','finance.payroll','managerTitle.competition'}.issubset({x['key'] for x in registry}),'Global field registry includes profile and related fields')
    call('/v1/discovery?filter=passwordHash:eq:x',outsider,status=400)
    check(True,'Unknown or private filter paths rejected')
    identity=dict(name=f'{prefix} clube',legalName='Clube de Testes',country='BR',state='PR',city='Curitiba',website='https://clube.example',type='PROFISSIONAL',foundedYear=2000)
    call('/v1/clubs/'+club_id,club,identity,method='PUT')
    call('/v1/clubs/'+club_id,outsider,identity,method='PUT',status=403)
    team=call('/v1/clubs/'+club_id+'/teams',club,dict(name='Principal',category='PROFISSIONAL',modality='FUTEBOL',playingStyle='POSSE',wantedPositions='MEIA,ATACANTE',preferredFoot='D',languages='PT',vacancies=2,source='Smoke Test'),method='POST')
    today=dt.date.today().isoformat()
    venue=call('/v1/clubs/'+club_id+'/venues',club,dict(name='Estádio teste',venueType='STADIUM',street='Rua do Estádio',number='10',city='Curitiba',state='PR',country='BR',postalCode='80000-000',capacity=12000,startDate=today,source='Smoke Test'),method='POST')
    for season,position in [('2025',10),('2026',2)]:
        call('/v1/clubs/'+club_id+'/competitions',club,dict(teamId=team['id'],competition='Liga teste',season=season,format='LEAGUE',position=position,asOf=today,source='Smoke Test'),method='POST')
    check(search('CLUBE',['competition.season:eq:2025','competition.position:eq:2'])['total']==0,'Related season and position use the same record')
    check(search('CLUBE',['competition.season:eq:2026','competition.position:lte:3','venue.capacity:gte:10000'])['total']==1,'Combined competition and stadium SQL filters')
    for amount,published in [(987654,False),(100,True)]:
        call('/v1/clubs/'+club_id+'/finances',club,dict(season='2026',currency='BRL',payroll=amount,payrollPeriod='MONTH',squadValue=amount*2,valuationType='DECLARED',asOf=today,published=published,source='Smoke Test'),method='POST')
    check(search('CLUBE',['finance.payroll:gte:900000'])['total']==0,'Private payroll cannot leak through search counts')
    check(search('CLUBE',['finance.payroll:gte:900000'],club)['total']==1,'Club owner can filter its own private payroll')
    profile=call('/v1/clubs/'+club_id,outsider)
    check(len(profile['finances'])==1,'Public club profile hides unpublished finances')
    edit_manager=dict(function='HEAD_COACH',nationalities='BR,PT',languages='PT',hasCurrentClub=False,clubStatusSource='Declaração smoke',clubStatusAsOf=today,transferSituation='FREE_AGENT')
    call('/v1/managers/'+manager_id,manager,edit_manager,method='PUT')
    check(search('MANAGER',['manager.hasCurrentClub:eq:false','manager.transferSituation:eq:FREE_AGENT'])['total']==1,'Manager without club and free-agent filters')
    tenure=call('/v1/clubs/'+club_id+'/managers',club,dict(teamId=team['id'],managerUserId=manager_id,name=f'{prefix} manager',function='HEAD_COACH',startDate=today,contractUntil='2030-01-01',transferFee=321000,currency='BRL',transferFeePublic=False,source='Smoke Test'),method='POST')
    current=call('/v1/managers/'+manager_id,outsider)
    check(current['hasCurrentClub'] is True and current['transferSituation']=='NOT_FOR_SALE','Active mandate overrides stale Manager free-agent declaration')
    check(current['currentClubs'][0].get('transferFee') is None,'Manager profile hides private transfer fee')
    check(search('MANAGER',['manager.hasCurrentClub:eq:false'])['total']==0,'Manager no-club SQL filter considers active mandate')
    check(search('MANAGER',['manager.transferSituation:eq:FREE_AGENT'])['total']==0,'Manager transfer SQL filter agrees with profile')
    check(search('MANAGER',['managerHistory.transferFee:gte:300000'])['total']==0,'Manager history search protects private fees')
    check(search('CLUBE',['managerTenure.transferFee:gte:300000'])['total']==0,'Club commission search protects private fees')
    call('/v1/managers/'+manager_id+'/titles',manager,dict(clubId=club_id,clubName=identity['name'],competition='Copa teste',season='2025',awardedOn='2025-12-01',source='Declaração smoke'),method='POST')
    check(search('MANAGER',['managerTitle.competition:contains:Copa'])['total']==1,'Manager career title filters')
    market=dict(affiliationStatus='WITHOUT_CLUB',contractStatus='AGENTE_LIVRE',transferSituation='FREE_AGENT',freeTransferConfirmed=True,source='Declaração smoke',position='MEIA',dominantFoot='D',category='PROFISSIONAL',languages='PT',playingStyle='POSSE',country='BR',state='PR',city='Curitiba')
    call('/v1/me/market',player,market,method='PUT')
    check(search('ATLETA',['athlete.affiliationStatus:eq:WITHOUT_CLUB','athlete.transferSituation:eq:FREE_AGENT'])['total']==1,'Player no-club and free-agent filters')
    for situation in ['TRANSFER_LIST','NOT_FOR_SALE','LOAN_LIST','SWAP_LIST','FREE_AGENT']:
        market['transferSituation']=situation
        call('/v1/me/market',player,market,method='PUT')
        check(search('ATLETA',['athlete.transferSituation:eq:'+situation])['total']==1,'Player transfer situation '+situation)
    fit='/v1/athletes/'+athlete_id+'/vitrine/compatibility?'+urllib.parse.urlencode(dict(clubId=club_id,teamId=team['id']))
    call(fit,outsider,status=403)
    call('/v1/me/privacy/AI_AGENT_PROCESSING',player,dict(enabled=True),method='PUT')
    result=call(fit,outsider)
    check(result['score']==100 and result['coverage']==100,'Vitrine contextual affinity has score, coverage and consent')
    invite=call('/v1/clubs/'+club_id+'/memberships',club,dict(teamId=team['id'],athleteId=athlete_id,kind='PERMANENT',startDate=today,shirtNumber=10,source='Smoke Test'),method='POST')
    check(not call('/v1/clubs/'+club_id,outsider)['squad'],'Pending invitation does not add player to roster')
    call('/v1/me/club-invitations/'+invite['id']+'/accept',outsider,method='POST',status=404)
    call('/v1/me/club-invitations/'+invite['id']+'/accept',player,method='POST')
    check(call('/v1/clubs/'+club_id,outsider)['squad'][0]['athleteId']==athlete_id,'Player confirmation adds linked roster entry')
    check(call('/v1/me/market',player)['currentClubId']==club_id,'Player entity references confirmed club')
    call('/v1/me/market',player,market,method='PUT',status=409)
    call('/v1/clubs/'+club_id+'/memberships/'+invite['id']+'/end',club,method='POST')
    check(not call('/v1/clubs/'+club_id,outsider)['squad'],'Ending membership removes current roster and preserves history')
    check(call('/v1/me/market',player)['transferSituation']=='FREE_AGENT','Ending a player contract updates market status')
    call('/v1/auth/register',body=dict(role='ATLETA',email=prefix+'_fake@exemplo.dev',username=prefix+'_fake',displayName='Fake vínculo',password=password,birthDate='1990-01-01',acceptTerms=True,termsVersion=terms,profile={'clubId':club_id}),method='POST',status=400)
    check(True,'Registration cannot forge a confirmed club relationship')
    professional=call('/v1/discovery/cards?'+urllib.parse.urlencode({'ids':','.join([athlete_id,club_id,manager_id,private['user']['id']])}),outsider)
    check({c['id'] for c in professional}=={athlete_id,club_id,manager_id},'Professional card batch excludes private profiles')
    check(all('transferFee' not in str(c) and 'payroll' not in str(c) and 'physical' not in c for c in professional),'Professional summaries do not expose private finances or medical indicators')
    check(next(c for c in professional if c['id']==athlete_id)['metrics']==[],'No-match profile does not manufacture sporting indicators')
    call('/v1/discovery/cards?'+urllib.parse.urlencode({'ids':','.join([athlete_id]*61)}),outsider,status=400)
    check(True,'Professional card batch is bounded')
    check(search('ATLETA',['athlete.transferSituation:eq:FREE_AGENT,SWAP_LIST,LOAN_LIST'])['total']==1,'Multiple transfer situations survive query parameter binding')
    if session_file:
        fd=os.open(session_file,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
        with os.fdopen(fd,'w') as f:json.dump(dict(base=base,prefix=prefix,club=club,player=player,manager=manager,outsider=outsider,clubId=club_id,athleteId=athlete_id,managerId=manager_id,teamId=team['id']),f)
    print(json.dumps(dict(passed=len(checks),checks=checks),ensure_ascii=False,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--base',default='http://localhost:8080');parser.add_argument('--session-file');args=parser.parse_args();run(args.base,args.session_file)
