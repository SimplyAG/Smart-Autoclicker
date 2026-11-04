package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Input;
import com.simplyag.autoclicker.util.Inventories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Holds left click: mines continuously. Keeps going while tabbed out, and stops before the tool breaks. */
public class HoldAttackModule extends Module {
    public HoldAttackModule() {
        super("hold_attack", Items.IRON_PICKAXE, 0xFF5AA9E6);
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null) return;
        if (ModuleManager.AUTO_EAT.isBusy()) {
            Input.release(mc.options.keyAttack);
            return;
        }

        int min = SmartConfig.get().toolMinDurability;
        ItemStack held = player.getMainHandItem();
        int left = Inventories.durabilityLeft(held);
        if (min > 0 && left >= 0 && left <= min) {
            stopWithReason(Component.translatable("smart-autoclicker.toast.tool_worn", held.getHoverName(), left), true);
            return;
        }
        Input.hold(mc.options.keyAttack);
    }

    @Override
    protected void onStop(Minecraft mc) {
        Input.release(mc.options.keyAttack);
    }
}
