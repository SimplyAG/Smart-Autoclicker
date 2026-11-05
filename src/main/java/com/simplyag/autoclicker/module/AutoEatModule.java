package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Compat;
import com.simplyag.autoclicker.util.Input;
import com.simplyag.autoclicker.util.Inventories;
import com.simplyag.autoclicker.util.Notifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Smart auto eat: eats the best safe food when hungry (or hurt), from the offhand or the hotbar,
 * restocks the hotbar from the inventory, then switches back to what you were holding.
 * Starts eating with a direct item use, so it never right-clicks the block or mob you're looking at.
 */
public class AutoEatModule extends Module {
    private enum State { IDLE, SWITCHED, START, EATING, COOLDOWN }

    private static final int LOW_FOOD_WARNING = 4;
    private static final int MAX_START_ATTEMPTS = 3;
    private static final int MAX_EAT_TICKS = 20 * 10;

    private State state = State.IDLE;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private int previousSlot = -1;
    private int waitTicks;
    private int startAttempts;
    private int eatTicks;
    private boolean warnedOutOfFood;
    private boolean warnedLowFood;
    private int foodLeft;

    public AutoEatModule() {
        super("auto_eat", Items.COOKED_BEEF, 0xFFE8A33D);
    }

    /** True while switching to, eating, or switching back from food. Other modules hold off meanwhile. */
    public boolean isBusy() {
        return isRunning() && state != State.IDLE && state != State.COOLDOWN;
    }

    @Override
    protected void onStart(Minecraft mc) {
        reset();
        warnedOutOfFood = false;
        warnedLowFood = false;
    }

    @Override
    protected void onStop(Minecraft mc) {
        if (state == State.EATING) {
            Input.release(mc.options.keyUse);
        }
        restoreSlot(mc.player);
        reset();
    }

    private void reset() {
        state = State.IDLE;
        previousSlot = -1;
        waitTicks = 0;
        startAttempts = 0;
        eatTicks = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        foodLeft = Inventories.countFood(player);

        // Creative players don't get hungry; Hold Right already eats whatever is in hand.
        if (Compat.hasInfiniteMaterials(player) || ModuleManager.HOLD_USE.isRunning()) return;
        // Menus stop key handling; finish up quietly if the eat ended meanwhile.
        if (Compat.screen(mc) != null) {
            if (state == State.EATING && !player.isUsingItem()) finishEating(mc, player);
            return;
        }

        switch (state) {
            case IDLE -> {
                if (waitTicks > 0) {
                    waitTicks--;
                } else if (needsFood(player)) {
                    beginEating(mc, player);
                }
            }
            case SWITCHED -> state = State.START;
            case START -> {
                Compat.useItem(mc, player, hand);
                if (player.isUsingItem()) {
                    Input.hold(mc.options.keyUse);
                    state = State.EATING;
                    eatTicks = 0;
                } else if (++startAttempts >= MAX_START_ATTEMPTS) {
                    restoreSlot(player);
                    reset();
                    waitTicks = 100;
                }
            }
            case EATING -> {
                if (player.isUsingItem() && ++eatTicks < MAX_EAT_TICKS) {
                    Input.hold(mc.options.keyUse);
                } else {
                    finishEating(mc, player);
                }
            }
            case COOLDOWN -> {
                if (--waitTicks <= 0) state = State.IDLE;
            }
        }
    }

    private boolean needsFood(LocalPlayer player) {
        SmartConfig cfg = SmartConfig.get();
        int hunger = player.getFoodData().getFoodLevel();
        if (hunger >= 20) return false;
        return hunger <= cfg.eatAtHunger || player.getHealth() <= cfg.eatAtHealth;
    }

    private void beginEating(Minecraft mc, LocalPlayer player) {
        startAttempts = 0;
        if (Inventories.foodScore(player.getOffhandItem()) > 0) {
            hand = InteractionHand.OFF_HAND;
            previousSlot = -1;
            state = State.START;
            foodFound(player);
            return;
        }

        int slot = Inventories.bestHotbarFood(player);
        if (slot == -1) {
            int stored = SmartConfig.get().restockFromInventory ? Inventories.bestInventoryFood(player) : -1;
            if (stored != -1 && Inventories.swapIntoHotbar(player, stored, Inventories.spareHotbarSlot(player))) {
                waitTicks = 2; // let the swap land, then pick it up from the hotbar
            } else {
                outOfFood();
                waitTicks = 40;
            }
            return;
        }

        foodFound(player);
        hand = InteractionHand.MAIN_HAND;
        int selected = Compat.selectedSlot(player.getInventory());
        if (slot == selected) {
            previousSlot = -1;
            state = State.START;
        } else {
            previousSlot = selected;
            Compat.selectSlot(player.getInventory(), slot);
            state = State.SWITCHED; // give the slot change a tick to reach the server
        }
    }

    private void finishEating(Minecraft mc, LocalPlayer player) {
        Input.release(mc.options.keyUse);
        Session session = ModuleManager.session();
        if (session != null && eatTicks > 0) session.foodsEaten++;
        restoreSlot(player);
        reset();
        state = State.COOLDOWN;
        waitTicks = 10;
    }

    private void restoreSlot(@Nullable LocalPlayer player) {
        if (player != null && previousSlot >= 0) {
            Compat.selectSlot(player.getInventory(), previousSlot);
        }
        previousSlot = -1;
    }

    private void foodFound(LocalPlayer player) {
        warnedOutOfFood = false;
        int left = Inventories.countFood(player);
        if (left > LOW_FOOD_WARNING) {
            warnedLowFood = false;
        } else if (!warnedLowFood) {
            warnedLowFood = true;
            Notifier.alert(icon(), Compat.translatable("smart-autoclicker.toast.low_food"),
                    Compat.translatable("smart-autoclicker.toast.low_food.desc", left), false);
        }
    }

    private void outOfFood() {
        if (!warnedOutOfFood) {
            warnedOutOfFood = true;
            Notifier.alert(icon(), Compat.translatable("smart-autoclicker.toast.out_of_food"),
                    Compat.translatable("smart-autoclicker.toast.out_of_food.desc"), true);
        }
    }

    @Override
    public @Nullable Component status() {
        if (isBusy()) return Compat.translatable("smart-autoclicker.status.eating");
        return Compat.translatable("smart-autoclicker.status.food_left", foodLeft);
    }
}
