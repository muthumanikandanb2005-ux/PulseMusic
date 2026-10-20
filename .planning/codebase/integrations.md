# Integrations

## External Services & Endpoints

1. **LRCLIB (`https://lrclib.net`)**:
   - Used for synchronized and unsynchronized lyrics.
   - Identified by dedicated client token: `Q577SzaQFl01aTiReKY9NKgkcENPwiaHVceaOk4p8JQ`.
   - Endpoints: `/api/get` (exact lookup), `/api/search` (fuzzy search).

2. **YouTube Music (ytmusicapi & innerTube)**:
   - Scraped through `kotlinYtmusicScraper`.
   - Dynamic cipher deciphering via `RemotePlayerConfigStore` and Metrolist Faraday registries.

3. **OTA Update Endpoints**:
   - Primary: `https://cdn.jsdelivr.net/gh/muthumanikandanb2005-ux/PulseMusic@main/version.json` (High-speed CDN, never rate limited).
   - Secondary: `https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/version.json`.
   - Fallback: GitHub Releases API `https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/latest`.

4. **Spotify Web API**:
   - Canvas animations and metadata enhancement.

5. **AI Translation**:
   - Gemini API integration for translating lyrics into user-selected languages.
