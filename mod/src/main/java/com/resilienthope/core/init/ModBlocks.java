package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.block.WaypointBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PickingHopeCore.MOD_ID);

    public static final RegistryObject<Block> TELEPORT_WAYPOINT = BLOCKS.register("teleport_waypoint", WaypointBlock::new);
}
