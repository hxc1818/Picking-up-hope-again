package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketEquipItem;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketProgressionAction;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class InfiniteInventoryScreen extends Screen {

    public enum Category {
        WEAPONS("武器库"),
        CHIPS("芯片装备"),
        MATERIALS("1-6级材料"),
        VALUABLES("货币与消耗品");

        private final String displayName;
        Category(String name) { this.displayName = name; }
        public String getDisplayName() { return displayName; }
    }

    private static class InvEntry {
        final int invIndex;
        final int equippedChipSlot;
        final ItemStack stack;
        final boolean isEquipped;

        InvEntry(int invIndex, int equippedChipSlot, ItemStack stack, boolean isEquipped) {
            this.invIndex = invIndex;
            this.equippedChipSlot = equippedChipSlot;
            this.stack = stack;
            this.isEquipped = isEquipped;
        }
    }

    private Category currentCategory;
    private final List<InvEntry> entries = new ArrayList<>();
    private int selectedIdx = 0;
    private int scrollRow = 0;
    private int autoRefreshTicks = 0;

    private Button primaryActionBtn;
    private Button secondaryActionBtn;

    public InfiniteInventoryScreen() {
        this(Category.CHIPS);
    }

    public InfiniteInventoryScreen(Category initialCategory) {
        super(new TextComponent("背包 · 装备溶解与材料合成 (B)"));
        this.currentCategory = initialCategory;
    }

    @Override
    protected void init() {
        super.init();
        refreshItems();

        int tabStartX = 24;
        int tabY = 28;
        int tabWidth = 82;

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            addRenderableWidget(new Button(tabStartX + i * (tabWidth + 5), tabY, tabWidth, 20,
                    new TextComponent(cat.getDisplayName()),
                    btn -> {
                        this.currentCategory = cat;
                        this.selectedIdx = 0;
                        this.scrollRow = 0;
                        refreshItems();
                        updateActionButtons();
                    }));
        }

        int rightX = this.width / 2 + 24;
        int bottomY = this.height - 46;

        this.primaryActionBtn = addRenderableWidget(new Button(rightX, bottomY - 24, 100, 20, new TextComponent("装备"), btn -> {
            if (selectedIdx >= 0 && selectedIdx < entries.size()) {
                InvEntry e = entries.get(selectedIdx);
                if (EquipmentStatsHelper.isWeapon(e.stack) || EquipmentStatsHelper.isChip(e.stack)) {
                    if (!e.isEquipped && e.invIndex >= 0) {
                        PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_EQUIP_FROM_INV, e.invIndex, e.stack));
                        autoRefreshTicks = 4;
                    }
                } else if (ModItems.getNextTierMaterial(e.stack.getItem()) != null && e.invIndex >= 0) {
                    PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_CRAFT_MAT_UP, e.invIndex, 0, ""));
                    autoRefreshTicks = 4;
                }
            }
        }));

        this.secondaryActionBtn = addRenderableWidget(new Button(rightX + 104, bottomY - 24, 108, 20, new TextComponent("溶解 / 强化"), btn -> {
            if (selectedIdx >= 0 && selectedIdx < entries.size()) {
                InvEntry e = entries.get(selectedIdx);
                if (!e.isEquipped && EquipmentStatsHelper.isChip(e.stack) && e.invIndex >= 0) {
                    PacketHandler.INSTANCE.sendToServer(new PacketProgressionAction(PacketProgressionAction.ACT_DISSOLVE_CHIP, e.invIndex, 0, ""));
                    autoRefreshTicks = 4;
                } else if (EquipmentStatsHelper.isWeapon(e.stack) || EquipmentStatsHelper.isChip(e.stack)) {
                    if (e.isEquipped && e.equippedChipSlot >= 0) {
                        PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_EQUIPPED, e.equippedChipSlot, ItemStack.EMPTY));
                    } else if (e.isEquipped && EquipmentStatsHelper.isWeapon(e.stack)) {
                        PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_EQUIPPED, -1, ItemStack.EMPTY));
                    } else if (e.invIndex >= 0) {
                        PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_INV, e.invIndex, e.stack));
                    }
                    autoRefreshTicks = 4;
                }
            }
        }));

        addRenderableWidget(new Button(rightX, bottomY, 100, 20, new TextComponent("角色与装备 (C)"), btn ->
                Minecraft.getInstance().setScreen(new CharacterProfileScreen())));

        addRenderableWidget(new Button(rightX + 104, bottomY, 108, 20, new TextComponent("抽卡界面 (G)"), btn ->
                Minecraft.getInstance().setScreen(new TerminalWishScreen())));

        addRenderableWidget(new Button(this.width - 68, 12, 46, 18, new TextComponent("关闭"), btn -> onClose()));

        updateActionButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (autoRefreshTicks > 0) {
            autoRefreshTicks--;
            if (autoRefreshTicks == 0) {
                refreshItems();
                updateActionButtons();
            }
        }
    }

    private int countSameItem(Item item) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return 0;
        int total = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    private void updateActionButtons() {
        boolean hasItem = selectedIdx >= 0 && selectedIdx < entries.size();
        if (!hasItem) {
            if (primaryActionBtn != null) primaryActionBtn.active = false;
            if (secondaryActionBtn != null) secondaryActionBtn.active = false;
            return;
        }
        InvEntry e = entries.get(selectedIdx);
        ItemStack stack = e.stack;

        if (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack)) {
            if (primaryActionBtn != null) {
                primaryActionBtn.active = !e.isEquipped;
                primaryActionBtn.setMessage(new TextComponent(e.isEquipped ? "已穿戴中" : "装备到当前角色"));
            }
            if (secondaryActionBtn != null) {
                if (!e.isEquipped && EquipmentStatsHelper.isChip(stack)) {
                    int yield = EquipmentStatsHelper.getDissolveEssenceYield(stack);
                    secondaryActionBtn.active = true;
                    secondaryActionBtn.setMessage(new TextComponent("溶解换粉尘 (+" + yield + ")"));
                } else {
                    boolean canUp = EquipmentStatsHelper.getEnhanceLevel(stack) < EquipmentStatsHelper.getMaxEnhanceLevel(stack);
                    secondaryActionBtn.active = canUp;
                    secondaryActionBtn.setMessage(new TextComponent(canUp ? "强化升级 (+1)" : "已满级"));
                }
            }
        } else if (ModItems.getNextTierMaterial(stack.getItem()) != null) {
            int count = countSameItem(stack.getItem());
            if (primaryActionBtn != null) {
                primaryActionBtn.active = count >= 3;
                primaryActionBtn.setMessage(new TextComponent("3合1合成高阶"));
            }
            if (secondaryActionBtn != null) {
                secondaryActionBtn.active = false;
                secondaryActionBtn.setMessage(new TextComponent("-"));
            }
        } else {
            if (primaryActionBtn != null) {
                primaryActionBtn.active = false;
                primaryActionBtn.setMessage(new TextComponent("-"));
            }
            if (secondaryActionBtn != null) {
                secondaryActionBtn.active = false;
                secondaryActionBtn.setMessage(new TextComponent("-"));
            }
        }
    }

    private void refreshItems() {
        entries.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            CharacterData active = team.getCharacter(team.getActiveSlotIndex());
            if (currentCategory == Category.WEAPONS && !active.getEquippedWeapon().isEmpty()) {
                entries.add(new InvEntry(0, -1, active.getEquippedWeapon().copy(), true));
            }
            if (currentCategory == Category.CHIPS) {
                for (int s = 0; s < 5; s++) {
                    ItemStack chip = active.getEquippedChip(s);
                    if (!chip.isEmpty()) {
                        entries.add(new InvEntry(-1, s, chip.copy(), true));
                    }
                }
            }
        });

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (currentCategory == Category.WEAPONS) {
                if (i == 0 && !entries.isEmpty() && entries.get(0).isEquipped && stack.getItem() == entries.get(0).stack.getItem()) {
                    continue;
                }
                if (EquipmentStatsHelper.isWeapon(stack)) {
                    entries.add(new InvEntry(i, -1, stack.copy(), false));
                }
            } else if (currentCategory == Category.CHIPS) {
                if (EquipmentStatsHelper.isChip(stack)) {
                    entries.add(new InvEntry(i, -1, stack.copy(), false));
                }
            } else if (currentCategory == Category.MATERIALS) {
                if (ModItems.getMaterialLine(stack.getItem()) != null || stack.is(ModItems.ALLOY_PLATE.get())) {
                    entries.add(new InvEntry(i, -1, stack.copy(), false));
                }
            } else if (currentCategory == Category.VALUABLES) {
                if (stack.is(ModItems.CREDIT_COIN.get()) || stack.is(ModItems.EXP_BOTTLE.get())
                        || stack.is(ModItems.EQUIP_ESSENCE.get()) || stack.is(ModItems.STAR_TICKET.get())
                        || stack.is(ModItems.QUANTUM_CORE.get()) || stack.is(ModItems.PRECURSOR_SPARK.get())
                        || stack.is(Items.EXPERIENCE_BOTTLE)) {
                    entries.add(new InvEntry(i, -1, stack.copy(), false));
                }
            }
        }

        if (selectedIdx >= entries.size()) {
            selectedIdx = Math.max(0, entries.size() - 1);
        }
    }

    private int getGridCols(int gridWidth) {
        return Math.max(3, (gridWidth - 12) / 44);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int gridWidth = this.width / 2 - 20;
        int cols = getGridCols(gridWidth);
        int totalRows = (int) Math.ceil(entries.size() / (double) cols);
        if (delta < 0 && scrollRow + 3 < totalRows) scrollRow++;
        if (delta > 0 && scrollRow > 0) scrollRow--;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridLeft = 24;
        int gridTop = 56;
        int gridRight = this.width / 2 + 12;
        int gridBottom = this.height - 22;
        int cols = getGridCols(gridRight - gridLeft);

        if (mouseX >= gridLeft && mouseX <= gridRight && mouseY >= gridTop && mouseY <= gridBottom) {
            int col = (int) ((mouseX - (gridLeft + 8)) / 44);
            int row = (int) ((mouseY - (gridTop + 8)) / 46) + scrollRow;
            if (col >= 0 && col < cols && row >= 0) {
                int idx = row * cols + col;
                if (idx >= 0 && idx < entries.size()) {
                    this.selectedIdx = idx;
                    updateActionButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ps);

        GenshinHudOverlay.drawVoxelPanel(ps, 14, 10, this.width - 28, this.height - 20,
                0xEE0B131E, 0xFF284868, 0xFF38BDF8);
        font.drawShadow(ps, getTitle(), 24, 15, 0xFF38BDF8);

        int gridLeft = 24;
        int gridTop = 56;
        int gridRight = this.width / 2 + 12;
        int gridBottom = this.height - 22;
        GenshinHudOverlay.drawVoxelPanel(ps, gridLeft, gridTop, gridRight - gridLeft, gridBottom - gridTop,
                0xA00D1724, 0xFF1F3850, 0xFF38BDF8);

        int cols = getGridCols(gridRight - gridLeft);
        if (entries.isEmpty()) {
            font.draw(ps, new TextComponent("当前分类暂无物品（探索箱子或通关模拟副本获取）"), gridLeft + 14, gridTop + 18, 0xFF94A3B8);
        } else {
            int startIdx = scrollRow * cols;
            for (int i = startIdx; i < entries.size(); i++) {
                int rel = i - startIdx;
                int row = rel / cols;
                int col = rel % cols;
                int cx = gridLeft + 8 + col * 44;
                int cy = gridTop + 8 + row * 46;
                if (cy + 42 > gridBottom) break;

                InvEntry entry = entries.get(i);
                ItemStack stack = entry.stack;
                boolean isSel = (i == selectedIdx);
                int rarity = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack))
                        ? EquipmentStatsHelper.getRarity(stack)
                        : Math.max(2, Math.min(5, ModItems.getMaterialTier(stack.getItem())));
                int rarityCol = EquipmentStatsHelper.getRarityColor(rarity);

                GenshinHudOverlay.drawVoxelPanel(ps, cx, cy, 40, 42,
                        isSel ? 0xD8213A54 : 0xB014202E, isSel ? 0xFFFFFFFF : rarityCol, rarityCol);

                this.itemRenderer.renderGuiItem(stack, cx + 12, cy + 7);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, cx + 12, cy + 7);

                String badge = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack))
                        ? ("+" + EquipmentStatsHelper.getEnhanceLevel(stack))
                        : ("x" + stack.getCount());
                int badgeCol = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack)) ? 0xFFFBBF24 : 0xFFE2E8F0;
                font.draw(ps, new TextComponent(badge), cx + 20 - font.width(badge) / 2, cy + 30, badgeCol);

                if (entry.isEquipped) {
                    fill(ps, cx + 2, cy + 2, cx + 12, cy + 11, 0xE016A34A);
                    font.draw(ps, new TextComponent("装"), cx + 3, cy + 3, 0xFFFFFFFF);
                }
            }
        }

        int rightX = this.width / 2 + 24;
        int rightTop = 56;
        int rightBottom = this.height - 76;
        int rightW = this.width - rightX - 24;
        GenshinHudOverlay.drawVoxelPanel(ps, rightX, rightTop, rightW, rightBottom - rightTop,
                0xB0101A26, 0xFF305880, 0xFF38BDF8);

        int dustCount = countSameItem(ModItems.EQUIP_ESSENCE.get());
        font.draw(ps, new TextComponent("持有强化粉尘: " + dustCount + " 份 (溶解闲置芯片获得)"),
                rightX + 4, this.height - 88, 0xFFC084FC);

        if (selectedIdx >= 0 && selectedIdx < entries.size()) {
            InvEntry selEntry = entries.get(selectedIdx);
            ItemStack sel = selEntry.stack;

            GenshinHudOverlay.drawVoxelPanel(ps, rightX + 10, rightTop + 10, 32, 32, 0xC00A1018, 0xFF38BDF8, 0xFFFBBF24);
            ps.pushPose();
            ps.translate(rightX + 14, rightTop + 14, 0);
            ps.scale(1.5f, 1.5f, 1.0f);
            this.itemRenderer.renderGuiItem(sel, 0, 0);
            ps.popPose();

            font.drawShadow(ps, new TextComponent(sel.getHoverName().getString()), rightX + 48, rightTop + 12, 0xFFF8FAFC);

            int y = rightTop + 48;
            if (EquipmentStatsHelper.isWeapon(sel) || EquipmentStatsHelper.isChip(sel)) {
                int rarity = EquipmentStatsHelper.getRarity(sel);
                int lv = EquipmentStatsHelper.getEnhanceLevel(sel);
                int maxLv = EquipmentStatsHelper.getMaxEnhanceLevel(sel);
                font.draw(ps, new TextComponent(EquipmentStatsHelper.getStarString(rarity) + "   等级 +" + lv + " / " + maxLv),
                        rightX + 48, rightTop + 26, EquipmentStatsHelper.getRarityColor(rarity));

                String typeLabel = EquipmentStatsHelper.isWeapon(sel)
                        ? ("适配属性：" + EquipmentStatsHelper.getWeaponElementLabel(sel))
                        : ("部位：" + EquipmentStatsHelper.getChipSlotTitle(EquipmentStatsHelper.getChipSlot(sel)) + " (" + rarity + "级装备)");
                font.draw(ps, new TextComponent(typeLabel), rightX + 12, y, 0xFF7DD3FC);
                y += 15;

                fill(ps, rightX + 10, y - 2, rightX + rightW - 10, y + 15, 0x80182C3E);
                font.draw(ps, new TextComponent("主词条：" + EquipmentStatsHelper.formatMainStatLabel(sel)),
                        rightX + 14, y + 2, 0xFF4ADE80);
                y += 20;

                font.draw(ps, new TextComponent("【副词条（每升3级解锁/强化）】"), rightX + 12, y, 0xFF94A3B8);
                y += 12;
                for (String sub : EquipmentStatsHelper.getSubStatLines(sel)) {
                    font.draw(ps, new TextComponent("  • " + sub), rightX + 12, y, 0xFFFBBF24);
                    y += 11;
                }
                if (EquipmentStatsHelper.isWeapon(sel)) {
                    y += 4;
                    for (FormattedCharSequence line : font.split(new TextComponent(EquipmentStatsHelper.getWeaponPassiveDesc(sel)), rightW - 24)) {
                        font.draw(ps, line, rightX + 12, y, 0xFF7DD3FC);
                        y += 11;
                    }
                } else if (!selEntry.isEquipped) {
                    y += 4;
                    font.draw(ps, new TextComponent("提示：溶解该装备可获得 " + EquipmentStatsHelper.getDissolveEssenceYield(sel) + " 份强化粉尘。"),
                            rightX + 12, y, 0xFFC084FC);
                }
            } else {
                font.draw(ps, new TextComponent("持有数量：" + sel.getCount()), rightX + 48, rightTop + 26, 0xFFFBBF24);
                String desc = "用途说明：";
                if (ModItems.getNextTierMaterial(sel.getItem()) != null) {
                    desc = "同系列高阶合成：消耗 3 个此材料可直接合成 1 个高一阶材料。";
                } else if (sel.is(ModItems.EXP_BOTTLE.get()) || sel.is(Items.EXPERIENCE_BOTTLE)) {
                    desc = "用于 [C] 角色面板手动提升角色等级（战斗不会直接增加角色经验）。";
                } else if (sel.is(ModItems.EQUIP_ESSENCE.get())) {
                    desc = "装备强化粉尘：极为珍贵，主要通过溶解其他多余芯片获得，用于强化装备等级。";
                } else if (sel.is(ModItems.STAR_TICKET.get()) || sel.is(ModItems.QUANTUM_CORE.get())) {
                    desc = "用于在 [G] 抽卡界面招募新同伴或获取高星武器！";
                } else if (sel.is(ModItems.CREDIT_COIN.get())) {
                    desc = "星舰通用货币，用于突破与日常开支。";
                }
                for (FormattedCharSequence line : font.split(new TextComponent(desc), rightW - 24)) {
                    font.draw(ps, line, rightX + 12, y, 0xFFE2E8F0);
                    y += 12;
                }
            }
        }

        super.render(ps, mouseX, mouseY, partialTicks);
    }
}
