package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketEquipItem;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketProgressionAction;
import com.resilienthope.core.subtitle.SystemToastManager;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

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
    private final List<SciFiButton> tabButtons = new ArrayList<>();
    private int selectedIdx = 0;
    private int scrollRow = 0;
    private int autoRefreshTicks = 0;

    private SciFiButton primaryActionBtn;
    private SciFiButton secondaryActionBtn;

    public InfiniteInventoryScreen() {
        this(Category.CHIPS);
    }

    public InfiniteInventoryScreen(Category initialCategory) {
        super(new TextComponent("背包 · 装备溶解与材料合成 (B)"));
        this.currentCategory = initialCategory;
    }

    private int gridLeft() { return 18; }
    private int gridTop() { return 48; }
    private int gridRight() { return (int) (this.width * 0.54f); }
    private int gridBottom() { return this.height - 18; }
    private int cellStepX() { return 38; }
    private int cellStepY() { return 40; }
    private int getGridCols() { return Math.max(3, (gridRight() - gridLeft() - 10) / cellStepX()); }
    private int rightX() { return gridRight() + 8; }
    private int rightW() { return this.width - rightX() - 18; }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();
        refreshItems();

        Category[] categories = Category.values();
        int tabSpan = gridRight() - gridLeft();
        int tabWidth = (tabSpan - (categories.length - 1) * 4) / categories.length;
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            SciFiButton tb = addRenderableWidget(new SciFiButton(gridLeft() + i * (tabWidth + 4), 26, tabWidth, 18,
                    new TextComponent(cat.getDisplayName()),
                    btn -> {
                        this.currentCategory = cat;
                        this.selectedIdx = 0;
                        this.scrollRow = 0;
                        refreshItems();
                        updateActionButtons();
                    }, cat == currentCategory ? 0xFFFBBF24 : 0xFF38BDF8));
            tabButtons.add(tb);
        }

        int rx = rightX();
        int rw = rightW();
        int halfBtn = (rw - 4) / 2;
        int bottomY = this.height - 38;

        this.primaryActionBtn = addRenderableWidget(new SciFiButton(rx, bottomY - 22, halfBtn, 18, new TextComponent("装备"), btn -> {
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
        }, 0xFF4ADE80));

        this.secondaryActionBtn = addRenderableWidget(new SciFiButton(rx + halfBtn + 4, bottomY - 22, halfBtn, 18, new TextComponent("溶解/强化"), btn -> {
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
        }, 0xFFC084FC));

        addRenderableWidget(new SciFiButton(rx, bottomY, halfBtn, 18, new TextComponent("角色养成(C)"), btn ->
                Minecraft.getInstance().setScreen(new CharacterProfileScreen())));

        addRenderableWidget(new SciFiButton(rx + halfBtn + 4, bottomY, halfBtn, 18, new TextComponent("跃迁抽卡(G)"), btn ->
                Minecraft.getInstance().setScreen(new TerminalWishScreen()), 0xFFFBBF24));

        addRenderableWidget(new SciFiButton(this.width - 58, 12, 40, 16, new TextComponent("关闭"), btn -> onClose(), 0xFFEF4444));

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
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length && i < tabButtons.size(); i++) {
            tabButtons.get(i).setAccentColor(cats[i] == currentCategory ? 0xFFFBBF24 : 0xFF38BDF8);
        }

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
                primaryActionBtn.setMessage(new TextComponent(e.isEquipped ? "已穿戴中" : "装备到角色"));
            }
            if (secondaryActionBtn != null) {
                if (!e.isEquipped && EquipmentStatsHelper.isChip(stack)) {
                    int yield = EquipmentStatsHelper.getDissolveEssenceYield(stack);
                    secondaryActionBtn.active = true;
                    secondaryActionBtn.setMessage(new TextComponent("溶解(+" + yield + "粉尘)"));
                } else {
                    boolean canUp = EquipmentStatsHelper.getEnhanceLevel(stack) < EquipmentStatsHelper.getMaxEnhanceLevel(stack);
                    secondaryActionBtn.active = canUp;
                    secondaryActionBtn.setMessage(new TextComponent(canUp ? "强化+1" : "已满级"));
                }
            }
        } else if (ModItems.getNextTierMaterial(stack.getItem()) != null) {
            int count = countSameItem(stack.getItem());
            if (primaryActionBtn != null) {
                primaryActionBtn.active = count >= 3;
                primaryActionBtn.setMessage(new TextComponent("3合1升阶"));
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int cols = getGridCols();
        int totalRows = (int) Math.ceil(entries.size() / (double) cols);
        if (delta < 0 && scrollRow + 3 < totalRows) scrollRow++;
        if (delta > 0 && scrollRow > 0) scrollRow--;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gl = gridLeft();
        int gt = gridTop();
        int gr = gridRight();
        int gb = gridBottom();
        int cols = getGridCols();

        if (mouseX >= gl && mouseX <= gr && mouseY >= gt && mouseY <= gb) {
            int col = (int) ((mouseX - (gl + 6)) / cellStepX());
            int row = (int) ((mouseY - (gt + 6)) / cellStepY()) + scrollRow;
            if (col >= 0 && col < cols && row >= 0) {
                int idx = row * cols + col;
                if (idx >= 0 && idx < entries.size()) {
                    this.selectedIdx = idx;
                    updateActionButtons();
                    return true;
                }
            }
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.setFocused(null);
        return handled;
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ps);

        GenshinHudOverlay.drawVoxelPanel(ps, 10, 8, this.width - 20, this.height - 16,
                0xEE0B131E, 0xFF284868, 0xFF38BDF8);
        font.drawShadow(ps, getTitle(), 18, 13, 0xFF38BDF8);

        int gl = gridLeft();
        int gt = gridTop();
        int gr = gridRight();
        int gb = gridBottom();
        GenshinHudOverlay.drawVoxelPanel(ps, gl, gt, gr - gl, gb - gt,
                0xA00D1724, 0xFF1F3850, 0xFF38BDF8);

        int cols = getGridCols();
        if (entries.isEmpty()) {
            font.draw(ps, new TextComponent("当前分类暂无物品（开物资箱或副本获取）"), gl + 10, gt + 16, 0xFF94A3B8);
        } else {
            int startIdx = scrollRow * cols;
            for (int i = startIdx; i < entries.size(); i++) {
                int rel = i - startIdx;
                int row = rel / cols;
                int col = rel % cols;
                int cx = gl + 6 + col * cellStepX();
                int cy = gt + 6 + row * cellStepY();
                if (cy + 36 > gb) break;

                InvEntry entry = entries.get(i);
                ItemStack stack = entry.stack;
                boolean isSel = (i == selectedIdx);
                int rarity = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack))
                        ? EquipmentStatsHelper.getRarity(stack)
                        : Math.max(2, Math.min(5, ModItems.getMaterialTier(stack.getItem())));
                int rarityCol = EquipmentStatsHelper.getRarityColor(rarity);

                GenshinHudOverlay.drawVoxelPanel(ps, cx, cy, 34, 36,
                        isSel ? 0xD8213A54 : 0xB014202E, isSel ? 0xFFFFFFFF : rarityCol, rarityCol);

                this.itemRenderer.renderGuiItem(stack, cx + 9, cy + 5);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, cx + 9, cy + 5);

                String badge = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack))
                        ? ("+" + EquipmentStatsHelper.getEnhanceLevel(stack))
                        : ("x" + stack.getCount());
                int badgeCol = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack)) ? 0xFFFBBF24 : 0xFFE2E8F0;
                font.draw(ps, new TextComponent(badge), cx + 17 - font.width(badge) / 2, cy + 25, badgeCol);

                if (entry.isEquipped) {
                    fill(ps, cx + 1, cy + 1, cx + 11, cy + 10, 0xE016A34A);
                    font.draw(ps, new TextComponent("装"), cx + 2, cy + 2, 0xFFFFFFFF);
                }
            }
        }

        int rx = rightX();
        int rw = rightW();
        int rightTop = 48;
        int rightBottom = this.height - 64;
        GenshinHudOverlay.drawVoxelPanel(ps, rx, rightTop, rw, rightBottom - rightTop,
                0xB0101A26, 0xFF305880, 0xFF38BDF8);

        int dustCount = countSameItem(ModItems.EQUIP_ESSENCE.get());
        font.draw(ps, new TextComponent("强化粉尘: " + dustCount + " (溶解闲置芯片)"),
                rx + 2, this.height - 74, 0xFFC084FC);

        if (selectedIdx >= 0 && selectedIdx < entries.size()) {
            InvEntry selEntry = entries.get(selectedIdx);
            ItemStack sel = selEntry.stack;

            GenshinHudOverlay.drawVoxelPanel(ps, rx + 8, rightTop + 8, 28, 28, 0xC00A1018, 0xFF38BDF8, 0xFFFBBF24);
            this.itemRenderer.renderGuiItem(sel, rx + 14, rightTop + 14);

            font.drawShadow(ps, new TextComponent(sel.getHoverName().getString()), rx + 42, rightTop + 10, 0xFFF8FAFC);

            int y = rightTop + 42;
            if (EquipmentStatsHelper.isWeapon(sel) || EquipmentStatsHelper.isChip(sel)) {
                int rarity = EquipmentStatsHelper.getRarity(sel);
                int lv = EquipmentStatsHelper.getEnhanceLevel(sel);
                int maxLv = EquipmentStatsHelper.getMaxEnhanceLevel(sel);
                font.draw(ps, new TextComponent(EquipmentStatsHelper.getStarString(rarity) + " +" + lv + "/" + maxLv),
                        rx + 42, rightTop + 22, EquipmentStatsHelper.getRarityColor(rarity));

                String typeLabel = EquipmentStatsHelper.isWeapon(sel)
                        ? ("属性：" + EquipmentStatsHelper.getWeaponElementLabel(sel))
                        : (EquipmentStatsHelper.getChipSlotTitle(EquipmentStatsHelper.getChipSlot(sel)));
                font.draw(ps, new TextComponent(typeLabel), rx + 8, y, 0xFF7DD3FC);
                y += 12;

                fill(ps, rx + 6, y - 1, rx + rw - 6, y + 12, 0x80182C3E);
                font.draw(ps, new TextComponent("主：" + EquipmentStatsHelper.formatMainStatLabel(sel)),
                        rx + 10, y + 1, 0xFF4ADE80);
                y += 15;

                font.draw(ps, new TextComponent("【副词条 (每+3级提升)】"), rx + 8, y, 0xFF94A3B8);
                y += 11;
                for (String sub : EquipmentStatsHelper.getSubStatLines(sel)) {
                    if (y + 10 > rightBottom - 18) break;
                    font.draw(ps, new TextComponent("• " + sub), rx + 10, y, 0xFFFBBF24);
                    y += 10;
                }
                if (EquipmentStatsHelper.isWeapon(sel)) {
                    for (FormattedCharSequence line : font.split(new TextComponent(EquipmentStatsHelper.getWeaponPassiveDesc(sel)), rw - 16)) {
                        if (y + 10 > rightBottom - 4) break;
                        font.draw(ps, line, rx + 8, y, 0xFF7DD3FC);
                        y += 10;
                    }
                } else if (!selEntry.isEquipped && y + 10 <= rightBottom - 4) {
                    font.draw(ps, new TextComponent("溶解可得粉尘: +" + EquipmentStatsHelper.getDissolveEssenceYield(sel)),
                            rx + 8, y, 0xFFC084FC);
                }
            } else {
                font.draw(ps, new TextComponent("数量：" + sel.getCount()), rx + 42, rightTop + 22, 0xFFFBBF24);
                String desc = "用途说明：";
                if (ModItems.getNextTierMaterial(sel.getItem()) != null) {
                    desc = "同系列高阶合成：消耗 3 个此材料可合成 1 个高一阶材料。";
                } else if (sel.is(ModItems.EXP_BOTTLE.get()) || sel.is(Items.EXPERIENCE_BOTTLE)) {
                    desc = "在 [C] 角色面板手动提升角色等级（战斗不直接涨角色经验）。";
                } else if (sel.is(ModItems.EQUIP_ESSENCE.get())) {
                    desc = "装备强化粉尘：通过溶解多余芯片获得，用于强化装备。";
                } else if (sel.is(ModItems.STAR_TICKET.get()) || sel.is(ModItems.QUANTUM_CORE.get())) {
                    desc = "用于在 [G] 抽卡界面招募新同伴与高星武器！";
                } else if (sel.is(ModItems.CREDIT_COIN.get())) {
                    desc = "星舰通用货币，用于突破与强化开销。";
                }
                for (FormattedCharSequence line : font.split(new TextComponent(desc), rw - 16)) {
                    if (y + 11 > rightBottom - 4) break;
                    font.draw(ps, line, rx + 8, y, 0xFFE2E8F0);
                    y += 11;
                }
            }
        }

        super.render(ps, mouseX, mouseY, partialTicks);
        SystemToastManager.render(ps, this.font, this.width, this.height);
    }
}
