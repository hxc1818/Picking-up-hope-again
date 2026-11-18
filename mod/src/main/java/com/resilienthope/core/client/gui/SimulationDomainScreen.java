package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketProgressionAction;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;

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
    private Button enterBtn;

    public SimulationDomainScreen() {
        super(new TextComponent("模拟训练副本终端 (E)"));
    }

    @Override
    protected void init() {
        super.init();

        int listX = 32;
        int listY = 60;
        for (int i = 0; i < DOMAINS.length; i++) {
            final int idx = i;
            addRenderableWidget(new Button(listX, listY + i * 26, 175, 22,
                    new TextComponent(DOMAINS[i][1]), btn -> this.selectedIdx = idx));
        }

        int rightX = 226;
        int bottomY = this.height - 46;
        this.enterBtn = addRenderableWidget(new Button(rightX, bottomY, 150, 22,
                new TextComponent("进入副本战斗"), btn -> {
            String domainId = DOMAINS[selectedIdx][0];
            onClose();
            PacketHandler.INSTANCE.sendToServer(
                    new PacketProgressionAction(PacketProgressionAction.ACT_START_DOMAIN, 0, 0, domainId));
        }));

        addRenderableWidget(new Button(this.width - 70, 14, 48, 18, new TextComponent("关闭"), btn -> onClose()));
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ps);

        GenshinHudOverlay.drawVoxelPanel(ps, 18, 10, this.width - 36, this.height - 20,
                0xEE0B121D, 0xFF284868, 0xFF38BDF8);
        font.drawShadow(ps, getTitle(), 32, 18, 0xFF38BDF8);

        Player p = Minecraft.getInstance().player;
        if (p != null) {
            p.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                String info = String.format("主等级 Lv.%d/%d   |   世界等级 Lv.%d   |   当前体力: %d / 180",
                        team.getPlayerLevel(), team.getPlayerLevelCapForWorldLevel(),
                        team.getWorldLevel(), team.getStamina());
                font.draw(ps, new TextComponent(info), 32, 36, 0xFFFBBF24);

                boolean isTrial = "world_trial".equals(DOMAINS[selectedIdx][0]);
                if (enterBtn != null) {
                    enterBtn.active = isTrial ? team.canBreakthroughWorldLevel() : (team.getStamina() >= 20);
                }
            });
        }

        int rightX = 226;
        int rightW = this.width - rightX - 32;
        GenshinHudOverlay.drawVoxelPanel(ps, rightX, 60, rightW, this.height - 118,
                0xA00D1724, 0xFF26425C, 0xFFFBBF24);

        String[] sel = DOMAINS[selectedIdx];
        font.drawShadow(ps, new TextComponent("【" + sel[1] + "】"), rightX + 12, 74, 0xFFFBBF24);
        font.draw(ps, new TextComponent(sel[2]), rightX + 12, 96, 0xFFE2E8F0);
        font.draw(ps, new TextComponent("说明："), rightX + 12, 122, 0xFF7DD3FC);
        font.draw(ps, new TextComponent("• 所有战斗均进入平坦独立的回合制舞台进行。"), rightX + 12, 138, 0xFF94A3B8);
        font.draw(ps, new TextComponent("• 战斗不会自动给角色或装备增加经验，请用刷取的道具手动培养。"), rightX + 12, 154, 0xFF94A3B8);
        if ("world_trial".equals(sel[0])) {
            font.draw(ps, new TextComponent("• 当主等级到达当前档位上限后，突破试炼按钮将自动点亮。"), rightX + 12, 170, 0xFF4ADE80);
        }

        super.render(ps, mouseX, mouseY, partialTicks);
    }
}
