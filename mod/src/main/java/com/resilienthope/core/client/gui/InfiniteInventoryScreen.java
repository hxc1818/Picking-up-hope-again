package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

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
    private final List<String> dummyItems = new ArrayList<>();
    private int selectedItemIndex = 0;

    public InfiniteInventoryScreen() {
        super(new TextComponent("星舰物资仓储终端 (无限背包)"));
        loadDummyData(Category.WEAPONS);
    }

    private void loadDummyData(Category category) {
        dummyItems.clear();
        selectedItemIndex = 0;
        switch (category) {
            case WEAPONS:
                dummyItems.add("【专武】等离子重切剑 ★★★★★ Lv.40");
                dummyItems.add("【专武】轻量高频震荡刃 ★★★★ Lv.20");
                dummyItems.add("【制式】工会重型扳手 ★★★ Lv.10");
                break;
            case CHIPS:
                dummyItems.add("【控制核心】重工装甲·核心 +16 (主:生命值)");
                dummyItems.add("【动力发生器】过载电浆·发生器 +12 (主:攻击力)");
                dummyItems.add("【能源电池】量子跃迁·电池 +8 (主:充能效率)");
                break;
            case MATERIALS:
                dummyItems.add("高纯度聚变矿原石 × 482");
                dummyItems.add("损毁哨兵伺服核心 × 64");
                dummyItems.add("精炼重氢罐 × 120");
                break;
            case VALUABLES:
                dummyItems.add("星舰舰桥二级通行密匙 × 1");
                dummyItems.add("星辉算力芯片(抽卡代币) × 1600");
                dummyItems.add("先驱者残存黑匣子数据 × 3");
                break;
        }
    }

    @Override
    protected void init() {
        super.init();
        int tabStartX = 30;
        int tabY = 25;
        int tabWidth = 80;

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            addRenderableWidget(new Button(tabStartX + i * (tabWidth + 5), tabY, tabWidth, 20,
                    new TextComponent(cat.getDisplayName()),
                    btn -> {
                        this.currentCategory = cat;
                        loadDummyData(cat);
                    }));
        }

        // Close Button
        addRenderableWidget(new Button(this.width - 60, 15, 45, 20, new TextComponent("关闭"), btn -> onClose()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);

        // Dark Sci-Fi Background Frame
        fill(poseStack, 20, 20, this.width - 20, this.height - 20, 0xE60C141E);
        fill(poseStack, 20, 20, this.width - 20, 22, 0xFF00ADB5);

        // Header Title
        font.draw(poseStack, getTitle(), 30, 10, 0xFFEEEEEE);

        // Left Category List Container
        int listLeft = 30;
        int listTop = 55;
        int listRight = this.width / 2 + 30;
        int listBottom = this.height - 35;
        fill(poseStack, listLeft, listTop, listRight, listBottom, 0x80141E28);

        // Render Item rows
        int rowY = listTop + 10;
        for (int i = 0; i < dummyItems.size(); i++) {
            boolean isSelected = (i == selectedItemIndex);
            int rowColor = isSelected ? 0x902A3E50 : 0x40101820;
            fill(poseStack, listLeft + 5, rowY - 3, listRight - 5, rowY + 18, rowColor);
            if (isSelected) {
                fill(poseStack, listLeft + 5, rowY - 3, listLeft + 8, rowY + 18, 0xFF00ADB5);
            }
            font.draw(poseStack, new TextComponent(dummyItems.get(i)), listLeft + 15, rowY + 2, isSelected ? 0xFFFFFF : 0xAAAAAA);
            rowY += 24;
        }

        // Right Detail Inspection Panel
        int detailLeft = listRight + 15;
        int detailTop = listTop;
        int detailRight = this.width - 30;
        int detailBottom = listBottom;
        fill(poseStack, detailLeft, detailTop, detailRight, detailBottom, 0x80141E28);

        if (!dummyItems.isEmpty() && selectedItemIndex < dummyItems.size()) {
            String selectedItem = dummyItems.get(selectedItemIndex);
            font.draw(poseStack, new TextComponent(selectedItem), detailLeft + 12, detailTop + 15, 0xFFD4AF37);
            font.draw(poseStack, new TextComponent("物品类型: " + currentCategory.getDisplayName()), detailLeft + 12, detailTop + 35, 0x88AAAAAA);
            font.draw(poseStack, new TextComponent("储存位置: 星舰量子物资矩阵 (无容量上限)"), detailLeft + 12, detailTop + 55, 0xFF4CAF50);
            font.draw(poseStack, new TextComponent("描述: 飞船中央主控计算机统一调配的标准战备物资。"), detailLeft + 12, detailTop + 80, 0xFFCCCCCC);
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listLeft = 30;
        int listTop = 55;
        int listRight = this.width / 2 + 30;

        int rowY = listTop + 10;
        for (int i = 0; i < dummyItems.size(); i++) {
            if (mouseX >= listLeft + 5 && mouseX <= listRight - 5 && mouseY >= rowY - 3 && mouseY <= rowY + 18) {
                this.selectedItemIndex = i;
                return true;
            }
            rowY += 24;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
