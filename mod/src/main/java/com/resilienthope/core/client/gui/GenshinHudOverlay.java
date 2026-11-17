package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.subtitle.RadioSubtitleManager;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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
        renderQuestTracker(poseStack, mc, player, screenWidth, screenHeight, font);
        renderInteractionPrompt(poseStack, mc, player, screenWidth, screenHeight, font);
        renderRadioDialogue(poseStack, mc, screenWidth, screenHeight, font);
    }

    private static void renderQuestTracker(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        String title = QuestManager.getQuestTitle();
        String objective = QuestManager.getObjectiveText();
        BlockPos target = QuestManager.getTargetPos();

        int startX = 16;
        int startY = 16;

        // Calculate distance to target
        double dist = Math.sqrt(player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5));
        String distStr = dist > 3.5 ? String.format("[ %.0fm ]", dist) : "[ 目标在此 ]";

        int textW = Math.max(font.width(title), font.width(objective) + font.width(distStr) + 8);
        int bgW = Math.max(textW + 24, 180);
        int bgH = 32;

        // Subtle Sci-Fi container
        GuiComponent.fill(poseStack, startX - 4, startY - 4, startX + bgW, startY + bgH, 0x8010151E);
        GuiComponent.fill(poseStack, startX - 4, startY - 4, startX - 2, startY + bgH, 0xFFFFCC00);

        // Yellow Diamond Icon & Title
        font.draw(poseStack, new TextComponent("◇ "), startX, startY, 0xFFFFCC00);
        font.draw(poseStack, new TextComponent(title), startX + 12, startY, 0xFFFFEEAA);

        // Objective & Distance
        font.draw(poseStack, new TextComponent(objective), startX + 12, startY + 14, 0xFFDDDDDD);
        font.draw(poseStack, new TextComponent(distStr), startX + 16 + font.width(objective), startY + 14, 0xFF4FC3F7);
    }

    private static void renderInteractionPrompt(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        if (mc.hitResult == null || mc.level == null) return;

        String promptLabel = null;

        if (mc.hitResult instanceof BlockHitResult blockHit) {
            if (player.getEyePosition().distanceTo(blockHit.getLocation()) <= 4.5) {
                BlockPos bPos = blockHit.getBlockPos();
                BlockState state = mc.level.getBlockState(bPos);
                if (state.is(ModBlocks.TELEPORT_WAYPOINT.get())) {
                    promptLabel = "星舰传送锚点 · 激活同步";
                } else if (state.is(ModBlocks.ENGINEERING_TERMINAL.get())) {
                    promptLabel = "工程维护主控终端 · 访问";
                } else if (state.is(Blocks.CHEST)) {
                    promptLabel = "冷冻物资箱 · 打开拾取";
                }
            }
        } else if (mc.hitResult instanceof EntityHitResult entityHit) {
            if (player.getEyePosition().distanceTo(entityHit.getLocation()) <= 4.5) {
                promptLabel = "交谈 · " + entityHit.getEntity().getName().getString();
            }
        }

        if (promptLabel != null) {
            int promptX = width / 2 + 25;
            int promptY = height / 2 - 10;
            int boxW = font.width(promptLabel) + 38;
            int boxH = 20;

            // Dark translucent capsule
            GuiComponent.fill(poseStack, promptX, promptY, promptX + boxW, promptY + boxH, 0xD0101824);
            GuiComponent.fill(poseStack, promptX, promptY, promptX + 2, promptY + boxH, 0xFF4A90E2);

            // [ F ] Key badge
            GuiComponent.fill(poseStack, promptX + 6, promptY + 3, promptX + 18, promptY + 17, 0x80000000);
            font.draw(poseStack, new TextComponent("F"), promptX + 8, promptY + 5, 0xFF55FFFF);

            // Action label
            font.draw(poseStack, new TextComponent(promptLabel), promptX + 24, promptY + 6, 0xFFFFFFFF);
        }
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
