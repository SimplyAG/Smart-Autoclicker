package com.simplyag.autoclicker.module;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Stats for one stretch of automation, shown in the HUD and in the summary toast when it ends. */
public final class Session {
    private final long startMillis = System.currentTimeMillis();
    private final int startXpLevel;
    public int attacks;
    public int foodsEaten;
    public int rocketsFired;

    Session(LocalPlayer player) {
        this.startXpLevel = player.experienceLevel;
    }

    public long elapsedMillis() {
        return System.currentTimeMillis() - startMillis;
    }

    public int levelsGained(LocalPlayer player) {
        return Math.max(0, player.experienceLevel - startXpLevel);
    }

    public static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) return String.format("%dh %02dm", hours, minutes);
        if (minutes > 0) return String.format("%dm %02ds", minutes, seconds);
        return seconds + "s";
    }

    /** e.g. "12m 04s · 340 hits · +6 levels · 4 eaten". Leaves out anything that stayed at zero. */
    public Component summary(LocalPlayer player) {
        StringBuilder sb = new StringBuilder(formatDuration(elapsedMillis()));
        if (attacks > 0) sb.append(" · ").append(attacks).append(attacks == 1 ? " hit" : " hits");
        int levels = levelsGained(player);
        if (levels > 0) sb.append(" · +").append(levels).append(levels == 1 ? " level" : " levels");
        if (foodsEaten > 0) sb.append(" · ").append(foodsEaten).append(" eaten");
        if (rocketsFired > 0) sb.append(" · ").append(rocketsFired).append(rocketsFired == 1 ? " rocket" : " rockets");
        return Component.literal(sb.toString());
    }
}
