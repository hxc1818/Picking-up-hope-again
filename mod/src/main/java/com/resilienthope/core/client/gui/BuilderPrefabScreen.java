package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.client.builder.BuilderPrefabClientState;
import com.resilienthope.core.network.PacketBuilderPrefabAction;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.subtitle.SystemToastManager;
import com.resilienthope.core.world.prefab.PrefabPlacementManager;
import com.resilienthope.core.world.prefab.PrefabRegistry;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Interactive sci-fi library GUI for browsing 28 modular prefabs + Custom Clipboard,
 * adjusting rotation, repeat segments, Y-offset, and stamping/undoing structures.
 */
public class BuilderPrefabScreen extends Screen {
    private PrefabRegistry.Category activeCategory = PrefabRegistry.Category.STARSHIP;
    private boolean viewingClipboard = false;

    private int listX, listY, listW, listH;
    private int detailX, detailY, detailW, detailH;

    public BuilderPrefabScreen() {
        super(new TextComponent("可复用预制结构与蓝图库 (K)"));
        if (BuilderPrefabClientState.isClipboardSelected()) {
            viewingClipboard = true;
        } else {
            PrefabRegistry.PrefabDef cur = PrefabRegistry.findById(BuilderPrefabClientState.getSelectedId());
            if (cur != null) {
                activeCategory = cur.category;
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

        int outerX = 12;
        int outerW = this.width - 24;
        this.listX = outerX + 8;
        this.listY = 50;
        this.detailW = Math.min(208, Math.max(150, (int) (outerW * 0.34f)));
        this.listW = Math.max(180, outerW - detailW - 22);
        this.listH = Math.max(110, this.height - 92);
        this.detailX = this.listX + this.listW + 8;
        this.detailY = this.listY;
        this.detailH = this.listH;

        // Top close button
        addRenderableWidget(new SciFiButton(this.width - 62, 11, 44, 16,
                new TextComponent("关闭"), btn -> onClose(), 0xFFEF4444));

        // Category tabs row
        boolean showClip = BuilderPrefabClientState.hasClipboard();
        PrefabRegistry.Category[] cats = PrefabRegistry.Category.values();
        int tabCount = cats.length + (showClip ? 1 : 0);
        int tabW = Math.min(110, (outerW - 16 - (tabCount - 1) * 4) / tabCount);
        int tabX = listX;
        for (PrefabRegistry.Category cat : cats) {
            boolean sel = !viewingClipboard && activeCategory == cat;
            addRenderableWidget(new SciFiButton(tabX, 28, tabW, 18,
                    new TextComponent(cat.label), btn -> {
                viewingClipboard = false;
                activeCategory = cat;
                List<PrefabRegistry.PrefabDef> list = PrefabRegistry.getByCategory(cat);
                if (!list.isEmpty()) {
                    PrefabRegistry.PrefabDef cur = PrefabRegistry.findById(BuilderPrefabClientState.getSelectedId());
                    if (cur == null || cur.category != cat) {
                        BuilderPrefabClientState.selectPrefab(list.get(0).id);
                    }
                }
                init();
            }, sel ? 0xFFFBBF24 : cat.accentColor));
            tabX += tabW + 4;
        }
        if (showClip) {
            addRenderableWidget(new SciFiButton(tabX, 28, tabW, 18,
                    new TextComponent("自定义剪贴板"), btn -> {
                viewingClipboard = true;
                BuilderPrefabClientState.selectPrefab(PrefabPlacementManager.CLIPBOARD_ID);
                init();
            }, viewingClipboard ? 0xFFFBBF24 : 0xFF22C55E));
        }

        // Left 2-column prefab selector cards (or clipboard card)
        if (!viewingClipboard) {
            List<PrefabRegistry.PrefabDef> defs = PrefabRegistry.getByCategory(activeCategory);
            int cols = 2;
            int colW = (listW - 14) / cols;
            int rows = Math.max(1, (defs.size() + 1) / 2);
            int cardH = Math.min(34, Math.max(22, (listH - 12 - (rows - 1) * 4) / rows));
            for (int i = 0; i < defs.size(); i++) {
                PrefabRegistry.PrefabDef def = defs.get(i);
                int c = i % cols;
                int r = i / cols;
                int bx = listX + 4 + c * (colW + 4);
                int by = listY + 6 + r * (cardH + 4);
                boolean selected = def.id.equals(BuilderPrefabClientState.getSelectedId());
                String label = def.name + " (" + def.sizeLabel() + ")";
                addRenderableWidget(new SciFiButton(bx, by, colW, cardH,
                        new TextComponent(label), btn -> {
                    BuilderPrefabClientState.selectPrefab(def.id);
                    init();
                }, selected ? 0xFFFBBF24 : def.category.accentColor));
            }
        } else {
            String clipLabel = String.format("自定义剪贴板 (%d×%d×%d)",
                    BuilderPrefabClientState.getClipX(),
                    BuilderPrefabClientState.getClipY(),
                    BuilderPrefabClientState.getClipZ());
            addRenderableWidget(new SciFiButton(listX + 8, listY + 12, listW - 16, 30,
                    new TextComponent(clipLabel), btn -> {
                BuilderPrefabClientState.selectPrefab(PrefabPlacementManager.CLIPBOARD_ID);
                init();
            }, 0xFF22C55E));
        }

        // Right parameter panel buttons (anchored nicely inside detail box)
        int px = detailX + 8;
        int pw = detailW - 16;
        int py = detailY + 86;

        addRenderableWidget(new SciFiButton(px, py, pw, 18,
                new TextComponent("朝向：" + BuilderPrefabClientState.getFacingLabel() + "  (点此顺时针转90°)"), btn -> {
            BuilderPrefabClientState.rotateCW();
            init();
        }, 0xFF38BDF8));

        py += 22;
        int miniW = 24;
        addRenderableWidget(new SciFiButton(px, py, miniW, 18, new TextComponent("-"), btn -> {
            BuilderPrefabClientState.adjustRepeat(-1);
            init();
        }, 0xFF64748B));
        addRenderableWidget(new SciFiButton(px + pw - miniW, py, miniW, 18, new TextComponent("+"), btn -> {
            BuilderPrefabClientState.adjustRepeat(1);
            init();
        }, 0xFF38BDF8));

        py += 22;
        addRenderableWidget(new SciFiButton(px, py, miniW, 18, new TextComponent("-"), btn -> {
            BuilderPrefabClientState.adjustYOffset(-1);
            init();
        }, 0xFF64748B));
        addRenderableWidget(new SciFiButton(px + pw - miniW, py, miniW, 18, new TextComponent("+"), btn -> {
            BuilderPrefabClientState.adjustYOffset(1);
            init();
        }, 0xFF38BDF8));

        py += 22;
        boolean airOnly = BuilderPrefabClientState.isAirOnly();
        addRenderableWidget(new SciFiButton(px, py, pw, 18,
                new TextComponent("放置模式：" + (airOnly ? "仅填充空气(保护已有方块)" : "完整覆盖(含室内掏空)")), btn -> {
            BuilderPrefabClientState.toggleAirOnly();
            init();
        }, airOnly ? 0xFF4ADE80 : 0xFFFB923C));

        // Bottom action bar
        int barY = this.height - 34;
        int bw = Math.min(126, (outerW - 20) / 4);
        int bx = listX;
        addRenderableWidget(new SciFiButton(bx, barY, bw, 20,
                new TextComponent("领取蓝图投影仪"), btn -> {
            PacketHandler.INSTANCE.sendToServer(PacketBuilderPrefabAction.giveProjector());
            onClose();
        }, 0xFF38BDF8));

        bx += bw + 4;
        addRenderableWidget(new SciFiButton(bx, barY, bw, 20,
                new TextComponent("脚下直接放置"), btn -> {
            PacketHandler.INSTANCE.sendToServer(PacketBuilderPrefabAction.placeAtPlayer(
                    BuilderPrefabClientState.getSelectedId(),
                    BuilderPrefabClientState.getRotSteps(),
                    BuilderPrefabClientState.getRepeatCount(),
                    BuilderPrefabClientState.getYOffset(),
                    BuilderPrefabClientState.isAirOnly()));
            onClose();
        }, 0xFF4ADE80));

        bx += bw + 4;
        addRenderableWidget(new SciFiButton(bx, barY, bw, 20,
                new TextComponent("撤回上次 (Undo)"), btn -> {
            PacketHandler.INSTANCE.sendToServer(PacketBuilderPrefabAction.undo());
        }, 0xFFFBBF24));

        bx += bw + 4;
        addRenderableWidget(new SciFiButton(bx, barY, bw, 20,
                new TextComponent("切换全图蓝图 (M)"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new BuilderRegionMapScreen());
            }
        }, 0xFFC084FC));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_K) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ps);

        GenshinHudOverlay.drawVoxelPanel(ps, 12, 8, this.width - 24, this.height - 16,
                0xF208101A, 0xFF284664, 0xFFFBBF24);
        font.drawShadow(ps, getTitle(), 20, 14, 0xFF38BDF8);
        font.draw(ps, new TextComponent("手持[蓝图投影仪]: 左键选模 · 右键准星放置 · Shift+右键旋转 · Shift+左键撤回"),
                Math.min(this.width - 310, 195), 14, 0xFF94A3B8);

        GenshinHudOverlay.drawVoxelPanel(ps, listX, listY, listW, listH, 0xB8050B14, 0xFF1E3A5F, 0xFF38BDF8);
        GenshinHudOverlay.drawVoxelPanel(ps, detailX, detailY, detailW, detailH, 0xCC0A1320, 0xFF284664, 0xFFFBBF24);

        // Selected structure header & description inside right detail panel
        int px = detailX + 8;
        int pw = detailW - 16;
        String name = BuilderPrefabClientState.getActiveName();
        String sizeStr = String.format("尺寸: %d×%d×%d | 步距: %d格",
                BuilderPrefabClientState.getActiveSizeX(),
                BuilderPrefabClientState.getActiveSizeY(),
                BuilderPrefabClientState.getActiveSizeZ(),
                BuilderPrefabClientState.getActiveStep());
        font.drawShadow(ps, new TextComponent(name), px, detailY + 8, 0xFFFBBF24);
        font.draw(ps, new TextComponent(sizeStr), px, detailY + 20, 0xFF7DD3FC);

        String desc;
        if (BuilderPrefabClientState.isClipboardSelected()) {
            desc = "已保存 /hope pos1~pos2 复制的区域方块。可用投影仪任意旋转 4 个方向并连续复制粘贴！";
        } else {
            PrefabRegistry.PrefabDef def = PrefabRegistry.findById(BuilderPrefabClientState.getSelectedId());
            desc = def != null ? def.description : "";
        }
        int ty = detailY + 33;
        for (FormattedCharSequence line : font.split(new TextComponent(desc), pw)) {
            if (ty + 9 > detailY + 82) break;
            font.draw(ps, line, px, ty, 0xFFCBD5E1);
            ty += 10;
        }
        GuiComponent.fill(ps, px, detailY + 82, px + pw, detailY + 83, 0xFF1E3A5F);

        // Parameter labels centered between - / + buttons
        String repLabel = "连续拼接段数：" + BuilderPrefabClientState.getRepeatCount() + " 段";
        font.drawShadow(ps, new TextComponent(repLabel),
                detailX + (detailW - font.width(repLabel)) / 2.0f, detailY + 113, 0xFFF1F5F9);

        int yOff = BuilderPrefabClientState.getYOffset();
        String yLabel = "上下高度偏移：" + (yOff > 0 ? "+" + yOff : String.valueOf(yOff)) + " 格";
        font.drawShadow(ps, new TextComponent(yLabel),
                detailX + (detailW - font.width(yLabel)) / 2.0f, detailY + 135, 0xFFF1F5F9);

        // Command copy/paste cheat sheet in bottom of right panel
        int tipY = detailY + detailH - 36;
        if (tipY > detailY + 172) {
            GuiComponent.fill(ps, px, tipY - 4, px + pw, tipY - 3, 0xFF1E3A5F);
            font.draw(ps, new TextComponent("自定义区域复制指令:"), px, tipY, 0xFF38BDF8);
            font.draw(ps, new TextComponent("/hope pos1 → /hope pos2 → /hope copy"), px, tipY + 11, 0xFF94A3B8);
            font.draw(ps, new TextComponent("/hope undo 可连退最近 20 步"), px, tipY + 22, 0xFF94A3B8);
        }

        super.render(ps, mouseX, mouseY, partialTicks);
        SystemToastManager.render(ps, this.font, this.width, this.height);
    }
}
