# Neuron: Shizuku IPC & Privileged Subsystem

## Synapses
- Upstream: [Central Index](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md)
- Cross-links:
  - [Package Installation Engine](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-package-installer.md)
  - [Traps & Contracts](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-traps-and-contracts.md)

---

## 1. Architectural Perimeter

The Shizuku privileged integration grants Monolith system-level execution capabilities (UID 2000 / shell or root) without running an interactive shell daemon or exposing arbitrary shell execution.

```
┌─────────────────────────────────┐
│     Monolith Application        │
│  (ShizukuManager / ViewModel)   │
└────────────────┬────────────────┘
                 │ Binder IPC via Shizuku UserService
                 ▼
┌─────────────────────────────────┐
│     IShizukuShellService.aidl   │
│       (Bound User Service)      │
└────────────────┬────────────────┘
                 │ Strict Argv Generation (ShizukuCommands)
                 ▼
┌─────────────────────────────────┐
│         ProcessBuilder          │
│    (pm, am, settings, cmd)      │
└─────────────────────────────────┘
```

## 2. AIDL Interface (`IShizukuShellService.aidl`)

- **Methods**:
  - `setSuspended(in String[] pkgs, boolean suspended)`
  - `setEnabled(in String[] pkgs, boolean enabled)`
  - `forceStop(in String pkg)`
  - `putSetting(in String namespace, in String key, in String value)`
  - `getSetting(in String namespace, in String key)`
  - `uninstall(in String pkg)`
  - `install(in ParcelFileDescriptor apk, long size, in String[] flags)`
  - `installMultiple(in ParcelFileDescriptor[] apks, in String[] names, in long[] sizes, in String[] flags)`
  - `listDisabledPackages()`, `trimCaches()`, `setDnd()`, `allowUsageStats()`

## 3. Security Boundary (`ShizukuCommands.kt`)

- **Zero Shell Expansion**: Never passes raw strings to `/system/bin/sh -c`. Commands are structured as exact argument lists (`List<String>`) executed directly by `ProcessBuilder`.
- **Input Validation**:
  - `PACKAGE`: Enforces regex `^[A-Za-z0-9_.]+$`. Rejects spaces, semicolons, backticks, quotes, and control characters.
  - `SETTING_KEY`: Enforces regex `^[A-Za-z0-9_.]+$`.
  - `NAMESPACES`: Restricts to `system`, `secure`, and `global`.
  - `INSTALL_FLAGS`: Validates each flag against known flags (`-r`, `-d`, `-g`, `-t`, `--bypass-low-target-sdk-block`, `-i <pkg>`).
  - `DND_MODES`: Restricts to `off`, `priority`, `alarms`, `none`.

## 4. Connection State Machine (`ShizukuState`)

Managed in `ShizukuManager.kt`:
- **States**:
  - `Unavailable`: Shizuku is not installed or service is not running.
  - `NoPermission`: Shizuku is running but permission has not been granted by user.
  - `Connecting`: Binding to the Shizuku user service.
  - `Ready`: User service is bound, active, and verified with ping.
  - `Dead`: Service was killed or disconnected unexpectedly.
- **Auto-Recovery**: Listens to binder death notifications and triggers reconnection logic when the service revives.
