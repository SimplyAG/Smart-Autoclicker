# Building and releasing

Smart Autoclicker is a [Stonecutter](https://stonecutter.kikugie.dev/) multi-version Fabric mod: one source tree,
one Gradle project per Minecraft version (`versions/<mc>/gradle.properties`), and as few published jars as possible.

You need JDK 25 (Gradle finds installed JDKs through toolchains).

```bash
./gradlew :26.3:build                    # one version
./gradlew build verifyGroupClasses       # every version, plus the release-group check
./gradlew :26.3:runClientGameTest        # in-game tests on one version (opens a game window, minimized and muted)
./gradlew runClientGameTest              # in-game tests on every version, one after another
./gradlew :26.3:runClientGameTest -PtestMods=/absolute/path/to/freecam.jar   # also runs the Freecam tests
```

## Version-specific code

Everything that differs between Minecraft versions goes in `util/Compat.java` where possible, so a new version
usually means changes there only. Branches are Stonecutter comments:

```java
//? if >=26.2 {
return mc.gui.screen();
//?} else
/*return mc.screen;*/
```

`src/` is checked out for the active version (`stonecutter.active` in `stonecutter.gradle`); the other versions build
from generated copies. Switch the active version with `./gradlew "Set active project to 26.2"` before editing code
for it, so your IDE checks the right branch.

## Release groups: as few jars as possible

`release-groups.json` lists which Minecraft versions share one published jar. The first version of a group (the
*lead*) builds the jar, and its `fabric.mod.json` lists exactly the group's versions, so on any other version Fabric
Loader shows a clear "requires Minecraft …" message instead of the game crashing.

A version may join a group only when both hold:

1. **`verifyGroupClasses` passes**: its build compiles to classes byte-identical to the lead's. Then the lead's jar
   is, class for class, the jar that version would have had.
2. **`runClientGameTest` passes on it.**

Every member compiles at the lead's Java level and with a fixed javac (toolchain), so the comparison is exact.
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
