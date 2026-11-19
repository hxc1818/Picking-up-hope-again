package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketWishPull;
import com.resilienthope.core.subtitle.SystemToastManager;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TerminalWishScreen extends Screen {
    private enum Phase { IDLE, WARP_STREAM, REVEAL_ONE, SUMMARY }

    private final List<ItemStack> latestPulls = new ArrayList<>();
    private Phase phase = Phase.IDLE;
    private int animTicks = 0;
    private int revealIdx = 0;
    private int maxBatchRarity = 3;

    private final float[] starX = new float[45];
    private final float[] starY = new float[45];
    private final float[] starSpeed = new float[45];

    public TerminalWishScreen() {
        super(new TextComponent("星海跃迁招募终端 (G)"));
        Random rng = new Random(2026);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = (rng.nextFloat() - 0.5f) * 2.0f;
            starY[i] = (rng.nextFloat() - 0.5f) * 2.0f;
            starSpeed[i] = 0.04f + rng.nextFloat() * 0.08f;
        }
    }

    public void setPullResults(List<ItemStack> pulls) {
        this.latestPulls.clear();
        this.maxBatchRarity = 3;
        if (pulls != null && !pulls.isEmpty()) {
            this.latestPulls.addAll(pulls);
            for (ItemStack s : pulls) {
                int r = getPullRarity(s);
                if (r > maxBatchRarity) maxBatchRarity = r;
            }
            this.phase = Phase.WARP_STREAM;
            this.animTicks = 0;
            this.revealIdx = 0;
            refreshControls();
        }
    }

    private int getPullRarity(ItemStack s) {
        CompoundTag t = s.getTag();
        if (t != null && t.contains("WishRarity")) return t.getInt("WishRarity");
        if (EquipmentStatsHelper.isWeapon(s) || EquipmentStatsHelper.isChip(s)) return EquipmentStatsHelper.getRarity(s);
        return 3;
    }

    @Override
    protected void init() {
        super.init();
        refreshControls();
    }

    private void refreshControls() {
        clearWidgets();
        int cx = this.width / 2;
        int btnY = this.height - 34;

        if (phase == Phase.WARP_STREAM) {
            addRenderableWidget(new SciFiButton(this.width - 90, 14, 72, 18,
                    new TextComponent("跳过动画 ▶"), btn -> {
                this.phase = Phase.SUMMARY;
                refreshControls();
            }, 0xFFFBBF24));
            return;
        }

        if (phase == Phase.REVEAL_ONE) {
            addRenderableWidget(new SciFiButton(cx - 108, btnY, 102, 20,
                    new TextComponent("下一张 ▶ (Space)"), btn -> advanceReveal(), 0xFF38BDF8));
            addRenderableWidget(new SciFiButton(cx + 6, btnY, 102, 20,
                    new TextComponent("查看总览"), btn -> {
                this.phase = Phase.SUMMARY;
                refreshControls();
            }, 0xFFFBBF24));
            return;
        }

        addRenderableWidget(new SciFiButton(cx - 134, btnY, 128, 20,
                new TextComponent("单次跃迁 (1张通行券)"), btn -> PacketHandler.INSTANCE.sendToServer(new PacketWishPull(1)), 0xFF38BDF8));

        addRenderableWidget(new SciFiButton(cx + 6, btnY, 128, 20,
                new TextComponent("十连跃迁 (10张通行券)"), btn -> PacketHandler.INSTANCE.sendToServer(new PacketWishPull(10)), 0xFFFBBF24));

        addRenderableWidget(new SciFiButton(this.width - 62, 12, 42, 16, new TextComponent("关闭"), btn -> onClose(), 0xFFEF4444));
    }

    private void advanceReveal() {
        if (revealIdx + 1 < latestPulls.size()) {
            revealIdx++;
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
            }
        } else {
            phase = Phase.SUMMARY;
            refreshControls();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (phase == Phase.WARP_STREAM) {
            animTicks++;
            if (animTicks >= 52) {
                phase = Phase.REVEAL_ONE;
                revealIdx = 0;
                refreshControls();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (phase == Phase.REVEAL_ONE && (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER)) {
            advanceReveal();
            return true;
        }
        if (phase == Phase.WARP_STREAM && keyCode == GLFW.GLFW_KEY_SPACE) {
            phase = Phase.SUMMARY;
            refreshControls();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int getTicketCount() {
        Player p = Minecraft.getInstance().player;
        if (p == null) return 0;
        int total = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.STAR_TICKET.get())) total += s.getCount();
        }
        return total;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        if (phase == Phase.WARP_STREAM) {
            renderWarpAnimation(ps, partialTicks);
            super.render(ps, mouseX, mouseY, partialTicks);
            return;
        }

        renderBackground(ps);
        GenshinHudOverlay.drawVoxelPanel(ps, 12, 8, this.width - 24, this.height - 16,
                0xEE09101A, 0xFF284868, 0xFFFBBF24);
        font.drawShadow(ps, getTitle(), 22, 13, 0xFFFBBF24);

        int tickets = getTicketCount();
        final int[] pityLeft = {80};
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team ->
                    pityLeft[0] = Math.max(1, 80 - team.getPityCounter()));
        }
        font.draw(ps, new TextComponent("持有抽卡券: " + tickets + " 张   |   距 5★ 保底: " + pityLeft[0] + " 抽"),
                22, 27, 0xFF7DD3FC);
        font.draw(ps, new TextComponent("可招募：5★ 韩冰(火·对单)、5★ 周敏(水·增益)、4★ 老郑(火·拉盾)及高星武器芯片"),
                22, 38, 0xFF94A3B8);

        int areaX = 22;
        int areaY = 52;
        int areaW = this.width - 44;
        int areaH = this.height - 94;
        GenshinHudOverlay.drawVoxelPanel(ps, areaX, areaY, areaW, areaH, 0x900E1624, 0xFF284868, 0xFF38BDF8);

        if (phase == Phase.REVEAL_ONE && revealIdx < latestPulls.size()) {
            renderSingleRevealCard(ps, latestPulls.get(revealIdx), areaX, areaY, areaW, areaH);
        } else if (latestPulls.isEmpty()) {
            String hint = "点击下方【单次跃迁】或【十连跃迁】招募新队友、武器与高阶芯片！";
            font.draw(ps, new TextComponent(hint), this.width / 2 - font.width(hint) / 2, areaY + areaH / 2 - 4, 0xFFE2E8F0);
        } else {
            renderSummaryGrid(ps, areaX, areaY, areaW, areaH);
        }

        super.render(ps, mouseX, mouseY, partialTicks);
        SystemToastManager.render(ps, this.font, this.width, this.height);
    }

    private void renderWarpAnimation(PoseStack ps, float partialTicks) {
        GuiComponent.fill(ps, 0, 0, this.width, this.height, 0xFF04070E);
        float t = (animTicks + partialTicks) / 52.0f;
        int cx = this.width / 2;
        int cy = this.height / 2;
        int streamCol = EquipmentStatsHelper.getRarityColor(maxBatchRarity);

        for (int i = 0; i < starX.length; i++) {
            float progress = (t * (1.2f + starSpeed[i] * 10.0f)) % 1.0f;
            int sx = cx + (int) (starX[i] * this.width * 0.65f * progress);
            int sy = cy + (int) (starY[i] * this.height * 0.65f * progress);
            int tx = cx + (int) (starX[i] * this.width * 0.65f * Math.max(0f, progress - 0.12f));
            int ty = cy + (int) (starY[i] * this.height * 0.65f * Math.max(0f, progress - 0.12f));
            GuiComponent.fill(ps, Math.min(sx, tx), Math.min(sy, ty), Math.max(sx, tx) + 2, Math.max(sy, ty) + 2, 0xAA94A3B8);
        }

        float beamT = Math.min(1.0f, t * 1.45f);
        int bx = (int) (this.width * 0.15f + (cx - this.width * 0.15f) * beamT);
        int by = (int) (this.height * 0.15f + (cy - this.height * 0.15f) * beamT);
        for (int tail = 0; tail < 14; tail++) {
            float bt = Math.max(0f, beamT - tail * 0.03f);
            int rx = (int) (this.width * 0.15f + (cx - this.width * 0.15f) * bt);
            int ry = (int) (this.height * 0.15f + (cy - this.height * 0.15f) * bt);
            int size = Math.max(2, 10 - tail / 2);
            GuiComponent.fill(ps, rx - size, ry - size, rx + size, ry + size, streamCol);
        }
        GuiComponent.fill(ps, bx - 10, by - 10, bx + 10, by + 10, 0xFFFFFFFF);

        if (t > 0.72f) {
            float flash = Math.min(1.0f, (t - 0.72f) / 0.28f);
            int radius = (int) (flash * Math.max(this.width, this.height) * 0.45f);
            GuiComponent.fill(ps, cx - radius, cy - radius, cx + radius, cy + radius, (0x88 << 24) | (streamCol & 0x00FFFFFF));
        }

        String txt = maxBatchRarity >= 5 ? "★ 金色流星信号接近中..." : (maxBatchRarity == 4 ? "◆ 紫色跃迁光束..." : "◇ 建立星海连接中...");
        font.drawShadow(ps, new TextComponent(txt), cx - font.width(txt) / 2.0f, this.height - 46, streamCol);
    }

    private void renderSingleRevealCard(PoseStack ps, ItemStack stack, int areaX, int areaY, int areaW, int areaH) {
        int rarity = getPullRarity(stack);
        int col = EquipmentStatsHelper.getRarityColor(rarity);
        int cw = Math.min(180, areaW - 20);
        int ch = Math.min(126, areaH - 12);
        int cx = areaX + (areaW - cw) / 2;
        int cy = areaY + (areaH - ch) / 2;

        GenshinHudOverlay.drawVoxelPanel(ps, cx, cy, cw, ch, 0xE80F1826, col, col);

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("WishCharId")) {
            String charId = tag.getString("WishCharId");
            GenshinHudOverlay.drawPortrait(ps, TurnBasedBattleScreen.getPortraitForChar(charId), cx + cw / 2 - 22, cy + 12, 44, 44);
            String sub = tag.getString("WishSubLabel");
            font.draw(ps, new TextComponent(sub), cx + (cw - font.width(sub)) / 2, cy + 62, 0xFF7DD3FC);
        } else {
            ps.pushPose();
            ps.translate(cx + cw / 2.0f - 16, cy + 20, 0);
            ps.scale(2.0f, 2.0f, 1.0f);
            this.itemRenderer.renderGuiItem(stack, 0, 0);
            ps.popPose();
        }

        String stars = EquipmentStatsHelper.getStarString(rarity);
        font.drawShadow(ps, new TextComponent(stars), cx + (cw - font.width(stars)) / 2.0f, cy + 78, col);

        String name = stack.getHoverName().getString();
        font.drawShadow(ps, new TextComponent(name), cx + (cw - font.width(name)) / 2.0f, cy + 92, 0xFFF8FAFC);

        String counter = (revealIdx + 1) + " / " + latestPulls.size();
        font.draw(ps, new TextComponent(counter), cx + (cw - font.width(counter)) / 2, cy + ch - 14, 0xFF94A3B8);
    }

    private void renderSummaryGrid(PoseStack ps, int areaX, int areaY, int areaW, int areaH) {
        int cols = Math.min(5, latestPulls.size());
        int cardW = Math.min(76, (areaW - 36) / Math.max(1, cols));
        int cardH = Math.min(58, (areaH - 22) / 2);
        int startX = areaX + (areaW - cols * (cardW + 6) + 6) / 2;
        int startY = areaY + Math.max(8, (areaH - cardH * 2 - 6) / 2);

        for (int i = 0; i < latestPulls.size(); i++) {
            ItemStack stack = latestPulls.get(i);
            int r = i / 5;
            int c = i % 5;
            int x = startX + c * (cardW + 6);
            int y = startY + r * (cardH + 6);
            int rarity = getPullRarity(stack);
            int col = EquipmentStatsHelper.getRarityColor(rarity);

            GenshinHudOverlay.drawVoxelPanel(ps, x, y, cardW, cardH, 0xD0121E2C, col, col);
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("WishCharId")) {
                GenshinHudOverlay.drawPortrait(ps, TurnBasedBattleScreen.getPortraitForChar(tag.getString("WishCharId")),
                        x + cardW / 2 - 11, y + 4, 22, 22);
            } else {
                this.itemRenderer.renderGuiItem(stack, x + cardW / 2 - 8, y + 6);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, x + cardW / 2 - 8, y + 6);
            }

            String stars = EquipmentStatsHelper.getStarString(rarity);
            font.draw(ps, new TextComponent(stars), x + (cardW - font.width(stars)) / 2, y + 29, col);

            String name = stack.getHoverName().getString().replaceAll("【.*?】", "");
            while (font.width(name) > cardW - 6 && name.length() > 2) {
                name = name.substring(0, name.length() - 1);
            }
            font.draw(ps, new TextComponent(name), x + (cardW - font.width(name)) / 2, y + 41, 0xFFEEEEEE);
        }
    }
}
