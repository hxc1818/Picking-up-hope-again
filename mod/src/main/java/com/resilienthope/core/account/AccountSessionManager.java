package com.resilienthope.core.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.network.PacketCloudSyncAck;
import com.resilienthope.core.network.PacketHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe manager for singleplayer account login, session persistence,
 * local profile caching, and asynchronous cloud save synchronization.
 * Common-safe: never references client-only Minecraft classes.
 */
public class AccountSessionManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final ScheduledExecutorService EXEC = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "PickingHope-CloudSync");
        t.setDaemon(true);
        return t;
    });

    public record AuthResult(boolean ok, String message) {}
    public record SyncResult(boolean ok, String message, long syncedAt) {}

    private static volatile AccountConfig config = new AccountConfig();
    private static volatile boolean initialized = false;

    private static volatile String accountId = null;
    private static volatile String username = "";
    private static volatile String displayName = "";
    private static volatile String authToken = "";
    private static volatile boolean offlineMode = false;

    private static volatile boolean serverReachable = false;
    private static volatile boolean hasCloudSave = false;
    private static volatile CloudSaveSnapshot.Summary cloudSaveSummary = null;
    private static volatile String syncStatusText = "待检测云端";
    private static volatile long lastSyncAt = 0L;

    private static final Object LOCK = new Object();
    private static CloudSaveSnapshot pendingRestoreSnapshot = null;
    private static CloudSaveSnapshot queuedPushSnapshot = null;
    private static ScheduledFuture<?> queuedPushTask = null;

    public static void initOnStartup() {
        synchronized (LOCK) {
            if (initialized) return;
            initialized = true;
            config = AccountConfig.load();
            if (config.rememberLogin && !config.savedAccountId.isEmpty()) {
                if (config.offlineMode) {
                    accountId = config.savedAccountId;
                    username = config.lastUsername.isEmpty() ? config.savedAccountId : config.lastUsername;
                    displayName = config.savedDisplayName.isEmpty() ? username : config.savedDisplayName;
                    authToken = "";
                    offlineMode = true;
                    syncStatusText = "离线本地账号";
                    loadLocalSummaryIfPresent(accountId);
                } else if (!config.savedToken.isEmpty()) {
                    accountId = config.savedAccountId;
                    username = config.lastUsername;
                    displayName = config.savedDisplayName.isEmpty() ? username : config.savedDisplayName;
                    authToken = config.savedToken;
                    offlineMode = false;
                    syncStatusText = "正在验证云端会话...";
                    loadLocalSummaryIfPresent(accountId);
                }
            }
        }

        checkHealthAsync().thenAccept(reachable -> {
            if (reachable && !offlineMode && authToken != null && !authToken.isEmpty()) {
                verifySavedTokenInternal();
            }
        });
    }

    private static void ensureLoaded() {
        if (!initialized) {
            initOnStartup();
        }
    }

    public static AccountConfig getConfig() {
        ensureLoaded();
        return config;
    }

    public static boolean isLoggedIn() {
        ensureLoaded();
        return accountId != null && !accountId.isEmpty();
    }

    public static String getAccountId() { return accountId != null ? accountId : ""; }
    public static String getUsername() { return username != null ? username : ""; }
    public static String getDisplayName() { return (displayName != null && !displayName.isEmpty()) ? displayName : getUsername(); }
    public static boolean isOfflineMode() { return offlineMode; }
    public static boolean isServerReachable() { return serverReachable; }
    public static String getSyncStatusText() { return syncStatusText; }
    public static long getLastSyncAt() { return lastSyncAt; }

    public static CloudSaveSnapshot.Summary getActiveSummaryForDisplay() {
        if (cloudSaveSummary != null) return cloudSaveSummary;
        if (accountId != null && !accountId.isEmpty()) {
            CloudSaveSnapshot local = readLocalSnapshot(accountId);
            if (local != null && local.summary != null) {
                return local.summary;
            }
        }
        return null;
    }

    public static boolean hasAnyLocalOrCloudSave() {
        if (hasCloudSave) return true;
        if (accountId == null || accountId.isEmpty()) return false;
        return Files.exists(getLocalProfilePath(accountId));
    }

    public static String getWorldFolderNameForActiveAccount() {
        String id = sanitizeId(accountId);
        if (id.isEmpty()) id = "guest";
        return "puh_acc_" + id;
    }

    public static void setServerUrl(String url) {
        ensureLoaded();
        config.serverUrl = AccountConfig.normalizeServerUrl(url);
        config.save();
        checkHealthAsync();
    }

    public static void setRememberLogin(boolean remember) {
        ensureLoaded();
        config.rememberLogin = remember;
        if (!remember) {
            config.savedToken = "";
            config.savedAccountId = "";
            config.savedDisplayName = "";
            config.offlineMode = false;
        } else if (isLoggedIn()) {
            persistActiveConfig();
        }
        config.save();
    }

    public static CompletableFuture<Boolean> checkHealthAsync() {
        ensureLoaded();
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(config.serverUrl + "/api/health"))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                boolean ok = resp.statusCode() == 200;
                serverReachable = ok;
                if (ok && (syncStatusText.contains("未连接") || syncStatusText.contains("待检测"))) {
                    syncStatusText = offlineMode ? "离线本地模式" : "云端已连接";
                } else if (!ok) {
                    syncStatusText = "云端响应异常 (" + resp.statusCode() + ")";
                }
                return ok;
            } catch (Exception e) {
                serverReachable = false;
                if (!offlineMode) {
                    syncStatusText = "云端未连接（可离线游玩）";
                }
                return false;
            }
        }, EXEC);
    }

    private static void verifySavedTokenInternal() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.serverUrl + "/api/auth/me"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200) {
                JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                applyAuthPayload(obj, authToken);
                syncStatusText = "云端会话已恢复";
            } else if (resp.statusCode() == 401) {
                clearSessionMemory();
                config.savedToken = "";
                config.savedAccountId = "";
                config.save();
                syncStatusText = "登录凭证已过期，请重新登录";
            }
        } catch (Exception e) {
            serverReachable = false;
            syncStatusText = "云端未连接（使用本地缓存）";
        }
    }

    public static CompletableFuture<AuthResult> registerAsync(String user, String password, String nick) {
        ensureLoaded();
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("username", user != null ? user.trim() : "");
                payload.addProperty("password", password != null ? password : "");
                if (nick != null && !nick.trim().isEmpty()) {
                    payload.addProperty("displayName", nick.trim());
                }

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(config.serverUrl + "/api/auth/register"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonObject json = safeParseJson(resp.body());
                serverReachable = true;

                if (resp.statusCode() >= 200 && resp.statusCode() < 300 && json != null && json.get("ok").getAsBoolean()) {
                    String token = json.get("token").getAsString();
                    applyAuthPayload(json, token);
                    persistActiveConfig();
                    syncStatusText = "注册并登录成功";
                    return new AuthResult(true, "注册成功");
                }
                String err = extractError(json, "注册失败 (" + resp.statusCode() + ")");
                return new AuthResult(false, err);
            } catch (Exception e) {
                serverReachable = false;
                return new AuthResult(false, "无法连接云端服务器，请确认本地测试服已启动或切换离线模式");
            }
        }, EXEC);
    }

    public static CompletableFuture<AuthResult> loginAsync(String user, String password) {
        ensureLoaded();
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("username", user != null ? user.trim() : "");
                payload.addProperty("password", password != null ? password : "");

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(config.serverUrl + "/api/auth/login"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonObject json = safeParseJson(resp.body());
                serverReachable = true;

                if (resp.statusCode() == 200 && json != null && json.get("ok").getAsBoolean()) {
                    String token = json.get("token").getAsString();
                    applyAuthPayload(json, token);
                    persistActiveConfig();
                    syncStatusText = "登录成功，云端已连接";
                    return new AuthResult(true, "登录成功");
                }
                String err = extractError(json, "登录失败 (" + resp.statusCode() + ")");
                return new AuthResult(false, err);
            } catch (Exception e) {
                serverReachable = false;
                return new AuthResult(false, "无法连接云端服务器，可点击下方离线游玩");
            }
        }, EXEC);
    }

    public static void loginOffline(String name) {
        ensureLoaded();
        String trimmed = name != null ? name.trim() : "";
        if (trimmed.isEmpty()) trimmed = "本地玩家";
        String id = sanitizeId(trimmed);
        if (id.isEmpty()) id = "local_player";

        synchronized (LOCK) {
            accountId = "off_" + id;
            username = trimmed;
            displayName = trimmed;
            authToken = "";
            offlineMode = true;
            hasCloudSave = false;
            cloudSaveSummary = null;
            syncStatusText = "离线本地模式";
            loadLocalSummaryIfPresent(accountId);
            persistActiveConfig();
        }
    }

    public static CompletableFuture<Void> logoutAsync() {
        ensureLoaded();
        final String tokenToRevoke = authToken;
        final String url = config.serverUrl;
        synchronized (LOCK) {
            clearSessionMemory();
            config.savedToken = "";
            config.savedAccountId = "";
            config.savedDisplayName = "";
            config.offlineMode = false;
            config.save();
            syncStatusText = "已退出账号";
        }
        return CompletableFuture.runAsync(() -> {
            if (tokenToRevoke != null && !tokenToRevoke.isEmpty()) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url + "/api/auth/logout"))
                            .timeout(Duration.ofSeconds(3))
                            .header("Authorization", "Bearer " + tokenToRevoke)
                            .POST(HttpRequest.BodyPublishers.ofString("{}"))
                            .build();
                    HTTP.send(req, HttpResponse.BodyHandlers.discarding());
                } catch (Exception ignored) {
                }
            }
        }, EXEC);
    }

    public static CompletableFuture<CloudSaveSnapshot> pullLatestBeforeEnterWorld() {
        ensureLoaded();
        final String curId = accountId;
        final String curToken = authToken;
        final boolean isOff = offlineMode;

        return CompletableFuture.supplyAsync(() -> {
            if (curId == null || curId.isEmpty()) return null;

            CloudSaveSnapshot local = readLocalSnapshot(curId);
            CloudSaveSnapshot chosen = local;

            if (!isOff && curToken != null && !curToken.isEmpty()) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(config.serverUrl + "/api/save/pull"))
                            .timeout(Duration.ofSeconds(5))
                            .header("Authorization", "Bearer " + curToken)
                            .GET()
                            .build();
                    HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (resp.statusCode() == 200) {
                        serverReachable = true;
                        JsonObject json = safeParseJson(resp.body());
                        if (json != null && json.has("hasSave") && json.get("hasSave").getAsBoolean()
                                && json.has("save") && json.get("save").isJsonObject()) {
                            CloudSaveSnapshot remote = GSON.fromJson(json.get("save"), CloudSaveSnapshot.class);
                            if (remote != null) {
                                hasCloudSave = true;
                                cloudSaveSummary = remote.summary;
                                long localTs = (local != null) ? local.updatedAt : 0L;
                                if (remote.updatedAt >= localTs) {
                                    chosen = remote;
                                    writeLocalSnapshot(curId, remote);
                                    lastSyncAt = remote.updatedAt;
                                    syncStatusText = "已拉取云端最新进度";
                                } else {
                                    syncStatusText = "本地备份较新，将同步至云端";
                                }
                            }
                        } else {
                            hasCloudSave = false;
                            syncStatusText = chosen != null ? "云端暂无存档，使用本地进度" : "新账号开荒";
                        }
                    } else if (resp.statusCode() == 401) {
                        syncStatusText = "云端凭证过期，已加载本地备份";
                    }
                } catch (Exception e) {
                    serverReachable = false;
                    syncStatusText = chosen != null ? "云端未连接，已加载本地备份" : "云端未连接（离线开荒）";
                }
            } else {
                syncStatusText = chosen != null ? "已加载本地离线存档" : "离线新档开荒";
            }

            synchronized (LOCK) {
                pendingRestoreSnapshot = chosen;
            }
            return chosen;
        }, EXEC);
    }

    public static CloudSaveSnapshot consumePendingRestore() {
        synchronized (LOCK) {
            CloudSaveSnapshot snap = pendingRestoreSnapshot;
            pendingRestoreSnapshot = null;
            return snap;
        }
    }

    public static void scheduleAutoPush(ServerPlayer player, String reason) {
        if (player == null) return;
        ensureLoaded();
        String curId = accountId;
        if (curId == null || curId.isEmpty()) return;

        CloudSaveSnapshot snap = CloudSaveSnapshot.captureFromPlayer(player, reason);
        writeLocalSnapshot(curId, snap);
        cloudSaveSummary = snap.summary;

        if (offlineMode || authToken == null || authToken.isEmpty()) {
            lastSyncAt = snap.updatedAt;
            syncStatusText = "已保存本地备份";
            return;
        }

        synchronized (LOCK) {
            queuedPushSnapshot = snap;
            if (queuedPushTask != null && !queuedPushTask.isDone()) {
                queuedPushTask.cancel(false);
            }
            queuedPushTask = EXEC.schedule(() -> {
                CloudSaveSnapshot toUpload;
                synchronized (LOCK) {
                    toUpload = queuedPushSnapshot;
                    queuedPushSnapshot = null;
                }
                if (toUpload != null) {
                    uploadSnapshotSync(curId, authToken, toUpload);
                }
            }, 2000L, TimeUnit.MILLISECONDS);
        }
    }

    public static void flushSyncBlocking(ServerPlayer player, String reason, long timeoutMs) {
        if (player == null) return;
        ensureLoaded();
        String curId = accountId;
        if (curId == null || curId.isEmpty()) return;

        synchronized (LOCK) {
            if (queuedPushTask != null && !queuedPushTask.isDone()) {
                queuedPushTask.cancel(false);
            }
            queuedPushSnapshot = null;
        }

        CloudSaveSnapshot snap = CloudSaveSnapshot.captureFromPlayer(player, reason);
        writeLocalSnapshot(curId, snap);
        cloudSaveSummary = snap.summary;

        if (offlineMode || authToken == null || authToken.isEmpty()) {
            lastSyncAt = snap.updatedAt;
            syncStatusText = "已保存本地备份";
            return;
        }

        String token = authToken;
        CompletableFuture<SyncResult> future = CompletableFuture.supplyAsync(
                () -> uploadSnapshotSync(curId, token, snap), EXEC);
        try {
            future.get(Math.max(500L, timeoutMs), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            PickingHopeCore.LOGGER.warn("Cloud flush on {} timed out or failed (local backup saved): {}", reason, e.getMessage());
        }
    }

    public static void pushManualFromServer(ServerPlayer player) {
        if (player == null) return;
        ensureLoaded();
        String curId = accountId;
        if (curId == null || curId.isEmpty()) {
            PacketHandler.sendToPlayer(player, new PacketCloudSyncAck(false, "当前未登录账号", System.currentTimeMillis()));
            return;
        }

        CloudSaveSnapshot snap = CloudSaveSnapshot.captureFromPlayer(player, "manual");
        writeLocalSnapshot(curId, snap);
        cloudSaveSummary = snap.summary;

        if (offlineMode || authToken == null || authToken.isEmpty()) {
            lastSyncAt = snap.updatedAt;
            syncStatusText = "离线模式：已保存本地备份";
            PacketHandler.sendToPlayer(player, new PacketCloudSyncAck(true, "离线模式：已保存至本地存档备份", snap.updatedAt));
            return;
        }

        String token = authToken;
        MinecraftServer server = player.getServer();
        EXEC.execute(() -> {
            SyncResult res = uploadSnapshotSync(curId, token, snap);
            if (server != null) {
                server.execute(() -> PacketHandler.sendToPlayer(player,
                        new PacketCloudSyncAck(res.ok(), res.message(), res.syncedAt())));
            }
        });
    }

    public static void applyClientSyncStatus(boolean ok, String message, long syncedAt) {
        syncStatusText = message != null ? message : (ok ? "同步成功" : "同步失败");
        if (ok && syncedAt > 0L) {
            lastSyncAt = syncedAt;
        }
    }

    private static SyncResult uploadSnapshotSync(String curId, String token, CloudSaveSnapshot snap) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.serverUrl + "/api/save/push"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(snap), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonObject json = safeParseJson(resp.body());

            if (resp.statusCode() == 200 && json != null && json.get("ok").getAsBoolean()) {
                serverReachable = true;
                hasCloudSave = true;
                long serverTs = json.has("updatedAt") ? json.get("updatedAt").getAsLong() : snap.updatedAt;
                int rev = json.has("revision") ? json.get("revision").getAsInt() : snap.revision + 1;
                snap.updatedAt = serverTs;
                snap.revision = rev;
                writeLocalSnapshot(curId, snap);
                lastSyncAt = serverTs;
                syncStatusText = "云端同步成功 (rev." + rev + ")";
                return new SyncResult(true, syncStatusText, serverTs);
            }
            String err = extractError(json, "云端保存失败 (" + resp.statusCode() + ")");
            syncStatusText = err + "（本地已备份）";
            return new SyncResult(false, syncStatusText, 0L);
        } catch (Exception e) {
            serverReachable = false;
            syncStatusText = "云端未连接，已保留本地备份";
            return new SyncResult(false, syncStatusText, 0L);
        }
    }

    private static void applyAuthPayload(JsonObject json, String token) {
        JsonObject acc = json.getAsJsonObject("account");
        synchronized (LOCK) {
            accountId = acc.get("id").getAsString();
            username = acc.get("username").getAsString();
            displayName = acc.has("displayName") ? acc.get("displayName").getAsString() : username;
            authToken = token;
            offlineMode = false;
            hasCloudSave = json.has("hasSave") && json.get("hasSave").getAsBoolean();
            if (json.has("saveSummary") && json.get("saveSummary").isJsonObject()) {
                cloudSaveSummary = GSON.fromJson(json.get("saveSummary"), CloudSaveSnapshot.Summary.class);
            } else {
                cloudSaveSummary = null;
                loadLocalSummaryIfPresent(accountId);
            }
        }
    }

    private static void persistActiveConfig() {
        config.lastUsername = username;
        if (config.rememberLogin) {
            config.savedAccountId = accountId != null ? accountId : "";
            config.savedDisplayName = displayName != null ? displayName : "";
            config.savedToken = offlineMode ? "" : (authToken != null ? authToken : "");
            config.offlineMode = offlineMode;
        }
        config.save();
    }

    private static void clearSessionMemory() {
        if (queuedPushTask != null && !queuedPushTask.isDone()) {
            queuedPushTask.cancel(false);
        }
        queuedPushTask = null;
        queuedPushSnapshot = null;
        accountId = null;
        username = "";
        displayName = "";
        authToken = "";
        offlineMode = false;
        hasCloudSave = false;
        cloudSaveSummary = null;
        pendingRestoreSnapshot = null;
    }

    private static void loadLocalSummaryIfPresent(String accId) {
        CloudSaveSnapshot local = readLocalSnapshot(accId);
        if (local != null && local.summary != null && cloudSaveSummary == null) {
            cloudSaveSummary = local.summary;
            lastSyncAt = local.updatedAt;
        }
    }

    private static Path getLocalProfileDir() {
        return AccountConfig.getConfigDir().resolve("local_profiles");
    }

    private static Path getLocalProfilePath(String accId) {
        return getLocalProfileDir().resolve(sanitizeId(accId) + ".json");
    }

    private static CloudSaveSnapshot readLocalSnapshot(String accId) {
        if (accId == null || accId.isEmpty()) return null;
        Path path = getLocalProfilePath(accId);
        if (!Files.exists(path)) return null;
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            return GSON.fromJson(raw, CloudSaveSnapshot.class);
        } catch (Exception e) {
            PickingHopeCore.LOGGER.warn("Failed to read local profile {}: {}", path, e.getMessage());
            return null;
        }
    }

    private static void writeLocalSnapshot(String accId, CloudSaveSnapshot snap) {
        if (accId == null || accId.isEmpty() || snap == null) return;
        try {
            Path dir = getLocalProfileDir();
            Files.createDirectories(dir);
            Path target = getLocalProfilePath(accId);
            Path tmp = dir.resolve(sanitizeId(accId) + ".json.tmp");
            Files.writeString(tmp, GSON.toJson(snap), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            PickingHopeCore.LOGGER.warn("Failed to write local profile backup: {}", e.getMessage());
        }
    }

    private static String sanitizeId(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replaceAll("[^a-zA-Z0-9_]", "_");
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }

    private static JsonObject safeParseJson(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractError(JsonObject json, String fallback) {
        if (json != null && json.has("error") && !json.get("error").isJsonNull()) {
            return json.get("error").getAsString();
        }
        return fallback;
    }
}
