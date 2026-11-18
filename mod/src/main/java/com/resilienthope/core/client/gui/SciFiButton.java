package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Custom sci-fi styled button replacing vanilla cobblestone Minecraft buttons.
 */
public class SciFiButton extends Button {
    private int accentColor = 0xFF38BDF8;

    public SciFiButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
    }

    public SciFiButton(int x, int y, int width, int height, Component message, OnPress onPress, int accentColor) {
        super(x, y, width, height, message, onPress);
        this.accentColor = accentColor;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    @Override
    public void renderButton(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = this.active && this.isHoveredOrFocused();
        int bgCol = !this.active ? 0xA0101620 : (hovered ? 0xEA1D3854 : 0xD00D1826);
        int borderCol = !this.active ? 0xFF283444 : (hovered ? 0xFFFBBF24 : this.accentColor);
        int cornerCol = !this.active ? 0xFF334155 : (hovered ? 0xFFFFFFFF : 0xFFFBBF24);

        GenshinHudOverlay.drawVoxelPanel(ps, this.x, this.y, this.width, this.height, bgCol, borderCol, cornerCol);

        // Left tech accent strip
        if (this.active) {
            fill(ps, this.x + 1, this.y + 3, this.x + 3, this.y + this.height - 3, hovered ? 0xFFFBBF24 : this.accentColor);
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        String text = this.getMessage().getString();
        int textColor = !this.active ? 0xFF64748B : (hovered ? 0xFFFFF7D6 : 0xFFF1F5F9);
        int textW = font.width(text);
        float maxAvail = Math.max(8.0f, this.width - 8.0f);
        float scale = textW > maxAvail ? (maxAvail / textW) : 1.0f;

        ps.pushPose();
        ps.translate(this.x + this.width / 2.0f, this.y + (this.height - 8 * scale) / 2.0f, 0);
        ps.scale(scale, scale, 1.0f);
        font.drawShadow(ps, this.getMessage(), -textW / 2.0f, 0, textColor);
        ps.popPose();
    }
}
