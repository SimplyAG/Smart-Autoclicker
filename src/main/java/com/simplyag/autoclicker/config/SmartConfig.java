package com.simplyag.autoclicker.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simplyag.autoclicker.AutoClickerClient;
import com.simplyag.autoclicker.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All user settings, saved as JSON in config/smart-autoclicker.json.
 * Which modules are running is deliberately NOT saved: nothing starts by itself on login.
 */
public final class SmartConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("smart-autoclicker.json");

    private static SmartConfig instance = new SmartConfig();

    public static SmartConfig get() {
        return instance;
    }

    // ---- Auto Attack ----
    public enum AttackTiming { PERFECT, FIXED }

    public AttackTiming attackTiming = AttackTiming.PERFECT;
    /** Used when attackTiming = FIXED. */
    public int attackDelayMs = 550;
    public boolean attackHostiles = true;
    public boolean attackPassives = false;
    public boolean attackPlayers = false;
    public boolean attackArmorStands = false;
    /** Protects pets, name-tagged mobs, villagers and golems. */
    public boolean protectNamedAndTamed = true;

    // ---- Auto Eat ----
    /** Eat when the hunger bar is at or below this (out of 20). */
    public int eatAtHunger = 14;
    /** Also eat when health is at or below this (out of 20), as long as hunger isn't full. */
    public int eatAtHealth = 12;
    public boolean allowGoldenFood = false;
    /** Pull food from the main inventory into the hotbar when the hotbar has none. */
    public boolean restockFromInventory = true;

    // ---- Hold Use (right click) ----
    public enum UseMode { HOLD, SPAM }

    public UseMode useMode = UseMode.HOLD;
    public int useSpamDelayMs = 250;

    // ---- Auto Walk ----
    public enum WalkDirection { FORWARD, BACKWARD, LEFT, RIGHT }

    public WalkDirection walkDirection = WalkDirection.FORWARD;
    public boolean walkSprint = false;
    public boolean walkJump = false;
    public boolean walkCrouch = false;

    // ---- Mob Sweep ----
    /** Total sweep width in degrees (half each side of where you started). */
    public int sweepWidthDeg = 60;
    /** Seconds for one full left-right-left cycle. */
    public int sweepPeriodTenths = 30;

    // ---- Elytra Cruise ----
    public int elytraTargetDistance = 7000;
    /** Fire a rocket when horizontal speed drops below this (blocks per second). */
    public int elytraMinSpeed = 25;
    public boolean elytraHoldAltitude = true;
    public boolean elytraCameraFollow = true;
    public boolean elytraCameraSteer = false;
    /** Stop when elytra durability is at or below this. */
    public int elytraMinDurability = 20;

    // ---- Safety ----
    public boolean keepRunningWhenUnfocused = true;
    /** Stop everything at or below this health (0 = off). */
    public int stopAtHealth = 6;
    public boolean stopWhenHitByPlayer = true;
    /** Stop attacking/mining when the held tool has this much durability left (0 = off). */
    public int toolMinDurability = 10;
    public boolean warnInventoryFull = true;
    /** Toast when you reach this XP level (0 = off). */
    public int notifyXpLevel = 30;

    // ---- Notifications & HUD ----
    public boolean toasts = true;
    public boolean alertSounds = true;
    public boolean flashTaskbar = true;
    public boolean showHud = true;

    public enum HudCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public HudCorner hudCorner = HudCorner.TOP_LEFT;

    // ---- Presets: preset id -> module ids it starts (editable in the control panel) ----
    public Map<String, List<String>> presets = defaultPresets();

    public static Map<String, List<String>> defaultPresets() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("mob_grinder", new ArrayList<>(ImmutableList.of("auto_attack", "auto_eat")));
        map.put("sweep_grinder", new ArrayList<>(ImmutableList.of("auto_attack", "auto_eat", "mob_sweep")));
        map.put("afk_miner", new ArrayList<>(ImmutableList.of("hold_attack", "auto_eat")));
        map.put("elytra_trip", new ArrayList<>(ImmutableList.of("elytra_cruise", "auto_eat")));
        return map;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                SmartConfig loaded = GSON.fromJson(reader, SmartConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                AutoClickerClient.LOGGER.error("Couldn't read {}, using defaults", PATH, e);
            }
        }
        instance.sanitize();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            AutoClickerClient.LOGGER.error("Couldn't save {}", PATH, e);
        }
    }

    /** Fixes values from hand-edited or older config files. */
    private void sanitize() {
        if (attackTiming == null) attackTiming = AttackTiming.PERFECT;
        if (useMode == null) useMode = UseMode.HOLD;
        if (walkDirection == null) walkDirection = WalkDirection.FORWARD;
        if (hudCorner == null) hudCorner = HudCorner.TOP_LEFT;
        if (walkCrouch) walkSprint = false;
        // Keep every built-in preset present (older configs, hand edits), with mutable lists
        Map<String, List<String>> fixed = defaultPresets();
        if (presets != null) {
            presets.forEach((id, modules) -> {
                if (fixed.containsKey(id) && modules != null) fixed.put(id, ModuleManager.normalize(modules));
            });
        }
        presets = fixed;
        attackDelayMs = clamp(attackDelayMs, 50, 5000);
        eatAtHunger = clamp(eatAtHunger, 1, 19);
        eatAtHealth = clamp(eatAtHealth, 0, 20);
        useSpamDelayMs = clamp(useSpamDelayMs, 50, 5000);
        sweepWidthDeg = clamp(sweepWidthDeg, 10, 180);
        sweepPeriodTenths = clamp(sweepPeriodTenths, 10, 200);
        elytraTargetDistance = clamp(elytraTargetDistance, 100, 1_000_000);
        elytraMinSpeed = clamp(elytraMinSpeed, 5, 60);
        elytraMinDurability = clamp(elytraMinDurability, 0, 400);
        stopAtHealth = clamp(stopAtHealth, 0, 19);
        toolMinDurability = clamp(toolMinDurability, 0, 500);
        notifyXpLevel = clamp(notifyXpLevel, 0, 100);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
