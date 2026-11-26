#!/usr/bin/env python3
"""Re-checks every provider in registry/providers.json and regenerates SOURCE_DISCOVERY.md.

Rules (no fake data):
- ok          : HTTP 2xx/3xx
- restricted  : HTTP 401/403/429 (site is alive but blocks automated requests)
- fail        : 404/410/5xx, DNS error, timeout
A provider is flagged status=dead after 2 consecutive fails; it goes back to active on the next ok/restricted.
Run with --offline to only regenerate the markdown.
"""
import json, sys, datetime, urllib.request, urllib.error, urllib.parse, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
REG = ROOT / "registry" / "providers.json"
MD = ROOT / "SOURCE_DISCOVERY.md"
Q = '"+201012345678" OR "01012345678" OR "201012345678"'
SAMPLE = {"e164": "%2B201012345678", "e164digits": "201012345678", "nsn": "1012345678",
          "national": "01012345678", "region_lc": "eg", "cc": "20", "query": urllib.parse.quote(Q)}
UA = "Mozilla/5.0 (compatible; LokaPhoneFinderRegistryCheck/1.0; +https://github.com/alykamal525/loka-phonefinder)"


def fill(t):
    for k, v in SAMPLE.items():
        t = t.replace("{" + k + "}", v)
    return t


def check(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept-Language": "en"})
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            return "ok", str(r.status)
    except urllib.error.HTTPError as e:
        if e.code in (401, 403, 429):
            return "restricted", str(e.code)
        return "fail", str(e.code)
    except Exception as e:
        return "fail", type(e).__name__


def write_md(data):
    lines = ["# Source discovery", "",
             "Generated from `registry/providers.json` by `scripts/check_providers.py`. Do not edit by hand.",
             "Sources come from the required list and from web search. Reachability is re-checked daily (see status columns). \"Direct lookup\" means the app itself reads a result from the source; for every source below this is **no**: no public lookup API was verified, so the app opens the source in the browser and never shows a result it did not retrieve.", "",
             f"Registry updated: {data.get('updated','')}", "",
             "| Source | Category | Website | Integration | Egypt coverage | Direct lookup / credentials | Registry status | Last checked | Last result |",
             "|---|---|---|---|---|---|---|---|---|"]
    for p in data["providers"]:
        lines.append("| {} | {} | {} | {} | {} | no / none | {} | {} | {} |".format(
            p["name"], p["category"], p["homepage"], p["type"], p.get("egypt", ""), p.get("status", ""),
            p.get("lastChecked") or "not yet", p.get("lastResult") or "-"))
    lines += ["", "## Discovery notes", "",
              "- Truecaller, Getcontact, Hiya, Sync.ME, CallApp, Eyecon, Whoscall, NumBuster, Should I Answer?, CallerSmart, UnknownPhone and WhoCallsMe are on the required list. Pages that load for a number are linked per number; the rest open the homepage.",
              "- Egyptian directories and spam sites found through web search: Yellow Pages Egypt, FindInEgypt, Dalil IMEX, NoCall, Amanak, tellows Egypt, CellNumber.info Egypt. Their phone-number coverage is not verified.",
              "- Add a source by editing `registry/providers.json` (see README). The app fetches that file on start, caches it, and falls back to the bundled copy."]
    MD.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    data = json.loads(REG.read_text(encoding="utf-8"))
    if "--offline" not in sys.argv:
        today = datetime.date.today().isoformat()
        for p in data["providers"]:
            url = fill(p["url"]) if p.get("url") else p["homepage"]
            res, detail = check(url)
            p["lastChecked"] = today
            p["lastResult"] = f"{res} {detail}"
            if res == "fail":
                p["failCount"] = int(p.get("failCount", 0)) + 1
            else:
                p["failCount"] = 0
            p["status"] = "dead" if p["failCount"] >= 2 else "active"
            print(p["id"], res, detail)
        data["updated"] = today
    REG.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_md(data)


main()
