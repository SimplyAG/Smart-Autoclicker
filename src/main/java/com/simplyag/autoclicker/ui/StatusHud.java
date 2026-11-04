package com.simplyag.autoclicker.ui;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.module.Module;
import com.simplyag.autoclicker.module.ModuleManager;
import com.simplyag.autoclicker.module.Session;
import com.simplyag.autoclicker.ui.panel.Theme;
import com.simplyag.autoclicker.util.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Compact panel listing what's running, with a live status for each, in a corner of the screen. */
public final class StatusHud {
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 11;
    private static final int MARGIN = 4;

    private StatusHud() {
    }

    /** Registered as a HUD layer by Compat.registerHud; runs every frame. */
    public static void draw(Gfx graphics) {
        Minecraft mc = Minecraft.getInstance();
        SmartConfig cfg = SmartConfig.get();
        if (!cfg.showHud || Compat.hudHidden(mc) || mc.player == null || !ModuleManager.anyRunning()) return;
        Font font = mc.font;

        List<Line> lines = new ArrayList<>();
        Session session = ModuleManager.session();
        Component header = Component.translatable("smart-autoclicker.hud.title",
                session == null ? "0s" : Session.formatDuration(session.elapsedMillis()));
        for (Module module : ModuleManager.ALL) {
            if (!module.isRunning()) continue;
            Component status = module.status();
            lines.add(new Line(module, status == null ? module.name()
                    : Component.empty().append(module.name()).append(Component.literal("  ").append(status).withColor(Theme.TEXT_DIM))));
        }

        int width = 8 + font.width(header);
        for (Line line : lines) width = Math.max(width, 10 + font.width(line.text));
        int panelW = width + PADDING * 2;
        int panelH = PADDING * 2 + LINE_HEIGHT * (lines.size() + 1);

        boolean right = cfg.hudCorner == SmartConfig.HudCorner.TOP_RIGHT || cfg.hudCorner == SmartConfig.HudCorner.BOTTOM_RIGHT;
        boolean bottom = cfg.hudCorner == SmartConfig.HudCorner.BOTTOM_LEFT || cfg.hudCorner == SmartConfig.HudCorner.BOTTOM_RIGHT;
        int x = right ? graphics.guiWidth() - panelW - MARGIN : MARGIN;
        int y = bottom ? graphics.guiHeight() - panelH - MARGIN : MARGIN;

        // Same look as the control panel: dark rounded card, thin border, colored dot per module
        Theme.roundBordered(graphics, x, y, panelW, panelH, 0xD8121620, 0xC02A3142);

        int textY = y + PADDING + 1;
        Theme.pill(graphics, x + PADDING, textY + 2, 4, 4, Theme.GOOD);
        graphics.text(font, header, x + PADDING + 8, textY, Theme.TEXT_DIM);
        for (Line line : lines) {
            textY += LINE_HEIGHT;
            Theme.round(graphics, x + PADDING, textY + 1, 5, 6, line.module.color());
            graphics.text(font, line.text, x + PADDING + 10, textY, Theme.TEXT);
        }
    }

    private record Line(Module module, Component text) {
    }
}
