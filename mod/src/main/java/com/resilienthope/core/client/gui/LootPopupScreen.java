package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketLootChest;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class LootPopupScreen extends Screen {
    private final BlockPos chestPos;
    private final List<ItemStack> lootItems = new ArrayList<>();

    public LootPopupScreen(BlockPos chestPos, Container container) {
        super(new TextComponent("星舰物资仓储箱"));
        this.chestPos = chestPos;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                lootItems.add(stack.copy());
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        int boxX = this.width / 2 - 110;
        int boxY = this.height / 2 - 80;

        addRenderableWidget(new Button(boxX + 15, boxY + 130, 90, 20, new TextComponent("[ F ] 全部拾取"), btn -> takeAll()));
        addRenderableWidget(new Button(boxX + 115, boxY + 130, 90, 20, new TextComponent("关闭 (ESC)"), btn -> onClose()));
    }

    private void takeAll() {
        PacketHandler.INSTANCE.sendToServer(new PacketLootChest(this.chestPos));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F) {
            takeAll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);

        int boxW = 220;
        int boxH = 165;
        int boxX = (this.width - boxW) / 2;
        int boxY = (this.height - boxH) / 2;

        // Sci-Fi Backdrop
        fill(poseStack, boxX, boxY, boxX + boxW, boxY + boxH, 0xEB101824);
        fill(poseStack, boxX, boxY, boxX + boxW, boxY + 2, 0xFF4A90E2);
        fill(poseStack, boxX, boxY + boxH - 2, boxX + boxW, boxY + boxH, 0xFF4A90E2);

        // Title
        font.draw(poseStack, new TextComponent("【战利品物资箱】"), boxX + 14, boxY + 12, 0xFF55FFFF);

        // Content
        if (lootItems.isEmpty()) {
            font.draw(poseStack, new TextComponent("（箱内暂无战利品）"), boxX + 16, boxY + 50, 0xFFAAAAAA);
        } else {
            int itemY = boxY + 34;
            for (int i = 0; i < Math.min(lootItems.size(), 4); i++) {
                ItemStack stack = lootItems.get(i);
                fill(poseStack, boxX + 14, itemY, boxX + boxW - 14, itemY + 22, 0x60000000);

                // Render item
                this.itemRenderer.renderGuiItem(stack, boxX + 18, itemY + 3);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, boxX + 18, itemY + 3);

                // Render item name & count
                String countStr = stack.getCount() > 1 ? " x" + stack.getCount() : "";
                font.draw(poseStack, new TextComponent(stack.getHoverName().getString() + countStr), boxX + 42, itemY + 7, 0xFFEEEEEE);

                itemY += 24;
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
