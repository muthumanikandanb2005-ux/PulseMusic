# Technology Stack

## Core Technologies

- **Language**: Kotlin 2.4.10
- **Build System**: Gradle 9.2.1 with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`)
- **UI Framework**: Jetpack Compose / Compose Multiplatform (Compose BOM 2026.08.00)
- **Dependency Injection**: Koin 4.x (`koin-core`, `koin-compose`, `koin-android`)
- **Asynchronous & Concurrency**: Kotlin Coroutines & Flow (1.10.x)
- **Networking**: Ktor 3.x (CIO engine, ContentNegotiation, Kotlinx Serialization)
- **Serialization**: `kotlinx.serialization` (JSON)
- **Local Persistence**:
  - AndroidX Room (SQLite database with KSP)
  - AndroidX DataStore Preferences (Multiplatform file-based atomic storage)
- **Image Loading**: Coil 3.x (Multiplatform image caching and crossfade)
- **Audio Engines**:
  - Android: AndroidX Media3 (ExoPlayer 1.6.x) with custom AudioTrack buffer tuning and foreground media notification service
  - Desktop: MPV native audio player integration (`libmpv`)
- **Distribution & Packaging**:
  - Android: Gradle `:androidApp:assembleRelease` signed with release keystore
  - Windows: Inno Setup 6 (`scripts/installer.iss`) with Authenticode code-signing (`signtool`) and RFC 3161 DigiCert timestamping
