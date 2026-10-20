# Architecture Overview

## System Architecture

Pulse Music is built as an offline-first, high-performance cross-platform application utilizing **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (Jetpack / Desktop Compose)**.

```mermaid
graph TD
    UI[Compose Multiplatform UI Layer<br/>(composeApp)] --> VM[State & ViewModels<br/>(SharedViewModel, PlayerViewModel, etc.)]
    VM --> DOMAIN[Domain Layer<br/>(core/domain)]
    DOMAIN --> DATA[Data & Repository Layer<br/>(core/data)]
    DATA --> LOCAL[Local Storage<br/>(Room Database, DataStore Preferences)]
    DATA --> REMOTE[Remote Services Layer<br/>(core/service)]
    
    subgraph Services
        REMOTE --> YT[kotlinYtmusicScraper]
        REMOTE --> LYRICS[lyricsService (LRCLIB, BetterLyrics)]
        REMOTE --> SPOTIFY[spotify (Canvas, Metadata)]
        REMOTE --> AI[aiService (Gemini, Lyrics Translation)]
        REMOTE --> SYNC[listenTogether (WebSocket P2P Session)]
    end

    subgraph Platform Targets
        UI --> ANDROID[androidApp (Media3, Foreground Service, WorkManager)]
        UI --> DESKTOP[desktopApp (MPV Native Audio Engine, Conveyor/InnoSetup)]
    end
```

## Architectural Layers

1. **Presentation / UI (`composeApp`)**:
   - Written in Compose Multiplatform.
   - Reactive state handling using Kotlin Coroutines `StateFlow` and Compose `collectAsStateWithLifecycle`.
   - Distinct, rich player styles: Classic, Spotify-style with animated gradient cards, Apple Music tabbed player with real-time blur and fluid nested scrolling.

2. **Domain (`core/domain`)**:
   - Pure Kotlin module defining business entities (`SongEntity`, `AlbumEntity`, `Lyrics`, `Line`), repository interfaces (`LyricsCanvasRepository`, `PlaylistRepository`, `SongRepository`), and core domain use-cases.

3. **Data (`core/data`)**:
   - Repository implementations (`LyricsCanvasRepositoryImpl`, `SongRepositoryImpl`, `UpdateRepositoryImpl`).
   - SQLite Room database for persistent caching and offline playback.
   - Multiplatform DataStore for settings and user preferences.

4. **Service Modules (`core/service`)**:
   - `kotlinYtmusicScraper`: Low-overhead YouTube Music API scraper with Faraday signature cipher deciphering and player configs cache.
   - `lyricsService`: Multi-provider lyrics resolver strictly prioritized to LRCLIB, with BetterLyrics and Apple Music LRC/TTML parsing.
   - `listenTogether`: Real-time synchronized listening sessions over WebSockets with server clock drift compensation.
