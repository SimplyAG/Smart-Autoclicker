package com.simplyag.autoclicker.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int smart$getClickCount();

    @Accessor("clickCount")
    void smart$setClickCount(int clickCount);

    /**
     * Writes the pressed state without going through setDown: KeyMapping has no setDown before 1.15, and the
     * Freecam mod cancels setDown while its camera is on (see Input.holdPastFreecam).
     */
    @Accessor("isDown")
    void smart$setIsDown(boolean down);
}
