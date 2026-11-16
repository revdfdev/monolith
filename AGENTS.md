# Monolith Repository Agent Guidelines

## Architecture & Brain
- Durable repository facts, architectural contracts, and subsystems are documented in the synaptic agent brain at [`docs/agent-memory/INDEX.md`](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md).
- Prior to making architectural decisions or modifying core systems, read the corresponding neuron in `docs/agent-memory/`.

## Key Commands
- **Unit Tests**: `./gradlew testDebugUnitTest`
- **Build APK**: `./gradlew assembleDebug`
- **Install on Device**: `adb -s 0268294a install -r app/build/outputs/apk/debug/app-debug.apk`
- **Launch Launcher**: `adb -s 0268294a shell am start -n com.rehan.monolith/.MainActivity`

## Core Rules & Invariants
1. **No AI Slop / No Third-Party Branding**: Strictly avoid generic AI boilerplate and never mention third-party app names in the codebase, strings, comments, or documentation.
2. **Deterministic Coroutines**: Inject `CoroutineDispatcher` (defaults to `Dispatchers.IO`) into ViewModels so tests can use `UnconfinedTestDispatcher()`.
3. **Test State Isolation**: Reset singletons like `PackageInstallTracker.reset()` in test `@Before` and `@After` methods.
4. **Security Contracts**: Validate all IPC arguments in `ShizukuCommands.kt`; execute using `ProcessBuilder` without raw shell expansion.
