package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Compat;
import com.simplyag.autoclicker.util.Input;
import com.simplyag.autoclicker.util.Inventories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Holds left click: mines continuously. Keeps going while tabbed out, and stops before the tool breaks. */
public class HoldAttackModule extends Module {
    /** True while mining directly because a Freecam camera is out (see MinecraftMixin). */
    private boolean miningDirectly;

    public HoldAttackModule() {
        super("hold_attack", Items.IRON_PICKAXE, 0xFF5AA9E6);
    }

    /** Whether the game's own left-click mining must stay out of the way this tick. */
    public boolean minesDirectly() {
        return isRunning() && miningDirectly;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || Compat.screen(mc) != null) return;
        if (ModuleManager.AUTO_EAT.isBusy()) {
            Input.release(mc.options.keyAttack);
            stopMiningDirectly(mc);
            return;
        }

        int min = SmartConfig.get().toolMinDurability;
        ItemStack held = player.getMainHandItem();
        int left = Inventories.durabilityLeft(held);
        if (min > 0 && left >= 0 && left <= min) {
            stopWithReason(Compat.translatable("smart-autoclicker.toast.tool_worn", held.getHoverName(), left), true);
            return;
        }
        if (cameraDetached(mc)) {
            // Freecam blocks the attack key and the game's mining while its camera is out, and the crosshair belongs to
            // the camera: mine the block the player itself faces, the same way holding the key does.
            Input.release(mc.options.keyAttack);
            mineFromPlayer(mc, player);
        } else {
            stopMiningDirectly(mc);
            Input.hold(mc.options.keyAttack);
        }
    }

    private void mineFromPlayer(Minecraft mc, LocalPlayer player) {
        if (mc.gameMode == null || player.isUsingItem()) return;
        BlockHitResult hit = blockFromPlayer(mc, player);
        if (hit == null) {
            stopMiningDirectly(mc);
            return;
        }
        mc.gameMode.continueDestroyBlock(hit.getBlockPos(), hit.getDirection());
        Compat.swingMainHand(player);
        miningDirectly = true;
    }

    private void stopMiningDirectly(Minecraft mc) {
        if (!miningDirectly) return;
        miningDirectly = false;
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
    }

    /** The block the player itself is looking at, within reach. */
    private static @Nullable BlockHitResult blockFromPlayer(Minecraft mc, LocalPlayer player) {
        double range = Compat.blockReach(mc, player);
        Vec3 eye = Compat.eyePosition(player);
        Vec3 end = eye.add(Compat.viewVector(Compat.xRot(player), Compat.yRot(player)).scale(range));
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    @Override
    protected void onStop(Minecraft mc) {
        Input.release(mc.options.keyAttack);
        stopMiningDirectly(mc);
    }
}
