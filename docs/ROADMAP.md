# Minecraft version roadmap

Goal: one source tree covering Minecraft 1.14.4 → latest on Fabric, with as few jars as possible (see release
groups in `CONTRIBUTING.md`). Each phase below is shippable on its own; newer versions come first because most
players are there.

| Phase | Minecraft | Status |
|---|---|---|
| 1 | 26.1 – 26.3 | **Done**: 3 jars (26.1–26.1.2 share one), in-game tests on every version |
| 2 | 1.21 – 1.21.11 | **Done**: 6 jars for the 12 versions, in-game tests on every version (Fabric's game tests from 1.21.4, the mod's own harness before) |
| 3 | 1.20.x | Planned |
| 4 | 1.17 – 1.19.4 | Planned |
| 5 | 1.14.4 – 1.16.5 | Planned |

## What changes going back

Minecraft before 26.1 is **obfuscated**, so those targets need the remapping Loom
(`net.fabricmc.fabric-loom-remap`) with Mojang's official mappings. Because the mod's code already uses Mojang names,
most classes keep their names; the work is in APIs that changed. The main breaks, newest first:

| From → to | What differs for this mod |
|---|---|
| 26.1 → 1.21.11 | Obfuscated game (remap Loom, Java 21). Rendering is `GuiGraphics` instead of `GuiGraphicsExtractor` (screen, HUD, toast drawing, all behind `ui/Gfx`). Pause on alt-tab is in `GameRenderer.render`, not `Minecraft.pauseIfInactive`. Inventory swaps use `ClickType`. Fabric API says `KeyBindingHelper`. |
| 1.21.11 → 1.21.10 | `ResourceLocation` instead of `Identifier`; golems and villagers in the older packages. |
| 1.21.9 → 1.21.8 | No `KeyMapping.Category` (categories are strings). No `KeyEvent`/`MouseButtonEvent`: screens get raw key/mouse arguments. |
| 1.21.6 → 1.21.5 | GUI render-state rewrite: matrix stack, blit calls and tooltips differ; the screen draws its own background; HUD via `HudRenderCallback` instead of `HudElementRegistry`. |
| 1.21.5 → 1.21.4 | Selected hotbar slot is a field, not `getSelectedSlot()`; owners are a UUID, not an `EntityReference`. |
| 1.21.4 → 1.21.3 | No Fabric client game-test API: the same tests run through the mod's own harness (`src/harness`) in a normal dev client instead. |
| 1.21.2 → 1.21.1 | Old toast interface (`ToastComponent`, one `render` that returns visibility); no `GLIDER` component (elytra only); blur takes the partial tick; `blit` without a render type. |
| 1.20.5 → 1.20.4 | Items use NBT, not data components (food, durability checks). Java 17. |
| 1.20 → 1.19.4 | No `GuiGraphics`: drawing goes through `PoseStack` and `GuiComponent`. |
| 1.17 → 1.16.5 | **Java 8**: records, sealed interfaces, pattern-matching `switch` and `instanceof` patterns must be rewritten as plain classes. Plan this as its own refactor before phase 5. |

Mod Menu and Freecam don't exist for every old version; those integrations stay optional and are tested only where
a build exists.
