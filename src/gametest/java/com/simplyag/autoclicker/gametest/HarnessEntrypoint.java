package com.simplyag.autoclicker.gametest;

import net.fabricmc.api.ClientModInitializer;

/**
 * Starts the mod's own test harness (src/harness) on Minecraft versions without Fabric's client game-test API, when the
 * game runs with -Dsmart-autoclicker.harness=true. Does nothing otherwise, so the test mod is the same on every version.
 */
public class HarnessEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("smart-autoclicker.harness")) return;
        try {
            Class<?> harness = Class.forName("com.simplyag.autoclicker.gametest.harness.TestHarness");
            ((Runnable) harness.getDeclaredConstructor().newInstance()).run();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("The test harness isn't built for this Minecraft version", e);
        }
    }
}
