package com.simplyag.autoclicker.gametest;

import com.simplyag.autoclicker.util.Compat;
import com.mojang.blaze3d.platform.InputConstants;
import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.module.ModuleManager;
import com.simplyag.autoclicker.ui.ControlPanelScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
//? if >=26.2 {
import net.minecraft.world.entity.EntityTypes;
//?} else
/*import net.minecraft.world.entity.EntityType;*/
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/**
 * Plays through each feature in a real singleplayer world and fails loudly if one doesn't work.
 * Screenshots land in the run's screenshots folder for a visual check of the HUD, toasts and control panel.
 * Runs through Fabric's client game tests (AutoClickerClientGameTest) or the mod's own harness, see {@link TestClient}.
 */
public final class SmartTests {
    private SmartTests() {
    }

    /**
     * Stay out of the way of whatever else is going on at this computer: no sound, no window on top. Call before the
     * test world is created.
     */
    public static void prepare(TestClient context) {
        context.runOnClient(mc -> {
            // Less work for slow machines (CI runners render in software): the lowest simulation distance, and a
            // render distance that still keeps chunks ahead of the elytra flight loaded.
            //? if >=1.19.3 {
            mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
            //?} else
            /*mc.options.setSoundCategoryVolume(SoundSource.MASTER, 0.0F);*/
            //? if >=1.19 {
            mc.options.simulationDistance().set(5);
            mc.options.renderDistance().set(4);
            //?} else
            /*mc.options.renderDistance = 4;*/
            //? if >=1.18 <1.19
            /*mc.options.simulationDistance = 5;*/
            Compat.minimizeWindow(mc);
        });
    }

    /** Every test, in a freshly created flat world. */
    public static void run(TestClient context, TestClient.Server server) {
        server.runCommand("difficulty easy");
        server.runCommand("gamemode survival @a");
        server.runCommand("gamerule doDaylightCycle false");
        server.runCommand("time set noon");
        server.runCommand("execute as @p at @p run tp @p ~ ~ ~ 0 0");
        context.waitTicks(20);
        // Where the tests stand: the flat world's floor, or the harness's sky arena on older versions. Flights end here.
        start = context.computeOnClient(mc -> new double[]{mc.player.getX(), mc.player.getY(), mc.player.getZ()});

        testAutoAttack(context, server);
        testProtectsNamedMobs(context, server);
        testArmorStands(context, server);
        testAutoEatFromHotbar(context, server);
        testAutoEatRestocksFromInventory(context, server);
        testHoldRightClickEats(context, server);
        testElytraCruise(context, server);
        testMobSweep(context);
        testConflicts(context);
        testAutoWalkCrouch(context, server);
        if (FabricLoader.getInstance().isModLoaded("freecam")) testFreecam(context, server);
        testControlPanel(context);
        testControlPanelClick(context);
    }
    /**
     * Runs only when the Freecam jar is in the test run's mods folder. The camera looks the other way
     * throughout, so everything here has to work from the player's own facing.
     */
    private static void testFreecam(TestClient context, TestClient.Server server) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p hotbar.0 with diamond_sword");
        server.runCommand("execute as @p at @p run tp @p ~ ~ ~ 0 0");
        context.waitTicks(10);
        toggleFreecam(context);
        context.runOnClient(mc -> mc.getCameraEntity().setYRot(180));
        check(context.computeOnClient(mc -> mc.getCameraEntity() != mc.player), "Freecam should detach the camera");

        // Auto Attack hits what the player faces, not what the camera sees
        server.runCommand("execute as @p at @p run summon husk ^ ^ ^2 {NoAI:1b}");
        context.waitTicks(5);
        context.runOnClient(mc -> ModuleManager.AUTO_ATTACK.start(true));
        context.waitTicks(20 * 6);
        check(server.computeOnServer(SmartTests::countHusks) == 0, "Auto Attack should kill a husk with Freecam on");
        context.takeScreenshot("sac_freecam_attack");

        // Sweep turns the player and leaves the camera alone
        float startYaw = context.computeOnClient(mc -> mc.player.getYRot());
        float cameraYaw = context.computeOnClient(mc -> mc.getCameraEntity().getYRot());
        context.runOnClient(mc -> ModuleManager.MOB_SWEEP.start(true));
        float max = 0;
        for (int i = 0; i < SmartConfig.get().sweepPeriodTenths * 2 + 10; i++) {
            context.waitTick();
            max = Math.max(max, Math.abs(context.computeOnClient(mc -> mc.player.getYRot()) - startYaw));
        }
        check(max > SmartConfig.get().sweepWidthDeg / 2f - 3, "Mob Sweep should turn the player with Freecam on, but only went " + max + "°");
        float cameraTurn = Math.abs(context.computeOnClient(mc -> mc.getCameraEntity().getYRot()) - cameraYaw);
        check(cameraTurn < 1, "Mob Sweep shouldn't turn the camera, but it moved " + cameraTurn + "°");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
        toggleFreecam(context);

        // Elytra Cruise carries the camera along
        server.runCommand("item replace entity @p armor.chest with elytra");
        server.runCommand("item replace entity @p inventory.5 with firework_rocket 32");
        server.runCommand("execute as @p at @p run tp @p ~ ~60 ~ 0 0");
        context.waitTicks(8);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(3);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitFor(mc -> mc.player.isFallFlying(), 40);
        context.runOnClient(mc -> ModuleManager.ELYTRA_CRUISE.start(true));
        toggleFreecam(context);
        context.waitTicks(2);
        double[] before = context.computeOnClient(mc -> offset(mc.player, mc.getCameraEntity()));
        double startZ = context.computeOnClient(mc -> mc.player.getZ());
        context.waitTicks(60);
        double[] after = context.computeOnClient(mc -> offset(mc.player, mc.getCameraEntity()));
        double flown = Math.abs(context.computeOnClient(mc -> mc.player.getZ()) - startZ);
        boolean stillFlying = context.computeOnClient(mc -> mc.player.isFallFlying());
        context.takeScreenshot("sac_freecam_elytra");
        check(stillFlying && flown > 20, "Should keep flying with Freecam on, but flew " + flown + " blocks");
        double drift = Math.hypot(after[0] - before[0], after[1] - before[1]);
        check(drift < 3, "The camera should stay with the player, but drifted " + drift + " blocks");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
        toggleFreecam(context);
        server.runCommand(backToStart());
        server.runCommand("clear @p");
        context.waitTicks(10);
    }

    private static double[] offset(Entity player, Entity camera) {
        return new double[]{camera.getX() - player.getX(), camera.getZ() - player.getZ()};
    }

    private static void toggleFreecam(TestClient context) {
        context.runOnClient(mc -> {
            try {
                Class.forName("net.xolt.freecam.Freecam").getMethod("toggle").invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Couldn't toggle Freecam", e);
            }
        });
        context.waitTicks(2);
    }

    /** Crouch and sprint in Auto Walk, switching mid-walk, in both hold and Toggle Sneak modes. */
    private static void testAutoWalkCrouch(TestClient context, TestClient.Server server) {
        setHunger(server, 20);
        SmartConfig cfg = SmartConfig.get();
        cfg.walkSprint = true;
        cfg.walkCrouch = false;
        context.runOnClient(mc -> ModuleManager.AUTO_WALK.start(false));
        context.waitTicks(10);
        check(context.computeOnClient(mc -> mc.player.isSprinting()), "Auto Walk should sprint");

        cfg.walkSprint = false;
        cfg.walkCrouch = true;
        context.waitTicks(10);
        check(context.computeOnClient(mc -> mc.player.isCrouching()), "Auto Walk should crouch");
        check(!context.computeOnClient(mc -> mc.player.isSprinting()), "Crouching should end the sprint");
        context.takeScreenshot("sac_walk_crouch");

        context.runOnClient(mc -> ModuleManager.AUTO_WALK.stop(false));
        context.waitTicks(5);
        check(!context.computeOnClient(mc -> mc.player.isCrouching()), "Stopping Auto Walk should stand back up");

        context.runOnClient(mc -> setToggleCrouch(mc, true));
        context.runOnClient(mc -> ModuleManager.AUTO_WALK.start(false));
        boolean steady = true;
        context.waitTicks(3);
        for (int i = 0; i < 10; i++) {
            context.waitTick();
            steady &= context.computeOnClient(mc -> mc.options.keyShift.isDown());
        }
        check(steady, "Auto Walk's crouch shouldn't flicker with Toggle Sneak on");
        context.runOnClient(mc -> ModuleManager.AUTO_WALK.stop(false));
        context.waitTicks(5);
        check(!context.computeOnClient(mc -> mc.options.keyShift.isDown()), "Stopping Auto Walk should release a toggled crouch");

        context.runOnClient(mc -> setToggleCrouch(mc, false));
        cfg.walkCrouch = false;
        server.runCommand("execute as @p at @p run tp @p ~ ~ ~ 0 0");
        context.waitTicks(5);
    }

    /** Modules that can't work together swap each other out, and Mob Sweep brings Auto Attack. */
    private static void testConflicts(TestClient context) {
        context.runOnClient(mc -> {
            ModuleManager.AUTO_ATTACK.start(true);
            ModuleManager.AUTO_EAT.start(true);
            ModuleManager.HOLD_ATTACK.start(true);
            check(!ModuleManager.AUTO_ATTACK.isRunning(), "Hold Left Click should stop Auto Attack");
            check(ModuleManager.AUTO_EAT.isRunning(), "Hold Left Click should leave Auto Eat running");
            check(ModuleManager.session() != null, "Swapping modules shouldn't end the session");

            ModuleManager.MOB_SWEEP.start(true);
            check(ModuleManager.AUTO_ATTACK.isRunning(), "Mob Sweep should start Auto Attack");
            check(!ModuleManager.HOLD_ATTACK.isRunning(), "Mob Sweep should stop Hold Left Click");

            ModuleManager.AUTO_ATTACK.stop(true);
            check(!ModuleManager.MOB_SWEEP.isRunning(), "Stopping Auto Attack should stop Mob Sweep");

            ModuleManager.HOLD_USE.start(true);
            check(!ModuleManager.AUTO_EAT.isRunning(), "Hold Right Click should stop Auto Eat");
            ModuleManager.stopAll(null);

            check(ModuleManager.normalize(List.of("hold_use", "auto_eat", "auto_attack")).equals(List.of("hold_use")),
                    "A preset can't keep modules that conflict with Hold Right Click");
            check(ModuleManager.normalize(List.of("mob_sweep", "hold_attack", "auto_eat")).equals(List.of("mob_sweep", "auto_eat", "auto_attack")),
                    "A preset with Mob Sweep should drop Hold Left Click and gain Auto Attack");
            check(ModuleManager.normalize(List.of("hold_attack", "mob_sweep")).equals(List.of("hold_attack")),
                    "Mob Sweep can't stay when Auto Attack can't join");
        });
        context.waitTick();
    }

    private static void testMobSweep(TestClient context) {
        float startYaw = context.computeOnClient(mc -> mc.player.getYRot());
        int width = SmartConfig.get().sweepWidthDeg;
        context.runOnClient(mc -> ModuleManager.MOB_SWEEP.start(true));
        float min = 0;
        float max = 0;
        // One full cycle plus a bit
        for (int i = 0; i < SmartConfig.get().sweepPeriodTenths * 2 + 10; i++) {
            context.waitTick();
            float offset = context.computeOnClient(mc -> mc.player.getYRot()) - startYaw;
            min = Math.min(min, offset);
            max = Math.max(max, offset);
            if (i == 15) context.takeScreenshot("sac_mob_sweep");
        }
        check(max > width / 2f - 3 && min < -width / 2f + 3,
                "Mob Sweep should turn about " + width / 2 + "° each way, but went " + min + "° to " + max + "°");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
        context.waitTick();
        float endYaw = context.computeOnClient(mc -> mc.player.getYRot());
        check(Math.abs(endYaw - startYaw) < 1f, "Mob Sweep should turn back to where it started (off by " + (endYaw - startYaw) + "°)");
    }

    /** Clicks the big Auto Attack switch with the real mouse, using the panel's layout. */
    private static void testControlPanelClick(TestClient context) {
        context.setScreen(() -> new ControlPanelScreen(null));
        context.waitForScreen(ControlPanelScreen.class);
        context.getInput().pressKey(InputConstants.KEY_DOWN); // Presets -> Auto Attack
        context.waitTicks(10); // let the open animation finish

        double[] point = context.computeOnClient(mc -> {
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            int w = Math.min(480, width - 20);
            int h = Math.min(280, height - 16);
            int x = (width - w) / 2;
            int y = (height - h) / 2;
            int contentW = w - 124 - 2 - 10 * 2 - 4;
            int switchX = x + 124 + 1 + 10 + 2 + contentW - 34 + 17;
            int switchY = y + 26 + 1 + 10 + 8 + 8;
            double scale = mc.getWindow().getGuiScale();
            return new double[]{switchX * scale, switchY * scale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(0);
        context.waitTicks(5);
        check(context.computeOnClient(mc -> ModuleManager.AUTO_ATTACK.isRunning()), "Clicking the switch should start Auto Attack");
        context.takeScreenshot("sac_panel_switch_on");
        context.getInput().pressMouse(0);
        context.waitTicks(5);
        check(!context.computeOnClient(mc -> ModuleManager.AUTO_ATTACK.isRunning()), "Clicking again should stop Auto Attack");
        context.setScreen(() -> null);
    }

    private static void testAutoAttack(TestClient context, TestClient.Server server) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p hotbar.0 with diamond_sword");
        server.runCommand("execute as @p at @p run summon husk ^ ^ ^2 {NoAI:1b}");
        context.waitTicks(10);

        context.runOnClient(mc -> {
            Compat.selectSlot(mc.player.getInventory(), 0);
            ModuleManager.AUTO_ATTACK.start(true);
        });
        context.waitTicks(15);
        context.takeScreenshot("sac_auto_attack_running");

        // Polled here because TestClient.Server.waitFor only exists in newer Fabric API versions
        for (int t = 0; t < 20 * 15 && server.computeOnServer(SmartTests::countHusks) > 0; t++) context.waitTick();
        int attacks = context.computeOnClient(mc -> ModuleManager.session().attacks);
        check(attacks >= 2, "Auto Attack: expected several swings, got " + attacks);
        context.runOnClient(mc -> ModuleManager.stopAllByUser());
        context.waitTicks(5);
        context.takeScreenshot("sac_stop_all_toast");
    }

    /** Armor stands are off limits until the option is on, and a name does not protect them then. */
    private static void testArmorStands(TestClient context, TestClient.Server server) {
        server.runCommand("execute as @p at @p run summon armor_stand ^ ^ ^2 {NoGravity:1b,CustomName:" + customName("Target") + "}");
        context.waitTicks(10);
        context.runOnClient(mc -> ModuleManager.AUTO_ATTACK.start(false));
        context.waitTicks(40);
        check(context.computeOnClient(mc -> ModuleManager.session().attacks) == 0, "Auto Attack should leave armor stands alone by default");
        SmartConfig.get().attackArmorStands = true;
        context.waitTicks(40);
        check(context.computeOnClient(mc -> ModuleManager.session().attacks) > 0, "Auto Attack should hit an armor stand with the option on");
        SmartConfig.get().attackArmorStands = false;
        context.runOnClient(mc -> ModuleManager.stopAll(null));
        server.runCommand("kill @e[type=armor_stand]");
        context.waitTicks(5);
    }

    private static void testProtectsNamedMobs(TestClient context, TestClient.Server server) {
        server.runCommand("execute as @p at @p run summon husk ^ ^ ^2 {NoAI:1b,CustomName:" + customName("Bob") + "}");
        context.waitTicks(10);
        context.runOnClient(mc -> ModuleManager.AUTO_ATTACK.start(false));
        context.waitTicks(60);
        int attacks = context.computeOnClient(mc -> ModuleManager.session().attacks);
        check(attacks == 0, "Auto Attack hit a name-tagged mob (" + attacks + " swings)");
        check(server.computeOnServer(SmartTests::countHusks) == 1, "Name-tagged husk should still be alive");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
        server.runCommand("kill @e[type=husk]");
    }

    private static void testAutoEatFromHotbar(TestClient context, TestClient.Server server) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p hotbar.0 with diamond_sword");
        server.runCommand("item replace entity @p hotbar.4 with cooked_beef 16");
        setHunger(server, 6);
        context.waitTicks(5);

        context.runOnClient(mc -> {
            Compat.selectSlot(mc.player.getInventory(), 0);
            ModuleManager.AUTO_EAT.start(true);
        });
        context.waitFor(mc -> mc.player.isUsingItem(), 20 * 5);
        context.takeScreenshot("sac_auto_eat_eating");
        context.waitFor(mc -> mc.player.getFoodData().getFoodLevel() > 6 && !ModuleManager.AUTO_EAT.isBusy(), 20 * 10);

        int selected = context.computeOnClient(mc -> Compat.selectedSlot(mc.player.getInventory()));
        check(selected == 0, "Auto Eat should switch back to the sword in slot 1, but slot " + (selected + 1) + " is selected");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
    }

    private static void testAutoEatRestocksFromInventory(TestClient context, TestClient.Server server) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p hotbar.0 with diamond_sword");
        server.runCommand("item replace entity @p inventory.10 with bread 8");
        setHunger(server, 6);
        context.waitTicks(5);

        context.runOnClient(mc -> {
            Compat.selectSlot(mc.player.getInventory(), 0);
            ModuleManager.AUTO_EAT.start(false);
        });
        context.waitFor(mc -> mc.player.getFoodData().getFoodLevel() > 6 && !ModuleManager.AUTO_EAT.isBusy(), 20 * 10);
        context.runOnClient(mc -> ModuleManager.stopAll(null));
    }

    private static void testHoldRightClickEats(TestClient context, TestClient.Server server) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p hotbar.0 with golden_carrot 16");
        setHunger(server, 4);
        context.waitTicks(5);

        context.runOnClient(mc -> {
            Compat.selectSlot(mc.player.getInventory(), 0);
            ModuleManager.HOLD_USE.start(true);
        });
        context.waitFor(mc -> mc.player.getFoodData().getFoodLevel() >= 10, 20 * 10);
        context.takeScreenshot("sac_hold_right_click");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
        context.waitTicks(5);
        boolean stillDown = context.computeOnClient(mc -> mc.options.keyUse.isDown());
        check(!stillDown, "Right click should be released after stopping");
    }

    private static void testElytraCruise(TestClient context, TestClient.Server server) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p armor.chest with elytra");
        server.runCommand("item replace entity @p hotbar.0 with diamond_sword");
        server.runCommand("item replace entity @p inventory.5 with firework_rocket 32");
        server.runCommand("execute as @p at @p run tp @p ~ ~60 ~ 0 0");
        int savedDistance = SmartConfig.get().elytraTargetDistance;
        SmartConfig.get().elytraTargetDistance = 300;
        context.waitTicks(8);

        // Jump while falling to open the elytra, exactly like a player would
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(3);
        context.getInput().releaseKey(options -> options.keyJump);
        context.takeScreenshot("sac_elytra_takeoff");
        context.waitFor(mc -> mc.player.isFallFlying(), 40);
        context.runOnClient(mc -> {
            Compat.selectSlot(mc.player.getInventory(), 0);
            ModuleManager.ELYTRA_CRUISE.start(true);
        });
        context.waitTicks(60);
        context.takeScreenshot("sac_elytra_cruise");

        context.waitFor(mc -> !ModuleManager.ELYTRA_CRUISE.isRunning(), 20 * 40);
        SmartConfig.get().elytraTargetDistance = savedDistance;
        int rockets = context.computeOnClient(mc -> 32 - mc.player.getInventory().countItem(Items.FIREWORK_ROCKET));
        int selected = context.computeOnClient(mc -> Compat.selectedSlot(mc.player.getInventory()));
        check(rockets >= 1, "Elytra Cruise never fired a rocket");
        check(selected == 0, "Elytra Cruise should switch back to slot 1 after firing, but slot " + (selected + 1) + " is selected");
        context.takeScreenshot("sac_elytra_arrived");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
        server.runCommand(backToStart());
        server.runCommand("clear @p");
        context.waitTicks(20);
    }

    private static void testControlPanel(TestClient context) {
        context.runOnClient(mc -> {
            ModuleManager.AUTO_ATTACK.start(false);
            ModuleManager.AUTO_EAT.start(false);
        });
        context.setScreen(() -> new ControlPanelScreen(null));
        context.waitForScreen(ControlPanelScreen.class);
        context.waitTicks(6);
        // Every page, via the arrow keys: presets, 7 modules, safety, general (ends back on presets)
        String[] pages = {"presets", "auto_attack", "auto_eat", "hold_left", "hold_right", "auto_walk", "mob_sweep",
                "elytra", "safety", "general"};
        for (String page : pages) {
            context.waitTicks(3);
            context.takeScreenshot("sac_panel_" + page);
            context.getInput().pressKey(InputConstants.KEY_DOWN);
        }
        context.setScreen(() -> null);
        context.waitTicks(10);
        context.takeScreenshot("sac_hud");
        context.runOnClient(mc -> ModuleManager.stopAll(null));
    }

    private static void setHunger(TestClient.Server server, int food) {
        server.runOnServer(s -> {
            ServerPlayer player = s.getPlayerList().getPlayers().get(0);
            player.getFoodData().setFoodLevel(food);
            player.getFoodData().setSaturation(0);
        });
    }

    private static int countHusks(MinecraftServer server) {
        //? if >=26.2 {
        return server.overworld().getEntities(EntityTypes.HUSK, e -> e.isAlive()).size();
        //?} else
        /*return server.overworld().getEntities(EntityType.HUSK, e -> e.isAlive()).size();*/
    }

    /** Where each test run stands; set at the start of {@link #run}. */
    private static double[] start = new double[3];

    /** Teleports the player back to where the tests started (after a flight). */
    private static String backToStart() {
        return String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f", start[0], start[1], start[2]);
    }

    private static void setToggleCrouch(Minecraft mc, boolean toggle) {
        //? if >=1.19 {
        mc.options.toggleCrouch().set(toggle);
        //?} else
        /*mc.options.toggleCrouch = toggle;*/
    }

    /** A CustomName value for /summon: a plain string from 1.20.3, a JSON text component in quotes before. */
    private static String customName(String name) {
        //? if >=1.20.3 {
        return "\"" + name + "\"";
        //?} else
        /*return "'\"" + name + "\"'";*/
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
