package com.resilienthope.core.client;

import com.resilienthope.core.PickingHopeCore;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Client helper that creates isolated per-account singleplayer worlds and the dedicated Map Builder workshop
 * from the bundled world_template/level.dat, ensuring 1.18.2 custom dimensions exist in WorldGenSettings.
 */
public class SingleplayerWorldLauncher {
    private static final String TEMPLATE_RESOURCE = "/assets/picking_hope_core/world_template/level.dat";
    public static final String LEGACY_WORLD_FOLDER = "Picking_Up_Hope_World";
    public static final String BUILDER_WORLD_FOLDER = "puh_map_builder";
    public static final String BUILDER_WORLD_TITLE = "重拾希望 · 地图造景工坊";

    public static boolean worldExists(Minecraft mc, String folderName) {
        if (mc == null || folderName == null || folderName.isEmpty()) return false;
        Path dir = mc.getLevelSource().getBaseDir().resolve(folderName);
        return Files.isDirectory(dir) && Files.exists(dir.resolve("level.dat"));
    }

    public static boolean hasLegacyWorld(Minecraft mc) {
        return worldExists(mc, LEGACY_WORLD_FOLDER);
    }

    public static void ensureAndLaunchWorld(Minecraft mc, String folderName, String displayTitle) throws IOException {
        if (mc == null || folderName == null || folderName.isEmpty()) {
            throw new IllegalArgumentException("Invalid world folder name");
        }
        if (!worldExists(mc, folderName)) {
            createWorldFromTemplate(mc, folderName, displayTitle, false);
        } else {
            ensureCustomDimensionsInLevelDat(mc.getLevelSource().getBaseDir().resolve(folderName).resolve("level.dat"), false);
        }
        mc.loadLevel(folderName);
    }

    public static void ensureAndLaunchBuilderWorld(Minecraft mc) throws IOException {
        if (mc == null) {
            throw new IllegalArgumentException("Minecraft instance is null");
        }
        if (!worldExists(mc, BUILDER_WORLD_FOLDER)) {
            createWorldFromTemplate(mc, BUILDER_WORLD_FOLDER, BUILDER_WORLD_TITLE, true);
        } else {
            ensureCustomDimensionsInLevelDat(mc.getLevelSource().getBaseDir().resolve(BUILDER_WORLD_FOLDER).resolve("level.dat"), true);
        }
        mc.loadLevel(BUILDER_WORLD_FOLDER);
    }

    public static void createWorldFromTemplate(Minecraft mc, String folderName, String displayTitle, boolean builderMode) throws IOException {
        Path worldDir = mc.getLevelSource().getBaseDir().resolve(folderName);
        Path levelDat = worldDir.resolve("level.dat");

        CompoundTag root;
        try (InputStream is = SingleplayerWorldLauncher.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (is == null) {
                throw new IOException("Missing bundled template: " + TEMPLATE_RESOURCE);
            }
            root = NbtIo.readCompressed(is);
        }

        if (root.contains("Data", Tag.TAG_COMPOUND)) {
            CompoundTag data = root.getCompound("Data");
            data.putString("LevelName", displayTitle != null && !displayTitle.isEmpty() ? displayTitle : "重拾希望");
            data.putLong("LastPlayed", System.currentTimeMillis());
            data.putBoolean("allowCommands", true);
            data.remove("Player");
            if (builderMode) {
                data.putInt("GameType", 1); // Creative mode
                data.putInt("SpawnX", 0);
                data.putInt("SpawnY", 62);
                data.putInt("SpawnZ", 0);
                CompoundTag rules = data.getCompound("GameRules");
                rules.putString("doDaylightCycle", "false");
                rules.putString("doWeatherCycle", "false");
                rules.putString("doMobSpawning", "false");
                rules.putString("keepInventory", "true");
                data.put("GameRules", rules);
            }
            injectCustomDimensions(data);
        }

        Files.createDirectories(worldDir);
        NbtIo.writeCompressed(root, levelDat.toFile());
        PickingHopeCore.LOGGER.info("Created isolated world folder: {} (builder={})", folderName, builderMode);
    }

    public static void ensureCustomDimensionsInLevelDat(Path levelDat, boolean ensureBuilderFlags) {
        if (levelDat == null || !Files.exists(levelDat)) return;
        try {
            CompoundTag root = NbtIo.readCompressed(levelDat.toFile());
            if (!root.contains("Data", Tag.TAG_COMPOUND)) return;
            CompoundTag data = root.getCompound("Data");
            boolean dirty = injectCustomDimensions(data);
            if (!data.getBoolean("allowCommands")) {
                data.putBoolean("allowCommands", true);
                dirty = true;
            }
            if (ensureBuilderFlags && data.getInt("GameType") != 1) {
                data.putInt("GameType", 1);
                dirty = true;
            }
            if (dirty) {
                NbtIo.writeCompressed(root, levelDat.toFile());
            }
        } catch (Exception e) {
            PickingHopeCore.LOGGER.warn("Failed ensuring custom dimensions in {}: {}", levelDat, e.getMessage());
        }
    }

    private static boolean injectCustomDimensions(CompoundTag data) {
        if (!data.contains("WorldGenSettings", Tag.TAG_COMPOUND)) return false;
        CompoundTag wgs = data.getCompound("WorldGenSettings");
        if (!wgs.contains("dimensions", Tag.TAG_COMPOUND)) {
            wgs.put("dimensions", new CompoundTag());
        }
        CompoundTag dims = wgs.getCompound("dimensions");
        boolean changed = false;

        if (!dims.contains("picking_hope_core:starship", Tag.TAG_COMPOUND)) {
            dims.put("picking_hope_core:starship", buildFlatDimTag(
                    "picking_hope_core:starship", "minecraft:the_void",
                    layer("minecraft:air", 1)
            ));
            changed = true;
        }
        if (!dims.contains("picking_hope_core:planet_cryo", Tag.TAG_COMPOUND)) {
            dims.put("picking_hope_core:planet_cryo", buildFlatDimTag(
                    "picking_hope_core:planet_cryo", "minecraft:snowy_plains",
                    layer("minecraft:bedrock", 1),
                    layer("minecraft:deepslate", 111),
                    layer("minecraft:packed_ice", 11),
                    layer("minecraft:snow_block", 5)
            ));
            changed = true;
        }
        if (!dims.contains("picking_hope_core:battle_arena", Tag.TAG_COMPOUND)) {
            dims.put("picking_hope_core:battle_arena", buildFlatDimTag(
                    "picking_hope_core:battle_arena", "minecraft:the_void",
                    layer("minecraft:air", 1)
            ));
            changed = true;
        }
        return changed;
    }

    private static CompoundTag layer(String blockId, int height) {
        CompoundTag tag = new CompoundTag();
        tag.putString("block", blockId);
        tag.putInt("height", height);
        return tag;
    }

    private static CompoundTag buildFlatDimTag(String dimTypeId, String biomeId, CompoundTag... layers) {
        CompoundTag dim = new CompoundTag();
        dim.putString("type", dimTypeId);

        CompoundTag settings = new CompoundTag();
        settings.putString("biome", biomeId);
        settings.putBoolean("lakes", false);
        settings.putBoolean("features", false);
        ListTag layerList = new ListTag();
        for (CompoundTag l : layers) {
            layerList.add(l);
        }
        settings.put("layers", layerList);
        settings.put("structure_overrides", new ListTag());

        CompoundTag generator = new CompoundTag();
        generator.putString("type", "minecraft:flat");
        generator.put("settings", settings);

        dim.put("generator", generator);
        return dim;
    }

    public static void importLegacyWorldIntoAccount(Minecraft mc, String targetFolderName, String displayTitle) throws IOException {
        if (!hasLegacyWorld(mc)) {
            throw new IOException("未找到旧本地存档 Picking_Up_Hope_World");
        }
        Path savesBase = mc.getLevelSource().getBaseDir();
        Path srcDir = savesBase.resolve(LEGACY_WORLD_FOLDER);
        Path dstDir = savesBase.resolve(targetFolderName);
        Files.createDirectories(dstDir);

        Files.walkFileTree(srcDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = srcDir.relativize(dir);
                Files.createDirectories(dstDir.resolve(rel));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if ("session.lock".equals(file.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                Path rel = srcDir.relativize(file);
                Files.copy(file, dstDir.resolve(rel), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        Path dstLevel = dstDir.resolve("level.dat");
        if (Files.exists(dstLevel)) {
            CompoundTag root = NbtIo.readCompressed(dstLevel.toFile());
            if (root.contains("Data", Tag.TAG_COMPOUND)) {
                CompoundTag data = root.getCompound("Data");
                data.putString("LevelName", displayTitle != null && !displayTitle.isEmpty() ? displayTitle : "重拾希望");
                data.putLong("LastPlayed", System.currentTimeMillis());
                injectCustomDimensions(data);
            }
            NbtIo.writeCompressed(root, dstLevel.toFile());
        }
        PickingHopeCore.LOGGER.info("Imported legacy save {} into {}", LEGACY_WORLD_FOLDER, targetFolderName);
    }
}
