package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.item.EquipmentStatsHelper;
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
        super(new TextComponent("应急物资箱"));
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
        int boxX = this.width / 2 - 120;
        int boxY = this.height / 2 - 86;

        addRenderableWidget(new Button(boxX + 16, boxY + 138, 100, 20, new TextComponent("[ F ] 全部拾取"), btn -> takeAll()));
        addRenderableWidget(new Button(boxX + 124, boxY + 138, 100, 20, new TextComponent("关闭 (ESC)"), btn -> onClose()));
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

        int boxW = 240;
        int boxH = 180;
        int boxX = (this.width - boxW) / 2;
        int boxY = (this.height - boxH) / 2;

        GenshinHudOverlay.drawVoxelPanel(poseStack, boxX, boxY, boxW, boxH, 0xEE0D1520, 0xFF4A90E2, 0xFFFBBF24);
        font.drawShadow(poseStack, new TextComponent("【应急物资箱】"), boxX + 14, boxY + 12, 0xFF38BDF8);

        if (lootItems.isEmpty()) {
            font.draw(poseStack, new TextComponent("（箱内物资已清空）"), boxX + 16, boxY + 54, 0xFFAAAAAA);
        } else {
            int itemY = boxY + 34;
            for (int i = 0; i < Math.min(lootItems.size(), 5); i++) {
                ItemStack stack = lootItems.get(i);
                int col = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack))
                        ? EquipmentStatsHelper.getRarityColor(EquipmentStatsHelper.getRarity(stack)) : 0xFF3880FF;
                GenshinHudOverlay.drawVoxelPanel(poseStack, boxX + 14, itemY, boxW - 28, 20, 0x90121E2C, col, col);

                this.itemRenderer.renderGuiItem(stack, boxX + 18, itemY + 2);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, boxX + 18, itemY + 2);

                String countStr = stack.getCount() > 1 ? " x" + stack.getCount() : "";
                font.draw(poseStack, new TextComponent(stack.getHoverName().getString() + countStr), boxX + 42, itemY + 6, 0xFFEEEEEE);

                itemY += 22;
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
