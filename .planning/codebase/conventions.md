# Code Conventions & Architecture Rules

## Kotlin Multiplatform Coding Standards

1. **State Management & ViewModel Architecture**:
   - Expose state via `StateFlow` and public immutable read-only properties (`val state: StateFlow<MyState> = _state.asStateFlow()`).
   - UI consumers must collect state with `collectAsStateWithLifecycle()` to prevent background battery drain.
   - Avoid blocking calls (`runBlocking`) on Android Main and UI dispatchers; use `viewModelScope.launch(Dispatchers.IO)`.

2. **Network Resilience & Deserialization**:
   - Always configure JSON deserializers with `ignoreUnknownKeys = true`, `coerceInputValues = true`, and `isLenient = true`.
   - Never assume remote API numerical IDs fit within standard 32-bit signed integers (`Int`); use `Long? = null`.
   - When consuming remote JSON endpoints that may respond with `text/plain` (e.g. GitHub raw files), read the raw string body (`response.bodyAsText()`) and parse with `json.decodeFromString<T>()`.

3. **Lyrics Filtering & Match Validation**:
   - Never fall back to arbitrary search candidates (`firstOrNull()`) when searching for lyrics.
   - Strictly reject candidates flagged with `instrumental == true` or where both `syncedLyrics` and `plainLyrics` are empty.
   - Validate song title and artist boundaries before accepting a candidate match.
   - When no lyrics exist for a song, wipe `lyricsData = null` to prevent empty or broken card rendering.

4. **UI Styling & Liquid Glass**:
   - Desktop and tablet capsules float over the background with real-time luminance sampling (`liquidGlass(backdrop, layer, ...)`).
   - Dynamic Island / Mini Capsule components must support smooth spring animations (`animateContentSize` and `animateDpAsState`) with touch ripple and gesture bounds.
