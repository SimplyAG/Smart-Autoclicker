# Smart Autoclicker

Smart AFK automation for Minecraft **26.1 – 26.3** (Fabric, client-side). Everything runs through Minecraft's own input handling, so it respects attack cooldowns, reach and item use exactly like real clicks, and it keeps working while you're tabbed out.

## Features

| Module | What it does |
|---|---|
| **Auto Attack** | Hits the mob under your crosshair. *Perfect* timing swings only at full attack charge (best damage per second); *Fixed Delay* swings every N ms. Filters for hostile mobs, passive mobs, players and armor stands, and never hits pets, name-tagged mobs, villagers or golems. |
| **Auto Eat** | Eats the best *safe* food when hungry or hurt, from your offhand or hotbar, then switches back to your weapon. Refills the hotbar from your inventory. Never eats rotten flesh, spider eyes, raw chicken, pufferfish or chorus fruit; saves golden apples unless you allow them. Never right-clicks the block or mob you're looking at. |
| **Hold Left Click** | Mines continuously, even while tabbed out. Stops before your tool breaks. |
| **Hold Right Click** | Holds right click (shield, eating, placing, charging), or clicks it repeatedly in Spam mode. |
| **Auto Walk** | Holds forward/back/left/right, with optional sprint, crouch and jump. |
| **Mob Sweep** | Smoothly turns left and right so Auto Attack covers a wider area. Returns you to your original heading when it stops. |
| **Elytra Cruise** | Fires a rocket whenever you slow down and holds the altitude you took off at, while you steer. Measures real distance flown. Stops at your target distance, when rockets run out, or before the elytra breaks, and leaves you gliding. |

Modules that can't work together (like Auto Attack and Hold Left Click) swap each other out, and each module's page lists what it can't run with.

**Presets** start a combination in one keypress: *Mob Grinder*, *Sweep Grinder*, *AFK Miner*, *Elytra Trip*. Click the module icons on a preset card to change what it runs.

**Control panel** (`K`): sidebar of modules with running lights, a page per module with switches, sliders and option pickers, hover help on every setting, and inline key binding (click a key box, press a key, Esc clears). Mouse wheel adjusts sliders; arrow keys switch pages.

**Safety:** stops everything on death, at low health, or if another player hits you. Warns when your inventory fills up or food runs low. Optional toast at an XP level (e.g. 30 for enchanting).

**Feedback:** Minecraft-style toasts, a live status HUD (session time, hits, food left, flight distance), alert sounds, and a taskbar flash when something needs you while you're tabbed out. A session summary shows when you stop.

**Freecam:** works with the [Freecam](https://modrinth.com/mod/freecam) mod. Auto Attack and Mob Sweep act on where your character faces, and Elytra Cruise carries the free camera along with you.

## Controls

| Key | Action |
|---|---|
| `K` | Open the control panel (also available from Mod Menu) |
| `Z` | Stop everything |
| `H` | Toggle Auto Walk (walks forward by default) |
| *unbound* | Toggle any other module, or start any preset |

Change these under **Options → Controls → Key Binds → Smart Autoclicker**.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.18.4+ (in Prism Launcher: *Edit instance → Version → Install Fabric*).
2. Add [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version.
3. Add the jar for your Minecraft version to the `mods` folder, e.g. `smart-autoclicker-mc26.1-26.1.2-<version>.jar` for 26.1, 26.1.1 and 26.1.2.
4. Optional: [Mod Menu](https://modrinth.com/mod/modmenu) for a config button in the mod list.

Settings are saved to `config/smart-autoclicker.json`. Nothing starts automatically when you join a world.

## Build & test

One source tree builds every supported Minecraft version with [Stonecutter](https://stonecutter.kikugie.dev/). Requires Java 25.

```bash
./gradlew build verifyGroupClasses       # every version; jars in versions/<mc>/build/libs
./gradlew :26.3:runClient                # dev client with the mod
./gradlew runClientGameTest              # plays through every feature in a real world, on every version
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for how versions share jars and how to add a new Minecraft version, and
[docs/ROADMAP.md](docs/ROADMAP.md) for older versions.

## Server rules

This is a client-side mod. Only use it on servers that allow auto clickers and AFK automation.

## License

MIT
