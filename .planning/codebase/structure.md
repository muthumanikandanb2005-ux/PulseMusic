# Codebase Directory Structure

```
simpmusic-clone/
├── androidApp/                                  # Android application module
│   ├── src/main/java/com/maxrave/simpmusic/
│   │   ├── MainActivity.kt                      # Main activity, system bar insets, edge-to-edge
│   │   └── service/update/AppUpdateWorker.kt    # Periodic WorkManager background OTA update checker
│   └── src/main/res/                            # Android icons, splash screen, strings
├── composeApp/                                  # Shared Compose Multiplatform UI
│   ├── src/commonMain/kotlin/com/maxrave/simpmusic/
│   │   ├── App.kt                               # Root composable, theme providers, navigation scaffold
│   │   ├── ui/
│   │   │   ├── component/                       # Reusable UI widgets
│   │   │   │   ├── AppBottomNavigationBar.kt    # Navigation bar & liquid glass tab bar
│   │   │   │   ├── FullscreenLyricsContent.kt   # High-performance synchronized lyrics view
│   │   │   │   ├── LyricsView.kt                # Line-by-line animated lyrics renderer
│   │   │   │   ├── LiquidGlassContainer.kt      # Frosted glass background with dynamic luminance
│   │   │   │   └── PlaybackIndicators.kt        # Equalizer bars, spinning disc, audio format pills
│   │   │   ├── screen/
│   │   │   │   ├── MiniPlayer.kt                # Floating liquid glass mini-player capsule
│   │   │   │   ├── home/HomeScreen.kt           # Home dashboard, quick picks, recent releases
│   │   │   │   ├── home/SettingScreen.kt        # App settings, OTA updater UI, audio options
│   │   │   │   └── player/content/              # NowPlaying player screen variants
│   │   │   │       ├── NowPlayingContentSpotify.kt
│   │   │   │       ├── NowPlayingExpressiveCards.kt
│   │   │   │       └── NowPlayingContentBitChord.kt
│   │   │   └── theme/                           # Color tokens, typography, shapes, dynamic palettes
│   │   └── viewModel/
│   │       ├── SharedViewModel.kt               # Central playback controller & reactive UI state
│   │       ├── PlayerViewModel.kt               # Track queue, shuffle, repeat, sleep timer
│   │       └── ListenTogetherViewModel.kt       # P2P synced session management
│   ├── src/jvmMain/kotlin/com/maxrave/simpmusic/
│   │   └── ui/mini_player/                      # Desktop Mini Player floating window
│   │       ├── MiniPlayerLayout.kt              # Compact, Medium, Square, and Expanded layouts
│   │       ├── MiniPlayerManager.kt             # Window position, docking, always-on-top controller
│   │       └── MiniPlayerWindow.kt              # Frameless draggable desktop window
│   └── src/androidMain/                         # Android-specific Compose integrations & native views
├── core/
│   ├── common/                                  # Shared utility functions, extensions, Logger
│   ├── domain/                                  # Pure Kotlin business entities & repository contracts
│   ├── data/                                    # SQLite Room DB, DataStore, repository implementations
│   ├── media/                                   # Audio engine interfaces and platform implementations
│   │   ├── media3/                              # Android ExoPlayer implementation with wake locks
│   │   └── jvm/                                 # Desktop native MPV player bindings
│   └── service/                                 # Remote network clients
│       ├── kotlinYtmusicScraper/                # YouTube Music streaming scraper
│       ├── lyricsService/                       # LRCLIB, BetterLyrics API & LRC text parsers
│       ├── listenTogether/                      # WebSocket group synchronization protocol
│       ├── spotify/                             # Spotify Canvas video fetching
│       └── aiService/                           # Gemini LLM translation and lyrics romanization
├── scripts/                                     # Windows Inno Setup installer scripts & signing tools
├── dist/                                        # Generated release APKs, EXE setup, CER, SHA256SUMS
└── gradle/                                      # Gradle version catalog (libs.versions.toml)
```
