# Mouse Configurator v5 — Optimized

This is a refactored version of `dxgod7470-crypto/mouse-epic`.

## What changed

- Split the 505-line monolithic activity/service into small components.
- Removed the per-input coroutine creation.
- Uses one single-thread output executor for serialized shell writes.
- Added `MouseConfig` model.
- Added input processing and event-rate statistics.
- Added Shizuku manager and isolated shell implementation.
- Added optimization/thermal/battery/resource diagnostics.
- Added an Optimization tab.
- Added Android 14+ special-use foreground-service subtype metadata.
- Updated version to 5.0.

## Important limitation

The activity can only receive mouse motion events while Android dispatches those events to the activity. Shizuku does not automatically make an ordinary app a global raw-input hook. The output path is isolated so it can later be replaced with a supported UserService-based implementation.

The optimization features reduce this app's own CPU/RAM/background work. They do not make the physical network faster and do not force CPU/GPU clocks.

## Build

Use the same Gradle wrapper from the original repository (Gradle 8.10.2), then:

`./gradlew assembleDebug`

Shizuku API 13.1.5 is retained for compatibility with the original project.
