package com.resilienthope.core.subtitle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.client.gui.GenshinHudOverlay;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;

/**
 * Overwriting lightweight toast notification for progression, equipment, party, and builder actions.
 * Never queues or piles up: newer messages immediately replace older ones, and identical messages increment repeat count.
 */
public final class SystemToastManager {
    private static final int DEFAULT_DURATION_TICKS = 45;

    private static String title = "";
    private static String text = "";
    private static int accentColor = 0xFF38BDF8;
    private static int remainingTicks = 0;
    private static int maxTicks = DEFAULT_DURATION_TICKS;
    private static int repeatCount = 1;

    private SystemToastManager() {}

    public static void show(String newTitle, String newText) {
        if (newText == null || newText.isEmpty()) return;
        String resolvedTitle = (newTitle != null && !newTitle.isEmpty()) ? newTitle : "系统提示";
        if (remainingTicks > 0 && newText.equals(text) && resolvedTitle.equals(title)) {
            repeatCount = Math.min(999, repeatCount + 1);
            remainingTicks = DEFAULT_DURATION_TICKS;
            maxTicks = DEFAULT_DURATION_TICKS;
            return;
        }
        title = resolvedTitle;
        text = newText;
        accentColor = resolveAccentColor(resolvedTitle, newText);
        remainingTicks = DEFAULT_DURATION_TICKS;
        maxTicks = DEFAULT_DURATION_TICKS;
        repeatCount = 1;
    }

    private static int resolveAccentColor(String t, String body) {
        if (t.contains("不足") || t.contains("限制") || body.contains("不足")) return 0xFFF87171;
        if (t.contains("成功") || t.contains("突破") || t.contains("造景") || body.contains("升至")) return 0xFF4ADE80;
        if (t.contains("溶解") || t.contains("装备") || t.contains("养成")) return 0xFFFBBF24;
        return 0xFF38BDF8;
    }

    public static void clientTick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                clear();
            }
        }
    }

    public static void clear() {
        remainingTicks = 0;
        title = "";
        text = "";
        repeatCount = 1;
    }

    public static void render(PoseStack ps, Font font, int screenWidth, int screenHeight) {
        if (remainingTicks <= 0 || text.isEmpty() || font == null) return;

        float fade = remainingTicks < 10 ? Mth.clamp(remainingTicks / 10.0F, 0.08F, 1.0F) : 1.0F;
        int alpha = ((int) (0xDC * fade)) & 0xFF;
        int borderAlpha = ((int) (0xFF * fade)) & 0xFF;
        int bgCol = (alpha << 24) | 0x0A121E;
        int borderCol = (borderAlpha << 24) | (accentColor & 0x00FFFFFF);

        String display = "【" + title + "】" + text + (repeatCount > 1 ? (" x" + repeatCount) : "");
        int maxW = Math.max(120, screenWidth - 40);
        while (font.width(display) > maxW - 16 && display.length() > 8) {
            display = display.substring(0, display.length() - 2) + "…";
        }
        int boxW = Math.min(maxW, font.width(display) + 18);
        int boxH = 18;
        int x = (screenWidth - boxW) / 2;
        int y = Math.min(screenHeight - 24, 14);

        GenshinHudOverlay.drawVoxelPanel(ps, x, y, boxW, boxH, bgCol, borderCol, 0xFFFBBF24);
        int textAlpha = ((int) (0xFF * fade)) & 0xFF;
        font.drawShadow(ps, new TextComponent(display), x + 9, y + 5, (textAlpha << 24) | 0xF8FAFC);
    }
}
