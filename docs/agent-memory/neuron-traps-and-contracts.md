# Neuron: Traps, Pitfalls & Invariants

## Synapses
- Upstream: [Central Index](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md)
- Cross-links:
  - [Core Architecture](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-core-architecture.md)
  - [Package Installation Engine](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-package-installer.md)
  - [Shizuku IPC](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-shizuku-ipc.md)

---

## 1. Unit Testing & Coroutine Dispatchers
- **Trap**: Using hardcoded `Dispatchers.IO` in ViewModels causes asynchronous background jobs to race against unit test assertions in Robolectric / JUnit, resulting in flaky or failing assertions.
- **Contract**: `LauncherViewModel` accepts an injectable `ioDispatcher: CoroutineDispatcher = Dispatchers.IO`. Tests must pass `UnconfinedTestDispatcher()` to ensure immediate synchronous execution of coroutine blocks.

## 2. Singleton State Leakage
- **Trap**: `PackageInstallTracker` is an `object` singleton. If state transitions to `Error` or `Success` in one test, subsequent tests will read the dirty state.
- **Contract**: All test suites testing package installation must invoke `PackageInstallTracker.reset()` in both `@Before setUp()` and `@After tearDown()`.

## 3. Shizuku Flag Validation
- **Trap**: Passing unverified flags or composite strings like `"-i com.android.vending"` to `pm install` throws `IllegalArgumentException` in `ShizukuCommands.kt`.
- **Contract**: Argument parsing must strictly validate individual argv tokens. Compound flags must be split into separate elements (`"-i"` followed by validated package name `com.android.vending`).

## 4. Android 12+ Foreground Service Restrictions
- **Trap**: Android 12+ (API 31+) strictly limits starting foreground services from the background, throwing `ForegroundServiceStartNotAllowedException`.
- **Contract**: `PackageInstallService.startInstall()` must only be triggered in response to direct user interaction (such as tapping "Install" or "Run in Background" inside `PackageInstallActivity` or `MonolithLauncher`). The service must declare `FOREGROUND_SERVICE_TYPE_DATA_SYNC` and provide `startForeground` with a valid notification within milliseconds of `onStartCommand`.

## 5. File Descriptor & Cache Cleanups
- **Trap**: Extracting bundles (`.xapk`, `.apks`, `.zip`) creates multiple temporary files in the cache directory (`bundle_<timestamp>/`). Leaking these files consumes internal device storage.
- **Contract**: Once installation completes (whether `Success` or `Error`), `PackageInstallService` and `ApkTools` must clean up both individual APK splits and the parent staging directory using `deleteRecursively()`.

## 6. Container Staging & Extension Handling
- **Trap**: Streaming a content URI to a generic `.apk` filename strips the source container format, breaking bundle detection for `.xapk`, `.apks`, and `.apkm` archives.
- **Contract**: When querying the content resolver `DISPLAY_NAME`, retain the source file extension in the staging file name so the container parser applies the correct archive analysis strategy.

## 7. Session Cleanup Contract
- **Trap**: If an install session aborts during streaming or commit without abandoning the session, Android's `PackageInstaller` keeps open staging files and session locks indefinitely.
- **Contract**: In `ShizukuShellService.installMultiple`, whenever `installWrite` or `installCommit` fails, immediately invoke `installAbandon(sessionId)`.
