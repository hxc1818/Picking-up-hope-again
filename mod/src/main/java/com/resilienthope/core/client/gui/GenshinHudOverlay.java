package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.subtitle.RadioSubtitleManager;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GenshinHudOverlay {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RadioSubtitleManager.clientTick();
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGameOverlayEvent.PreLayer event) {
        IIngameOverlay overlay = event.getOverlay();
        // Fully cancel vanilla survival hotbar and vanilla status elements
        if (overlay == ForgeIngameGui.HOTBAR_ELEMENT ||
            overlay == ForgeIngameGui.PLAYER_HEALTH_ELEMENT ||
            overlay == ForgeIngameGui.FOOD_LEVEL_ELEMENT ||
            overlay == ForgeIngameGui.EXPERIENCE_BAR_ELEMENT ||
            overlay == ForgeIngameGui.ARMOR_LEVEL_ELEMENT ||
            overlay == ForgeIngameGui.MOUNT_HEALTH_ELEMENT ||
            overlay == ForgeIngameGui.ITEM_NAME_ELEMENT) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        PoseStack poseStack = event.getMatrixStack();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        renderGenshinPartySidebar(poseStack, mc, player, screenWidth, screenHeight, font);
        renderRightBottomSkillRing(poseStack, mc, screenWidth, screenHeight, font);
        renderRadioDialogue(poseStack, mc, screenWidth, screenHeight, font);
    }

    private static void renderGenshinPartySidebar(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            int startX = width - 130;
            int startY = height / 2 - 80;

            for (int i = 0; i < 4; i++) {
                CharacterData character = team.getCharacter(i);
                boolean isActive = (i == team.getActiveSlotIndex());
                int itemY = startY + i * 38;

                // Frame Background
                int bgColor = isActive ? 0x902A3B4C : 0x6010151C;
                GuiComponent.fill(poseStack, startX, itemY, startX + 124, itemY + 34, bgColor);
                if (isActive) {
                    GuiComponent.fill(poseStack, startX, itemY, startX + 3, itemY + 34, 0xFF4A90E2);
                }

                // Slot Key Number
                String keyStr = String.valueOf(i + 1);
                GuiComponent.fill(poseStack, startX + 5, itemY + 5, startX + 17, itemY + 17, 0x80000000);
                font.draw(poseStack, new TextComponent(keyStr), startX + 8, itemY + 7, isActive ? 0xFFFFFF : 0xAAAAAA);

                // Character Name
                String name = character.getName();
                font.draw(poseStack, new TextComponent(name), startX + 22, itemY + 6, isActive ? 0xFFEEAA : 0xDDDDDD);

                // HP Bar
                int maxHp = Math.max(1, (int) character.getMaxHp());
                int curHp = Math.max(0, (int) character.getCurrentHp());
                float hpPercent = Math.min(1.0f, (float) curHp / maxHp);

                int barWidth = 75;
                int barX = startX + 22;
                int barY = itemY + 18;
                GuiComponent.fill(poseStack, barX, barY, barX + barWidth, barY + 5, 0x80333333);
                int hpColor = hpPercent > 0.3f ? 0xFF4CAF50 : 0xFFE53935;
                GuiComponent.fill(poseStack, barX, barY, barX + (int)(barWidth * hpPercent), barY + 5, hpColor);

                // Energy Status indicator
                boolean burstReady = character.getCurrentEnergy() >= character.getMaxEnergy();
                int energyColor = burstReady ? 0xFFFFD700 : 0x66888888;
                GuiComponent.fill(poseStack, startX + 104, itemY + 10, startX + 118, itemY + 24, energyColor);
                if (burstReady) {
                    font.draw(poseStack, new TextComponent("Q"), startX + 108, itemY + 13, 0x000000);
                }
            }
        });
    }

    private static void renderRightBottomSkillRing(PoseStack poseStack, Minecraft mc, int width, int height, Font font) {
        int eX = width - 85;
        int eY = height - 45;
        int qX = width - 42;
        int qY = height - 45;

        // E Skill Box
        GuiComponent.fill(poseStack, eX, eY, eX + 32, eY + 32, 0x901B2A38);
        GuiComponent.fill(poseStack, eX + 2, eY + 2, eX + 30, eY + 30, 0x60000000);
        font.draw(poseStack, new TextComponent("E"), eX + 12, eY + 11, 0x55FFFF);

        // Q Burst Box
        GuiComponent.fill(poseStack, qX, qY, qX + 32, qY + 32, 0x90382A1B);
        GuiComponent.fill(poseStack, qX + 2, qY + 2, qX + 30, qY + 30, 0x80FFAA00);
        font.draw(poseStack, new TextComponent("Q"), qX + 12, qY + 11, 0xFFFFFF);
    }

    private static void renderRadioDialogue(PoseStack poseStack, Minecraft mc, int width, int height, Font font) {
        if (!RadioSubtitleManager.hasActiveDialogue()) return;

        String speaker = RadioSubtitleManager.getCurrentSpeaker();
        String text = RadioSubtitleManager.getCurrentText();

        int textWidth = font.width(text);
        int boxWidth = Math.max(textWidth + 30, 240);
        int boxHeight = 36;
        int startX = (width - boxWidth) / 2;
        int startY = height - 80;

        // Semi-transparent Sci-Fi backdrop
        GuiComponent.fill(poseStack, startX, startY, startX + boxWidth, startY + boxHeight, 0xCC0B121A);
        GuiComponent.fill(poseStack, startX, startY, startX + 2, startY + boxHeight, 0xFF3880FF);

        // Speaker tag
        font.draw(poseStack, new TextComponent("【" + speaker + "】"), startX + 8, startY + 5, 0xFF4FC3F7);
        // Dialogue content
        font.draw(poseStack, new TextComponent(text), startX + 12, startY + 18, 0xFFF0F0F0);
    }
}
