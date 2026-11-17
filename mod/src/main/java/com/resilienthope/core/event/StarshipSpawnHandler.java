package com.resilienthope.core.event;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;

public class StarshipSpawnHandler {
    private static final String SPAWN_TAG = "picking_hope_spawned";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        if (!player.getTags().contains(SPAWN_TAG)) {
            ServerLevel level = player.getLevel();
            StructureManager templateManager = level.getStructureManager();
            ResourceLocation structureId = new ResourceLocation(PickingHopeCore.MOD_ID, "starship_prologue");
            Optional<StructureTemplate> template = templateManager.get(structureId);

            BlockPos origin = new BlockPos(0, 60, 0);
            if (template.isPresent()) {
                template.get().placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 2);
                PickingHopeCore.LOGGER.info("Picking Hope: Successfully placed starship_prologue via template.");
            } else {
                PickingHopeCore.LOGGER.warn("Picking Hope: Template not found via manager, building starship directly.");
                buildStarshipDirectly(level, origin);
            }

            // Teleport player into Awakening Bay (x: 6.5, y: 62.0, z: 10.5)
            player.teleportTo(level, 6.5, 62.0, 10.5, 90.0f, 0.0f);
            player.setRespawnPosition(level.dimension(), new BlockPos(6, 62, 10), 90.0f, true, false);

            // Give starter plasma cutter weapon
            player.getInventory().add(new ItemStack(ModItems.PLASMA_CUTTER.get()));

            // Broadcast initial transmission
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new PacketRadioDialogue("重拾希望号导航系统", "检测到冷冻休眠特工生命体征复苏。当前位于工业第7舱区，请前往聚变大厅连接传送中继。", 200));

            player.addTag(SPAWN_TAG);
        }
    }

    private static void buildStarshipDirectly(ServerLevel level, BlockPos origin) {
        int sizeX = 40;
        int sizeY = 7;
        int sizeZ = 20;

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                // Floor
                boolean isHazard = (x == 12 || x == 13 || x == 24 || x == 25) && (z >= 7 && z <= 12);
                level.setBlock(origin.offset(x, 0, z), isHazard ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState(), 2);

                // Ceiling
                boolean isLight = (x % 6 == 3) && (z % 6 == 3);
                level.setBlock(origin.offset(x, 6, z), isLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState(), 2);

                // Walls & partitions
                for (int y = 1; y < 6; y++) {
                    if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if ((x >= 13 && x <= 24) && !(z >= 7 && z <= 12)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if (x == 12 && !(z >= 8 && z <= 11)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if (x == 24 && !(z >= 8 && z <= 11)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else {
                        // Clear air inside
                        level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Interior Props:
        // Awakening Bay pedestal & chest
        level.setBlock(origin.offset(6, 1, 10), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        level.setBlock(origin.offset(6, 2, 10), Blocks.CHEST.defaultBlockState(), 2);

        // Fusion Hall: Central Teleport Waypoint
        level.setBlock(origin.offset(32, 1, 10), ModBlocks.TELEPORT_WAYPOINT.get().defaultBlockState(), 2);

        // Fusion Core Columns
        for (int y = 1; y < 6; y++) {
            level.setBlock(origin.offset(32, y, 5), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(origin.offset(32, y, 15), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(origin.offset(30, y, 10), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(origin.offset(34, y, 10), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        }
    }
}
