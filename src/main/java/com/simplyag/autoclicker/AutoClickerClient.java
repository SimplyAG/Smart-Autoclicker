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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
/*import net.minecraft.resources.ResourceLocation;*/
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AutoClickerClient implements ClientModInitializer {
    public static final String MOD_ID = "smart-autoclicker";
    public static final Logger LOGGER = LoggerFactory.getLogger("Smart Autoclicker");

    //? if >=1.21.11 {
    public static final Identifier ICON_TEXTURE = id("icon.png");
    //?} else
    /*public static final ResourceLocation ICON_TEXTURE = id("icon.png");*/

    private static KeyMapping openPanelKey;
    private static KeyMapping stopAllKey;
    private static final List<PresetKey> presetKeys = new ArrayList<>();

    private record PresetKey(ModuleManager.Preset preset, KeyMapping key) {
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
    //?} else {
    /*public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> ModuleManager.stopAll(null)));
        Compat.registerHud("status", StatusHud::draw);
    }

    private static void registerKeys() {
        openPanelKey = register("open_panel", InputConstants.KEY_K);
        stopAllKey = register("stop_all", InputConstants.KEY_Z);
        for (ModuleManager.Preset preset : ModuleManager.PRESETS) {
            presetKeys.add(new PresetKey(preset, register("preset." + preset.id(), InputConstants.UNKNOWN.getValue())));
        }
        for (Module module : ModuleManager.ALL) {
            int defaultKey = module == ModuleManager.AUTO_WALK ? InputConstants.KEY_H : InputConstants.UNKNOWN.getValue();
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
