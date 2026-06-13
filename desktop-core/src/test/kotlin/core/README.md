# Test fixtures

`withTempGameFixture` builds an isolated game-like directory for core tests:

- `root`: fake game install directory.
- `plugins`: `root/honkai_rts/GamePlugins`.
- `docs`: `root/honkai_rts/docs`.

Tests should create only the files they need inside this temporary tree. This keeps path, plugin, and docs indexing tests independent from any real Honkai RTS installation.
