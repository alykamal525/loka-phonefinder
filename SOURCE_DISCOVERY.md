# Source discovery

Generated from `registry/providers.json` by `scripts/check_providers.py`. Do not edit by hand.
Sources come from the required list and from web search. Reachability is re-checked daily (see status columns). "Direct lookup" means the app itself reads a result from the source; for every source below this is **no**: no public lookup API was verified, so the app opens the source in the browser and never shows a result it did not retrieve.

Registry updated: 2026-11-27

| Source | Category | Website | Integration | Egypt coverage | Direct lookup / credentials | Registry status | Last checked | Last result |
|---|---|---|---|---|---|---|---|---|
| Google | engine | https://www.google.com/ | search_engine | unverified | no / none | active | not yet | - |
| Bing | engine | https://www.bing.com/ | search_engine | unverified | no / none | active | not yet | - |
| DuckDuckGo | engine | https://duckduckgo.com/ | search_engine | unverified | no / none | active | not yet | - |
| Yahoo | engine | https://search.yahoo.com/ | search_engine | unverified | no / none | active | not yet | - |
| Yandex | engine | https://yandex.com/ | search_engine | unverified | no / none | active | not yet | - |
| Brave Search | engine | https://search.brave.com/ | search_engine | unverified | no / none | active | not yet | - |
| Truecaller | caller_id | https://www.truecaller.com/ | website_lookup | unverified | no / none | active | not yet | - |
| tellows | caller_id | https://www.tellows.com/ | website_lookup | verified_page_loads | no / none | active | not yet | - |
| tellows Egypt | egypt | https://eg.tellows.net/ | homepage_only | egypt_focused_site | no / none | active | not yet | - |
| Sync.ME | caller_id | https://sync.me/ | website_lookup | unverified | no / none | active | not yet | - |
| UnknownPhone | caller_id | https://www.unknownphone.com/ | homepage_only | unverified | no / none | active | not yet | - |
| WhoCallsMe | caller_id | https://whocallsme.com/ | homepage_only | unverified | no / none | active | not yet | - |
| Hiya | caller_id | https://www.hiya.com/ | homepage_only | unverified | no / none | active | not yet | - |
| Getcontact | caller_id | https://www.getcontact.com/ | app_based | unverified | no / none | active | not yet | - |
| CallApp | caller_id | https://callapp.com/ | app_based | unverified | no / none | active | not yet | - |
| Eyecon | caller_id | https://www.eyecon-app.com/ | app_based | unverified | no / none | active | not yet | - |
| Whoscall | caller_id | https://whoscall.com/ | app_based | unverified | no / none | active | not yet | - |
| NumBuster | caller_id | https://numbuster.com/ | app_based | unverified | no / none | active | not yet | - |
| Should I Answer? | caller_id | https://www.shouldianswer.com/ | homepage_only | unverified | no / none | active | not yet | - |
| CallerSmart | caller_id | https://www.callersmart.com/ | homepage_only | unverified (US focused) | no / none | active | not yet | - |
| NoCall (Egypt) | egypt | https://nocall.io/eg | homepage_only | egypt_focused_site | no / none | active | not yet | - |
| Amanak (أمانك) | egypt | https://amanak.app/ | homepage_only | egypt_focused_site | no / none | active | not yet | - |
| CellNumber.info Egypt | egypt | https://egy.cellnumber.info/ | homepage_only | egypt_focused_site | no / none | active | not yet | - |
| Yellow Pages Egypt | egypt | https://yellowpages.com.eg/ | directory_site_search | unverified | no / none | active | not yet | - |
| FindInEgypt | egypt | https://www.findinegypt.com/ | directory_site_search | unverified | no / none | active | not yet | - |
| Dalil IMEX | egypt | https://dalilimex.com/en/ | directory_site_search | unverified | no / none | active | not yet | - |
| WhatsApp | social | https://www.whatsapp.com/ | deep_link | unverified | no / none | active | not yet | - |
| Telegram | social | https://telegram.org/ | deep_link | unverified | no / none | active | not yet | - |
| Facebook | social | https://www.facebook.com/ | website_lookup | unverified | no / none | active | not yet | - |
| Instagram | social | https://instagram.com/ | site_search | unverified | no / none | active | not yet | - |
| LinkedIn | social | https://linkedin.com/ | site_search | unverified | no / none | active | not yet | - |
| X (Twitter) | social | https://x.com/ | site_search | unverified | no / none | active | not yet | - |

## Discovery notes

- Truecaller, Getcontact, Hiya, Sync.ME, CallApp, Eyecon, Whoscall, NumBuster, Should I Answer?, CallerSmart, UnknownPhone and WhoCallsMe are on the required list. Pages that load for a number are linked per number; the rest open the homepage.
- Egyptian directories and spam sites found through web search: Yellow Pages Egypt, FindInEgypt, Dalil IMEX, NoCall, Amanak, tellows Egypt, CellNumber.info Egypt. Their phone-number coverage is not verified.
- Add a source by editing `registry/providers.json` (see README). The app fetches that file on start, caches it, and falls back to the bundled copy.
