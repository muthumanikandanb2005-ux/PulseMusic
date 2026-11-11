# Known Concerns & Mitigations

## 1. Lyrics Precision & "Unknown Lyrics" Mismatch
- **Issue**: Songs without lyrics previously displayed unrelated or "unknown" songs' lyrics due to aggressive fallback heuristics (`rs.find { abs(duration - duration) <= 10 } ?: rs.firstOrNull()`).
- **Mitigation**: Require strict normalized title and artist validation. Never fall back to arbitrary results. If no exact or title-verified match exists, return null so the UI cleanly suppresses the lyrics card or displays an unavailable state.

## 2. GitHub OTA Rate Limits (403 Forbidden)
- **Issue**: Unauthenticated GitHub API requests hit a 60 req/hr rate limit per IP on shared mobile carrier CGNAT networks, causing 403 Forbidden errors when checking for updates.
- **Mitigation**: Prioritize jsDelivr CDN (`https://cdn.jsdelivr.net/gh/muthumanikandanb2005-ux/PulseMusic@main/version.json`) as the primary update endpoint. It caches `version.json` with zero rate limits and standard `application/json` content type.

## 3. Deserialization Crashes ("Server Error 200" / `NoTransformationFoundException`)
- **Issue**: GitHub raw content serves `ContentType: text/plain; charset=utf-8`. If the HTTP client's content negotiation only registers `application/json`, Ktor fails with `NoTransformationFoundException` even though the HTTP status is 200. Furthermore, GitHub release IDs exceed 32-bit integer limits (`id: 418500000` / asset IDs `659272996`).
- **Mitigation**: Parse the response body string directly via `response.bodyAsText()` and deserialize with `normalJson.decodeFromString<GithubResponse>()`. All integer ID fields are typed as `Long?` with default `= null`.

## 4. Listen Together Reconnection & Latency Lockup
- **Issue**: Users were unable to reconnect after leaving a session or experienced extreme latency due to stale room tokens and uncancelled ping coroutines on closed sockets.
- **Mitigation**: Connection timeouts reduced to 5-8s. Purge `sessionToken` on room exit, cancel pending handshake jobs, and explicitly cancel socket pingers upon disconnect.

## 5. Dynamic Island / Mini Capsule Hardware Notch Fragmentation
- **Issue**: Android device cutouts vary across center punch-hole, pill notch (iPhone / HyperOS), left-side hole, and notchless displays.
- **Mitigation**: Read Android `WindowInsets.displayCutout` dynamically to determine camera cutout geometry, and provide user-adjustable horizontal/vertical offset and width controls in Settings.
