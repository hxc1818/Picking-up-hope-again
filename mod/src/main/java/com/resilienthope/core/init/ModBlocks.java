package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PickingHopeCore.MOD_ID);

    // Interactive Voxel Machine Blocks
    public static final RegistryObject<Block> TELEPORT_WAYPOINT = BLOCKS.register("teleport_waypoint", WaypointBlock::new);
    public static final RegistryObject<Block> ENGINEERING_TERMINAL = BLOCKS.register("engineering_terminal", EngineeringTerminalBlock::new);
    public static final RegistryObject<Block> AUXILIARY_ENERGY_CORE = BLOCKS.register("auxiliary_energy_core", AuxiliaryEnergyCoreBlock::new);
    public static final RegistryObject<Block> ORBITAL_DROP_POD = BLOCKS.register("orbital_drop_pod", OrbitalDropPodBlock::new);
    public static final RegistryObject<Block> PRECURSOR_ALTAR = BLOCKS.register("precursor_altar", PrecursorAltarBlock::new);
    public static final RegistryObject<Block> SUPPLY_CRATE = BLOCKS.register("supply_crate", SupplyCrateBlock::new);

    // 3D Voxel Environmental Props
    public static final RegistryObject<Block> CRYO_SLEEP_POD = BLOCKS.register("cryo_sleep_pod",
            () -> new VoxelPropBlock(9, Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)));
    public static final RegistryObject<Block> CONDUIT_PILLAR = BLOCKS.register("conduit_pillar",
            () -> new VoxelPropBlock(6, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D)));
    public static final RegistryObject<Block> HOLOTABLE_PROP = BLOCKS.register("holotable_prop",
            () -> new VoxelPropBlock(12, Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D)));

    // Custom Sci-Fi & Planetary Structural Blocks (replaces vanilla greybox blocks)
    public static final RegistryObject<Block> STARSHIP_FLOOR = BLOCKS.register("starship_floor",
            () -> new Block(BlockBehaviour.Properties.of(Material.METAL).strength(5.0f, 15.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> STARSHIP_WALL = BLOCKS.register("starship_wall",
            () -> new Block(BlockBehaviour.Properties.of(Material.METAL).strength(5.0f, 15.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> STARSHIP_HULL = BLOCKS.register("starship_hull",
            () -> new Block(BlockBehaviour.Properties.of(Material.METAL).strength(6.0f, 18.0f).sound(SoundType.NETHERITE_BLOCK)));
    public static final RegistryObject<Block> HAZARD_STRIPE_BLOCK = BLOCKS.register("hazard_stripe_block",
            () -> new Block(BlockBehaviour.Properties.of(Material.METAL).strength(5.0f, 15.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> STARSHIP_LIGHT = BLOCKS.register("starship_light",
            () -> new Block(BlockBehaviour.Properties.of(Material.GLASS).strength(2.0f, 6.0f).sound(SoundType.GLASS).lightLevel(s -> 15)));
    public static final RegistryObject<Block> ALARM_BEACON = BLOCKS.register("alarm_beacon",
            () -> new Block(BlockBehaviour.Properties.of(Material.GLASS).strength(2.0f, 6.0f).sound(SoundType.GLASS).lightLevel(s -> 14)));
    public static final RegistryObject<Block> VIEWPORT_GLASS = BLOCKS.register("viewport_glass",
            () -> new Block(BlockBehaviour.Properties.of(Material.GLASS).strength(3.0f, 8.0f).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<Block> PERMAFROST_ROCK = BLOCKS.register("permafrost_rock",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE).strength(4.0f, 12.0f).sound(SoundType.BASALT)));
    public static final RegistryObject<Block> GLACIER_SNOW = BLOCKS.register("glacier_snow",
            () -> new Block(BlockBehaviour.Properties.of(Material.SNOW).strength(2.0f, 6.0f).sound(SoundType.SNOW)));
    public static final RegistryObject<Block> FROZEN_ORE_VEIN = BLOCKS.register("frozen_ore_vein",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE).strength(4.5f, 12.0f).sound(SoundType.AMETHYST).lightLevel(s -> 8)));
}
