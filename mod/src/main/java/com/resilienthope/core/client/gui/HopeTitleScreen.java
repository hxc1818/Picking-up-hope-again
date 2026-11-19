package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.account.AccountConfig;
import com.resilienthope.core.account.AccountSessionManager;
import com.resilienthope.core.account.CloudSaveSnapshot;
import com.resilienthope.core.client.SingleplayerWorldLauncher;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Custom singleplayer RPG login and launch screen replacing vanilla TitleScreen.
 * Hides multiplayer/realms/world-list clutter and launches per-account isolated saves.
 */
public class HopeTitleScreen extends Screen {
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("MM-dd HH:mm");

    private boolean registerMode = false;
    private boolean showServerConfig = false;
    private boolean busy = false;
    private int bgTicks = 0;

    private String usernameText = "";
    private String passwordText = "";
    private String nicknameText = "";
    private String serverUrlText = AccountConfig.DEFAULT_SERVER_URL;
    private String statusMessage = "";
    private int statusColor = 0xFF94A3B8;

    private boolean lastLoggedInState = false;

    private EditBox usernameBox;
    private EditBox passwordBox;
    private EditBox nicknameBox;
    private EditBox serverUrlBox;

    private SciFiButton primaryActionBtn;
    private SciFiButton enterGameBtn;

    private int cardX, cardY, cardW, cardH;

    public HopeTitleScreen() {
        super(new TextComponent("重拾希望 · 启动器"));
        AccountConfig cfg = AccountSessionManager.getConfig();
        this.usernameText = cfg.lastUsername != null ? cfg.lastUsername : "";
        this.serverUrlText = cfg.serverUrl != null ? cfg.serverUrl : AccountConfig.DEFAULT_SERVER_URL;
        this.lastLoggedInState = AccountSessionManager.isLoggedIn();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        lastLoggedInState = AccountSessionManager.isLoggedIn();

        this.cardW = Math.min(286, this.width - 24);
        this.cardH = Math.min(168, Math.max(146, this.height - 76));
        this.cardX = (this.width - this.cardW) / 2;
        this.cardY = Math.max(40, (this.height - this.cardH) / 2 + 2);

        // Top-right refresh connection button
        addRenderableWidget(new SciFiButton(this.width - 50, 6, 42, 16,
                new TextComponent("刷新"), btn -> {
            statusMessage = "正在检测云端连接...";
            statusColor = 0xFF38BDF8;
            AccountSessionManager.checkHealthAsync().thenAccept(ok -> {
                if (this.minecraft != null) {
                    this.minecraft.execute(() -> {
                        statusMessage = ok ? "云端测试服在线" : "云端未连接（可离线游玩）";
                        statusColor = ok ? 0xFF4ADE80 : 0xFFFBBF24;
                    });
                }
            });
        }, 0xFF38BDF8));

        if (showServerConfig) {
            buildServerConfigWidgets();
        } else if (AccountSessionManager.isLoggedIn()) {
            buildLoggedInWidgets();
        } else {
            buildAuthFormWidgets();
        }

        // Bottom footer: Builder Workshop, Options & Quit
        int footY = this.height - 24;
        int footW = 84;
        int gap = 6;
        int startFootX = (this.width - (footW * 3 + gap * 2)) / 2;
        addRenderableWidget(new SciFiButton(startFootX, footY, footW, 18,
                new TextComponent("地图造景工坊"),
                btn -> launchBuilderWorkshop(), 0xFF4ADE80));
        addRenderableWidget(new SciFiButton(startFootX + footW + gap, footY, footW, 18,
                new TextComponent("游戏设置"),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
                    }
                }, 0xFF64748B));
        addRenderableWidget(new SciFiButton(startFootX + (footW + gap) * 2, footY, footW, 18,
                new TextComponent("退出游戏"),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.stop();
                    }
                }, 0xFFEF4444));
    }

    private void buildAuthFormWidgets() {
        int innerX = cardX + 12;
        int innerW = cardW - 24;
        int y = cardY + 8;

        // Mode switch tabs
        int tabW = (innerW - 6) / 2;
        addRenderableWidget(new SciFiButton(innerX, y, tabW, 16,
                new TextComponent("登录账号"), btn -> {
            if (registerMode) {
                registerMode = false;
                statusMessage = "";
                init();
            }
        }, !registerMode ? 0xFFFBBF24 : 0xFF334155));

        addRenderableWidget(new SciFiButton(innerX + tabW + 6, y, tabW, 16,
                new TextComponent("注册新账号"), btn -> {
            if (!registerMode) {
                registerMode = true;
                statusMessage = "";
                init();
            }
        }, registerMode ? 0xFF38BDF8 : 0xFF334155));

        y += 22;
        int labelW = 46;
        int boxX = innerX + labelW;
        int boxW = innerW - labelW;

        usernameBox = new EditBox(this.font, boxX, y, boxW, 15, new TextComponent("用户名"));
        usernameBox.setMaxLength(24);
        usernameBox.setValue(usernameText);
        usernameBox.setResponder(s -> usernameText = s);
        addRenderableWidget(usernameBox);
        y += 20;

        passwordBox = new EditBox(this.font, boxX, y, boxW, 15, new TextComponent("密码"));
        passwordBox.setMaxLength(64);
        passwordBox.setValue(passwordText);
        passwordBox.setResponder(s -> passwordText = s);
        passwordBox.setFormatter((txt, idx) -> FormattedCharSequence.forward("*".repeat(txt.length()), Style.EMPTY));
        addRenderableWidget(passwordBox);
        y += 20;

        if (registerMode) {
            nicknameBox = new EditBox(this.font, boxX, y, boxW, 15, new TextComponent("昵称"));
            nicknameBox.setMaxLength(24);
            nicknameBox.setValue(nicknameText);
            nicknameBox.setResponder(s -> nicknameText = s);
            addRenderableWidget(nicknameBox);
            y += 20;
        } else {
            nicknameBox = null;
        }

        // Remember toggle & Server config toggle
        boolean remember = AccountSessionManager.getConfig().rememberLogin;
        addRenderableWidget(new SciFiButton(innerX, y, tabW, 15,
                new TextComponent("记住登录: " + (remember ? "开" : "关")), btn -> {
            AccountSessionManager.setRememberLogin(!AccountSessionManager.getConfig().rememberLogin);
            init();
        }, remember ? 0xFF4ADE80 : 0xFF64748B));

        addRenderableWidget(new SciFiButton(innerX + tabW + 6, y, tabW, 15,
                new TextComponent("服务器设置"), btn -> {
            serverUrlText = AccountSessionManager.getConfig().serverUrl;
            showServerConfig = true;
            statusMessage = "";
            init();
        }, 0xFF38BDF8));

        y += 20;
        int mainBtnW = (innerW - 6) * 3 / 5;
        int offBtnW = innerW - 6 - mainBtnW;

        primaryActionBtn = addRenderableWidget(new SciFiButton(innerX, y, mainBtnW, 19,
                new TextComponent(registerMode ? "注册并登录" : "登录账号"),
                btn -> submitAuth(), 0xFFFBBF24));
        primaryActionBtn.active = !busy;

        addRenderableWidget(new SciFiButton(innerX + mainBtnW + 6, y, offBtnW, 19,
                new TextComponent("离线游玩"), btn -> {
            AccountSessionManager.loginOffline(usernameText);
            statusMessage = "";
            init();
        }, 0xFF64748B));

        // Focus username or password automatically
        if (usernameText.isEmpty()) {
            this.setFocused(usernameBox);
            usernameBox.setFocus(true);
        } else {
            this.setFocused(passwordBox);
            passwordBox.setFocus(true);
        }
    }

    private void buildServerConfigWidgets() {
        int innerX = cardX + 12;
        int innerW = cardW - 24;
        int y = cardY + 34;

        serverUrlBox = new EditBox(this.font, innerX, y, innerW, 16, new TextComponent("服务器地址"));
        serverUrlBox.setMaxLength(180);
        serverUrlBox.setValue(serverUrlText);
        serverUrlBox.setResponder(s -> serverUrlText = s);
        addRenderableWidget(serverUrlBox);

        y += 26;
        int halfW = (innerW - 6) / 2;
        addRenderableWidget(new SciFiButton(innerX, y, halfW, 18,
                new TextComponent("保存并检测"), btn -> saveServerUrl(), 0xFF4ADE80));
        addRenderableWidget(new SciFiButton(innerX + halfW + 6, y, halfW, 18,
                new TextComponent("恢复默认"), btn -> {
            serverUrlText = AccountConfig.DEFAULT_SERVER_URL;
            if (serverUrlBox != null) serverUrlBox.setValue(serverUrlText);
        }, 0xFF38BDF8));

        y += 24;
        addRenderableWidget(new SciFiButton(innerX, y, innerW, 18,
                new TextComponent("返回登录界面"), btn -> {
            showServerConfig = false;
            statusMessage = "";
            init();
        }, 0xFF64748B));

        this.setFocused(serverUrlBox);
        serverUrlBox.setFocus(true);
    }

    private void buildLoggedInWidgets() {
        int innerX = cardX + 12;
        int innerW = cardW - 24;
        int btnY = cardY + cardH - 56;

        enterGameBtn = addRenderableWidget(new SciFiButton(innerX, btnY, innerW, 19,
                new TextComponent(busy ? "正在同步云存档..." : "进入游戏"),
                btn -> launchSingleplayerGame(), 0xFFFBBF24));
        enterGameBtn.active = !busy;

        int subY = btnY + 22;
        String folderName = AccountSessionManager.getWorldFolderNameForActiveAccount();
        boolean canImportLegacy = this.minecraft != null
                && !AccountSessionManager.hasAnyLocalOrCloudSave()
                && !SingleplayerWorldLauncher.worldExists(this.minecraft, folderName)
                && SingleplayerWorldLauncher.hasLegacyWorld(this.minecraft);

        if (canImportLegacy) {
            int w3 = (innerW - 8) / 3;
            addRenderableWidget(new SciFiButton(innerX, subY, w3, 15,
                    new TextComponent("导入旧存档"), btn -> importLegacySave(), 0xFF4ADE80));
            addRenderableWidget(new SciFiButton(innerX + w3 + 4, subY, w3, 15,
                    new TextComponent("服务器设置"), btn -> {
                serverUrlText = AccountSessionManager.getConfig().serverUrl;
                showServerConfig = true;
                init();
            }, 0xFF38BDF8));
            addRenderableWidget(new SciFiButton(innerX + (w3 + 4) * 2, subY, w3, 15,
                    new TextComponent("切换账号"), btn -> switchAccount(), 0xFFEF4444));
        } else {
            int w2 = (innerW - 6) / 2;
            addRenderableWidget(new SciFiButton(innerX, subY, w2, 15,
                    new TextComponent("服务器设置"), btn -> {
                serverUrlText = AccountSessionManager.getConfig().serverUrl;
                showServerConfig = true;
                init();
            }, 0xFF38BDF8));
            addRenderableWidget(new SciFiButton(innerX + w2 + 6, subY, w2, 15,
                    new TextComponent("切换账号"), btn -> switchAccount(), 0xFFEF4444));
        }
    }

    private void submitAuth() {
        if (busy) return;
        String user = usernameText.trim();
        String pass = passwordText;
        if (user.length() < 3) {
            statusMessage = "用户名至少需要 3 个字符";
            statusColor = 0xFFF87171;
            return;
        }
        if (pass.length() < 4) {
            statusMessage = "密码至少需要 4 个字符";
            statusColor = 0xFFF87171;
            return;
        }

        busy = true;
        if (primaryActionBtn != null) primaryActionBtn.active = false;
        statusMessage = registerMode ? "正在注册账号..." : "正在登录验证...";
        statusColor = 0xFF38BDF8;

        var future = registerMode
                ? AccountSessionManager.registerAsync(user, pass, nicknameText)
                : AccountSessionManager.loginAsync(user, pass);

        future.whenComplete((res, err) -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    busy = false;
                    if (err != null) {
                        statusMessage = "请求异常：" + err.getMessage();
                        statusColor = 0xFFF87171;
                        if (primaryActionBtn != null) primaryActionBtn.active = true;
                        return;
                    }
                    if (res.ok()) {
                        passwordText = "";
                        statusMessage = "";
                        init();
                    } else {
                        statusMessage = res.message();
                        statusColor = 0xFFF87171;
                        if (primaryActionBtn != null) primaryActionBtn.active = true;
                    }
                });
            }
        });
    }

    private void saveServerUrl() {
        AccountSessionManager.setServerUrl(serverUrlText);
        serverUrlText = AccountSessionManager.getConfig().serverUrl;
        statusMessage = "已保存地址，正在检测连通性...";
        statusColor = 0xFF38BDF8;
        AccountSessionManager.checkHealthAsync().thenAccept(ok -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    statusMessage = ok ? "连接成功！测试服在线" : "无法连接该地址，请确认服务已启动";
                    statusColor = ok ? 0xFF4ADE80 : 0xFFFBBF24;
                });
            }
        });
    }

    private void switchAccount() {
        if (busy) return;
        AccountSessionManager.logoutAsync();
        statusMessage = "已退出当前账号";
        statusColor = 0xFF94A3B8;
        init();
    }

    private void importLegacySave() {
        if (this.minecraft == null || busy) return;
        try {
            String folder = AccountSessionManager.getWorldFolderNameForActiveAccount();
            String title = "《重拾希望》- " + AccountSessionManager.getDisplayName();
            SingleplayerWorldLauncher.importLegacyWorldIntoAccount(this.minecraft, folder, title);
            statusMessage = "旧存档已导入！点击【进入游戏】即可继续冒险";
            statusColor = 0xFF4ADE80;
            init();
        } catch (Exception e) {
            PickingHopeCore.LOGGER.error("Failed to import legacy world", e);
            statusMessage = "导入失败：" + e.getMessage();
            statusColor = 0xFFF87171;
        }
    }

    private void launchBuilderWorkshop() {
        if (this.minecraft == null || busy) return;
        busy = true;
        statusMessage = "正在进入【地图造景工坊】创造存档...";
        statusColor = 0xFF4ADE80;
        try {
            SingleplayerWorldLauncher.ensureAndLaunchBuilderWorld(this.minecraft);
        } catch (Exception e) {
            PickingHopeCore.LOGGER.error("Failed to launch builder workshop", e);
            busy = false;
            statusMessage = "打开造景工坊失败：" + e.getMessage();
            statusColor = 0xFFF87171;
            init();
        }
    }

    private void launchSingleplayerGame() {
        if (this.minecraft == null || busy) return;
        busy = true;
        if (enterGameBtn != null) {
            enterGameBtn.active = false;
            enterGameBtn.setMessage(new TextComponent("正在拉取云存档..."));
        }
        statusMessage = "正在同步账号进度...";
        statusColor = 0xFF38BDF8;

        AccountSessionManager.pullLatestBeforeEnterWorld().whenComplete((snap, err) -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    try {
                        String folder = AccountSessionManager.getWorldFolderNameForActiveAccount();
                        String title = "《重拾希望》- " + AccountSessionManager.getDisplayName();
                        SingleplayerWorldLauncher.ensureAndLaunchWorld(this.minecraft, folder, title);
                    } catch (Exception e) {
                        PickingHopeCore.LOGGER.error("Failed to launch singleplayer world", e);
                        busy = false;
                        statusMessage = "启动单人世界失败：" + e.getMessage();
                        statusColor = 0xFFF87171;
                        init();
                    }
                });
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        bgTicks++;
        if (usernameBox != null) usernameBox.tick();
        if (passwordBox != null) passwordBox.tick();
        if (nicknameBox != null) nicknameBox.tick();
        if (serverUrlBox != null) serverUrlBox.tick();

        boolean loggedInNow = AccountSessionManager.isLoggedIn();
        if (loggedInNow != lastLoggedInState && !showServerConfig) {
            init();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        var focused = this.getFocused();
        if (usernameBox != null) usernameBox.setFocus(focused == usernameBox);
        if (passwordBox != null) passwordBox.setFocus(focused == passwordBox);
        if (nicknameBox != null) nicknameBox.setFocus(focused == nicknameBox);
        if (serverUrlBox != null) serverUrlBox.setFocus(focused == serverUrlBox);
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (showServerConfig) {
                saveServerUrl();
                return true;
            }
            if (!AccountSessionManager.isLoggedIn()) {
                submitAuth();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_TAB && !showServerConfig && !AccountSessionManager.isLoggedIn()) {
            cycleFocus();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cycleFocus() {
        if (usernameBox != null && usernameBox.isFocused()) {
            usernameBox.setFocus(false);
            if (passwordBox != null) {
                this.setFocused(passwordBox);
                passwordBox.setFocus(true);
            }
        } else if (passwordBox != null && passwordBox.isFocused()) {
            passwordBox.setFocus(false);
            if (nicknameBox != null) {
                this.setFocused(nicknameBox);
                nicknameBox.setFocus(true);
            } else if (usernameBox != null) {
                this.setFocused(usernameBox);
                usernameBox.setFocus(true);
            }
        } else {
            if (nicknameBox != null) nicknameBox.setFocus(false);
            if (usernameBox != null) {
                this.setFocused(usernameBox);
                usernameBox.setFocus(true);
            }
        }
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        renderSciFiBackdrop(ps);

        // Top-right cloud status pill
        boolean reachable = AccountSessionManager.isServerReachable();
        String badge = reachable ? "● 云端测试服在线" : "● 本地离线可用";
        int badgeCol = reachable ? 0xFF4ADE80 : 0xFFFBBF24;
        int badgeW = font.width(badge) + 12;
        GenshinHudOverlay.drawVoxelPanel(ps, this.width - badgeW - 54, 6, badgeW, 16, 0xCC0A1320, 0xFF1E3A5F, badgeCol);
        font.draw(ps, new TextComponent(badge), this.width - badgeW - 48, 10, badgeCol);

        // Title Banner
        String title = "重拾希望 · PICKING UP HOPE AGAIN";
        String sub = "单人科幻回合制冒险  |  v3.0";
        font.drawShadow(ps, new TextComponent(title), (this.width - font.width(title)) / 2.0f, 12, 0xFFF8FAFC);
        font.draw(ps, new TextComponent(sub), (this.width - font.width(sub)) / 2.0f, 24, 0xFF7DD3FC);

        // Central Panel Card
        GenshinHudOverlay.drawVoxelPanel(ps, cardX, cardY, cardW, cardH, 0xDC0A121E, 0xFF284664, 0xFFFBBF24);

        if (showServerConfig) {
            renderServerConfigLabels(ps);
        } else if (AccountSessionManager.isLoggedIn()) {
            renderLoggedInInfo(ps);
        } else {
            renderAuthLabels(ps);
        }

        // Status / feedback line
        if (statusMessage != null && !statusMessage.isEmpty()) {
            int msgY = cardY + cardH - 13;
            String trimmed = statusMessage;
            while (font.width(trimmed) > cardW - 16 && trimmed.length() > 6) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            font.draw(ps, new TextComponent(trimmed), cardX + (cardW - font.width(trimmed)) / 2.0f, msgY, statusColor);
        }

        super.render(ps, mouseX, mouseY, partialTicks);
    }

    private void renderAuthLabels(PoseStack ps) {
        int innerX = cardX + 12;
        int y = cardY + 34;
        font.draw(ps, new TextComponent("账号名:"), innerX, y, 0xFFCBD5E1);
        font.draw(ps, new TextComponent("密  码:"), innerX, y + 20, 0xFFCBD5E1);
        if (registerMode) {
            font.draw(ps, new TextComponent("昵  称:"), innerX, y + 40, 0xFFCBD5E1);
        }
    }

    private void renderServerConfigLabels(PoseStack ps) {
        int innerX = cardX + 12;
        font.drawShadow(ps, new TextComponent("云存档服务器配置"), innerX, cardY + 10, 0xFFFBBF24);
        font.draw(ps, new TextComponent("服务地址 (默认 http://127.0.0.1:3847):"), innerX, cardY + 23, 0xFF94A3B8);
    }

    private void renderLoggedInInfo(PoseStack ps) {
        int innerX = cardX + 12;
        int innerW = cardW - 24;
        int y = cardY + 10;

        String nameLine = "当前账号：" + AccountSessionManager.getDisplayName();
        font.drawShadow(ps, new TextComponent(nameLine), innerX, y, 0xFFF8FAFC);

        String modeTag = AccountSessionManager.isOfflineMode() ? "[本地离线]" : "[云端账号]";
        int modeCol = AccountSessionManager.isOfflineMode() ? 0xFFFBBF24 : 0xFF4ADE80;
        font.draw(ps, new TextComponent(modeTag), innerX + innerW - font.width(modeTag), y, modeCol);

        y += 14;
        GenshinHudOverlay.drawVoxelPanel(ps, innerX, y, innerW, 56, 0xB80E1A2A, 0xFF1E3A5F, 0xFF38BDF8);

        CloudSaveSnapshot.Summary sum = AccountSessionManager.getActiveSummaryForDisplay();
        if (sum != null) {
            String stageLine = "主线：第 " + sum.questStage + " 阶段 · " + (sum.questTitle != null ? sum.questTitle : "");
            while (font.width(stageLine) > innerW - 12 && stageLine.length() > 8) {
                stageLine = stageLine.substring(0, stageLine.length() - 1);
            }
            font.drawShadow(ps, new TextComponent(stageLine), innerX + 6, y + 6, 0xFFFBBF24);

            String lvLine = "开拓 Lv." + sum.playerLevel + "  |  世界等级 " + sum.worldLevel + "  |  抽卡券 x" + sum.starTicketsCount;
            font.draw(ps, new TextComponent(lvLine), innerX + 6, y + 19, 0xFFE2E8F0);

            List<String> names = sum.activePartyNames;
            String partyStr = (names != null && !names.isEmpty()) ? String.join(" / ", names) : "检修员·林野";
            String partyLine = "编队：" + partyStr;
            while (font.width(partyLine) > innerW - 12 && partyLine.length() > 8) {
                partyLine = partyLine.substring(0, partyLine.length() - 1);
            }
            font.draw(ps, new TextComponent(partyLine), innerX + 6, y + 32, 0xFF94A3B8);
        } else {
            font.drawShadow(ps, new TextComponent("全新单人存档（未开荒）"), innerX + 6, y + 10, 0xFF7DD3FC);
            font.draw(ps, new TextComponent("进入后将从《重拾希望号》第 0 阶段开始旅程"), innerX + 6, y + 25, 0xFF94A3B8);
        }

        long syncTs = AccountSessionManager.getLastSyncAt();
        String syncStr = AccountSessionManager.getSyncStatusText()
                + (syncTs > 0L ? " (" + TIME_FMT.format(new Date(syncTs)) + ")" : "");
        while (font.width(syncStr) > innerW - 12 && syncStr.length() > 8) {
            syncStr = syncStr.substring(0, syncStr.length() - 1);
        }
        font.draw(ps, new TextComponent(syncStr), innerX + 6, y + 44, 0xFF64748B);
    }

    private void renderSciFiBackdrop(PoseStack ps) {
        fillGradient(ps, 0, 0, this.width, this.height, 0xFF040811, 0xFF0D1B2E);

        // Subtle starfield drift
        for (int i = 0; i < 42; i++) {
            int seed = i * 7919 + 131;
            int sx = ((seed & 0xFFFF) + (bgTicks / (2 + (i % 3)))) % Math.max(1, this.width);
            int sy = ((seed >> 4) * 37) % Math.max(1, this.height);
            int alpha = (i % 3 == 0) ? 0xCC : 0x66;
            int col = (alpha << 24) | ((i % 4 == 0) ? 0x7DD3FC : 0xE2E8F0);
            fill(ps, sx, sy, sx + (i % 5 == 0 ? 2 : 1), sy + 1, col);
        }

        // Perspective horizon grid lines near bottom
        int horizonY = this.height * 2 / 3;
        fill(ps, 0, horizonY, this.width, horizonY + 1, 0x3038BDF8);
        for (int gx = 0; gx < this.width; gx += 48) {
            fill(ps, gx, horizonY, gx + 1, this.height, 0x1838BDF8);
        }
    }
}
