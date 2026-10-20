# Technology Stack

## Core Technologies

- **Language**: Kotlin 2.4.10 (Multiplatform target: Android API 24+ & Desktop JVM 17+)
- **Build System**: Gradle 9.2.1 with Kotlin DSL & Gradle Version Catalog (`gradle/libs.versions.toml`)
- **UI Framework**: Compose Multiplatform (Jetpack Compose 1.7.x / Compose Desktop)
- **Architecture**: Unidirectional Data Flow (MVI/MVVM) with reactive StateFlows
- **Dependency Injection**: Koin 4.x (`koin-core`, `koin-compose`, `koin-android`)
- **Asynchronous Execution**: Kotlin Coroutines & Flow (1.10.x) with Dispatchers IO/Default/Main
- **Networking**: Ktor 3.x with CIO engine, ContentNegotiation, and WebSockets
- **Serialization**: `kotlinx.serialization` (JSON configured leniently with `ignoreUnknownKeys = true`, `coerceInputValues = true`)
- **Local Persistence**:
  - AndroidX Room (SQLite database with KSP compiler)
  - AndroidX DataStore Preferences (atomic multiplatform preferences)
- **Image Pipeline**: Coil 3.x (Multiplatform image loader with memory/disk disk caching)
- **Audio Engines**:
  - Android: AndroidX Media3 (ExoPlayer 1.6.x) with custom AudioTrack buffer tuning, background playback service, and media session controls.
  - Desktop: MPV native audio player engine (`libmpv`) with hardware decoding and gapless transitions.
- **Distribution & Release**:
  - Android: Universal & architecture-split APKs (arm64-v8a, armeabi-v7a, x86_64).
  - Windows: Authenticode signed installer (`PulseMusic-v3.1.8-windows-x64-Setup.exe`) published by "Pulse Music Studio" with RFC 3161 DigiCert timestamping.
