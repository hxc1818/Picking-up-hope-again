package com.resilienthope.core.world.prefab;

import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.world.prefab.PrefabRegistry.Category;
import com.resilienthope.core.world.prefab.PrefabRegistry.PrefabBuilder;
import com.resilienthope.core.world.prefab.PrefabRegistry.PrefabDef;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Ancient Alien Ruins, Ark Harbor, and Polar Terrain Stamps (7 prefabs).
 */
final class RuinTerrainPrefabDefs {
    private RuinTerrainPrefabDefs() {}

    static void registerAll() {
        PrefabRegistry.register(new PrefabDef(
                "ruin_hardlight_bridge", "外星遗迹硬光悬浮桥段", Category.RUINS_TERRAIN,
                9, 5, 10, 10,
                "半透明硬光主桥面 + 白金饰边与硬光能量护栏，跨渊秒搭。",
                RuinTerrainPrefabDefs::buildHardlightBridge));

        PrefabRegistry.register(new PrefabDef(
                "ruin_monolith_gate", "远古黑曜白金巨型拱门", Category.RUINS_TERRAIN,
                15, 15, 5, 10,
                "15格高威严外星神殿门楼，黑曜石主柱配白金饰线、青/金双色能量电路与悬浮棱柱。",
                RuinTerrainPrefabDefs::buildMonolithGate));

        PrefabRegistry.register(new PrefabDef(
                "ruin_obelisk_pillar", "外星悬浮符文方尖碑", Category.RUINS_TERRAIN,
                7, 19, 7, 16,
                "带能量中继底座的渐缩黑曜方尖碑，高空悬停白金环与发光能量核心。",
                RuinTerrainPrefabDefs::buildObeliskPillar));

        PrefabRegistry.register(new PrefabDef(
                "ruin_sanctuary_pad", "遗迹八角仪式 / 战斗广场", Category.RUINS_TERRAIN,
                19, 4, 19, 22,
                "直径 19 格对称几何地砖与白金护栏广场，适合遗迹主神殿中心与 Boss 战前坪。",
                RuinTerrainPrefabDefs::buildSanctuaryPad));

        PrefabRegistry.register(new PrefabDef(
                "ruin_ark_dock_arm", "休眠方舟停泊对接巨臂", Category.RUINS_TERRAIN,
                11, 12, 14, 14,
                "暗金休眠方舟停泊港专用重型对接码头臂，内置导航信标与暖金灯带。",
                RuinTerrainPrefabDefs::buildArkDockArm));

        PrefabRegistry.register(new PrefabDef(
                "terrain_ice_spire", "极地天然蓝冰尖峰", Category.RUINS_TERRAIN,
                11, 19, 11, 14,
                "自然渐缩多层冰川地形印章（冻土基座 + 透蓝坚冰核心 + 发光冰晶簇），快速造雪山群。",
                RuinTerrainPrefabDefs::buildIceSpire));

        PrefabRegistry.register(new PrefabDef(
                "terrain_snow_ridge", "冻土雪山脊模块", Category.RUINS_TERRAIN,
                17, 9, 15, 15,
                "宽体极地雪丘与岩石护坡地形块，用于快速勾勒 P1/P3 大陆山地轮廓。",
                RuinTerrainPrefabDefs::buildSnowRidge));
    }

    private static void buildHardlightBridge(PrefabBuilder b) {
        Block obs = ModBlocks.RUIN_OBSIDIAN_BLOCK.get();
        Block trim = ModBlocks.RUIN_PLATINUM_TRIM.get();
        Block bridge = ModBlocks.RUIN_HARDLIGHT_BRIDGE.get();
        Block barrier = ModBlocks.RUIN_HARDLIGHT_BARRIER.get();
        Block circuit = ModBlocks.RUIN_CIRCUIT_TEAL.get();

        for (int z = 0; z < b.sizeZ; z++) {
            b.set(1, 0, z, obs);
            b.set(7, 0, z, obs);
            b.set(1, 1, z, (z == 0 || z == 9) ? circuit : trim);
            b.set(7, 1, z, (z == 0 || z == 9) ? circuit : trim);
            b.fillSlab(2, 1, z, 6, 1, z, bridge, SlabType.TOP);
            b.set(1, 2, z, barrier);
            b.set(7, 2, z, barrier);
            if (z == 0 || z == 9) {
                b.set(1, 3, z, trim);
                b.set(7, 3, z, trim);
                b.set(1, 4, z, ModBlocks.RUIN_FLOATING_PRISM.get());
                b.set(7, 4, z, ModBlocks.RUIN_FLOATING_PRISM.get());
            }
        }
    }

    private static void buildMonolithGate(PrefabBuilder b) {
        Block obs = ModBlocks.RUIN_OBSIDIAN_BLOCK.get();
        Block cracked = ModBlocks.RUIN_CRACKED_OBSIDIAN.get();
        Block trim = ModBlocks.RUIN_PLATINUM_TRIM.get();
        Block frame = ModBlocks.RUIN_PORTAL_FRAME.get();
        Block teal = ModBlocks.RUIN_CIRCUIT_TEAL.get();
        Block gold = ModBlocks.RUIN_CIRCUIT_GOLD.get();
        Block pillar = ModBlocks.RUIN_PILLAR.get();

        b.fill(0, 0, 0, 14, 0, 4, ModBlocks.RUIN_HEX_FLOOR.get());
        // Twin massive piers
        for (int x0 : new int[]{1, 10}) {
            b.fill(x0, 1, 1, x0 + 3, 10, 3, obs);
            b.fill(x0 + 1, 1, 0, x0 + 2, 10, 4, trim);
            b.pillarCol(x0 + 1, 1, 9, 0, pillar);
            b.pillarCol(x0 + 2, 1, 9, 4, pillar);
            b.set(x0, 5, 2, cracked);
            b.set(x0 + 3, 5, 2, cracked);
        }
        // Monumental lintel & floating crown y=10..14
        b.fill(0, 10, 0, 14, 12, 4, obs);
        b.fill(1, 11, 0, 13, 11, 4, trim);
        b.fill(4, 10, 1, 10, 12, 3, frame);
        b.set(7, 11, 0, gold);
        b.set(7, 11, 4, gold);
        b.set(3, 11, 0, teal);
        b.set(11, 11, 0, teal);
        b.set(3, 11, 4, teal);
        b.set(11, 11, 4, teal);
        b.fill(5, 13, 1, 9, 13, 3, obs);
        b.set(7, 14, 2, ModBlocks.RUIN_ENERGY_CORE.get());
        b.prop(5, 1, 2, ModBlocks.PROP_RUIN_RELAY.get(), Direction.SOUTH);
        b.prop(9, 1, 2, ModBlocks.PROP_RUIN_RELAY.get(), Direction.SOUTH);
    }

    private static void buildObeliskPillar(PrefabBuilder b) {
        Block obs = ModBlocks.RUIN_OBSIDIAN_BLOCK.get();
        Block trim = ModBlocks.RUIN_PLATINUM_TRIM.get();
        Block glyph = ModBlocks.RUIN_CARVED_GLYPH.get();
        Block conduit = ModBlocks.RUIN_ENERGY_CONDUIT.get();

        // Stepped base y=0..2
        b.fill(0, 0, 0, 6, 0, 6, obs);
        b.fill(1, 1, 1, 5, 1, 5, trim);
        b.fill(2, 2, 2, 4, 2, 4, obs);
        // Tapered shaft y=3..11
        b.fill(2, 3, 2, 4, 10, 4, obs);
        for (int y : new int[]{4, 8}) {
            b.set(3, y, 2, glyph);
            b.set(3, y, 4, glyph);
            b.set(2, y, 3, glyph);
            b.set(4, y, 3, glyph);
        }
        b.pillarCol(3, 3, 12, 3, conduit);

        // Floating ring & hovering core y=14..18
        b.boxWall(1, 15, 1, 5, 15, 5, trim);
        b.set(3, 15, 3, ModBlocks.RUIN_ENERGY_CORE.get());
        b.set(3, 17, 3, ModBlocks.RUIN_CIRCUIT_GOLD.get());
        b.set(3, 18, 3, ModBlocks.RUIN_FLOATING_PRISM.get());
    }

    private static void buildSanctuaryPad(PrefabBuilder b) {
        Block tile = ModBlocks.RUIN_FLOOR_TILE.get();
        Block hex = ModBlocks.RUIN_HEX_FLOOR.get();
        Block obs = ModBlocks.RUIN_OBSIDIAN_BLOCK.get();
        Block trim = ModBlocks.RUIN_PLATINUM_TRIM.get();
        Block wall = ModBlocks.RUIN_PLATINUM_WALL.get();
        Block teal = ModBlocks.RUIN_CIRCUIT_TEAL.get();
        Block gold = ModBlocks.RUIN_CIRCUIT_GOLD.get();

        for (int x = 0; x < 19; x++) {
            for (int z = 0; z < 19; z++) {
                double d = Math.hypot(x - 9.0, z - 9.0);
                if (d <= 9.2) {
                    b.set(x, 0, z, obs);
                    if (d <= 2.3) {
                        b.set(x, 1, z, gold);
                    } else if (d <= 5.5) {
                        b.set(x, 1, z, hex);
                    } else if (d <= 8.2) {
                        b.set(x, 1, z, ((x + z) % 2 == 0) ? tile : hex);
                    } else {
                        b.set(x, 1, z, trim);
                        if (Math.abs(x - 9) > 2 && Math.abs(z - 9) > 2) {
                            b.set(x, 2, z, wall);
                        }
                    }
                }
            }
        }
        b.set(9, 1, 9, teal);
        for (int[] c : new int[][]{{4, 4}, {14, 4}, {4, 14}, {14, 14}}) {
            b.pillarCol(c[0], 2, 3, c[1], ModBlocks.RUIN_ENERGY_CONDUIT.get());
        }
        b.prop(9, 2, 9, ModBlocks.PROP_HOLOGRAM_PROJECTOR.get(), Direction.SOUTH);
    }

    private static void buildArkDockArm(PrefabBuilder b) {
        Block ark = ModBlocks.RUIN_ARK_HULL.get();
        Block arkGlow = ModBlocks.RUIN_ARK_GLOW_PANEL.get();
        Block beacon = ModBlocks.RUIN_DOCKING_BEACON.get();
        Block hardlight = ModBlocks.RUIN_HARDLIGHT_BRIDGE.get();
        Block barrier = ModBlocks.RUIN_HARDLIGHT_BARRIER.get();

        for (int z = 0; z < b.sizeZ; z++) {
            b.fill(1, 0, z, 9, 1, z, ark);
            b.fill(3, 2, z, 7, 2, z, (z % 4 == 0)
                    ? arkGlow.defaultBlockState()
                    : hardlight.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, SlabType.TOP));
            b.set(2, 2, z, ark);
            b.set(8, 2, z, ark);
            b.set(2, 3, z, barrier);
            b.set(8, 3, z, barrier);

            if (z == 2 || z == 7 || z == 12) {
                // Rib claw arms arching up and inward
                b.fill(0, 1, z, 1, 8, z, ark);
                b.fill(9, 1, z, 10, 8, z, ark);
                b.fill(1, 9, z, 3, 10, z, ark);
                b.fill(7, 9, z, 9, 10, z, ark);
                b.set(2, 11, z, beacon);
                b.set(8, 11, z, beacon);
                b.set(0, 5, z, arkGlow);
                b.set(10, 5, z, arkGlow);
            }
        }
    }

    private static void buildIceSpire(PrefabBuilder b) {
        Block rock = ModBlocks.PERMAFROST_ROCK.get();
        Block ice = ModBlocks.GLACIER_ICE_BLOCK.get();
        Block iceBrick = ModBlocks.POLAR_ICE_BRICKS.get();
        Block snow = ModBlocks.POLAR_PACKED_SNOW.get();
        Block crystal = ModBlocks.PROP_ICE_CRYSTAL.get();

        for (int y = 0; y < b.sizeY; y++) {
            double maxR = 5.2 * (1.0 - (double) y / (b.sizeY + 1));
            for (int x = 0; x < 11; x++) {
                for (int z = 0; z < 11; z++) {
                    double d = Math.hypot(x - 5.0, z - 5.0);
                    double jitter = ((x * 7 + z * 11 + y * 3) % 5) * 0.16;
                    if (d <= maxR + jitter) {
                        Block fillBlock;
                        if (y <= 2) {
                            fillBlock = (d > maxR - 1.2) ? snow : rock;
                        } else if (y <= 11) {
                            fillBlock = (d > maxR - 1.0) ? iceBrick : ice;
                        } else {
                            fillBlock = ice;
                        }
                        b.set(x, y, z, fillBlock);
                    }
                }
            }
        }
        b.set(5, 18, 5, crystal);
        b.set(2, 4, 3, crystal);
        b.set(8, 3, 7, crystal);
    }

    private static void buildSnowRidge(PrefabBuilder b) {
        Block rock = ModBlocks.PERMAFROST_ROCK.get();
        Block packed = ModBlocks.POLAR_PACKED_SNOW.get();
        Block snow = ModBlocks.GLACIER_SNOW.get();

        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 15; z++) {
                double dx = Math.abs(x - 8.0) / 8.5;
                double dz = Math.abs(z - 7.0) / 7.5;
                double heightFactor = Math.max(0.0, 1.0 - (dx * dx * 0.8 + dz * dz));
                int topY = (int) Math.round(heightFactor * 8.0);
                if (topY <= 0) continue;
                for (int y = 0; y <= topY; y++) {
                    Block blk = (y == topY) ? snow : (y >= topY - 2 ? packed : rock);
                    b.set(x, y, z, blk);
                }
            }
        }
        b.set(8, 8, 7, Blocks.AIR); // clean natural peak variation
        b.set(8, 7, 7, ModBlocks.SNOW_PACKED_SLAB.get());
    }
}
