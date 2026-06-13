# Architecture

HonkaiGameLauncher is organized as a desktop-only Kotlin + Compose project with three modules:

```text
desktop-app -> desktop-ui -> desktop-core
```

Dependencies should only point from left to right. Keep platform lifecycle in `desktop-app`, UI state and interaction scheduling in `desktop-ui`, and testable business logic in `desktop-core`.

## Module Boundaries

- `desktop-core` owns domain models, parsing, path resolution, docs indexing, launcher logs, process abstractions, and services that do not depend on Compose, Voyager, FileKit, or desktop UI state.
- `desktop-ui` owns Compose screens, components, navigation descriptors, localization, resources, themes, and ScreenModel state mapping.
- `desktop-app` owns application startup, window configuration, tray integration, service startup/shutdown, and native distribution settings.

## UI And Core Boundary

- Put file scanning, TOML parsing, markdown index building, log parsing, and process launching behind `desktop-core` services or platform abstractions.
- Keep ScreenModel responsibilities to loading state, UI state composition, collecting core flows, and dispatching user intents.
- Do not update Compose state directly from IO coroutines. Switch back to the ScreenModel/UI coroutine context before publishing UI state.
- If a behavior can be tested without Compose, prefer implementing it in `desktop-core` and covering it with unit tests.

## Navigation

Register every page in `navigation.screenDescriptors`. A descriptor is the single place for route, title resource, icon, visibility, and order.

When adding a new page:

1. Add a `SharedScreen` provider.
2. Register the provider in `registerNavigation()`.
3. Add a `ScreenDescriptor` with a stable route such as `/new-page`.
4. Make the screen's `getUrl()` return `screenRoute(SharedScreen.NewPage)`.
5. Add localization keys to both `values-zh` and `values-en`.

## Core Services

When adding a new core service:

1. Define input and output models in `desktop-core`.
2. Keep filesystem, process, or environment access behind a small abstraction when practical.
3. Return structured status or result objects instead of UI strings.
4. Add unit tests under `desktop-core/src/test/kotlin`.
5. Let ScreenModel translate service results into localized UI state.

## Recommended Checks

Run the smallest relevant check while developing, then run the app-level check before merging:

```powershell
.\gradlew.bat :desktop-core:test
.\gradlew.bat :desktop-ui:compileKotlin
.\gradlew.bat :desktop-app:check
```
