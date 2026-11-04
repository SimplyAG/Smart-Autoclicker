package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.util.Notifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One automation feature that can be switched on and off.
 * Subclasses only see {@link #tick} while running and while the player is in a world.
 */
public abstract class Module {
    private final String id;
    private final Item icon;
    private final int color;
    private boolean running;
    private @Nullable KeyMapping toggleKey;

    protected Module(String id, Item icon, int color) {
        this.id = id;
        this.icon = icon;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public Component name() {
        return Component.translatable("smart-autoclicker.module." + id);
    }

    public Component description() {
        return Component.translatable("smart-autoclicker.module." + id + ".desc");
    }

    public ItemStack icon() {
        return new ItemStack(icon);
    }

    /** Accent color (ARGB) for the HUD and toasts. */
    public int color() {
        return color;
    }

    public boolean isRunning() {
        return running;
    }

    public @Nullable KeyMapping toggleKey() {
        return toggleKey;
    }

    public void setToggleKey(KeyMapping key) {
        this.toggleKey = key;
    }

    public void toggle() {
        if (running) {
            stop(true);
        } else {
            start(true);
        }
    }

    public void start(boolean announce) {
        if (running) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ModuleManager.beforeModuleStart();
        List<Module> replaced = ModuleManager.prepareStart(this);
        running = true;
        onStart(mc);
        if (announce) {
            Notifier.moduleStarted(this, replaced);
        }
    }

    public void stop(boolean announce) {
        if (!running) return;
        running = false;
        onStop(Minecraft.getInstance());
        ModuleManager.afterStop(this);
        if (announce) {
            Notifier.moduleToggled(this, false);
        }
        ModuleManager.afterModuleStop();
    }

    /** Stops this module and shows why, e.g. "Out of rockets". */
    protected void stopWithReason(Component reason, boolean bad) {
        stop(false);
        Notifier.alert(icon(), name(), reason, bad);
    }

    /** True while the view comes from something other than the player, like the Freecam mod's camera. */
    protected static boolean cameraDetached(Minecraft mc) {
        return mc.getCameraEntity() != null && mc.getCameraEntity() != mc.player;
    }

    protected void onStart(Minecraft mc) {
    }

    protected void onStop(Minecraft mc) {
    }

    /** Called every client tick while running and in a world. */
    public abstract void tick(Minecraft mc);

    /** Short live status for the HUD, e.g. "342 hits". Null for none. */
    public @Nullable Component status() {
        return null;
    }
}
