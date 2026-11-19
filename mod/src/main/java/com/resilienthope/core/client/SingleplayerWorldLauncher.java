package com.resilienthope.core.client;

import com.resilienthope.core.PickingHopeCore;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
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
 * Client helper that creates isolated per-account singleplayer void worlds
 * from the bundled world_template/level.dat and launches them directly.
 */
public class SingleplayerWorldLauncher {
    private static final String TEMPLATE_RESOURCE = "/assets/picking_hope_core/world_template/level.dat";
    public static final String LEGACY_WORLD_FOLDER = "Picking_Up_Hope_World";

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
            createWorldFromTemplate(mc, folderName, displayTitle);
        }
        mc.loadLevel(folderName);
    }

    public static void createWorldFromTemplate(Minecraft mc, String folderName, String displayTitle) throws IOException {
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
            data.remove("Player");
        }

        Files.createDirectories(worldDir);
        NbtIo.writeCompressed(root, levelDat.toFile());
        PickingHopeCore.LOGGER.info("Created isolated account world folder: {}", folderName);
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
            }
            NbtIo.writeCompressed(root, dstLevel.toFile());
        }
        PickingHopeCore.LOGGER.info("Imported legacy save {} into {}", LEGACY_WORLD_FOLDER, targetFolderName);
    }
}
