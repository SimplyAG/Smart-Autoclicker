package com.simplyag.autoclicker.gametest.harness;

import com.simplyag.autoclicker.util.Compat;
import com.mojang.blaze3d.platform.InputConstants;
import com.simplyag.autoclicker.gametest.SmartTests;
import com.simplyag.autoclicker.gametest.TestClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
//? if >=1.19.4 {
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Runs {@link SmartTests} on Minecraft versions older than Fabric API's client game-test API (before 1.21.4).
 * Only built into the test mod of those versions (see gradle/target-common.gradle) and started by HarnessEntrypoint when
 * the game runs with -Dsmart-autoclicker.harness=true, which `./gradlew :<mc>:runClientGameTest` does there.
 *
 * <p>It drives a normal development client from a test thread: creates a flat world the way Fabric's test world
 * builder does, runs every test, then quits. Exit code 0 means every test passed. Differences from Fabric's API: the
 * client is not paused between test steps (waits are on real ticks), keys go through the game's keyboard handler, and
 * mouse clicks go straight to the open screen at the cursor position.
 */
public class TestHarness implements Runnable, TestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("Smart Autoclicker test harness");
    private static final long TIMEOUT_MINUTES = 20;

    private final Object tickLock = new Object();
    private long ticks;
    private double cursorX;
    private double cursorY;

    /** Called once at client start-up by HarnessEntrypoint, only when the harness is switched on. */
    @Override
    public void run() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            synchronized (tickLock) {
                ticks++;
                tickLock.notifyAll();
            }
        });
        Thread thread = new Thread(this::runAll, "Smart Autoclicker test harness");
        thread.setDaemon(true);
        thread.start();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(TIMEOUT_MINUTES));
            } catch (InterruptedException e) {
                return;
            }
            fail(new AssertionError("The tests took longer than " + TIMEOUT_MINUTES + " minutes"));
        }, "Smart Autoclicker test watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void runAll() {
        try {
            // Wait for the game to finish loading (title screen or first-launch onboarding, no loading overlay).
            waitUntil(() -> Minecraft.getInstance() != null);
            waitFor(mc -> mc.getOverlay() == null && mc.screen != null, 20 * 120);
            SmartTests.prepare(this);
            runOnClient(mc -> mc.options.pauseOnLostFocus = false);
            createWorld();
            MinecraftServer server = computeOnClient(Minecraft::getSingleplayerServer);
            SmartTests.run(this, new HarnessServer(server));
            LOGGER.info("All tests passed");
            runOnClient(Minecraft::stop);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /** Logs the failure and ends the game with exit code 1, so the Gradle task fails. */
    private static void fail(Throwable t) {
        LOGGER.error("TEST FAILED", t);
        LogManager.shutdown();
        Runtime.getRuntime().halt(1);
    }

    /**
     * A flat, peaceful-weather, no-daylight-cycle world, like Fabric's test world builder makes. Before 1.19.4 the
     * Create World screen has no API for the world type, so those versions make a normal world and build a flat stone
     * arena high above the terrain instead (see {@link #buildArena}).
     */
    private void createWorld() {
        runOnClient(mc -> {
            deleteSaves(mc.gameDirectory.toPath().resolve("saves"));
            //? if >=1.19 {
            CreateWorldScreen.openFresh(mc, mc.screen);
            //?} else if >=1.18.2 {
            /*mc.setScreen(CreateWorldScreen.createFresh(mc.screen));
            *///?} else
            /*mc.setScreen(CreateWorldScreen.create(mc.screen));*/
            if (!(mc.screen instanceof CreateWorldScreen)) throw new AssertionError("Create World screen didn't open");
            CreateWorldScreen screen = (CreateWorldScreen) mc.screen;
            //? if >=1.19.4 {
            WorldCreationUiState state = screen.getUiState();
            Holder<WorldPreset> flat = state.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET)
                    .getOrThrow(WorldPresets.FLAT);
            state.setWorldType(new WorldCreationUiState.WorldTypeEntry(flat));
            state.setSeed("1");
            state.setGenerateStructures(false);
            state.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            state.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            state.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            //?}
            Button create = findButton(screen, "selectWorld.create");
            if (create == null) throw new AssertionError("No Create World button");
            create.onPress();
        });
        waitFor(mc -> mc.level != null && mc.player != null && mc.screen == null, 20 * 120);
        waitTicks(20);
        //? if <1.19.4
        /*buildArena(new HarnessServer(computeOnClient(Minecraft::getSingleplayerServer)));*/
    }

    /** Arena floor height: above the terrain of every version, below the old 256 build limit with room to fly. */
    private static final int ARENA_Y = 150;
    /** Half the arena's width; 181 x 181 blocks is just under /fill's 32768-block limit. */
    private static final int ARENA_RADIUS = 90;

    /** A flat stone floor in the sky, no mobs, no weather, fixed noon: the tests' flat world on older versions. */
    private void buildArena(HarnessServer server) {
        server.runCommand("gamerule doMobSpawning false");
        server.runCommand("gamerule doWeatherCycle false");
        server.runCommand("gamerule doDaylightCycle false");
        server.runCommand("weather clear");
        int r = ARENA_RADIUS;
        // /fill only works on loaded chunks: keep the whole arena loaded, and fill until the floor is really there
        // (the corners are the last chunks to load). The player only goes up once it is, or they'd fall.
        server.runCommand("forceload add " + -r + " " + -r + " " + r + " " + r);
        String fill = "fill " + -r + " " + ARENA_Y + " " + -r + " " + r + " " + ARENA_Y + " " + r + " minecraft:smooth_stone";
        for (int attempt = 0; !arenaBuilt(server, r); attempt++) {
            if (attempt == 60) throw new AssertionError("The test arena's chunks never loaded");
            waitTicks(20);
            server.runCommand(fill);
        }
        server.runCommand("kill @e[type=!player]");
        server.runCommand("tp @p 0.5 " + (ARENA_Y + 1) + " 0.5 0 0");
        //? if >=1.20 {
        waitFor(mc -> mc.player.onGround(), 20 * 30);
        //?} else
        /*waitFor(mc -> mc.player.isOnGround(), 20 * 30);*/
        waitTicks(20);
    }

    private static boolean arenaBuilt(HarnessServer server, int r) {
        return server.computeOnServer(s -> {
            for (int x : new int[]{-r, r}) {
                for (int z : new int[]{-r, r}) {
                    if (s.overworld().getBlockState(new BlockPos(x, ARENA_Y, z)).getBlock() != Blocks.SMOOTH_STONE) return false;
                }
            }
            return true;
        });
    }

    private static void deleteSaves(Path saves) {
        if (!Files.isDirectory(saves)) return;
        try (Stream<Path> paths = Files.walk(saves)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            Files.createDirectories(saves);
        } catch (IOException e) {
            LOGGER.warn("Couldn't clear old test worlds", e);
        }
    }

    private static Button findButton(ContainerEventHandler parent, String translationKey) {
        Component label = Compat.translatable(translationKey);
        for (GuiEventListener child : parent.children()) {
            if (child instanceof Button button && button.getMessage().equals(label)) return button;
            if (child instanceof ContainerEventHandler container) {
                Button found = findButton(container, translationKey);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ TestClient

    @Override
    public void runOnClient(Consumer<Minecraft> action) {
        computeOnClient(mc -> {
            action.accept(mc);
            return null;
        });
    }

    @Override
    public <T> T computeOnClient(Function<Minecraft, T> function) {
        Minecraft mc = Minecraft.getInstance();
        try {
            return mc.submit(() -> function.apply(mc)).join();
        } catch (CompletionException e) {
            throw rethrow(e.getCause());
        }
    }

    private static RuntimeException rethrow(Throwable cause) {
        if (cause instanceof RuntimeException runtime) throw runtime;
        if (cause instanceof Error error) throw error;
        throw new RuntimeException(cause);
    }

    @Override
    public void waitTick() {
        waitTicks(1);
    }

    @Override
    public void waitTicks(int count) {
        synchronized (tickLock) {
            long target = ticks + count;
            while (ticks < target) {
                try {
                    tickLock.wait();
                } catch (InterruptedException e) {
                    throw new AssertionError("Interrupted", e);
                }
            }
        }
    }

    @Override
    public void waitFor(Predicate<Minecraft> condition, int timeoutTicks) {
        for (int i = 0; i <= timeoutTicks; i++) {
            if (computeOnClient(condition::test)) return;
            waitTick();
        }
        throw new AssertionError("Timed out after " + timeoutTicks + " ticks waiting for a condition");
    }

    private static void waitUntil(Supplier<Boolean> condition) throws InterruptedException {
        while (!condition.get()) Thread.sleep(50);
    }

    @Override
    public void waitForScreen(Class<? extends Screen> screenClass) {
        waitFor(mc -> screenClass.isInstance(mc.screen), 20 * 10);
    }

    @Override
    public void setScreen(Supplier<Screen> screen) {
        runOnClient(mc -> mc.setScreen(screen.get()));
        waitTick();
    }

    @Override
    public void takeScreenshot(String name) {
        // Let a frame render first, as Fabric's API does.
        waitTick();
        //? if >=1.17.1 {
        runOnClient(mc -> Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> LOGGER.info("Screenshot {}: {}", name, message.getString())));
        //?} else {
        /*runOnClient(mc -> Screenshot.grab(mc.gameDirectory, name + ".png", mc.getWindow().getWidth(),
                mc.getWindow().getHeight(), mc.getMainRenderTarget(),
                message -> LOGGER.info("Screenshot {}: {}", name, message.getString())));
        *///?}
    }

    @Override
    public Input getInput() {
        return new Input() {
            @Override
            public void holdKey(Function<Options, KeyMapping> key) {
                runOnClient(mc -> key.apply(mc.options).setDown(true));
            }

            @Override
            public void releaseKey(Function<Options, KeyMapping> key) {
                runOnClient(mc -> key.apply(mc.options).setDown(false));
            }

            @Override
            public void pressKey(int key) {
                runOnClient(mc -> {
                    long window = mc.getWindow().getWindow();
                    mc.keyboardHandler.keyPress(window, key, 0, InputConstants.PRESS, 0);
                    mc.keyboardHandler.keyPress(window, key, 0, InputConstants.RELEASE, 0);
                });
                waitTick();
            }

            @Override
            public void setCursorPos(double x, double y) {
                cursorX = x;
                cursorY = y;
            }

            @Override
            public void pressMouse(int button) {
                runOnClient(mc -> {
                    if (mc.screen == null) throw new AssertionError("pressMouse needs an open screen in the harness");
                    double scale = mc.getWindow().getGuiScale();
                    mc.screen.mouseClicked(cursorX / scale, cursorY / scale, button);
                    mc.screen.mouseReleased(cursorX / scale, cursorY / scale, button);
                });
                waitTick();
            }
        };
    }

    private record HarnessServer(MinecraftServer server) implements TestClient.Server {
        @Override
        public void runCommand(String command) {
            //? if >=1.19 {
            runOnServer(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), command));
            //?} else
            /*runOnServer(s -> s.getCommands().performCommand(s.createCommandSourceStack(), command));*/
        }

        @Override
        public void runOnServer(Consumer<MinecraftServer> action) {
            computeOnServer(s -> {
                action.accept(s);
                return null;
            });
        }

        @Override
        public <T> T computeOnServer(Function<MinecraftServer, T> function) {
            try {
                return server.submit(() -> function.apply(server)).join();
            } catch (CompletionException e) {
                throw rethrow(e.getCause());
            }
        }
    }
}
