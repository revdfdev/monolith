# Neuron: UI Design & Minimalist Philosophy

## Synapses
- Upstream: [Central Index](file:///home/mkodekar/code/monolith/docs/agent-memory/INDEX.md)
- Cross-links:
  - [Core Architecture](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-core-architecture.md)
  - [Package Installation Engine](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-package-installer.md)
  - [Traps & Contracts](file:///home/mkodekar/code/monolith/docs/agent-memory/neuron-traps-and-contracts.md)

---

## 1. Minimalist Philosophy

Monolith is a radically distraction-free, text-first Android launcher inspired by:
- **Before Launcher**: Extreme visual calm, plain typography, intentional friction for non-essential apps.
- **Niagara Launcher**: Ergonomic vertical flow, single-handed alphabet scrolling, zero visual clutter.
- **Sailfish OS**: Gesture-driven Pulley Menu (pull-down anywhere with threshold-based haptic ticks) replacing static clutter buttons.
- **Escape Launcher & Minima**: Compact, responsive, no decorative gradients, no unnecessary drop shadows.

## 2. Sailfish Pulley Menu & Niagara A-Z Wave (`PulleyMenu.kt`, `HomeLauncherPage.kt`)

- **Sailfish-Style Pulley Menu**:
  - Pull-down anywhere on Home (or downward overscroll at top of grid) smoothly translates screen content downward.
  - Reveals stacked menu items above content (`CUSTOMIZE HOME`, `FOCUS MODE`, `SHIZUKU SHELF`, `WIDGET MANAGER`).
  - Haptic ticks fire on threshold boundaries (`HapticFeedbackType.TextHandleMove`).
  - Active item is prominently highlighted; releasing touch executes the highlighted item (`HapticFeedbackType.LongPress`).
  - Removing static top button cards eliminates visual clutter while preserving immediate one-gesture access.
- **Niagara A-Z Wave Scrubber**:
  - Right-edge vertical alphabet wave directly on Home (`LetterScrubber`).
  - Dynamic fisheye letter expansion and magnified letter bubble.
  - Tapping or sliding to a letter jumps into the App Drawer initialized at that exact section (`onOpenDrawerAt(letter)`).

## 2. Design System Tokens (`CommonUi.kt`)

- **Palette**:
  - `PureBlack`: `#000000` (OLED-friendly primary background).
  - `SurfaceBlack`: `#0A0A0A` (Card and bottom sheet background).
  - `PureWhite`: `#FFFFFF` (Primary text, active action buttons).
  - `TextDim`: `Color(0xFF888888)` (Secondary metadata, package IDs, timestamps).
  - `BorderHairline`: `Color(0x22FFFFFF)` (Hairline borders for cards and chips).
- **Typography**:
  - Primary text: Sans-serif, bold headers, regular body.
  - Metadata & badges: `FontFamily.Monospace`, uppercase tracking (`letterSpacing = 1.2.sp`), 10-11sp.

## 3. Overlay Architecture (`Theme.Monolith.TranslucentDialog`)

- When an APK or bundle is opened from external apps (Files, Telegram, Chrome), `PackageInstallActivity` renders as a floating bottom sheet / modal card over the existing app.
- Never launches the launcher home screen or takes over system navigation.
- Smooth dismiss returns the user directly to the caller app.

## 4. Anti-Slop Directive (CRITICAL)

- **Absolute prohibition of AI slop**:
  - No synthetic commentary, conversational padding, or self-congratulatory text.
  - No third-party app names (e.g., InstallerX, Curbox, Split APKs Installer) in code, UI strings, comments, commit messages, or docs.
  - Clean, concise, professional system-level terminology only ("Package Installer", "Bundle Splits", "Session Installer").
