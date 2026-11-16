# Neuron: Package Installation Engine

## Synapses
- Upstream: [Central Index](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md)
- Cross-links:
  - [Core Architecture](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-core-architecture.md)
  - [Shizuku IPC](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-shizuku-ipc.md)
  - [UI & Philosophy](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-ui-minimalism.md)
  - [Traps & Contracts](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-traps-and-contracts.md)

---

## 1. Architectural Mission & Invariants

Provide 1:1 functional engine parity with modern advanced package installers:
- Support for all modern container/archive formats (standalone APK, APKS, XAPK, APKM, multi-APK zip).
- Atomic session installation (`PackageInstaller.Session` / `pm install-create/write/commit/abandon`).
- ABI/architecture detection and compatibility filtering.
- Pre-flight signature verification and automatic downgrade detection.
- 1-tap "Uninstall & Reinstall" conflict resolution when signatures differ.
- Full background execution via `ForegroundService` with ongoing notification progress.
- Translucent floating dialog overlaying external callers (files, browser, chat apps) without disrupting home.
- System installer fallback when unprivileged.
- **Strict Invariant**: Zero AI slop, zero third-party app branding in codebase, UI strings, or documentation.

---

## 2. Container Analysis Strategies

Located in `app/src/main/java/com/rehan/monolith/ui/install/ApkTools.kt`:

### A. Single APK Strategy
- Standalone `.apk` file.
- Inspected via `pm.getPackageArchiveInfo(path, PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES)`.
- Base APK is the single file; target ABI extracted from `lib/` directory inside the APK.

### B. APKS Bundle Strategy
- Archive created by Google `bundletool` or app stores.
- Base APK identification: searches container entries for `base.apk`, `base-master.apk`, or `standalone.apk`.
- Config splits: splits for screen densities (`config.xxhdpi`), languages (`config.en`), and ABIs (`config.arm64_v8a`).

### C. XAPK Container Strategy
- Container format containing base APK, splits, icon, and metadata manifest.
- Primary parsing path: decodes `manifest.json` from the archive root to identify:
  - `package_name`, `version_name`, `version_code`
  - `splits`: list of `{ "name": "...", "split_name": "..." }`
- Fallback path: If `manifest.json` is missing or corrupt, scans ZIP entries directly for `base.apk` or splits.
- Icon extraction: reads `icon.png` directly from the container if present.

### D. APKM Container Strategy
- Archive containing multi-APK splits and container metadata.
- Traversed as a ZIP to locate the root base APK and all auxiliary architecture/language splits.

### E. Multi-APK ZIP Strategy
- Generic ZIP archive containing multiple `.apk` files.
- Inspects each APK entry to determine the primary application base APK (package declaring main activity / application name) and associates auxiliary config splits.

---

## 3. ABI Compatibility & Split Filtering

To prevent `INSTALL_FAILED_NO_MATCHING_ABIS` and avoid staging conflicting native libraries:
- Device ABIs are retrieved from `Build.SUPPORTED_ABIS` in order of preference (e.g., `["arm64-v8a", "armeabi-v7a", "armeabi"]`).
- For split containers:
  1. Detect ABI splits by checking split names (`split_arm64_v8a.apk`, `config.arm64_v8a.apk`, `lib/arm64-v8a/...`).
  2. If an APK split is dedicated to an ABI not present in `Build.SUPPORTED_ABIS` (e.g. `x86_64` on an ARM device), it is filtered out of the staging directory.
  3. The highest priority matching ABI is retained for the install session.

---

## 4. Pre-Flight Verification & Conflict Resolution

Performed prior to presenting the install prompt to avoid unexpected session failures:

### A. Signature Matching (`signaturesMatch`)
- Queries the device `PackageManager` for the installed package:
  - API 28+: `pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))`
  - API < 28: `pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)`
- Compares signing certificates with the incoming APK's signing certificates.
- If signatures mismatch:
  - Flag `PendingApkInfo.signaturesMatch = false`.
  - The UI presents a warning and replaces the default "Install" button with **"Uninstall & Reinstall"**.
  - Tapping **"Uninstall & Reinstall"** triggers a silent uninstallation of the old package via privileged Shizuku before streaming the new package, overcoming `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

### B. Downgrade & Version Classification
- Categorizes the install into:
  - `NEW`: Package not currently installed.
  - `UPGRADE`: Incoming `versionCode` > installed `versionCode`.
  - `SAME`: Incoming `versionCode` == installed `versionCode`.
  - `DOWNGRADE`: Incoming `versionCode` < installed `versionCode`.
- Automatically toggles the `-d` (allow downgrade) installation flag when `DOWNGRADE` is detected.

---

## 5. Atomic Session Installation Protocol

Executed through Shizuku privileged shell service (`ShizukuShellService.kt` / `ShizukuCommands.kt`):

```
┌───────────────────────┐
│  pm install-create    │ ── Allocates session ID (e.g. [123456])
└──────────┬────────────┘
           │
           ▼
┌───────────────────────┐
│  pm install-write     │ ── Streams each APK split via STDIN
└──────────┬────────────┘
           │
           ▼
┌───────────────────────┐
│  pm install-commit    │ ── Atomically commits all splits into PackageInstaller
└──────────┬────────────┘
           │ (On any write/commit failure)
           ▼
┌───────────────────────┐
│  pm install-abandon   │ ── Immediately discards session to prevent resource leaks
└───────────────────────┘
```

- **Supported Install Flags**:
  - `-r`: Replace existing application.
  - `-d`: Allow version downgrade.
  - `-g`: Grant all runtime permissions on installation.
  - `-t`: Allow test packages.
  - `--bypass-low-target-sdk-block`: Allows installing packages targeting legacy SDKs (Android 14+).
  - `-i <installer-pkg>`: Sets installer package name metadata (e.g. `com.android.vending`).

---

## 6. Background Execution & User Feedback

- **`PackageInstallService.kt`**:
  - Android `ForegroundService` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
  - Displays ongoing notification with live animated progress (`setProgress(100, percent, false)`), active stage text (*"Streaming splits (45%)..."*, *"Verifying package..."*), and `CATEGORY_PROGRESS`.
  - Automatically cleans up staged temporary splits (`bundle_<timestamp>/`) upon completion or error.
  - On success: posts completion notification with the real app icon, intent to launch, and an immediate 1-tap "Open" action button.
  - On error: maps raw package manager error codes to concise user-facing explanations.
- **`PackageInstallTracker`**:
  - Observable singleton state flow consumed by both foreground activity and launcher UI.
- **System Installer Fallback**:
  - When Shizuku is not running or permission is denied, packages can be handed off to Android's built-in package installer via `FileProvider`.
