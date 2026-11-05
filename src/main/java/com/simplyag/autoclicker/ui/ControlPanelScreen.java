package com.simplyag.autoclicker.ui;

import com.google.common.collect.ImmutableList;
import com.simplyag.autoclicker.util.Compat;
import com.mojang.blaze3d.platform.InputConstants;
import com.simplyag.autoclicker.AutoClickerClient;
import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.module.Module;
import com.simplyag.autoclicker.module.ModuleManager;
import com.simplyag.autoclicker.module.Session;
import com.simplyag.autoclicker.ui.panel.PanelSettings;
import com.simplyag.autoclicker.ui.panel.Setting;
import com.simplyag.autoclicker.ui.panel.Theme;
import com.simplyag.autoclicker.util.Notifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * The Smart Autoclicker control panel: a sidebar of pages (presets, each module, safety, general)
 * and a detail view with switches, sliders, option pickers and inline key binding.
 * Drawn and hit-tested by hand so the whole thing has one consistent look.
 * Changes apply instantly and are saved when the panel closes.
 */
public class ControlPanelScreen extends Screen {
    private static final String T = "smart-autoclicker.panel.";
    private static final int HEADER_H = 26;
    private static final int SIDEBAR_W = 124;
    private static final int ENTRY_H = 20;
    private static final int ROW_H = 20;
    private static final int PAD = 10;
    private static final int CONTROL_W = 122;
    private static final int SLIDER_W = 76;

    private enum Kind { PRESETS, MODULE, SAFETY, GENERAL }

    /** A sidebar page. Equal by kind and module, so the page remembered from the last time the panel was open matches. */
    private static final class Page {
        private final Kind kind;
        private final @Nullable Module module;

        Page(Kind kind, @Nullable Module module) {
            this.kind = kind;
            this.module = module;
        }

        Kind kind() {
            return kind;
        }

        @Nullable Module module() {
            return module;
        }

        Component name() {
            return kind == Kind.MODULE ? module.name() : Compat.translatable(T + kind.name().toLowerCase());
        }

        ItemStack icon() {
            switch (kind) {
                case PRESETS:
                    return new ItemStack(Items.NETHER_STAR);
                case MODULE:
                    return module.icon();
                case SAFETY:
                    return new ItemStack(Items.TOTEM_OF_UNDYING);
                default:
                    return new ItemStack(Items.COMPARATOR);
            }
        }

        int accent() {
            return kind == Kind.MODULE ? module.color() : Theme.ACCENT;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Page && ((Page) o).kind == kind && ((Page) o).module == module;
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, module);
        }
    }

    /** A clickable, draggable or scrollable rectangle, collected while drawing. */
    private static final class Hit {
        private final int x0, y0, x1, y1;
        private final @Nullable Runnable click;
        private final @Nullable DoubleConsumer drag;
        private final @Nullable IntConsumer wheel;

        Hit(int x0, int y0, int x1, int y1, @Nullable Runnable click, @Nullable DoubleConsumer drag, @Nullable IntConsumer wheel) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.click = click;
            this.drag = drag;
            this.wheel = wheel;
        }

        @Nullable Runnable click() {
            return click;
        }

        @Nullable DoubleConsumer drag() {
            return drag;
        }

        @Nullable IntConsumer wheel() {
            return wheel;
        }

        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    private static @Nullable Page lastPage;

    private final @Nullable Screen parent;
    private final List<Page> pages = new ArrayList<>();
    private Page page;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Float> anim = new HashMap<>();
    private @Nullable KeyMapping listening;
    private @Nullable DoubleConsumer dragging;
    private @Nullable Component tooltip;
    private double scroll;
    private double maxScroll;
    private double sidebarScroll;
    private double maxSidebarScroll;
    private long openedAt;
    private long lastFrame;
    private float frameDelta;
    // Visible content rectangle; hits outside it are ignored (scrolled out of view)
    private int clipX0, clipY0, clipX1, clipY1;
    private int contentX0, contentY0, contentX1, contentY1;
    private int sidebarX0, sidebarY0, sidebarX1, sidebarY1;

    public ControlPanelScreen(@Nullable Screen parent) {
        super(Compat.translatable("smart-autoclicker.screen.title"));
        this.parent = parent;
        pages.add(new Page(Kind.PRESETS, null));
        for (Module module : ModuleManager.ALL) pages.add(new Page(Kind.MODULE, module));
        pages.add(new Page(Kind.SAFETY, null));
        pages.add(new Page(Kind.GENERAL, null));
        this.page = lastPage != null ? lastPage : pages.get(0);
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        lastFrame = System.nanoTime();
    }

    // ------------------------------------------------------------------ rendering

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (minecraft.level == null) {
            super.extractBackground(graphics, mouseX, mouseY, a);
            return;
        }
        extractBlurredBackground(graphics);
        graphics.fill(0, 0, width, height, 0x80000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        drawPanel(new Gfx(graphics), mouseX, mouseY);
    }
    //?} else if >=1.20 {
    /*@Override
    //? if >=1.20.2 {
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float a) {
    //?} else
    /^public void renderBackground(GuiGraphics graphics) {^/
        if (minecraft.level == null) {
            //? if >=1.20.2 {
            super.renderBackground(graphics, mouseX, mouseY, a);
            //?} else
            /^super.renderBackground(graphics);^/
            return;
        }
        // The blurred world behind menus exists from 1.20.5; before it the dimmed world shows through.
        //? if >=1.21.6 {
        renderBlurredBackground(graphics);
        //?} else if >=1.21.2 {
        /^renderBlurredBackground();
        ^///?} else if >=1.20.5 {
        /^renderBlurredBackground(a);
        ^///?}
        graphics.fill(0, 0, width, height, 0x80000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        // Until 1.21.5 a screen draws its own background; from 1.21.6 the game does it before render().
        //? if <1.20.2 {
        /^renderBackground(graphics);
        ^///?} else if <1.21.6
        /^renderBackground(graphics, mouseX, mouseY, a);^/
        drawPanel(new Gfx(graphics), mouseX, mouseY);
    }
    *///?} else {
    /*// Before 1.20 screens draw with a PoseStack and paint their own background.
    @Override
    public void renderBackground(PoseStack pose) {
        if (minecraft.level == null) {
            super.renderBackground(pose);
            return;
        }
        GuiComponent.fill(pose, 0, 0, width, height, 0x80000000);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float a) {
        renderBackground(pose);
        drawPanel(new Gfx(pose), mouseX, mouseY);
    }
    *///?}

    private void drawPanel(Gfx g, int mouseX, int mouseY) {
        long now = System.nanoTime();
        frameDelta = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        hits.clear();
        tooltip = null;

        float open = ease(Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f));
        int w = Math.min(480, width - 20);
        int h = Math.min(280, height - 16);
        int x = (width - w) / 2;
        int y = (height - h) / 2 + Math.round((1 - open) * 12);

        Theme.round(g, x + 2, y + 3, w, h, Theme.SHADOW);
        Theme.roundBordered(g, x, y, w, h, Theme.PANEL, Theme.PANEL_BORDER);
        drawHeader(g, x, y, w, mouseX, mouseY);
        drawSidebar(g, x, y + HEADER_H + 1, h - HEADER_H - 2, mouseX, mouseY);
        drawContent(g, x + SIDEBAR_W + 1, y + HEADER_H + 1, w - SIDEBAR_W - 2, h - HEADER_H - 2, mouseX, mouseY);

        if (tooltip != null && dragging == null) {
            List<FormattedCharSequence> lines = font.split(tooltip, 220);
            g.tooltip(font, lines, mouseX, mouseY);
        }
    }

    private void drawHeader(Gfx g, int x, int y, int w, int mx, int my) {
        Theme.round(g, x + 1, y + 1, w - 2, HEADER_H, Theme.HEADER);
        g.fill(x + 1, y + HEADER_H, x + w - 1, y + HEADER_H + 1, Theme.DIVIDER);
        g.modIcon(x + 6, y + 5, 16);
        g.text(font, title, x + 28, y + 9, Theme.TEXT);
        String version = FabricLoader.getInstance().getModContainer(AutoClickerClient.MOD_ID)
                .map(c -> "v" + c.getMetadata().getVersion().getFriendlyString()).orElse("");
        g.text(font, version, x + 32 + font.width(title), y + 9, Theme.TEXT_FAINT);

        setClip(x, y, x + w, y + HEADER_H);
        int right = x + w - 6;
        long running = ModuleManager.ALL.stream().filter(Module::isRunning).count();
        if (running > 0) {
            Component label = Compat.translatable(T + "stop_all");
            int bw = font.width(label) + 16;
            int bx = right - bw;
            boolean hover = inside(mx, my, bx, y + 5, bw, 16);
            Theme.round(g, bx, y + 5, bw, 16, hover ? 0xFFB23A3A : 0xFF8C2D2D);
            g.text(font, label, bx + 8, y + 9, Theme.TEXT);
            addHit(bx, y + 5, bw, 16, ModuleManager::stopAllByUser);
            right = bx - 8;

            Session session = ModuleManager.session();
            Component summary = Compat.translatable(T + "running", running,
                    session == null ? "0s" : Session.formatDuration(session.elapsedMillis()));
            int sw = font.width(summary);
            Theme.pill(g, right - sw - 8, y + 11, 4, 4, Theme.GOOD);
            g.text(font, summary, right - sw, y + 9, Theme.GOOD);
        } else {
            Component idle = Compat.translatable(T + "idle");
            g.text(font, idle, right - font.width(idle), y + 9, Theme.TEXT_FAINT);
        }
    }

    private void drawSidebar(Gfx g, int x, int y, int h, int mx, int my) {
        sidebarX0 = x + 1;
        sidebarY0 = y;
        sidebarX1 = x + SIDEBAR_W;
        sidebarY1 = y + h;
        g.fill(x + 1, y, x + SIDEBAR_W, y + h, Theme.SIDEBAR);
        g.fill(x + SIDEBAR_W, y, x + SIDEBAR_W + 1, y + h, Theme.DIVIDER);
        g.enableScissor(sidebarX0, sidebarY0, sidebarX1, sidebarY1);
        setClip(sidebarX0, sidebarY0, sidebarX1, sidebarY1);

        int top = y + 6 - (int) sidebarScroll;
        int iy = top;
        for (Page p : pages) {
            if (p.kind() == Kind.MODULE && p.module() == ModuleManager.ALL.get(0)) iy = sectionLabel(g, "modules", x, iy);
            if (p.kind() == Kind.SAFETY) iy = sectionLabel(g, "settings", x, iy);
            drawSidebarEntry(g, p, x + 4, iy, SIDEBAR_W - 8, mx, my);
            iy += ENTRY_H + 1;
        }
        maxSidebarScroll = Math.max(0, iy - top + 6 - h);
        sidebarScroll = Mth.clamp(sidebarScroll, 0, maxSidebarScroll);
        g.disableScissor();
    }

    private int sectionLabel(Gfx g, String key, int x, int y) {
        g.text(font, Compat.translatable(T + "section." + key), x + 10, y + 7, Theme.TEXT_FAINT);
        return y + 19;
    }

    private void drawSidebarEntry(Gfx g, Page p, int x, int y, int w, int mx, int my) {
        boolean selected = p.equals(page);
        boolean hover = inside(mx, my, x, y, w, ENTRY_H);
        float sel = animate("side:" + p, selected ? 1 : 0);
        if (selected || sel > 0.01f) Theme.round(g, x, y, w, ENTRY_H, Theme.withAlpha(Theme.SELECTED, sel));
        if (hover && !selected) Theme.round(g, x, y, w, ENTRY_H, Theme.ROW_HOVER);
        if (selected) g.fill(x, y + 4, x + 2, y + ENTRY_H - 4, p.accent());
        g.item(p.icon(), x + 5, y + 2);
        g.text(font, p.name(), x + 25, y + 6, selected ? Theme.TEXT : Theme.TEXT_DIM);

        boolean active = p.kind() == Kind.MODULE ? p.module().isRunning()
                : p.kind() == Kind.PRESETS && ModuleManager.PRESETS.stream().anyMatch(ModuleManager.Preset::isRunning);
        if (active) Theme.pill(g, x + w - 9, y + 8, 4, 4, Theme.GOOD);
        addHit(x, y, w, ENTRY_H, () -> selectPage(p));
    }

    private void drawContent(Gfx g, int x, int y, int w, int h, int mx, int my) {
        contentX0 = x;
        contentY0 = y;
        contentX1 = x + w;
        contentY1 = y + h;
        g.enableScissor(contentX0, contentY0, contentX1, contentY1);
        setClip(contentX0, contentY0, contentX1, contentY1);

        int cx = x + PAD + 2;
        int cw = w - PAD * 2 - 4;
        int top = y + PAD - (int) scroll;
        int bottom;
        switch (page.kind()) {
            case PRESETS:
                bottom = drawPresetsPage(g, cx, top, cw, mx, my);
                break;
            case MODULE:
                bottom = drawModulePage(g, page.module(), cx, top, cw, mx, my);
                break;
            case SAFETY:
                bottom = drawSettingsPage(g, "safety", PanelSettings.safety(), cx, top, cw, mx, my);
                break;
            default:
                bottom = drawSettingsPage(g, "general", PanelSettings.general(), cx, top, cw, mx, my);
                break;
        }
        maxScroll = Math.max(0, bottom - top + PAD - h);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        g.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(16, (int) (h * (h / (double) (h + maxScroll))));
            int barY = y + (int) ((h - barH) * (scroll / maxScroll));
            Theme.round(g, x + w - 4, barY + 2, 2, barH - 4, Theme.TEXT_FAINT);
        }
    }

    // ------------------------------------------------------------------ pages

    private int drawPageTitle(Gfx g, Component title, @Nullable Component subtitle, int x, int y, int w) {
        g.push();
        g.translate(x, y);
        g.scale(1.5f);
        g.text(font, title, 0, 0, Theme.TEXT);
        g.pop();
        y += 17;
        if (subtitle != null) {
            for (FormattedCharSequence line : font.split(subtitle, w)) {
                g.text(font, line, x, y, Theme.TEXT_DIM);
                y += 10;
            }
        }
        return y + 6;
    }

    private int drawModulePage(Gfx g, Module module, int x, int y, int w, int mx, int my) {
        boolean inWorld = minecraft.player != null;
        g.push();
        g.translate(x, y);
        g.scale(2f);
        g.item(module.icon(), 0, 0);
        g.pop();

        g.push();
        g.translate(x + 40, y + 2);
        g.scale(1.5f);
        g.text(font, module.name(), 0, 0, Theme.TEXT);
        g.pop();

        int statusY = y + 20;
        if (module.isRunning()) {
            Theme.pill(g, x + 40, statusY + 2, 4, 4, Theme.GOOD);
            Component status = module.status();
            Component text = status == null ? Compat.translatable(T + "running_short")
                    : Compat.translatable(T + "running_short").append(Compat.literal(" · ")).append(status);
            g.text(font, text, x + 48, statusY, Theme.GOOD);
        } else if (!inWorld) {
            g.text(font, Compat.translatable(T + "join_world"), x + 40, statusY, Theme.WARN);
        } else {
            g.text(font, Compat.translatable(T + "stopped"), x + 40, statusY, Theme.TEXT_FAINT);
        }

        drawSwitch(g, "module:" + module.id(), x + w - 34, y + 8, 34, 16, module.isRunning(), inWorld, module.color(),
                mx, my, module::toggle);

        y += 40;
        for (FormattedCharSequence line : font.split(module.description(), w)) {
            g.text(font, line, x, y, Theme.TEXT_DIM);
            y += 10;
        }
        y = drawCompatibility(g, module, x, y + 4, w);
        y += 6;
        g.fill(x, y, x + w, y + 1, Theme.DIVIDER);
        y += 6;

        y = drawRow(g, new Setting.Hotkey("module_key", module::toggleKey), x, y, w, module.color(), mx, my);
        for (Setting setting : PanelSettings.forModule(module)) {
            y = drawRow(g, setting, x, y, w, module.color(), mx, my);
        }
        return y;
    }

    /**
     * "Needs Auto Attack" / "Can't run with Hold Left Click, ..." under the description, and a warning
     * naming whatever is running now that turning this module on would stop.
     */
    private int drawCompatibility(Gfx g, Module module, int x, int y, int w) {
        List<Component> lines = new ArrayList<>();
        Module required = ModuleManager.requirementOf(module);
        if (required != null) lines.add(Compat.translatable(T + "requires", required.name()));
        List<Module> conflicts = ModuleManager.conflictsOf(module);
        if (!conflicts.isEmpty()) lines.add(Compat.translatable(T + "conflicts", Notifier.names(conflicts)));
        for (Component line : lines) {
            for (FormattedCharSequence part : font.split(line, w)) {
                g.text(font, part, x, y, Theme.TEXT_FAINT);
                y += 10;
            }
        }
        List<Module> wouldStop = conflicts.stream().filter(Module::isRunning).collect(Collectors.toList());
        if (!module.isRunning() && !wouldStop.isEmpty()) {
            for (FormattedCharSequence part : font.split(Compat.translatable(T + "would_stop", Notifier.names(wouldStop)), w)) {
                g.text(font, part, x, y, Theme.WARN);
                y += 10;
            }
        }
        return y;
    }

    private int drawSettingsPage(Gfx g, String key, List<Setting> settings, int x, int y, int w, int mx, int my) {
        y = drawPageTitle(g, Compat.translatable(T + key), Compat.translatable(T + key + ".desc"), x, y, w);
        g.fill(x, y, x + w, y + 1, Theme.DIVIDER);
        y += 6;
        for (Setting setting : settings) {
            y = drawRow(g, setting, x, y, w, Theme.ACCENT, mx, my);
        }
        return y;
    }

    private int drawPresetsPage(Gfx g, int x, int y, int w, int mx, int my) {
        y = drawPageTitle(g, Compat.translatable(T + "presets"), Compat.translatable(T + "presets.desc"), x, y, w);
        int columns = w >= 2 * 140 + 6 ? 2 : 1;
        int cardW = columns == 2 ? (w - 6) / 2 : w;
        int cardH = 86;
        for (int i = 0; i < ModuleManager.PRESETS.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            drawPresetCard(g, ModuleManager.PRESETS.get(i), x + col * (cardW + 6), y + row * (cardH + 6), cardW, cardH, mx, my);
        }
        int rows = (ModuleManager.PRESETS.size() + columns - 1) / columns;
        return y + rows * (cardH + 6);
    }

    private void drawPresetCard(Gfx g, ModuleManager.Preset preset, int x, int y, int w, int h, int mx, int my) {
        boolean running = preset.isRunning();
        boolean hover = inside(mx, my, x, y, w, h);
        Theme.roundBordered(g, x, y, w, h, hover ? Theme.CARD_HOVER : Theme.CARD, running ? Theme.GOOD : Theme.DIVIDER);
        g.item(new ItemStack(preset.icon()), x + 6, y + 6);
        g.text(font, preset.name(), x + 26, y + 6, Theme.TEXT);

        // Inline hotkey: "Key: none" -> click to bind
        KeyMapping key = AutoClickerClient.presetKey(preset);
        Component keyText = keyLabel(key, true);
        int kw = font.width(keyText);
        boolean keyHover = inside(mx, my, x + 26, y + 15, kw, 10);
        g.text(font, keyText, x + 26, y + 16, listening != null && listening == key ? Theme.WARN
                : keyHover ? Theme.TEXT : Theme.TEXT_FAINT);
        if (key != null) addHit(x + 26, y + 15, kw, 10, () -> listening = key);

        int ty = y + 30;
        List<FormattedCharSequence> lines = font.split(preset.description(), w - 12);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.text(font, lines.get(i), x + 6, ty, Theme.TEXT_DIM);
            ty += 10;
        }

        // Module chips, one per module across the bottom: click to include/exclude. A module that can't
        // run with what's already in the preset is crossed out; clicking it swaps it in.
        int chipY = y + h - 22;
        int step = Math.min(18, (w - 12) / ModuleManager.ALL.size());
        for (int i = 0; i < ModuleManager.ALL.size(); i++) {
            Module module = ModuleManager.ALL.get(i);
            int chipX = x + 6 + i * step;
            boolean included = preset.includes(module);
            List<Module> replaces = included ? ImmutableList.of() : preset.wouldReplace(module);
            boolean chipHover = inside(mx, my, chipX, chipY, 17, 17);
            Theme.round(g, chipX, chipY, 17, 17, included ? Theme.withAlpha(module.color(), 0.45f) : Theme.CONTROL);
            g.item(module.icon(), chipX, chipY);
            if (!included) Theme.round(g, chipX, chipY, 17, 17, replaces.isEmpty() ? 0xB0121620 : 0xD8121620);
            if (!replaces.isEmpty()) {
                for (int d = 0; d < 13; d++) g.fill(chipX + 2 + d, chipY + 14 - d, chipX + 4 + d, chipY + 15 - d, 0xC0FF6B6B);
            }
            if (chipHover) {
                g.outline(chipX, chipY, 17, 17, Theme.TEXT_DIM);
                Module required = ModuleManager.requirementOf(module);
                tooltip = included ? Compat.translatable(T + "chip.included", module.name())
                        : !replaces.isEmpty() ? Compat.translatable(T + "chip.conflict", module.name(), Notifier.names(replaces))
                        : required != null && !preset.includes(required) ? Compat.translatable(T + "chip.adds", module.name(), required.name())
                        : Compat.translatable(T + "chip.excluded", module.name());
            }
            addHit(chipX, chipY, 17, 17, () -> preset.setIncluded(module, !included));
        }

        // Start / stop button, top right so the chips get the full width
        boolean usable = minecraft.player != null && !preset.modules().isEmpty();
        Component label = Compat.translatable(running ? T + "preset.stop" : T + "preset.start");
        int bw = font.width(label) + 14;
        int bx = x + w - bw - 6;
        int by = y + 6;
        boolean bHover = usable && inside(mx, my, bx, by, bw, 15);
        int bg = !usable ? Theme.CONTROL : running ? (bHover ? 0xFFB23A3A : 0xFF8C2D2D) : (bHover ? 0xFF3E8F5F : 0xFF2F7049);
        Theme.round(g, bx, by, bw, 15, bg);
        g.text(font, label, bx + 7, by + 4, usable ? Theme.TEXT : Theme.TEXT_FAINT);
        if (usable) {
            addHit(bx, by, bw, 15, () -> {
                if (running) {
                    preset.modules().forEach(m -> m.stop(true));
                } else {
                    ModuleManager.applyPreset(preset);
                }
            });
        }
    }

    // ------------------------------------------------------------------ rows & controls

    private int drawRow(Gfx g, Setting setting, int x, int y, int w, int accent, int mx, int my) {
        boolean enabled = setting.enabled().getAsBoolean();
        boolean rowHover = inside(mx, my, x - 4, y, w + 8, ROW_H) && inClip(mx, my);
        if (rowHover) {
            Theme.round(g, x - 4, y, w + 8, ROW_H, Theme.ROW_HOVER);
            if (mx < x + w - CONTROL_W - 4) tooltip = setting.tooltip();
        }
        String label = font.plainSubstrByWidth(setting.label().getString(), w - CONTROL_W - 8);
        g.text(font, label, x, y + 6, enabled ? Theme.TEXT : Theme.TEXT_FAINT);

        int right = x + w;
        int cy = y + 2;
        // Plain instanceof and casts: Java 8 (Minecraft before 1.17) has no pattern matching.
        if (setting instanceof Setting.Toggle) {
            Setting.Toggle t = (Setting.Toggle) setting;
            drawSwitch(g, "toggle:" + t.id(), right - 26, y + 4, 26, 12, t.get().getAsBoolean(), enabled,
                    accent, mx, my, () -> t.set().accept(!t.get().getAsBoolean()));
        } else if (setting instanceof Setting.Range) {
            drawSlider(g, (Setting.Range) setting, right - SLIDER_W, y, enabled, accent, mx, my);
        } else if (setting instanceof Setting.Choice<?>) {
            drawChoice(g, (Setting.Choice<?>) setting, right - CONTROL_W, cy, enabled, mx, my);
        } else if (setting instanceof Setting.Hotkey) {
            drawHotkey(g, ((Setting.Hotkey) setting).key().get(), right - CONTROL_W, cy, enabled, mx, my);
        }
        return y + ROW_H;
    }

    private void drawSwitch(Gfx g, String key, int x, int y, int w, int h, boolean on, boolean enabled,
                            int color, int mx, int my, Runnable toggle) {
        float t = animate(key, on ? 1 : 0);
        boolean hover = enabled && inside(mx, my, x, y, w, h) && inClip(mx, my);
        int track = Theme.lerpColor(t, Theme.TRACK, enabled ? color : Theme.TEXT_FAINT);
        if (hover) track = Theme.lerpColor(0.15f, track, 0xFFFFFFFF);
        Theme.pill(g, x, y, w, h, enabled ? track : Theme.withAlpha(track, 0.5f));
        int knob = h - 4;
        int kx = x + 2 + Math.round(t * (w - h));
        Theme.pill(g, kx, y + 2, knob, knob, enabled ? 0xFFFFFFFF : 0xFF8A909C);
        if (enabled) addHit(x, y, w, h, toggle);
    }

    private void drawSlider(Gfx g, Setting.Range r, int x, int y, boolean enabled, int accent, int mx, int my) {
        float f = (float) r.fraction();
        float shown = animate("slider:" + r.id(), f);
        String value = r.format().apply(r.get().getAsInt());
        g.text(font, value, x - 8 - font.width(value), y + 6, enabled ? Theme.TEXT_DIM : Theme.TEXT_FAINT);

        int trackY = y + 8;
        Theme.pill(g, x, trackY, SLIDER_W, 4, Theme.TRACK);
        int fill = Math.max(4, Math.round(SLIDER_W * shown));
        Theme.pill(g, x, trackY, fill, 4, enabled ? accent : Theme.TEXT_FAINT);
        boolean hover = enabled && inside(mx, my, x - 3, y + 2, SLIDER_W + 6, 16) && inClip(mx, my);
        int kx = x + Math.round((SLIDER_W - 6) * shown);
        Theme.round(g, kx, y + 5, 6, 10, !enabled ? Theme.TEXT_FAINT : hover || dragging != null ? 0xFFFFFFFF : 0xFFDDE3EC);
        if (enabled) {
            DoubleConsumer set = mouseX -> r.set().accept(r.valueAt((mouseX - x - 3) / (SLIDER_W - 6)));
            addClipped(clipped(x - 3, y + 2, SLIDER_W + 6, 16, null, set,
                    dir -> r.set().accept(Mth.clamp(r.get().getAsInt() + dir * r.step(), r.min(), r.max()))));
        }
    }

    private void drawChoice(Gfx g, Setting.Choice<?> c, int x, int y, boolean enabled, int mx, int my) {
        boolean hover = enabled && inside(mx, my, x, y, CONTROL_W, 16) && inClip(mx, my);
        Theme.roundBordered(g, x, y, CONTROL_W, 16, hover ? Theme.CONTROL_HOVER : Theme.CONTROL, Theme.DIVIDER);
        int textColor = enabled ? Theme.TEXT : Theme.TEXT_FAINT;
        g.centeredText(font, c.valueLabel(), x + CONTROL_W / 2, y + 4, textColor);
        g.text(font, "‹", x + 5, y + 4, enabled ? Theme.TEXT_DIM : Theme.TEXT_FAINT);
        g.text(font, "›", x + CONTROL_W - 9, y + 4, enabled ? Theme.TEXT_DIM : Theme.TEXT_FAINT);
        if (enabled) {
            addClipped(clipped(x, y, CONTROL_W / 2, 16, () -> c.cycle(-1), null, c::cycle));
            addClipped(clipped(x + CONTROL_W / 2, y, CONTROL_W - CONTROL_W / 2, 16, () -> c.cycle(1), null, c::cycle));
        }
    }

    private void drawHotkey(Gfx g, @Nullable KeyMapping key, int x, int y, boolean enabled, int mx, int my) {
        boolean waiting = key != null && listening == key;
        boolean hover = key != null && inside(mx, my, x, y, CONTROL_W, 16) && inClip(mx, my);
        Theme.roundBordered(g, x, y, CONTROL_W, 16, hover || waiting ? Theme.CONTROL_HOVER : Theme.CONTROL,
                waiting ? Theme.WARN : Theme.DIVIDER);
        Component text = waiting ? Compat.translatable(T + "press_key") : keyLabel(key, false);
        int color = waiting ? Theme.WARN : key == null || key.isUnbound() ? Theme.TEXT_FAINT : Theme.TEXT;
        g.centeredText(font, text, x + CONTROL_W / 2, y + 4, color);
        if (key != null && enabled) addHit(x, y, CONTROL_W, 16, () -> listening = waiting ? null : key);
    }

    private Component keyLabel(@Nullable KeyMapping key, boolean withPrefix) {
        Component name = key == null || key.isUnbound() ? Compat.translatable(T + "unbound") : key.getTranslatedKeyMessage();
        return withPrefix ? Compat.translatable(T + "key_prefix", name) : name;
    }

    // ------------------------------------------------------------------ input
    // Minecraft hands screens event objects from 1.21.9 and raw numbers before; both feed the same handlers below.

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return click(event.x(), event.y()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return drag(event.x()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        KeyMapping openKey = AutoClickerClient.openPanelKey();
        return key(event.key(), event.isEscape(), InputConstants.getKey(event), openKey != null && openKey.matches(event))
                || super.keyPressed(event);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return click(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        return drag(mouseX) || super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyMapping openKey = AutoClickerClient.openPanelKey();
        return key(keyCode, keyCode == Compat.KEY_ESCAPE, InputConstants.getKey(keyCode, scanCode),
                openKey != null && openKey.matches(keyCode, scanCode))
                || super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?}

    private boolean click(double x, double y) {
        if (listening != null) {
            listening = null; // clicking anywhere cancels key binding
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!hit.contains(x, y)) continue;
            if (hit.drag() != null) {
                dragging = hit.drag();
                dragging.accept(x);
            } else if (hit.click() != null) {
                hit.click().run();
            }
            playClick();
            return true;
        }
        return false;
    }

    private boolean drag(double x) {
        if (dragging == null) return false;
        dragging.accept(x);
        return true;
    }

    /** True if the panel used the key press. */
    private boolean key(int keyCode, boolean escape, InputConstants.Key pressed, boolean isOpenPanelKey) {
        if (listening != null) {
            listening.setKey(escape ? InputConstants.UNKNOWN : pressed);
            KeyMapping.resetMapping();
            minecraft.options.save();
            listening = null;
            playClick();
            return true;
        }
        if (isOpenPanelKey) {
            onClose();
            return true;
        }
        // Up/down arrows move between pages
        if (keyCode == Compat.KEY_DOWN || keyCode == Compat.KEY_UP) {
            int index = pages.indexOf(page) + (keyCode == Compat.KEY_DOWN ? 1 : -1);
            selectPage(pages.get(Math.floorMod(index, pages.size())));
            return true;
        }
        return false;
    }

    @Override
    //? if >=1.20.2 {
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
    //?} else
    /*public boolean mouseScrolled(double x, double y, double scrollY) {*/
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.wheel() != null && hit.contains(x, y)) {
                hit.wheel().accept(scrollY > 0 ? 1 : -1);
                return true;
            }
        }
        if (x >= sidebarX0 && x < sidebarX1 && y >= sidebarY0 && y < sidebarY1) {
            sidebarScroll = Mth.clamp(sidebarScroll - scrollY * 20, 0, maxSidebarScroll);
        } else {
            scroll = Mth.clamp(scroll - scrollY * 20, 0, maxScroll);
        }
        return true;
    }

    @Override
    public void onClose() {
        Compat.setScreen(minecraft, parent);
    }

    @Override
    public void removed() {
        SmartConfig.save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private void selectPage(Page p) {
        if (!p.equals(page)) {
            page = p;
            lastPage = p;
            scroll = 0;
        }
    }

    private void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private float animate(Object key, float target) {
        float current = anim.getOrDefault(key, target);
        float next = current + (target - current) * Math.min(1f, frameDelta * 16f);
        if (Math.abs(next - target) < 0.002f) next = target;
        anim.put(key, next);
        return next;
    }

    private static float ease(float t) {
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void setClip(int x0, int y0, int x1, int y1) {
        clipX0 = x0;
        clipY0 = y0;
        clipX1 = x1;
        clipY1 = y1;
    }

    private boolean inClip(double mx, double my) {
        return mx >= clipX0 && mx < clipX1 && my >= clipY0 && my < clipY1;
    }

    private void addHit(int x, int y, int w, int h, Runnable click) {
        Hit hit = clipped(x, y, w, h, click, null, null);
        if (hit != null) hits.add(hit);
    }

    private void addClipped(@Nullable Hit hit) {
        if (hit != null) hits.add(hit);
    }

    /** A hit region trimmed to the current clip, so controls scrolled out of view can't be clicked. */
    private @Nullable Hit clipped(int x, int y, int w, int h, @Nullable Runnable click, @Nullable DoubleConsumer drag,
                                  @Nullable IntConsumer wheel) {
        int x0 = Math.max(x, clipX0);
        int y0 = Math.max(y, clipY0);
        int x1 = Math.min(x + w, clipX1);
        int y1 = Math.min(y + h, clipY1);
        if (x0 >= x1 || y0 >= y1) return null;
        return new Hit(x0, y0, x1, y1, click, drag, wheel);
    }
}
