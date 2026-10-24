#!/usr/bin/env python3
"""
enrichment_agent.py — agente de enriquecimento do Gêmeo Digital.

Lê um JSON de atletas (seed ou export da API), deriva localmente — com as MESMAS regras do backend —
o status físico, os badges Athletic Persona e uma prévia do Score_Total/tier, e grava um JSON enriquecido
que o Copilot/Autopilot usa como rascunho. Nunca envia dado de saúde sem `shareMedicalData = true`.

Uso:
  python scripts/enrichment_agent.py --in docs/data/athletes_seed.json --out docs/data/athletes_enriched.json
  python scripts/enrichment_agent.py --in seed.json --api http://localhost:8080 --token $ACCESS_TOKEN --dry-run

Formato de entrada (por atleta):
  {"id": "...", "name": "<PREENCHER>", "position": "MEIA", "captain": false, "consent": {"shareMedicalData": false},
   "season": {"games": 22, "goals": 14, "assists": 7, "minutes": 1980, "yellow": 1, "red": 0, "cleanSheets": 0, "tackles": 3, "mvp": 2, "ratings": [7.8, 8.1, 7.6]},
   "training": {"planned": 5, "done": 4, "readiness": 78, "acuteLoad": 1210, "chronicLoad": 1000}, "injuryActive": false,
   "technical": [{"rating": 8.2, "weight": 1.0}], "week": {"matches": 2, "minutes": 160, "avgRating": 7.8, "goalContributions": 2}}
"""
from __future__ import annotations

import argparse
import json
import sys
from decimal import ROUND_HALF_EVEN, Decimal
from pathlib import Path
from typing import Any

import requests

TIERS = [("COMMON", 0, 400), ("RARE", 401, 900), ("EPIC", 901, 1600), ("LEGENDARY", 1601, 2400), ("MYTHIC", 2401, 10**9)]
CREST = {"COMMON": "ic-tier-common", "RARE": "ic-tier-rare", "EPIC": "ic-tier-epic", "LEGENDARY": "ic-tier-legendary", "MYTHIC": "ic-tier-mythic"}
D = Decimal


def q4(x: Decimal) -> Decimal:
    return x.quantize(D("0.0001"), rounding=ROUND_HALF_EVEN)


def clamp(x: Decimal, lo: Decimal = D(0), hi: Decimal = D(1)) -> Decimal:
    return max(lo, min(hi, x))


def tier_of(score: int) -> str:
    for name, lo, hi in TIERS:
        if lo <= score <= hi:
            return name
    return "MYTHIC"


def physical_status(injury_active: bool, acwr: Decimal, readiness: int) -> str:
    if injury_active:
        return "INJURED"
    if acwr > D("1.5") or readiness < 60:
        return "FATIGUE_RISK"
    return "READY"


def season_badges(s: dict[str, Any], position: str | None, captain: bool) -> dict[str, int]:
    ratings = s.get("ratings") or []
    streak = best = 0
    for r in ratings:
        streak = streak + 1 if r >= 7.5 else 0
        best = max(best, streak)
    out: dict[str, int] = {}
    if s.get("goals", 0) > 0: out["ARTILHEIRO"] = s["goals"]
    if s.get("assists", 0) > 0: out["GARCOM"] = s["assists"]
    if position == "GOLEIRO" and s.get("cleanSheets", 0) > 0: out["PAREDAO"] = s["cleanSheets"]
    if position in ("ZAGUEIRO", "LATERAL", "VOLANTE") and s.get("tackles", 0) >= 20: out["XERIFAO"] = s["tackles"]
    if s.get("minutes", 0) >= 900: out["MOTORZINHO"] = s["minutes"]
    if s.get("mvp", 0) > 0: out["MVP"] = s["mvp"]
    if best >= 3: out["EM_CHAMAS"] = best
    if s.get("games", 0) >= 10 and s.get("yellow", 0) <= 1 and s.get("red", 0) == 0: out["FAIR_PLAY"] = 0
    if captain: out["LIDER_CAPITAO"] = 0
    return out


def badge_bonus(badges: dict[str, int]) -> int:
    b = 0
    for t, c in badges.items():
        b += {"MVP": 25 * max(1, c), "EM_CHAMAS": 10 * max(1, c), "MESTRE_TATICO": 30, "LIDER_CAPITAO": 20, "FAIR_PLAY": 15}.get(t, 0)
    return min(300, b)


def score(a: dict[str, Any], badges: dict[str, int]) -> tuple[int, dict[str, str]]:
    tr, wk = a.get("training", {}), a.get("week", {})
    planned, done = int(tr.get("planned", 0)), int(tr.get("done", 0))
    attendance = clamp(q4(D(done) / D(planned))) if planned > 0 else D(0)
    readiness = clamp(q4(D(int(tr.get("readiness", 0))) / D(100)))
    acwr = q4(D(str(tr.get("acuteLoad", 0))) / D(str(tr.get("chronicLoad", 1) or 1)))
    penalty = D(1) if D("0.8") <= acwr <= D("1.3") else (D("0.8") if D("0.5") <= acwr <= D("1.5") else D("0.5"))
    physical = q4(D("0.6") * attendance + D("0.4") * readiness * penalty)
    tech = a.get("technical") or []
    wsum = sum(D(str(t.get("weight", 1))) for t in tech)
    technical = clamp(q4((sum(D(str(t["rating"])) * D(str(t.get("weight", 1))) for t in tech) / wsum) / D(10))) if wsum else D(0)
    m = int(wk.get("matches", 0))
    if m > 0:
        minutes = clamp(q4(D(int(wk.get("minutes", 0))) / D(m * 90)))
        rating = clamp(q4(D(str(wk.get("avgRating", 0))) / D(10)))
        contrib = clamp(q4(D(int(wk.get("goalContributions", 0))) / D(10)))
        match = q4(D("0.5") * minutes + D("0.4") * rating + D("0.1") * contrib)
    else:
        match = D(0)
    bonus = D(badge_bonus(badges))
    total = int(((D("0.30") * physical + D("0.40") * technical + D("0.30") * match) * D(3000) + bonus).quantize(D(1), rounding=ROUND_HALF_EVEN))
    return total, {"physical": str(physical), "technical": str(technical), "match": str(match), "bonus": str(bonus), "acwr": str(acwr)}


def enrich(a: dict[str, Any]) -> dict[str, Any]:
    season = a.get("season", {})
    badges = season_badges(season, a.get("position"), bool(a.get("captain")))
    total, parts = score(a, badges)
    tr = a.get("training", {})
    status = physical_status(bool(a.get("injuryActive")), D(parts["acwr"]), int(tr.get("readiness", 100)))
    consent = bool((a.get("consent") or {}).get("shareMedicalData"))
    tier = tier_of(total)
    out = {"id": a.get("id"), "name": a.get("name"), "position": a.get("position"), "scoreTotal": total, "tier": tier, "crestIcon": CREST[tier],
           "parts": parts, "badges": badges,
           # dado de saúde só sai do agente com consentimento explícito (Art. 11)
           "physicalStatus": status if consent else None, "medicalShared": consent,
           "attributes": [x for x in [a.get("position") and {"icon": "◎", "label": a["position"].capitalize(), "kind": "pos"},
                                       a.get("age") and {"icon": "", "label": f"{a['age']} anos", "kind": "age"},
                                       a.get("club") and {"icon": "⬢", "label": a["club"], "kind": "club"}] if x],
           "copilotDraft": {"title": f"Semana do gêmeo: {season.get('goals', 0)} gols e prontidão em alta"[:90],
                            "subtitle": f"Score {total} · {len(badges)} badges ativos", "contentType": "TREINO"}}
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--in", dest="inp", type=Path, required=True)
    ap.add_argument("--out", type=Path, default=Path("docs/data/athletes_enriched.json"))
    ap.add_argument("--api", help="URL base da API para publicar rascunhos (opcional)")
    ap.add_argument("--token", help="access token do operador do AGENTE_IA")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    athletes = json.loads(args.inp.read_text(encoding="utf-8"))
    enriched = [enrich(a) for a in athletes]
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(enriched, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"{len(enriched)} atletas enriquecidos → {args.out}")
    if args.api and args.token:
        s = requests.Session(); s.headers.update({"Authorization": f"Bearer {args.token}", "Content-Type": "application/json"})
        for e in enriched:
            body = {**e["copilotDraft"], "window": "GEMEO"}
            if args.dry_run:
                print("[dry-run] POST /v1/posts", body)
            else:
                r = s.post(f"{args.api.rstrip('/')}/v1/posts", data=json.dumps(body), timeout=30)
                print(e["name"], r.status_code)
    return 0


if __name__ == "__main__":
    sys.exit(main())
