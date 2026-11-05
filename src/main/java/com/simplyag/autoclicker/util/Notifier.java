package com.simplyag.autoclicker.util;

import com.simplyag.autoclicker.AutoClickerClient;
import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.module.Module;
import com.simplyag.autoclicker.ui.ControlPanelScreen;
import com.simplyag.autoclicker.ui.SmartToast;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Toasts, sounds and taskbar flashes. Alerts are loud on purpose, since you may be tabbed out. */
public final class Notifier {
    private static final int COLOR_ON = 0xFF7CE07C;
    private static final int COLOR_OFF = 0xFFB0B0B0;
    private static final int COLOR_INFO = 0xFFFFE066;
    private static final int COLOR_GOOD = 0xFF7CE07C;
    private static final int COLOR_BAD = 0xFFFF6B6B;

    private static final Object TOGGLE_TOKEN = new Object();

    private Notifier() {
    }

    public static void moduleToggled(Module module, boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (SmartConfig.get().alertSounds) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, on ? 1.4f : 0.8f));
        }
        // The control panel already shows on/off; a toast there would just cover its header.
        if (Compat.screen(mc) instanceof ControlPanelScreen) return;
        toast(TOGGLE_TOKEN, module.icon(),
                Compat.translatable(on ? "smart-autoclicker.toast.on" : "smart-autoclicker.toast.off", module.name()),
                on ? stopHint() : null, on ? COLOR_ON : COLOR_OFF);
    }

    /** "Hold Left Click on", noting anything it had to stop because they can't run together. */
    public static void moduleStarted(Module module, List<Module> replaced) {
        if (replaced.isEmpty()) {
            moduleToggled(module, true);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (SmartConfig.get().alertSounds) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4f));
        }
        if (Compat.screen(mc) instanceof ControlPanelScreen) return;
        toast(TOGGLE_TOKEN, module.icon(), Compat.translatable("smart-autoclicker.toast.on", module.name()),
                Compat.translatable("smart-autoclicker.toast.replaced", names(replaced)), COLOR_ON);
    }

    /** "Auto Attack, Hold Left Click". */
    public static Component names(List<Module> modules) {
        MutableComponent names = Compat.empty();
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) names.append(", ");
            names.append(modules.get(i).name());
        }
        return names;
    }

    /** "Press Z to stop everything", using whatever key Stop Everything is bound to. */
    public static @Nullable Component stopHint() {
        KeyMapping stop = AutoClickerClient.stopAllKey();
        if (stop == null || stop.isUnbound()) return null;
        return Compat.translatable("smart-autoclicker.toast.stop_hint", stop.getTranslatedKeyMessage());
    }

    public static void info(ItemStack icon, Component title, @Nullable Component message) {
        toast(TOGGLE_TOKEN, icon, title, message, COLOR_INFO);
    }

    /**
     * Something happened that the player should know about even if they're tabbed out:
     * plays a sound and flashes the taskbar icon.
     *
     * @param bad true for problems (red, low tone), false for good news (green, high tone)
     */
    public static void alert(ItemStack icon, Component title, @Nullable Component message, boolean bad) {
        Minecraft mc = Minecraft.getInstance();
        SmartConfig cfg = SmartConfig.get();
        if (cfg.alertSounds) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(
                    Compat.alertSound(bad), bad ? 0.7f : 1.2f, 1.0f));
        }
        if (cfg.flashTaskbar && !Compat.windowFocused(mc)) {
            Compat.flashWindow(mc);
        }
        toast(new Object(), icon, title, message, bad ? COLOR_BAD : COLOR_GOOD);
    }

    private static void toast(Object token, ItemStack icon, Component title, @Nullable Component message, int color) {
        if (!SmartConfig.get().toasts) return;
        SmartToast.show(Minecraft.getInstance(), token, icon, title, message, color);
    }
}
