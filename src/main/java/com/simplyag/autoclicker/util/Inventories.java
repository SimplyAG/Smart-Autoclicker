package com.simplyag.autoclicker.util;

import com.google.common.collect.ImmutableSet;
import com.simplyag.autoclicker.config.SmartConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.function.Predicate;

public final class Inventories {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 36;

    /** Foods that poison, hurt, starve or teleport you. Never auto-eaten. */
    private static final Set<Item> HARMFUL_FOOD = ImmutableSet.of(
            Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.PUFFERFISH,
            Items.CHICKEN, Items.SUSPICIOUS_STEW, Items.CHORUS_FRUIT);
    private static final Set<Item> GOLDEN_FOOD = ImmutableSet.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);

    private Inventories() {
    }

    /** How good a food is to auto-eat; negative if it shouldn't be eaten at all. */
    public static float foodScore(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        FoodProperties food = Compat.food(stack);
        if (food == null || HARMFUL_FOOD.contains(stack.getItem())) return -1;
        if (GOLDEN_FOOD.contains(stack.getItem()) && !SmartConfig.get().allowGoldenFood) return -1;
        return Compat.nutrition(food) + Compat.saturation(food);
    }

    public static int nutrition(ItemStack stack) {
        FoodProperties food = Compat.food(stack);
        return food == null ? 0 : Compat.nutrition(food);
    }

    /** Best food slot in the hotbar (0-8), or -1. */
    public static int bestHotbarFood(LocalPlayer player) {
        return bestSlot(player, 0, HOTBAR_SIZE);
    }

    /** Best food slot in the main inventory above the hotbar (9-35), or -1. */
    public static int bestInventoryFood(LocalPlayer player) {
        return bestSlot(player, HOTBAR_SIZE, MAIN_SIZE);
    }

    private static int bestSlot(LocalPlayer player, int from, int to) {
        Inventory inventory = Compat.inventory(player);
        int best = -1;
        float bestScore = 0;
        for (int slot = from; slot < to; slot++) {
            float score = foodScore(inventory.getItem(slot));
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    public static int countFood(LocalPlayer player) {
        int count = count(player, stack -> foodScore(stack) > 0);
        if (foodScore(player.getOffhandItem()) > 0) count += player.getOffhandItem().getCount();
        return count;
    }

    public static int count(LocalPlayer player, Predicate<ItemStack> filter) {
        Inventory inventory = Compat.inventory(player);
        int total = 0;
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && filter.test(stack)) total += stack.getCount();
        }
        return total;
    }

    /** First hotbar slot holding this item, or -1. */
    public static int findHotbar(LocalPlayer player, Item item) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            if (Compat.inventory(player).getItem(slot).getItem() == item) return slot;
        }
        return -1;
    }

    /** First main-inventory slot (9-35) holding this item, or -1. */
    public static int findInventory(LocalPlayer player, Item item) {
        for (int slot = HOTBAR_SIZE; slot < MAIN_SIZE; slot++) {
            if (Compat.inventory(player).getItem(slot).getItem() == item) return slot;
        }
        return -1;
    }

    /**
     * Picks a hotbar slot to move an item into: an empty one if there is one,
     * otherwise the last slot that isn't currently selected (so the weapon in hand stays put).
     */
    public static int spareHotbarSlot(LocalPlayer player) {
        Inventory inventory = Compat.inventory(player);
        int selected = Compat.selectedSlot(inventory);
        for (int slot = HOTBAR_SIZE - 1; slot >= 0; slot--) {
            if (slot != selected && inventory.getItem(slot).isEmpty()) return slot;
        }
        return selected == HOTBAR_SIZE - 1 ? HOTBAR_SIZE - 2 : HOTBAR_SIZE - 1;
    }

    /**
     * Swaps a main-inventory slot (9-35) with a hotbar slot, like hovering it and pressing a number key.
     * Only works while no other container is open.
     */
    public static boolean swapIntoHotbar(LocalPlayer player, int inventorySlot, int hotbarSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || player.containerMenu != player.inventoryMenu) return false;
        // In the player's own inventory menu, main inventory slots 9-35 keep the same index.
        Compat.swapWithHotbar(mc, player, inventorySlot, hotbarSlot);
        return true;
    }

    public static boolean isFull(LocalPlayer player) {
        return Compat.inventory(player).getFreeSlot() == -1;
    }

    /** Durability left before the item breaks, or -1 if it can't break. */
    public static int durabilityLeft(ItemStack stack) {
        if (!stack.isDamageableItem()) return -1;
        return stack.getMaxDamage() - stack.getDamageValue();
    }
}
