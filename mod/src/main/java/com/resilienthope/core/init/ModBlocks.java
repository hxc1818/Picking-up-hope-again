package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PickingHopeCore.MOD_ID);

    private static BlockBehaviour.Properties metalProps(float strength, int light) {
        return BlockBehaviour.Properties.of(Material.METAL).strength(strength, strength * 3.0f).sound(SoundType.METAL).lightLevel(s -> light);
    }

    private static BlockBehaviour.Properties hullProps() {
        return BlockBehaviour.Properties.of(Material.METAL).strength(6.0f, 18.0f).sound(SoundType.NETHERITE_BLOCK);
    }

    private static BlockBehaviour.Properties stoneProps(float strength, SoundType sound, int light) {
        return BlockBehaviour.Properties.of(Material.STONE).strength(strength, strength * 2.5f).sound(sound).lightLevel(s -> light);
    }

    // ============================================================
    // 1. TAB_PROPS (25): Interactive Machine Blocks + Voxel Furniture & Props
    // ============================================================
    public static final RegistryObject<Block> TELEPORT_WAYPOINT = BLOCKS.register("teleport_waypoint", WaypointBlock::new);
    public static final RegistryObject<Block> ENGINEERING_TERMINAL = BLOCKS.register("engineering_terminal", EngineeringTerminalBlock::new);
    public static final RegistryObject<Block> AUXILIARY_ENERGY_CORE = BLOCKS.register("auxiliary_energy_core", AuxiliaryEnergyCoreBlock::new);
    public static final RegistryObject<Block> ORBITAL_DROP_POD = BLOCKS.register("orbital_drop_pod", OrbitalDropPodBlock::new);
    public static final RegistryObject<Block> PRECURSOR_ALTAR = BLOCKS.register("precursor_altar", PrecursorAltarBlock::new);
    public static final RegistryObject<Block> SUPPLY_CRATE = BLOCKS.register("supply_crate", SupplyCrateBlock::new);

    public static final RegistryObject<Block> CRYO_SLEEP_POD = BLOCKS.register("cryo_sleep_pod",
            () -> new VoxelPropBlock(9, Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)));
    public static final RegistryObject<Block> CONDUIT_PILLAR = BLOCKS.register("conduit_pillar",
            () -> new VoxelPropBlock(6, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D)));
    public static final RegistryObject<Block> HOLOTABLE_PROP = BLOCKS.register("holotable_prop",
            () -> new VoxelPropBlock(12, Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D)));

    public static final RegistryObject<Block> PROP_MONITOR_DESK = BLOCKS.register("prop_monitor_desk",
            () -> new RotatablePropBlock(8, Shapes.or(
                    Block.box(1.0D, 0.0D, 2.0D, 15.0D, 9.0D, 14.0D),
                    Block.box(2.0D, 9.0D, 11.0D, 14.0D, 15.0D, 14.0D))));
    public static final RegistryObject<Block> PROP_WALL_SCREEN = BLOCKS.register("prop_wall_screen",
            () -> new RotatablePropBlock(Material.METAL, SoundType.GLASS, 10,
                    Block.box(1.0D, 2.0D, 13.5D, 15.0D, 14.0D, 16.0D), true));
    public static final RegistryObject<Block> PROP_SERVER_RACK = BLOCKS.register("prop_server_rack",
            () -> new RotatablePropBlock(6, Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D)));
    public static final RegistryObject<Block> PROP_CABLE_PIPE = BLOCKS.register("prop_cable_pipe",
            () -> new RotatablePropBlock(0, Block.box(0.0D, 10.0D, 4.0D, 16.0D, 15.0D, 12.0D)));
    public static final RegistryObject<Block> PROP_VENT_DUCT = BLOCKS.register("prop_vent_duct",
            () -> new RotatablePropBlock(0, Block.box(1.0D, 8.0D, 0.0D, 15.0D, 16.0D, 16.0D)));
    public static final RegistryObject<Block> PROP_MEDICAL_BED = BLOCKS.register("prop_medical_bed",
            () -> new RotatablePropBlock(Material.METAL, SoundType.WOOL, 2,
                    Block.box(1.0D, 0.0D, 0.0D, 15.0D, 8.5D, 16.0D), true));
    public static final RegistryObject<Block> PROP_MED_CABINET = BLOCKS.register("prop_med_cabinet",
            () -> new RotatablePropBlock(4, Block.box(2.0D, 0.0D, 6.0D, 14.0D, 16.0D, 15.5D)));
    public static final RegistryObject<Block> PROP_STORAGE_RACK = BLOCKS.register("prop_storage_rack",
            () -> new RotatablePropBlock(0, Block.box(1.0D, 0.0D, 3.0D, 15.0D, 16.0D, 13.0D)));
    public static final RegistryObject<Block> PROP_LAB_DESK = BLOCKS.register("prop_lab_desk",
            () -> new RotatablePropBlock(4, Block.box(0.5D, 0.0D, 2.0D, 15.5D, 10.0D, 14.0D)));
    public static final RegistryObject<Block> PROP_CREW_CHAIR = BLOCKS.register("prop_crew_chair",
            () -> new RotatablePropBlock(0, Shapes.or(
                    Block.box(3.0D, 0.0D, 3.0D, 13.0D, 7.0D, 13.0D),
                    Block.box(3.5D, 7.0D, 10.5D, 12.5D, 15.0D, 13.0D))));
    public static final RegistryObject<Block> PROP_CARGO_BARREL = BLOCKS.register("prop_cargo_barrel",
            () -> new VoxelPropBlock(0, Block.box(2.5D, 0.0D, 2.5D, 13.5D, 15.0D, 13.5D)));
    public static final RegistryObject<Block> PROP_FLOOR_LAMP = BLOCKS.register("prop_floor_lamp",
            () -> new RotatablePropBlock(15, Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D)));
    public static final RegistryObject<Block> PROP_ICE_CRYSTAL = BLOCKS.register("prop_ice_crystal",
            () -> new VoxelPropBlock(9, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 14.0D, 13.0D)));
    public static final RegistryObject<Block> PROP_BREAKER_BOX = BLOCKS.register("prop_breaker_box",
            () -> new RotatablePropBlock(5, Block.box(3.0D, 2.0D, 11.0D, 13.0D, 14.0D, 16.0D)));
    public static final RegistryObject<Block> PROP_BLACKBOX_RECORDER = BLOCKS.register("prop_blackbox_recorder",
            () -> new RotatablePropBlock(8, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D)));
    public static final RegistryObject<Block> PROP_RUIN_RELAY = BLOCKS.register("prop_ruin_relay",
            () -> new RotatablePropBlock(11, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 15.0D, 13.0D)));
    public static final RegistryObject<Block> PROP_WARNING_SIGN = BLOCKS.register("prop_warning_sign",
            () -> new RotatablePropBlock(8, Block.box(2.0D, 0.0D, 6.0D, 14.0D, 15.0D, 10.0D)));
    public static final RegistryObject<Block> PROP_FLOODLIGHT = BLOCKS.register("prop_floodlight",
            () -> new RotatablePropBlock(15, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D)));
    public static final RegistryObject<Block> PROP_ANTENNA_MAST = BLOCKS.register("prop_antenna_mast",
            () -> new VoxelPropBlock(4, Block.box(5.5D, 0.0D, 5.5D, 10.5D, 16.0D, 10.5D)));
    public static final RegistryObject<Block> PROP_SOLAR_PANEL = BLOCKS.register("prop_solar_panel",
            () -> new RotatablePropBlock(2, Block.box(1.0D, 0.0D, 1.0D, 15.0D, 11.0D, 15.0D)));
    public static final RegistryObject<Block> PROP_SUPPLY_PALLET = BLOCKS.register("prop_supply_pallet",
            () -> new RotatablePropBlock(0, Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D)));
    public static final RegistryObject<Block> PROP_BUNK_FRAME = BLOCKS.register("prop_bunk_frame",
            () -> new RotatablePropBlock(0, Block.box(2.0D, 0.0D, 3.0D, 14.0D, 16.0D, 15.0D)));
    public static final RegistryObject<Block> PROP_DINING_TABLE = BLOCKS.register("prop_dining_table",
            () -> new VoxelPropBlock(0, Block.box(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D)));
    public static final RegistryObject<Block> PROP_HOLOGRAM_PROJECTOR = BLOCKS.register("prop_hologram_projector",
            () -> new VoxelPropBlock(12, Block.box(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D)));

    // ============================================================
    // 2. TAB_STARSHIP (35): Starship Structural, Interior, Glass & Lighting
    // ============================================================
    public static final RegistryObject<Block> STARSHIP_FLOOR = BLOCKS.register("starship_floor",
            () -> new Block(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_WALL = BLOCKS.register("starship_wall",
            () -> new Block(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_HULL = BLOCKS.register("starship_hull",
            () -> new Block(hullProps()));
    public static final RegistryObject<Block> HAZARD_STRIPE_BLOCK = BLOCKS.register("hazard_stripe_block",
            () -> new Block(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_LIGHT = BLOCKS.register("starship_light",
            () -> new Block(BlockBehaviour.Properties.of(Material.GLASS).strength(2.0f, 6.0f).sound(SoundType.GLASS).lightLevel(s -> 15)));
    public static final RegistryObject<Block> ALARM_BEACON = BLOCKS.register("alarm_beacon",
            () -> new Block(BlockBehaviour.Properties.of(Material.GLASS).strength(2.0f, 6.0f).sound(SoundType.GLASS).lightLevel(s -> 14)));
    public static final RegistryObject<Block> VIEWPORT_GLASS = BLOCKS.register("viewport_glass",
            () -> new TranslucentFullBlock(BlockBehaviour.Properties.of(Material.GLASS).strength(3.0f, 8.0f).sound(SoundType.GLASS).noOcclusion()));

    public static final RegistryObject<Block> STARSHIP_FLOOR_STAIRS = BLOCKS.register("starship_floor_stairs",
            () -> new SciFiStairBlock(() -> STARSHIP_FLOOR.get().defaultBlockState(), metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_FLOOR_SLAB = BLOCKS.register("starship_floor_slab",
            () -> new SlabBlock(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_WALL_STAIRS = BLOCKS.register("starship_wall_stairs",
            () -> new SciFiStairBlock(() -> STARSHIP_WALL.get().defaultBlockState(), metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_WALL_SLAB = BLOCKS.register("starship_wall_slab",
            () -> new SlabBlock(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_HULL_STAIRS = BLOCKS.register("starship_hull_stairs",
            () -> new SciFiStairBlock(() -> STARSHIP_HULL.get().defaultBlockState(), hullProps()));
    public static final RegistryObject<Block> STARSHIP_HULL_SLAB = BLOCKS.register("starship_hull_slab",
            () -> new SlabBlock(hullProps()));

    public static final RegistryObject<Block> STARSHIP_GRATE_FLOOR = BLOCKS.register("starship_grate_floor",
            () -> new Block(metalProps(4.0f, 0).noOcclusion()));
    public static final RegistryObject<Block> STARSHIP_WHITE_PANEL = BLOCKS.register("starship_white_panel",
            () -> new Block(metalProps(4.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_WHITE_SLAB = BLOCKS.register("starship_white_slab",
            () -> new SlabBlock(metalProps(4.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_DARK_RIB = BLOCKS.register("starship_dark_rib",
            () -> new RotatedPillarBlock(hullProps()));
    public static final RegistryObject<Block> STARSHIP_CEILING_TILE = BLOCKS.register("starship_ceiling_tile",
            () -> new Block(metalProps(4.5f, 0)));
    public static final RegistryObject<Block> STARSHIP_DAMAGED_HULL = BLOCKS.register("starship_damaged_hull",
            () -> new Block(hullProps()));
    public static final RegistryObject<Block> STARSHIP_RAILING = BLOCKS.register("starship_railing",
            () -> new SciFiRailingBlock(metalProps(3.5f, 0).noOcclusion()));
    public static final RegistryObject<Block> STARSHIP_VIEWPORT_PANE = BLOCKS.register("starship_viewport_pane",
            () -> new SciFiRailingBlock(BlockBehaviour.Properties.of(Material.GLASS).strength(2.5f, 6.0f).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<Block> STARSHIP_AIRLOCK_DOOR = BLOCKS.register("starship_airlock_door",
            () -> new SciFiDoorBlock(metalProps(5.0f, 0).noOcclusion()));
    public static final RegistryObject<Block> STARSHIP_VENT_HATCH = BLOCKS.register("starship_vent_hatch",
            () -> new SciFiHatchBlock(metalProps(4.0f, 0).noOcclusion()));
    public static final RegistryObject<Block> CYAN_LIGHT_STRIP = BLOCKS.register("cyan_light_strip",
            () -> new RotatablePropBlock(Material.GLASS, SoundType.GLASS, 15,
                    Block.box(1.0D, 6.0D, 14.0D, 15.0D, 10.0D, 16.0D), false));
    public static final RegistryObject<Block> AMBER_LIGHT_STRIP = BLOCKS.register("amber_light_strip",
            () -> new RotatablePropBlock(Material.GLASS, SoundType.GLASS, 14,
                    Block.box(1.0D, 6.0D, 14.0D, 15.0D, 10.0D, 16.0D), false));
    public static final RegistryObject<Block> STARSHIP_HULL_DARK = BLOCKS.register("starship_hull_dark",
            () -> new Block(hullProps()));
    public static final RegistryObject<Block> STARSHIP_ACCENT_BLUE = BLOCKS.register("starship_accent_blue",
            () -> new Block(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_ACCENT_ORANGE = BLOCKS.register("starship_accent_orange",
            () -> new Block(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_WALL_VENT = BLOCKS.register("starship_wall_vent",
            () -> new Block(metalProps(4.5f, 0)));
    public static final RegistryObject<Block> STARSHIP_REINFORCED_GLASS = BLOCKS.register("starship_reinforced_glass",
            () -> new TranslucentFullBlock(BlockBehaviour.Properties.of(Material.GLASS).strength(4.0f, 10.0f).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<Block> STARSHIP_CATWALK_SLAB = BLOCKS.register("starship_catwalk_slab",
            () -> new SlabBlock(metalProps(4.0f, 0).noOcclusion()));
    public static final RegistryObject<Block> STARSHIP_HAZARD_SLAB = BLOCKS.register("starship_hazard_slab",
            () -> new SlabBlock(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> STARSHIP_PILLAR_LIGHT = BLOCKS.register("starship_pillar_light",
            () -> new RotatedPillarBlock(metalProps(4.5f, 13)));
    public static final RegistryObject<Block> STARSHIP_BULKHEAD_WALL = BLOCKS.register("starship_bulkhead_wall",
            () -> new WallBlock(hullProps()));
    public static final RegistryObject<Block> STARSHIP_LADDER = BLOCKS.register("starship_ladder",
            () -> new SciFiLadderBlock(metalProps(3.0f, 0).noOcclusion()));

    // ============================================================
    // 3. TAB_PLANET (34): Polar Glacier, Outpost, Deep Mine & Geothermal
    // ============================================================
    public static final RegistryObject<Block> PERMAFROST_ROCK = BLOCKS.register("permafrost_rock",
            () -> new Block(stoneProps(4.0f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> GLACIER_SNOW = BLOCKS.register("glacier_snow",
            () -> new Block(BlockBehaviour.Properties.of(Material.SNOW).strength(2.0f, 6.0f).sound(SoundType.SNOW)));
    public static final RegistryObject<Block> FROZEN_ORE_VEIN = BLOCKS.register("frozen_ore_vein",
            () -> new Block(stoneProps(4.5f, SoundType.AMETHYST, 8)));

    public static final RegistryObject<Block> PERMAFROST_STAIRS = BLOCKS.register("permafrost_stairs",
            () -> new SciFiStairBlock(() -> PERMAFROST_ROCK.get().defaultBlockState(), stoneProps(4.0f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> PERMAFROST_SLAB = BLOCKS.register("permafrost_slab",
            () -> new SlabBlock(stoneProps(4.0f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> PERMAFROST_BRICKS = BLOCKS.register("permafrost_bricks",
            () -> new Block(stoneProps(4.5f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> PERMAFROST_BRICKS_STAIRS = BLOCKS.register("permafrost_bricks_stairs",
            () -> new SciFiStairBlock(() -> PERMAFROST_BRICKS.get().defaultBlockState(), stoneProps(4.5f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> PERMAFROST_BRICKS_SLAB = BLOCKS.register("permafrost_bricks_slab",
            () -> new SlabBlock(stoneProps(4.5f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> GLACIER_ICE_BLOCK = BLOCKS.register("glacier_ice_block",
            () -> new TranslucentFullBlock(BlockBehaviour.Properties.of(Material.ICE_SOLID).strength(2.5f, 6.0f).sound(SoundType.GLASS).friction(0.96f).noOcclusion()));
    public static final RegistryObject<Block> SNOW_PACKED_SLAB = BLOCKS.register("snow_packed_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of(Material.SNOW).strength(2.0f, 6.0f).sound(SoundType.SNOW)));

    public static final RegistryObject<Block> OUTPOST_INSULATED_WALL = BLOCKS.register("outpost_insulated_wall",
            () -> new Block(metalProps(4.5f, 0)));
    public static final RegistryObject<Block> OUTPOST_INSULATED_SLAB = BLOCKS.register("outpost_insulated_slab",
            () -> new SlabBlock(metalProps(4.5f, 0)));
    public static final RegistryObject<Block> OUTPOST_ROOF_PLATING = BLOCKS.register("outpost_roof_plating",
            () -> new Block(metalProps(4.5f, 0)));
    public static final RegistryObject<Block> OUTPOST_ROOF_STAIRS = BLOCKS.register("outpost_roof_stairs",
            () -> new SciFiStairBlock(() -> OUTPOST_ROOF_PLATING.get().defaultBlockState(), metalProps(4.5f, 0)));

    public static final RegistryObject<Block> MINE_STEEL_BEAM = BLOCKS.register("mine_steel_beam",
            () -> new RotatedPillarBlock(hullProps()));
    public static final RegistryObject<Block> MINE_STEEL_TRUSS = BLOCKS.register("mine_steel_truss",
            () -> new Block(metalProps(4.0f, 0).noOcclusion()));
    public static final RegistryObject<Block> MINE_RUSTED_PLATE = BLOCKS.register("mine_rusted_plate",
            () -> new Block(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> MINE_CONCRETE_FLOOR = BLOCKS.register("mine_concrete_floor",
            () -> new Block(stoneProps(4.5f, SoundType.STONE, 0)));

    public static final RegistryObject<Block> GEOTHERMAL_BASALT = BLOCKS.register("geothermal_basalt",
            () -> new Block(stoneProps(4.5f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> GEOTHERMAL_BASALT_STAIRS = BLOCKS.register("geothermal_basalt_stairs",
            () -> new SciFiStairBlock(() -> GEOTHERMAL_BASALT.get().defaultBlockState(), stoneProps(4.5f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> GEOTHERMAL_BASALT_SLAB = BLOCKS.register("geothermal_basalt_slab",
            () -> new SlabBlock(stoneProps(4.5f, SoundType.BASALT, 0)));
    public static final RegistryObject<Block> GEOTHERMAL_WARM_STONE = BLOCKS.register("geothermal_warm_stone",
            () -> new Block(stoneProps(4.0f, SoundType.STONE, 7)));
    public static final RegistryObject<Block> GEOTHERMAL_STEAM_VENT = BLOCKS.register("geothermal_steam_vent",
            () -> new Block(metalProps(4.5f, 6)));
    public static final RegistryObject<Block> GEOTHERMAL_GLOW_MOSS = BLOCKS.register("geothermal_glow_moss",
            () -> new Block(stoneProps(2.5f, SoundType.MOSS, 11)));
    public static final RegistryObject<Block> POLAR_PACKED_SNOW = BLOCKS.register("polar_packed_snow",
            () -> new Block(BlockBehaviour.Properties.of(Material.SNOW).strength(2.2f, 6.0f).sound(SoundType.SNOW)));
    public static final RegistryObject<Block> POLAR_ICE_BRICKS = BLOCKS.register("polar_ice_bricks",
            () -> new Block(BlockBehaviour.Properties.of(Material.ICE_SOLID).strength(3.0f, 8.0f).sound(SoundType.GLASS).friction(0.94f)));
    public static final RegistryObject<Block> OUTPOST_YELLOW_WALL = BLOCKS.register("outpost_yellow_wall",
            () -> new Block(metalProps(4.5f, 0)));
    public static final RegistryObject<Block> OUTPOST_DARK_TRIM = BLOCKS.register("outpost_dark_trim",
            () -> new Block(metalProps(5.0f, 0)));
    public static final RegistryObject<Block> OUTPOST_WINDOW_GLASS = BLOCKS.register("outpost_window_glass",
            () -> new TranslucentFullBlock(BlockBehaviour.Properties.of(Material.GLASS).strength(3.0f, 8.0f).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<Block> OUTPOST_CORRUGATED_SLAB = BLOCKS.register("outpost_corrugated_slab",
            () -> new SlabBlock(metalProps(4.5f, 0)));
    public static final RegistryObject<Block> MINE_SUPPORT_WALL = BLOCKS.register("mine_support_wall",
            () -> new WallBlock(hullProps()));
    public static final RegistryObject<Block> MINE_CONCRETE_SLAB = BLOCKS.register("mine_concrete_slab",
            () -> new SlabBlock(stoneProps(4.5f, SoundType.STONE, 0)));
    public static final RegistryObject<Block> MINE_CAUTION_CONCRETE = BLOCKS.register("mine_caution_concrete",
            () -> new Block(stoneProps(4.5f, SoundType.STONE, 0)));
    public static final RegistryObject<Block> GEOTHERMAL_MAGMA_CRUST = BLOCKS.register("geothermal_magma_crust",
            () -> new Block(stoneProps(4.5f, SoundType.BASALT, 10)));

    // ============================================================
    // 4. TAB_RUINS (28): Alien Obsidian/Platinum Ruins, Hardlight & Ark Harbor
    // ============================================================
    public static final RegistryObject<Block> RUIN_OBSIDIAN_BLOCK = BLOCKS.register("ruin_obsidian_block",
            () -> new Block(stoneProps(6.5f, SoundType.DEEPSLATE, 0)));
    public static final RegistryObject<Block> RUIN_OBSIDIAN_STAIRS = BLOCKS.register("ruin_obsidian_stairs",
            () -> new SciFiStairBlock(() -> RUIN_OBSIDIAN_BLOCK.get().defaultBlockState(), stoneProps(6.5f, SoundType.DEEPSLATE, 0)));
    public static final RegistryObject<Block> RUIN_OBSIDIAN_SLAB = BLOCKS.register("ruin_obsidian_slab",
            () -> new SlabBlock(stoneProps(6.5f, SoundType.DEEPSLATE, 0)));

    public static final RegistryObject<Block> RUIN_PLATINUM_TRIM = BLOCKS.register("ruin_platinum_trim",
            () -> new Block(metalProps(6.0f, 2)));
    public static final RegistryObject<Block> RUIN_PLATINUM_STAIRS = BLOCKS.register("ruin_platinum_stairs",
            () -> new SciFiStairBlock(() -> RUIN_PLATINUM_TRIM.get().defaultBlockState(), metalProps(6.0f, 2)));
    public static final RegistryObject<Block> RUIN_PLATINUM_SLAB = BLOCKS.register("ruin_platinum_slab",
            () -> new SlabBlock(metalProps(6.0f, 2)));

    public static final RegistryObject<Block> RUIN_CARVED_GLYPH = BLOCKS.register("ruin_carved_glyph",
            () -> new Block(stoneProps(6.0f, SoundType.DEEPSLATE, 4)));
    public static final RegistryObject<Block> RUIN_CIRCUIT_TEAL = BLOCKS.register("ruin_circuit_teal",
            () -> new Block(stoneProps(6.0f, SoundType.AMETHYST, 10)));
    public static final RegistryObject<Block> RUIN_CIRCUIT_GOLD = BLOCKS.register("ruin_circuit_gold",
            () -> new Block(stoneProps(6.0f, SoundType.AMETHYST, 10)));
    public static final RegistryObject<Block> RUIN_PILLAR = BLOCKS.register("ruin_pillar",
            () -> new RotatedPillarBlock(stoneProps(6.0f, SoundType.DEEPSLATE, 0)));
    public static final RegistryObject<Block> RUIN_FLOOR_TILE = BLOCKS.register("ruin_floor_tile",
            () -> new Block(stoneProps(6.0f, SoundType.DEEPSLATE_TILES, 0)));
    public static final RegistryObject<Block> RUIN_ENERGY_CORE = BLOCKS.register("ruin_energy_core",
            () -> new Block(BlockBehaviour.Properties.of(Material.GLASS).strength(4.0f, 12.0f).sound(SoundType.AMETHYST).lightLevel(s -> 15)));

    public static final RegistryObject<Block> RUIN_HARDLIGHT_BRIDGE = BLOCKS.register("ruin_hardlight_bridge",
            () -> new SlabBlock(BlockBehaviour.Properties.of(Material.GLASS).strength(3.0f, 10.0f).sound(SoundType.AMETHYST).lightLevel(s -> 12).noOcclusion()));
    public static final RegistryObject<Block> RUIN_HARDLIGHT_BARRIER = BLOCKS.register("ruin_hardlight_barrier",
            () -> new SciFiRailingBlock(BlockBehaviour.Properties.of(Material.GLASS).strength(3.5f, 10.0f).sound(SoundType.AMETHYST).lightLevel(s -> 10).noOcclusion()));

    public static final RegistryObject<Block> RUIN_ARK_HULL = BLOCKS.register("ruin_ark_hull",
            () -> new Block(hullProps().lightLevel(s -> 2)));
    public static final RegistryObject<Block> RUIN_ARK_STAIRS = BLOCKS.register("ruin_ark_stairs",
            () -> new SciFiStairBlock(() -> RUIN_ARK_HULL.get().defaultBlockState(), hullProps().lightLevel(s -> 2)));
    public static final RegistryObject<Block> RUIN_ARK_SLAB = BLOCKS.register("ruin_ark_slab",
            () -> new SlabBlock(hullProps().lightLevel(s -> 2)));
    public static final RegistryObject<Block> RUIN_DOCKING_BEACON = BLOCKS.register("ruin_docking_beacon",
            () -> new Block(BlockBehaviour.Properties.of(Material.GLASS).strength(3.5f, 10.0f).sound(SoundType.AMETHYST).lightLevel(s -> 15)));

    public static final RegistryObject<Block> RUIN_ANCIENT_STELE = BLOCKS.register("ruin_ancient_stele",
            () -> new RotatablePropBlock(Material.STONE, SoundType.DEEPSLATE, 9,
                    Block.box(2.0D, 0.0D, 5.0D, 14.0D, 16.0D, 11.0D), true));
    public static final RegistryObject<Block> RUIN_FLOATING_PRISM = BLOCKS.register("ruin_floating_prism",
            () -> new VoxelPropBlock(13, Block.box(4.0D, 3.0D, 4.0D, 12.0D, 15.0D, 12.0D)));
    public static final RegistryObject<Block> RUIN_OBSIDIAN_WALL = BLOCKS.register("ruin_obsidian_wall",
            () -> new WallBlock(stoneProps(6.5f, SoundType.DEEPSLATE, 0)));
    public static final RegistryObject<Block> RUIN_PLATINUM_WALL = BLOCKS.register("ruin_platinum_wall",
            () -> new WallBlock(metalProps(6.0f, 2)));
    public static final RegistryObject<Block> RUIN_CRACKED_OBSIDIAN = BLOCKS.register("ruin_cracked_obsidian",
            () -> new Block(stoneProps(6.0f, SoundType.DEEPSLATE, 4)));
    public static final RegistryObject<Block> RUIN_MOSSY_OBSIDIAN = BLOCKS.register("ruin_mossy_obsidian",
            () -> new Block(stoneProps(6.0f, SoundType.DEEPSLATE, 2)));
    public static final RegistryObject<Block> RUIN_HEX_FLOOR = BLOCKS.register("ruin_hex_floor",
            () -> new Block(stoneProps(6.0f, SoundType.DEEPSLATE_TILES, 3)));
    public static final RegistryObject<Block> RUIN_ENERGY_CONDUIT = BLOCKS.register("ruin_energy_conduit",
            () -> new RotatedPillarBlock(stoneProps(5.5f, SoundType.AMETHYST, 13)));
    public static final RegistryObject<Block> RUIN_PORTAL_FRAME = BLOCKS.register("ruin_portal_frame",
            () -> new Block(stoneProps(7.0f, SoundType.DEEPSLATE, 6)));
    public static final RegistryObject<Block> RUIN_ARK_GLOW_PANEL = BLOCKS.register("ruin_ark_glow_panel",
            () -> new Block(hullProps().lightLevel(s -> 11)));
}
