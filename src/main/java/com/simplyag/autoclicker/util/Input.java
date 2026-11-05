package com.simplyag.autoclicker.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplyag.autoclicker.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Drives Minecraft's own key mappings, so every action goes through the game's normal
 * input handling (attack cooldowns, item use, block breaking) instead of faking OS input.
 */
public final class Input {
    private Input() {
    }

    /** Press a key mapping for this tick. Call every tick to keep it held. */
    public static void hold(KeyMapping key) {
        key.setDown(true);
    }

    /** Let go of a key mapping, unless the player is physically holding that key themselves. */
    public static void release(KeyMapping key) {
        key.setDown(isPhysicallyDown(key));
    }

    /** Keeps crouching. With Toggle Sneak on, every press flips the key, so only press it when it's up. */
    public static void holdCrouch(Options options) {
        if (!options.keyShift.isDown()) hold(options.keyShift);
    }

    public static void releaseCrouch(Options options) {
        if (!Compat.toggleCrouch(options)) {
            release(options.keyShift);
        } else if (options.keyShift.isDown()) {
            options.keyShift.setDown(true); // Toggle Sneak ignores a release; another press turns it off
        }
    }

    /** Queue one click, exactly like a single key/mouse press. */
    public static void click(KeyMapping key) {
        KeyMappingAccessor accessor = (KeyMappingAccessor) key;
        accessor.smart$setClickCount(accessor.smart$getClickCount() + 1);
    }

    /** Keyboard keys are checked directly; mouse buttons count as released (the next real click re-syncs them). */
    private static boolean isPhysicallyDown(KeyMapping key) {
        InputConstants.Key bound = Compat.boundKey(key);
        if (bound.getType() != Compat.KEYBOARD || bound.getValue() == InputConstants.UNKNOWN.getValue()) {
            return false;
        }
        return Compat.windowFocused(Minecraft.getInstance()) && Compat.isKeyDown(Minecraft.getInstance(), bound.getValue());
    }
}
