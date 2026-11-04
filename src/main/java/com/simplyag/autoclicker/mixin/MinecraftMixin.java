package com.simplyag.autoclicker.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simplyag.autoclicker.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    //? if >=26.1 {
    /** Don't open the pause menu on alt-tab while automation is running, so it keeps working in the background. */
    @Inject(method = "pauseIfInactive", at = @At("HEAD"), cancellable = true)
    private void smart$keepRunningWhenUnfocused(CallbackInfo ci) {
        if (ModuleManager.keepRunningUnfocused()) {
            ci.cancel();
        }
    }
    //?}
    // Before 26.1 the pause on alt-tab happens in GameRenderer.render instead: see GameRendererMixin.

    /** Holding attack only mines while the mouse is grabbed; treat it as grabbed while Hold Left is on. */
    @ModifyExpressionValue(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z")
    )
    private boolean smart$mineWhileUnfocused(boolean grabbed) {
        return grabbed || ModuleManager.HOLD_ATTACK.isRunning();
    }
}
