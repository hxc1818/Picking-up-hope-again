package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.DamagePopupManager;
import com.resilienthope.core.client.KeyInit;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.entity.PrecursorMechBossEntity;
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

import java.util.Comparator;
import java.util.List;

public class GenshinHudOverlay {

    private static final ResourceLocation TEX_PORTRAIT_DAWN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_dawn.png");
    private static final ResourceLocation TEX_PORTRAIT_LOCKED = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_locked.png");
    private static final ResourceLocation TEX_PORTRAIT_ZHENG = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_zheng.png");
    private static final ResourceLocation TEX_PORTRAIT_HAN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_han.png");
    private static final ResourceLocation TEX_PORTRAIT_SU = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_su.png");
    private static final ResourceLocation TEX_SKILL_E = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/skill_e_dawn.png");
    private static final ResourceLocation TEX_SKILL_Q = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/skill_q_dawn.png");

    private static int beaconTickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        RadioSubtitleManager.clientTick();
        DamagePopupManager.clientTick();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && !mc.isPaused()) {
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

        PoseStack poseStack = event.getMatrixStack();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        renderDamagePopups(poseStack, mc, screenWidth, screenHeight, font);
        renderInWorldQuestMarker(poseStack, mc, player, screenWidth, screenHeight, font);
        renderQuestTracker(poseStack, mc, player, screenWidth, screenHeight, font);
        renderMiniRadar(poseStack, mc, player, screenWidth, screenHeight, font);
        renderBossBar(poseStack, mc, player, screenWidth, font);
        renderGenshinPartySidebar(poseStack, player, screenWidth, screenHeight, font);
        renderBottomCenterPlayerStatus(poseStack, player, screenWidth, screenHeight, font);
        renderRightBottomSkillRing(poseStack, player, screenWidth, screenHeight, font);
        renderInteractionPrompt(poseStack, mc, player, screenWidth, screenHeight, font);
        renderRadioDialogue(poseStack, screenWidth, screenHeight, font);
    }

    // =========================================================================
    // Voxel UI Primitives
    // =========================================================================

    public static void drawVoxelPanel(PoseStack ps, int x, int y, int w, int h, int bgCol, int borderCol, int cornerCol) {
        GuiComponent.fill(ps, x, y, x + w, y + h, bgCol);
        // 1px outer border
        GuiComponent.fill(ps, x, y, x + w, y + 1, borderCol);
        GuiComponent.fill(ps, x, y + h - 1, x + w, y + h, borderCol);
        GuiComponent.fill(ps, x, y, x + 1, y + h, borderCol);
        GuiComponent.fill(ps, x + w - 1, y, x + w, y + h, borderCol);
        // Voxel corner pixels
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

    // =========================================================================
    // Exact Orthonormal Camera Basis 3D -> 2D Projection
    // =========================================================================

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
            // Target is behind or parallel to camera plane: smoothly project to viewport perimeter
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
        int[] proj = projectWorldToScreen(mc, target.getX() + 0.5, target.getY() + 1.65, target.getZ() + 0.5, width, height);

        int rawX = proj[0];
        int rawY = proj[1];
        boolean inFront = proj[2] == 1;
        boolean offScreen = !inFront || rawX < 28 || rawX > width - 28 || rawY < 42 || rawY > height - 56;

        int clampedX = Math.max(28, Math.min(width - 28, rawX));
        int clampedY = Math.max(42, Math.min(height - 56, rawY));

        String distStr = dist < 2.5 ? "到达" : String.format("%.0fm", dist);
        int boxW = Math.max(28, font.width(distStr) + 10);

        drawVoxelPanel(poseStack, clampedX - boxW / 2, clampedY - 12, boxW, 22,
                0xC00B131D, offScreen ? 0xFFFF8A00 : 0xFFFFD54F, 0xFF55FFFF);

        String icon = offScreen ? (clampedX <= 30 ? "◀◈" : (clampedX >= width - 30 ? "◈▶" : "◈")) : "◈";
        int iw = font.width(icon);
        font.draw(poseStack, new TextComponent(icon), clampedX - iw / 2, clampedY - 9, 0xFFFFD54F);

        int dw = font.width(distStr);
        font.draw(poseStack, new TextComponent(distStr), clampedX - dw / 2, clampedY + 1, 0xFF66FFFF);
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
            int color = p.isCrit ? 0xFFFFD54F : (p.isSkill ? 0xFF55FFFF : 0xFFFFFFFF);
            int lw = font.width(label);

            poseStack.pushPose();
            float scale = p.isCrit ? 1.25f : 1.0f;
            poseStack.translate(sx, sy, 0);
            poseStack.scale(scale, scale, 1.0f);
            font.drawShadow(poseStack, new TextComponent(label), -lw / 2.0f, 0, color);
            poseStack.popPose();
        }
    }

    // =========================================================================
    // Top-Right Voxel Tactical Scan Radar
    // =========================================================================

    private static void renderMiniRadar(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        int radius = 28;
        int radarX = width - 38;
        int radarY = 38;

        drawVoxelPanel(poseStack, radarX - radius, radarY - radius, radius * 2, radius * 2,
                0xBA091018, 0xFF2E5C88, 0xFF4FC3F7);
        // Subtle radar crosshairs
        GuiComponent.fill(poseStack, radarX - radius + 2, radarY, radarX + radius - 2, radarY + 1, 0x304FC3F7);
        GuiComponent.fill(poseStack, radarX, radarY - radius + 2, radarX + 1, radarY + radius - 2, 0x304FC3F7);

        double yawRad = Math.toRadians(player.getYRot());
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        // Scan nearby wall blocks (step 1 within 11 blocks)
        if (mc.level != null) {
            BlockPos pPos = player.blockPosition();
            for (int ox = -10; ox <= 10; ox++) {
                for (int oz = -10; oz <= 10; oz++) {
                    BlockPos check = pPos.offset(ox, 1, oz);
                    BlockState bs = mc.level.getBlockState(check);
                    if (!bs.isAir() && bs.getMaterial().isSolid()) {
                        double lr = ox * rightX + oz * rightZ;
                        double lf = ox * fwdX + oz * fwdZ;
                        int px = radarX + (int) (lr * 2.1);
                        int py = radarY - (int) (lf * 2.1);
                        if (Math.abs(px - radarX) < radius - 3 && Math.abs(py - radarY) < radius - 3) {
                            GuiComponent.fill(poseStack, px, py, px + 2, py + 2, 0x664A709A);
                        }
                    }
                }
            }

            // Scan nearby Hostile Monsters (Red) & Faction NPCs (Cyan)
            AABB scanBox = new AABB(pPos).inflate(16.0);
            for (Monster m : mc.level.getEntitiesOfClass(Monster.class, scanBox)) {
                double dx = m.getX() - player.getX();
                double dz = m.getZ() - player.getZ();
                double lr = dx * rightX + dz * rightZ;
                double lf = dx * fwdX + dz * fwdZ;
                int ex = Mth.clamp(radarX + (int) (lr * 1.5), radarX - radius + 3, radarX + radius - 4);
                int ey = Mth.clamp(radarY - (int) (lf * 1.5), radarY - radius + 3, radarY + radius - 4);
                GuiComponent.fill(poseStack, ex - 1, ey - 1, ex + 2, ey + 2, 0xFFFF3B3B);
            }
            for (FactionNpcEntity npc : mc.level.getEntitiesOfClass(FactionNpcEntity.class, scanBox)) {
                double dx = npc.getX() - player.getX();
                double dz = npc.getZ() - player.getZ();
                double lr = dx * rightX + dz * rightZ;
                double lf = dx * fwdX + dz * fwdZ;
                int ex = Mth.clamp(radarX + (int) (lr * 1.5), radarX - radius + 3, radarX + radius - 4);
                int ey = Mth.clamp(radarY - (int) (lf * 1.5), radarY - radius + 3, radarY + radius - 4);
                GuiComponent.fill(poseStack, ex - 1, ey - 1, ex + 2, ey + 2, 0xFF40FFCC);
            }
        }

        // North marker on perimeter (dx=0, dz=-1)
        double nr = -rightZ;
        double nf = -fwdZ;
        int nX = radarX + (int) (nr * (radius - 6));
        int nY = radarY - (int) (nf * (radius - 6));
        font.draw(poseStack, new TextComponent("N"), nX - 2, nY - 4, 0xFF55FFFF);

        // Quest target marker
        BlockPos target = QuestManager.getEffectiveBreadcrumbPos(player);
        if (target != null) {
            double dx = (target.getX() + 0.5) - player.getX();
            double dz = (target.getZ() + 0.5) - player.getZ();
            double lr = dx * rightX + dz * rightZ;
            double lf = dx * fwdX + dz * fwdZ;
            double len = Math.sqrt(lr * lr + lf * lf);
            double maxDist = radius - 6;
            double scale = len > 14.0 ? (maxDist / Math.max(0.01, len)) : 1.5;
            int tx = Mth.clamp(radarX + (int) (lr * scale), radarX - radius + 4, radarX + radius - 4);
            int ty = Mth.clamp(radarY - (int) (lf * scale), radarY - radius + 4, radarY + radius - 4);
            font.draw(poseStack, new TextComponent("◆"), tx - 3, ty - 4, 0xFFFFD700);
        }

        // Center Player Marker
        GuiComponent.fill(poseStack, radarX - 2, radarY - 2, radarX + 2, radarY + 2, 0xFFFFFFFF);
        GuiComponent.fill(poseStack, radarX - 1, radarY - 4, radarX + 1, radarY - 2, 0xFF55FFFF);
    }

    // =========================================================================
    // Top-Left Quest Tracker + Shortcut Bar
    // =========================================================================

    private static void renderQuestTracker(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        String title = QuestManager.getQuestTitle();
        String objective = QuestManager.getObjectiveText();
        BlockPos target = QuestManager.getEffectiveBreadcrumbPos(player);
        String hint = QuestManager.getBreadcrumbHint(player);

        double dist = target != null ? Math.sqrt(player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5)) : 0;
        String distStr = dist > 3.2 ? String.format(" [%.0fm]", dist) : " [到达目标]";
        String fullObj = objective + hint;

        int startX = 12;
        int startY = 12;
        int textW = Math.max(font.width(title), font.width(fullObj) + font.width(distStr));
        int bgW = Math.max(textW + 26, 200);
        int bgH = 34;

        drawVoxelPanel(poseStack, startX, startY, bgW, bgH, 0xB80D141F, 0xFF284664, 0xFFFFCC00);
        GuiComponent.fill(poseStack, startX, startY, startX + 3, startY + bgH, 0xFFFFCC00);

        font.draw(poseStack, new TextComponent("◈ " + title), startX + 8, startY + 5, 0xFFFFE082);
        font.draw(poseStack, new TextComponent(fullObj), startX + 10, startY + 18, 0xFFE6EEF5);
        font.draw(poseStack, new TextComponent(distStr), startX + 12 + font.width(fullObj), startY + 18, 0xFF55FFFF);

        // Quick Keybind Hints Row directly underneath
        String keys = "[B]背包  [C]角色  [G]祈愿  [F]交互";
        int kw = font.width(keys) + 12;
        drawVoxelPanel(poseStack, startX, startY + bgH + 4, kw, 14, 0x90091018, 0xFF20344A, 0xFF4FC3F7);
        font.draw(poseStack, new TextComponent(keys), startX + 6, startY + bgH + 7, 0xFF9EC5E8);
    }

    // =========================================================================
    // Top-Center Boss Health Bar
    // =========================================================================

    private static void renderBossBar(PoseStack poseStack, Minecraft mc, Player player, int width, Font font) {
        if (mc.level == null) return;
        List<PrecursorMechBossEntity> bosses = mc.level.getEntitiesOfClass(PrecursorMechBossEntity.class,
                new AABB(player.blockPosition()).inflate(48.0));
        PrecursorMechBossEntity boss = bosses.stream()
                .filter(b -> b.isAlive() && b.getHealth() > 0)
                .min(Comparator.comparingDouble(b -> b.distanceToSqr(player)))
                .orElse(null);
        if (boss == null) return;

        int barW = 240;
        int x = (width - barW) / 2;
        int y = 14;

        String name = "【世界首领】先驱重型挖掘机甲·泰坦";
        int nw = font.width(name);
        font.drawShadow(poseStack, new TextComponent(name), width / 2.0f - nw / 2.0f, y - 2, 0xFFFFD54F);

        drawVoxelPanel(poseStack, x, y + 10, barW, 10, 0xD0120D14, 0xFF7A3820, 0xFFFFAA00);
        float pct = Mth.clamp(boss.getHealth() / Math.max(1.0f, boss.getMaxHealth()), 0.0f, 1.0f);
        int fillW = (int) ((barW - 4) * pct);
        GuiComponent.fill(poseStack, x + 2, y + 12, x + 2 + fillW, y + 18, 0xFFE53935);
        GuiComponent.fill(poseStack, x + 2, y + 12, x + 2 + fillW, y + 14, 0xFFFF8A65);

        String hpText = (int) boss.getHealth() + " / " + (int) boss.getMaxHealth();
        int hw = font.width(hpText);
        font.drawShadow(poseStack, new TextComponent(hpText), width / 2.0f - hw / 2.0f, y + 22, 0xFFFFCCBC);
    }

    // =========================================================================
    // Right Sidebar (Protagonist + 3 Reserved Vertical Expansion Slots)
    // =========================================================================

    private static void renderGenshinPartySidebar(PoseStack poseStack, Player player, int width, int height, Font font) {
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            int startX = width - 138;
            int startY = height / 2 - 72;

            for (int i = 0; i < 4; i++) {
                CharacterData character = team.getCharacter(i);
                boolean unlocked = character.isUnlocked();
                boolean isActive = (i == team.getActiveSlotIndex());
                int itemY = startY + i * 38;

                int bgCol = isActive ? 0xC81C2E42 : (unlocked ? 0x90101824 : 0x600B0F16);
                int borderCol = isActive ? 0xFF4FC3F7 : 0xFF243548;
                int cornerCol = isActive ? 0xFFFFD54F : 0xFF3A5570;

                drawVoxelPanel(poseStack, startX, itemY, 128, 33, bgCol, borderCol, cornerCol);

                // Voxel Portrait
                ResourceLocation portrait = unlocked ? TEX_PORTRAIT_DAWN : TEX_PORTRAIT_LOCKED;
                drawTexture(poseStack, portrait, startX + 4, itemY + 4, 24, 24, 32, 32);

                // Key slot number badge
                GuiComponent.fill(poseStack, startX + 2, itemY + 2, startX + 11, itemY + 11, 0xD0000000);
                font.draw(poseStack, new TextComponent(String.valueOf(i + 1)), startX + 4, itemY + 3, isActive ? 0xFFFFD54F : 0xFF99AABB);

                // Name
                String name = character.getName();
                font.draw(poseStack, new TextComponent(name), startX + 32, itemY + 5,
                        unlocked ? (isActive ? 0xFFFFEEAA : 0xFFEEEEEE) : 0xFF708090);

                if (unlocked) {
                    int maxHp = Math.max(1, (int) character.getTotalMaxHp());
                    int curHp = Math.max(0, (int) character.getCurrentHp());
                    float hpPct = Mth.clamp((float) curHp / maxHp, 0.0f, 1.0f);

                    int barX = startX + 32;
                    int barY = itemY + 18;
                    int barW = 72;
                    GuiComponent.fill(poseStack, barX, barY, barX + barW, barY + 6, 0xB016202B);
                    int hpCol = hpPct > 0.35f ? 0xFF4ADE80 : 0xFFF87171;
                    GuiComponent.fill(poseStack, barX, barY, barX + (int) (barW * hpPct), barY + 6, hpCol);

                    // Mini Q Energy Ring Indicator
                    boolean burstReady = character.getCurrentEnergy() >= character.getMaxEnergy();
                    int eCol = burstReady ? 0xFFFFCC00 : 0xFF283A4E;
                    GuiComponent.fill(poseStack, startX + 109, itemY + 9, startX + 123, itemY + 23, eCol);
                    font.draw(poseStack, new TextComponent("Q"), startX + 113, itemY + 12, burstReady ? 0xFF101010 : 0xFF778899);
                } else {
                    font.draw(poseStack, new TextComponent("随后续剧情解锁"), startX + 32, itemY + 18, 0xFF556677);
                }
            }
        });
    }

    // =========================================================================
    // Bottom-Center Protagonist Health, Shield & Stat Bar
    // =========================================================================

    private static void renderBottomCenterPlayerStatus(PoseStack poseStack, Player player, int width, int height, Font font) {
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            CharacterData active = team.getCharacter(team.getActiveSlotIndex());
            int barW = 216;
            int barH = 24;
            int x = (width - barW) / 2;
            int y = height - 34;

            drawVoxelPanel(poseStack, x, y, barW, barH, 0xCC0D1622, 0xFF2E5072, 0xFF4FC3F7);

            int maxHp = Math.max(1, (int) active.getTotalMaxHp());
            int curHp = Math.max(0, (int) active.getCurrentHp());
            float hpPct = Mth.clamp((float) curHp / maxHp, 0.0f, 1.0f);

            String lvName = "Lv." + active.getLevel() + " " + active.getName();
            font.draw(poseStack, new TextComponent(lvName), x + 6, y + 3, 0xFFFFEEAA);

            String atkBadge = String.format("ATK:%d", Math.round(active.getTotalAtk()));
            font.draw(poseStack, new TextComponent(atkBadge), x + barW - font.width(atkBadge) - 6, y + 3, 0xFF55FFFF);

            int hpBarX = x + 6;
            int hpBarY = y + 13;
            int hpBarW = barW - 12;
            GuiComponent.fill(poseStack, hpBarX, hpBarY, hpBarX + hpBarW, hpBarY + 8, 0xCC121A24);
            int hpCol = hpPct > 0.35f ? 0xFF22C55E : 0xFFEF4444;
            int fillW = (int) (hpBarW * hpPct);
            GuiComponent.fill(poseStack, hpBarX, hpBarY, hpBarX + fillW, hpBarY + 8, hpCol);
            GuiComponent.fill(poseStack, hpBarX, hpBarY, hpBarX + fillW, hpBarY + 2, 0x60FFFFFF);

            if (player.getAbsorptionAmount() > 0) {
                int shieldW = Math.min(hpBarW, (int) (hpBarW * (player.getAbsorptionAmount() / 12.0f)));
                GuiComponent.fill(poseStack, hpBarX, hpBarY + 6, hpBarX + shieldW, hpBarY + 8, 0xFFFFD54F);
            }

            String hpStr = curHp + " / " + maxHp;
            int hw = font.width(hpStr);
            font.drawShadow(poseStack, new TextComponent(hpStr), x + (barW - hw) / 2.0f, hpBarY, 0xFFFFFFFF);
        });
    }

    // =========================================================================
    // Bottom-Right Voxel E / Q Skill Slots with Live Energy Waterline
    // =========================================================================

    private static void renderRightBottomSkillRing(PoseStack poseStack, Player player, int width, int height, Font font) {
        int eX = width - 90;
        int eY = height - 48;
        int qX = width - 46;
        int qY = height - 48;

        float cd = KeyInit.getSkillCooldownSeconds();
        boolean onCd = cd > 0.01f;

        // E Skill Box
        drawVoxelPanel(poseStack, eX, eY, 34, 34, 0xCC101B28, onCd ? 0xFF3A4A5A : 0xFF38BDF8, 0xFF55FFFF);
        drawTexture(poseStack, TEX_SKILL_E, eX + 5, eY + 5, 24, 24, 16, 16);

        if (onCd) {
            float ratio = Mth.clamp(cd / 6.0f, 0.0f, 1.0f);
            int maskH = (int) (30 * ratio);
            GuiComponent.fill(poseStack, eX + 2, eY + 32 - maskH, eX + 32, eY + 32, 0xB8000000);
            String cdStr = String.format("%.1f", cd);
            font.drawShadow(poseStack, new TextComponent(cdStr), eX + (34 - font.width(cdStr)) / 2.0f, eY + 13, 0xFFFFCC00);
        }
        GuiComponent.fill(poseStack, eX + 10, eY + 27, eX + 24, eY + 37, 0xE00A121C);
        font.draw(poseStack, new TextComponent("E"), eX + 14, eY + 28, onCd ? 0xFF8899AA : 0xFF55FFFF);

        // Q Ultimate Burst Box (Synced Energy Level!)
        float curE = 0f;
        float maxE = 100f;
        TeamCapability.ITeamData teamData = player.getCapability(TeamCapability.TEAM_CAPABILITY).resolve().orElse(null);
        if (teamData != null) {
            CharacterData active = teamData.getCharacter(teamData.getActiveSlotIndex());
            curE = active.getCurrentEnergy();
            maxE = Math.max(1.0f, active.getMaxEnergy());
        }
        float ePct = Mth.clamp(curE / maxE, 0.0f, 1.0f);
        boolean ready = ePct >= 0.999f;

        drawVoxelPanel(poseStack, qX, qY, 34, 34, 0xCC1C140E, ready ? 0xFFFFD54F : 0xFF5A4A3A, ready ? 0xFFFFFFFF : 0xFFFFAA00);

        // Rising Energy Fill inside Q Slot
        int fillH = (int) (30 * ePct);
        if (fillH > 0) {
            GuiComponent.fill(poseStack, qX + 2, qY + 32 - fillH, qX + 32, qY + 32, ready ? 0x90FFAA00 : 0x70E67E22);
        }
        drawTexture(poseStack, TEX_SKILL_Q, qX + 5, qY + 5, 24, 24, 16, 16);

        if (!ready) {
            String pctStr = ((int) (ePct * 100)) + "%";
            font.drawShadow(poseStack, new TextComponent(pctStr), qX + (34 - font.width(pctStr)) / 2.0f, qY + 13, 0xFFFFE0B2);
        }
        GuiComponent.fill(poseStack, qX + 10, qY + 27, qX + 24, qY + 37, 0xE00A121C);
        font.draw(poseStack, new TextComponent("Q"), qX + 14, qY + 28, ready ? 0xFFFFD54F : 0xFFAA9988);
    }

    // =========================================================================
    // Crosshair Interaction Capsule
    // =========================================================================

    private static void renderInteractionPrompt(PoseStack poseStack, Minecraft mc, Player player, int width, int height, Font font) {
        if (mc.level == null) return;
        String promptLabel = null;

        if (mc.hitResult instanceof EntityHitResult entityHit && player.getEyePosition().distanceTo(entityHit.getLocation()) <= 4.8) {
            promptLabel = "交谈 · " + entityHit.getEntity().getName().getString();
        } else if (mc.hitResult instanceof BlockHitResult blockHit && player.getEyePosition().distanceTo(blockHit.getLocation()) <= 4.8) {
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            if (state.is(ModBlocks.TELEPORT_WAYPOINT.get())) {
                promptLabel = player.getX() > 90 ? "异星信标锚点 · 回复 / 返舰" : "星舰传送锚点 · 激活同步";
            } else if (state.is(ModBlocks.ENGINEERING_TERMINAL.get())) {
                promptLabel = "工程主控终端 · 访问";
            } else if (state.is(ModBlocks.AUXILIARY_ENERGY_CORE.get())) {
                promptLabel = "辅助聚变核心 · 修复启动";
            } else if (state.is(ModBlocks.ORBITAL_DROP_POD.get())) {
                promptLabel = "近地轨道空投舱 · 空降极寒矿坑";
            } else if (state.is(ModBlocks.PRECURSOR_ALTAR.get())) {
                promptLabel = "先驱高能祭坛 · 唤醒机甲Boss";
            } else if (state.is(Blocks.CHEST)) {
                promptLabel = "冷冻物资箱 · 开启拾取";
            }
        } else {
            List<FactionNpcEntity> nearby = mc.level.getEntitiesOfClass(FactionNpcEntity.class,
                    new AABB(player.blockPosition()).inflate(3.2));
            if (!nearby.isEmpty()) {
                promptLabel = "交谈 · " + nearby.get(0).getName().getString();
            }
        }

        if (promptLabel != null) {
            int promptX = width / 2 + 24;
            int promptY = height / 2 - 11;
            int boxW = font.width(promptLabel) + 38;
            int boxH = 22;

            drawVoxelPanel(poseStack, promptX, promptY, boxW, boxH, 0xD80D1520, 0xFF4A90E2, 0xFF55FFFF);
            GuiComponent.fill(poseStack, promptX + 5, promptY + 4, promptX + 18, promptY + 18, 0xB0000000);
            font.draw(poseStack, new TextComponent("F"), promptX + 9, promptY + 7, 0xFF55FFFF);
            font.draw(poseStack, new TextComponent(promptLabel), promptX + 24, promptY + 7, 0xFFFFFFFF);
        }
    }

    // =========================================================================
    // Queued Typewriter Radio Subtitle with Speaker Portrait
    // =========================================================================

    private static void renderRadioDialogue(PoseStack poseStack, int width, int height, Font font) {
        if (!RadioSubtitleManager.hasActiveDialogue()) return;

        String speaker = RadioSubtitleManager.getCurrentSpeaker();
        String text = RadioSubtitleManager.getCurrentText();
        int accentCol = RadioSubtitleManager.getSpeakerColor();

        int textWidth = font.width(text);
        int boxWidth = Math.max(textWidth + 68, 280);
        int boxHeight = 40;
        int startX = (width - boxWidth) / 2;
        int startY = height - 84;

        drawVoxelPanel(poseStack, startX, startY, boxWidth, boxHeight, 0xD80A121C, accentCol, 0xFFFFFFFF);

        ResourceLocation speakerPortrait = TEX_PORTRAIT_DAWN;
        if (speaker.contains("郑铁山") || speaker.contains("工会")) speakerPortrait = TEX_PORTRAIT_ZHENG;
        else if (speaker.contains("韩振岳") || speaker.contains("舰桥")) speakerPortrait = TEX_PORTRAIT_HAN;
        else if (speaker.contains("苏挽星") || speaker.contains("科学")) speakerPortrait = TEX_PORTRAIT_SU;

        drawTexture(poseStack, speakerPortrait, startX + 6, startY + 6, 28, 28, 32, 32);

        String header = "【" + speaker + "】";
        int qCount = RadioSubtitleManager.getQueuedCount();
        if (qCount > 0) header += "  +" + qCount;
        font.draw(poseStack, new TextComponent(header), startX + 40, startY + 6, accentCol);
        font.draw(poseStack, new TextComponent(text), startX + 40, startY + 20, 0xFFF4F7FA);
    }
}
