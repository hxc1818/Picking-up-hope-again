package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketEquipItem;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class InfiniteInventoryScreen extends Screen {

    public enum Category {
        WEAPONS("武器库"),
        CHIPS("科技芯片"),
        MATERIALS("养成材料"),
        VALUABLES("贵重任务物");

        private final String displayName;
        Category(String name) { this.displayName = name; }
        public String getDisplayName() { return displayName; }
    }

    private static class InvEntry {
        final int invIndex; // -1 if currently equipped chip in CharacterData
        final int equippedChipSlot; // -1 if in player inventory
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

    private Button equipBtn;
    private Button enhanceBtn;

    public InfiniteInventoryScreen() {
        this(Category.WEAPONS);
    }

    public InfiniteInventoryScreen(Category initialCategory) {
        super(new TextComponent("星舰战备仓储终端 · 体素背包 (B)"));
        this.currentCategory = initialCategory;
    }

    @Override
    protected void init() {
        super.init();
        refreshItems();

        int tabStartX = 28;
        int tabY = 26;
        int tabWidth = 72;

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
        int bottomY = this.height - 48;

        this.equipBtn = addRenderableWidget(new Button(rightX, bottomY - 24, 96, 20, new TextComponent("装备到主角"), btn -> {
            if (selectedIdx >= 0 && selectedIdx < entries.size()) {
                InvEntry e = entries.get(selectedIdx);
                if (!e.isEquipped && e.invIndex >= 0) {
                    PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_EQUIP_FROM_INV, e.invIndex, e.stack));
                    autoRefreshTicks = 5;
                }
            }
        }));

        this.enhanceBtn = addRenderableWidget(new Button(rightX + 102, bottomY - 24, 96, 20, new TextComponent("强化升级 (+1)"), btn -> {
            if (selectedIdx >= 0 && selectedIdx < entries.size()) {
                InvEntry e = entries.get(selectedIdx);
                if (e.isEquipped && e.equippedChipSlot >= 0) {
                    PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_EQUIPPED, e.equippedChipSlot, ItemStack.EMPTY));
                } else if (e.isEquipped && EquipmentStatsHelper.isWeapon(e.stack)) {
                    PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_EQUIPPED, -1, ItemStack.EMPTY));
                } else if (e.invIndex >= 0) {
                    PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(PacketEquipItem.ACTION_ENHANCE_INV, e.invIndex, e.stack));
                }
                autoRefreshTicks = 5;
            }
        }));

        addRenderableWidget(new Button(rightX, bottomY, 96, 20, new TextComponent("主角终端 (C)"), btn ->
                Minecraft.getInstance().setScreen(new CharacterProfileScreen())));

        addRenderableWidget(new Button(rightX + 102, bottomY, 96, 20, new TextComponent("终端祈愿 (G)"), btn ->
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

    private void updateActionButtons() {
        boolean hasItem = selectedIdx >= 0 && selectedIdx < entries.size();
        boolean isEquipable = false;
        boolean canEquip = false;
        if (hasItem) {
            InvEntry e = entries.get(selectedIdx);
            isEquipable = EquipmentStatsHelper.isWeapon(e.stack) || EquipmentStatsHelper.isChip(e.stack);
            canEquip = isEquipable && !e.isEquipped;
        }
        if (equipBtn != null) {
            equipBtn.active = canEquip;
            equipBtn.setMessage(new TextComponent(hasItem && entries.get(selectedIdx).isEquipped ? "已穿戴中" : "装备到主角"));
        }
        if (enhanceBtn != null) {
            enhanceBtn.active = isEquipable;
        }
    }

    private void refreshItems() {
        entries.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // First include currently equipped items in the matching category so player can view/enhance them
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
                    continue; // Already listed as equipped weapon at slot 0
                }
                if (EquipmentStatsHelper.isWeapon(stack)) {
                    entries.add(new InvEntry(i, -1, stack.copy(), false));
                }
            } else if (currentCategory == Category.CHIPS) {
                if (EquipmentStatsHelper.isChip(stack)) {
                    entries.add(new InvEntry(i, -1, stack.copy(), false));
                }
            } else if (currentCategory == Category.MATERIALS) {
                if (stack.is(ModItems.ALLOY_PLATE.get()) || stack.is(Items.IRON_INGOT) || stack.is(Items.REDSTONE)) {
                    entries.add(new InvEntry(i, -1, stack.copy(), false));
                }
            } else if (currentCategory == Category.VALUABLES) {
                if (stack.is(ModItems.QUANTUM_CORE.get()) || stack.is(ModItems.PRECURSOR_SPARK.get())) {
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
        int gridLeft = 28;
        int gridWidth = this.width / 2 - 20;
        int cols = getGridCols(gridWidth);
        int totalRows = (int) Math.ceil(entries.size() / (double) cols);
        if (delta < 0 && scrollRow + 3 < totalRows) scrollRow++;
        if (delta > 0 && scrollRow > 0) scrollRow--;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridLeft = 28;
        int gridTop = 54;
        int gridRight = this.width / 2 + 10;
        int gridBottom = this.height - 24;
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

    private int countEnhanceMaterials(Player player) {
        if (player == null) return 0;
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.ALLOY_PLATE.get())) count += s.getCount();
        }
        return count;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);

        // Main Frame
        GenshinHudOverlay.drawVoxelPanel(poseStack, 16, 12, this.width - 32, this.height - 24,
                0xEE0B131E, 0xFF284868, 0xFF00E5FF);
        font.drawShadow(poseStack, getTitle(), 28, 16, 0xFF66FFFF);

        // Left Grid Backdrop
        int gridLeft = 28;
        int gridTop = 54;
        int gridRight = this.width / 2 + 10;
        int gridBottom = this.height - 24;
        GenshinHudOverlay.drawVoxelPanel(poseStack, gridLeft, gridTop, gridRight - gridLeft, gridBottom - gridTop,
                0xA00D1724, 0xFF1F3850, 0xFF4FC3F7);

        int cols = getGridCols(gridRight - gridLeft);
        if (entries.isEmpty()) {
            font.draw(poseStack, new TextComponent("当前分类暂无物品（探索或击败敌方即可获取）"), gridLeft + 16, gridTop + 20, 0xFF8899AA);
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
                        ? EquipmentStatsHelper.getRarity(stack) : 3;
                int rarityCol = EquipmentStatsHelper.getRarityColor(rarity);

                GenshinHudOverlay.drawVoxelPanel(poseStack, cx, cy, 40, 42,
                        isSel ? 0xD8213A54 : 0xB014202E,
                        isSel ? 0xFFFFFFFF : rarityCol,
                        rarityCol);

                // Item 16x16 Voxel Icon
                this.itemRenderer.renderGuiItem(stack, cx + 12, cy + 7);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, cx + 12, cy + 7);

                // Bottom label (+Lv or xCount)
                String badge;
                int badgeCol;
                if (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack)) {
                    badge = "+" + EquipmentStatsHelper.getEnhanceLevel(stack);
                    badgeCol = 0xFFFFE082;
                } else {
                    badge = "x" + stack.getCount();
                    badgeCol = 0xFFCCEEFF;
                }
                int bw = font.width(badge);
                font.draw(poseStack, new TextComponent(badge), cx + 20 - bw / 2, cy + 30, badgeCol);

                // Equipped badge
                if (entry.isEquipped) {
                    fill(poseStack, cx + 2, cy + 2, cx + 11, cy + 11, 0xE000A86B);
                    font.draw(poseStack, new TextComponent("E"), cx + 4, cy + 3, 0xFFFFFFFF);
                }
            }
        }

        // Right Inspector Panel
        int rightX = this.width / 2 + 20;
        int rightTop = 54;
        int rightBottom = this.height - 76;
        int rightW = this.width - rightX - 24;
        GenshinHudOverlay.drawVoxelPanel(poseStack, rightX, rightTop, rightW, rightBottom - rightTop,
                0xB0101A26, 0xFF305880, 0xFF4FC3F7);

        Player player = Minecraft.getInstance().player;
        int alloyCount = countEnhanceMaterials(player);
        font.draw(poseStack, new TextComponent("持有重工强化合金: " + alloyCount), rightX + 8, this.height - 90, 0xFFFFD54F);

        if (selectedIdx >= 0 && selectedIdx < entries.size()) {
            InvEntry selEntry = entries.get(selectedIdx);
            ItemStack sel = selEntry.stack;

            // Large icon preview frame
            GenshinHudOverlay.drawVoxelPanel(poseStack, rightX + 10, rightTop + 10, 32, 32, 0xC00A1018, 0xFF4FC3F7, 0xFFFFCC00);
            poseStack.pushPose();
            poseStack.translate(rightX + 14, rightTop + 14, 0);
            poseStack.scale(1.5f, 1.5f, 1.0f);
            this.itemRenderer.renderGuiItem(sel, 0, 0);
            poseStack.popPose();

            font.drawShadow(poseStack, new TextComponent(sel.getHoverName().getString()), rightX + 48, rightTop + 12, 0xFF66FFFF);

            int y = rightTop + 48;
            if (EquipmentStatsHelper.isWeapon(sel) || EquipmentStatsHelper.isChip(sel)) {
                int rarity = EquipmentStatsHelper.getRarity(sel);
                int lv = EquipmentStatsHelper.getEnhanceLevel(sel);
                String typeLabel = EquipmentStatsHelper.isWeapon(sel)
                        ? "类型：双手重剑专武"
                        : "部位：" + EquipmentStatsHelper.getChipSlotTitle(EquipmentStatsHelper.getChipSlot(sel));
                font.draw(poseStack, new TextComponent(EquipmentStatsHelper.getStarString(rarity) + "   强化等级 +" + lv + " / 20"),
                        rightX + 48, rightTop + 26, EquipmentStatsHelper.getRarityColor(rarity));
                font.draw(poseStack, new TextComponent(typeLabel), rightX + 12, y, 0xFFDCE6F2);
                y += 16;

                fill(poseStack, rightX + 10, y - 2, rightX + rightW - 10, y + 16, 0x80182C3E);
                font.draw(poseStack, new TextComponent("主词条：" + EquipmentStatsHelper.formatMainStatLabel(sel)),
                        rightX + 14, y + 3, 0xFF4ADE80);
                y += 22;

                font.draw(poseStack, new TextComponent("【副词条属性】"), rightX + 12, y, 0xFF9EC5E8);
                y += 13;
                for (String sub : EquipmentStatsHelper.getSubStatLines(sel)) {
                    font.draw(poseStack, new TextComponent("  • " + sub), rightX + 12, y, 0xFFFFE082);
                    y += 12;
                }
                if (EquipmentStatsHelper.isWeapon(sel)) {
                    y += 4;
                    for (FormattedCharSequence line : font.split(new TextComponent("特效：" + EquipmentStatsHelper.getWeaponPassiveDesc(sel)), rightW - 24)) {
                        font.draw(poseStack, line, rightX + 12, y, 0xFF7DD3FC);
                        y += 11;
                    }
                }
            } else {
                font.draw(poseStack, new TextComponent("持有数量：" + sel.getCount()), rightX + 48, rightTop + 26, 0xFFFFE082);
                String desc = sel.is(ModItems.QUANTUM_CORE.get())
                        ? "用途：在【终端数据复现 (G)】界面消耗算力芯片，抽取 4★/5★ 专武与全套科技芯片。"
                        : (sel.is(ModItems.PRECURSOR_SPARK.get())
                        ? "用途：极寒异星先驱机甲核心产出的高能火种，决定重拾希望号的终极航向。"
                        : "用途：用于武器与 5 部位科技芯片的强化升级（每次强化消耗 1 枚重工强化合金）。");
                for (FormattedCharSequence line : font.split(new TextComponent(desc), rightW - 24)) {
                    font.draw(poseStack, line, rightX + 12, y, 0xFFDCE6F2);
                    y += 11;
                }
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
