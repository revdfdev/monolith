# Neuron: Repository Modifications & Architectural Delta

## Synapses
- Upstream: [Central Index](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md)
- Cross-links:
  - [Core Architecture](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-core-architecture.md)
  - [Package Installation Engine](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-package-installer.md)
  - [Shizuku IPC](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-shizuku-ipc.md)
  - [Traps & Contracts](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-traps-and-contracts.md)

---

## 1. Summary of Changes & Drift Correction

### A. Branding & Anti-Slop Purge
- Completely removed all third-party app names (e.g. Curbox, InstallerX, etc.) from UI strings, comments, and variables across:
  - `MindfulPause.kt`
  - `UsageLimits.kt`
  - `WidgetManager.kt`
  - `MonolithLauncher.kt`

### B. Package Installation Subsystem
- **New Files**:
  - `PackageInstallActivity.kt`: Translucent dialog activity (`Theme.Monolith.TranslucentDialog`) handling `ACTION_VIEW` and `ACTION_INSTALL_PACKAGE` without disrupting home.
  - `PackageInstallService.kt`: Foreground service (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`) managing ongoing progress notifications, Shizuku session streaming, and completion notifications.
  - `PackageLoadingDialog.kt`: Modal progress dialog showing transferred bytes, total size, and cancellation.
  - `app/src/main/res/values/themes.xml`: Declares translucent floating window theme.
  - `app/src/main/res/xml/file_paths.xml`: FileProvider configuration for external cache paths.
- **Engine Capabilities in `ApkTools.kt`**:
  - Container detection: `.apk`, `.xapk`, `.apks`, `.apkm`, multi-APK `.zip`.
  - Staging & split extraction to temporary cache (`bundle_<timestamp>/`).
  - Base APK discovery (`base.apk`, `base-master.apk`, `standalone.apk`).
  - Device ABI compatibility filtering based on `Build.SUPPORTED_ABIS`.
  - Pre-flight signature verification (`signaturesMatch`) and downgrade classification.

### C. Shizuku Privileged Layer
- **`IShizukuShellService.aidl`**: Added `installMultiple(in ParcelFileDescriptor[] apks, in String[] names, in long[] sizes, in String[] flags) = 18;`.
- **`ShizukuCommands.kt`**: Added command builders for atomic session workflow: `installCreate`, `installWrite`, `installCommit`, `installAbandon`.
- **`ShizukuShellService.kt`**: Implemented session streaming pipeline with automatic abandon on failure.
- **`ShizukuOps.kt` & `ShizukuManager.kt`**: Added `installSplitApks(apkPaths, flags)` method.

### D. ViewModel & Test Determinism
- **`LauncherViewModel.kt`**: Injected `ioDispatcher` (defaults to `Dispatchers.IO`) allowing synchronous test execution via `UnconfinedTestDispatcher()`.
- **`PackageInstallTracker`**: Introduced observable singleton state flow; added state reset hooks (`PackageInstallTracker.reset()`) to test fixtures in `LauncherActionsTest.kt`.

### E. Sailfish OS Silica Interaction Architecture
- **`PulleyMenu.kt`**: Gesture-driven top overscroll menu with dampening curves, threshold calculations, and haptic feedback (`HapticFeedbackType.TextHandleMove`, `LongPress`).
- **`HomeLauncherPage.kt`**: Pulley Menu integrated for pull-down anywhere on home; Active Covers layout mode for pinned apps showing real-time usage minutes; pull-up anywhere on home opens the App Drawer. Purged the A-Z alphabet wave scrubber to maintain pure edge-to-edge Sailfish minimalism.
- **`AppDrawer.kt`**: Full-bleed Sailfish Silica app drawer with pull-down gesture to dismiss back to home or activate the Pulley Menu (`VIEW MODE`, `PAUSED APPS`, `AUTO-LAUNCH MATCH`, `CLOSE DRAWER`); removed alphabet wave scrubber.
- **Single Root Home & Settings Hierarchy**: Removed the horizontal root pager and top pager dots. Home is now the single root screen. Long-press bottom sheet shortcuts have been removed. Privileged Shizuku management and Digital Wellbeing are housed under the Settings Manager overlay accessed via Home's Pulley Menu.

### F. Digital Wellbeing Engine & Settings Manager
- **Unprivileged Tier (Standard Android)**: Mindful breathing interstitial (`MindfulPause.kt`), 15-minute intentional session cooldown state machine (`isMindfulCooldownActive`), daily minute budgets, keyguard unlock counter (`UsageEvents.Event.KEYGUARD_HIDDEN`), and parent PIN protection.
- **Privileged Tier (Shizuku Required)**: Dynamic per-app display daltonizer monochrome switching before launch and automatic color restoration in `MainActivity.onResume()`, framework-level app suspension (`pm suspend`), and system installer lock.
- **`SettingsManagerPage.kt` (formerly `ShizukuShelfPage.kt`)**: Unified settings page with top Sailfish Pulley Menu and clearly demarcated sections: "Digital Wellbeing & Intention (Standard)" vs "Privileged System Engine (Shizuku Required)".

### G. Invariant Hardening & Variant Verification
- **Koin Multi-Init Guard**: `MonolithApp.kt` guards `startKoin` with `GlobalContext.getOrNull() == null`, preventing `KoinApplicationAlreadyStartedException` in Robolectric test suites.
- **Process Timeout**: `ShizukuShellService.kt` bounds execution with a 30s timeout and forced destroy.
- **Bounded LRU Cache**: `BoundedIconCache` replaces unbounded maps with 256-entry LRU retention.
- **Drawer Sorting Contract**: Drawer sections sort `#` after `Z` (`compareBy<Char> { if (it == '#') 1 else 0 }.thenBy { it }`).
- **Compose Test Manifest**: Declared `androidx.activity.ComponentActivity` in `AndroidManifest.xml` ensuring both `testDebugUnitTest` and `testReleaseUnitTest` execute cleanly across all variants.
