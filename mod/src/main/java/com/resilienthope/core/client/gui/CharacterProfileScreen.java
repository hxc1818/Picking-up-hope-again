package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketEquipItem;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketSwitchCharacter;
import com.resilienthope.core.subtitle.RadioSubtitleManager;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CharacterProfileScreen extends Screen {
    private static final ResourceLocation TEX_PORTRAIT_DAWN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/gui/portrait_dawn.png");

    // -1 = Weapon slot, 0..4 = Tech Chip slots 1..5
    private int selectedEquipSlot = -1;
    private Button enhanceSlotBtn;
    private Button unequipSlotBtn;

    public CharacterProfileScreen() {
        super(new TextComponent("星舰战术终端 · 主角养成面板 (C)"));
    }

    @Override
    protected void init() {
        super.init();

        Player player = Minecraft.getInstance().player;

        // 4 Party Slot tabs (Protagonist + 3 vertical expansion slots)
        String[] charNames = {"1. 破晓", "2. 铁卫[锁]", "3. 矩阵[锁]", "4. 星枢[锁]"};
        int btnW = 72;
        for (int i = 0; i < 4; i++) {
            int slot = i;
            addRenderableWidget(new Button(28 + i * (btnW + 4), 30, btnW, 18, new TextComponent(charNames[i]), btn -> {
                if (player != null) {
                    player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                        if (!team.getCharacter(slot).isUnlocked()) {
                            RadioSubtitleManager.postRadioMessage("战术终端", "该协同席位尚未开放，当前专注养成【零号特工·破晓】。", 60);
                        } else {
                            team.switchSlot(player, slot);
                            PacketHandler.INSTANCE.sendToServer(new PacketSwitchCharacter(slot));
                        }
                    });
                }
            }));
        }

        int rightX = this.width / 2 + 45;
        int bottomY = this.height - 40;

        this.enhanceSlotBtn = addRenderableWidget(new Button(rightX, bottomY, 82, 20, new TextComponent("强化 (+1)"), btn -> {
            PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_EQUIPPED, selectedEquipSlot, ItemStack.EMPTY));
        }));

        this.unequipSlotBtn = addRenderableWidget(new Button(rightX + 86, bottomY, 64, 20, new TextComponent("卸下"), btn -> {
            if (selectedEquipSlot >= 0) {
                PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_UNEQUIP_CHIP, selectedEquipSlot, ItemStack.EMPTY));
            }
        }));

        addRenderableWidget(new Button(rightX + 154, bottomY, 72, 20, new TextComponent("背包替换"), btn -> {
            InfiniteInventoryScreen.Category cat = (selectedEquipSlot == -1)
                    ? InfiniteInventoryScreen.Category.WEAPONS
                    : InfiniteInventoryScreen.Category.CHIPS;
            Minecraft.getInstance().setScreen(new InfiniteInventoryScreen(cat));
        }));

        addRenderableWidget(new Button(this.width - 68, 12, 46, 18, new TextComponent("关闭"), btn -> onClose()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rightX = this.width / 2 + 45;
        int listTop = 62;
        int rightEnd = this.width - 26;
        if (mouseX >= rightX && mouseX <= rightEnd) {
            for (int row = 0; row < 6; row++) {
                int slotY = listTop + row * 24;
                if (mouseY >= slotY && mouseY <= slotY + 22) {
                    this.selectedEquipSlot = row - 1; // -1 for weapon, 0..4 for chips
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);

        GenshinHudOverlay.drawVoxelPanel(poseStack, 16, 10, this.width - 32, this.height - 20,
                0xEE0B131E, 0xFF284868, 0xFF4FC3F7);
        font.drawShadow(poseStack, getTitle(), 28, 15, 0xFF66FFFF);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            CharacterData active = team.getCharacter(team.getActiveSlotIndex());

            // 1. Left Column: Live 3D Protagonist Model
            int modelBoxX = 28;
            int modelBoxY = 54;
            int modelBoxW = 108;
            int modelBoxH = this.height - 74;
            GenshinHudOverlay.drawVoxelPanel(poseStack, modelBoxX, modelBoxY, modelBoxW, modelBoxH,
                    0xA00D1724, 0xFF284664, 0xFFFFCC00);

            GenshinHudOverlay.drawTexture(poseStack, TEX_PORTRAIT_DAWN, modelBoxX + 6, modelBoxY + 6, 22, 22, 32, 32);
            font.draw(poseStack, new TextComponent("Lv." + active.getLevel() + " 破晓"), modelBoxX + 32, modelBoxY + 12, 0xFFFFE082);

            int entityCenterX = modelBoxX + modelBoxW / 2;
            int entityBaseY = modelBoxY + modelBoxH - 22;
            int scale = Math.max(30, Math.min(48, modelBoxH / 4));
            InventoryScreen.renderEntityInInventory(entityCenterX, entityBaseY, scale,
                    (float) (entityCenterX - mouseX), (float) (entityBaseY - 60 - mouseY), player);

            // 2. Middle Column: Live Computed Combat Stats
            int statX = modelBoxX + modelBoxW + 8;
            int statW = (this.width / 2 + 38) - statX;
            GenshinHudOverlay.drawVoxelPanel(poseStack, statX, modelBoxY, statW, modelBoxH,
                    0xA00E1826, 0xFF26425C, 0xFF4FC3F7);

            font.drawShadow(poseStack, new TextComponent("【实时战斗属性】"), statX + 8, modelBoxY + 8, 0xFF55FFFF);
            int sy = modelBoxY + 26;
            int step = 16;

            font.draw(poseStack, new TextComponent("生命上限: " + Math.round(active.getTotalMaxHp())), statX + 10, sy, 0xFF4ADE80);
            font.draw(poseStack, new TextComponent(String.format("攻击力: %d", Math.round(active.getTotalAtk()))), statX + 10, sy + step, 0xFFFF8A65);
            font.draw(poseStack, new TextComponent(String.format("  (白字 %d + 绿字 %d)", Math.round(active.getBaseAtk()), Math.round(active.getExtraAtk()))),
                    statX + 10, sy + step * 2 - 3, 0xFF99AABB);
            font.draw(poseStack, new TextComponent("防御力: " + Math.round(active.getTotalDef())), statX + 10, sy + step * 3, 0xFF90CAF9);
            font.draw(poseStack, new TextComponent(String.format("暴击率: %.1f%%", active.getCritRate())), statX + 10, sy + step * 4, 0xFFFFD54F);
            font.draw(poseStack, new TextComponent(String.format("暴击伤害: %.1f%%", active.getCritDmg())), statX + 10, sy + step * 5, 0xFFFFB74D);
            font.draw(poseStack, new TextComponent(String.format("充能效率: %.1f%%", active.getEnergyRecharge())), statX + 10, sy + step * 6, 0xFF4FC3F7);
            font.draw(poseStack, new TextComponent(String.format("电浆增伤: +%.1f%%", active.getPlasmaDmgBonus())), statX + 10, sy + step * 7, 0xFFCE93D8);
            font.draw(poseStack, new TextComponent("爆发能量: " + (int) active.getCurrentEnergy() + " / " + (int) active.getMaxEnergy()),
                    statX + 10, sy + step * 8, 0xFF80DEEA);

            int chipCount = active.countEquippedChips();
            String resonance = chipCount >= 4 ? "【4芯片共鸣】战技爆伤 +20%" : (chipCount >= 2 ? "【2芯片共鸣】全攻 +12%" : "芯片共鸣：" + chipCount + "/2");
            font.draw(poseStack, new TextComponent(resonance), statX + 10, modelBoxY + modelBoxH - 18, chipCount >= 2 ? 0xFFFFD54F : 0xFF778899);

            // 3. Right Column: Weapon + 5 Interactive Tech Chip Slots
            int rightX = this.width / 2 + 45;
            int rightW = this.width - rightX - 24;
            GenshinHudOverlay.drawVoxelPanel(poseStack, rightX - 4, modelBoxY, rightW + 8, modelBoxH,
                    0xA00E1826, 0xFF26425C, 0xFFFFCC00);
            font.drawShadow(poseStack, new TextComponent("【专武与 5 部位科技芯片 (点击选中)】"), rightX + 2, modelBoxY + 6, 0xFFFFD54F);

            String[] slotNames = {"主武", "核心", "动力", "电池", "超频", "感知"};
            for (int row = 0; row < 6; row++) {
                int slotIdx = row - 1;
                ItemStack stack = (slotIdx == -1) ? active.getEquippedWeapon() : active.getEquippedChip(slotIdx);
                boolean isSel = (slotIdx == selectedEquipSlot);
                int slotY = 62 + row * 24;

                int borderCol = isSel ? 0xFFFFFFFF : (stack.isEmpty() ? 0xFF283A4E : EquipmentStatsHelper.getRarityColor(EquipmentStatsHelper.getRarity(stack)));
                GenshinHudOverlay.drawVoxelPanel(poseStack, rightX, slotY, rightW, 21,
                        isSel ? 0xC8203B56 : 0x90121E2C, borderCol, borderCol);

                font.draw(poseStack, new TextComponent("[" + slotNames[row] + "]"), rightX + 4, slotY + 7, 0xFF88AABB);

                if (stack.isEmpty()) {
                    font.draw(poseStack, new TextComponent("未装配 (点击下方替换)"), rightX + 44, slotY + 7, 0xFF556677);
                } else {
                    this.itemRenderer.renderGuiItem(stack, rightX + 38, slotY + 2);
                    int lv = EquipmentStatsHelper.getEnhanceLevel(stack);
                    String label = stack.getHoverName().getString();
                    if (label.length() > 14) label = label.substring(0, 14) + "..";
                    font.draw(poseStack, new TextComponent(label + " +" + lv), rightX + 58, slotY + 7, 0xFFEEEEEE);
                }
            }

            // Selected Slot Detail Box
            ItemStack selStack = (selectedEquipSlot == -1) ? active.getEquippedWeapon() : active.getEquippedChip(selectedEquipSlot);
            int detY = 62 + 6 * 24 + 4;
            if (!selStack.isEmpty()) {
                font.draw(poseStack, new TextComponent(EquipmentStatsHelper.formatMainStatLabel(selStack)), rightX + 4, detY, 0xFF4ADE80);
                var subs = EquipmentStatsHelper.getSubStatLines(selStack);
                if (!subs.isEmpty()) {
                    font.draw(poseStack, new TextComponent(String.join("  |  ", subs.subList(0, Math.min(2, subs.size())))),
                            rightX + 4, detY + 12, 0xFFFFE082);
                }
            } else {
                font.draw(poseStack, new TextComponent("当前槽位为空，请点击【背包替换】装配对应芯片。"), rightX + 4, detY + 4, 0xFF99AABB);
            }

            if (enhanceSlotBtn != null) enhanceSlotBtn.active = !selStack.isEmpty();
            if (unequipSlotBtn != null) unequipSlotBtn.active = (selectedEquipSlot >= 0 && !selStack.isEmpty());
        });

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
