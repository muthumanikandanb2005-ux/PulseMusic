# Integrations

## External Services & Endpoints

1. **LRCLIB (`https://lrclib.net`)**:
   - Official API integration for synchronized and plain lyrics.
   - Identified by registered client token: `Q577SzaQFl01aTiReKY9NKgkcENPwiaHVceaOk4p8JQ`.
   - Strict filtering rejects `instrumental` and mismatching titles to prevent incorrect lyrics on instrumental or unindexed songs.

2. **YouTube Music (InnerTube & ytmusicapi)**:
   - Primary music catalog and streaming audio provider via `kotlinYtmusicScraper`.
   - Dynamic cipher deciphering using remote Faraday player configs and format signature cache.

3. **High-Speed CDN & OTA Distribution**:
   - Primary CDN: `https://cdn.jsdelivr.net/gh/muthumanikandanb2005-ux/PulseMusic@main/version.json` (zero rate limits, instant worldwide edge distribution).
   - Secondary Raw GitHub: `https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/version.json` with lenient text/plain decoding.
   - Fallback GitHub Releases API: `https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/latest`.

4. **Listen Together WebSocket Synchronization**:
   - WebSocket real-time synchronization server (`sync.simpmusic.org`) for shared playback.
   - Robust reconnection protocol with token cleanup, 5s socket timeouts, and uncancelled ping loop safeguards.

5. **Spotify Web API**:
   - Spotify Canvas video animations and artwork enrichment.

6. **Gemini AI Translation**:
   - On-demand lyrics translation and pronunciation romanization for multilingual tracks.
