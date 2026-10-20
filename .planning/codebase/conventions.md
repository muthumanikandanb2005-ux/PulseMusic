# Development Conventions

## Code & Naming Conventions

1. **Kotlin Style**:
   - Follow standard Kotlin official naming guidelines: `PascalCase` for classes and Composable functions, `camelCase` for variables and methods.
   - Resource strings defined in `composeApp/src/commonMain/composeResources/values/strings.xml` accessed via `Res.string.<id>`.

2. **Layering Rules**:
   - UI (`composeApp`) communicates strictly with `Domain` interfaces and `ViewModel`s.
   - Low-level network models are mapped to `Domain` models before crossing into business logic.

3. **Lifecycle & Cleanups**:
   - Media playback bindings and Coroutine jobs must be cancelled on track change to prevent race conditions.
