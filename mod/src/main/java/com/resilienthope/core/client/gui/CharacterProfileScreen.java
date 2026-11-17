package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketSwitchCharacter;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;

public class CharacterProfileScreen extends Screen {

    private int selectedSlot = 0;

    public CharacterProfileScreen() {
        super(new TextComponent("星舰战术终端 · 角色配队面板"));
    }

    @Override
    protected void init() {
        super.init();

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                this.selectedSlot = team.getActiveSlotIndex();
            });
        }

        // 4 Character selection buttons
        int btnW = 95;
        int btnH = 22;
        int btnStartX = 30;
        int btnY = 32;

        String[] charNames = {"1. 特工·曙光", "2. 幽影·刺客", "3. 泰坦·重装", "4. 圣泉·医护"};
        for (int i = 0; i < 4; i++) {
            int slot = i;
            addRenderableWidget(new Button(btnStartX + i * (btnW + 8), btnY, btnW, btnH,
                    new TextComponent(charNames[i]),
                    btn -> this.selectedSlot = slot));
        }

        // Switch to this character button
        int panelX = this.width / 2;
        addRenderableWidget(new Button(panelX + 40, this.height - 45, 120, 20, new TextComponent("切换出战此角色"), btn -> {
            if (player != null) {
                player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                    team.switchSlot(player, selectedSlot);
                    PacketHandler.INSTANCE.sendToServer(new PacketSwitchCharacter(selectedSlot));
                });
            }
        }));

        // Close button
        addRenderableWidget(new Button(this.width - 65, 12, 45, 20, new TextComponent("关闭"), btn -> onClose()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);

        // Frame
        fill(poseStack, 20, 18, this.width - 20, this.height - 18, 0xEE0B121A);
        fill(poseStack, 20, 18, this.width - 20, 20, 0xFF4A90E2);

        font.draw(poseStack, getTitle(), 30, 8, 0xFFFFFFFF);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            CharacterData charData = team.getCharacter(selectedSlot);
            boolean isCurrent = (selectedSlot == team.getActiveSlotIndex());

            // Left: Detailed Numerical Attributes
            int leftX = 35;
            int topY = 70;
            fill(poseStack, leftX - 5, topY - 5, leftX + 220, this.height - 35, 0x60142232);

            font.draw(poseStack, new TextComponent("【角色属性详情】" + (isCurrent ? " (当前出战中)" : "")), leftX, topY, 0xFF55FFFF);

            int statY = topY + 20;
            font.draw(poseStack, new TextComponent("• 角色名称: " + charData.getName()), leftX, statY, 0xFFEEEEEE);
            font.draw(poseStack, new TextComponent("• 生命上限 (Max HP): " + (int)charData.getMaxHp()), leftX, statY + 16, 0xFF4CAF50);
            font.draw(poseStack, new TextComponent("• 当前生命 (HP): " + (int)charData.getCurrentHp()), leftX, statY + 32, 0xFF81C784);
            font.draw(poseStack, new TextComponent("• 基础攻击力 (ATK): 1,840"), leftX, statY + 48, 0xFFFF7043);
            font.draw(poseStack, new TextComponent("• 基础防御力 (DEF): 820"), leftX, statY + 64, 0xFF90CAF9);
            font.draw(poseStack, new TextComponent("• 暴击率 (Crit Rate): 45.2%"), leftX, statY + 80, 0xFFFFD54F);
            font.draw(poseStack, new TextComponent("• 暴击伤害 (Crit DMG): 142.8%"), leftX, statY + 96, 0xFFFFB74D);
            font.draw(poseStack, new TextComponent("• 聚变充能效率 (ER): 135.0%"), leftX, statY + 112, 0xFF4FC3F7);
            font.draw(poseStack, new TextComponent("• 爆发能量: " + (int)charData.getCurrentEnergy() + "/100"), leftX, statY + 128, 0xFFCE93D8);

            // Right: 5 Chip Relic Slots
            int rightX = this.width / 2 + 10;
            fill(poseStack, rightX - 5, topY - 5, this.width - 30, this.height - 55, 0x60142232);

            font.draw(poseStack, new TextComponent("【科技战术芯片 (圣遗物 5 槽位)】"), rightX, topY, 0xFFFFCC00);

            String[] slotNames = {
                    "槽位 1【核心】: 聚变脉冲核心芯片 ★★★★ (+16)",
                    "槽位 2【动力】: 虚空驱动脉冲器 ★★★★★ (+20)",
                    "槽位 3【传动】: 高精伺服齿轮组 ★★★★ (+12)",
                    "槽位 4【偏转】: 偏振电磁力场护盾 ★★★★★ (+20)",
                    "槽位 5【运算】: 量子神谕逻辑微芯片 ★★★★★ (+20)"
            };

            for (int s = 0; s < 5; s++) {
                int slotY = topY + 22 + s * 24;
                fill(poseStack, rightX, slotY, this.width - 35, slotY + 20, 0x50182E44);
                font.draw(poseStack, new TextComponent(slotNames[s]), rightX + 8, slotY + 6, 0xFFE0E0E0);
            }
        });

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
