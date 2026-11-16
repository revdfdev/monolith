# Monolith Agent Brain (Synaptic Memory Index)

Interconnected neural memory network ("neurons") capturing verified architecture facts, operational contracts, subsystems, and traps for the Monolith repository.

```
                              ┌───────────────────────────────────┐
                              │       [INDEX.md] Central Hub       │
                              └─────────────────┬─────────────────┘
                                                │
         ┌──────────────────────────────┬───────┴───────┬──────────────────────────────┐
         ▼                              ▼               ▼                              ▼
┌──────────────────┐           ┌──────────────────┐    ┌──────────────────┐   ┌──────────────────┐
│   Core System    │◄─────────►│ Package Install  │◄──►│   Shizuku IPC    │   │  UI & Philosophy │
│ [core-arch.md]   │           │   [installer.md] │    │ [shizuku-ipc.md] │   │ [ui-minimal.md]  │
└────────┬─────────┘           └────────┬─────────┘    └────────┬─────────┘   └────────┬─────────┘
         │                              │                       │                      │
         ├──────────────────────────────┴───────┬───────────────┴──────────────────────┤
         ▼                                      ▼                                      ▼
┌──────────────────┐                   ┌──────────────────┐                   ┌──────────────────┐
│Digital Wellbeing │◄─────────────────►│Traps & Contracts │◄─────────────────►│Modifications/Δ  │
│ [wellbeing.md]   │                   │[traps-contracts] │                   │ [delta.md]       │
└──────────────────┘                   └──────────────────┘                   └──────────────────┘
```

## Synaptic Neurons (Knowledge Nodes)

1. **[Neuron: Core Architecture & Data Layer](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-core-architecture.md)**
   - Entry points: `MainActivity`, `MonolithApp`
   - DI Graph: Koin modules (`appModule`), singletons vs factories
   - Storage & State: `AppRepository`, `LauncherPrefs` (DataStore/Preferences), `MonolithSettings`
   - State flows, coroutine scopes, and reactive UI boundaries

2. **[Neuron: Package Installation Engine](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-package-installer.md)**
   - Functional parity with advanced privileged installers (without copying code or branding)
   - Archive formats: `.apk`, `.xapk`, `.apks`, `.apkm`, multi-APK zip
   - Split extraction, base APK detection, ABI compatibility filtering
   - Pre-flight signature verification & downgrade detection
   - Session installation via Shizuku: `pm install-create` -> `pm install-write` -> `pm install-commit`
   - Foreground service (`PackageInstallService`), notification channels, and `PackageInstallTracker` state sync
   - Fallback: System package installer (`FileProvider`)

3. **[Neuron: Shizuku IPC & Privileged Subsystem](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-shizuku-ipc.md)**
   - AIDL contract: `IShizukuShellService.aidl`, `OpResult.aidl`
   - Security perimeter: `ShizukuCommands.kt` (strict argv generation, zero shell evaluation, injection prevention)
   - Service implementation: `ShizukuShellService.kt`
   - Connection lifecycle: `ShizukuState` state machine, pinging, permission management

4. **[Neuron: UI Design & Minimalist Philosophy](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-ui-minimalism.md)**
   - Design inspirations: Before Launcher, Niagara, Escape, Minima
   - Visual tokens: Pure Black (`#000000`), Pure White (`#FFFFFF`), Hairline borders, Monospace chips
   - Translucent overlay: `Theme.Monolith.TranslucentDialog` for package inspection without launching home
   - Strict Anti-Slop Directive: NO third-party app branding or AI commentary anywhere in code, strings, or docs

5. **[Neuron: Digital Wellbeing & Anti-Distraction Engine](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-digital-wellbeing.md)**
   - Launcher boundary vs standalone accessibility service advantages
   - Display daltonizer & monochrome mode (`Settings.Secure` daltonizer toggle via Shizuku)
   - Intentional friction, breathing interstitials, cooldown state machines
   - Framework-level app suspension (`pm suspend`) and prevention of the "forever suspended" trap
   - In-app view node inspection & short-form element filtering
   - Physical friction with NFC tag hardware binding
   - Screen time quotas (`UsageStatsManager`) and rest break enforcement

6. **[Neuron: Traps, Pitfalls & Invariants](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-traps-and-contracts.md)**
   - Coroutine dispatcher injection in ViewModels for deterministic unit tests
   - Singleton state leaks: `PackageInstallTracker.reset()` in test setup/teardown
   - Android 12+ Foreground Service restrictions (`FOREGROUND_SERVICE_DATA_SYNC`)
   - Split APK file descriptor streaming and cleanup of staged extraction folders
   - Signature mismatch handling: 1-tap "Uninstall & Reinstall" contract

7. **[Neuron: Repository Modifications & Architectural Delta](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-modifications-and-delta.md)**
   - Detailed audit of all modified and newly created files in Monolith
   - Third-party branding purge across wellbeing, widgets, and launcher
   - Package installer engine additions and Shizuku AIDL session implementation
