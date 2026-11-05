package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Compat;
import com.simplyag.autoclicker.util.Input;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Holds a movement key, with optional sprinting and jumping (for climbing over blocks). */
public class AutoWalkModule extends Module {
    private @Nullable KeyMapping heldKey;
    private boolean sprinting;
    private boolean jumping;
    private boolean crouching;

    public AutoWalkModule() {
        super("auto_walk", Items.LEATHER_BOOTS, 0xFF6CC56C);
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.player == null || Compat.screen(mc) != null) return;
        SmartConfig cfg = SmartConfig.get();
        KeyMapping key = keyFor(mc.options, cfg.walkDirection);
        if (heldKey != null && heldKey != key) Input.release(heldKey); // direction changed in settings
        heldKey = key;
        Input.hold(key);

        // Minecraft won't let you start sprinting while crouching, so crouch wins if both are ever set
        if (cfg.walkSprint && !cfg.walkCrouch && cfg.walkDirection == SmartConfig.WalkDirection.FORWARD) {
            mc.player.setSprinting(true);
            sprinting = true;
        } else if (sprinting) {
            mc.player.setSprinting(false);
            sprinting = false;
        }
        if (cfg.walkJump) {
            Input.hold(mc.options.keyJump);
            jumping = true;
        } else if (jumping) {
            Input.release(mc.options.keyJump);
            jumping = false;
        }
        if (cfg.walkCrouch) {
            Input.holdCrouch(mc.options);
            crouching = true;
        } else if (crouching) {
            Input.releaseCrouch(mc.options);
            crouching = false;
        }
    }

    @Override
    protected void onStop(Minecraft mc) {
        if (heldKey != null) Input.release(heldKey);
        heldKey = null;
        if (jumping) Input.release(mc.options.keyJump);
        jumping = false;
        if (crouching) Input.releaseCrouch(mc.options);
        crouching = false;
        if (sprinting && mc.player != null) mc.player.setSprinting(false);
        sprinting = false;
    }

    private static KeyMapping keyFor(Options options, SmartConfig.WalkDirection direction) {
        switch (direction) {
            case BACKWARD:
                return options.keyDown;
            case LEFT:
                return options.keyLeft;
            case RIGHT:
                return options.keyRight;
            default:
                return options.keyUp;
        }
    }

    @Override
    public @Nullable Component status() {
        return Compat.translatable("smart-autoclicker.walk." + SmartConfig.get().walkDirection.name().toLowerCase());
    }
}
