#!/usr/bin/env python3
"""
trello_setup.py — cria/atualiza o Kanban do Football AI · Social no Trello (RF1…RF6 + DOCS).

Uso:
  export TRELLO_KEY=<PREENCHER_TRELLO_KEY> TRELLO_TOKEN=<PREENCHER_TRELLO_TOKEN>
  python scripts/trello_setup.py --workspace <idOrganization> [--board-name futebolsocial] [--dry-run]

Idempotente: reutiliza board/listas/cards pelo nome. Nunca apaga nada.
"""
from __future__ import annotations

import argparse
import os
import sys
from typing import Any

import requests

try:  # opcional
    from dotenv import load_dotenv
    load_dotenv()
except Exception:  # pragma: no cover
    pass

API = "https://api.trello.com/1"
LISTS = ["Backlog", "Em Progresso", "Em Review", "Concluído"]

CARDS: list[dict[str, Any]] = [
    {"name": "RF1 — Autenticação JWT, sessões Redis e rate limit", "labels": ["backend", "segurança"],
     "desc": "Access 15 min + refresh 7 d com rotação · session:{userId}:{sessionId} · blacklist jti · bucket por IP/usuário (429).",
     "checklist": ["JwtTokenService HS512 sem PII", "RedisSessionService com detecção de reuso", "RateLimitFilter 429 + Retry-After",
                   "Login bloqueia após 5 falhas (15 min)", "Logout e logout-all invalidam em tempo real"]},
    {"name": "RF2 — Cadastro polimórfico, termos Velion e anel por papel", "labels": ["backend", "identidade"],
     "desc": "User JOINED + 11 subclasses · TermsAcceptance versionado · UserRole com ringColor/dotClass (tokens do modelo visual).",
     "checklist": ["Aceite obrigatório da versão vigente (428 TERMS_REQUIRED)", "Novo usuário nasce SOMENTE_EU e consentimentos FALSE",
                   "Gêmeo DRAFT→ACTIVE (menores aguardam responsável)", "RoleCapability: LESAO_ALERTA só atleta/saúde verificado"]},
    {"name": "RF3 — SPA: 4 janelas de feed com filtros segmentados", "labels": ["frontend", "api"],
     "desc": "GET /v1/feeds/{window} · <aside class=filters-panel> por aba · bottom nav 4 abas + FAB · cards .pcard/.vcard.",
     "checklist": ["PERFIL: tier, position, idade, clube, contrato", "GEMEO: period, metricType, eventType, athleteId",
                   "SCOUTING: attr[velocidade]=gt:80, compare, source", "ALERTA: severity, alertType, source",
                   "Título obrigatório 5–12 palavras; sem texto corrido", "Atributos do card como lista de badges", "Janela segmentada Temporada · Atributos"]},
    {"name": "RF4 — Rarity Engine & Gêmeo Digital (CORE)", "labels": ["backend", "core"],
     "desc": "Score_Total 30/40/30 determinístico · tiers COMMON…MYTHIC · snapshot semanal idempotente · máquina de estados.",
     "checklist": ["Mesma entrada ⇒ mesmo score/tier/hash", "Faixas 0–400/401–900/901–1600/1601–2400/2401+", "Brasão por tier (nunca texto)",
                   "Job @Scheduled segunda 03:00", "InvalidTwinTransitionException"]},
    {"name": "RF4.1 — Athletic Persona: badges e status físico", "labels": ["backend", "ux"],
     "desc": "BadgeAwardService determinístico · PhysicalStatus exclusivo · top 3 na área inferior do card · tap to expand.",
     "checklist": ["INJURED bloqueia convocação", "topBadges ≤ 3 com DM primeiro", "Badges de saúde só com shareMedicalData", "BadgeAuditLog append-only"]},
    {"name": "RF5/RF23 — Central de Compliance LGPD", "labels": ["backend", "lgpd"],
     "desc": "Opt-in FALSE · ConsentAuditLog · export JSON (Art. 18, V) para S3 7 d · soft delete 30 d + anonimização (Art. 18, VI) · step-up.",
     "checklist": ["PUT /v1/me/privacy/{pref} com X-Step-Up-Token para sensíveis", "POST /v1/me/export gera JSON e URL pré-assinada",
                   "DELETE /v1/me agenda purge +30 d; login restaura", "Job diário de anonimização", "Sem dark patterns (cancelar = 1 toque)"]},
    {"name": "RF6 — Modo FLAIR (cards do app)", "labels": ["frontend", "produto"],
     "desc": "Duelo 1×1 por eixo PP/RP/MP · multiplicador por tier · sem dinheiro real (ECA Digital art. 20; Lei 14.790/2023).",
     "checklist": ["Power = score × tierMultiplier × 1,2 capitão", "Pacotes gratuitos com probabilidades visíveis", "Sub-13 sem modo competitivo público"]},
    {"name": "DOCS — Prompt, taxonomia, anatomia de cards, diagramas, previews", "labels": ["docs"],
     "desc": "docs/PROMPT_FOOTBALL_AI_SOCIAL.md · ENTITY_TAXONOMY.md · CARD_ANATOMY.md · diagrams/** · preview/spa/*.png",
     "checklist": ["Diagramas PlantUML validam (-checkonly)", "Prints atualizados a cada mudança de UI", "README com docker compose + perfis"]},
]


class Trello:
    def __init__(self, key: str, token: str, dry_run: bool = False) -> None:
        self.auth = {"key": key, "token": token}
        self.dry = dry_run
        self.s = requests.Session()

    def _req(self, method: str, path: str, **params: Any) -> Any:
        if self.dry and method != "GET":
            print(f"[dry-run] {method} {path} {params}")
            return {"id": "dry", "name": params.get("name")}
        r = self.s.request(method, f"{API}{path}", params={**self.auth, **params}, timeout=30)
        r.raise_for_status()
        return r.json()

    def board(self, workspace: str, name: str) -> dict:
        for b in self._req("GET", f"/organizations/{workspace}/boards", fields="name,url"):
            if b["name"] == name:
                print(f"board existente: {b['url']}")
                return b
        b = self._req("POST", "/boards/", name=name, idOrganization=workspace, defaultLists="false", prefs_permissionLevel="org")
        print(f"board criado: {b.get('url')}")
        return b

    def lists(self, board_id: str) -> dict[str, str]:
        existing = {l["name"]: l["id"] for l in self._req("GET", f"/boards/{board_id}/lists", fields="name")} if not self.dry else {}
        out: dict[str, str] = {}
        for i, name in enumerate(LISTS, start=1):
            out[name] = existing.get(name) or self._req("POST", "/lists", name=name, idBoard=board_id, pos=i)["id"]
        return out

    def labels(self, board_id: str) -> dict[str, str]:
        colors = {"backend": "blue", "frontend": "green", "segurança": "red", "identidade": "purple", "api": "sky", "core": "orange",
                  "ux": "lime", "lgpd": "black", "produto": "pink", "docs": "yellow"}
        existing = {l["name"]: l["id"] for l in self._req("GET", f"/boards/{board_id}/labels", fields="name")} if not self.dry else {}
        out: dict[str, str] = {}
        for name, color in colors.items():
            out[name] = existing.get(name) or self._req("POST", "/labels", name=name, color=color, idBoard=board_id)["id"]
        return out

    def cards(self, board_id: str, list_id: str, label_ids: dict[str, str]) -> None:
        existing = {c["name"]: c for c in self._req("GET", f"/boards/{board_id}/cards", fields="name,idChecklists")} if not self.dry else {}
        for pos, spec in enumerate(CARDS, start=1):
            card = existing.get(spec["name"])
            if card is None:
                card = self._req("POST", "/cards", idList=list_id, name=spec["name"], desc=spec["desc"], pos=pos,
                                 idLabels=",".join(label_ids[l] for l in spec["labels"] if l in label_ids))
                print(f"card criado: {spec['name']}")
            else:
                self._req("PUT", f"/cards/{card['id']}", desc=spec["desc"])
            if not card.get("idChecklists"):
                cl = self._req("POST", "/checklists", idCard=card["id"], name="Critérios de aceite")
                for item in spec["checklist"]:
                    self._req("POST", f"/checklists/{cl['id']}/checkItems", name=item)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--workspace", required=True, help="id (24 hex) da área de trabalho Trello")
    ap.add_argument("--board-name", default="futebolsocial")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    key, token = os.getenv("TRELLO_KEY"), os.getenv("TRELLO_TOKEN")
    if not args.dry_run and not (key and token):
        print("defina TRELLO_KEY e TRELLO_TOKEN no ambiente", file=sys.stderr)
        return 2
    t = Trello(key or "", token or "", dry_run=args.dry_run)
    board = t.board(args.workspace, args.board_name)
    lists = t.lists(board["id"])
    labels = t.labels(board["id"])
    t.cards(board["id"], lists["Backlog"], labels)
    print("ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
