package com.simplyag.autoclicker.ui.panel;

import com.simplyag.autoclicker.util.Compat;
import com.google.common.collect.ImmutableList;
import com.simplyag.autoclicker.AutoClickerClient;
import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.module.Module;
import com.simplyag.autoclicker.module.ModuleManager;

import java.util.List;

/** Which settings appear on each page of the control panel. */
public final class PanelSettings {
    private PanelSettings() {
    }

    private static SmartConfig cfg() {
        return SmartConfig.get();
    }

    private static String off() {
        return Compat.translatable("options.off").getString();
    }

    private static String hearts(int health) {
        return (health % 2 == 0 ? String.valueOf(health / 2) : String.valueOf(health / 2.0)) + " ❤";
    }

    private static String ms(int value) {
        return value + " ms";
    }

    private static final Setting TOOL_DURABILITY = new Setting.Range("tool_min_durability", 0, 100, 1,
            () -> cfg().toolMinDurability, v -> cfg().toolMinDurability = v, v -> v == 0 ? off() : String.valueOf(v));

    public static List<Setting> forModule(Module module) {
        if (module == ModuleManager.AUTO_ATTACK) {
            return ImmutableList.of(
                    new Setting.Choice<>("attack_timing", SmartConfig.AttackTiming.values(), () -> cfg().attackTiming, v -> cfg().attackTiming = v),
                    new Setting.Range("attack_delay", 50, 2000, 10, () -> cfg().attackDelayMs, v -> cfg().attackDelayMs = v,
                            PanelSettings::ms, () -> cfg().attackTiming == SmartConfig.AttackTiming.FIXED),
                    new Setting.Toggle("attack_hostiles", () -> cfg().attackHostiles, v -> cfg().attackHostiles = v),
                    new Setting.Toggle("attack_passives", () -> cfg().attackPassives, v -> cfg().attackPassives = v),
                    new Setting.Toggle("attack_players", () -> cfg().attackPlayers, v -> cfg().attackPlayers = v),
                    new Setting.Toggle("attack_armor_stands", () -> cfg().attackArmorStands, v -> cfg().attackArmorStands = v),
                    new Setting.Toggle("protect_named", () -> cfg().protectNamedAndTamed, v -> cfg().protectNamedAndTamed = v),
                    TOOL_DURABILITY);
        }
        if (module == ModuleManager.AUTO_EAT) {
            return ImmutableList.of(
                    new Setting.Range("eat_at_hunger", 1, 19, 1, () -> cfg().eatAtHunger, v -> cfg().eatAtHunger = v, v -> v + " / 20"),
                    new Setting.Range("eat_at_health", 0, 20, 1, () -> cfg().eatAtHealth, v -> cfg().eatAtHealth = v,
                            v -> v == 0 ? off() : hearts(v)),
                    new Setting.Toggle("golden_food", () -> cfg().allowGoldenFood, v -> cfg().allowGoldenFood = v),
                    new Setting.Toggle("restock", () -> cfg().restockFromInventory, v -> cfg().restockFromInventory = v));
        }
        if (module == ModuleManager.HOLD_ATTACK) {
            return ImmutableList.of(TOOL_DURABILITY);
        }
        if (module == ModuleManager.HOLD_USE) {
            return ImmutableList.of(
                    new Setting.Choice<>("use_mode", SmartConfig.UseMode.values(), () -> cfg().useMode, v -> cfg().useMode = v),
                    new Setting.Range("use_delay", 50, 2000, 10, () -> cfg().useSpamDelayMs, v -> cfg().useSpamDelayMs = v,
                            PanelSettings::ms, () -> cfg().useMode == SmartConfig.UseMode.SPAM));
        }
        if (module == ModuleManager.AUTO_WALK) {
            return ImmutableList.of(
                    new Setting.Choice<>("walk_direction", SmartConfig.WalkDirection.values(), () -> cfg().walkDirection, v -> cfg().walkDirection = v),
                    new Setting.Toggle("walk_sprint", () -> cfg().walkSprint, v -> {
                        cfg().walkSprint = v;
                        if (v) cfg().walkCrouch = false;
                    }, () -> cfg().walkDirection == SmartConfig.WalkDirection.FORWARD),
                    new Setting.Toggle("walk_crouch", () -> cfg().walkCrouch, v -> {
                        cfg().walkCrouch = v;
                        if (v) cfg().walkSprint = false;
                    }),
                    new Setting.Toggle("walk_jump", () -> cfg().walkJump, v -> cfg().walkJump = v));
        }
        if (module == ModuleManager.MOB_SWEEP) {
            return ImmutableList.of(
                    new Setting.Range("sweep_width", 10, 180, 5, () -> cfg().sweepWidthDeg, v -> cfg().sweepWidthDeg = v, v -> v + "°"),
                    new Setting.Range("sweep_period", 10, 100, 1, () -> cfg().sweepPeriodTenths, v -> cfg().sweepPeriodTenths = v,
                            v -> String.format("%.1f s", v / 10.0)));
        }
        if (module == ModuleManager.ELYTRA_CRUISE) {
            return ImmutableList.of(
                    new Setting.Range("elytra_distance", 500, 50_000, 500, () -> cfg().elytraTargetDistance,
                            v -> cfg().elytraTargetDistance = v, v -> String.format("%,d", v)),
                    new Setting.Range("elytra_min_speed", 5, 60, 1, () -> cfg().elytraMinSpeed, v -> cfg().elytraMinSpeed = v, v -> v + " b/s"),
                    new Setting.Toggle("elytra_hold_altitude", () -> cfg().elytraHoldAltitude, v -> cfg().elytraHoldAltitude = v),
                    new Setting.Toggle("elytra_camera_follow", () -> cfg().elytraCameraFollow, v -> cfg().elytraCameraFollow = v),
                    new Setting.Toggle("elytra_camera_steer", () -> cfg().elytraCameraSteer, v -> cfg().elytraCameraSteer = v),
                    new Setting.Range("elytra_min_durability", 0, 100, 1, () -> cfg().elytraMinDurability,
                            v -> cfg().elytraMinDurability = v, v -> v == 0 ? off() : String.valueOf(v)));
        }
        return ImmutableList.of();
    }

    public static List<Setting> safety() {
        return ImmutableList.of(
                new Setting.Toggle("keep_running", () -> cfg().keepRunningWhenUnfocused, v -> cfg().keepRunningWhenUnfocused = v),
                new Setting.Range("stop_at_health", 0, 19, 1, () -> cfg().stopAtHealth, v -> cfg().stopAtHealth = v,
                        v -> v == 0 ? off() : hearts(v)),
                new Setting.Toggle("stop_hit_by_player", () -> cfg().stopWhenHitByPlayer, v -> cfg().stopWhenHitByPlayer = v),
                TOOL_DURABILITY,
                new Setting.Toggle("warn_full", () -> cfg().warnInventoryFull, v -> cfg().warnInventoryFull = v),
                new Setting.Range("notify_xp", 0, 100, 1, () -> cfg().notifyXpLevel, v -> cfg().notifyXpLevel = v,
                        v -> v == 0 ? off() : String.valueOf(v)));
    }

    public static List<Setting> general() {
        return ImmutableList.of(
                new Setting.Hotkey("key_open_panel", AutoClickerClient::openPanelKey),
                new Setting.Hotkey("key_stop_all", AutoClickerClient::stopAllKey),
                new Setting.Toggle("show_hud", () -> cfg().showHud, v -> cfg().showHud = v),
                new Setting.Choice<>("hud_corner", SmartConfig.HudCorner.values(), () -> cfg().hudCorner, v -> cfg().hudCorner = v,
                        () -> cfg().showHud),
                new Setting.Toggle("toasts", () -> cfg().toasts, v -> cfg().toasts = v),
                new Setting.Toggle("alert_sounds", () -> cfg().alertSounds, v -> cfg().alertSounds = v),
                new Setting.Toggle("flash_taskbar", () -> cfg().flashTaskbar, v -> cfg().flashTaskbar = v));
    }
}
