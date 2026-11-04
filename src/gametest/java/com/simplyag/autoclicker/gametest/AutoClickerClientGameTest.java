package com.simplyag.autoclicker.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
 * Runs {@link SmartTests} through Fabric API's client game-test API (Minecraft 1.21.4 and later; older versions build
 * without this class and use the harness in src/harness instead).
 */
public class AutoClickerClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        TestClient client = new FabricClient(context);
        SmartTests.prepare(client);
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            SmartTests.run(client, new FabricServer(singleplayer.getServer()));
        }
    }

    private record FabricClient(ClientGameTestContext context) implements TestClient {
        @Override
        public void runOnClient(Consumer<Minecraft> action) {
            context.runOnClient(action::accept);
        }

        @Override
        public <T> T computeOnClient(Function<Minecraft, T> function) {
            return context.computeOnClient(function::apply);
        }

        @Override
        public void waitTick() {
            context.waitTick();
        }

        @Override
        public void waitTicks(int ticks) {
            context.waitTicks(ticks);
        }

        @Override
        public void waitFor(Predicate<Minecraft> condition, int timeoutTicks) {
            context.waitFor(condition, timeoutTicks);
        }

        @Override
        public void waitForScreen(Class<? extends Screen> screenClass) {
            context.waitForScreen(screenClass);
        }

        @Override
        public void setScreen(Supplier<Screen> screen) {
            context.setScreen(screen);
        }

        @Override
        public void takeScreenshot(String name) {
            context.takeScreenshot(name);
        }

        @Override
        public Input getInput() {
            return new Input() {
                @Override
                public void holdKey(Function<Options, KeyMapping> key) {
                    context.getInput().holdKey(key);
                }

                @Override
                public void releaseKey(Function<Options, KeyMapping> key) {
                    context.getInput().releaseKey(key);
                }

                @Override
                public void pressKey(int key) {
                    context.getInput().pressKey(key);
                }

                @Override
                public void setCursorPos(double x, double y) {
                    context.getInput().setCursorPos(x, y);
                }

                @Override
                public void pressMouse(int button) {
                    context.getInput().pressMouse(button);
                }
            };
        }
    }

    private record FabricServer(TestServerContext server) implements TestClient.Server {
        @Override
        public void runCommand(String command) {
            server.runCommand(command);
        }

        @Override
        public void runOnServer(Consumer<MinecraftServer> action) {
            server.runOnServer(action::accept);
        }

        @Override
        public <T> T computeOnServer(Function<MinecraftServer, T> function) {
            return server.computeOnServer(function::apply);
        }
    }
}
