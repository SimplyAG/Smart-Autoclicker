package com.simplyag.autoclicker.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplyag.autoclicker.AutoClickerClient;
import com.simplyag.autoclicker.ui.Gfx;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
//? if >=1.20.5
import net.minecraft.core.component.DataComponents;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
//? if >=1.17 {
import net.minecraft.world.entity.OwnableEntity;
//?} else {
/*import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
*///?}
//? if >=1.16 <1.17
/*import net.minecraft.network.chat.TextColor;*/
//? if <1.16
/*import net.minecraft.ChatFormatting;*/
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
//? if >=26.3 {
import org.lwjgl.sdl.SDLVideo;
//?} else {
/*import org.lwjgl.glfw.GLFW;
*///?}
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.world.inventory.ClickType;
*///?}
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;*/

/**
 * Every Minecraft API that differs between the supported versions, in one place. The rest of the mod calls these
 * instead of the game directly, so a new Minecraft version usually means changes here only. Drawing goes through
 * {@link Gfx}, which plays the same role for the GUI graphics classes.
 * Branches are Stonecutter conditions: each version project compiles only the branch that matches it.
 */
public final class Compat {
    /** The keyboard key type: KEYSYM until 26.2, KEYBOARD from 26.3 (SDL input). */
    //? if >=26.3 {
    public static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYBOARD;
    //?} else
    /*public static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYSYM;*/

    /** Key category: a registered object from 1.21.9, a translation key before. Both show as "Smart Autoclicker". */
    //? if >=1.21.9 {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AutoClickerClient.id("main"));
    //?} else
    /*private static final String CATEGORY = "key.category." + AutoClickerClient.MOD_ID + ".main";*/

    private Compat() {
    }

    /** The selected hotbar slot (0-8). */
    public static int selectedSlot(Inventory inventory) {
        //? if >=1.21.5 {
        return inventory.getSelectedSlot();
        //?} else
        /*return inventory.selected;*/
    }

    public static void selectSlot(Inventory inventory, int slot) {
        //? if >=1.21.5 {
        inventory.setSelectedSlot(slot);
        //?} else
        /*inventory.selected = slot;*/
    }

    /** Whether a mob is someone's pet (a tamed wolf, cat, parrot, horse...). */
    public static boolean isOwnedPet(Entity entity) {
        //? if >=1.21.5 {
        return entity instanceof OwnableEntity && ((OwnableEntity) entity).getOwnerReference() != null;
        //?} else if >=1.17 {
        /*return entity instanceof OwnableEntity && ((OwnableEntity) entity).getOwnerUUID() != null;
        *///?} else {
        /*return entity instanceof TamableAnimal && ((TamableAnimal) entity).getOwnerUUID() != null
                || entity instanceof AbstractHorse && ((AbstractHorse) entity).getOwnerUUID() != null;
        *///?}
    }

    // Keyboard keys the mod binds or checks. InputConstants has them from 1.17 (SDL codes from 26.3); before, the
    // same GLFW key codes.
    //? if >=1.17 {
    public static final int KEY_K = InputConstants.KEY_K;
    public static final int KEY_Z = InputConstants.KEY_Z;
    public static final int KEY_H = InputConstants.KEY_H;
    public static final int KEY_ESCAPE = InputConstants.KEY_ESCAPE;
    public static final int KEY_UP = InputConstants.KEY_UP;
    public static final int KEY_DOWN = InputConstants.KEY_DOWN;
    //?} else {
    /*public static final int KEY_K = 75;
    public static final int KEY_Z = 90;
    public static final int KEY_H = 72;
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_UP = 265;
    public static final int KEY_DOWN = 264;
    *///?}

    // Rotation, inventory and abilities became getters in 1.17; before they were public fields.
    public static float xRot(Entity entity) {
        //? if >=1.17 {
        return entity.getXRot();
        //?} else
        /*return entity.xRot;*/
    }

    public static float yRot(Entity entity) {
        //? if >=1.17 {
        return entity.getYRot();
        //?} else
        /*return entity.yRot;*/
    }

    public static void setXRot(Entity entity, float xRot) {
        //? if >=1.17 {
        entity.setXRot(xRot);
        //?} else
        /*entity.xRot = xRot;*/
    }

    public static void setYRot(Entity entity, float yRot) {
        //? if >=1.17 {
        entity.setYRot(yRot);
        //?} else
        /*entity.yRot = yRot;*/
    }

    public static Inventory inventory(Player player) {
        //? if >=1.17 {
        return player.getInventory();
        //?} else
        /*return player.inventory;*/
    }

    /** Where the entity's eyes are this tick. */
    public static Vec3 eyePosition(Entity entity) {
        //? if >=1.17 {
        return entity.getEyePosition();
        //?} else
        /*return entity.getEyePosition(1.0F);*/
    }

    private static boolean instabuild(Player player) {
        //? if >=1.17 {
        return player.getAbilities().instabuild;
        //?} else
        /*return player.abilities.instabuild;*/
    }

    /** Translated text. Component.translatable/literal/empty exist from 1.19; before it they were classes. */
    public static MutableComponent translatable(String key, Object... args) {
        //? if >=1.19 {
        return Component.translatable(key, args);
        //?} else
        /*return new TranslatableComponent(key, args);*/
    }

    public static MutableComponent literal(String text) {
        //? if >=1.19 {
        return Component.literal(text);
        //?} else
        /*return new TextComponent(text);*/
    }

    public static MutableComponent empty() {
        //? if >=1.19 {
        return Component.empty();
        //?} else
        /*return new TextComponent("");*/
    }

    /** Uses the item in a hand, as a right click does. */
    public static void useItem(Minecraft mc, LocalPlayer player, InteractionHand hand) {
        //? if >=1.19 {
        mc.gameMode.useItem(player, hand);
        //?} else
        /*mc.gameMode.useItem(player, mc.level, hand);*/
    }

    /** Whether the player set Sneak to toggle instead of hold. */
    public static boolean toggleCrouch(Options options) {
        //? if >=1.19 {
        return options.toggleCrouch().get();
        //?} else
        /*return options.toggleCrouch;*/
    }

    /** The low and high note-block sounds the alerts use. */
    public static SoundEvent alertSound(boolean bad) {
        //? if >=1.19.3 {
        return bad ? SoundEvents.NOTE_BLOCK_BASS.value() : SoundEvents.NOTE_BLOCK_CHIME.value();
        //?} else
        /*return bad ? SoundEvents.NOTE_BLOCK_BASS : SoundEvents.NOTE_BLOCK_CHIME;*/
    }

    /** How far away the player can hit an entity. */
    public static double entityReach(LocalPlayer player) {
        //? if >=1.20.5 {
        return player.entityInteractionRange();
        //?} else
        /*return instabuild(player) ? 6.0 : 3.0;*/
    }

    /** Creative mode: no hunger, no item use-up. */
    public static boolean hasInfiniteMaterials(LocalPlayer player) {
        //? if >=1.20.5 {
        return player.hasInfiniteMaterials();
        //?} else
        /*return instabuild(player);*/
    }

    /** Colours a text component with an ARGB/RGB colour. */
    public static MutableComponent withColor(MutableComponent text, int color) {
        //? if >=1.20.3 {
        return text.withColor(color);
        //?} else if >=1.17 {
        /*return text.withStyle(style -> style.withColor(color & 0xFFFFFF));
        *///?} else if >=1.16 {
        /*return text.withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        *///?} else
        /*return text.withStyle(ChatFormatting.GRAY);*/
    }

    /** A food item's properties, or null if it isn't food. */
    public static @Nullable FoodProperties food(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.get(DataComponents.FOOD);
        //?} else
        /*return stack.getItem().getFoodProperties();*/
    }

    /** Hunger points a food restores. */
    public static int nutrition(FoodProperties food) {
        //? if >=1.20.5 {
        return food.nutrition();
        //?} else
        /*return food.getNutrition();*/
    }

    /** Saturation points a food restores. */
    public static float saturation(FoodProperties food) {
        //? if >=1.20.5 {
        return food.saturation();
        //?} else
        /*return food.getNutrition() * food.getSaturationModifier() * 2.0F;*/
    }

    /** Whether a chest-slot item lets you glide: any glider from 1.21.2, only the elytra before. */
    public static boolean isGlider(ItemStack stack) {
        //? if >=1.21.2 {
        return stack.has(DataComponents.GLIDER);
        //?} else
        /*return stack.getItem() == Items.ELYTRA;*/
    }

    public static @Nullable Screen screen(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.screen();
        //?} else
        /*return mc.screen;*/
    }

    public static void setScreen(Minecraft mc, @Nullable Screen screen) {
        //? if >=26.2 {
        mc.gui.setScreen(screen);
        //?} else
        /*mc.setScreen(screen);*/
    }

    /** True while the HUD is hidden with F1. */
    public static boolean hudHidden(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.hud.isHidden();
        //?} else
        /*return mc.options.hideGui;*/
    }

    /** Registers a key mapping in the mod's own category of the Controls screen. */
    public static KeyMapping registerKey(String name, int defaultKey) {
        KeyMapping key = new KeyMapping("key." + AutoClickerClient.MOD_ID + "." + name, KEYBOARD, defaultKey, CATEGORY);
        //? if >=26.1 {
        return KeyMappingHelper.registerKeyMapping(key);
        //?} else
        /*return KeyBindingHelper.registerKeyBinding(key);*/
    }

    /** The key a mapping is bound to right now (after the player rebinds it). */
    public static InputConstants.Key boundKey(KeyMapping key) {
        //? if >=26.1 {
        return KeyMappingHelper.getBoundKeyOf(key);
        //?} else
        /*return KeyBindingHelper.getBoundKeyOf(key);*/
    }

    /** Draws something on the HUD every frame, above the vanilla HUD. */
    public static void registerHud(String name, Consumer<Gfx> draw) {
        //? if >=1.21.6 {
        HudElementRegistry.addLast(AutoClickerClient.id(name), (graphics, deltaTracker) -> draw.accept(new Gfx(graphics)));
        //?} else
        /*HudRenderCallback.EVENT.register((graphics, tickDelta) -> draw.accept(new Gfx(graphics)));*/
    }

    /** Whether the game window has focus (the player isn't tabbed out). */
    public static boolean windowFocused(Minecraft mc) {
        return mc.isWindowActive();
    }

    /** Whether a keyboard key is physically held down right now. */
    public static boolean isKeyDown(Minecraft mc, int key) {
        //? if >=26.3 {
        return InputConstants.isKeyDown(key);
        //?} else if >=1.21.9 {
        /*return InputConstants.isKeyDown(mc.getWindow(), key);
        *///?} else
        /*return InputConstants.isKeyDown(mc.getWindow().getWindow(), key);*/
    }

    /** Swaps an inventory slot with a hotbar slot, as pressing a number key over it in the inventory does. */
    public static void swapWithHotbar(Minecraft mc, LocalPlayer player, int inventorySlot, int hotbarSlot) {
        //? if >=26.1 {
        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, inventorySlot, hotbarSlot, ContainerInput.SWAP, player);
        //?} else
        /*mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, inventorySlot, hotbarSlot, ClickType.SWAP, player);*/
    }

    /** Plays the main-hand swing, as a real attack does. */
    public static void swingMainHand(LocalPlayer player) {
        //? if >=26.3 {
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
        //?} else
        /*player.swing(InteractionHand.MAIN_HAND);*/
    }

    /**
     * The direction a pitch/yaw looks in. Computed here rather than with Entity.calculateViewVector, which changed
     * from an instance to a static method and also follows a Freecam camera on some versions.
     */
    public static Vec3 viewVector(float xRot, float yRot) {
        float pitch = xRot * ((float) (Math.PI / 180));
        float yaw = -yRot * ((float) (Math.PI / 180));
        float horizontal = Mth.cos(pitch);
        return new Vec3(Mth.sin(yaw) * horizontal, -Mth.sin(pitch), Mth.cos(yaw) * horizontal);
    }

    /** Flashes the game's taskbar button until the window is focused. */
    public static void flashWindow(Minecraft mc) {
        //? if >=26.3 {
        SDLVideo.SDL_FlashWindow(mc.getWindow().handle(), SDLVideo.SDL_FLASH_UNTIL_FOCUSED);
        //?} else
        /*GLFW.glfwRequestWindowAttention(windowHandle(mc));*/
    }

    /** Minimizes the game window (used by the in-game tests to stay out of the way). */
    public static void minimizeWindow(Minecraft mc) {
        //? if >=26.3 {
        SDLVideo.SDL_MinimizeWindow(mc.getWindow().handle());
        //?} else
        /*GLFW.glfwIconifyWindow(windowHandle(mc));*/
    }

    private static long windowHandle(Minecraft mc) {
        //? if >=1.21.9 {
        return mc.getWindow().handle();
        //?} else
        /*return mc.getWindow().getWindow();*/
    }
}
