# Neuron: Digital Wellbeing & Anti-Distraction Engine

## Synapses
- Upstream: [Central Synaptic Index](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md)
- Cross-links:
  - [Core Architecture & Data Layer](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-core-architecture.md)
  - [Shizuku IPC & Privileged Subsystem](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-shizuku-ipc.md)
  - [UI Design & Minimalist Philosophy](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-ui-minimalism.md)
  - [Traps, Pitfalls & Invariants](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-traps-and-contracts.md)

---

## 1. Architectural Mission & The Launcher Advantage

Standalone anti-distraction and digital habit utilities traditionally rely on continuous background polling and accessibility services. Monolith approaches digital wellbeing from the root origin of app execution:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ TRADITIONAL STANDALONE UTILITIES                                                │
│                                                                                 │
│  User taps App ───► Target App Launches ───► AccessibilityService detects event │
│                                             │ (Frame flicker, CPU drain,        │
│                                             │  OEM process killing)             │
│                                             ▼                                   │
│                                     Draws System Overlay Window                 │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│ MONOLITH LAUNCHER-NATIVE ENGINE                                                 │
│                                                                                 │
│  User taps App in Drawer ───► Intercept Synchronously at Launcher Boundary      │
│                               │                                                 │
│                               ├─► Check Quota / Daily Minute Budget             │
│                               ├─► Trigger Mindful Friction (Breathing / Prompt) │
│                               └─► Flip Daltonizer Grayscale via Shizuku         │
│                               ▼                                                 │
│                  Only then dispatches startActivity()                           │
│                  (Zero flicker, zero accessibility daemon, zero battery drain)  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

- **The Launcher Advantage**: As the primary Android Home (`CATEGORY_HOME`), Monolith controls the exact moment an app intent is created. Interception happens before Android's window manager transitions to the target package.
- **Zero-Daemon Efficiency**: Eliminates background battery drain and immunity to OEM process killers (OxygenOS, ColorOS, MIUI) that frequently terminate background services.

---

## 2. Core Subsystems & Mechanisms

### A. Display Daltonizer & System Monochrome (Anti-Stimulation)
Modern digital addiction relies on dopamine triggers fueled by saturated color palettes. Stripping color significantly reduces mindless consumption:
- **Android Framework Keys**:
  - `Settings.Secure.accessibility_display_daltonizer_enabled` (`1` = enabled, `0` = disabled)
  - `Settings.Secure.accessibility_display_daltonizer` (`0` = Monochrome / Grayscale mode)
- **Privilege Escalation Strategy**:
  1. *Direct Shizuku Command*: `settings put secure accessibility_display_daltonizer_enabled 1`
  2. *Permanent Self-Grant*: Dispatch `pm grant <package> android.permission.WRITE_SECURE_SETTINGS` via Shizuku once. Subsequent writes execute natively via `Settings.Secure.putInt(contentResolver, ...)` with zero IPC overhead across reboots.
- **Dynamic Per-App Trigger Contract**:
  - On launch of a marked distraction app: Enable grayscale immediately before `startActivity()`.
  - On launcher resume (`MainActivity.onResume()`): Revert grayscale to `0` automatically when returning to the home screen.

### B. Intentional Friction & Mindful Interstitials
Subconscious reflex taps account for a majority of addictive opens:
- **Breathing Ring / Interstitial Delay**: Enforces a non-skippable 5-to-15 second countdown (`MindfulPauseDialog.kt`) with an intention prompt ("What is your goal for opening this?").
- **Cooldown State Machine**: A thread-safe `ConcurrentHashMap<String, Long>` tracking cooldown expiration timestamps (`System.currentTimeMillis() + durationMs`). Once authenticated or waited through, subsequent rapid app-switches within the cooldown window open immediately without re-prompting.

### C. Framework-Level App Suspension (`pm suspend`)
Traditional blockers show an overlay window on top of running apps, leaving the background service and notifications running. Privileged suspension is far deeper:
- **Framework Command**: `pm suspend --user 0 <pkg1> <pkg2> ...`
- **Native OS Behavior**:
  - Package is frozen at the `PackageManagerService` framework layer.
  - Launcher icons are automatically dimmed or disabled by the OS.
  - Background processes are halted and push notifications are completely suppressed.
  - Attempting to launch suspended apps displays Android's native system dialog: *"This app is paused"*.
- **Restoration**: `pm unsuspend --user 0 <pkg1> ...`

### D. In-App UI Node Inspection & Element Filtering
Targeting specific addictive sub-features inside multi-purpose apps (e.g., short-form video feeds, explore tabs, algorithmic recommendation reels):
- **Accessibility Traversal**: Traverses active window node hierarchies (`AccessibilityNodeInfo`) searching for specific view IDs (e.g., `clips_viewer_view_pager`, `reel_recycler`).
- **Bounding Box Match**: Verifies candidate child nodes are within active screen viewport bounds (`node.cy >= page.top and node.cy <= page.bottom`).
- **Immediate Remediation**: Dispatches `GLOBAL_ACTION_BACK` or injects a focused blocking surface (`TYPE_APPLICATION_OVERLAY`) over the distracting section while allowing productive functions (e.g., direct messaging) to remain accessible.

### E. Physical Friction & Hardware NFC Token Locking
Software locks can often be bypassed by weak moments of willpower. Physical tokens introduce hard real-world friction:
- **Mechanism**: `NfcAdapter.enableReaderMode(activity, callback, READER_FLAGS, null)`
- **Reader Flags**: `FLAG_READER_NFC_A | FLAG_READER_NFC_B | FLAG_READER_NFC_F | FLAG_READER_NFC_V | FLAG_READER_NO_PLATFORM_SOUNDS`
- **Hardware Binding**: Focus mode cannot be terminated, quotas cannot be edited, and blocked apps cannot be opened unless the device detects a registered NFC card or sticker placed in a physically distant location (e.g., another room or desk).

### F. Screen Time Quotas & Rest Intervals
- **Usage Tracking**: `UsageStatsManager.queryUsageStats(INTERVAL_DAILY, midnightStartMs, nowMs)`.
- **Interval Schedules**: Configurable active windows (e.g., work hours 09:00–17:00, everyday intervals vs. day-specific schedules).
- **Physical Break Enforcement**: Tracks cumulative continuous screen time across distracting packages. When a threshold (e.g. 45 min) is breached, an enforced break screen mandates physical movement and rest before access is unlocked.

---

## 3. Critical Invariants & Pitfalls

1. **The "Forever Suspended" Trap**:
   - *Trap*: Calling `pm suspend` on user packages while Shizuku is active, and then the user terminates or unauthorizes Shizuku. The packages remain suspended permanently at the Android OS layer with no way for unprivileged apps to unsuspend them.
   - *Contract*: Always verify `Shizuku.pingBinder()` before applying suspension; provide an explicit recovery mechanism to unsuspend all packages on app launch or clean shutdown.
2. **System UI & Input Method Exclusion**:
   - *Trap*: Applying global grayscale or blocking logic without filtering system packages (`com.android.systemui`, active IME keyboard package via `Settings.Secure.getString(..., DEFAULT_INPUT_METHOD)`).
   - *Contract*: Hardcode exclusions for System UI, active keyboard IME, and emergency dialers in all blocking evaluators.
3. **Strict Zero-Branding Invariant**:
   - Monolith's UI, strings, comments, and documentation must NEVER reference external third-party utility names or AI boilerplate. All functionality is integrated as native, clean Monolith system features.
4. **Lifecycle Resource Cleanup**:
   - All BroadcastReceivers, schedule handler runnables (`handler.removeCallbacksAndMessages(null)`), and coroutine supervisor jobs must be cleanly cancelled upon component teardown to prevent activity leaks.
