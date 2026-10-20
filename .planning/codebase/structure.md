# Codebase Structure

```
.
├── androidApp/          # Android entry point, MainActivity, Android Manifest, WorkManager update worker
├── desktopApp/          # Desktop entry point (main.kt), Conveyor config, Windows packaging assets
├── composeApp/          # Compose Multiplatform shared UI, Navigation, ViewModels, Themes, Screens
│   ├── src/commonMain/kotlin/com/maxrave/simpmusic/
│   │   ├── ui/
│   │   │   ├── component/       # LyricsView, FullscreenLyricsContent, Player controls
│   │   │   ├── screen/          # Home, Player, Library, Search, Setting screens
│   │   │   └── theme/           # Color schemes, typography, shapes
│   │   └── viewModel/           # SharedViewModel, NowPlayingScreenData, State flows
├── core/
│   ├── common/          # Cross-cutting utilities, extensions, platform helpers
│   ├── domain/          # Pure entities, domain repositories, use cases
│   ├── data/            # Room database, DAO, repository implementations, mapping
│   ├── media/           # Media players (Media3 for Android, libmpv for JVM Desktop)
│   └── service/         # Scrapers, API clients (YouTube, Lyrics, Spotify, AI)
├── scripts/             # Windows build installer (.iss, .bat), signing scripts, packaging tools
└── gradle/              # Version catalog (libs.versions.toml), wrapper
```
