# Building and releasing

Smart Autoclicker is a [Stonecutter](https://stonecutter.kikugie.dev/) multi-version Fabric mod: one source tree,
one Gradle project per Minecraft version (`versions/<mc>/gradle.properties`), and as few published jars as possible.

Two buildscripts share `gradle/target-common.gradle`: `build.gradle` for 26.x (unobfuscated, non-remapping Loom) and
`remap.gradle` for 1.21.x (obfuscated: compiled against Mojang's official mappings, remapped to intermediary by
`remapJar`). `settings.gradle` picks one per version. The code uses Mojang names everywhere.

You need JDK 21 and JDK 25 (Gradle finds installed JDKs through toolchains): 1.21.x builds and runs on 21, 26.x on 25.

```bash
./gradlew :26.3:build                    # one version
./gradlew build verifyGroupClasses       # every version, plus the release-group check
./gradlew :26.3:runClientGameTest        # in-game tests on one version (opens a game window, minimized and muted)
./gradlew runClientGameTest              # in-game tests on every version, one after another
./gradlew :26.3:runClientGameTest -PtestMods=/absolute/path/to/freecam.jar   # also runs the Freecam tests
```

## Version-specific code

Everything that differs between Minecraft versions goes in `util/Compat.java` where possible, and all drawing goes
through `ui/Gfx.java` (`GuiGraphicsExtractor` from 26.1, `GuiGraphics` before, with the 1.21.6 render rewrite inside),
so a new version usually means changes there only. Classes that implement a game interface whose methods changed
(the control panel screen, the toast, the mixins) branch in place. Branches are Stonecutter comments:

```java
//? if >=26.2 {
return mc.gui.screen();
//?} else
/*return mc.screen;*/
```

`src/` is checked out for the active version (`stonecutter.active` in `stonecutter.gradle`); the other versions build
from generated copies. Switch the active version with `./gradlew "Set active project to 26.2"` before editing code
for it, so your IDE checks the right branch.

## In-game tests

The tests themselves are in `src/gametest/.../SmartTests.java`, written against a small `TestClient` interface:

- **1.21.4 and later**: Fabric API's client game-test API runs them (`AutoClickerClientGameTest`).
- **1.21 – 1.21.3**: that API doesn't exist yet, so those versions set `fabric_client_gametest=false` and build the
  mod's own harness (`src/harness`) into the test mod instead. It launches a normal dev client, creates a flat world
  the way Fabric's test world builder does, runs the same tests and quits (exit code 1 on a failure). Differences: the
  game isn't paused between test steps, and mouse clicks go straight to the open screen.

Either way the task is `runClientGameTest`, the window is minimized and the game is muted, and screenshots land in
`versions/<mc>/build/run/clientGameTest/screenshots/`. On Linux CI the 1.21.x game runs under `xvfb-run`.

## Release groups: as few jars as possible

`release-groups.json` lists which Minecraft versions share one published jar. The first version of a group (the
*lead*) builds the jar, and its `fabric.mod.json` lists exactly the group's versions, so on any other version Fabric
Loader shows a clear "requires Minecraft …" message instead of the game crashing.

A version may join a group only when both hold:

1. **`verifyGroupClasses` passes**: its build compiles to classes byte-identical to the lead's. Then the lead's jar
   is, class for class, the jar that version would have had.
2. **`runClientGameTest` passes on it.**

Every member compiles at the lead's Java level and with a fixed javac (toolchain), so the comparison is exact. On
remapped targets Loom writes the mixins' intermediary target names into the classes themselves (no refmap), so
the comparison covers them too.
`build` also runs `verifyJarContents`, which fails on anything in a jar outside the allowlist.

## Adding a new Minecraft version

1. Add `versions/<mc>/gradle.properties` (Minecraft, Fabric API, Mod Menu, Loader and Java versions) and list the
   version in `settings.gradle`.
2. Build it. Fix any compile errors in `util/Compat.java` with a version branch.
3. Try it in the newest group first: add it to that group in `release-groups.json` and run
   `./gradlew build verifyGroupClasses` and `./gradlew :<mc>:runClientGameTest`.
   - Both pass: done. The existing jar now also covers the new version, with no code change for players.
   - Classes differ: give it its own group (a new jar).
4. The `new minecraft release` GitHub workflow fails daily (and emails the repository owner) while a released
   Minecraft version is missing from `release-groups.json`.

## Release

1. Set `mod_version` in `gradle.properties` and add a changelog entry to `CURSEFORGE.md`.
2. Clean build and stage in one invocation: `./gradlew clean build stageRelease`. This copies each group's jar into
   `release-staging/<mod_version>/` with `manifest.json` (hashes, Minecraft versions, jar contents, git commit). It
   refuses to overwrite a staged release.
3. Run `./gradlew runClientGameTest` and check the GitHub build is green for the staged commit.
4. Upload each staged jar to CurseForge, tagged with exactly the Minecraft versions listed for it in `manifest.json`.
