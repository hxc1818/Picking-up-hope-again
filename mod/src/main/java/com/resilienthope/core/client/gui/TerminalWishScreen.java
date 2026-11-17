package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketWishPull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TerminalWishScreen extends Screen {
    private final List<ItemStack> latestPulls = new ArrayList<>();

    public TerminalWishScreen() {
        super(new TextComponent("星辉算力终端 · 数据复现祈愿 (G)"));
    }

    public void setPullResults(List<ItemStack> pulls) {
        this.latestPulls.clear();
        if (pulls != null) {
            this.latestPulls.addAll(pulls);
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int btnY = this.height - 44;

        addRenderableWidget(new Button(cx - 140, btnY, 130, 22, new TextComponent("单次复现 (消耗10算力)"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketWishPull(1))));

        addRenderableWidget(new Button(cx + 10, btnY, 130, 22, new TextComponent("十连复现 (消耗100算力)"), btn ->
                PacketHandler.INSTANCE.sendToServer(new PacketWishPull(10))));

        addRenderableWidget(new Button(this.width - 70, 14, 48, 18, new TextComponent("关闭"), btn -> onClose()));
    }

    private int getQuantumCoreCount() {
        Player p = Minecraft.getInstance().player;
        if (p == null) return 0;
        int total = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.QUANTUM_CORE.get())) total += s.getCount();
        }
        return total;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);

        GenshinHudOverlay.drawVoxelPanel(poseStack, 20, 12, this.width - 40, this.height - 24,
                0xEE0B121D, 0xFF3880FF, 0xFFFFD54F);
        font.drawShadow(poseStack, getTitle(), 32, 18, 0xFFFFD54F);

        int cores = getQuantumCoreCount();
        font.draw(poseStack, new TextComponent("当前持有星辉算力芯片: " + cores + " 枚"), 32, 34, 0xFF66FFFF);
        font.draw(poseStack, new TextComponent("复现概率：5★ 先驱专武/芯片 8%  |  4★ 专武/芯片 47%  |  强化合金 45%"),
                32, 46, 0xFF99AABB);

        int areaX = 32;
        int areaY = 64;
        int areaW = this.width - 64;
        int areaH = this.height - 120;
        GenshinHudOverlay.drawVoxelPanel(poseStack, areaX, areaY, areaW, areaH, 0x900E1624, 0xFF284868, 0xFF55FFFF);

        if (latestPulls.isEmpty()) {
            String hint = "点击下方【单次复现】或【十连复现】，消耗星辉算力芯片获取全套专武与随机词条科技芯片！";
            int hw = font.width(hint);
            font.draw(poseStack, new TextComponent(hint), this.width / 2 - hw / 2, areaY + areaH / 2 - 4, 0xFFDCE6F2);
        } else {
            int cols = Math.min(5, latestPulls.size());
            int cardW = 68;
            int cardH = 56;
            int startX = this.width / 2 - (cols * (cardW + 6)) / 2;
            int startY = areaY + 14;

            for (int i = 0; i < latestPulls.size(); i++) {
                ItemStack stack = latestPulls.get(i);
                int r = i / 5;
                int c = i % 5;
                int x = startX + c * (cardW + 6);
                int y = startY + r * (cardH + 8);
                int rarity = (EquipmentStatsHelper.isWeapon(stack) || EquipmentStatsHelper.isChip(stack))
                        ? EquipmentStatsHelper.getRarity(stack) : 3;
                int col = EquipmentStatsHelper.getRarityColor(rarity);

                GenshinHudOverlay.drawVoxelPanel(poseStack, x, y, cardW, cardH, 0xD0121E2C, col, col);
                this.itemRenderer.renderGuiItem(stack, x + cardW / 2 - 8, y + 8);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, x + cardW / 2 - 8, y + 8);

                String stars = EquipmentStatsHelper.getStarString(rarity);
                font.draw(poseStack, new TextComponent(stars), x + (cardW - font.width(stars)) / 2, y + 29, col);

                String name = stack.getHoverName().getString();
                if (name.length() > 7) name = name.substring(0, 7) + "..";
                font.draw(poseStack, new TextComponent(name), x + (cardW - font.width(name)) / 2, y + 41, 0xFFEEEEEE);
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}
