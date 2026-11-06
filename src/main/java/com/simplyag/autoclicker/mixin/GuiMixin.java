package com.simplyag.autoclicker.mixin;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.15 {
/*import com.simplyag.autoclicker.util.Compat;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * Fabric API has no HUD callback for Minecraft 1.14, so there the status HUD is drawn right after the vanilla HUD from
 * here. On every other version Fabric API's callback does it (see Compat.registerHud) and this mixin is empty.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    //? if <1.15 {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void smart$drawHud(float partialTicks, CallbackInfo ci) {
        Compat.drawHuds();
    }
    *///?}
}
