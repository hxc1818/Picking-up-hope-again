package com.resilienthope.core.client.battle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.resilienthope.core.PickingHopeCore;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads battle animation JSON clips from resources/assets/picking_hope_core/battle_animations/*.json
 * AND optional hot-reload overrides from config/picking_hope/battle_animations/*.json.
 */
public class BattleAnimationRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final BattleAnimationRegistry INSTANCE = new BattleAnimationRegistry();

    private final Map<String, BattleAnimClip> clips = new LinkedHashMap<>();

    private BattleAnimationRegistry() {
        super(GSON, "battle_animations");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        clips.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            if (!PickingHopeCore.MOD_ID.equals(entry.getKey().getNamespace())) continue;
            if (!entry.getValue().isJsonObject()) continue;
            String key = extractClipId(entry.getKey());
            BattleAnimClip clip = BattleAnimClip.fromJson(key, entry.getValue().getAsJsonObject());
            clips.put(clip.id, clip);
        }
        loadExternalOverrides();
        PickingHopeCore.LOGGER.info("Loaded {} battle animation clips.", clips.size());
    }

    public int hotReloadNow() {
        Minecraft mc = Minecraft.getInstance();
        clips.clear();
        ResourceManager rm = mc.getResourceManager();
        try {
            Collection<ResourceLocation> found = rm.listResources("battle_animations", name -> name.endsWith(".json"));
            for (ResourceLocation rl : found) {
                if (!PickingHopeCore.MOD_ID.equals(rl.getNamespace())) continue;
                try (Resource res = rm.getResource(rl);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonElement el = JsonParser.parseReader(reader);
                    if (el != null && el.isJsonObject()) {
                        String fallback = rl.getPath()
                                .replace("battle_animations/", "")
                                .replace(".json", "");
                        BattleAnimClip clip = BattleAnimClip.fromJson(fallback, el.getAsJsonObject());
                        clips.put(clip.id, clip);
                    }
                } catch (Exception ex) {
                    PickingHopeCore.LOGGER.warn("Failed to parse battle animation {}: {}", rl, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            PickingHopeCore.LOGGER.warn("Error scanning resource battle_animations: {}", ex.getMessage());
        }
        loadExternalOverrides();
        return clips.size();
    }

    private void loadExternalOverrides() {
        Path overrideDir = FMLPaths.CONFIGDIR.get().resolve("picking_hope").resolve("battle_animations");
        if (!Files.isDirectory(overrideDir)) return;
        try (var stream = Files.list(overrideDir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                try {
                    String raw = Files.readString(path, StandardCharsets.UTF_8);
                    JsonElement el = JsonParser.parseString(raw);
                    if (el != null && el.isJsonObject()) {
                        String fallback = path.getFileName().toString().replace(".json", "");
                        BattleAnimClip clip = BattleAnimClip.fromJson(fallback, el.getAsJsonObject());
                        clips.put(clip.id, clip);
                    }
                } catch (Exception ex) {
                    PickingHopeCore.LOGGER.warn("Invalid override battle_anim {}: {}", path.getFileName(), ex.getMessage());
                }
            });
        } catch (Exception ex) {
            PickingHopeCore.LOGGER.warn("Failed reading config battle_animations overrides: {}", ex.getMessage());
        }
    }

    private static String extractClipId(ResourceLocation rl) {
        String p = rl.getPath();
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(slash + 1) : p;
    }

    public BattleAnimClip getClip(String id) {
        if (id != null && clips.containsKey(id)) {
            return clips.get(id);
        }
        if (id != null && clips.containsKey("humanoid_idle") && id.endsWith("_idle")) {
            return clips.get("humanoid_idle");
        }
        return BattleAnimClip.createFallbackClip(id, id != null && id.contains("idle"));
    }

    public boolean hasClip(String id) {
        return id != null && clips.containsKey(id);
    }

    public List<String> getSortedClipIds() {
        List<String> ids = new ArrayList<>(clips.keySet());
        Collections.sort(ids);
        return ids;
    }
}
