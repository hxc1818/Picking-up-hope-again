package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.account.AccountSessionManager;
import com.resilienthope.core.network.PacketCloudSyncRequest;
import com.resilienthope.core.network.PacketHandler;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Custom pause screen replacing vanilla PauseScreen.
 * Exposes cloud save status, manual sync trigger, options, and return-to-login action.
 */
public class HopePauseScreen extends Screen {
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");
    private String pendingNotice = "";
    private String statusSnapshotAtClick = "";
    private long tsSnapshotAtClick = 0L;

    public HopePauseScreen() {
        super(new TextComponent("游戏菜单"));
    }

    @Override
    public boolean isPauseScreen() { return true; }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        boolean creative = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative();
        int boxH = creative ? 198 : 156;
        int by = (this.height - boxH) / 2;
        int panelW = 196;
        int x = (this.width - panelW) / 2;
        int y = by + 34;

        addRenderableWidget(new SciFiButton(x, y, panelW, 20,
                new TextComponent("继续游戏"), btn -> onClose(), 0xFFFBBF24));

        int curY = y + 24;
        if (creative) {
            addRenderableWidget(new SciFiButton(x, curY, panelW, 19,
                    new TextComponent("打开双维度蓝图与传送 (M)"), btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new BuilderRegionMapScreen());
                }
            }, 0xFF4ADE80));
            curY += 22;

            int hw = (panelW - 4) / 2;
            addRenderableWidget(new SciFiButton(x, curY, hw, 18,
                    new TextComponent("飞船(Starship)"), btn -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.chat("/hope map starship");
                    onClose();
                }
            }, 0xFF38BDF8));
            addRenderableWidget(new SciFiButton(x + hw + 4, curY, hw, 18,
                    new TextComponent("星球(Planet)"), btn -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.chat("/hope map planet");
                    onClose();
                }
            }, 0xFFA5F3FC));
            curY += 22;
        } else {
            addRenderableWidget(new SciFiButton(x, curY, panelW, 20,
                    new TextComponent("立即同步云存档"), btn -> {
                statusSnapshotAtClick = AccountSessionManager.getSyncStatusText();
                tsSnapshotAtClick = AccountSessionManager.getLastSyncAt();
                pendingNotice = "正在上传进度至云端...";
                PacketHandler.INSTANCE.sendToServer(new PacketCloudSyncRequest());
            }, 0xFF38BDF8));
            curY += 24;
        }

        addRenderableWidget(new SciFiButton(x, curY, panelW, 20,
                new TextComponent("游戏设置"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
            }
        }, 0xFF64748B));

        addRenderableWidget(new SciFiButton(x, curY + 24, panelW, 20,
                new TextComponent("保存并返回登录页"), btn -> saveAndReturnToLogin(), 0xFFEF4444));
    }

    private void saveAndReturnToLogin() {
        if (this.minecraft == null) return;
        boolean singleplayer = this.minecraft.isLocalServer();
        if (this.minecraft.level != null) {
            this.minecraft.level.disconnect();
        }
        if (singleplayer) {
            this.minecraft.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("menu.savingLevel")));
        } else {
            this.minecraft.clearLevel();
        }
        this.minecraft.setScreen(new HopeTitleScreen());
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ps);

        boolean creative = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative();
        int boxW = 228;
        int boxH = creative ? 198 : 156;
        int bx = (this.width - boxW) / 2;
        int by = (this.height - boxH) / 2;

        GenshinHudOverlay.drawVoxelPanel(ps, bx, by, boxW, boxH, 0xE20A121E, 0xFF284664, 0xFFFBBF24);

        String title = "《重拾希望》 · 暂停菜单";
        font.drawShadow(ps, new TextComponent(title), (this.width - font.width(title)) / 2.0f, by + 8, 0xFFF8FAFC);

        String accName = AccountSessionManager.isLoggedIn() ? AccountSessionManager.getDisplayName() : "本地玩家";
        String mode = AccountSessionManager.isOfflineMode() ? "离线模式" : "云端账号";
        String accLine = "账号：" + accName + " [" + mode + "]";
        font.draw(ps, new TextComponent(accLine), (this.width - font.width(accLine)) / 2.0f, by + 20, 0xFF7DD3FC);

        String currentStatus = AccountSessionManager.getSyncStatusText();
        long lastTs = AccountSessionManager.getLastSyncAt();
        if (!pendingNotice.isEmpty() && (!currentStatus.equals(statusSnapshotAtClick) || lastTs != tsSnapshotAtClick)) {
            pendingNotice = "";
        }

        String syncText = !pendingNotice.isEmpty()
                ? pendingNotice
                : currentStatus + (lastTs > 0 ? " (" + TIME_FMT.format(new Date(lastTs)) + ")" : "");
        while (font.width(syncText) > boxW - 16 && syncText.length() > 8) {
            syncText = syncText.substring(0, syncText.length() - 1);
        }
        font.draw(ps, new TextComponent(syncText), (this.width - font.width(syncText)) / 2.0f, by + boxH - 16, 0xFF94A3B8);

        super.render(ps, mouseX, mouseY, partialTicks);
    }
}
