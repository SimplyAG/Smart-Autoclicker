package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Compat;
import com.simplyag.autoclicker.util.Input;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Holds right click (eat, block, charge, place), or spams it at a set speed. */
public class HoldUseModule extends Module {
    private long lastClickMillis;

    public HoldUseModule() {
        super("hold_use", Items.SHIELD, 0xFF9B7BE0);
    }

    @Override
    protected void onStart(Minecraft mc) {
        lastClickMillis = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.player == null || Compat.screen(mc) != null) return;
        SmartConfig cfg = SmartConfig.get();
        if (cfg.useMode == SmartConfig.UseMode.HOLD) {
            // Freecam ignores held keys while its camera is out; holding past it keeps eating, blocking and charging working.
            if (cameraDetached(mc)) {
                Input.holdPastFreecam(mc.options.keyUse);
            } else {
                Input.hold(mc.options.keyUse);
            }
        } else {
            long now = System.currentTimeMillis();
            if (now - lastClickMillis >= cfg.useSpamDelayMs) {
                Input.click(mc.options.keyUse);
                lastClickMillis = now;
            }
        }
    }

    @Override
    protected void onStop(Minecraft mc) {
        Input.release(mc.options.keyUse);
    }

    @Override
    public @Nullable Component status() {
        SmartConfig cfg = SmartConfig.get();
        return cfg.useMode == SmartConfig.UseMode.HOLD
                ? Compat.translatable("smart-autoclicker.status.holding")
                : Compat.translatable("smart-autoclicker.status.every_ms", cfg.useSpamDelayMs);
    }
}
