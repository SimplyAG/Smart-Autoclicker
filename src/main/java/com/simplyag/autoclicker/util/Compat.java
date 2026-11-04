package com.simplyag.autoclicker.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
//? if >=26.3 {
import org.lwjgl.sdl.SDLVideo;
//?} else {
/*import org.lwjgl.glfw.GLFW;
*///?}

/**
 * Every Minecraft API that differs between the supported versions, in one place. The rest of the mod calls these
 * instead of the game directly, so a new Minecraft version usually means changes here only.
 * Branches are Stonecutter conditions: each version project compiles only the branch that matches it.
 */
public final class Compat {
    /** The keyboard key type: KEYSYM until 26.2, KEYBOARD from 26.3 (SDL input). */
    //? if >=26.3 {
    public static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYBOARD;
    //?} else
    /*public static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYSYM;*/

    private Compat() {
    }

    public static @Nullable Screen screen(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.screen();
        //?} else
        /*return mc.screen;*/
    }

    public static void setScreen(Minecraft mc, @Nullable Screen screen) {
        //? if >=26.2 {
        mc.gui.setScreen(screen);
        //?} else
        /*mc.setScreen(screen);*/
    }

    /** True while the HUD is hidden with F1. */
    public static boolean hudHidden(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.hud.isHidden();
        //?} else
        /*return mc.options.hideGui;*/
    }

    public static ToastManager toasts(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.toastManager();
        //?} else
        /*return mc.getToastManager();*/
    }

    /** Whether a keyboard key is physically held down right now. */
    public static boolean isKeyDown(Minecraft mc, int key) {
        //? if >=26.3 {
        return InputConstants.isKeyDown(key);
        //?} else
        /*return InputConstants.isKeyDown(mc.getWindow(), key);*/
    }

    /** Plays the main-hand swing, as a real attack does. */
    public static void swingMainHand(LocalPlayer player) {
        //? if >=26.3 {
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
        //?} else
        /*player.swing(InteractionHand.MAIN_HAND);*/
    }

    /**
     * The direction a pitch/yaw looks in. Computed here rather than with Entity.calculateViewVector, which changed
     * from an instance to a static method and also follows a Freecam camera on some versions.
     */
    public static Vec3 viewVector(float xRot, float yRot) {
        float pitch = xRot * Mth.DEG_TO_RAD;
        float yaw = -yRot * Mth.DEG_TO_RAD;
        float horizontal = Mth.cos(pitch);
        return new Vec3(Mth.sin(yaw) * horizontal, -Mth.sin(pitch), Mth.cos(yaw) * horizontal);
    }

    /** Flashes the game's taskbar button until the window is focused. */
    public static void flashWindow(Minecraft mc) {
        //? if >=26.3 {
        SDLVideo.SDL_FlashWindow(mc.getWindow().handle(), SDLVideo.SDL_FLASH_UNTIL_FOCUSED);
        //?} else
        /*GLFW.glfwRequestWindowAttention(mc.getWindow().handle());*/
    }

    /** Minimizes the game window (used by the in-game tests to stay out of the way). */
    public static void minimizeWindow(Minecraft mc) {
        //? if >=26.3 {
        SDLVideo.SDL_MinimizeWindow(mc.getWindow().handle());
        //?} else
        /*GLFW.glfwIconifyWindow(mc.getWindow().handle());*/
    }
}
