package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketNpcAction;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class NpcDialogueScreen extends Screen {
    private static final ResourceLocation TEX_ZHENG = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_zheng.png");
    private static final ResourceLocation TEX_HAN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_han.png");
    private static final ResourceLocation TEX_SU = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_su.png");

    private final String npcId;
    private final int questStage;
    private final List<String> pages = new ArrayList<>();
    private int pageIndex = 0;
    private int revealChars = 0;
    private boolean buttonsBuilt = false;

    public NpcDialogueScreen(String npcId, int questStage) {
        super(new TextComponent("阵营交谈"));
        this.npcId = npcId;
        this.questStage = questStage;
        buildScript();
    }

    private void buildScript() {
        if (questStage == 2 && "zheng".equals(npcId)) {
            pages.add("特工破晓！你终于从B区杀出来了。连廊的安保无人机刚才集体被舰桥外网的未知信号劫持了。");
            pages.add("我刚重启了副反应堆，但主聚变炉冷却液已经见底，韩振岳那帮舰桥军官死守指令只肯削减下层配给。");
            pages.add("这是机械工会刚赶工出来的【2号位·脉冲动力发生器】芯片和强化合金。升降梯我已经给你解锁了，下去把守卫清光，再重启下层核心！");
        } else if (questStage == 4) {
            pages.add("【苏挽星】：辅助反应堆虽然稳定了，但重元素燃料最多只能再撑十四天。只有空降近地轨道的极寒死星矿坑，拿到先驱文明留下的高能火种才有一线生机！");
            pages.add("【韩振岳】：胡闹！地表零下七十度，还盘踞着失控的先驱重型挖掘机甲。全舰进入深空静默休眠才是零伤亡的最稳方案。");
            pages.add("【郑铁山】：特工破晓，我们工会和科学院站在同一边！我已配发 3/4 号位芯片和【极寒高频裂解刃】，请从西侧轨道空投舱出发，带回先驱火种！");
        } else if (questStage == 7) {
            pages.add("先驱聚变火种已经带回聚变大厅！现在重拾希望号的三方核心代表都在此等候你的最终裁断——");
            pages.add("【郑铁山（底层机械工会）】：将火种全面接入平民生活区与供暖网络，废除严苛军事配给，靠全舰工人共同建造新家园！");
            pages.add("【韩振岳（舰桥指挥层）】：将火种专供主引擎跃迁与军工防卫，以铁腕秩序跨越深空危机，不惜代价保全人类火种！");
            pages.add("【苏挽星（深空科学派）】：解析火种中的先驱科技与基因共鸣，让人类的体质与星舰技术共同跃升到全新纪元！");
        } else {
            switch (npcId) {
                case "han" -> pages.add("舰桥防卫总长韩振岳在此戒备。只有钢铁般的纪律，才能让这艘载着数万人的星舰穿过黑暗深空。");
                case "su" -> pages.add("深空科学院首席苏挽星向你致意。记住：按 [B] 强化芯片、按 [C] 查看真实双暴面板、按 [G] 使用终端数据复现！");
                default -> pages.add("机械工会总工老郑随时待命。只要炉火不灭，重拾希望号就永远不会停下脚步！");
            }
        }
    }

    private String getSpeakerTitle() {
        return switch (npcId) {
            case "han" -> "【舰桥防卫总长】韩振岳";
            case "su" -> "【深空科学院首席】苏挽星";
            default -> "【机械工会总工】郑铁山";
        };
    }

    private ResourceLocation getPortrait() {
        return switch (npcId) {
            case "han" -> TEX_HAN;
            case "su" -> TEX_SU;
            default -> TEX_ZHENG;
        };
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        boolean onLastPage = pageIndex >= pages.size() - 1;
        int btnX = this.width - 250;
        int btnBaseY = this.height - 135;

        if (!onLastPage) {
            addRenderableWidget(new Button(btnX, btnBaseY + 60, 210, 22, new TextComponent("下一句 ▶ (Space)"), btn -> advancePage()));
            return;
        }

        if (questStage == 2 && "zheng".equals(npcId)) {
            addRenderableWidget(new Button(btnX, btnBaseY + 25, 220, 22,
                    new TextComponent("◆ 接收芯片与合金，开启下层升降梯"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "advance_stage_2"));
                onClose();
            }));
        } else if (questStage == 4) {
            addRenderableWidget(new Button(btnX, btnBaseY + 25, 220, 22,
                    new TextComponent("◆ 领取寒刃与芯片，前往轨道空投"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "advance_stage_4"));
                onClose();
            }));
        } else if (questStage == 7) {
            addRenderableWidget(new Button(btnX - 20, btnBaseY - 12, 240, 20,
                    new TextComponent("① 【众生薪火】支持机械工会·郑铁山"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "choose_zheng"));
                onClose();
            }));
            addRenderableWidget(new Button(btnX - 20, btnBaseY + 12, 240, 20,
                    new TextComponent("② 【铁律远征】支持舰桥防卫·韩振岳"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "choose_han"));
                onClose();
            }));
            addRenderableWidget(new Button(btnX - 20, btnBaseY + 36, 240, 20,
                    new TextComponent("③ 【新人类黎明】支持深空科学·苏挽星"), btn -> {
                PacketHandler.INSTANCE.sendToServer(new PacketNpcAction(npcId, "choose_su"));
                onClose();
            }));
        }

        addRenderableWidget(new Button(btnX, btnBaseY + 62, 220, 20, new TextComponent("结束交谈 (ESC)"), btn -> onClose()));
    }

    private void advancePage() {
        String full = pages.get(pageIndex);
        if (revealChars < full.length()) {
            revealChars = full.length();
            return;
        }
        if (pageIndex < pages.size() - 1) {
            pageIndex++;
            revealChars = 0;
            rebuildButtons();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (pageIndex < pages.size()) {
            String full = pages.get(pageIndex);
            if (revealChars < full.length()) {
                revealChars = Math.min(full.length(), revealChars + 3);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_F) {
            advancePage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // Bottom half-screen Genshin dialogue panel
        int boxW = Math.min(this.width - 40, 560);
        int boxH = 112;
        int boxX = (this.width - boxW) / 2;
        int boxY = this.height - boxH - 16;

        GenshinHudOverlay.drawVoxelPanel(poseStack, boxX, boxY, boxW, boxH, 0xEC09111A, 0xFF4A90E2, 0xFFFFD54F);

        // Portrait Frame
        GenshinHudOverlay.drawVoxelPanel(poseStack, boxX + 12, boxY + 14, 56, 56, 0xD00B1522, 0xFF4FC3F7, 0xFFFFCC00);
        GenshinHudOverlay.drawTexture(poseStack, getPortrait(), boxX + 16, boxY + 18, 48, 48, 32, 32);

        font.drawShadow(poseStack, new TextComponent(getSpeakerTitle()), boxX + 80, boxY + 14, 0xFFFFD54F);

        if (pageIndex < pages.size()) {
            String full = pages.get(pageIndex);
            String visible = full.substring(0, Math.min(full.length(), revealChars));
            int ty = boxY + 32;
            for (FormattedCharSequence line : font.split(new TextComponent(visible), boxW - 104)) {
                font.draw(poseStack, line, boxX + 80, ty, 0xFFF2F6FA);
                ty += 12;
            }
        }

        String pageHint = String.format("(%d / %d)", pageIndex + 1, pages.size());
        font.draw(poseStack, new TextComponent(pageHint), boxX + 18, boxY + 80, 0xFF88AABB);

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
