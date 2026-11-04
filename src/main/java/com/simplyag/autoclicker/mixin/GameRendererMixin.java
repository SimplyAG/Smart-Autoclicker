package com.simplyag.autoclicker.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if <26.1 {
/*import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.simplyag.autoclicker.module.ModuleManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.At;
*///?}

/**
 * Before 26.1 the game opens the pause menu on alt-tab from GameRenderer.render; from 26.1 it does so in
 * Minecraft.pauseIfInactive (see MinecraftMixin), and this mixin is empty.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    //? if <26.1 {
    /*// Don't open the pause menu on alt-tab while automation is running, so it keeps working in the background.
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame(Z)V"))
    private boolean smart$keepRunningWhenUnfocused(Minecraft minecraft, boolean pauseOnly) {
        return !ModuleManager.keepRunningUnfocused();
    }
    *///?}
}
