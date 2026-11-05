package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.util.Compat;
import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Inventories;
import com.simplyag.autoclicker.util.Notifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Watches for danger while anything is running, and stops everything before it goes wrong. */
public final class SafetyMonitor {
    private static int lastHurtTime;
    private static boolean warnedFull;
    private static int lastXpLevel = -1;

    private SafetyMonitor() {
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            lastXpLevel = -1;
            return;
        }
        SmartConfig cfg = SmartConfig.get();
        boolean running = ModuleManager.anyRunning();

        // XP milestone (useful for grinders feeding an enchanting table)
        if (lastXpLevel >= 0 && running && cfg.notifyXpLevel > 0
                && lastXpLevel < cfg.notifyXpLevel && player.experienceLevel >= cfg.notifyXpLevel) {
            Notifier.alert(new ItemStack(Items.EXPERIENCE_BOTTLE),
                    Compat.translatable("smart-autoclicker.toast.xp_level", player.experienceLevel), null, false);
        }
        lastXpLevel = player.experienceLevel;

        boolean justHurt = player.hurtTime > lastHurtTime;
        lastHurtTime = player.hurtTime;
        if (!running) {
            warnedFull = false;
            return;
        }

        if (player.isDeadOrDying()) {
            ModuleManager.stopAll(Compat.translatable("smart-autoclicker.toast.died"));
            return;
        }
        if (cfg.stopAtHealth > 0 && player.getHealth() <= cfg.stopAtHealth) {
            ModuleManager.stopAll(Compat.translatable("smart-autoclicker.toast.low_health", (int) Math.ceil(player.getHealth() / 2)));
            return;
        }
        if (justHurt && cfg.stopWhenHitByPlayer) {
            DamageSource source = player.getLastDamageSource();
            if (source != null && source.getEntity() instanceof Player attacker && attacker != player) {
                ModuleManager.stopAll(Compat.translatable("smart-autoclicker.toast.hit_by_player", attacker.getName()));
                return;
            }
        }

        boolean collecting = ModuleManager.AUTO_ATTACK.isRunning() || ModuleManager.HOLD_ATTACK.isRunning();
        if (cfg.warnInventoryFull && collecting && Inventories.isFull(player)) {
            if (!warnedFull) {
                warnedFull = true;
                Notifier.alert(new ItemStack(Items.CHEST), Compat.translatable("smart-autoclicker.toast.inventory_full"),
                        Compat.translatable("smart-autoclicker.toast.inventory_full.desc"), false);
            }
        } else {
            warnedFull = false;
        }
    }
}
