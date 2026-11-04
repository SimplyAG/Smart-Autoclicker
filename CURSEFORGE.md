# CurseForge release kit

Everything needed to publish Smart Autoclicker on CurseForge.

## Create the project (once)

At <https://authors.curseforge.com> → **Create Project**:

| Field | Value |
|---|---|
| Game | Minecraft |
| Class | Mods |
| Name | Smart Autoclicker |
| Project URL / slug | `smart-autoclicker` (the mod's Homepage link points at `curseforge.com/minecraft/mc-mods/smart-autoclicker`, so keep this slug) |
| Summary | AFK farming done right: perfect-timing auto attack, smart auto eat, hold clicks, mob sweep and elytra cruise. Keeps going while you're tabbed out. |
| Main category | Utility & QoL |
| Other categories | Automation, Fabric |
| License | MIT |
| Avatar | `src/main/resources/assets/smart-autoclicker/icon.png` (128×128) |
| Source | https://github.com/SimplyAG/Smart-Autoclicker |
| Issues | https://github.com/SimplyAG/Smart-Autoclicker/issues |

**Screenshots:** take a few in a real world with F2: the control panel (K), the Presets page, the status HUD while grinding, and a toast. `./gradlew runClientGameTest` also saves test screenshots to `versions/<mc>/build/run/clientGameTest/screenshots/`.

## Upload a file (each release)

1. Bump `mod_version` in `gradle.properties` and add a changelog entry below.
2. `./gradlew clean build stageRelease` (see `CONTRIBUTING.md`). Upload **each** jar from `release-staging/<version>/`, one CurseForge file per jar (never the `-sources` jars).
3. File settings, per jar:

| Field | Value |
|---|---|
| Release type | **Beta** for the first release, then Release once it has had real-world use |
| Game versions | Exactly the versions listed for that jar in `manifest.json`. For 1.2.0: `mc1.21-1.21.1` → 1.21, 1.21.1; `mc1.21.2-1.21.4` → 1.21.2, 1.21.3, 1.21.4; `mc1.21.5` → 1.21.5; `mc1.21.6-1.21.8` → 1.21.6, 1.21.7, 1.21.8; `mc1.21.9-1.21.10` → 1.21.9, 1.21.10; `mc1.21.11` → 1.21.11; `mc26.1-26.1.2` → 26.1, 26.1.1, 26.1.2; `mc26.2` → 26.2; `mc26.3` → 26.3 |
| Mod loader | Fabric |
| Environment | Client |
| Java | 21 for the 1.21.x jars, 25 for the 26.x jars |
| Related projects | **Fabric API**: Required dependency. **Mod Menu**: Optional dependency |
| Changelog | Paste from the section below |

CurseForge moderators review every file before it goes live, usually within a day or two.

## Description (paste into the project page)

---

**Smart Autoclicker** takes care of the boring parts of AFK farming, the right way. Instead of faking mouse clicks from outside the game, it uses Minecraft's own controls, so it respects attack cooldowns and reach and keeps working while you're tabbed out.

### ⚔ Auto Attack
Hits the mob under your crosshair. **Perfect timing** swings only at full attack charge: the most damage per second with any weapon. Choose what to hit: hostile mobs, passive mobs, players, or armor stands (for farms that use one). It never touches pets, name-tagged mobs, villagers or golems.

### 🍖 Smart Auto Eat
Eats the best safe food when you're hungry or hurt, from your offhand or hotbar, then switches straight back to your sword. Refills your hotbar from your inventory. It never eats rotten flesh, spider eyes, raw chicken, pufferfish or chorus fruit, and it saves your golden apples. It doesn't right-click the chest or villager you're looking at, either.

### ⛏ Hold Left / Right Click
Mine all day at a cobblestone generator, hold a shield, place blocks, or spam right click at a set speed. Stops before your tool breaks.

### 🧭 Mob Sweep
Smoothly sweeps your view left and right so Auto Attack covers two kill chambers at once.

### 🪽 Elytra Cruise
Fires a rocket whenever you slow down, holds your altitude, and tracks real distance flown. Stops at your target distance, before the elytra breaks, or when rockets run out, and leaves you gliding.

### 🚶 Auto Walk
Hold any direction, with optional sprint, crouch and jump.

### Presets
**Mob Grinder**, **Sweep Grinder**, **AFK Miner** and **Elytra Trip**: one key starts the right combination. Click the icons on a preset to change what it runs. Modules that can't work together swap each other out instead of fighting over your mouse.

### A real control panel
Press **K** for a clean, dark control panel: every module in a sidebar with a live running light, animated switches, sliders, hover help for every setting, and key binding right where you need it.

### Built to be left alone
- Stops everything if you die, drop to low health, or another player hits you
- Toasts for out of food, inventory full, tool nearly broken, out of rockets, XP level reached
- Alert sounds and a taskbar flash, so you notice while tabbed out
- Live status HUD with session time, hits, food left and flight progress
- Session summary when you stop: time, hits, levels gained, food eaten

### Works with Freecam
Using the Freecam mod? Auto Attack and Mob Sweep keep working from where your character faces, and Elytra Cruise carries the free camera along so you can watch your flight.

### Controls
**K** opens the control panel (or use Mod Menu). **Z** stops everything. **H** toggles Auto Walk. Every module and preset can have its own key in *Controls → Key Binds*.

### Requirements
Minecraft 1.21 – 1.21.11 and 26.1 – 26.3, Fabric Loader, **Fabric API**. Optional: Mod Menu. Pick the file for your Minecraft version.

⚠ **Only use this on servers that allow auto clickers and AFK automation.** It's client-side only.

---

## Changelog for 1.2.0

Now also for Minecraft 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10 and 1.21.11, with every feature.

- 1.21.x on Fabric: six files cover the twelve versions (pick the one whose name includes your version)
- No changes for players on 26.x

## Changelog for 1.1.0

First release, for Minecraft 26.1, 26.1.1, 26.1.2, 26.2 and 26.3.

- Auto Attack with perfect swing timing, mob filters, armor stand option, and protection for pets, named mobs, villagers and golems
- Smart Auto Eat: best safe food, hotbar refill, switches back to your weapon
- Hold Left Click and Hold Right Click (hold or spam), with tool protection
- Auto Walk in any direction, with sprint, crouch and jump
- Mob Sweep to cover a wider area
- Elytra Cruise with rocket timing, altitude hold and distance tracking
- Presets: Mob Grinder, Sweep Grinder, AFK Miner, Elytra Trip
- Control panel (K), status HUD, toasts, alert sounds, taskbar flash
- Safety stops on death, low health and being hit by a player
- Works with Freecam
