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
}
