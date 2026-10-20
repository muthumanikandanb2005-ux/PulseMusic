# Code Quality & Standards

## Code Quality Status

- **Kotlin Compatibility**: Uses Kotlin 2.4.10 and modern coroutines.
- **Type Safety**: Strongly typed domain models with explicit nullability contracts.
- **Serialization**: `kotlinx.serialization` with `ignoreUnknownKeys = true` and nullable fields with defaults to prevent runtime deserialization exceptions on partial API payloads.
- **Concurrency**: Coroutines scoped strictly to `viewModelScope` and background `Dispatchers.IO`. No blocking `runBlocking` calls on the main UI thread.
- **UI Architecture**: Declarative Compose components with unified state hoisted to ViewModel `StateFlow`s.
