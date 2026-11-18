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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class CharacterProfileScreen extends Screen {
    private int selectedEquipSlot = -1; // -1 = weapon, 0..4 = chips
    private final List<SciFiButton> slotButtons = new ArrayList<>();
    private SciFiButton enhanceSlotBtn;
    private SciFiButton secondarySlotBtn;
    private SciFiButton worldBreakBtn;

    public CharacterProfileScreen() {
        super(new TextComponent("角色与装备养成 (C)"));
    }

    private int innerX() { return 16; }
    private int innerW() { return this.width - 32; }
    private int leftW() { return Math.max(106, (int) (innerW() * 0.28f)); }
    private int midW() { return Math.max(115, (int) (innerW() * 0.34f)); }
    private int rightX() { return innerX() + leftW() + 6 + midW() + 6; }
    private int rightW() { return innerX() + innerW() - rightX(); }
    private int panelTop() { return 58; }
    private int panelH() { return Math.max(120, this.height - 92); }
    private int rowH() { return Math.max(13, Math.min(18, (panelH() - 58) / 6)); }
    private int listTop() { return panelTop() + 16; }

    @Override
    protected void init() {
        super.init();
        slotButtons.clear();
        Player player = Minecraft.getInstance().player;

        int slotAvail = this.width - 104;
        int btnW = Math.min(68, (slotAvail - 12) / 4);
        for (int i = 0; i < 4; i++) {
            int slot = i;
            SciFiButton sb = addRenderableWidget(new SciFiButton(16 + i * (btnW + 4), 38, btnW, 16,
                    new TextComponent((slot + 1) + ".角色"), btn -> {
                if (player == null) return;
                player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                    CharacterData c = team.getCharacter(slot);
                    if (!c.isUnlocked()) {
                        RadioSubtitleManager.postRadioMessage("编队系统", "该位置队友尚未解锁（推进剧情或抽卡可招募）。", 70);
                    } else {
                        team.switchSlot(player, slot);
                        PacketHandler.INSTANCE.sendToServer(new PacketSwitchCharacter(slot));
                    }
                });
            }));
            slotButtons.add(sb);
        }

        addRenderableWidget(new SciFiButton(16 + 4 * (btnW + 4), 38, 64, 16, new TextComponent("轮换角色"), btn -> {
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
        }, 0xFFFBBF24));

        this.worldBreakBtn = addRenderableWidget(new SciFiButton(this.width - 148, 12, 88, 16,
                new TextComponent("模拟副本(E)"), btn -> Minecraft.getInstance().setScreen(new SimulationDomainScreen()), 0xFFFBBF24));

        addRenderableWidget(new SciFiButton(this.width - 56, 12, 40, 16, new TextComponent("关闭"), btn -> onClose(), 0xFFEF4444));

        // Left & Mid Footer Buttons
        int progY = this.height - 28;
        int leftSpan = leftW() + 6 + midW();
        int lBtnW = (leftSpan - 8) / 3;
        addRenderableWidget(new SciFiButton(innerX(), progY, lBtnW, 18, new TextComponent("经验瓶+1"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_USE_EXP_BOTTLE, 0, 0, "")), 0xFF4ADE80));

        addRenderableWidget(new SciFiButton(innerX() + lBtnW + 4, progY, lBtnW, 18, new TextComponent("角色突破"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_ASCEND_CHAR, 0, 0, "")), 0xFFFBBF24));

        addRenderableWidget(new SciFiButton(innerX() + (lBtnW + 4) * 2, progY, lBtnW, 18, new TextComponent("天赋+1"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_UPGRADE_TALENT, 0, 0, "")), 0xFF38BDF8));

        // Right Footer Buttons
        int rx = rightX();
        int rw = rightW();
        int rBtnW = (rw - 8) / 3;
        this.enhanceSlotBtn = addRenderableWidget(new SciFiButton(rx, progY, rBtnW, 18, new TextComponent("强化+1"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_EQUIPPED, selectedEquipSlot, ItemStack.EMPTY)), 0xFFC084FC));

        this.secondarySlotBtn = addRenderableWidget(new SciFiButton(rx + rBtnW + 4, progY, rBtnW, 18, new TextComponent("卸下"), btn -> {
            if (selectedEquipSlot == -1) {
                PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_ASCEND_WEAPON, 0, 0, ""));
            } else {
                PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_UNEQUIP_CHIP, selectedEquipSlot, ItemStack.EMPTY));
            }
        }, 0xFFFBBF24));

        addRenderableWidget(new SciFiButton(rx + (rBtnW + 4) * 2, progY, rBtnW, 18, new TextComponent("替换/溶解"), btn -> {
            InfiniteInventoryScreen.Category cat = (selectedEquipSlot == -1)
                    ? InfiniteInventoryScreen.Category.WEAPONS
                    : InfiniteInventoryScreen.Category.CHIPS;
            Minecraft.getInstance().setScreen(new InfiniteInventoryScreen(cat));
        }, 0xFF38BDF8));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rx = rightX();
        int rw = rightW();
        int top = listTop();
        int rh = rowH();
        if (mouseX >= rx && mouseX <= rx + rw) {
            for (int row = 0; row < 6; row++) {
                int slotY = top + row * rh;
                if (mouseY >= slotY && mouseY < slotY + rh) {
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
        GenshinHudOverlay.drawVoxelPanel(ps, 10, 8, this.width - 20, this.height - 16,
                0xF209101A, 0xFF284868, 0xFF38BDF8);

        font.drawShadow(ps, getTitle(), 16, 13, 0xFF38BDF8);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int expBottles = countItem(player, ModItems.EXP_BOTTLE.get()) + countItem(player, Items.EXPERIENCE_BOTTLE);
        int dust = countItem(player, ModItems.EQUIP_ESSENCE.get());
        int credits = countItem(player, ModItems.CREDIT_COIN.get());

        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            String topStrip = String.format("主Lv.%d/%d(%d/%d) | 世界Lv.%d(上限Lv.%d) | 体力%d | 币%d | 瓶%d | 粉%d",
                    team.getPlayerLevel(), team.getPlayerLevelCapForWorldLevel(),
                    team.getPlayerExp(), team.getPlayerExpNeeded(),
                    team.getWorldLevel(), Math.min(80, 20 + team.getWorldLevel() * 10),
                    team.getStamina(), credits, expBottles, dust);
            font.draw(ps, new TextComponent(topStrip), 16, 28, 0xFFE2E8F0);

            if (worldBreakBtn != null) {
                worldBreakBtn.setMessage(new TextComponent(team.canBreakthroughWorldLevel() ? "★世界突破试炼" : "模拟副本(E)"));
            }

            for (int i = 0; i < 4 && i < slotButtons.size(); i++) {
                CharacterData cd = team.getCharacter(i);
                String sn = cd.getName().contains("·") ? cd.getName().substring(cd.getName().indexOf('·') + 1) : cd.getName();
                SciFiButton b = slotButtons.get(i);
                b.setMessage(new TextComponent((i + 1) + "." + (cd.isUnlocked() ? sn : "空位")));
                b.setAccentColor(i == team.getActiveSlotIndex() ? 0xFFFBBF24 : 0xFF38BDF8);
            }

            CharacterData active = team.getCharacter(team.getActiveSlotIndex());
            int pTop = panelTop();
            int pH = panelH();

            // 1. Left Column
            int lx = innerX();
            int lw = leftW();
            int elemCol = CharacterData.getElementColor(active.getElement());
            GenshinHudOverlay.drawVoxelPanel(ps, lx, pTop, lw, pH, 0xB80E1622, elemCol, elemCol);
            GenshinHudOverlay.drawPortrait(ps, TurnBasedBattleScreen.getPortraitForChar(active.getCharId()), lx + 5, pTop + 5, 28, 28);

            font.drawShadow(ps, new TextComponent(active.getName()), lx + 37, pTop + 6, 0xFFF8FAFC);
            font.draw(ps, new TextComponent(active.getElementLabel() + " · " + active.getRoleLabel()), lx + 37, pTop + 17, elemCol);
            font.draw(ps, new TextComponent("Lv." + active.getLevel() + "/" + active.getMaxLevelForAscension() + " 潜" + active.getEidolon()),
                    lx + 37, pTop + 28, 0xFFFBBF24);

            int reqTier = Math.min(6, active.getAscension() + 1);
            int reqMatCount = 2 + active.getAscension();
            int haveCharMat = countItem(player, ModItems.getCharMat(reqTier));
            int talTier = Math.min(6, 1 + (active.getTalentLevel() / 2));
            int reqTalCount = 1 + (active.getTalentLevel() % 2);
            int haveTalMat = countItem(player, ModItems.getTalentMat(talTier));

            int infoY = pTop + 42;
            font.draw(ps, new TextComponent("【养成说明】"), lx + 5, infoY, 0xFF7DD3FC);
            infoY += 12;
            font.draw(ps, new TextComponent("• 战斗不涨经验，用瓶升级"), lx + 5, infoY, 0xFF94A3B8);
            infoY += 12;
            String ascText = active.getAscension() < 6
                    ? ("• 突破:" + reqTier + "级零件 " + haveCharMat + "/" + reqMatCount)
                    : "• 角色已满突破";
            font.draw(ps, new TextComponent(ascText), lx + 5, infoY,
                    (active.getAscension() >= 6 || haveCharMat >= reqMatCount) ? 0xFF4ADE80 : 0xFFF87171);
            infoY += 12;
            String talText = active.getTalentLevel() < 10
                    ? ("• 天赋" + active.getTalentLevel() + "→" + (active.getTalentLevel() + 1) + ":" + talTier + "级手册 " + haveTalMat + "/" + reqTalCount)
                    : "• 天赋已满 Lv.10";
            font.draw(ps, new TextComponent(talText), lx + 5, infoY,
                    (active.getTalentLevel() >= 10 || haveTalMat >= reqTalCount) ? 0xFF4ADE80 : 0xFFF87171);

            // 2. Middle Column
            int mx = lx + lw + 6;
            int mw = midW();
            GenshinHudOverlay.drawVoxelPanel(ps, mx, pTop, mw, pH, 0xB80E1622, 0xFF26425C, 0xFF38BDF8);
            font.drawShadow(ps, new TextComponent("【战斗属性】"), mx + 5, pTop + 5, 0xFF38BDF8);
            int sy = pTop + 17;
            font.draw(ps, new TextComponent("生命: " + Math.round(active.getTotalMaxHp()) + "  防: " + Math.round(active.getTotalDef())), mx + 5, sy, 0xFF4ADE80);
            font.draw(ps, new TextComponent(String.format("攻击: %d (%d+%d)", Math.round(active.getTotalAtk()), Math.round(active.getBaseAtk()), Math.round(active.getExtraAtk()))),
                    mx + 5, sy + 11, 0xFFFB923C);
            font.draw(ps, new TextComponent(String.format("速度: %d  暴率: %.0f%%", active.getTotalSpeed(), active.getCritRate())),
                    mx + 5, sy + 22, 0xFF93C5FD);
            font.draw(ps, new TextComponent(String.format("暴伤: %.0f%%  增伤: +%.0f%%", active.getCritDmg(), active.getElemDmgBonus())),
                    mx + 5, sy + 33, 0xFFFBBF24);

            GuiComponent.fill(ps, mx + 4, sy + 44, mx + mw - 4, sy + 45, 0xFF26425C);
            font.drawShadow(ps, new TextComponent("【技能效果 (天赋Lv." + active.getTalentLevel() + ")】"), mx + 5, sy + 48, 0xFF7DD3FC);

            int textY = sy + 60;
            for (String desc : new String[]{active.getBasicDesc(), active.getSkillDesc(), active.getUltDesc()}) {
                for (FormattedCharSequence line : font.split(new TextComponent(desc), mw - 10)) {
                    if (textY + 9 <= pTop + pH - 3) {
                        font.draw(ps, line, mx + 5, textY, 0xFFCBD5E1);
                        textY += 10;
                    }
                }
                textY += 2;
            }

            // 3. Right Column
            int rx = rightX();
            int rw = rightW();
            GenshinHudOverlay.drawVoxelPanel(ps, rx, pTop, rw, pH, 0xB80E1622, 0xFF26425C, 0xFFFBBF24);
            font.drawShadow(ps, new TextComponent("【武器与芯片 (点击选中)】"), rx + 5, pTop + 5, 0xFFFBBF24);

            String[] slotNames = {"武", "1生", "2攻", "3能", "4增", "5核"};
            int top = listTop();
            int rh = rowH();
            for (int row = 0; row < 6; row++) {
                int slotIdx = row - 1;
                ItemStack stack = (slotIdx == -1) ? active.getEquippedWeapon() : active.getEquippedChip(slotIdx);
                boolean isSel = (slotIdx == selectedEquipSlot);
                int slotY = top + row * rh;

                int rarity = stack.isEmpty() ? 2 : EquipmentStatsHelper.getRarity(stack);
                int borderCol = isSel ? 0xFFFFFFFF : (stack.isEmpty() ? 0xFF283A4E : EquipmentStatsHelper.getRarityColor(rarity));
                GenshinHudOverlay.drawVoxelPanel(ps, rx + 4, slotY, rw - 8, rh - 1,
                        isSel ? 0xD8203B56 : 0x90121E2C, borderCol, borderCol);

                font.draw(ps, new TextComponent("[" + slotNames[row] + "]"), rx + 6, slotY + (rh - 9) / 2, 0xFF94A3B8);

                if (stack.isEmpty()) {
                    font.draw(ps, new TextComponent("未装配"), rx + 30, slotY + (rh - 9) / 2, 0xFF64748B);
                } else {
                    int lv = EquipmentStatsHelper.getEnhanceLevel(stack);
                    int maxLv = EquipmentStatsHelper.getMaxEnhanceLevel(stack);
                    String lvTag = "+" + lv + "/" + maxLv;
                    String label = stack.getHoverName().getString().replaceAll("【.*?】", "");
                    int availNameW = rw - 38 - font.width(lvTag);
                    while (font.width(label) > availNameW && label.length() > 2) {
                        label = label.substring(0, label.length() - 1);
                    }
                    font.draw(ps, new TextComponent(label), rx + 28, slotY + (rh - 9) / 2, EquipmentStatsHelper.getRarityColor(rarity));
                    font.draw(ps, new TextComponent(lvTag), rx + rw - 6 - font.width(lvTag), slotY + (rh - 9) / 2, 0xFFE2E8F0);
                }
            }

            // Selected Item Inspector
            ItemStack selStack = (selectedEquipSlot == -1) ? active.getEquippedWeapon() : active.getEquippedChip(selectedEquipSlot);
            int detY = top + 6 * rh + 3;
            if (!selStack.isEmpty()) {
                int r = EquipmentStatsHelper.getRarity(selStack);
                String mainStr = EquipmentStatsHelper.getStarString(r) + " " + EquipmentStatsHelper.formatMainStatLabel(selStack);
                for (FormattedCharSequence line : font.split(new TextComponent(mainStr), rw - 10)) {
                    if (detY + 9 <= pTop + pH - 2) {
                        font.drawShadow(ps, line, rx + 5, detY, EquipmentStatsHelper.getRarityColor(r));
                        detY += 10;
                    }
                }
                List<String> subs = EquipmentStatsHelper.getSubStatLines(selStack);
                if (!subs.isEmpty()) {
                    String subJoin = String.join(" / ", subs);
                    for (FormattedCharSequence line : font.split(new TextComponent(subJoin), rw - 10)) {
                        if (detY + 9 <= pTop + pH - 10) {
                            font.draw(ps, line, rx + 5, detY, 0xFFE2E8F0);
                            detY += 10;
                        }
                    }
                }
                String tip;
                if (EquipmentStatsHelper.isChip(selStack)) {
                    int cost = EquipmentStatsHelper.getEnhanceDustCost(selStack);
                    tip = "需粉尘:" + cost + " (+3/6/9..解锁副词条)";
                } else {
                    int wAsc = EquipmentStatsHelper.getAscension(selStack);
                    int wTier = Math.min(6, wAsc + 1);
                    int haveWMat = countItem(player, ModItems.getWeaponMat(wTier));
                    tip = wAsc < 6 ? ("突破需" + wTier + "级构件:" + haveWMat + "/" + (2 + wAsc)) : "武器已满突破";
                }
                font.draw(ps, new TextComponent(tip), rx + 5, pTop + pH - 11, 0xFF7DD3FC);
            } else {
                font.draw(ps, new TextComponent("空槽位，点击【替换/溶解】装配。"), rx + 5, detY + 4, 0xFF94A3B8);
            }

            if (enhanceSlotBtn != null) {
                enhanceSlotBtn.active = !selStack.isEmpty() && EquipmentStatsHelper.getEnhanceLevel(selStack) < EquipmentStatsHelper.getMaxEnhanceLevel(selStack);
                enhanceSlotBtn.setMessage(new TextComponent(selectedEquipSlot == -1 ? "武器强化" : "粉尘+1级"));
            }
            if (secondarySlotBtn != null) {
                secondarySlotBtn.active = !selStack.isEmpty();
                secondarySlotBtn.setMessage(new TextComponent(selectedEquipSlot == -1 ? "武器突破" : "卸下芯片"));
            }
        });

        super.render(ps, mouseX, mouseY, partialTicks);
    }
}
