package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.DamagePopupManager;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.subtitle.RadioSubtitleManager;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class GenshinHudOverlay {

    private static final ResourceLocation TEX_PORTRAIT_LOCKED = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_locked.png");
    private static int beaconTickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        RadioSubtitleManager.clientTick();
        DamagePopupManager.clientTick();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && !mc.isPaused() && !(mc.screen instanceof TurnBasedBattleScreen)) {
            beaconTickCounter++;
            if (beaconTickCounter % 5 == 0) {
                BlockPos target = QuestManager.getEffectiveBreadcrumbPos(mc.player);
                if (target != null && target.distSqr(mc.player.blockPosition()) < 2500) {
                    for (int i = 0; i < 3; i++) {
                        double py = target.getY() + 0.8 + i * 0.75;
                        mc.level.addParticle(ParticleTypes.END_ROD,
                                target.getX() + 0.5, py, target.getZ() + 0.5,
                                0.0, 0.04, 0.0);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGameOverlayEvent.PreLayer event) {
        IIngameOverlay overlay = event.getOverlay();
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
        if (mc.screen instanceof TurnBasedBattleScreen) return;

        PoseStack poseStack = event.getMatrixStack();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        renderDamagePopups(poseStack, mc, screenWidth, screenHeight, font);
        renderInWorldQuestMarker(poseStack, mc, player, screenWidth, screenHeight, font);
        renderQuestTracker(poseStack, mc, player, screenWidth, screenHeight, font);
        renderMiniRadar(poseStack, mc, player, screenWidth, screenHeight, font);
        renderPartySidebar(poseStack, player, screenWidth, screenHeight, font);
        renderBottomCenterPlayerStatus(poseStack, player, screenWidth, screenHeight, font);
        renderOverworldEncounterTip(poseStack, screenWidth, screenHeight, font);
        renderInteractionPrompt(poseStack, mc, player, screenWidth, screenHeight, font);
        if (!(mc.screen instanceof NpcDialogueScreen)) {
            renderRadioDialogue(poseStack, screenWidth, screenHeight, font);
        }
    }

    public static void drawVoxelPanel(PoseStack ps, int x, int y, int w, int h, int bgCol, int borderCol, int cornerCol) {
        GuiComponent.fill(ps, x, y, x + w, y + h, bgCol);
        GuiComponent.fill(ps, x, y, x + w, y + 1, borderCol);
        GuiComponent.fill(ps, x, y + h - 1, x + w, y + h, borderCol);
        GuiComponent.fill(ps, x, y, x + 1, y + h, borderCol);
        GuiComponent.fill(ps, x + w - 1, y, x + w, y + h, borderCol);
        GuiComponent.fill(ps, x, y, x + 3, y + 2, cornerCol);
        GuiComponent.fill(ps, x + w - 3, y, x + w, y + 2, cornerCol);
        GuiComponent.fill(ps, x, y + h - 2, x + 3, y + h, cornerCol);
        GuiComponent.fill(ps, x + w - 3, y + h - 2, x + w, y + h, cornerCol);
    }

    public static void drawTexture(PoseStack ps, ResourceLocation tex, int x, int y, int dstW, int dstH, int srcW, int srcH) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, tex);
        GuiComponent.blit(ps, x, y, dstW, dstH, 0.0F, 0.0F, srcW, srcH, srcW, srcH);
    }

    public static void drawPortrait(PoseStack ps, ResourceLocation tex, int x, int y, int dstW, int dstH) {
        drawTexture(ps, tex, x, y, dstW, dstH, 64, 64);
    }

    private static Vec3 viewVector(float xRotDeg, float yRotDeg) {
        float pitchRad = xRotDeg * ((float) Math.PI / 180.0F);
        float yawRad = -yRotDeg * ((float) Math.PI / 180.0F);
        float cosY = Mth.cos(yawRad);
        float sinY = Mth.sin(yawRad);
        float cosP = Mth.cos(pitchRad);
        float sinP = Mth.sin(pitchRad);
        return new Vec3(sinY * cosP, -sinP, cosY * cosP);
    }

    private static int[] projectWorldToScreen(Minecraft mc, double wx, double wy, double wz, int width, int height) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        double dx = wx - camPos.x;
        double dy = wy - camPos.y;
        double dz = wz - camPos.z;

        float pitch = camera.getXRot();
        float yaw = camera.getYRot();
        Vec3 fwd = viewVector(pitch, yaw);
        Vec3 up = viewVector(pitch - 90.0f, yaw);
        Vec3 right = fwd.cross(up).normalize();

        double forwardDot = dx * fwd.x + dy * fwd.y + dz * fwd.z;
        double rightDot = dx * right.x + dy * right.y + dz * right.z;
        double upDot = dx * up.x + dy * up.y + dz * up.z;

        double fovDeg = Math.max(30.0, Math.min(115.0, mc.options.fov));
        double halfH = Math.tan(Math.toRadians(fovDeg) / 2.0);
        double halfW = halfH * ((double) width / Math.max(1, height));

        if (forwardDot > 0.08) {
            int sx = (int) ((width / 2.0) * (1.0 + (rightDot / forwardDot) / halfW));
            int sy = (int) ((height / 2.0) * (1.0 - (upDot / forwardDot) / halfH));
            return new int[]{sx, sy, 1};
        } else {
            double len = Math.sqrt(rightDot * rightDot + upDot * upDot);
            if (len < 1e-4) {
                return new int[]{width / 2, height - 56, 0};
            }
            double nx = rightDot / len;
            double ny = -upDot / len;
            double rx = (width / 2.0 - 30.0) / Math.max(0.01, Math.abs(nx));
            double ry = (height / 2.0 - 50.0) / Math.max(0.01, Math.abs(ny));
            double scale = Math.min(rx, ry);
            int sx = (int) (width / 2.0 + nx * scale);
            int sy = (int) (height / 2.0 + ny * scale);
            return new int[]{sx, sy, 0};
        }
    }

    private static void renderInWorldQuestMarker(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        BlockPos target = QuestManager.getEffectiveBreadcrumbPos(player);
        if (target == null) return;

        double dist = Math.sqrt(player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5));
        int[] proj = projectWorldToScreen(mc, target.getX() + 0.5, target.getY() + 1.5, target.getZ() + 0.5, width, height);

        int rawX = proj[0];
        int rawY = proj[1];
        boolean inFront = proj[2] == 1;
        boolean offScreen = !inFront || rawX < 28 || rawX > width - 28 || rawY < 42 || rawY > height - 56;

        int clampedX = Math.max(28, Math.min(width - 28, rawX));
        int clampedY = Math.max(42, Math.min(height - 56, rawY));

        String distStr = dist < 2.5 ? "到达" : String.format("%.0fm", dist);
        int boxW = Math.max(28, font.width(distStr) + 10);

        drawVoxelPanel(poseStack, clampedX - boxW / 2, clampedY - 12, boxW, 22,
                0xC00B131D, offScreen ? 0xFFFF8A00 : 0xFFFBBF24, 0xFF38BDF8);

        String icon = offScreen ? (clampedX <= 30 ? "◀◈" : (clampedX >= width - 30 ? "◈▶" : "◈")) : "◈";
        font.draw(poseStack, new TextComponent(icon), clampedX - font.width(icon) / 2, clampedY - 9, 0xFFFBBF24);
        font.draw(poseStack, new TextComponent(distStr), clampedX - font.width(distStr) / 2, clampedY + 1, 0xFF7DD3FC);
    }

    private static void renderDamagePopups(PoseStack poseStack, Minecraft mc, int width, int height, Font font) {
        for (DamagePopupManager.Popup p : DamagePopupManager.getPopups()) {
            float progress = (float) p.ageTicks / (float) p.maxTicks;
            double wy = p.worldY + progress * 0.9;
            int[] proj = projectWorldToScreen(mc, p.worldX, wy, p.worldZ, width, height);
            if (proj[2] == 0) continue;

            int sx = proj[0];
            int sy = proj[1];
            if (sx < 10 || sx > width - 10 || sy < 10 || sy > height - 10) continue;

            String label = p.isCrit ? ("✦ " + p.amount) : String.valueOf(p.amount);
            int color = p.isCrit ? 0xFFFBBF24 : (p.isSkill ? 0xFF38BDF8 : 0xFFFFFFFF);
            int lw = font.width(label);

            poseStack.pushPose();
            float scale = p.isCrit ? 1.25f : 1.0f;
            poseStack.translate(sx, sy, 0);
            poseStack.scale(scale, scale, 1.0f);
            font.drawShadow(poseStack, new TextComponent(label), -lw / 2.0f, 0, color);
            poseStack.popPose();
        }
    }

    private static void renderMiniRadar(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        int radius = 26;
        int radarX = width - 34;
        int radarY = 34;

        drawVoxelPanel(poseStack, radarX - radius, radarY - radius, radius * 2, radius * 2,
                0xBA091018, 0xFF2E5C88, 0xFF38BDF8);
        GuiComponent.fill(poseStack, radarX - radius + 2, radarY, radarX + radius - 2, radarY + 1, 0x3038BDF8);
        GuiComponent.fill(poseStack, radarX, radarY - radius + 2, radarX + 1, radarY + radius - 2, 0x3038BDF8);

        double yawRad = Math.toRadians(player.getYRot());
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        if (mc.level != null) {
            BlockPos pPos = player.blockPosition();
            for (int ox = -10; ox <= 10; ox++) {
                for (int oz = -10; oz <= 10; oz++) {
                    BlockPos check = pPos.offset(ox, 1, oz);
                    BlockState bs = mc.level.getBlockState(check);
                    if (!bs.isAir() && bs.getMaterial().isSolid()) {
                        double lr = ox * rightX + oz * rightZ;
                        double lf = ox * fwdX + oz * fwdZ;
                        int px = radarX + (int) (lr * 2.0);
                        int py = radarY - (int) (lf * 2.0);
                        if (Math.abs(px - radarX) < radius - 3 && Math.abs(py - radarY) < radius - 3) {
                            GuiComponent.fill(poseStack, px, py, px + 2, py + 2, 0x664A709A);
                        }
                    }
                }
            }

            AABB scanBox = new AABB(pPos).inflate(16.0);
            for (Monster m : mc.level.getEntitiesOfClass(Monster.class, scanBox)) {
                double dx = m.getX() - player.getX();
                double dz = m.getZ() - player.getZ();
                double lr = dx * rightX + dz * rightZ;
                double lf = dx * fwdX + dz * fwdZ;
                int ex = Mth.clamp(radarX + (int) (lr * 1.4), radarX - radius + 3, radarX + radius - 4);
                int ey = Mth.clamp(radarY - (int) (lf * 1.4), radarY - radius + 3, radarY + radius - 4);
                GuiComponent.fill(poseStack, ex - 1, ey - 1, ex + 2, ey + 2, 0xFFFF3B3B);
            }
            for (FactionNpcEntity npc : mc.level.getEntitiesOfClass(FactionNpcEntity.class, scanBox)) {
                double dx = npc.getX() - player.getX();
                double dz = npc.getZ() - player.getZ();
                double lr = dx * rightX + dz * rightZ;
                double lf = dx * fwdX + dz * fwdZ;
                int ex = Mth.clamp(radarX + (int) (lr * 1.4), radarX - radius + 3, radarX + radius - 4);
                int ey = Mth.clamp(radarY - (int) (lf * 1.4), radarY - radius + 3, radarY + radius - 4);
                GuiComponent.fill(poseStack, ex - 1, ey - 1, ex + 2, ey + 2, 0xFF40FFCC);
            }
        }

        double nr = -rightZ;
        double nf = -fwdZ;
        int nX = radarX + (int) (nr * (radius - 6));
        int nY = radarY - (int) (nf * (radius - 6));
        font.draw(poseStack, new TextComponent("N"), nX - 2, nY - 4, 0xFF38BDF8);

        BlockPos target = QuestManager.getEffectiveBreadcrumbPos(player);
        if (target != null) {
            double dx = (target.getX() + 0.5) - player.getX();
            double dz = (target.getZ() + 0.5) - player.getZ();
            double lr = dx * rightX + dz * rightZ;
            double lf = dx * fwdX + dz * fwdZ;
            double len = Math.sqrt(lr * lr + lf * lf);
            double maxDist = radius - 6;
            double scale = len > 14.0 ? (maxDist / Math.max(0.01, len)) : 1.4;
            int tx = Mth.clamp(radarX + (int) (lr * scale), radarX - radius + 4, radarX + radius - 4);
            int ty = Mth.clamp(radarY - (int) (lf * scale), radarY - radius + 4, radarY + radius - 4);
            font.draw(poseStack, new TextComponent("◆"), tx - 3, ty - 4, 0xFFFBBF24);
        }

        GuiComponent.fill(poseStack, radarX - 2, radarY - 2, radarX + 2, radarY + 2, 0xFFFFFFFF);
    }

    private static void renderQuestTracker(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        String title = QuestManager.getQuestTitle();
        String objective = QuestManager.getObjectiveText();
        BlockPos target = QuestManager.getEffectiveBreadcrumbPos(player);
        String hint = QuestManager.getBreadcrumbHint(player);

        double dist = target != null ? Math.sqrt(player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5)) : 0;
        String distStr = dist > 3.2 ? String.format(" [%.0fm]", dist) : " [到达]";
        String fullObj = objective + hint + distStr;

        int startX = 10;
        int startY = 8;
        int maxAllowedW = Math.max(160, width - 90);
        List<FormattedCharSequence> lines = font.split(new TextComponent(fullObj), maxAllowedW - 18);
        int bgW = Math.min(maxAllowedW, Math.max(font.width(title) + 28, Math.min(font.width(fullObj) + 22, maxAllowedW)));
        int bgH = 20 + Math.min(lines.size(), 2) * 11;

        drawVoxelPanel(poseStack, startX, startY, bgW, bgH, 0xB80D141F, 0xFF284664, 0xFFFBBF24);
        GuiComponent.fill(poseStack, startX, startY, startX + 3, startY + bgH, 0xFFFBBF24);

        font.draw(poseStack, new TextComponent("◈ " + title), startX + 8, startY + 5, 0xFFFBBF24);
        int oy = startY + 17;
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            font.draw(poseStack, lines.get(i), startX + 10, oy, 0xFFE6EEF5);
            oy += 11;
        }

        String keys = "[B]背包 [C]角色 [E]副本 [G]抽卡 [F]交互";
        int kw = Math.min(maxAllowedW, font.width(keys) + 12);
        drawVoxelPanel(poseStack, startX, startY + bgH + 3, kw, 14, 0x90091018, 0xFF20344A, 0xFF38BDF8);
        font.draw(poseStack, new TextComponent(keys), startX + 6, startY + bgH + 6, 0xFF9EC5E8);
    }

    private static void renderPartySidebar(PoseStack poseStack, Player player, int width, int height, Font font) {
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            int cardW = 114;
            int cardH = 28;
            int startX = width - cardW - 8;
            int startY = Math.max(66, height / 2 - 58);

            for (int i = 0; i < 4; i++) {
                CharacterData character = team.getCharacter(i);
                boolean unlocked = character.isUnlocked();
                boolean isActive = (i == team.getActiveSlotIndex());
                int itemY = startY + i * (cardH + 3);
                if (itemY + cardH > height - 28) break;

                int elemCol = unlocked ? CharacterData.getElementColor(character.getElement()) : 0xFF243548;
                int bgCol = isActive ? 0xC81C2E42 : (unlocked ? 0x90101824 : 0x600B0F16);
                int borderCol = isActive ? 0xFFFBBF24 : elemCol;

                drawVoxelPanel(poseStack, startX, itemY, cardW, cardH, bgCol, borderCol, borderCol);

                ResourceLocation portrait = unlocked ? TurnBasedBattleScreen.getPortraitForChar(character.getCharId()) : TEX_PORTRAIT_LOCKED;
                drawPortrait(poseStack, portrait, startX + 3, itemY + 3, 22, 22);

                GuiComponent.fill(poseStack, startX + 1, itemY + 1, startX + 9, itemY + 10, 0xD0000000);
                font.draw(poseStack, new TextComponent(String.valueOf(i + 1)), startX + 3, itemY + 2, isActive ? 0xFFFBBF24 : 0xFF94A3B8);

                String rawName = unlocked ? character.getName() : "编队空位";
                String name = rawName.contains("·") ? rawName.substring(rawName.indexOf('·') + 1) : rawName;
                font.draw(poseStack, new TextComponent(name), startX + 28, itemY + 4,
                        unlocked ? (isActive ? 0xFFFBBF24 : 0xFFEEEEEE) : 0xFF708090);

                if (unlocked) {
                    int maxHp = Math.max(1, (int) character.getTotalMaxHp());
                    int curHp = Math.max(0, (int) character.getCurrentHp());
                    float hpPct = Mth.clamp((float) curHp / maxHp, 0.0f, 1.0f);

                    int barX = startX + 28;
                    int barY = itemY + 15;
                    int barW = 64;
                    GuiComponent.fill(poseStack, barX, barY, barX + barW, barY + 6, 0xB016202B);
                    int hpCol = hpPct > 0.35f ? 0xFF4ADE80 : 0xFFF87171;
                    GuiComponent.fill(poseStack, barX, barY, barX + (int) (barW * hpPct), barY + 6, hpCol);

                    String elemShort = character.getElementLabel();
                    font.draw(poseStack, new TextComponent(elemShort), startX + cardW - 16, itemY + 13, elemCol);
                } else {
                    font.draw(poseStack, new TextComponent("主线/抽卡解锁"), startX + 28, itemY + 15, 0xFF556677);
                }
            }
        });
    }

    private static void renderBottomCenterPlayerStatus(PoseStack poseStack, Player player, int width, int height, Font font) {
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            CharacterData active = team.getCharacter(team.getActiveSlotIndex());
            int barW = Math.min(230, width - 16);
            int barH = 24;
            int x = (width - barW) / 2;
            int y = height - 29;

            drawVoxelPanel(poseStack, x, y, barW, barH, 0xCC0D1622, 0xFF2E5072, 0xFF38BDF8);

            int maxHp = Math.max(1, (int) active.getTotalMaxHp());
            int curHp = Math.max(0, (int) active.getCurrentHp());
            float hpPct = Mth.clamp((float) curHp / maxHp, 0.0f, 1.0f);

            String lvName = "Lv." + active.getLevel() + " " + active.getName() + " [" + active.getElementLabel() + "]";
            font.draw(poseStack, new TextComponent(lvName), x + 6, y + 3, 0xFFFBBF24);

            String worldBadge = String.format("世Lv.%d|体%d", team.getWorldLevel(), team.getStamina());
            font.draw(poseStack, new TextComponent(worldBadge), x + barW - font.width(worldBadge) - 5, y + 3, 0xFF7DD3FC);

            int hpBarX = x + 6;
            int hpBarY = y + 13;
            int hpBarW = barW - 12;
            GuiComponent.fill(poseStack, hpBarX, hpBarY, hpBarX + hpBarW, hpBarY + 7, 0xCC121A24);
            int hpCol = hpPct > 0.35f ? 0xFF22C55E : 0xFFEF4444;
            int fillW = (int) (hpBarW * hpPct);
            GuiComponent.fill(poseStack, hpBarX, hpBarY, hpBarX + fillW, hpBarY + 7, hpCol);

            String hpStr = curHp + " / " + maxHp;
            font.drawShadow(poseStack, new TextComponent(hpStr), x + (barW - font.width(hpStr)) / 2.0f, hpBarY, 0xFFFFFFFF);
        });
    }

    private static void renderOverworldEncounterTip(PoseStack poseStack, int width, int height, Font font) {
        if (width < 460) return; // Avoid crowding bottom bar at compact 427x240 scale
        String tip = "左键攻击怪物：先手进战";
        int tw = font.width(tip) + 12;
        int x = width - tw - 8;
        int y = height - 24;
        drawVoxelPanel(poseStack, x, y, tw, 16, 0xB80A121C, 0xFF284664, 0xFFFBBF24);
        font.draw(poseStack, new TextComponent(tip), x + 6, y + 4, 0xFFE2E8F0);
    }

    private static void renderInteractionPrompt(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        if (mc.level == null) return;
        String promptLabel = null;

        if (mc.hitResult instanceof EntityHitResult entityHit && player.getEyePosition().distanceTo(entityHit.getLocation()) <= 4.8) {
            promptLabel = "交谈 · " + entityHit.getEntity().getName().getString();
        } else if (mc.hitResult instanceof BlockHitResult blockHit && player.getEyePosition().distanceTo(blockHit.getLocation()) <= 4.8) {
            promptLabel = describeInteractiveBlock(mc.level.getBlockState(blockHit.getBlockPos()), blockHit.getBlockPos(), player);
        }

        if (promptLabel == null) {
            List<FactionNpcEntity> nearby = mc.level.getEntitiesOfClass(FactionNpcEntity.class,
                    new AABB(player.blockPosition()).inflate(3.2));
            if (!nearby.isEmpty()) {
                promptLabel = "交谈 · " + nearby.get(0).getName().getString();
            }
        }

        if (promptLabel == null) {
            BlockPos pPos = player.blockPosition();
            for (int dx = -2; dx <= 2 && promptLabel == null; dx++) {
                for (int dy = -1; dy <= 2 && promptLabel == null; dy++) {
                    for (int dz = -2; dz <= 2 && promptLabel == null; dz++) {
                        BlockPos check = pPos.offset(dx, dy, dz);
                        promptLabel = describeInteractiveBlock(mc.level.getBlockState(check), check, player);
                    }
                }
            }
        }

        if (promptLabel != null) {
            int promptX = Math.min(width - 170, width / 2 + 18);
            int promptY = height / 2 - 11;
            int boxW = font.width(promptLabel) + 34;
            int boxH = 20;

            drawVoxelPanel(poseStack, promptX, promptY, boxW, boxH, 0xD80D1520, 0xFF4A90E2, 0xFF38BDF8);
            GuiComponent.fill(poseStack, promptX + 4, promptY + 3, promptX + 16, promptY + 17, 0xB0000000);
            font.draw(poseStack, new TextComponent("F"), promptX + 8, promptY + 6, 0xFF38BDF8);
            font.draw(poseStack, new TextComponent(promptLabel), promptX + 21, promptY + 6, 0xFFFFFFFF);
        }
    }

    private static String describeInteractiveBlock(BlockState state, BlockPos pos, Player player) {
        if (state.is(ModBlocks.SUPPLY_CRATE.get()) || state.is(Blocks.CHEST)) {
            return "装甲物资箱 · 开启搜寻";
        }
        if (state.is(ModBlocks.TELEPORT_WAYPOINT.get())) {
            return player.getX() > 90 ? "冰原锚点 · 恢复 / 返舰" : "传送锚点 · 恢复 / 传送";
        }
        if (state.is(ModBlocks.ENGINEERING_TERMINAL.get())) {
            return pos.getX() < 20 ? "走廊闸门开关 · 启动开启" : "模拟训练终端 · 刷取材料副本";
        }
        if (state.is(ModBlocks.AUXILIARY_ENERGY_CORE.get())) {
            return pos.getX() > 90 ? "东配电合闸核心 · 通电解锁" : "备用发电机组 · 合闸启动";
        }
        if (state.is(ModBlocks.ORBITAL_DROP_POD.get())) {
            return "轨道逃生舱 · 脱离飞船";
        }
        if (state.is(ModBlocks.PRECURSOR_ALTAR.get())) {
            return "深空通讯塔控制台 · 迎战挖掘机";
        }
        return null;
    }

    private static void renderRadioDialogue(PoseStack poseStack, int width, int height, Font font) {
        if (!RadioSubtitleManager.hasActiveDialogue()) return;

        String speaker = RadioSubtitleManager.getCurrentSpeaker();
        String text = RadioSubtitleManager.getCurrentText();
        int accentCol = RadioSubtitleManager.getSpeakerColor();

        int maxBoxW = Math.min(width - 20, 420);
        int textMaxW = maxBoxW - 50;
        List<FormattedCharSequence> lines = font.split(new TextComponent(text), textMaxW);
        int showLines = Math.min(lines.size(), 3);
        int boxWidth = Math.max(220, maxBoxW);
        int boxHeight = 20 + showLines * 11;
        int startX = (width - boxWidth) / 2;
        int startY = height - 34 - boxHeight;

        drawVoxelPanel(poseStack, startX, startY, boxWidth, boxHeight, 0xDE0A121C, accentCol, 0xFFFFFFFF);

        String charId = "linye";
        if (speaker.contains("老郑")) charId = "laozheng";
        else if (speaker.contains("陈风")) charId = "chenfeng";
        else if (speaker.contains("苏晴")) charId = "suqing";
        else if (speaker.contains("韩冰")) charId = "hanbing";

        drawPortrait(poseStack, TurnBasedBattleScreen.getPortraitForChar(charId), startX + 6, startY + 6, 26, 26);

        String header = "【" + speaker + "】";
        int qCount = RadioSubtitleManager.getQueuedCount();
        if (qCount > 0) header += "  +" + qCount;
        font.draw(poseStack, new TextComponent(header), startX + 38, startY + 5, accentCol);
        int ty = startY + 17;
        for (int i = 0; i < showLines; i++) {
            font.draw(poseStack, lines.get(i), startX + 38, ty, 0xFFF4F7FA);
            ty += 11;
        }
    }
}
