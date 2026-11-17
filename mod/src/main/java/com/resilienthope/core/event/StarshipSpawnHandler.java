package com.resilienthope.core.event;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
            template.ifPresent(t -> {
                t.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 2);
            });

            // Teleport player into Awakening Bay (x: 6.5, y: 62.0, z: 10.5)
            player.teleportTo(level, 6.5, 62.0, 10.5, 90.0f, 0.0f);

            // Give starter plasma cutter weapon
            player.getInventory().add(new ItemStack(ModItems.PLASMA_CUTTER.get()));

            // Broadcast initial transmission
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new PacketRadioDialogue("重拾希望号导航系统", "检测到冷冻休眠特工生命体征复苏。当前位于工业第7舱区，请前往聚变大厅连接传送中继。", 200));

            player.addTag(SPAWN_TAG);
        }
    }
}
