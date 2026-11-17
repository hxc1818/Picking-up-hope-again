package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketEquipItem;
import com.resilienthope.core.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;

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

    private Category currentCategory = Category.WEAPONS;
    private final List<ItemStack> categoryItems = new ArrayList<>();
    private int selectedIndex = -1;

    public InfiniteInventoryScreen() {
        super(new TextComponent("星舰战备仓储终端 (无限背包)"));
    }

    @Override
    protected void init() {
        super.init();
        refreshItems();

        int tabStartX = 30;
        int tabY = 25;
        int tabWidth = 75;

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            addRenderableWidget(new Button(tabStartX + i * (tabWidth + 6), tabY, tabWidth, 20,
                    new TextComponent(cat.getDisplayName()),
                    btn -> {
                        this.currentCategory = cat;
                        this.selectedIndex = -1;
                        refreshItems();
                    }));
        }

        // Action Buttons on Right Inspector Panel
        int rightPanelX = this.width / 2 + 50;
        int bottomY = this.height - 50;

        addRenderableWidget(new Button(rightPanelX, bottomY, 110, 20, new TextComponent("装备到角色"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < categoryItems.size()) {
                ItemStack selected = categoryItems.get(selectedIndex);
                PacketHandler.INSTANCE.sendToServer(new PacketEquipItem(selected));
            }
        }));

        addRenderableWidget(new Button(rightPanelX + 115, bottomY, 110, 20, new TextComponent("角色配队 (C)"), btn -> {
            Minecraft.getInstance().setScreen(new CharacterProfileScreen());
        }));

        // Close Button
        addRenderableWidget(new Button(this.width - 65, 15, 45, 20, new TextComponent("关闭"), btn -> onClose()));
    }

    private void refreshItems() {
        categoryItems.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Populate items based on category from inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (currentCategory == Category.WEAPONS && (stack.getItem() instanceof SwordItem || stack.is(ModItems.PLASMA_CUTTER.get()))) {
                categoryItems.add(stack);
            } else if (currentCategory == Category.CHIPS && stack.is(ModItems.FUSION_CORE_CHIP.get())) {
                categoryItems.add(stack);
            } else if (currentCategory == Category.MATERIALS && (stack.is(Items.IRON_INGOT) || stack.is(Items.REDSTONE) || stack.is(ModItems.QUANTUM_CORE.get()))) {
                categoryItems.add(stack);
            } else if (currentCategory == Category.VALUABLES) {
                categoryItems.add(stack);
            }
        }

        // Always ensure default weapon is visible in weapons tab
        if (currentCategory == Category.WEAPONS && categoryItems.isEmpty()) {
            categoryItems.add(new ItemStack(ModItems.PLASMA_CUTTER.get()));
        }
        if (currentCategory == Category.CHIPS && categoryItems.isEmpty()) {
            categoryItems.add(new ItemStack(ModItems.FUSION_CORE_CHIP.get()));
        }

        if (selectedIndex < 0 && !categoryItems.isEmpty()) {
            selectedIndex = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listLeft = 30;
        int listTop = 55;
        int listRight = this.width / 2 + 30;

        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop) {
            int itemIndex = (int) ((mouseY - listTop) / 32);
            if (itemIndex >= 0 && itemIndex < categoryItems.size()) {
                this.selectedIndex = itemIndex;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);

        // Frame
        fill(poseStack, 20, 18, this.width - 20, this.height - 18, 0xEE0D1520);
        fill(poseStack, 20, 18, this.width - 20, 20, 0xFF00ADB5);

        // Header Title
        font.draw(poseStack, getTitle(), 30, 8, 0xFFEEEEEE);

        // Left Container
        int listLeft = 30;
        int listTop = 55;
        int listRight = this.width / 2 + 30;
        int listBottom = this.height - 25;
        fill(poseStack, listLeft, listTop, listRight, listBottom, 0x90111A24);

        // Render Item rows
        for (int i = 0; i < categoryItems.size(); i++) {
            ItemStack stack = categoryItems.get(i);
            int rowY = listTop + i * 32;
            if (rowY + 30 > listBottom) break;

            boolean isSelected = (i == selectedIndex);
            int rowBg = isSelected ? 0x9028445E : 0x5018222E;
            fill(poseStack, listLeft + 4, rowY + 2, listRight - 4, rowY + 30, rowBg);
            if (isSelected) {
                fill(poseStack, listLeft + 4, rowY + 2, listLeft + 7, rowY + 30, 0xFF55FFFF);
            }

            // Item Icon
            this.itemRenderer.renderGuiItem(stack, listLeft + 10, rowY + 7);
            this.itemRenderer.renderGuiItemDecorations(this.font, stack, listLeft + 10, rowY + 7);

            // Item Name & Star level
            font.draw(poseStack, new TextComponent(stack.getHoverName().getString()), listLeft + 32, rowY + 8, isSelected ? 0xFFFFFFFF : 0xFFCCCCCC);
            String stars = currentCategory == Category.WEAPONS ? "★★★★★ 5-STAR" : (currentCategory == Category.CHIPS ? "★★★★ 4-STAR 芯片" : "常规战备物资");
            font.draw(poseStack, new TextComponent(stars), listLeft + 32, rowY + 19, 0xFFFFCC00);
        }

        // Right Inspector Panel
        int rightPanelX = this.width / 2 + 50;
        int rightPanelTop = 55;
        int rightPanelBottom = this.height - 60;
        int rightPanelW = this.width - rightPanelX - 30;

        fill(poseStack, rightPanelX, rightPanelTop, rightPanelX + rightPanelW, rightPanelBottom, 0x80131E2A);
        fill(poseStack, rightPanelX, rightPanelTop, rightPanelX + 2, rightPanelBottom, 0xFF3880FF);

        if (selectedIndex >= 0 && selectedIndex < categoryItems.size()) {
            ItemStack selected = categoryItems.get(selectedIndex);

            // Large Icon preview
            this.itemRenderer.renderGuiItem(selected, rightPanelX + 15, rightPanelTop + 15);

            // Name
            font.draw(poseStack, new TextComponent(selected.getHoverName().getString()), rightPanelX + 38, rightPanelTop + 15, 0xFF55FFFF);
            font.draw(poseStack, new TextComponent("【属性装配面板】"), rightPanelX + 38, rightPanelTop + 27, 0xFFAAAAAA);

            // Stats breakdown
            int statY = rightPanelTop + 48;
            if (currentCategory == Category.WEAPONS) {
                font.draw(poseStack, new TextComponent("• 武器类型: 双手等离子巨剑 (Claymore)"), rightPanelX + 15, statY, 0xFFE0E0E0);
                font.draw(poseStack, new TextComponent("• 基础攻击力: +120 (突破阶数: 2/6)"), rightPanelX + 15, statY + 14, 0xFF4CAF50);
                font.draw(poseStack, new TextComponent("• 动作模组: Better Combat 连击大剑挥砍"), rightPanelX + 15, statY + 28, 0xFF4FC3F7);
                font.draw(poseStack, new TextComponent("• 专属特效: 普通攻击命中附加电浆灼烧"), rightPanelX + 15, statY + 42, 0xFFFFB74D);
            } else if (currentCategory == Category.CHIPS) {
                font.draw(poseStack, new TextComponent("• 芯片槽位: 槽位 1【聚变核心】"), rightPanelX + 15, statY, 0xFFE0E0E0);
                font.draw(poseStack, new TextComponent("• 主属性: 基础攻击力 +46.6%"), rightPanelX + 15, statY + 14, 0xFF4CAF50);
                font.draw(poseStack, new TextComponent("• 副词条 1: 暴击伤害 +14.2%"), rightPanelX + 15, statY + 28, 0xFFFFCC00);
                font.draw(poseStack, new TextComponent("• 副词条 2: 聚变充能效率 +11.8%"), rightPanelX + 15, statY + 42, 0xFF4FC3F7);
            } else {
                font.draw(poseStack, new TextComponent("• 物资数量: " + selected.getCount()), rightPanelX + 15, statY, 0xFFE0E0E0);
                font.draw(poseStack, new TextComponent("• 用途: 星舰反应堆维护 / 角色突破"), rightPanelX + 15, statY + 14, 0xFFAAAAAA);
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
