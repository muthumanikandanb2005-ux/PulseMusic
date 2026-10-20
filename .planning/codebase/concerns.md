# Known Concerns & Mitigations

## 1. Lyrics Precision & Fallback Integrity
- **Issue**: Songs without lyrics previously displayed unrelated or "unknown" songs' lyrics due to aggressive fallback heuristics (`rs.find { abs(duration - duration) <= 10 } ?: rs.firstOrNull()`).
- **Mitigation**: Require strict normalized title and artist validation. Never fall back to arbitrary results. If no exact or title-verified match exists, return null so the UI cleanly suppresses the lyrics card or displays an unavailable state.

## 2. GitHub OTA Rate Limits
- **Issue**: GitHub API unauthenticated requests hit a 60 req/hr rate limit per IP on public cellular networks (CGNAT), causing 403 Forbidden errors when checking for updates.
- **Mitigation**: Prioritize jsDelivr CDN (`cdn.jsdelivr.net`) as the primary update endpoint. It caches `version.json` with zero rate limits and standard `application/json` content type.

## 3. Windows Code-Signing Trust
- **Issue**: Windows SmartScreen alerts when binaries are unsigned.
- **Mitigation**: Automatically sign both `Pulse.exe` and `PulseMusic-v3.1.8-windows-x64-Setup.exe` with the Authenticode certificate for "Pulse Music Studio" using RFC 3161 DigiCert timestamping, bundled with `trust_publisher.bat`.
