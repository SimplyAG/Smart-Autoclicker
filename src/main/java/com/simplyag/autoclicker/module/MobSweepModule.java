package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Smoothly turns left and right around where you were facing, so Auto Attack covers a wider area
 * (e.g. two spawner kill chambers). Moves by a delta each tick, so you can still adjust your aim.
 */
public class MobSweepModule extends Module {
    private int ticks;
    private float lastOffset;

    public MobSweepModule() {
        super("mob_sweep", Items.COMPASS, 0xFF4FC3C3);
    }

    @Override
    protected void onStart(Minecraft mc) {
        ticks = 0;
        lastOffset = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || Compat.screen(mc) != null || ModuleManager.AUTO_EAT.isBusy()) return;
        SmartConfig cfg = SmartConfig.get();
        ticks++;
        float periodTicks = cfg.sweepPeriodTenths * 2f; // tenths of a second -> ticks
        float offset = (cfg.sweepWidthDeg / 2f) * Mth.sin(ticks * Mth.TWO_PI / periodTicks);
        turn(player, offset - lastOffset);
        lastOffset = offset;
    }

    @Override
    protected void onStop(Minecraft mc) {
        // Return to the original heading
        if (mc.player != null) turn(mc.player, -lastOffset);
        lastOffset = 0;
    }

    /** Rotates without a visual snap: the renderer interpolates from the old yaw to the new one. */
    private static void turn(LocalPlayer player, float degrees) {
        player.yRotO = player.getYRot();
        player.setYRot(player.getYRot() + degrees);
    }

    @Override
    public @Nullable Component status() {
        return Compat.translatable("smart-autoclicker.status.sweep", SmartConfig.get().sweepWidthDeg);
    }
}
