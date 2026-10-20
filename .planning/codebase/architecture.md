# Architecture Overview

## System Architecture

Pulse Music is an offline-first, high-performance cross-platform music streaming and local playback application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (Jetpack Compose & Desktop Compose)**.

```mermaid
graph TD
    UI[Compose Multiplatform UI Layer<br/>(composeApp)] --> VM[State & ViewModels<br/>(SharedViewModel, PlayerViewModel, ListenTogetherViewModel)]
    UI --> ISLAND[Dynamic Island / MiniCapsule / Hyper Island Subsystem<br/>(MiniPlayer, Floating Island, System Overlay)]
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

### 1. Presentation & UI Layer (`composeApp`)
- **Framework**: Compose Multiplatform with Material 3 expressive components and custom Liquid Glass shader styling.
- **State Management**: Unidirectional Data Flow (UDF) powered by Kotlin Coroutines `StateFlow` and Compose `collectAsStateWithLifecycle`.
- **Player Experiences**:
  - `NowPlayingContentSpotify`: Canvas video loop animations, adaptive gradient color extraction, and dynamic lyrics cards.
  - `NowPlayingExpressiveCards`: Clean card layout with real-time waveform progress and gestures.
  - `NowPlayingContentBitChord`: Dedicated audiophile player with breathing vinyl album art, hairline capsule scrubber, and dynamic ambient glow.
- **Capsule & Island Subsystem**:
  - `MiniPlayer.kt`: Floating bottom liquid glass capsule with transport controls, slider progress, and track metadata.
  - `MiniPlayerLayout.kt` & `MiniPlayerWindow.kt`: Desktop multi-layout mini-player (Compact, Medium, Square, Expanded) with window docking and opacity controls.
  - `Dynamic Island / Hyper Island / Mini Capsule`: Morphing status pill supporting collapsed disc + equalizer indicator, expanding to an interactive floating card with real-time lyrics, sound wave scrubbing, and swipe gesture controls.

### 2. Domain Layer (`core/domain`)
- Independent, pure Kotlin module containing:
  - Entities: `SongEntity`, `AlbumEntity`, `ArtistEntity`, `Lyrics`, `Line`, `UpdateData`.
  - Interfaces: `LyricsCanvasRepository`, `SongRepository`, `PlaylistRepository`, `UpdateRepository`, `ListenTogetherRepository`.
  - Use cases: Business rules for lyrics resolution, version comparison, and playback queue orchestration.

### 3. Data Layer (`core/data`)
- **Offline Persistence**: SQLite Room database with KSP annotations for offline cached songs, history, favorites, and playlists.
- **Preferences**: Multiplatform DataStore Preferences for settings, audio cache size, update endpoints, and UI configurations.
- **Resilient Update System (`UpdateRepositoryImpl.kt`)**: Prioritizes high-speed CDN (`cdn.jsdelivr.net`), secondary raw GitHub, and GitHub API fallback with 64-bit ID deserialization.

### 4. Remote Services Layer (`core/service`)
- `kotlinYtmusicScraper`: Low-overhead YouTube Music API scraper with Faraday signature cipher deciphering and player configs cache.
- `lyricsService`: Multi-provider lyrics resolver prioritizing LRCLIB, strictly rejecting instrumental matches and unmatched titles to guarantee lyrics accuracy.
- `listenTogether`: WebSocket-based synchronized group listening client with server clock drift compensation, auto-reconnect, and timeout hardening.
