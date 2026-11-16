# Neuron: Core Architecture & Data Layer

## Synapses
- Upstream: [Central Index](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md)
- Cross-links:
  - [Package Installation Engine](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-package-installer.md)
  - [Shizuku IPC](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-shizuku-ipc.md)
  - [Traps & Contracts](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-traps-and-contracts.md)

---

## 1. Entry Points & Process Hierarchy

- **`MonolithApp.kt`**: Subclasses `Application`, initializes Koin dependency injection via `startKoin` with `appModule`.
- **`MainActivity.kt`**: Main launcher activity (`android.intent.category.HOME`, `DEFAULT`, `LAUNCHER`). Configured with `launchMode="singleTask"` and window insets handling for edge-to-edge rendering.
- **`PackageInstallActivity.kt`**: Dedicated translucent activity for handling `ACTION_VIEW` and `ACTION_INSTALL_PACKAGE` intents. It runs on `Theme.Monolith.TranslucentDialog` over caller apps (browsers, file managers, messaging apps) without navigating to or disrupting the launcher home screen.

## 2. Dependency Injection (Koin)

Located in `MonolithApp.kt`:
- **Singletons**:
  - `ShizukuManager`: handles Shizuku lifecycle, status flow, and AIDL transactions.
  - `AppRepository`: caches and computes installed apps, usage stats, tags, favorites, and hidden apps.
  - `LauncherPrefs`: persistent DataStore-backed preferences.
  - `CoroutineDispatcher`: defaults to `Dispatchers.IO` (overridable in unit tests via `UnconfinedTestDispatcher`).
- **ViewModels**:
  - `LauncherViewModel`: central UI state orchestrator for app lists, search queries, package install status, wellbeing limits, and kid mode.

## 3. Data & State Management

- **`AppRepository`**:
  - Collects apps via `LauncherApps` and `PackageManager`.
  - Exposes `apps: StateFlow<List<AppItem>>`.
  - Maintains tag mappings, custom labels, hidden apps set, and pinned home favorites.
  - Exposes `refresh()` to invalidate and re-query after package installations or uninstalls.
- **`LauncherPrefs` & `MonolithSettings`**:
  - Key-value configurations backed by SharedPreferences/DataStore.
  - Contains toggles for minimal widgets, clock formats, haptic feedback, search auto-open keyboard, and screen-time monitoring.
- **Reactive Flow Pipeline**:
  - State emitted via Kotlin `StateFlow` and consumed in Jetpack Compose via `collectAsState()` or `collectAsStateWithLifecycle()`.
