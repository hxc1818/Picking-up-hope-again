package com.resilienthope.core.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.resilienthope.core.PickingHopeCore;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persistent configuration for singleplayer account login and cloud save server endpoint.
 * Common-safe: never references client-only Minecraft classes.
 */
public class AccountConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String DEFAULT_SERVER_URL = "http://127.0.0.1:3847";

    public String serverUrl = DEFAULT_SERVER_URL;
    public boolean rememberLogin = true;
    public String lastUsername = "";
    public String savedToken = "";
    public String savedAccountId = "";
    public String savedDisplayName = "";
    public boolean offlineMode = false;

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("picking_hope");
    }

    public static Path getConfigPath() {
        return getConfigDir().resolve("account_config.json");
    }

    public static AccountConfig load() {
        Path path = getConfigPath();
        AccountConfig cfg = new AccountConfig();
        if (Files.exists(path)) {
            try {
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                AccountConfig parsed = GSON.fromJson(raw, AccountConfig.class);
                if (parsed != null) {
                    cfg = parsed;
                }
            } catch (Exception e) {
                PickingHopeCore.LOGGER.warn("Failed to read account_config.json, using defaults: {}", e.getMessage());
            }
        }
        cfg.normalize();
        return cfg;
    }

    public void normalize() {
        this.serverUrl = normalizeServerUrl(this.serverUrl);
        if (this.lastUsername == null) this.lastUsername = "";
        if (this.savedToken == null) this.savedToken = "";
        if (this.savedAccountId == null) this.savedAccountId = "";
        if (this.savedDisplayName == null) this.savedDisplayName = "";
    }

    public static String normalizeServerUrl(String url) {
        if (url == null) return DEFAULT_SERVER_URL;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return DEFAULT_SERVER_URL;
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://" + trimmed;
        }
        while (trimmed.endsWith("/") && trimmed.length() > 8) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public void save() {
        normalize();
        try {
            Path dir = getConfigDir();
            Files.createDirectories(dir);
            Path target = getConfigPath();
            Path tmp = dir.resolve("account_config.json.tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            PickingHopeCore.LOGGER.warn("Failed to save account_config.json: {}", e.getMessage());
        }
    }
}
