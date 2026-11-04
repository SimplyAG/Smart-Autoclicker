package com.simplyag.autoclicker.ui;

import com.simplyag.autoclicker.AutoClickerClient;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
*///?}

/**
 * Every drawing call the mod makes, on top of whichever GUI graphics class this Minecraft version has
 * (GuiGraphicsExtractor from 26.1, GuiGraphics before). The screen, HUD and toast draw through this only, so the
 * differences between versions (method names, the pose stack, texture blits, tooltips) live here and in nowhere else.
 */
public final class Gfx {
    //? if >=26.1 {
    public final GuiGraphicsExtractor raw;

    public Gfx(GuiGraphicsExtractor raw) {
        this.raw = raw;
    }
    //?} else {
    /*public final GuiGraphics raw;

    public Gfx(GuiGraphics raw) {
        this.raw = raw;
    }
    *///?}

    public int guiWidth() {
        return raw.guiWidth();
    }

    public int guiHeight() {
        return raw.guiHeight();
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        raw.fill(x0, y0, x1, y1, color);
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
        //?} else
        /*raw.drawString(font, text, x, y, color, false);*/
    }

    public void text(Font font, String text, int x, int y, int color) {
        //? if >=26.1 {
        raw.text(font, text, x, y, color, false);
        //?} else
        /*raw.drawString(font, text, x, y, color, false);*/
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if >=26.1 {
        raw.text(font, text, x, y, color, false);
        //?} else
        /*raw.drawString(font, text, x, y, color, false);*/
    }

    /** Centered text, with the shadow vanilla gives it. */
    public void centeredText(Font font, Component text, int centerX, int y, int color) {
        //? if >=26.1 {
        raw.centeredText(font, text, centerX, y, color);
        //?} else
        /*raw.drawCenteredString(font, text, centerX, y, color);*/
    }

    /** An item icon without count or durability bar. */
    public void item(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        raw.fakeItem(stack, x, y);
        //?} else
        /*raw.renderFakeItem(stack, x, y);*/
    }

    /** The mod's 128x128 icon, scaled to size x size. */
    public void modIcon(int x, int y, int size) {
        //? if >=1.21.6 {
        raw.blit(RenderPipelines.GUI_TEXTURED, AutoClickerClient.ICON_TEXTURE, x, y, 0, 0, size, size, 128, 128, 128, 128);
        //?} else if >=1.21.2 {
        /*raw.blit(RenderType::guiTextured, AutoClickerClient.ICON_TEXTURE, x, y, 0, 0, size, size, 128, 128, 128, 128);
        *///?} else
        /*raw.blit(AutoClickerClient.ICON_TEXTURE, x, y, size, size, 0, 0, 128, 128, 128, 128);*/
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        raw.enableScissor(x0, y0, x1, y1);
    }

    public void disableScissor() {
        raw.disableScissor();
    }

    public void push() {
        //? if >=1.21.6 {
        raw.pose().pushMatrix();
        //?} else
        /*raw.pose().pushPose();*/
    }

    public void translate(float x, float y) {
        //? if >=1.21.6 {
        raw.pose().translate(x, y);
        //?} else
        /*raw.pose().translate(x, y, 0);*/
    }

    public void scale(float factor) {
        //? if >=1.21.6 {
        raw.pose().scale(factor, factor);
        //?} else
        /*raw.pose().scale(factor, factor, 1);*/
    }

    public void pop() {
        //? if >=1.21.6 {
        raw.pose().popMatrix();
        //?} else
        /*raw.pose().popPose();*/
    }

    /** A tooltip at the mouse. Call last in the frame: before 1.21.6 it is drawn right away, over what came before. */
    public void tooltip(Font font, List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        //? if >=1.21.6 {
        raw.setTooltipForNextFrame(lines, mouseX, mouseY);
        //?} else
        /*raw.renderTooltip(font, lines, mouseX, mouseY);*/
    }
}
