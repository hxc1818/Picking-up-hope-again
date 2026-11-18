package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketProgressionAction;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class SimulationDomainScreen extends Screen {
    private static final String[][] DOMAINS = {
            {"domain_credit", "信用币刷取副本", "消耗 20 体力  |  稳定产出：信用币 x120"},
            {"domain_exp", "角色经验副本", "消耗 20 体力  |  稳定产出：角色经验瓶 x3"},
            {"domain_char", "人物突破材料副本", "消耗 20 体力  |  稳定产出：1级人物零件 x2（可合成高阶）"},
            {"domain_weapon", "武器突破材料副本", "消耗 20 体力  |  稳定产出：1级武器构件 x2（与人物材料独立）"},
            {"domain_talent", "天赋升级材料副本", "消耗 20 体力  |  稳定产出：1级天赋手册 x2（提升技能倍率）"},
            {"world_trial", "世界等级突破试炼", "消耗 0 体力   |  主等级达标后挑战，开放角色与武器突破上限"}
    };

    private int selectedIdx = 0;
    private SciFiButton enterBtn;
    private final List<SciFiButton> listButtons = new ArrayList<>();

    public SimulationDomainScreen() {
        super(new TextComponent("模拟训练副本终端 (E)"));
    }

    @Override
    protected void init() {
        super.init();
        listButtons.clear();

        int listX = 20;
        int listY = 48;
        int listW = Math.min(180, (int) ((this.width - 56) * 0.44f));
        for (int i = 0; i < DOMAINS.length; i++) {
            final int idx = i;
            SciFiButton b = addRenderableWidget(new SciFiButton(listX, listY + i * 24, listW, 20,
                    new TextComponent(DOMAINS[i][1]), btn -> {
                this.selectedIdx = idx;
                updateSelectionColors();
            }));
            listButtons.add(b);
        }

        int rightX = listX + listW + 10;
        int rightW = this.width - rightX - 20;
        this.enterBtn = addRenderableWidget(new SciFiButton(rightX, this.height - 38, Math.min(160, rightW), 20,
                new TextComponent("进入副本战斗"), btn -> {
            String domainId = DOMAINS[selectedIdx][0];
            onClose();
            PacketHandler.INSTANCE.sendToServer(
                    new PacketProgressionAction(PacketProgressionAction.ACT_START_DOMAIN, 0, 0, domainId));
        }, 0xFF4ADE80));

        addRenderableWidget(new SciFiButton(this.width - 62, 12, 42, 16, new TextComponent("关闭"), btn -> onClose(), 0xFFEF4444));
        updateSelectionColors();
    }

    private void updateSelectionColors() {
        for (int i = 0; i < listButtons.size(); i++) {
            listButtons.get(i).setAccentColor(i == selectedIdx ? 0xFFFBBF24 : 0xFF38BDF8);
        }
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ps);

        GenshinHudOverlay.drawVoxelPanel(ps, 12, 8, this.width - 24, this.height - 16,
                0xEE0B121D, 0xFF284868, 0xFF38BDF8);
        font.drawShadow(ps, getTitle(), 20, 14, 0xFF38BDF8);

        Player p = Minecraft.getInstance().player;
        if (p != null) {
            p.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                String info = String.format("主Lv.%d/%d   |   世界Lv.%d   |   体力: %d / 180",
                        team.getPlayerLevel(), team.getPlayerLevelCapForWorldLevel(),
                        team.getWorldLevel(), team.getStamina());
                font.draw(ps, new TextComponent(info), 20, 30, 0xFFFBBF24);

                boolean isTrial = "world_trial".equals(DOMAINS[selectedIdx][0]);
                if (enterBtn != null) {
                    enterBtn.active = isTrial ? team.canBreakthroughWorldLevel() : (team.getStamina() >= 20);
                }
            });
        }

        int listW = Math.min(180, (int) ((this.width - 56) * 0.44f));
        int rightX = 20 + listW + 10;
        int rightW = this.width - rightX - 20;
        int detailH = Math.max(80, this.height - 96);
        GenshinHudOverlay.drawVoxelPanel(ps, rightX, 48, rightW, detailH,
                0xA00D1724, 0xFF26425C, 0xFFFBBF24);

        String[] sel = DOMAINS[selectedIdx];
        font.drawShadow(ps, new TextComponent("【" + sel[1] + "】"), rightX + 10, 58, 0xFFFBBF24);
        int dy = 74;
        for (FormattedCharSequence line : font.split(new TextComponent(sel[2]), rightW - 20)) {
            font.draw(ps, line, rightX + 10, dy, 0xFFE2E8F0);
            dy += 11;
        }
        dy += 6;
        font.draw(ps, new TextComponent("说明："), rightX + 10, dy, 0xFF7DD3FC);
        dy += 12;
        for (String tip : new String[]{
                "• 所有战斗均进入独立平坦回合制舞台。",
                "• 打怪不自动加角色或装备经验，请使用产出道具手动升级。",
                "• 达到主等级上限后可挑战世界突破试炼。"
        }) {
            for (FormattedCharSequence line : font.split(new TextComponent(tip), rightW - 20)) {
                if (dy + 10 > 48 + detailH - 4) break;
                font.draw(ps, line, rightX + 10, dy, 0xFF94A3B8);
                dy += 11;
            }
        }

        super.render(ps, mouseX, mouseY, partialTicks);
    }
}
