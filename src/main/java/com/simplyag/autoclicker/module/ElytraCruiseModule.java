package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Compat;
import com.simplyag.autoclicker.util.Inventories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Elytra autopilot: fires a rocket whenever you slow down, holds the altitude you took off at,
 * and stops at the target distance, when rockets run out, or before the elytra breaks.
 * You keep steering left/right with the mouse.
 */
public class ElytraCruiseModule extends Module {
    private static final int ROCKET_COOLDOWN_TICKS = 30;
    private static final float MAX_PITCH_STEP = 1.5f;
    private static final float MAX_YAW_STEP = 3f;

    private @Nullable Vec3 start;
    private @Nullable Vec3 lastPos;
    private double targetY;
    private boolean flying;
    private int groundedTicks;
    private int rocketCooldown;
    private int previousSlot = -1;
    private boolean rocketQueued;
    private double distance;
    private double speed;
    private int rocketsLeft;

    public ElytraCruiseModule() {
        super("elytra_cruise", Items.FIREWORK_ROCKET, 0xFFC77DFF);
    }

    @Override
    protected void onStart(Minecraft mc) {
        start = null;
        lastPos = null;
        flying = false;
        groundedTicks = 0;
        rocketCooldown = 0;
        previousSlot = -1;
        rocketQueued = false;
        distance = 0;
        speed = 0;
    }

    @Override
    protected void onStop(Minecraft mc) {
        restoreSlot(mc.player);
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        SmartConfig cfg = SmartConfig.get();
        rocketsLeft = Inventories.count(player, s -> s.getItem() == Items.FIREWORK_ROCKET)
                + (player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET ? player.getOffhandItem().getCount() : 0);

        // A rocket queued last tick after switching to it: fire it now, then switch back.
        if (rocketQueued) {
            rocketQueued = false;
            fire(mc, player, InteractionHand.MAIN_HAND);
            return;
        }
        restoreSlot(player);

        ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
        int durability = Inventories.durabilityLeft(elytra);
        if (Compat.isGlider(elytra) && durability >= 0 && durability <= cfg.elytraMinDurability) {
            stopWithReason(Compat.translatable("smart-autoclicker.toast.elytra_worn", durability), true);
            return;
        }

        if (!player.isFallFlying()) {
            lastPos = null;
            if (flying && ++groundedTicks > 40) {
                stopWithReason(Compat.translatable("smart-autoclicker.toast.landed", (int) distance), false);
            }
            return;
        }
        groundedTicks = 0;
        if (!flying) {
            flying = true;
            start = player.position();
            targetY = player.position().y;
        }

        Vec3 pos = player.position();
        if (cameraDetached(mc)) followWithCamera(mc, player, cfg);
        lastPos = pos;
        distance = Math.hypot(pos.x - start.x, pos.z - start.z);
        Vec3 motion = player.getDeltaMovement();
        speed = Math.hypot(motion.x, motion.z) * 20;

        if (distance >= cfg.elytraTargetDistance) {
            stopWithReason(Compat.translatable("smart-autoclicker.toast.arrived", (int) distance), false);
            return;
        }
        if (Compat.screen(mc) != null) return;

        if (cfg.elytraHoldAltitude) holdAltitude(player);

        if (rocketCooldown > 0) {
            rocketCooldown--;
        } else if (speed < cfg.elytraMinSpeed && !ModuleManager.AUTO_EAT.isBusy()) {
            // Switching to a rocket mid-bite would cancel the meal; it's only a second or two
            useRocket(mc, player);
        }
    }

    /**
     * With a detached camera (Freecam) the mouse only turns the camera, and the camera stays behind
     * as you fly off. Carries it along with you, keeping its offset so you can still fly it around,
     * and optionally turns you toward wherever it's facing.
     */
    private void followWithCamera(Minecraft mc, LocalPlayer player, SmartConfig cfg) {
        Entity camera = mc.getCameraEntity();
        if (cfg.elytraCameraFollow && lastPos != null) {
            Vec3 moved = player.position().subtract(lastPos);
            camera.setPos(camera.position().x + moved.x, camera.position().y + moved.y, camera.position().z + moved.z);
        }
        if (cfg.elytraCameraSteer) {
            float yaw = Compat.yRot(player);
            player.yRotO = yaw;
            Compat.setYRot(player, yaw + Mth.clamp(Mth.wrapDegrees(Compat.yRot(camera) - yaw), -MAX_YAW_STEP, MAX_YAW_STEP));
        }
    }

    /** Pitches gently up when below the takeoff height and down when above it. */
    private void holdAltitude(LocalPlayer player) {
        double below = targetY - player.position().y;
        float desired = (float) Mth.clamp(-4 - below * 1.5, -30, 15);
        float pitch = Compat.xRot(player);
        float next = pitch + Mth.clamp(desired - pitch, -MAX_PITCH_STEP, MAX_PITCH_STEP);
        player.xRotO = pitch;
        Compat.setXRot(player, next);
    }

    private void useRocket(Minecraft mc, LocalPlayer player) {
        if (player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET) {
            fire(mc, player, InteractionHand.OFF_HAND);
            return;
        }
        int slot = Inventories.findHotbar(player, Items.FIREWORK_ROCKET);
        if (slot == -1) {
            int stored = Inventories.findInventory(player, Items.FIREWORK_ROCKET);
            if (stored != -1 && Inventories.swapIntoHotbar(player, stored, Inventories.spareHotbarSlot(player))) {
                rocketCooldown = 2;
            } else {
                stopWithReason(Compat.translatable("smart-autoclicker.toast.out_of_rockets", (int) distance), true);
            }
            return;
        }
        int selected = Compat.selectedSlot(Compat.inventory(player));
        if (slot == selected) {
            fire(mc, player, InteractionHand.MAIN_HAND);
        } else {
            previousSlot = selected;
            Compat.selectSlot(Compat.inventory(player), slot);
            rocketQueued = true; // fire next tick, once the slot change has reached the server
        }
    }

    private void fire(Minecraft mc, LocalPlayer player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() != Items.FIREWORK_ROCKET || mc.gameMode == null) return;
        Compat.useItem(mc, player, hand);
        rocketCooldown = ROCKET_COOLDOWN_TICKS;
        Session session = ModuleManager.session();
        if (session != null) session.rocketsFired++;
    }

    private void restoreSlot(@Nullable LocalPlayer player) {
        if (player != null && previousSlot >= 0) {
            Compat.selectSlot(Compat.inventory(player), previousSlot);
        }
        previousSlot = -1;
    }

    @Override
    public @Nullable Component status() {
        if (!flying) return Compat.translatable("smart-autoclicker.status.waiting_takeoff");
        return Compat.translatable("smart-autoclicker.status.elytra",
                String.format("%,d", (int) distance), String.format("%,d", SmartConfig.get().elytraTargetDistance),
                (int) speed, rocketsLeft);
    }
}
