# Core fixes, architecture split, background enforcement, Shizuku rework
Problem: New installs never reach the drawer; limits, Mindful Pause and Kids Space only work when an app is opened from the drawer; installs can't handle split APKs; Shizuku disables apps, ignores errors and runs injectable shell strings; widgets can't be configured; the whole app lives in one 4,682-line MainActivity.kt.
Wanted: Each feature in its own module, built the way the reference apps do it: Olauncher's app list, Curbox's accessibility enforcement plus watchdog and boot receiver, InstallerX's session installs. Shizuku stays an optional booster.

Touches: MainActivity.kt (all UI and state), LauncherPrefs.kt (persisted keys, which must stay readable), ShizukuManager/ShellService/AIDL, AndroidManifest.xml, app/build.gradle.kts. LockScreenActivity.kt and MonolithNotificationListener.kt are not in the manifest and nothing references them.
Licence (decided): copy patterns, not code, from all four GPLv3 references.
Architecture follows Kvaesitso:
- Gradle modules `core/` (base, preferences, permissions, shizuku), `data/` (applications, widgets, usage, rules), `services/` (enforcement, installer, badges) and `app/ui`
- Koin for dependency injection
- A single `@Serializable` settings data class in DataStore, with numbered migrations from the old SharedPreferences
- Room for widgets, groups and the restriction ledger
- A sealed `Widget` class whose config is JSON per row
- A `LauncherBottomSheetManager` for sheets
Lockscreen (decided): Android 12+ blocks lockscreen customisation, so drop it.
Out of scope (later plans): Curbox reel blocking, keyword/browser blocking, the UI hider, NFC/QR unlock, anti-uninstall/device admin, the companion API; the P0 security checklist (escape routes, PIN throttling), P1 Mindful Pause stats, P2 presets, P3 per-device verification, P4 mode engine.

## Phase 1: Build baseline
- [x] Gradle wrapper 8.14 (AGP 8.7.3), JDK 21 pinned via gradle-daemon-jvm.properties, JUnit + Robolectric — proof: `env -u JAVA_HOME ./gradlew testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL

- [x] Manifest (now): allowBackup=false, enableOnBackInvokedCallback, stateHidden|adjustResize, ACCESS_HIDDEN_PROFILES, EXPAND_STATUS_BAR — proof: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL; `aapt2 dump xmltree` shows all five. Remaining items below move with their feature: QUERY_ALL_PACKAGES removal → Phase 3; InstallActivity + REQUEST_INSTALL_PACKAGES → Phase 6; PinItemActivity → Phase 10; POST_NOTIFICATIONS/BOOT/NOTIFICATION_POLICY → Phase 5.
- [ ] (deferred items) Manifest corrections, based on the patterns in the four reference manifests:
  - Remove `QUERY_ALL_PACKAGES`; LauncherApps doesn't need it, and neither Olauncher nor Kvaesitso declares it.
  - Move the APK VIEW/INSTALL_PACKAGE filter from the HOME activity to a separate `InstallActivity`, and add `REQUEST_INSTALL_PACKAGES`.
  - Add `PinItemActivity` for CONFIRM_PIN_SHORTCUT/APPWIDGET.
  - Add `ACCESS_HIDDEN_PROFILES` (private space) and `EXPAND_STATUS_BAR`.
  - Add `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and `ACCESS_NOTIFICATION_POLICY` (DND without shell). Add FOREGROUND_SERVICE plus a type when the install service lands.
  - Set `enableOnBackInvokedCallback`, and use `adjustResize|stateHidden` (Kvaesitso).
  - Exclude prefs from backup with `dataExtractionRules`.

  Proof: `aapt2 dump xmltree` / `apkanalyzer manifest permissions` diff; on device, opening an APK from the Files app opens InstallActivity, not home
- [x] Parent PIN: no default, PBKDF2 salted hash, legacy migration, setup required before Kids Space — proof: `./gradlew testDebugUnitTest` → ParentPinTest 6/6 pass (seen failing first); on-device flow not verified

## Phase 2: Split the single file (no behaviour change)
- [x] Feature packages (data/shizuku/ui/*), MonolithApp + Koin (app-scoped Shizuku & prefs), DataStore `MonolithSettings` with legacy SharedPreferences migration — proof: `./gradlew testDebugUnitTest assembleDebug` → 11/11 pass; on both phones legacy prefs file removed and values present in `files/datastore/monolith_settings.json`, launch ok, 0 crashes. Gradle submodules deferred until ViewModels exist.
- [ ] Remaining Phase 2 for this item: ViewModels (below). Move each feature into its own package: `data/` (prefs, apps), `shizuku/`, `ui/home`, `ui/drawer`, `ui/shelf`, `ui/kids`, `ui/widgets`, `ui/settings`, `ui/install`, `ui/pause`. Add a ViewModel per screen (state comes out of the ~40 `remember` variables). Proof: assembleDebug; a LauncherPrefs round-trip unit test shows the old keys still read the same; manual smoke test on device via adb (home, drawer and shelf all open)
- [x] Removed hardcoded seed packages (now first 4 user apps), invented labels/product comments, LockScreenActivity, lockscreen message + pitch-black wallpaper (kept lockscreen notification mode) — proof: `./gradlew testDebugUnitTest assembleDebug` → 15/15; grep finds no refs; both phones launch, 0 crashes; OnePlus System Tweaks shows no lockscreen message/wallpaper. Notification listener registration moved to the badges/media work (unused until then).

## Phase 3: Drawer gets new installs (bug #1, following Olauncher)
- [x] AppRepository (Koin singleton, StateFlow) reloads on LauncherApps.Callback (debounced 300 ms), onResume and Shizuku bind — proof: AppRepositoryTest 4/4 (seen failing first); Oppo CPH2359: drawer search "meizo" empty → `adb install` MeizoPush with drawer open → appears; `adb uninstall` → gone. LauncherApps-per-profile loader moved to the Final phase (user request).
- [ ] An `AppRepository` exposed as a `StateFlow`, built from `LauncherApps.getActivityList` for each `UserManager` profile (work profile included). A `LauncherApps.Callback` (added, removed, changed, available, unavailable) triggers a debounced reload, and so do `onResume` and Shizuku connecting. Proof: a unit test (fake LauncherApps) shows a reload when a package is added or removed; `adb install x.apk` → the app appears in the drawer without a restart; `adb uninstall` → it disappears

## Phase 4: Shizuku service rework
- [ ] One `ShizukuManager` that lives for the whole app, with states Unavailable, NoPermission and Ready, re-binding when the binder comes back. Services and workers can use it, not just the activity. Proof: a unit test of the state machine; killing the Shizuku app on device changes the UI state, and restarting it re-binds
- [ ] A typed AIDL in place of `exec(String)`: `suspend(pkgs, bool)`, `setEnabled`, `forceStop`, `putSetting(ns, key, value)`, `getSetting`, `installSession(pfds[])`, `uninstall`. Arguments go to `ProcessBuilder` as an argv list (no `sh -c`), and package names must match `^[A-Za-z0-9_.]+$`. Every call returns `OpResult(ok, code, message)`. Proof: unit tests reject `a;rm -rf` and `$(id)` as package names and pass setting values containing quotes or backticks through as literal text; on device `settings get` returns the value unchanged
- [ ] Remove the `2>/dev/null; true` patterns and show every failure in the UI (Focus, Kids and freeze no longer toast success unconditionally). Proof: unit test where a fake service fails → the UI state shows the error
- [ ] Focus, limits and Kids Space use `pm suspend` (as Curbox does) instead of `disable-user`. A protected list can never be suspended or disabled: Monolith, the default dialer, IME, Settings, Shizuku, the installer and SystemUI. Proof: a unit test shows protected packages are filtered out; on device a suspended app stays in the drawer, greyed out
- [ ] A persisted "applied restrictions" ledger. A "Restore everything" action, plus boot and watchdog jobs, unsuspend or re-enable whatever the ledger lists; if Shizuku is missing, show a persistent notification. Exiting Kids Space no longer reports success without Shizuku. Proof: unit test of the ledger; on device: focus on → kill Shizuku → reboot → start Shizuku → apps restored
- [ ] Rework grayscale through the accessibility service's colour filter, falling back to the secure daltonizer setting. Drop the OEM-only keys unless the device has them, and add a "what this does" line to Trim caches, Stay awake and the animation controls. Proof: on device, read the settings back with `adb shell settings get`

## Phase 5: Background enforcement (bug #2, following Curbox)
- [ ] An `EnforcementAccessibilityService` that watches `TYPE_WINDOW_STATE_CHANGED` for the foreground package and asks the rule engine (`domain/RuleEngine`: limits, pause, Kids allowlist, focus) for a decision. A block launches the `PauseActivity` or `LimitActivity` overlay, or goes `GLOBAL_ACTION_HOME`. Rules now hold however the app was opened. Proof: unit tests for the rule engine (positive and negative cases for each rule); on device, opening a paused app from a notification or Recents shows the pause
- [ ] Move Mindful Pause into its own activity, rename it away from Curbox, keep Cancel enabled throughout, and show Continue once the timer ends. Proof: Compose UI test
- [ ] `BootReceiver` (BOOT_COMPLETED, MY_PACKAGE_REPLACED), a WorkManager `WatchdogWorker` (15 min, checks the service and Shizuku, notifies when either is down), and a daily-reset worker for limits. Proof: a WorkManager TestDriver unit test; `adb reboot` → `adb shell dumpsys jobscheduler | grep monolith` lists the job
- [ ] Each restriction row shows "Enforced" or "Launcher-only" along with the permission it needs. Proof: Compose UI test for both states

## Phase 6: Installer (bug #4, following InstallerX)
- [ ] Parse APK, APKS, XAPK and APKM files (a zip of splits) and show the details: label, version, signature change and splits. Proof: unit tests with fixture archives
- [ ] An `InstallService` foreground service. Without Shizuku it uses the public `PackageInstaller` session API and the system confirmation; with Shizuku it uses `pm install-create`, `install-write` (fed one file descriptor per split) and `install-commit` through the new AIDL. Status codes become readable errors. Proof: on device, install an APKS split set in both modes, then `adb shell pm path <pkg>` lists all the splits
- [ ] Export installed apps with their splits, as an APKS. Proof: export, reinstall, then `pm path` matches

## Phase 7: Curbox-style controls (patterns from Curbox's data/models and blockers)
All of these run through the Phase 5 RuleEngine and accessibility service, and are stored with DataStore and kotlinx.serialization.
- [ ] **App groups** instead of the separate per-app sets (focus, pause, limits). A group holds a name, packages, active flag, auto-add new apps, and a "disabled until" time. Existing pinned, focus, mindful and limit sets migrate into groups. Proof: migration unit test (old prefs → groups, nothing lost)
- [ ] **Rules per group:** a daily limit per weekday (7 values), time-of-day block windows, and a limit per session. Proof: RuleEngine unit tests at the boundaries (23:59 to 00:00 reset, at the limit, inside or outside a window, weekday switch)
- [ ] **Warning screen config per group:** custom messages, Proceed delay (3/5/10/15 s), a cap on Proceeds per time window, a cooldown after Proceed, "hide the warning and just go home", and Cancel always available. This replaces the Mindful Pause dialog. Proof: UI test of the timer → Continue; unit test that the Proceed cap blocks the 4th proceed out of 3
- [ ] **Focus sessions** (ManualFocusGroup): block the selected apps or everything except the allowed ones, a duration, a Pomodoro option, optional DND, and an "exitable" flag. Proof: RuleEngine tests for both modes; on device, DND is turned on and off
- [ ] **Anti-stimulants:** grayscale on a schedule per group, Sip & Stretch (a break after N minutes), Mindful messages (an overlay inside chosen apps), and an Urge Surf breathing screen offered after 3 unlock attempts. Proof: unit tests for each trigger; on device, one check per feature
- [ ] **Strict settings:** a settings-change delay (changes that loosen rules wait N hours, through a WorkManager job) and protection of the service and Shizuku (self-heal via Shizuku when available). Proof: unit test that loosening is queued and tightening applies at once; WorkManager TestDriver test
- [ ] **Usage view:** a local per-app screen-time list for today and the week, plus counts of pauses cancelled versus continued. Proof: unit test of the aggregation with fake UsageStats events

## Phase 8: Settings rebuild
- [ ] Settings becomes its own Compose navigation destination, replacing the shelf page and loose dialogs. Top level: **Appearance** (theme, accent, wallpaper, fonts, icon/label style), **Home & Widgets**, **Drawer** (layout, transparency, sort, hidden apps, search behaviour), **Wellbeing** (groups, focus, anti-stimulants, usage), **Kids Space**, **Installer**, **Advanced** (Shizuku status, system tweaks with an explanation and confirmation each, restore everything), **About**. One shared row style. Every restriction row shows Enforced or Launcher-only. Proof: Compose UI tests reaching every section through navigation; on-device screenshots, light and AMOLED

## Phase 9: Customisation and widgets (bugs #6, #7)
- [ ] A widget model in the Kvaesitso pattern: a sealed `Widget` stored in a Room table with its position and JSON config, which can be added, removed, reordered and resized. The clock becomes a pinned top widget with a choice of clock faces (digital, analog, segment, binary), compact or full size, 12/24h and seconds. Underneath it sit optional, ordered **parts**: date, battery, alarm, media and stats (RAM/storage). The system stats widget lets you pick battery, RAM and/or storage. Old HomeConfig values migrate. Proof: migration unit test; Compose UI test of add, remove, reorder and resize
- [ ] Customiser with a live mini preview of home. A strict minimal preset (clock plus up to 4 apps). AMOLED black with amber stays the default. The everyday home screen has no advanced controls. Proof: UI test that the preview updates on change; screenshot

## Phase 10: Bottom sheets
- [ ] A `LauncherBottomSheetManager` (Kvaesitso pattern: a single state holder decides which sheet is open, so no more `showX` booleans), with one `MonolithSheet` component used by every sheet: a drag handle, a header (icon, label, package, version, size), grouped action rows, and correct insets, back and IME handling. Proof: UI test that back closes the sheet
- [ ] **App sheet** regrouped:
  - Quick row: Pin, Hide, App info, Uninstall
  - Wellbeing: add to group, daily limit, warning screen
  - Restrictions: suspend/resume and force stop, each with a one-line explanation and its enforced state
  - Kids: allow/remove
  - Installer: export APK/APKS
  - Shortcuts: app shortcuts from `LauncherApps` (as Olauncher does)
  - Rename the label

  Actions that need Shizuku are greyed out with the reason when it isn't available. Proof: UI tests with Shizuku Ready vs Unavailable; on device, a long-press shows the shortcuts
- [ ] Move the customiser, widget manager, widget picker and app pickers to MonolithSheet; the app pickers get search plus multi-select. Proof: UI test of a search-filtered pick

## Final phase (deferred by user)
- [ ] Loader uses `LauncherApps.getActivityList` per `UserManager` profile (work profile, private space); then remove `QUERY_ALL_PACKAGES`. Proof: unit test with fake profiles; drawer on both phones with the permission removed
