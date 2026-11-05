package com.simplyag.autoclicker;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.module.Module;
import com.simplyag.autoclicker.module.ModuleManager;
import com.simplyag.autoclicker.module.SafetyMonitor;
import com.simplyag.autoclicker.ui.ControlPanelScreen;
import com.simplyag.autoclicker.ui.StatusHud;
import com.simplyag.autoclicker.util.Compat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=1.17
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
/*import net.minecraft.resources.ResourceLocation;*/
import org.jspecify.annotations.Nullable;
//? if >=1.17 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

import java.util.ArrayList;
import java.util.List;

public class AutoClickerClient implements ClientModInitializer {
    public static final String MOD_ID = "smart-autoclicker";
    // slf4j ships with Minecraft from 1.17; before it, log4j (same calls).
    //? if >=1.17 {
    public static final Logger LOGGER = LoggerFactory.getLogger("Smart Autoclicker");
    //?} else
    /*public static final Logger LOGGER = LogManager.getLogger("Smart Autoclicker");*/

    //? if >=1.21.11 {
    public static final Identifier ICON_TEXTURE = id("icon.png");
    //?} else
    /*public static final ResourceLocation ICON_TEXTURE = id("icon.png");*/

    private static KeyMapping openPanelKey;
    private static KeyMapping stopAllKey;
    private static final List<PresetKey> presetKeys = new ArrayList<>();
    //? if <1.17
    /*private static boolean inWorld;*/

    private static final class PresetKey {
        private final ModuleManager.Preset preset;
        private final KeyMapping key;

        PresetKey(ModuleManager.Preset preset, KeyMapping key) {
            this.preset = preset;
            this.key = key;
        }

        ModuleManager.Preset preset() {
            return preset;
        }

        KeyMapping key() {
            return key;
        }
    }

    public static @Nullable KeyMapping stopAllKey() {
        return stopAllKey;
    }

    public static @Nullable KeyMapping openPanelKey() {
        return openPanelKey;
    }

    public static @Nullable KeyMapping presetKey(ModuleManager.Preset preset) {
        for (PresetKey presetKey : presetKeys) {
            if (presetKey.preset().id().equals(preset.id())) return presetKey.key();
        }
        return null;
    }

    //? if >=1.21.11 {
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    //?} else if >=1.19 {
    /*// tryBuild exists from 1.19 to 1.21.10 (fromNamespaceAndPath only from 1.21, the constructor only before it),
    // so 1.20.x and 1.21.x compile to the same call. The id is always valid.
    public static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild(MOD_ID, path);
    }
    *///?} else {
    /*public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
    *///?}

    @Override
    public void onInitializeClient() {
        SmartConfig.load();
        registerKeys();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeys(client);
            ModuleManager.tick(client);
            SafetyMonitor.tick(client);
        });
        // Leaving a world stops everything: Fabric API's disconnect event from 1.17. Before, watch for the world going
        // away (the event only exists in late 1.16 builds; this lets all of 1.16.2 - 1.16.5 share one jar).
        //? if >=1.17 {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> ModuleManager.stopAll(null)));
        //?} else {
        /*ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null && inWorld) ModuleManager.stopAll(null);
            inWorld = client.level != null;
        });
        *///?}
        Compat.registerHud("status", StatusHud::draw);
    }

    private static void registerKeys() {
        openPanelKey = register("open_panel", Compat.KEY_K);
        stopAllKey = register("stop_all", Compat.KEY_Z);
        for (ModuleManager.Preset preset : ModuleManager.PRESETS) {
            presetKeys.add(new PresetKey(preset, register("preset." + preset.id(), InputConstants.UNKNOWN.getValue())));
        }
        for (Module module : ModuleManager.ALL) {
            int defaultKey = module == ModuleManager.AUTO_WALK ? Compat.KEY_H : InputConstants.UNKNOWN.getValue();
            module.setToggleKey(register("toggle." + module.id(), defaultKey));
        }
    }

    private static KeyMapping register(String name, int defaultKey) {
        return Compat.registerKey(name, defaultKey);
    }

    private static void handleKeys(Minecraft mc) {
        while (openPanelKey.consumeClick()) {
            if (Compat.screen(mc) == null) Compat.setScreen(mc, new ControlPanelScreen(null));
        }
        while (stopAllKey.consumeClick()) {
            ModuleManager.stopAllByUser();
        }
        for (PresetKey presetKey : presetKeys) {
            while (presetKey.key().consumeClick()) {
                ModuleManager.applyPreset(presetKey.preset());
            }
        }
        for (Module module : ModuleManager.ALL) {
            KeyMapping key = module.toggleKey();
            while (key != null && key.consumeClick()) {
                module.toggle();
            }
        }
    }
}
