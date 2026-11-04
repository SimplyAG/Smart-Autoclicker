package com.simplyag.autoclicker.ui.panel;

import com.simplyag.autoclicker.ui.Gfx;
import net.minecraft.util.Mth;

/** Colors and small drawing primitives shared by the control panel. */
public final class Theme {
    public static final int SHADOW = 0x66000000;
    public static final int PANEL = 0xF2121620;
    public static final int PANEL_BORDER = 0xFF2A3142;
    public static final int HEADER = 0xFF161B27;
    public static final int SIDEBAR = 0xFF0F131B;
    public static final int DIVIDER = 0xFF232A38;
    public static final int ROW_HOVER = 0x18FFFFFF;
    public static final int SELECTED = 0xFF1D2433;
    public static final int CARD = 0xFF181E2B;
    public static final int CARD_HOVER = 0xFF1E2535;
    public static final int CONTROL = 0xFF262E3F;
    public static final int CONTROL_HOVER = 0xFF2F3950;
    public static final int TRACK = 0xFF2A3245;

    public static final int TEXT = 0xFFF1F4F9;
    public static final int TEXT_DIM = 0xFF9AA3B5;
    public static final int TEXT_FAINT = 0xFF5F6879;
    public static final int ACCENT = 0xFF5AA9E6;
    public static final int GOOD = 0xFF5FD38A;
    public static final int BAD = 0xFFFF6B6B;
    public static final int WARN = 0xFFFFD166;

    private Theme() {
    }

    /** A rectangle with its corner pixels cut off: reads as rounded at Minecraft's pixel scale. */
    public static void round(Gfx g, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Rounded rectangle with a 1px border. */
    public static void roundBordered(Gfx g, int x, int y, int w, int h, int fill, int border) {
        round(g, x, y, w, h, border);
        round(g, x + 1, y + 1, w - 2, h - 2, fill);
    }

    /** A pill: fully rounded ends, built from a stack of shrinking rows. */
    public static void pill(Gfx g, int x, int y, int w, int h, int color) {
        int r = h / 2;
        for (int row = 0; row < h; row++) {
            double dy = Math.abs(row + 0.5 - h / 2.0);
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    /** Blends two ARGB colors channel by channel (in sRGB, as the game's own lerp does). */
    public static int lerpColor(float t, int from, int to) {
        float f = Mth.clamp(t, 0, 1);
        int color = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int a = (from >>> shift) & 0xFF;
            int b = (to >>> shift) & 0xFF;
            color |= (a + Mth.floor(f * (b - a))) << shift;
        }
        return color;
    }

    public static int withAlpha(int color, float alpha) {
        int a = Math.round((color >>> 24) * Mth.clamp(alpha, 0, 1));
        return a << 24 | color & 0xFFFFFF;
    }
}
