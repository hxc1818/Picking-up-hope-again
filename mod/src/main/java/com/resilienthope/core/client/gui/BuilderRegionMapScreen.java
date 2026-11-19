package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.network.PacketBuilderTeleport;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.subtitle.SystemToastManager;
import com.resilienthope.core.world.MapZoneRegistry;
import com.resilienthope.core.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Interactive multi-rectangle region blueprint & click-to-teleport map for Creative builders.
 * Scales seamlessly from the 1560x820 Starship to the 11,700x9,700 Cryo Planet continent.
 */
public class BuilderRegionMapScreen extends Screen {
    private String viewingDimId = "starship"; // "starship" or "planet"
    private MapZoneRegistry.ZoneDef hoveredZone = null;

    private SciFiButton starshipTabBtn;
    private SciFiButton planetTabBtn;

    private int canvasX, canvasY, canvasW, canvasH;
    private int sideX, sideY, sideW, sideH;
    private int minX, maxX, minZ, maxZ;
    private double scale;
    private int originPxX, originPxZ;

    public BuilderRegionMapScreen() {
        super(new TextComponent("双维度区域蓝图与一键传送 (M)"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            String curDim = ModDimensions.toZoneDimensionId(mc.level.dimension());
            if ("planet".equals(curDim)) {
                this.viewingDimId = "planet";
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        this.canvasX = 18;
        this.canvasY = 48;
        this.sideW = Math.min(182, Math.max(132, (int) (this.width * 0.30f)));
        this.canvasW = Math.max(180, this.width - this.sideW - 44);
        this.canvasH = Math.max(120, this.height - 80);
        this.sideX = this.canvasX + this.canvasW + 8;
        this.sideY = this.canvasY;
        this.sideH = this.canvasH;

        this.starshipTabBtn = addRenderableWidget(new SciFiButton(18, 26, 126, 17,
                new TextComponent("飞船 · 重拾希望号 (5区)"), btn -> {
            viewingDimId = "starship";
            init();
        }, "starship".equals(viewingDimId) ? 0xFFFBBF24 : 0xFF38BDF8));

        this.planetTabBtn = addRenderableWidget(new SciFiButton(148, 26, 152, 17,
                new TextComponent("第一颗星球 · 万格大陆 (8区)"), btn -> {
            viewingDimId = "planet";
            init();
        }, "planet".equals(viewingDimId) ? 0xFFFBBF24 : 0xFF38BDF8));

        addRenderableWidget(new SciFiButton(this.width - 60, 10, 42, 16,
                new TextComponent("关闭"), btn -> onClose(), 0xFFEF4444));

        // Sidebar fast-teleport buttons for each zone in the active dimension view
        List<MapZoneRegistry.ZoneDef> zones = MapZoneRegistry.getZonesForDimId(viewingDimId);
        int btnY = sideY + 6;
        int btnH = Math.min(20, Math.max(14, (sideH - 64) / Math.max(1, zones.size()) - 2));
        for (MapZoneRegistry.ZoneDef z : zones) {
            addRenderableWidget(new SciFiButton(sideX + 6, btnY, sideW - 12, btnH,
                    new TextComponent(z.code + " · " + z.name), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketBuilderTeleport(viewingDimId, z.centerX, z.centerZ, z.id));
                onClose();
            }, z.mapColor));
            btnY += btnH + 2;
        }
    }

    private void computeTransform() {
        int[] bounds = MapZoneRegistry.getBoundsForDimId(viewingDimId);
        int pad = "planet".equals(viewingDimId) ? 450 : 90;
        this.minX = bounds[0] - pad;
        this.maxX = bounds[1] + pad;
        this.minZ = bounds[2] - pad;
        this.maxZ = bounds[3] + pad;

        double spanX = Math.max(1, maxX - minX);
        double spanZ = Math.max(1, maxZ - minZ);
        double sx = (canvasW - 20) / spanX;
        double sz = (canvasH - 20) / spanZ;
        this.scale = Math.min(sx, sz);

        double drawW = spanX * scale;
        double drawH = spanZ * scale;
        this.originPxX = canvasX + (int) ((canvasW - drawW) / 2.0);
        this.originPxZ = canvasY + (int) ((canvasH - drawH) / 2.0);
    }

    private int worldToScreenX(double wx) {
        return originPxX + (int) Math.round((wx - minX) * scale);
    }

    private int worldToScreenZ(double wz) {
        return originPxZ + (int) Math.round((wz - minZ) * scale);
    }

    private int screenToWorldX(double mx) {
        return (int) Math.round(minX + (mx - originPxX) / Math.max(1e-6, scale));
    }

    private int screenToWorldZ(double mz) {
        return (int) Math.round(minZ + (mz - originPxZ) / Math.max(1e-6, scale));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_M) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (mouseX >= canvasX && mouseX <= canvasX + canvasW && mouseY >= canvasY && mouseY <= canvasY + canvasH) {
                computeTransform();
                int wx = screenToWorldX(mouseX);
                int wz = screenToWorldZ(mouseY);
                MapZoneRegistry.ZoneDef hit = MapZoneRegistry.findZoneAt(viewingDimId, wx, wz);
                PacketHandler.INSTANCE.sendToServer(new PacketBuilderTeleport(
                        viewingDimId, wx, wz, hit != null ? hit.id : ""));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ps);
        computeTransform();

        GenshinHudOverlay.drawVoxelPanel(ps, 10, 6, this.width - 20, this.height - 12,
                0xF208101A, 0xFF284664, 0xFFFBBF24);
        font.drawShadow(ps, getTitle(), 18, 12, 0xFF38BDF8);
        font.draw(ps, new TextComponent("左键点击地图任意坐标或右侧区域直接传送 · /hope speed 1~10 调速"),
                Math.min(this.width - 240, 210), 12, 0xFF94A3B8);

        // Map Canvas Background & Coordinate Grid
        GenshinHudOverlay.drawVoxelPanel(ps, canvasX, canvasY, canvasW, canvasH,
                0xE0050A12, 0xFF1E3A5F, 0xFF38BDF8);

        int gridStep = "planet".equals(viewingDimId) ? 1000 : 200;
        int gxStart = (int) (Math.floor((double) minX / gridStep) * gridStep);
        for (int gx = gxStart; gx <= maxX; gx += gridStep) {
            int sx = worldToScreenX(gx);
            if (sx > canvasX + 2 && sx < canvasX + canvasW - 2) {
                GuiComponent.fill(ps, sx, canvasY + 2, sx + 1, canvasY + canvasH - 2,
                        gx == 0 ? 0x5538BDF8 : 0x2238BDF8);
            }
        }
        int gzStart = (int) (Math.floor((double) minZ / gridStep) * gridStep);
        for (int gz = gzStart; gz <= maxZ; gz += gridStep) {
            int sz = worldToScreenZ(gz);
            if (sz > canvasY + 2 && sz < canvasY + canvasH - 2) {
                GuiComponent.fill(ps, canvasX + 2, sz, canvasX + canvasW - 2, sz + 1,
                        gz == 0 ? 0x5538BDF8 : 0x2238BDF8);
            }
        }

        // Route connection lines
        for (MapZoneRegistry.RouteLink link : MapZoneRegistry.ROUTE_LINKS) {
            MapZoneRegistry.ZoneDef a = MapZoneRegistry.findById(link.fromId());
            MapZoneRegistry.ZoneDef b = MapZoneRegistry.findById(link.toId());
            if (a == null || b == null || !viewingDimId.equals(a.dimensionId)) continue;
            drawDashedLine(ps, worldToScreenX(a.centerX), worldToScreenZ(a.centerZ),
                    worldToScreenX(b.centerX), worldToScreenZ(b.centerZ), 0xAAE2E8F0);
        }

        // Hovered world coordinates
        boolean mouseInCanvas = mouseX >= canvasX && mouseX <= canvasX + canvasW
                && mouseY >= canvasY && mouseY <= canvasY + canvasH;
        int hoverWX = screenToWorldX(mouseX);
        int hoverWZ = screenToWorldZ(mouseY);
        hoveredZone = mouseInCanvas ? MapZoneRegistry.findZoneAt(viewingDimId, hoverWX, hoverWZ) : null;

        // Render irregular multi-rectangle regions
        List<MapZoneRegistry.ZoneDef> zones = MapZoneRegistry.getZonesForDimId(viewingDimId);
        for (MapZoneRegistry.ZoneDef z : zones) {
            boolean isHover = (z == hoveredZone);
            int rgb = z.mapColor & 0x00FFFFFF;
            int fillCol = (isHover ? 0x90 : 0x48) << 24 | rgb;
            int edgeCol = (isHover ? 0xFF : 0xC8) << 24 | rgb;

            for (int[] r : z.rects) {
                int x0 = worldToScreenX(r[0]);
                int x1 = worldToScreenX(r[1]);
                int z0 = worldToScreenZ(r[2]);
                int z1 = worldToScreenZ(r[3]);
                GuiComponent.fill(ps, x0, z0, x1, z1, fillCol);
                GuiComponent.fill(ps, x0, z0, x1, z0 + 1, edgeCol);
                GuiComponent.fill(ps, x0, z1 - 1, x1, z1, edgeCol);
                GuiComponent.fill(ps, x0, z0, x0 + 1, z1, edgeCol);
                GuiComponent.fill(ps, x1 - 1, z0, x1, z1, edgeCol);
            }

            int cx = worldToScreenX(z.centerX);
            int cz = worldToScreenZ(z.centerZ);
            GuiComponent.fill(ps, cx - 2, cz - 2, cx + 3, cz + 3, 0xFFFBBF24);
            String label = z.code + " " + z.name;
            font.drawShadow(ps, new TextComponent(label), cx - font.width(label) / 2.0f, cz - 12,
                    isHover ? 0xFFFFFFFF : 0xFFE2E8F0);
        }

        // Live player marker if currently inside the viewed dimension
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null
                && viewingDimId.equals(ModDimensions.toZoneDimensionId(mc.level.dimension()))) {
            int px = worldToScreenX(mc.player.getX());
            int pz = worldToScreenZ(mc.player.getZ());
            if (px >= canvasX && px <= canvasX + canvasW && pz >= canvasY && pz <= canvasY + canvasH) {
                GuiComponent.fill(ps, px - 3, pz - 3, px + 4, pz + 4, 0xFF22C55E);
                GuiComponent.fill(ps, px - 1, pz - 1, px + 2, pz + 2, 0xFFFFFFFF);
                font.drawShadow(ps, new TextComponent("你"), px - 4, pz + 5, 0xFF4ADE80);
            }
        }

        // Sidebar Info Box below zone buttons
        GenshinHudOverlay.drawVoxelPanel(ps, sideX, sideY, sideW, sideH,
                0xB00B1420, 0xFF284664, 0xFFFBBF24);
        int infoTop = sideY + sideH - 54;
        GuiComponent.fill(ps, sideX + 4, infoTop - 3, sideX + sideW - 4, infoTop - 2, 0xFF284664);
        if (hoveredZone != null) {
            font.drawShadow(ps, new TextComponent(hoveredZone.code + " · " + hoveredZone.name), sideX + 6, infoTop, 0xFFFBBF24);
            font.draw(ps, new TextComponent("尺寸:" + hoveredZone.sizeLabel() + " (" + hoverWX + "," + hoverWZ + ")"),
                    sideX + 6, infoTop + 11, 0xFF7DD3FC);
            int ty = infoTop + 22;
            for (FormattedCharSequence line : font.split(new TextComponent(hoveredZone.summary), sideW - 12)) {
                if (ty + 9 > sideY + sideH - 3) break;
                font.draw(ps, line, sideX + 6, ty, 0xFFCBD5E1);
                ty += 10;
            }
        } else {
            String dimFoot = "planet".equals(viewingDimId) ? "第一颗星球 · 11700x9700 格" : "重拾希望号 · 1560x820 格";
            font.drawShadow(ps, new TextComponent(dimFoot), sideX + 6, infoTop, 0xFF38BDF8);
            String coordStr = mouseInCanvas ? ("指向坐标: X=" + hoverWX + ", Z=" + hoverWZ) : "点击任意位置即可直达传送";
            font.draw(ps, new TextComponent(coordStr), sideX + 6, infoTop + 12, 0xFFE2E8F0);
            font.draw(ps, new TextComponent("已自动划定区界，不会覆盖建筑"), sideX + 6, infoTop + 24, 0xFF94A3B8);
        }

        super.render(ps, mouseX, mouseY, partialTicks);
        SystemToastManager.render(ps, this.font, this.width, this.height);
    }

    private static void drawDashedLine(PoseStack ps, int x0, int y0, int x1, int y1, int col) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1.0) return;
        int steps = Math.max(1, (int) (dist / 4.0));
        for (int i = 0; i <= steps; i++) {
            if (i % 2 == 1) continue;
            int px = (int) Mth.lerp(i / (float) steps, x0, x1);
            int py = (int) Mth.lerp(i / (float) steps, y0, y1);
            GuiComponent.fill(ps, px, py, px + 2, py + 2, col);
        }
    }
}
