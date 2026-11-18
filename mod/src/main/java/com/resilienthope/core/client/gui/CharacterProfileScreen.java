package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketEquipItem;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketProgressionAction;
import com.resilienthope.core.network.PacketSwitchCharacter;
import com.resilienthope.core.subtitle.RadioSubtitleManager;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class CharacterProfileScreen extends Screen {
    private int selectedEquipSlot = -1; // -1 = weapon, 0..4 = chips
    private Button enhanceSlotBtn;
    private Button secondarySlotBtn;
    private Button worldBreakBtn;

    public CharacterProfileScreen() {
        super(new TextComponent("角色与装备养成终端 (C)"));
    }

    @Override
    protected void init() {
        super.init();
        Player player = Minecraft.getInstance().player;

        int btnW = 76;
        for (int i = 0; i < 4; i++) {
            int slot = i;
            addRenderableWidget(new Button(24 + i * (btnW + 4), 46, btnW, 18,
                    new TextComponent("槽位 " + (i + 1)), btn -> {
                if (player == null) return;
                player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                    CharacterData c = team.getCharacter(slot);
                    if (!c.isUnlocked()) {
                        RadioSubtitleManager.postRadioMessage("编队系统", "该位置队友尚未解锁（推进剧情或抽卡可招募更多角色）。", 70);
                    } else {
                        team.switchSlot(player, slot);
                        PacketHandler.INSTANCE.sendToServer(new PacketSwitchCharacter(slot));
                    }
                });
            }));
        }

        addRenderableWidget(new Button(348, 46, 86, 18, new TextComponent("轮换其他角色"), btn -> {
            if (player == null) return;
            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                int slot = team.getActiveSlotIndex();
                int curRoster = team.getPartyRosterIndex(slot);
                for (int step = 1; step <= TeamCapability.ROSTER_SIZE; step++) {
                    int candidate = (curRoster + step) % TeamCapability.ROSTER_SIZE;
                    if (team.getRosterCharacter(candidate).isUnlocked()) {
                        PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(
                                PacketProgressionAction.ACT_SET_PARTY_SLOT, slot, candidate, ""));
                        break;
                    }
                }
            });
        }));

        this.worldBreakBtn = addRenderableWidget(new Button(this.width - 170, 14, 96, 18,
                new TextComponent("世界突破试炼"), btn -> Minecraft.getInstance().setScreen(new SimulationDomainScreen())));

        addRenderableWidget(new Button(this.width - 68, 14, 46, 18, new TextComponent("关闭"), btn -> onClose()));

        // Left Column Progression Buttons (EXP, Char Ascend, Talent Upgrade)
        int leftX = 24;
        int progY = this.height - 40;
        addRenderableWidget(new Button(leftX, progY, 68, 20, new TextComponent("经验瓶+1级"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_USE_EXP_BOTTLE, 0, 0, ""))));

        addRenderableWidget(new Button(leftX + 71, progY, 64, 20, new TextComponent("角色突破"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_ASCEND_CHAR, 0, 0, ""))));

        addRenderableWidget(new Button(leftX + 138, progY, 64, 20, new TextComponent("天赋+1"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_UPGRADE_TALENT, 0, 0, ""))));

        // Right Column Equipment Buttons
        int rightX = this.width / 2 + 40;
        this.enhanceSlotBtn = addRenderableWidget(new Button(rightX, progY, 76, 20, new TextComponent("强化 (+1)"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_EQUIPPED, selectedEquipSlot, ItemStack.EMPTY))));

        this.secondarySlotBtn = addRenderableWidget(new Button(rightX + 80, progY, 64, 20, new TextComponent("突破/卸下"), btn -> {
            if (selectedEquipSlot == -1) {
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_ASCEND_WEAPON, 0, 0, ""));
            } else {
                PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_UNEQUIP_CHIP, selectedEquipSlot, ItemStack.EMPTY));
            }
        }));

        addRenderableWidget(new Button(rightX + 148, progY, 76, 20, new TextComponent("背包替换/溶解"), btn -> {
            InfiniteInventoryScreen.Category cat = (selectedEquipSlot == -1)
                    ? InfiniteInventoryScreen.Category.WEAPONS
                    : InfiniteInventoryScreen.Category.CHIPS;
            Minecraft.getInstance().setScreen(new InfiniteInventoryScreen(cat));
        }));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rightX = this.width / 2 + 40;
        int listTop = 74;
        int rightEnd = this.width - 24;
        if (mouseX >= rightX && mouseX <= rightEnd) {
            for (int row = 0; row < 6; row++) {
                int slotY = listTop + row * 25;
                if (mouseY >= slotY && mouseY <= slotY + 22) {
                    this.selectedEquipSlot = row - 1;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int countItem(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ps);
        GenshinHudOverlay.drawVoxelPanel(ps, 14, 10, this.width - 28, this.height - 20,
                0xF009101A, 0xFF284868, 0xFF38BDF8);

        font.drawShadow(ps, getTitle(), 24, 15, 0xFF38BDF8);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int expBottles = countItem(player, ModItems.EXP_BOTTLE.get()) + countItem(player, Items.EXPERIENCE_BOTTLE);
        int dust = countItem(player, ModItems.EQUIP_ESSENCE.get());
        int credits = countItem(player, ModItems.CREDIT_COIN.get());

        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            // Top Status Strip
            String topStrip = String.format("主等级 Lv.%d/%d (%d/%d)  |  世界等级 Lv.%d (角色上限 Lv.%d)  |  体力 %d/180  |  信用币 %d  |  经验瓶 %d  |  粉尘 %d",
                    team.getPlayerLevel(), team.getPlayerLevelCapForWorldLevel(),
                    team.getPlayerExp(), team.getPlayerExpNeeded(),
                    team.getWorldLevel(), (team.getWorldLevel() + 2) * 10 > 80 ? 80 : 20 + team.getWorldLevel() * 10,
                    team.getStamina(), credits, expBottles, dust);
            font.draw(ps, new TextComponent(topStrip), 24, 30, 0xFFE2E8F0);

            if (worldBreakBtn != null) {
                worldBreakBtn.setMessage(new TextComponent(team.canBreakthroughWorldLevel() ? "★可突破世界等级" : "模拟副本(E)"));
            }

            // Update party slot button labels cleanly
            for (int i = 0; i < 4 && i < this.renderables.size(); i++) {
                if (this.renderables.get(i) instanceof Button b) {
                    CharacterData cd = team.getCharacter(i);
                    String sn = cd.getName().contains("·") ? cd.getName().substring(cd.getName().indexOf('·') + 1) : cd.getName();
                    b.setMessage(new TextComponent((i + 1) + "." + (cd.isUnlocked() ? sn : "未解锁")));
                }
            }

            CharacterData active = team.getCharacter(team.getActiveSlotIndex());
            int panelTop = 70;
            int panelH = this.height - 116;

            // 1. Left Column: Character Card & Progression Materials
            int leftX = 24;
            int leftW = 136;
            int elemCol = CharacterData.getElementColor(active.getElement());
            GenshinHudOverlay.drawVoxelPanel(ps, leftX, panelTop, leftW, panelH, 0xB80E1622, elemCol, elemCol);

            GenshinHudOverlay.drawTexture(ps, TurnBasedBattleScreen.getPortraitForChar(active.getCharId()),
                    leftX + 8, panelTop + 8, 36, 36, 32, 32);

            font.drawShadow(ps, new TextComponent(active.getName()), leftX + 50, panelTop + 10, 0xFFF8FAFC);
            font.draw(ps, new TextComponent("属性：" + active.getElementLabel() + "   职业：" + active.getRoleLabel()),
                    leftX + 50, panelTop + 23, elemCol);
            font.draw(ps, new TextComponent("Lv." + active.getLevel() + " / " + active.getMaxLevelForAscension()
                    + "   潜能+" + active.getEidolon()), leftX + 50, panelTop + 35, 0xFFFBBF24);

            // Level & Talent requirements
            int reqTier = Math.min(6, active.getAscension() + 1);
            int reqMatCount = 2 + active.getAscension();
            int haveCharMat = countItem(player, ModItems.getCharMat(reqTier));

            int talTier = Math.min(6, 1 + (active.getTalentLevel() / 2));
            int reqTalCount = 1 + (active.getTalentLevel() % 2);
            int haveTalMat = countItem(player, ModItems.getTalentMat(talTier));

            int infoY = panelTop + 56;
            font.draw(ps, new TextComponent("【养成需求提示】"), leftX + 8, infoY, 0xFF7DD3FC);
            font.draw(ps, new TextComponent("• 升级：消耗 1 个经验瓶 (战斗不涨经验)"), leftX + 8, infoY + 14, 0xFF94A3B8);
            if (active.getAscension() < 6) {
                font.draw(ps, new TextComponent("• 下次突破：" + reqTier + "级人物零件 " + haveCharMat + "/" + reqMatCount),
                        leftX + 8, infoY + 28, haveCharMat >= reqMatCount ? 0xFF4ADE80 : 0xFFF87171);
            } else {
                font.draw(ps, new TextComponent("• 角色已达最高突破阶段"), leftX + 8, infoY + 28, 0xFFFBBF24);
            }
            if (active.getTalentLevel() < 10) {
                font.draw(ps, new TextComponent("• 天赋 Lv." + active.getTalentLevel() + "→" + (active.getTalentLevel() + 1)
                                + "：" + talTier + "级手册 " + haveTalMat + "/" + reqTalCount),
                        leftX + 8, infoY + 42, haveTalMat >= reqTalCount ? 0xFF4ADE80 : 0xFFF87171);
            } else {
                font.draw(ps, new TextComponent("• 技能天赋已满级 (Lv.10)"), leftX + 8, infoY + 42, 0xFFFBBF24);
            }

            // 2. Middle Column: Combat Attributes & Skills
            int midX = leftX + leftW + 8;
            int midW = (this.width / 2 + 32) - midX;
            GenshinHudOverlay.drawVoxelPanel(ps, midX, panelTop, midW, panelH, 0xB80E1622, 0xFF26425C, 0xFF38BDF8);

            font.drawShadow(ps, new TextComponent("【战斗属性面板】"), midX + 8, panelTop + 8, 0xFF38BDF8);
            int sy = panelTop + 23;
            font.draw(ps, new TextComponent("生命上限: " + Math.round(active.getTotalMaxHp())), midX + 8, sy, 0xFF4ADE80);
            font.draw(ps, new TextComponent(String.format("攻击力: %d (白字 %d + 绿字 %d)",
                    Math.round(active.getTotalAtk()), Math.round(active.getBaseAtk()), Math.round(active.getExtraAtk()))),
                    midX + 8, sy + 13, 0xFFFB923C);
            font.draw(ps, new TextComponent("防御力: " + Math.round(active.getTotalDef()) + "   速度: " + active.getTotalSpeed()),
                    midX + 8, sy + 26, 0xFF93C5FD);
            font.draw(ps, new TextComponent(String.format("暴击率: %.1f%%   暴击伤害: %.1f%%", active.getCritRate(), active.getCritDmg())),
                    midX + 8, sy + 39, 0xFFFBBF24);
            font.draw(ps, new TextComponent(String.format("属性增伤: +%.1f%%", active.getElemDmgBonus())),
                    midX + 8, sy + 52, 0xFFC084FC);

            GuiComponent.fill(ps, midX + 6, sy + 65, midX + midW - 6, sy + 66, 0xFF26425C);
            font.drawShadow(ps, new TextComponent("【技能效果预览 (天赋 Lv." + active.getTalentLevel() + ")】"), midX + 8, sy + 70, 0xFF7DD3FC);

            int textY = sy + 84;
            for (String desc : new String[]{active.getBasicDesc(), active.getSkillDesc(), active.getUltDesc()}) {
                for (FormattedCharSequence line : font.split(new TextComponent(desc), midW - 16)) {
                    if (textY + 10 < panelTop + panelH - 4) {
                        font.draw(ps, line, midX + 8, textY, 0xFFCBD5E1);
                        textY += 10;
                    }
                }
                textY += 3;
            }

            // 3. Right Column: Weapon + 5 Equipment Chip Cards
            int rightX = this.width / 2 + 40;
            int rightW = this.width - rightX - 24;
            GenshinHudOverlay.drawVoxelPanel(ps, rightX - 4, panelTop, rightW + 8, panelH, 0xB80E1622, 0xFF26425C, 0xFFFBBF24);
            font.drawShadow(ps, new TextComponent("【武器与 2~5 级芯片装备 (点击选中)】"), rightX + 2, panelTop + 6, 0xFFFBBF24);

            String[] slotNames = {"武器", "生命", "攻击", "电池", "增幅", "核心"};
            for (int row = 0; row < 6; row++) {
                int slotIdx = row - 1;
                ItemStack stack = (slotIdx == -1) ? active.getEquippedWeapon() : active.getEquippedChip(slotIdx);
                boolean isSel = (slotIdx == selectedEquipSlot);
                int slotY = 86 + row * 23;

                int rarity = stack.isEmpty() ? 2 : EquipmentStatsHelper.getRarity(stack);
                int borderCol = isSel ? 0xFFFFFFFF : (stack.isEmpty() ? 0xFF283A4E : EquipmentStatsHelper.getRarityColor(rarity));
                GenshinHudOverlay.drawVoxelPanel(ps, rightX, slotY, rightW, 21,
                        isSel ? 0xD8203B56 : 0x90121E2C, borderCol, borderCol);

                font.draw(ps, new TextComponent("[" + slotNames[row] + "]"), rightX + 4, slotY + 7, 0xFF94A3B8);

                if (stack.isEmpty()) {
                    font.draw(ps, new TextComponent("未装配 (去背包装配)"), rightX + 42, slotY + 7, 0xFF64748B);
                } else {
                    this.itemRenderer.renderGuiItem(stack, rightX + 36, slotY + 2);
                    int lv = EquipmentStatsHelper.getEnhanceLevel(stack);
                    int maxLv = EquipmentStatsHelper.getMaxEnhanceLevel(stack);
                    String label = stack.getHoverName().getString();
                    if (label.length() > 11) label = label.substring(0, 11) + "..";
                    font.draw(ps, new TextComponent(label + " +" + lv + "/" + maxLv), rightX + 56, slotY + 7, EquipmentStatsHelper.getRarityColor(rarity));
                }
            }

            // Selected Equipment Inspector Card
            ItemStack selStack = (selectedEquipSlot == -1) ? active.getEquippedWeapon() : active.getEquippedChip(selectedEquipSlot);
            int detY = 86 + 6 * 23 + 6;
            if (!selStack.isEmpty()) {
                int r = EquipmentStatsHelper.getRarity(selStack);
                int lv = EquipmentStatsHelper.getEnhanceLevel(selStack);
                int maxLv = EquipmentStatsHelper.getMaxEnhanceLevel(selStack);
                font.drawShadow(ps, new TextComponent(EquipmentStatsHelper.getStarString(r) + "  " + EquipmentStatsHelper.formatMainStatLabel(selStack)
                        + "  (+" + lv + "/" + maxLv + ")"), rightX + 2, detY, EquipmentStatsHelper.getRarityColor(r));

                List<String> subs = EquipmentStatsHelper.getSubStatLines(selStack);
                int subY = detY + 13;
                for (int i = 0; i < subs.size(); i += 2) {
                    String pair = subs.get(i) + (i + 1 < subs.size() ? "   |   " + subs.get(i + 1) : "");
                    font.draw(ps, new TextComponent(pair), rightX + 2, subY, 0xFFE2E8F0);
                    subY += 11;
                }
                if (EquipmentStatsHelper.isChip(selStack)) {
                    int cost = EquipmentStatsHelper.getEnhanceDustCost(selStack);
                    font.draw(ps, new TextComponent("每 +3 级解锁/强化副词条 | 强化需粉尘: " + cost),
                            rightX + 2, panelTop + panelH - 12, 0xFF7DD3FC);
                } else {
                    int wAsc = EquipmentStatsHelper.getAscension(selStack);
                    int wTier = Math.min(6, wAsc + 1);
                    int haveWMat = countItem(player, ModItems.getWeaponMat(wTier));
                    font.draw(ps, new TextComponent(wAsc < 6
                            ? ("武器突破需 " + wTier + "级武器构件: " + haveWMat + "/" + (2 + wAsc))
                            : "武器已达最高突破上限"), rightX + 2, panelTop + panelH - 12, 0xFF7DD3FC);
                }
            } else {
                font.draw(ps, new TextComponent("当前槽位为空，点击下方【背包替换/溶解】选取装备。"), rightX + 4, detY + 8, 0xFF94A3B8);
            }

            if (enhanceSlotBtn != null) {
                enhanceSlotBtn.active = !selStack.isEmpty() && EquipmentStatsHelper.getEnhanceLevel(selStack) < EquipmentStatsHelper.getMaxEnhanceLevel(selStack);
                enhanceSlotBtn.setMessage(new TextComponent(selectedEquipSlot == -1 ? "强化武器+1" : "粉尘强化+1"));
            }
            if (secondarySlotBtn != null) {
                secondarySlotBtn.active = !selStack.isEmpty();
                secondarySlotBtn.setMessage(new TextComponent(selectedEquipSlot == -1 ? "武器突破" : "卸下装备"));
            }
        });

        super.render(ps, mouseX, mouseY, partialTicks);
    }
}
