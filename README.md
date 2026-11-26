# Loka PhoneFinder

Android app (Kotlin, Jetpack Compose, Room, libphonenumber). Arabic (RTL) by default, English supported, dark UI.

What it does:
- Validates and normalizes a phone number (all Egyptian mobile prefixes and international numbers) and generates number variants and Arabic/English search phrases.
- Opens each source (search engines, caller-ID sites, Egyptian directories, social/deep links) in the browser from a provider card with an honest status. It never shows a result it did not retrieve itself. No scraping, no CAPTCHA or login bypass, no private APIs.
- Keeps an optional local search history (Room) that the user can delete.

Provider list: `registry/providers.json` is fetched by the app on start (raw GitHub URL), cached locally, and a bundled copy is the fallback. A GitHub Action (`Check provider registry`) re-checks every URL daily, flags a provider as `dead` after two failed checks, and regenerates `SOURCE_DISCOVERY.md`.

Add a source: add an object to `registry/providers.json` with `id`, `name`, `category` (engine, caller_id, egypt, social), `type`, `url` (template with `{query}`, `{e164}`, `{e164digits}`, `{nsn}`, `{national}`, `{region_lc}`, `{cc}`, or empty), `homepage`, `note_en`, `note_ar`, `modes` (quick, deep, engines). Commit it and the app picks it up.

Build: run the `Build LOKA PhoneFinder APK` workflow. It publishes `LOKA-PhoneFinder.apk` as a release. The build is debug-signed with a fresh key, so uninstall older builds first.
