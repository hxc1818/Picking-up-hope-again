package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.item.PrefabProjectorItem;
import com.resilienthope.core.item.TechChipItem;
import com.resilienthope.core.item.TechWeaponItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PickingHopeCore.MOD_ID);

    private static RegistryObject<Item> regBlockItem(String id, RegistryObject<Block> block, CreativeModeTab tab) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().tab(tab)));
    }

    // ============================================================
    // 1. TAB_PROPS (33 Blocks + 1 Builder Tool): Interactive & Voxel Props
    // ============================================================
    public static final RegistryObject<Item> PREFAB_PROJECTOR = ITEMS.register("prefab_projector",
            () -> new PrefabProjectorItem(new Item.Properties().tab(ModCreativeTabs.TAB_PROPS).stacksTo(1)));
    public static final RegistryObject<Item> TELEPORT_WAYPOINT = regBlockItem("teleport_waypoint", ModBlocks.TELEPORT_WAYPOINT, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> ENGINEERING_TERMINAL = regBlockItem("engineering_terminal", ModBlocks.ENGINEERING_TERMINAL, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> AUXILIARY_ENERGY_CORE = regBlockItem("auxiliary_energy_core", ModBlocks.AUXILIARY_ENERGY_CORE, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> ORBITAL_DROP_POD = regBlockItem("orbital_drop_pod", ModBlocks.ORBITAL_DROP_POD, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PRECURSOR_ALTAR = regBlockItem("precursor_altar", ModBlocks.PRECURSOR_ALTAR, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> SUPPLY_CRATE = regBlockItem("supply_crate", ModBlocks.SUPPLY_CRATE, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> CRYO_SLEEP_POD = regBlockItem("cryo_sleep_pod", ModBlocks.CRYO_SLEEP_POD, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> CONDUIT_PILLAR = regBlockItem("conduit_pillar", ModBlocks.CONDUIT_PILLAR, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> HOLOTABLE_PROP = regBlockItem("holotable_prop", ModBlocks.HOLOTABLE_PROP, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_MONITOR_DESK = regBlockItem("prop_monitor_desk", ModBlocks.PROP_MONITOR_DESK, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_WALL_SCREEN = regBlockItem("prop_wall_screen", ModBlocks.PROP_WALL_SCREEN, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_SERVER_RACK = regBlockItem("prop_server_rack", ModBlocks.PROP_SERVER_RACK, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_CABLE_PIPE = regBlockItem("prop_cable_pipe", ModBlocks.PROP_CABLE_PIPE, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_VENT_DUCT = regBlockItem("prop_vent_duct", ModBlocks.PROP_VENT_DUCT, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_MEDICAL_BED = regBlockItem("prop_medical_bed", ModBlocks.PROP_MEDICAL_BED, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_MED_CABINET = regBlockItem("prop_med_cabinet", ModBlocks.PROP_MED_CABINET, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_STORAGE_RACK = regBlockItem("prop_storage_rack", ModBlocks.PROP_STORAGE_RACK, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_LAB_DESK = regBlockItem("prop_lab_desk", ModBlocks.PROP_LAB_DESK, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_CREW_CHAIR = regBlockItem("prop_crew_chair", ModBlocks.PROP_CREW_CHAIR, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_CARGO_BARREL = regBlockItem("prop_cargo_barrel", ModBlocks.PROP_CARGO_BARREL, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_FLOOR_LAMP = regBlockItem("prop_floor_lamp", ModBlocks.PROP_FLOOR_LAMP, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_ICE_CRYSTAL = regBlockItem("prop_ice_crystal", ModBlocks.PROP_ICE_CRYSTAL, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_BREAKER_BOX = regBlockItem("prop_breaker_box", ModBlocks.PROP_BREAKER_BOX, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_BLACKBOX_RECORDER = regBlockItem("prop_blackbox_recorder", ModBlocks.PROP_BLACKBOX_RECORDER, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_RUIN_RELAY = regBlockItem("prop_ruin_relay", ModBlocks.PROP_RUIN_RELAY, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_WARNING_SIGN = regBlockItem("prop_warning_sign", ModBlocks.PROP_WARNING_SIGN, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_FLOODLIGHT = regBlockItem("prop_floodlight", ModBlocks.PROP_FLOODLIGHT, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_ANTENNA_MAST = regBlockItem("prop_antenna_mast", ModBlocks.PROP_ANTENNA_MAST, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_SOLAR_PANEL = regBlockItem("prop_solar_panel", ModBlocks.PROP_SOLAR_PANEL, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_SUPPLY_PALLET = regBlockItem("prop_supply_pallet", ModBlocks.PROP_SUPPLY_PALLET, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_BUNK_FRAME = regBlockItem("prop_bunk_frame", ModBlocks.PROP_BUNK_FRAME, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_DINING_TABLE = regBlockItem("prop_dining_table", ModBlocks.PROP_DINING_TABLE, ModCreativeTabs.TAB_PROPS);
    public static final RegistryObject<Item> PROP_HOLOGRAM_PROJECTOR = regBlockItem("prop_hologram_projector", ModBlocks.PROP_HOLOGRAM_PROJECTOR, ModCreativeTabs.TAB_PROPS);

    // ============================================================
    // 2. TAB_STARSHIP (35): Starship Architecture & Fixtures
    // ============================================================
    public static final RegistryObject<Item> STARSHIP_FLOOR = regBlockItem("starship_floor", ModBlocks.STARSHIP_FLOOR, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_WALL = regBlockItem("starship_wall", ModBlocks.STARSHIP_WALL, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_HULL = regBlockItem("starship_hull", ModBlocks.STARSHIP_HULL, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> HAZARD_STRIPE_BLOCK = regBlockItem("hazard_stripe_block", ModBlocks.HAZARD_STRIPE_BLOCK, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_LIGHT = regBlockItem("starship_light", ModBlocks.STARSHIP_LIGHT, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> ALARM_BEACON = regBlockItem("alarm_beacon", ModBlocks.ALARM_BEACON, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> VIEWPORT_GLASS = regBlockItem("viewport_glass", ModBlocks.VIEWPORT_GLASS, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_FLOOR_STAIRS = regBlockItem("starship_floor_stairs", ModBlocks.STARSHIP_FLOOR_STAIRS, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_FLOOR_SLAB = regBlockItem("starship_floor_slab", ModBlocks.STARSHIP_FLOOR_SLAB, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_WALL_STAIRS = regBlockItem("starship_wall_stairs", ModBlocks.STARSHIP_WALL_STAIRS, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_WALL_SLAB = regBlockItem("starship_wall_slab", ModBlocks.STARSHIP_WALL_SLAB, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_HULL_STAIRS = regBlockItem("starship_hull_stairs", ModBlocks.STARSHIP_HULL_STAIRS, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_HULL_SLAB = regBlockItem("starship_hull_slab", ModBlocks.STARSHIP_HULL_SLAB, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_GRATE_FLOOR = regBlockItem("starship_grate_floor", ModBlocks.STARSHIP_GRATE_FLOOR, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_WHITE_PANEL = regBlockItem("starship_white_panel", ModBlocks.STARSHIP_WHITE_PANEL, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_WHITE_SLAB = regBlockItem("starship_white_slab", ModBlocks.STARSHIP_WHITE_SLAB, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_DARK_RIB = regBlockItem("starship_dark_rib", ModBlocks.STARSHIP_DARK_RIB, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_CEILING_TILE = regBlockItem("starship_ceiling_tile", ModBlocks.STARSHIP_CEILING_TILE, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_DAMAGED_HULL = regBlockItem("starship_damaged_hull", ModBlocks.STARSHIP_DAMAGED_HULL, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_RAILING = regBlockItem("starship_railing", ModBlocks.STARSHIP_RAILING, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_VIEWPORT_PANE = regBlockItem("starship_viewport_pane", ModBlocks.STARSHIP_VIEWPORT_PANE, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_AIRLOCK_DOOR = regBlockItem("starship_airlock_door", ModBlocks.STARSHIP_AIRLOCK_DOOR, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_VENT_HATCH = regBlockItem("starship_vent_hatch", ModBlocks.STARSHIP_VENT_HATCH, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> CYAN_LIGHT_STRIP = regBlockItem("cyan_light_strip", ModBlocks.CYAN_LIGHT_STRIP, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> AMBER_LIGHT_STRIP = regBlockItem("amber_light_strip", ModBlocks.AMBER_LIGHT_STRIP, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_HULL_DARK = regBlockItem("starship_hull_dark", ModBlocks.STARSHIP_HULL_DARK, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_ACCENT_BLUE = regBlockItem("starship_accent_blue", ModBlocks.STARSHIP_ACCENT_BLUE, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_ACCENT_ORANGE = regBlockItem("starship_accent_orange", ModBlocks.STARSHIP_ACCENT_ORANGE, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_WALL_VENT = regBlockItem("starship_wall_vent", ModBlocks.STARSHIP_WALL_VENT, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_REINFORCED_GLASS = regBlockItem("starship_reinforced_glass", ModBlocks.STARSHIP_REINFORCED_GLASS, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_CATWALK_SLAB = regBlockItem("starship_catwalk_slab", ModBlocks.STARSHIP_CATWALK_SLAB, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_HAZARD_SLAB = regBlockItem("starship_hazard_slab", ModBlocks.STARSHIP_HAZARD_SLAB, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_PILLAR_LIGHT = regBlockItem("starship_pillar_light", ModBlocks.STARSHIP_PILLAR_LIGHT, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_BULKHEAD_WALL = regBlockItem("starship_bulkhead_wall", ModBlocks.STARSHIP_BULKHEAD_WALL, ModCreativeTabs.TAB_STARSHIP);
    public static final RegistryObject<Item> STARSHIP_LADDER = regBlockItem("starship_ladder", ModBlocks.STARSHIP_LADDER, ModCreativeTabs.TAB_STARSHIP);

    // ============================================================
    // 3. TAB_PLANET (34): Polar, Outpost, Mine & Geothermal
    // ============================================================
    public static final RegistryObject<Item> PERMAFROST_ROCK = regBlockItem("permafrost_rock", ModBlocks.PERMAFROST_ROCK, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GLACIER_SNOW = regBlockItem("glacier_snow", ModBlocks.GLACIER_SNOW, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> FROZEN_ORE_VEIN = regBlockItem("frozen_ore_vein", ModBlocks.FROZEN_ORE_VEIN, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> PERMAFROST_STAIRS = regBlockItem("permafrost_stairs", ModBlocks.PERMAFROST_STAIRS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> PERMAFROST_SLAB = regBlockItem("permafrost_slab", ModBlocks.PERMAFROST_SLAB, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> PERMAFROST_BRICKS = regBlockItem("permafrost_bricks", ModBlocks.PERMAFROST_BRICKS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> PERMAFROST_BRICKS_STAIRS = regBlockItem("permafrost_bricks_stairs", ModBlocks.PERMAFROST_BRICKS_STAIRS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> PERMAFROST_BRICKS_SLAB = regBlockItem("permafrost_bricks_slab", ModBlocks.PERMAFROST_BRICKS_SLAB, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GLACIER_ICE_BLOCK = regBlockItem("glacier_ice_block", ModBlocks.GLACIER_ICE_BLOCK, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> SNOW_PACKED_SLAB = regBlockItem("snow_packed_slab", ModBlocks.SNOW_PACKED_SLAB, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_INSULATED_WALL = regBlockItem("outpost_insulated_wall", ModBlocks.OUTPOST_INSULATED_WALL, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_INSULATED_SLAB = regBlockItem("outpost_insulated_slab", ModBlocks.OUTPOST_INSULATED_SLAB, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_ROOF_PLATING = regBlockItem("outpost_roof_plating", ModBlocks.OUTPOST_ROOF_PLATING, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_ROOF_STAIRS = regBlockItem("outpost_roof_stairs", ModBlocks.OUTPOST_ROOF_STAIRS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> MINE_STEEL_BEAM = regBlockItem("mine_steel_beam", ModBlocks.MINE_STEEL_BEAM, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> MINE_STEEL_TRUSS = regBlockItem("mine_steel_truss", ModBlocks.MINE_STEEL_TRUSS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> MINE_RUSTED_PLATE = regBlockItem("mine_rusted_plate", ModBlocks.MINE_RUSTED_PLATE, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> MINE_CONCRETE_FLOOR = regBlockItem("mine_concrete_floor", ModBlocks.MINE_CONCRETE_FLOOR, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GEOTHERMAL_BASALT = regBlockItem("geothermal_basalt", ModBlocks.GEOTHERMAL_BASALT, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GEOTHERMAL_BASALT_STAIRS = regBlockItem("geothermal_basalt_stairs", ModBlocks.GEOTHERMAL_BASALT_STAIRS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GEOTHERMAL_BASALT_SLAB = regBlockItem("geothermal_basalt_slab", ModBlocks.GEOTHERMAL_BASALT_SLAB, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GEOTHERMAL_WARM_STONE = regBlockItem("geothermal_warm_stone", ModBlocks.GEOTHERMAL_WARM_STONE, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GEOTHERMAL_STEAM_VENT = regBlockItem("geothermal_steam_vent", ModBlocks.GEOTHERMAL_STEAM_VENT, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GEOTHERMAL_GLOW_MOSS = regBlockItem("geothermal_glow_moss", ModBlocks.GEOTHERMAL_GLOW_MOSS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> POLAR_PACKED_SNOW = regBlockItem("polar_packed_snow", ModBlocks.POLAR_PACKED_SNOW, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> POLAR_ICE_BRICKS = regBlockItem("polar_ice_bricks", ModBlocks.POLAR_ICE_BRICKS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_YELLOW_WALL = regBlockItem("outpost_yellow_wall", ModBlocks.OUTPOST_YELLOW_WALL, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_DARK_TRIM = regBlockItem("outpost_dark_trim", ModBlocks.OUTPOST_DARK_TRIM, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_WINDOW_GLASS = regBlockItem("outpost_window_glass", ModBlocks.OUTPOST_WINDOW_GLASS, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> OUTPOST_CORRUGATED_SLAB = regBlockItem("outpost_corrugated_slab", ModBlocks.OUTPOST_CORRUGATED_SLAB, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> MINE_SUPPORT_WALL = regBlockItem("mine_support_wall", ModBlocks.MINE_SUPPORT_WALL, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> MINE_CONCRETE_SLAB = regBlockItem("mine_concrete_slab", ModBlocks.MINE_CONCRETE_SLAB, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> MINE_CAUTION_CONCRETE = regBlockItem("mine_caution_concrete", ModBlocks.MINE_CAUTION_CONCRETE, ModCreativeTabs.TAB_PLANET);
    public static final RegistryObject<Item> GEOTHERMAL_MAGMA_CRUST = regBlockItem("geothermal_magma_crust", ModBlocks.GEOTHERMAL_MAGMA_CRUST, ModCreativeTabs.TAB_PLANET);

    // ============================================================
    // 4. TAB_RUINS (28): Alien Obsidian Ruins, Hardlight & Ark Harbor
    // ============================================================
    public static final RegistryObject<Item> RUIN_OBSIDIAN_BLOCK = regBlockItem("ruin_obsidian_block", ModBlocks.RUIN_OBSIDIAN_BLOCK, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_OBSIDIAN_STAIRS = regBlockItem("ruin_obsidian_stairs", ModBlocks.RUIN_OBSIDIAN_STAIRS, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_OBSIDIAN_SLAB = regBlockItem("ruin_obsidian_slab", ModBlocks.RUIN_OBSIDIAN_SLAB, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_PLATINUM_TRIM = regBlockItem("ruin_platinum_trim", ModBlocks.RUIN_PLATINUM_TRIM, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_PLATINUM_STAIRS = regBlockItem("ruin_platinum_stairs", ModBlocks.RUIN_PLATINUM_STAIRS, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_PLATINUM_SLAB = regBlockItem("ruin_platinum_slab", ModBlocks.RUIN_PLATINUM_SLAB, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_CARVED_GLYPH = regBlockItem("ruin_carved_glyph", ModBlocks.RUIN_CARVED_GLYPH, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_CIRCUIT_TEAL = regBlockItem("ruin_circuit_teal", ModBlocks.RUIN_CIRCUIT_TEAL, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_CIRCUIT_GOLD = regBlockItem("ruin_circuit_gold", ModBlocks.RUIN_CIRCUIT_GOLD, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_PILLAR = regBlockItem("ruin_pillar", ModBlocks.RUIN_PILLAR, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_FLOOR_TILE = regBlockItem("ruin_floor_tile", ModBlocks.RUIN_FLOOR_TILE, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_ENERGY_CORE = regBlockItem("ruin_energy_core", ModBlocks.RUIN_ENERGY_CORE, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_HARDLIGHT_BRIDGE = regBlockItem("ruin_hardlight_bridge", ModBlocks.RUIN_HARDLIGHT_BRIDGE, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_HARDLIGHT_BARRIER = regBlockItem("ruin_hardlight_barrier", ModBlocks.RUIN_HARDLIGHT_BARRIER, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_ARK_HULL = regBlockItem("ruin_ark_hull", ModBlocks.RUIN_ARK_HULL, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_ARK_STAIRS = regBlockItem("ruin_ark_stairs", ModBlocks.RUIN_ARK_STAIRS, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_ARK_SLAB = regBlockItem("ruin_ark_slab", ModBlocks.RUIN_ARK_SLAB, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_DOCKING_BEACON = regBlockItem("ruin_docking_beacon", ModBlocks.RUIN_DOCKING_BEACON, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_ANCIENT_STELE = regBlockItem("ruin_ancient_stele", ModBlocks.RUIN_ANCIENT_STELE, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_FLOATING_PRISM = regBlockItem("ruin_floating_prism", ModBlocks.RUIN_FLOATING_PRISM, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_OBSIDIAN_WALL = regBlockItem("ruin_obsidian_wall", ModBlocks.RUIN_OBSIDIAN_WALL, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_PLATINUM_WALL = regBlockItem("ruin_platinum_wall", ModBlocks.RUIN_PLATINUM_WALL, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_CRACKED_OBSIDIAN = regBlockItem("ruin_cracked_obsidian", ModBlocks.RUIN_CRACKED_OBSIDIAN, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_MOSSY_OBSIDIAN = regBlockItem("ruin_mossy_obsidian", ModBlocks.RUIN_MOSSY_OBSIDIAN, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_HEX_FLOOR = regBlockItem("ruin_hex_floor", ModBlocks.RUIN_HEX_FLOOR, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_ENERGY_CONDUIT = regBlockItem("ruin_energy_conduit", ModBlocks.RUIN_ENERGY_CONDUIT, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_PORTAL_FRAME = regBlockItem("ruin_portal_frame", ModBlocks.RUIN_PORTAL_FRAME, ModCreativeTabs.TAB_RUINS);
    public static final RegistryObject<Item> RUIN_ARK_GLOW_PANEL = regBlockItem("ruin_ark_glow_panel", ModBlocks.RUIN_ARK_GLOW_PANEL, ModCreativeTabs.TAB_RUINS);

    // ============================================================
    // Legacy & Base Valuables / Consumables
    // ============================================================
    public static final RegistryObject<Item> QUANTUM_CORE = ITEMS.register("quantum_core",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> ALLOY_PLATE = ITEMS.register("alloy_plate",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> PRECURSOR_SPARK = ITEMS.register("precursor_spark",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(16)));

    // v2.0 Currencies & Progression Consumables
    public static final RegistryObject<Item> CREDIT_COIN = ITEMS.register("credit_coin",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> EXP_BOTTLE = ITEMS.register("exp_bottle",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> EQUIP_ESSENCE = ITEMS.register("equip_essence",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> STAR_TICKET = ITEMS.register("star_ticket",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    // Character Ascension Materials (Tier 1..6)
    public static final RegistryObject<Item> CHAR_MAT_1 = registerMat("char_mat_1");
    public static final RegistryObject<Item> CHAR_MAT_2 = registerMat("char_mat_2");
    public static final RegistryObject<Item> CHAR_MAT_3 = registerMat("char_mat_3");
    public static final RegistryObject<Item> CHAR_MAT_4 = registerMat("char_mat_4");
    public static final RegistryObject<Item> CHAR_MAT_5 = registerMat("char_mat_5");
    public static final RegistryObject<Item> CHAR_MAT_6 = registerMat("char_mat_6");

    // Weapon Ascension Materials (Tier 1..6)
    public static final RegistryObject<Item> WEAPON_MAT_1 = registerMat("weapon_mat_1");
    public static final RegistryObject<Item> WEAPON_MAT_2 = registerMat("weapon_mat_2");
    public static final RegistryObject<Item> WEAPON_MAT_3 = registerMat("weapon_mat_3");
    public static final RegistryObject<Item> WEAPON_MAT_4 = registerMat("weapon_mat_4");
    public static final RegistryObject<Item> WEAPON_MAT_5 = registerMat("weapon_mat_5");
    public static final RegistryObject<Item> WEAPON_MAT_6 = registerMat("weapon_mat_6");

    // Talent Upgrade Materials (Tier 1..6)
    public static final RegistryObject<Item> TALENT_MAT_1 = registerMat("talent_mat_1");
    public static final RegistryObject<Item> TALENT_MAT_2 = registerMat("talent_mat_2");
    public static final RegistryObject<Item> TALENT_MAT_3 = registerMat("talent_mat_3");
    public static final RegistryObject<Item> TALENT_MAT_4 = registerMat("talent_mat_4");
    public static final RegistryObject<Item> TALENT_MAT_5 = registerMat("talent_mat_5");
    public static final RegistryObject<Item> TALENT_MAT_6 = registerMat("talent_mat_6");

    private static RegistryObject<Item> registerMat(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));
    }

    // Weapons (Earth, Fire, Water, Air, 5-star Heavy)
    public static final RegistryObject<Item> PLASMA_CUTTER = ITEMS.register("plasma_cutter",
            () -> new TechWeaponItem(4, -2.4f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> CRYO_SAWBLADE = ITEMS.register("cryo_sawblade",
            () -> new TechWeaponItem(5, -2.3f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> HYDRO_PULSE_GUN = ITEMS.register("hydro_pulse_gun",
            () -> new TechWeaponItem(4, -2.2f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> AERO_BLADE = ITEMS.register("aero_blade",
            () -> new TechWeaponItem(4, -2.1f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> PRECURSOR_CLAYMORE = ITEMS.register("precursor_claymore",
            () -> new TechWeaponItem(8, -2.2f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    // 5-Slot Equipment Chips
    public static final RegistryObject<Item> FUSION_CORE_CHIP = ITEMS.register("fusion_core_chip",
            () -> new TechChipItem(0, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> VOID_DRIVE_CHIP = ITEMS.register("void_drive_chip",
            () -> new TechChipItem(1, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> SERVO_BATTERY_CHIP = ITEMS.register("servo_battery_chip",
            () -> new TechChipItem(2, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> OVERCLOCK_CHIP = ITEMS.register("overclock_chip",
            () -> new TechChipItem(3, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> NEURAL_HALO_CHIP = ITEMS.register("neural_halo_chip",
            () -> new TechChipItem(4, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    // Material Tier Helpers (1..6)
    public static Item getCharMat(int tier) {
        return switch (tier) {
            case 2 -> CHAR_MAT_2.get();
            case 3 -> CHAR_MAT_3.get();
            case 4 -> CHAR_MAT_4.get();
            case 5 -> CHAR_MAT_5.get();
            case 6 -> CHAR_MAT_6.get();
            default -> CHAR_MAT_1.get();
        };
    }

    public static Item getWeaponMat(int tier) {
        return switch (tier) {
            case 2 -> WEAPON_MAT_2.get();
            case 3 -> WEAPON_MAT_3.get();
            case 4 -> WEAPON_MAT_4.get();
            case 5 -> WEAPON_MAT_5.get();
            case 6 -> WEAPON_MAT_6.get();
            default -> WEAPON_MAT_1.get();
        };
    }

    public static Item getTalentMat(int tier) {
        return switch (tier) {
            case 2 -> TALENT_MAT_2.get();
            case 3 -> TALENT_MAT_3.get();
            case 4 -> TALENT_MAT_4.get();
            case 5 -> TALENT_MAT_5.get();
            case 6 -> TALENT_MAT_6.get();
            default -> TALENT_MAT_1.get();
        };
    }

    public static String getMaterialLine(Item item) {
        for (int t = 1; t <= 6; t++) {
            if (item == getCharMat(t)) return "char";
            if (item == getWeaponMat(t)) return "weapon";
            if (item == getTalentMat(t)) return "talent";
        }
        return null;
    }

    public static int getMaterialTier(Item item) {
        for (int t = 1; t <= 6; t++) {
            if (item == getCharMat(t) || item == getWeaponMat(t) || item == getTalentMat(t)) {
                return t;
            }
        }
        return 0;
    }

    public static Item getNextTierMaterial(Item item) {
        String line = getMaterialLine(item);
        int tier = getMaterialTier(item);
        if (line == null || tier <= 0 || tier >= 6) return null;
        return switch (line) {
            case "char" -> getCharMat(tier + 1);
            case "weapon" -> getWeaponMat(tier + 1);
            case "talent" -> getTalentMat(tier + 1);
            default -> null;
        };
    }

    public static boolean isMaterialItem(Item item) {
        return getMaterialLine(item) != null
                || item == ALLOY_PLATE.get()
                || item == EQUIP_ESSENCE.get()
                || item == EXP_BOTTLE.get();
    }
}
