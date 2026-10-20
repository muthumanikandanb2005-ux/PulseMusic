# Testing & Verification Strategy

## Test Suite Architecture

Pulse Music maintains unit and integration verification across all KMP modules:

1. **Service Tests (`core/service/kotlinYtmusicScraper/src/commonTest`)**:
   - `GithubResponseDeserializationTest`: Verifies parsing of GitHub release metadata with 64-bit integer IDs (`Long`), multi-gigabyte asset byte sizes, missing optional fields, and reactions objects.

2. **Domain Tests (`core/domain/src/commonTest`)**:
   - `UpdateVersionTest`: Rigorously verifies semantic versioning comparisons (`isNewerVersion`) across patch, minor, and major bumps (`3.1.8` vs `3.1.7`, `3.1.7-beta` vs `3.1.8`).

3. **Lyrics Resolution Tests (`core/service/lyricsService/src/commonTest`)**:
   - `LyricsMatchingAndParsingTest`: Validates strict title cleaning (stripping `[Audio]`, `(Official Music Video)`, `(Remastered)`), rejection of instrumental tracks, and blank card prevention when lyrics are missing.

4. **Listen Together Protocol Tests (`core/service/listenTogether/src/commonTest`)**:
   - `ListenTogetherTest`: Validates WebSocket message serialization/deserialization, room state transitions, and session token renewal on reconnection.

5. **Gradle Test Execution**:
   - Execute all module JVM tests: `./gradlew :domain:jvmTest :kotlinYtmusicScraper:jvmTest :lyricsService:jvmTest :listenTogether:jvmTest`
   - Compile UI and Compose layers: `./gradlew :composeApp:compileKotlinJvm`
