package com.simplyag.autoclicker.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A vanilla-style toast with an item icon, a colored title and up to two lines of text.
 * Toasts with the same token replace each other instead of stacking up.
 */
public class SmartToast implements Toast {
    private static final int PANEL = 0xF0141820;
    private static final int BORDER = 0xFF2E3648;
    private static final int TEXT_X = 32;
    private static final int MAX_TEXT_WIDTH = 200;
    private static final long DISPLAY_MS = 4500;

    private final Object token;
    private ItemStack icon;
    private Component title;
    private @Nullable Component message;
    private int titleColor;
    private List<FormattedCharSequence> lines = List.of();
    private int width = DEFAULT_WIDTH;
    private boolean changed = true;
    private long shownSince;
    private Toast.Visibility visibility = Toast.Visibility.HIDE;

    private SmartToast(Object token, ItemStack icon, Component title, @Nullable Component message, int titleColor) {
        this.token = token;
        set(icon, title, message, titleColor);
    }

    /** Shows a toast, or refreshes the one already showing for this token. */
    public static void show(ToastManager manager, Object token, ItemStack icon, Component title, @Nullable Component message, int titleColor) {
        SmartToast existing = manager.getToast(SmartToast.class, token);
        if (existing != null) {
            existing.set(icon, title, message, titleColor);
        } else {
            manager.addToast(new SmartToast(token, icon, title, message, titleColor));
        }
    }

    private void set(ItemStack icon, Component title, @Nullable Component message, int titleColor) {
        this.icon = icon;
        this.title = title;
        this.message = message;
        this.titleColor = titleColor;
        this.changed = true;
    }

    private void layout(Font font) {
        lines = message == null ? List.of() : font.split(message, MAX_TEXT_WIDTH);
        if (lines.size() > 2) lines = lines.subList(0, 2);
        int textWidth = font.width(title);
        for (FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        width = Math.max(DEFAULT_WIDTH, TEXT_X + textWidth + 10);
    }

    @Override
    public Object getToken() {
        return token;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return lines.size() <= 1 ? 32 : 42;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        if (changed) {
            layout(manager.getMinecraft().font);
            shownSince = fullyVisibleForMs;
            changed = false;
        }
        double displayMs = DISPLAY_MS * manager.getNotificationDisplayTimeMultiplier();
        visibility = fullyVisibleForMs - shownSince < displayMs ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        int w = width();
        int h = height();
        // Dark panel with a thin border and an accent stripe in the title color
        graphics.fill(0, 0, w, h, BORDER);
        graphics.fill(1, 1, w - 1, h - 1, PANEL);
        graphics.fill(1, 1, 4, h - 1, titleColor);
        graphics.fakeItem(icon, 10, (h - 16) / 2);
        if (lines.isEmpty()) {
            graphics.text(font, title, TEXT_X, (height() - 8) / 2, titleColor, false);
            return;
        }
        int y = 7;
        graphics.text(font, title, TEXT_X, y, titleColor, false);
        for (FormattedCharSequence line : lines) {
            y += 11;
            graphics.text(font, line, TEXT_X, y, 0xFFFFFFFF, false);
        }
    }
}
