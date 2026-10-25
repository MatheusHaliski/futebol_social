#!/usr/bin/env python3
"""
ecosystem_discovery_agent.py — agente de descoberta do ecossistema (clubes, ligas e federações) para a aba
Explorar (globo) e para o RoleRegistry de papéis futuros.

Fontes abertas: Wikidata (SPARQL) e, opcionalmente, um CSV local. Só dados de pessoas jurídicas (clubes/ligas) —
nenhum dado pessoal é coletado (LGPD por design).

Uso:
  python scripts/ecosystem_discovery_agent.py --country BR --limit 300 --out docs/data/clubs_br.json
  python scripts/ecosystem_discovery_agent.py --csv meus_clubes.csv --out docs/data/clubs.json
  python scripts/ecosystem_discovery_agent.py --country BR --push http://localhost:8080 --token $ACCESS_TOKEN   # opcional

Saída (JSON): [{"name","country","city","lat","lng","founded","league","wikidata","division":null,"tier":null}]
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
import time
from pathlib import Path
from typing import Any

import requests

WIKIDATA = "https://query.wikidata.org/sparql"
UA = "FootballAISocial-DiscoveryAgent/0.1 (+https://<PREENCHER_DOMINIO>; contato: <PREENCHER_EMAIL>)"

SPARQL = """
SELECT ?club ?clubLabel ?countryCode ?cityLabel ?coord ?founded ?leagueLabel WHERE {
  ?club wdt:P31 wd:Q476028 ;              # association football club
        wdt:P17 ?country .
  ?country wdt:P297 "%(country)s" .
  BIND("%(country)s" AS ?countryCode)
  OPTIONAL { ?club wdt:P159 ?city . ?city wdt:P625 ?coord . }
  OPTIONAL { ?club wdt:P625 ?coord . }
  OPTIONAL { ?club wdt:P571 ?founded . }
  OPTIONAL { ?club wdt:P118 ?league . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "pt,en". }
}
LIMIT %(limit)d
"""


def parse_point(wkt: str | None) -> tuple[float | None, float | None]:
    # "Point(-49.27 -25.43)" → (lat, lng)
    if not wkt or not wkt.startswith("Point("):
        return None, None
    lng, lat = wkt[6:-1].split()
    return float(lat), float(lng)


def from_wikidata(country: str, limit: int, retries: int = 3) -> list[dict[str, Any]]:
    q = SPARQL % {"country": country.upper(), "limit": limit}
    for attempt in range(1, retries + 1):
        try:
            r = requests.get(WIKIDATA, params={"query": q, "format": "json"}, headers={"User-Agent": UA}, timeout=60)
            if r.status_code == 429:
                time.sleep(5 * attempt)
                continue
            r.raise_for_status()
            rows = r.json()["results"]["bindings"]
            break
        except requests.RequestException as e:
            if attempt == retries:
                raise
            print(f"tentativa {attempt} falhou: {e}", file=sys.stderr)
            time.sleep(2 * attempt)
    out: dict[str, dict[str, Any]] = {}
    for b in rows:
        wid = b["club"]["value"].rsplit("/", 1)[-1]
        lat, lng = parse_point(b.get("coord", {}).get("value"))
        item = out.setdefault(wid, {
            "name": b["clubLabel"]["value"], "country": b["countryCode"]["value"], "city": b.get("cityLabel", {}).get("value"),
            "lat": lat, "lng": lng, "founded": (b.get("founded", {}).get("value") or "")[:4] or None,
            "league": b.get("leagueLabel", {}).get("value"), "wikidata": wid, "division": None, "tier": None,
        })
        if item["lat"] is None and lat is not None:
            item["lat"], item["lng"] = lat, lng
    return sorted(out.values(), key=lambda c: c["name"])


def from_csv(path: Path) -> list[dict[str, Any]]:
    with path.open(encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    out = []
    for r in rows:
        out.append({"name": r.get("name"), "country": (r.get("country") or "BR").upper(), "city": r.get("city"),
                    "lat": float(r["lat"]) if r.get("lat") else None, "lng": float(r["lng"]) if r.get("lng") else None,
                    "founded": r.get("founded"), "league": r.get("league"), "wikidata": r.get("wikidata"),
                    "division": int(r["division"]) if r.get("division") else None, "tier": None})
    return out


def push(api: str, token: str, clubs: list[dict[str, Any]]) -> None:
    """Opcional: cadastra clubes como contas CLUBE pendentes de verificação (sem dados pessoais)."""
    s = requests.Session()
    s.headers.update({"Authorization": f"Bearer {token}", "Content-Type": "application/json"})
    for c in clubs:
        payload = {"role": "CLUBE", "email": f"<PREENCHER_EMAIL_CLUBE_{c['wikidata'] or c['name']}>", "password": "<PREENCHER_SENHA_FORTE>",
                   "username": "".join(ch for ch in c["name"].lower() if ch.isalnum() or ch in "._")[:30] or "clube",
                   "displayName": c["name"][:80], "acceptTerms": True, "termsVersion": "2026.10",
                   "profile": {"legalName": c["name"], "lat": c["lat"], "lng": c["lng"], "foundedYear": c["founded"], "division": c["division"]}}
        r = s.post(f"{api}/v1/auth/register", data=json.dumps(payload), timeout=30)
        print(c["name"], r.status_code)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--country", default="BR")
    ap.add_argument("--limit", type=int, default=300)
    ap.add_argument("--csv", type=Path)
    ap.add_argument("--out", type=Path, default=Path("docs/data/clubs.json"))
    ap.add_argument("--push", help="URL base da API (opcional)")
    ap.add_argument("--token", help="access token de um OFICIAL_LIGA (para --push)")
    args = ap.parse_args()
    clubs = from_csv(args.csv) if args.csv else from_wikidata(args.country, args.limit)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(clubs, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"{len(clubs)} clubes → {args.out}")
    if args.push:
        if not args.token:
            print("--push exige --token", file=sys.stderr)
            return 2
        push(args.push.rstrip("/"), args.token, clubs)
    return 0


if __name__ == "__main__":
    sys.exit(main())
