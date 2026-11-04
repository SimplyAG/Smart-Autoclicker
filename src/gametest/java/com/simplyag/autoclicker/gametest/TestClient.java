package com.simplyag.autoclicker.gametest;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What the in-game tests need from a running game. From Minecraft 1.21.4 it is backed by Fabric API's client game-test
 * API (AutoClickerClientGameTest); before that by the mod's own harness (src/harness), which drives a normal client.
 * Method names follow Fabric's API, so the tests read the same either way. Every method is called from the test
 * thread, never from the game's own threads.
 */
public interface TestClient {
    void runOnClient(Consumer<Minecraft> action);

    <T> T computeOnClient(Function<Minecraft, T> function);

    void waitTick();

    void waitTicks(int ticks);

    /** Waits until the condition holds on the client; fails the test after the timeout. */
    void waitFor(Predicate<Minecraft> condition, int timeoutTicks);

    void waitForScreen(Class<? extends Screen> screenClass);

    void setScreen(Supplier<Screen> screen);

    /** Saves a screenshot to the run's screenshots folder. */
    void takeScreenshot(String name);

    Input getInput();

    interface Input {
        void holdKey(Function<Options, KeyMapping> key);

        void releaseKey(Function<Options, KeyMapping> key);

        /** Presses and releases a keyboard key (GLFW key code). */
        void pressKey(int key);

        /** Moves the mouse, in window pixels. */
        void setCursorPos(double x, double y);

        /** Clicks a mouse button where the cursor is. */
        void pressMouse(int button);
    }

    /** The integrated server of the test world. */
    interface Server {
        void runCommand(String command);

        void runOnServer(Consumer<MinecraftServer> action);

        <T> T computeOnServer(Function<MinecraftServer, T> function);
    }
}
