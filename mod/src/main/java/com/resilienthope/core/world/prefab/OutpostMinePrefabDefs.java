package com.resilienthope.core.world.prefab;

import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.world.prefab.PrefabRegistry.Category;
import com.resilienthope.core.world.prefab.PrefabRegistry.PrefabBuilder;
import com.resilienthope.core.world.prefab.PrefabRegistry.PrefabDef;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Polar Outpost, Power Industry, Deep Mine, and Geothermal modular prefabs (12 prefabs).
 */
final class OutpostMinePrefabDefs {
    private OutpostMinePrefabDefs() {}

    static void registerAll() {
        // --- OUTPOST & INDUSTRY (7) ---
        PrefabRegistry.register(new PrefabDef(
                "outpost_module_hut", "极地保温模块化站房", Category.OUTPOST,
                13, 9, 11, 12,
                "白橙保温墙体、波纹坡屋顶、极地工作灯与办公台组成的标准化科研营房。",
                OutpostMinePrefabDefs::buildOutpostHut));

        PrefabRegistry.register(new PrefabDef(
                "outpost_connector", "极地架空封闭连廊", Category.OUTPOST,
                7, 6, 8, 8,
                "离地架空支柱 + 防寒双层玻璃的保温通道，可直接连接各栋科研站房。",
                OutpostMinePrefabDefs::buildOutpostConnector));

        PrefabRegistry.register(new PrefabDef(
                "outpost_radar_tower", "极地气象雷达通讯塔", Category.OUTPOST,
                9, 21, 9, 12,
                "高耸工字钢塔架与多方向雷达天线的通讯地标建筑，顶部配警示信标。",
                OutpostMinePrefabDefs::buildRadarTower));

        PrefabRegistry.register(new PrefabDef(
                "outpost_supply_depot", "极地物资停机补给棚", Category.OUTPOST,
                15, 8, 13, 16,
                "带黄色工程顶棚、成堆货物栈板、太阳能板与探照灯的前哨后勤站。",
                OutpostMinePrefabDefs::buildSupplyDepot));

        PrefabRegistry.register(new PrefabDef(
                "industry_power_pylon", "高压输电铁塔", Category.OUTPOST,
                7, 19, 7, 24,
                "工业级镂空铁桁架高压电塔，默认步距 24 格，一次拉出整条万格大陆输电线！",
                OutpostMinePrefabDefs::buildPowerPylon));

        PrefabRegistry.register(new PrefabDef(
                "industry_pipeline_span", "地热保温输送管廊段", Category.OUTPOST,
                7, 7, 10, 10,
                "地热区向科考站输热的架空双管管廊模块，带检修平台与黄色管架。",
                OutpostMinePrefabDefs::buildPipelineSpan));

        PrefabRegistry.register(new PrefabDef(
                "polar_wreck_rib", "失事飞船断裂肋骨残骸", Category.OUTPOST,
                13, 9, 9, 11,
                "半埋入积雪的焦黑弯曲星舰骨架残骸与火花黑匣子布景，快速铺设坠机现场。",
                OutpostMinePrefabDefs::buildWreckRib));

        // --- MINE & GEOTHERMAL (5) ---
        PrefabRegistry.register(new PrefabDef(
                "mine_tunnel_arch", "矿井钢梁支护巷道段", Category.MINE_GEO,
                9, 7, 8, 8,
                "自动掏空岩层并用工字钢拱架与锈铁护墙支护的标准矿道段，内置照明与电缆。",
                OutpostMinePrefabDefs::buildMineTunnel));

        PrefabRegistry.register(new PrefabDef(
                "mine_gantry_tower", "矿区重型井架龙门塔", Category.MINE_GEO,
                15, 18, 15, 18,
                "矿井升降井口重型工字钢龙门井架，配套爬梯直达顶部维修天车平台。",
                OutpostMinePrefabDefs::buildMineGantry));

        PrefabRegistry.register(new PrefabDef(
                "mine_catwalk_bridge", "矿坑/裂谷悬空钢索栈桥", Category.MINE_GEO,
                7, 6, 10, 10,
                "镂空铁格栅桥面 + 金属安全护栏 + 桥头探照灯，一键跨越大裂谷或深矿井。",
                OutpostMinePrefabDefs::buildMineCatwalk));

        PrefabRegistry.register(new PrefabDef(
                "geo_steam_pool", "地下地热温泉池基座", Category.MINE_GEO,
                13, 5, 13, 14,
                "玄武岩围边、温热裂隙石衬底、蒸汽孔与荧光苔藓点缀的温泉水池模块。",
                OutpostMinePrefabDefs::buildGeoPool));

        PrefabRegistry.register(new PrefabDef(
                "geo_basalt_arch", "地热裂谷天然玄武岩拱桥", Category.MINE_GEO,
                11, 11, 9, 11,
                "跨度 9 格的暗色玄武岩自然石拱，点缀微弱荧光苔藓与地热裂纹。",
                OutpostMinePrefabDefs::buildGeoArch));
    }

    private static void buildOutpostHut(PrefabBuilder b) {
        Block trim = ModBlocks.OUTPOST_DARK_TRIM.get();
        Block wall = ModBlocks.OUTPOST_INSULATED_WALL.get();
        Block yellowWall = ModBlocks.OUTPOST_YELLOW_WALL.get();
        Block glass = ModBlocks.OUTPOST_WINDOW_GLASS.get();
        Block roof = ModBlocks.OUTPOST_ROOF_PLATING.get();
        Block roofStair = ModBlocks.OUTPOST_ROOF_STAIRS.get();
        Block floor = ModBlocks.STARSHIP_FLOOR.get();

        // Raised insulated foundation stilts
        b.fill(0, 0, 0, 12, 0, 10, trim);
        b.fill(1, 0, 1, 11, 0, 9, floor);
        b.air(1, 1, 1, 11, 5, 9);

        // Walls y=1..4
        b.boxWall(0, 1, 0, 12, 4, 10, wall);
        b.boxWall(0, 1, 0, 12, 1, 10, yellowWall);
        for (int x : new int[]{0, 12}) {
            for (int z : new int[]{0, 10}) {
                b.fill(x, 1, z, x, 4, z, trim);
            }
        }
        b.fill(3, 2, 0, 4, 3, 0, glass);
        b.fill(8, 2, 0, 9, 3, 0, glass);
        b.fill(3, 2, 10, 9, 3, 10, glass);
        b.fill(0, 2, 3, 0, 3, 7, glass);
        b.fill(12, 2, 3, 12, 3, 7, glass);

        // Front doorway at x=6, z=0
        b.door(6, 1, 0, ModBlocks.STARSHIP_AIRLOCK_DOOR.get(), Direction.SOUTH, DoorHingeSide.LEFT);

        // Gabled corrugated roof y=5..7
        for (int z = 0; z < b.sizeZ; z++) {
            b.stair(0, 5, z, roofStair, Direction.EAST, Half.BOTTOM);
            b.stair(12, 5, z, roofStair, Direction.WEST, Half.BOTTOM);
            b.fill(1, 5, z, 11, 5, z, ModBlocks.STARSHIP_CEILING_TILE.get());
            b.fill(2, 6, z, 10, 6, z, roof);
            b.fill(4, 7, z, 8, 7, z, roof);
        }
        b.set(4, 5, 5, ModBlocks.STARSHIP_LIGHT.get());
        b.set(8, 5, 5, ModBlocks.STARSHIP_LIGHT.get());
        b.prop(6, 8, 5, ModBlocks.PROP_ANTENNA_MAST.get(), Direction.SOUTH);

        // Interior gear
        b.prop(2, 1, 8, ModBlocks.PROP_LAB_DESK.get(), Direction.NORTH);
        b.prop(4, 1, 8, ModBlocks.PROP_SERVER_RACK.get(), Direction.NORTH);
        b.prop(10, 1, 8, ModBlocks.PROP_STORAGE_RACK.get(), Direction.WEST);
        b.prop(10, 1, 3, ModBlocks.PROP_MEDICAL_BED.get(), Direction.WEST);
    }

    private static void buildOutpostConnector(PrefabBuilder b) {
        Block trim = ModBlocks.OUTPOST_DARK_TRIM.get();
        Block wall = ModBlocks.OUTPOST_INSULATED_WALL.get();
        Block glass = ModBlocks.OUTPOST_WINDOW_GLASS.get();
        Block roof = ModBlocks.OUTPOST_ROOF_PLATING.get();

        // Support stilts on ends
        for (int z : new int[]{0, 7}) {
            b.fill(1, 0, z, 1, 1, z, trim);
            b.fill(5, 0, z, 5, 1, z, trim);
        }
        b.fill(1, 1, 0, 5, 1, 7, ModBlocks.STARSHIP_FLOOR.get());
        for (int z = 0; z < b.sizeZ; z++) {
            b.air(2, 2, z, 4, 4, z);
            b.fill(1, 2, z, 1, 4, z, (z % 3 == 0) ? wall : glass);
            b.fill(5, 2, z, 5, 4, z, (z % 3 == 0) ? wall : glass);
            b.fill(1, 5, z, 5, 5, z, roof);
            if (z % 4 == 2) b.set(3, 5, z, ModBlocks.STARSHIP_LIGHT.get());
        }
    }

    private static void buildRadarTower(PrefabBuilder b) {
        Block beam = ModBlocks.MINE_STEEL_BEAM.get();
        Block truss = ModBlocks.MINE_STEEL_TRUSS.get();
        Block trim = ModBlocks.OUTPOST_DARK_TRIM.get();
        Block rail = ModBlocks.STARSHIP_RAILING.get();

        // Concrete base 9x9
        b.fill(0, 0, 0, 8, 0, 8, ModBlocks.MINE_CONCRETE_FLOOR.get());
        b.fill(1, 0, 1, 7, 0, 7, ModBlocks.MINE_CAUTION_CONCRETE.get());
        b.fill(2, 1, 2, 6, 1, 6, trim);

        // Steel lattice legs Y=2..14
        for (int y = 2; y <= 14; y++) {
            int inset = y >= 10 ? 3 : 2;
            for (int x : new int[]{inset, 8 - inset}) {
                for (int z : new int[]{inset, 8 - inset}) {
                    b.pillar(x, y, z, beam, Direction.Axis.Y);
                }
            }
            if (y % 4 == 0) {
                b.boxWall(inset, y, inset, 8 - inset, y, 8 - inset, truss);
            }
        }
        // Central ladder core
        b.fill(4, 1, 4, 4, 14, 4, trim);
        b.ladder(4, 1, 14, 3, ModBlocks.STARSHIP_LADDER.get(), Direction.NORTH);

        // Observation & radar dish platform Y=15..20
        b.fill(1, 15, 1, 7, 15, 7, ModBlocks.STARSHIP_HULL_DARK.get());
        b.boxWall(1, 16, 1, 7, 16, 7, rail);
        b.set(4, 16, 3, Blocks.AIR); // ladder exit
        b.pillarCol(4, 16, 19, 4, truss);
        b.set(4, 20, 4, ModBlocks.ALARM_BEACON.get());
        b.set(2, 17, 2, ModBlocks.PROP_ANTENNA_MAST.get());
        b.set(6, 17, 6, ModBlocks.PROP_SOLAR_PANEL.get());
        b.prop(2, 16, 6, ModBlocks.PROP_FLOODLIGHT.get(), Direction.SOUTH);
    }

    private static void buildSupplyDepot(PrefabBuilder b) {
        Block concrete = ModBlocks.MINE_CONCRETE_FLOOR.get();
        Block caution = ModBlocks.MINE_CAUTION_CONCRETE.get();
        Block beam = ModBlocks.MINE_STEEL_BEAM.get();
        Block truss = ModBlocks.MINE_STEEL_TRUSS.get();
        Block roof = ModBlocks.OUTPOST_CORRUGATED_SLAB.get();

        b.fill(0, 0, 0, 14, 0, 12, concrete);
        b.boxWall(1, 0, 1, 13, 0, 11, caution);
        for (int x : new int[]{1, 13}) {
            for (int z : new int[]{1, 6, 11}) {
                b.pillarCol(x, 1, 5, z, beam);
            }
        }
        // Overhead open canopy
        for (int x = 1; x <= 13; x++) {
            for (int z = 0; z <= 12; z++) {
                boolean rib = (z == 1 || z == 6 || z == 11);
                b.set(x, 6, z, rib ? truss : roof.defaultBlockState());
            }
        }
        b.set(7, 6, 3, ModBlocks.STARSHIP_LIGHT.get());
        b.set(7, 6, 9, ModBlocks.STARSHIP_LIGHT.get());
        b.prop(3, 1, 3, ModBlocks.PROP_SUPPLY_PALLET.get(), Direction.EAST);
        b.prop(3, 1, 5, ModBlocks.PROP_SUPPLY_PALLET.get(), Direction.EAST);
        b.set(11, 1, 4, ModBlocks.PROP_CARGO_BARREL.get());
        b.set(11, 1, 5, ModBlocks.PROP_CARGO_BARREL.get());
        b.prop(11, 1, 8, ModBlocks.PROP_SOLAR_PANEL.get(), Direction.WEST);
        b.prop(2, 1, 10, ModBlocks.PROP_FLOODLIGHT.get(), Direction.NORTH);
    }

    private static void buildPowerPylon(PrefabBuilder b) {
        Block truss = ModBlocks.MINE_STEEL_TRUSS.get();
        Block beam = ModBlocks.MINE_STEEL_BEAM.get();
        Block core = ModBlocks.AUXILIARY_ENERGY_CORE.get();

        // 4 tapered steel legs
        b.fill(0, 0, 0, 1, 0, 1, beam);
        b.fill(5, 0, 0, 6, 0, 1, beam);
        b.fill(0, 0, 5, 1, 0, 6, beam);
        b.fill(5, 0, 5, 6, 0, 6, beam);
        for (int y = 1; y <= 17; y++) {
            int o = (y < 6) ? 1 : 2;
            for (int x : new int[]{o, 6 - o}) {
                for (int z : new int[]{o, 6 - o}) {
                    b.set(x, y, z, truss);
                }
            }
            if (y == 6 || y == 12 || y == 17) {
                b.boxWall(o, y, o, 6 - o, y, 6 - o, truss);
            }
        }
        // Horizontal insulator crossarms at y=13 and y=16
        b.fill(0, 13, 3, 6, 13, 3, truss);
        b.fill(0, 16, 3, 6, 16, 3, truss);
        b.set(0, 12, 3, ModBlocks.STARSHIP_LIGHT.get());
        b.set(6, 12, 3, ModBlocks.STARSHIP_LIGHT.get());
        b.set(3, 18, 3, ModBlocks.ALARM_BEACON.get());
        b.set(3, 1, 3, core);
    }

    private static void buildPipelineSpan(PrefabBuilder b) {
        Block beam = ModBlocks.MINE_STEEL_BEAM.get();
        Block pipe = ModBlocks.PROP_CABLE_PIPE.get();
        Block warm = ModBlocks.OUTPOST_YELLOW_WALL.get();
        Block catwalk = ModBlocks.STARSHIP_CATWALK_SLAB.get();

        for (int z = 0; z < b.sizeZ; z++) {
            if (z == 1 || z == 8) {
                b.pillarCol(1, 0, 5, z, beam);
                b.pillarCol(5, 0, 5, z, beam);
                b.fill(1, 3, z, 5, 3, z, beam);
            }
            b.set(2, 4, z, warm);
            b.set(4, 4, z, warm);
            b.slab(3, 4, z, catwalk, SlabType.TOP);
            b.prop(3, 5, z, pipe, Direction.SOUTH);
        }
    }

    private static void buildWreckRib(PrefabBuilder b) {
        Block burnt = ModBlocks.STARSHIP_DAMAGED_HULL.get();
        Block rib = ModBlocks.STARSHIP_DARK_RIB.get();
        Block hull = ModBlocks.STARSHIP_HULL_DARK.get();
        Block hazard = ModBlocks.HAZARD_STRIPE_BLOCK.get();

        b.fill(1, 0, 1, 11, 0, 7, ModBlocks.GLACIER_SNOW.get());
        b.fill(3, 0, 2, 9, 0, 6, burnt);
        for (int z : new int[]{1, 4, 7}) {
            int tilt = (z == 4) ? 1 : 0;
            for (int y = 0; y <= 6 + tilt; y++) {
                if (y <= 5) {
                    b.set(2 + (y / 3), y, z, rib);
                    b.set(10 - (y / 3), y, z, (y % 2 == 0) ? burnt : rib);
                } else {
                    b.set(4, y, z, hull);
                    b.set(8, y, z, burnt);
                }
            }
        }
        b.set(6, 1, 3, hazard);
        b.prop(6, 1, 4, ModBlocks.PROP_BLACKBOX_RECORDER.get(), Direction.SOUTH);
        b.set(4, 1, 6, ModBlocks.SUPPLY_CRATE.get());
    }

    private static void buildMineTunnel(PrefabBuilder b) {
        Block rock = ModBlocks.PERMAFROST_ROCK.get();
        Block concrete = ModBlocks.MINE_CONCRETE_FLOOR.get();
        Block beam = ModBlocks.MINE_STEEL_BEAM.get();
        Block wall = ModBlocks.MINE_SUPPORT_WALL.get();
        Block truss = ModBlocks.MINE_STEEL_TRUSS.get();

        for (int z = 0; z < b.sizeZ; z++) {
            boolean frame = (z % 4 == 0);
            b.air(1, 1, z, 7, 5, z);
            b.fill(0, 0, z, 8, 0, z, concrete);
            b.fill(3, 0, z, 5, 0, z, ModBlocks.MINE_CAUTION_CONCRETE.get());
            b.fill(0, 1, z, 0, 6, z, rock);
            b.fill(8, 1, z, 8, 6, z, rock);
            b.fill(1, 6, z, 7, 6, z, rock);
            if (frame) {
                b.pillarCol(1, 1, 5, z, beam);
                b.pillarCol(7, 1, 5, z, beam);
                b.fill(1, 5, z, 7, 5, z, truss);
                b.set(4, 5, z, ModBlocks.STARSHIP_LIGHT.get());
            } else {
                b.set(1, 1, z, wall);
                b.set(7, 1, z, wall);
                b.prop(2, 4, z, ModBlocks.PROP_CABLE_PIPE.get(), Direction.SOUTH);
            }
        }
        b.set(1, 3, 2, ModBlocks.FROZEN_ORE_VEIN.get());
        b.set(7, 3, 6, ModBlocks.FROZEN_ORE_VEIN.get());
    }

    private static void buildMineGantry(PrefabBuilder b) {
        Block concrete = ModBlocks.MINE_CONCRETE_FLOOR.get();
        Block caution = ModBlocks.MINE_CAUTION_CONCRETE.get();
        Block beam = ModBlocks.MINE_STEEL_BEAM.get();
        Block truss = ModBlocks.MINE_STEEL_TRUSS.get();
        Block rust = ModBlocks.MINE_RUSTED_PLATE.get();

        b.fill(0, 0, 0, 14, 0, 14, concrete);
        b.boxWall(4, 0, 4, 10, 0, 10, caution);
        b.air(5, 0, 5, 9, 0, 9); // open shaft center!

        // 4 massive tower columns Y=1..14
        for (int x : new int[]{2, 12}) {
            for (int z : new int[]{2, 12}) {
                b.fill(x, 1, z, x + 0, 14, z + 0, beam);
                b.set(x, 1, z, rust);
            }
        }
        b.boxWall(2, 7, 2, 12, 7, 12, truss);
        b.boxWall(2, 14, 2, 12, 14, 12, beam);
        b.fill(2, 15, 2, 12, 15, 12, rust);
        b.fill(5, 15, 5, 9, 15, 9, ModBlocks.STARSHIP_GRATE_FLOOR.get());
        b.fill(6, 16, 6, 8, 17, 8, truss);
        b.set(7, 16, 7, ModBlocks.ALARM_BEACON.get());
        b.ladder(3, 1, 15, 2, ModBlocks.STARSHIP_LADDER.get(), Direction.EAST);
        b.prop(1, 1, 1, ModBlocks.PROP_FLOODLIGHT.get(), Direction.SOUTH);
        b.prop(13, 1, 13, ModBlocks.PROP_FLOODLIGHT.get(), Direction.NORTH);
    }

    private static void buildMineCatwalk(PrefabBuilder b) {
        Block truss = ModBlocks.MINE_STEEL_TRUSS.get();
        Block grate = ModBlocks.STARSHIP_GRATE_FLOOR.get();
        Block rail = ModBlocks.STARSHIP_RAILING.get();
        Block light = ModBlocks.STARSHIP_LIGHT.get();

        for (int z = 0; z < b.sizeZ; z++) {
            b.fill(1, 1, z, 5, 1, z, grate);
            b.set(1, 1, z, truss);
            b.set(5, 1, z, truss);
            b.set(1, 2, z, rail);
            b.set(5, 2, z, rail);
            if (z == 0 || z == 9) {
                b.pillarCol(1, 2, 5, z, truss);
                b.pillarCol(5, 2, 5, z, truss);
                b.fill(1, 5, z, 5, 5, z, truss);
                b.set(3, 5, z, light);
            }
        }
    }

    private static void buildGeoPool(PrefabBuilder b) {
        Block basalt = ModBlocks.GEOTHERMAL_BASALT.get();
        Block warm = ModBlocks.GEOTHERMAL_WARM_STONE.get();
        Block magma = ModBlocks.GEOTHERMAL_MAGMA_CRUST.get();
        Block moss = ModBlocks.GEOTHERMAL_GLOW_MOSS.get();
        Block vent = ModBlocks.GEOTHERMAL_STEAM_VENT.get();

        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 13; z++) {
                double d = Math.hypot(x - 6.0, z - 6.0);
                if (d <= 4.3) {
                    b.set(x, 0, z, (x + z) % 5 == 0 ? magma : warm);
                    b.set(x, 1, z, Blocks.WATER);
                } else if (d <= 6.2) {
                    b.set(x, 0, z, basalt);
                    b.set(x, 1, z, ((x * 3 + z * 5) % 4 == 0) ? moss : basalt);
                    if ((x == 2 && z == 6) || (x == 10 && z == 6)) {
                        b.set(x, 2, z, vent);
                    }
                }
            }
        }
        b.set(2, 2, 2, ModBlocks.GEOTHERMAL_BASALT_SLAB.get());
        b.set(10, 2, 10, ModBlocks.GEOTHERMAL_BASALT_SLAB.get());
    }

    private static void buildGeoArch(PrefabBuilder b) {
        Block basalt = ModBlocks.GEOTHERMAL_BASALT.get();
        Block warm = ModBlocks.GEOTHERMAL_WARM_STONE.get();
        Block moss = ModBlocks.GEOTHERMAL_GLOW_MOSS.get();

        for (int z = 0; z < b.sizeZ; z++) {
            int rise = Math.round(4.5f * (float) Math.sin(Math.PI * z / (b.sizeZ - 1)));
            int yBase = 4 + rise;
            for (int x = 2; x <= 8; x++) {
                b.set(x, yBase, z, basalt);
                b.set(x, yBase + 1, z, (x == 2 || x == 8) ? ((x + z) % 3 == 0 ? moss : basalt) : warm);
            }
            if (z <= 1 || z >= b.sizeZ - 2) {
                for (int y = 0; y < yBase; y++) {
                    b.fill(1, y, z, 3, y, z, basalt);
                    b.fill(7, y, z, 9, y, z, basalt);
                }
            }
        }
    }
}
