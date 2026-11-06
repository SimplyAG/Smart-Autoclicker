package com.simplyag.autoclicker.ui;

import com.simplyag.autoclicker.AutoClickerClient;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
//? if >=1.16.2 {
import net.minecraft.util.FormattedCharSequence;
//?} else {
/*import com.simplyag.autoclicker.ui.legacy.FormattedCharSequence;
import java.util.ArrayList;
*///?}
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import org.lwjgl.opengl.GL11;
*///?}
//? if >=1.16 <1.20
/*import com.mojang.blaze3d.vertex.PoseStack;*/
//? if >=1.15 <1.20
/*import com.mojang.blaze3d.systems.RenderSystem;*/
//? if <1.15 {
/*import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
*///?}
//? if >=1.16 <1.16.2 {
/*import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
*///?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
*///?}

/**
 * Every drawing call the mod makes, on top of whichever GUI drawing API this Minecraft version has:
 * GuiGraphicsExtractor from 26.1, GuiGraphics from 1.20, a PoseStack with GuiComponent's static helpers from 1.16, and
 * the fixed-function matrix with string text before that. The screen, HUD and toast draw and measure text through this
 * only, so the differences between versions (method names, the pose stack, texture blits, tooltips, scissoring, the
 * text line type) live here and nowhere else.
 */
public final class Gfx {
    //? if >=26.1 {
    public final GuiGraphicsExtractor raw;

    public Gfx(GuiGraphicsExtractor raw) {
        this.raw = raw;
    }
    //?} else if >=1.20 {
    /*public final GuiGraphics raw;

    public Gfx(GuiGraphics raw) {
        this.raw = raw;
    }
    *///?} else if >=1.16 {
    /*public final PoseStack raw;

    public Gfx(PoseStack raw) {
        this.raw = raw;
    }
    *///?} else {
    /*// Before 1.16 GUI drawing has no pose: transforms go straight to the fixed-function matrix.
    public Gfx() {
    }
    *///?}

    //? if <1.20 {
    /*private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static Window window() {
        //? if >=1.15 {
        return mc().getWindow();
        //?} else
        /^return mc().window;^/
    }
    *///?}

    // ------------------------------------------------------------------ measuring text

    /** Width of text in pixels. */
    public static int width(Font font, Component text) {
        //? if >=1.16 {
        return font.width(text);
        //?} else
        /*return font.width(text.getColoredString());*/
    }

    public static int width(Font font, String text) {
        return font.width(text);
    }

    public static int width(Font font, FormattedCharSequence line) {
        //? if >=1.16.2 {
        return font.width(line);
        //?} else
        /*return font.width(line.text);*/
    }

    /** Wraps text into lines at most maxWidth pixels wide. */
    public static List<FormattedCharSequence> split(Font font, Component text, int maxWidth) {
        //? if >=1.16.2 {
        return font.split(text, maxWidth);
        //?} else if >=1.16 {
        /*List<FormattedCharSequence> lines = new ArrayList<>();
        for (FormattedText line : font.split(text, maxWidth)) lines.add(new FormattedCharSequence(line.getString()));
        return lines;
        *///?} else {
        /*List<FormattedCharSequence> lines = new ArrayList<>();
        for (String line : font.split(text.getColoredString(), maxWidth)) lines.add(new FormattedCharSequence(line));
        return lines;
        *///?}
    }

    /** The longest start of a string that fits in maxWidth pixels. */
    public static String trim(Font font, String text, int maxWidth) {
        //? if >=1.16 {
        return font.plainSubstrByWidth(text, maxWidth);
        //?} else
        /*return font.substrByWidth(text, maxWidth);*/
    }

    // ------------------------------------------------------------------ drawing

    public int guiWidth() {
        //? if >=1.20 {
        return raw.guiWidth();
        //?} else
        /*return window().getGuiScaledWidth();*/
    }

    public int guiHeight() {
        //? if >=1.20 {
        return raw.guiHeight();
        //?} else
        /*return window().getGuiScaledHeight();*/
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        //? if >=1.20 {
        raw.fill(x0, y0, x1, y1, color);
        //?} else if >=1.16 {
        /*GuiComponent.fill(raw, x0, y0, x1, y1, color);
        *///?} else
        /*GuiComponent.fill(x0, y0, x1, y1, color);*/
    }

    /** A 1px rectangle outline. */
    public void outline(int x, int y, int w, int h, int color) {
        fill(x, y, x + w, y + 1, color);
        fill(x, y + h - 1, x + w, y + h, color);
        fill(x, y + 1, x + 1, y + h - 1, color);
        fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Text without a shadow. */
    public void text(Font font, Component text, int x, int y, int color) {
        //? if >=26.1 {
        raw.text(font, text, x, y, color, false);
        //?} else if >=1.20 {
        /*raw.drawString(font, text, x, y, color, false);
        *///?} else if >=1.16 {
        /*font.draw(raw, text, x, y, color);
        *///?} else
        /*font.draw(text.getColoredString(), x, y, color);*/
    }

    public void text(Font font, String text, int x, int y, int color) {
        //? if >=26.1 {
        raw.text(font, text, x, y, color, false);
        //?} else if >=1.20 {
        /*raw.drawString(font, text, x, y, color, false);
        *///?} else if >=1.16 {
        /*font.draw(raw, text, x, y, color);
        *///?} else
        /*font.draw(text, x, y, color);*/
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if >=26.1 {
        raw.text(font, text, x, y, color, false);
        //?} else if >=1.20 {
        /*raw.drawString(font, text, x, y, color, false);
        *///?} else if >=1.16.2 {
        /*font.draw(raw, text, x, y, color);
        *///?} else if >=1.16 {
        /*font.draw(raw, text.text, x, y, color);
        *///?} else
        /*font.draw(text.text, x, y, color);*/
    }

    /** Centered text, with the shadow vanilla gives it. */
    public void centeredText(Font font, Component text, int centerX, int y, int color) {
        //? if >=26.1 {
        raw.centeredText(font, text, centerX, y, color);
        //?} else if >=1.20 {
        /*raw.drawCenteredString(font, text, centerX, y, color);
        *///?} else if >=1.16.2 {
        /*GuiComponent.drawCenteredString(raw, font, text, centerX, y, color);
        *///?} else if >=1.16 {
        /*font.drawShadow(raw, text, centerX - font.width(text) / 2f, y, color);
        *///?} else {
        /*String plain = text.getColoredString();
        font.drawShadow(plain, centerX - font.width(plain) / 2f, y, color);
        *///?}
    }

    /** An item icon without count or durability bar. */
    public void item(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        raw.fakeItem(stack, x, y);
        //?} else if >=1.20 {
        /*raw.renderFakeItem(stack, x, y);
        *///?} else if >=1.19.4 {
        /*mc().getItemRenderer().renderGuiItem(raw, stack, x, y);
        *///?} else if >=1.17 {
        /*// Item rendering ignores the pose here: carry the current translation and scale over to the model-view stack.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(raw.last().pose());
        RenderSystem.applyModelViewMatrix();
        mc().getItemRenderer().renderGuiItem(stack, x, y);
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        *///?} else if >=1.16 {
        /*// Items render with the fixed-function matrix: carry the pose over to it.
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(raw.last().pose());
        mc().getItemRenderer().renderGuiItem(stack, x, y);
        RenderSystem.popMatrix();
        *///?} else if >=1.15 {
        /*mc().getItemRenderer().renderGuiItem(stack, x, y);
        *///?} else {
        /*// 1.14 leaves item lighting to the caller; turn it off again so later fills and text aren't shaded.
        Lighting.turnOnGui();
        mc().getItemRenderer().renderGuiItem(stack, x, y);
        Lighting.turnOff();
        *///?}
    }

    /** The mod's 128x128 icon, scaled to size x size. */
    public void modIcon(int x, int y, int size) {
        //? if >=1.21.6 {
        raw.blit(RenderPipelines.GUI_TEXTURED, AutoClickerClient.ICON_TEXTURE, x, y, 0, 0, size, size, 128, 128, 128, 128);
        //?} else if >=1.21.2 {
        /*raw.blit(RenderType::guiTextured, AutoClickerClient.ICON_TEXTURE, x, y, 0, 0, size, size, 128, 128, 128, 128);
        *///?} else if >=1.20 {
        /*raw.blit(AutoClickerClient.ICON_TEXTURE, x, y, size, size, 0, 0, 128, 128, 128, 128);
        *///?} else if >=1.17 {
        /*RenderSystem.setShaderTexture(0, AutoClickerClient.ICON_TEXTURE);
        RenderSystem.enableBlend();
        GuiComponent.blit(raw, x, y, size, size, 0, 0, 128, 128, 128, 128);
        RenderSystem.disableBlend();
        *///?} else if >=1.16 {
        /*mc().getTextureManager().bind(AutoClickerClient.ICON_TEXTURE);
        RenderSystem.enableBlend();
        GuiComponent.blit(raw, x, y, size, size, 0, 0, 128, 128, 128, 128);
        RenderSystem.disableBlend();
        *///?} else if >=1.15 {
        /*mc().getTextureManager().bind(AutoClickerClient.ICON_TEXTURE);
        RenderSystem.enableBlend();
        GuiComponent.blit(x, y, size, size, 0, 0, 128, 128, 128, 128);
        RenderSystem.disableBlend();
        *///?} else {
        /*mc().getTextureManager().bind(AutoClickerClient.ICON_TEXTURE);
        GlStateManager.color4f(1, 1, 1, 1); // 1.14 fills leave their color set as the tint
        GlStateManager.enableBlend();
        GuiComponent.blit(x, y, size, size, 0, 0, 128, 128, 128, 128);
        GlStateManager.disableBlend();
        *///?}
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        //? if >=1.20 {
        raw.enableScissor(x0, y0, x1, y1);
        //?} else {
        /*// GUI coordinates to window pixels; OpenGL counts y from the bottom.
        double scale = window().getGuiScale();
        int windowHeight = window().getHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x0 * scale), (int) (windowHeight - y1 * scale),
                Math.max(0, (int) ((x1 - x0) * scale)), Math.max(0, (int) ((y1 - y0) * scale)));
        *///?}
    }

    public void disableScissor() {
        //? if >=1.20 {
        raw.disableScissor();
        //?} else
        /*GL11.glDisable(GL11.GL_SCISSOR_TEST);*/
    }

    public void push() {
        //? if >=1.21.6 {
        raw.pose().pushMatrix();
        //?} else if >=1.20 {
        /*raw.pose().pushPose();
        *///?} else if >=1.16 {
        /*raw.pushPose();
        *///?} else if >=1.15 {
        /*RenderSystem.pushMatrix();
        *///?} else
        /*GlStateManager.pushMatrix();*/
    }

    public void translate(float x, float y) {
        //? if >=1.21.6 {
        raw.pose().translate(x, y);
        //?} else if >=1.20 {
        /*raw.pose().translate(x, y, 0);
        *///?} else if >=1.16 {
        /*raw.translate(x, y, 0);
        *///?} else if >=1.15 {
        /*RenderSystem.translatef(x, y, 0);
        *///?} else
        /*GlStateManager.translatef(x, y, 0);*/
    }

    public void scale(float factor) {
        //? if >=1.21.6 {
        raw.pose().scale(factor, factor);
        //?} else if >=1.20 {
        /*raw.pose().scale(factor, factor, 1);
        *///?} else if >=1.16 {
        /*raw.scale(factor, factor, 1);
        *///?} else if >=1.15 {
        /*RenderSystem.scalef(factor, factor, 1);
        *///?} else
        /*GlStateManager.scalef(factor, factor, 1);*/
    }

    public void pop() {
        //? if >=1.21.6 {
        raw.pose().popMatrix();
        //?} else if >=1.20 {
        /*raw.pose().popPose();
        *///?} else if >=1.16 {
        /*raw.popPose();
        *///?} else if >=1.15 {
        /*RenderSystem.popMatrix();
        *///?} else
        /*GlStateManager.popMatrix();*/
    }

    /** A tooltip at the mouse. Call last in the frame: before 1.21.6 it is drawn right away, over what came before. */
    public void tooltip(Font font, List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        //? if >=1.21.6 {
        raw.setTooltipForNextFrame(lines, mouseX, mouseY);
        //?} else if >=1.20 {
        /*raw.renderTooltip(font, lines, mouseX, mouseY);
        *///?} else if >=1.16.2 {
        /*if (mc().screen != null) mc().screen.renderTooltip(raw, lines, mouseX, mouseY);
        *///?} else if >=1.16 {
        /*List<FormattedText> text = new ArrayList<>();
        for (FormattedCharSequence line : lines) text.add(new TextComponent(line.text));
        if (mc().screen != null) mc().screen.renderTooltip(raw, text, mouseX, mouseY);
        *///?} else {
        /*List<String> text = new ArrayList<>();
        for (FormattedCharSequence line : lines) text.add(line.text);
        if (mc().screen != null) mc().screen.renderTooltip(text, mouseX, mouseY);
        *///?}
    }
}
