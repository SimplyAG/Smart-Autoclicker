package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Compat;
import com.simplyag.autoclicker.util.Input;
import com.simplyag.autoclicker.util.Inventories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
//? if >=1.21.11 {
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
//?} else {
/*import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
*///?}
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Attacks whatever mob is under the crosshair. In PERFECT timing it waits for a full attack charge,
 * which is the most damage per second with any weapon. Clicks go through Minecraft's own attack
 * handling, so reach, cooldowns and sweeping work exactly like real clicks.
 */
public class AutoAttackModule extends Module {
    private long lastAttackMillis;
    private boolean toolWarned;

    public AutoAttackModule() {
        super("auto_attack", Items.IRON_SWORD, 0xFFE0524A);
    }

    @Override
    protected void onStart(Minecraft mc) {
        lastAttackMillis = 0;
        toolWarned = false;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || Compat.screen(mc) != null) return;
        if (player.isUsingItem() || ModuleManager.AUTO_EAT.isBusy()) return;
        // With a detached camera (Freecam), the crosshair belongs to the camera, not to where you're facing
        boolean detached = cameraDetached(mc);
        HitResult target = detached ? pickFromPlayer(mc, player) : mc.hitResult;
        if (!(target instanceof EntityHitResult)) return;
        EntityHitResult hit = (EntityHitResult) target;
        if (!shouldAttack(player, hit.getEntity())) return;
        if (toolTooWorn(player)) return;

        SmartConfig cfg = SmartConfig.get();
        long now = System.currentTimeMillis();
        boolean ready = cfg.attackTiming == SmartConfig.AttackTiming.PERFECT
                ? player.getAttackStrengthScale(0.5f) >= 1.0f
                : now - lastAttackMillis >= cfg.attackDelayMs;
        if (!ready) return;

        if (detached) {
            // Freecam blocks the attack key while it's on, so hit directly, the same way the key does
            if (player.isHandsBusy() || mc.gameMode == null) return;
            mc.gameMode.attack(player, hit.getEntity());
            Compat.swingMainHand(player);
        } else {
            Input.click(mc.options.keyAttack);
        }
        lastAttackMillis = now;
        Session session = ModuleManager.session();
        if (session != null) session.attacks++;
    }

    /** What the player itself is looking at: the nearest entity within reach that no block is in front of. */
    private static @Nullable EntityHitResult pickFromPlayer(Minecraft mc, LocalPlayer player) {
        double range = Compat.entityReach(player);
        Vec3 eye = player.getEyePosition();
        Vec3 look = Compat.viewVector(player.getXRot(), player.getYRot());
        Vec3 end = eye.add(look.scale(range));
        HitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double maxSqr = block.getType() == HitResult.Type.MISS ? range * range : block.getLocation().distanceToSqr(eye);
        AABB area = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
        return ProjectileUtil.getEntityHitResult(player, eye, end, area, e -> !e.isSpectator() && e.isPickable(), maxSqr);
    }

    public static boolean shouldAttack(LocalPlayer self, Entity target) {
        if (!(target instanceof LivingEntity) || !target.isAlive() || target == self) return false;
        SmartConfig cfg = SmartConfig.get();
        if (target instanceof Player) return cfg.attackPlayers;
        if (target instanceof ArmorStand) return cfg.attackArmorStands;
        if (cfg.protectNamedAndTamed) {
            if (target.hasCustomName()) return false;
            if (target instanceof OwnableEntity && Compat.hasOwner((OwnableEntity) target)) return false;
            if (target instanceof AbstractVillager || target instanceof AbstractGolem) return false;
        }
        return target instanceof Enemy ? cfg.attackHostiles : cfg.attackPassives;
    }

    /** Stops swinging a nearly broken weapon (unless the option is off). Warns once. */
    private boolean toolTooWorn(LocalPlayer player) {
        int min = SmartConfig.get().toolMinDurability;
        ItemStack held = player.getMainHandItem();
        int left = Inventories.durabilityLeft(held);
        if (min <= 0 || left < 0 || left > min) {
            toolWarned = false;
            return false;
        }
        if (!toolWarned) {
            toolWarned = true;
            stopWithReason(Compat.translatable("smart-autoclicker.toast.tool_worn", held.getHoverName(), left), true);
        }
        return true;
    }

    @Override
    public @Nullable Component status() {
        Session session = ModuleManager.session();
        return session == null ? null : Compat.translatable("smart-autoclicker.status.hits", session.attacks);
    }
}
