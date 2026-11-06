package com.simplyag.autoclicker.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.15
/*import org.spongepowered.asm.mixin.gen.Accessor;*/

/**
 * KeyMapping has no setDown before Minecraft 1.15, so there the pressed state is written through this accessor
 * (see Compat.setDown). Empty on every other version.
 */
@Mixin(KeyMapping.class)
public interface LegacyKeyMappingAccessor {
    //? if <1.15 {
    /*@Accessor("isDown")
    void smart$setIsDown(boolean down);
    *///?}
}
