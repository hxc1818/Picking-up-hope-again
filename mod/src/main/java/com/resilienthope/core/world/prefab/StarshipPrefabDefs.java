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
 * 9 snap-compatible Starship modular prefabs built around a shared 5x4 interior corridor cross-section
 * (walkable floor at ly=1, ceiling at ly=5, centered on cx = sizeX / 2).
 */
final class StarshipPrefabDefs {
    private StarshipPrefabDefs() {}

    static void registerAll() {
        PrefabRegistry.register(new PrefabDef(
                "ship_corridor", "标准星舰主干走廊", Category.STARSHIP,
                9, 7, 8, 8,
                "带有斜切顶板、蓝纹饰带、灯柱与管线的标准 9x7x8 星舰直通走廊，可连续平铺延伸。",
                StarshipPrefabDefs::buildCorridor));

        PrefabRegistry.register(new PrefabDef(
                "ship_junction_t", "星舰 T 字分岔路口", Category.STARSHIP,
                9, 7, 9, 9,
                "直行连通并向东(+X)分出标准走廊接口，带格栅转角标识与顶部照明。",
                StarshipPrefabDefs::buildJunctionT));

        PrefabRegistry.register(new PrefabDef(
                "ship_junction_x", "星舰十字全息中庭", Category.STARSHIP,
                11, 8, 11, 11,
                "四向全通大型交汇中庭，中央预置全息投影仪与挑高顶部结构灯阵。",
                StarshipPrefabDefs::buildJunctionX));

        PrefabRegistry.register(new PrefabDef(
                "ship_airlock_gate", "气密防爆闸门段", Category.STARSHIP,
                9, 7, 5, 5,
                "带双扇气密防爆门、两侧警戒黑黄地砖与琥珀警示灯带的舱段分隔闸门。",
                StarshipPrefabDefs::buildAirlockGate));

        PrefabRegistry.register(new PrefabDef(
                "ship_stairs_deck", "双层甲板连接阶梯", Category.STARSHIP,
                9, 11, 12, 12,
                "从底层走廊稳步爬升 5 格至上层甲板的宽体合金阶梯通道，含安全护栏。",
                StarshipPrefabDefs::buildStairsDeck));

        PrefabRegistry.register(new PrefabDef(
                "ship_viewport_bay", "全景舷窗观测长廊", Category.STARSHIP,
                9, 7, 10, 10,
                "左右两翼配有大尺寸加固星际舷窗、金属护栏与格栅地板的观景长廊。",
                StarshipPrefabDefs::buildViewportBay));

        PrefabRegistry.register(new PrefabDef(
                "ship_crew_cabin", "标准休眠与船员舱室", Category.STARSHIP,
                11, 7, 11, 11,
                "精装船员休息舱：双层铁架床、桌面终端、储物架、医疗柜与独立气密门。",
                StarshipPrefabDefs::buildCrewCabin));

        PrefabRegistry.register(new PrefabDef(
                "ship_hangar_arch", "机库巨型跨度结构拱架", Category.STARSHIP,
                25, 17, 8, 8,
                "25格大跨度星舰机库穹顶桁架段，自带侧面二层维修栈桥与悬挂探照灯，重复堆叠秒成机库。",
                StarshipPrefabDefs::buildHangarArch));

        PrefabRegistry.register(new PrefabDef(
                "ship_escape_bay", "轨道逃生舱发射位", Category.STARSHIP,
                11, 10, 11, 11,
                "环形警戒地砖、底部发射槽与四向护栏围成的轨道撤离发射平台。",
                StarshipPrefabDefs::buildEscapeBay));
    }

    private static void buildCorridorSlice(PrefabBuilder b, int z, boolean isRib) {
        Block hullDark = ModBlocks.STARSHIP_HULL_DARK.get();
        Block wall = ModBlocks.STARSHIP_WALL.get();
        Block blue = ModBlocks.STARSHIP_ACCENT_BLUE.get();
        Block floor = ModBlocks.STARSHIP_FLOOR.get();
        Block grate = ModBlocks.STARSHIP_GRATE_FLOOR.get();
        Block ceil = ModBlocks.STARSHIP_CEILING_TILE.get();
        Block rib = ModBlocks.STARSHIP_DARK_RIB.get();
        Block lightPillar = ModBlocks.STARSHIP_PILLAR_LIGHT.get();
        Block wallStair = ModBlocks.STARSHIP_WALL_STAIRS.get();
        Block light = ModBlocks.STARSHIP_LIGHT.get();

        // Carve interior first
        b.air(2, 1, z, 6, 5, z);

        // Floor deck & outer structural shell
        b.fill(0, 0, z, 8, 0, z, hullDark);
        b.fill(2, 0, z, 6, 0, z, floor);
        b.set(4, 0, z, isRib ? floor : grate);

        // Outer structural casing & inner walls
        for (int sideX : new int[]{0, 8}) {
            b.fill(sideX, 0, z, sideX, 6, z, isRib ? rib : hullDark);
        }
        for (int sideX : new int[]{1, 7}) {
            b.set(sideX, 1, z, hullDark);
            b.set(sideX, 2, z, isRib ? lightPillar : wall);
            b.set(sideX, 3, z, isRib ? blue : wall);
            b.set(sideX, 4, z, wall);
            b.set(sideX, 5, z, hullDark);
        }

        // Chamfered ceiling stairs & overhead lighting + roof hull
        b.stair(2, 4, z, wallStair, Direction.WEST, Half.TOP);
        b.stair(6, 4, z, wallStair, Direction.EAST, Half.TOP);
        b.fill(2, 5, z, 6, 5, z, ceil);
        b.set(4, 5, z, isRib ? ceil : light);
        b.fill(0, 6, z, 8, 6, z, hullDark);
        if (!isRib) {
            b.prop(3, 4, z, ModBlocks.PROP_CABLE_PIPE.get(), Direction.SOUTH);
        }
    }

    private static void buildCorridor(PrefabBuilder b) {
        for (int z = 0; z < b.sizeZ; z++) {
            buildCorridorSlice(b, z, z % 4 == 0);
        }
        b.prop(5, 4, 2, ModBlocks.PROP_VENT_DUCT.get(), Direction.SOUTH);
        b.prop(5, 4, 6, ModBlocks.PROP_VENT_DUCT.get(), Direction.SOUTH);
    }

    private static void buildJunctionT(PrefabBuilder b) {
        for (int z = 0; z < b.sizeZ; z++) {
            buildCorridorSlice(b, z, z == 0 || z == b.sizeZ - 1);
        }
        // Carve East branch mouth (x=5..8, z=2..6, y=1..4) matching standard 5x4 opening!
        b.fill(4, 0, 2, 8, 0, 6, ModBlocks.STARSHIP_FLOOR.get());
        b.fill(5, 0, 4, 8, 0, 4, ModBlocks.STARSHIP_GRATE_FLOOR.get());
        b.air(6, 1, 2, 8, 4, 6);
        b.fill(6, 5, 2, 8, 5, 6, ModBlocks.STARSHIP_CEILING_TILE.get());
        b.fill(6, 6, 2, 8, 6, 6, ModBlocks.STARSHIP_HULL_DARK.get());
        b.stair(7, 4, 2, ModBlocks.STARSHIP_WALL_STAIRS.get(), Direction.NORTH, Half.TOP);
        b.stair(7, 4, 6, ModBlocks.STARSHIP_WALL_STAIRS.get(), Direction.SOUTH, Half.TOP);
        b.set(4, 5, 4, ModBlocks.STARSHIP_LIGHT.get());
        b.prop(1, 2, 4, ModBlocks.PROP_WALL_SCREEN.get(), Direction.EAST);
    }

    private static void buildJunctionX(PrefabBuilder b) {
        Block hullDark = ModBlocks.STARSHIP_HULL_DARK.get();
        Block wall = ModBlocks.STARSHIP_WALL.get();
        Block floor = ModBlocks.STARSHIP_FLOOR.get();
        Block blue = ModBlocks.STARSHIP_ACCENT_BLUE.get();
        Block ceil = ModBlocks.STARSHIP_CEILING_TILE.get();
        Block light = ModBlocks.STARSHIP_LIGHT.get();

        // Floor & base shell
        b.fill(0, 0, 0, 10, 0, 10, floor);
        b.fill(3, 0, 3, 7, 0, 7, blue);
        b.fill(4, 0, 4, 6, 0, 6, ModBlocks.STARSHIP_GRATE_FLOOR.get());
        b.air(1, 1, 1, 9, 6, 9);

        // 4 corner structural pillars with vertical light strips
        for (int x : new int[]{0, 1, 2, 8, 9, 10}) {
            for (int z : new int[]{0, 1, 2, 8, 9, 10}) {
                boolean isInnerCorner = (x == 2 && z == 2) || (x == 8 && z == 2) || (x == 2 && z == 8) || (x == 8 && z == 8);
                if (isInnerCorner) {
                    b.pillarCol(x, 1, 5, z, ModBlocks.STARSHIP_PILLAR_LIGHT.get());
                } else {
                    b.fill(x, 1, z, x, 6, z, (x == 0 || x == 10 || z == 0 || z == 10) ? hullDark : wall);
                }
            }
        }
        // Carve 4 symmetric 5x4 corridor mouths centered on cx=5 / cz=5
        b.air(3, 1, 0, 7, 4, 10);
        b.air(0, 1, 3, 10, 4, 7);

        // Raised ceiling dome with center skylight ring
        b.fill(0, 6, 0, 10, 6, 10, ceil);
        b.fill(0, 7, 0, 10, 7, 10, hullDark);
        b.fill(3, 6, 3, 7, 6, 7, ModBlocks.STARSHIP_WHITE_PANEL.get());
        b.set(5, 6, 5, light);
        b.set(3, 6, 5, light);
        b.set(7, 6, 5, light);
        b.set(5, 6, 3, light);
        b.set(5, 6, 7, light);

        b.set(5, 1, 5, ModBlocks.PROP_HOLOGRAM_PROJECTOR.get());
    }

    private static void buildAirlockGate(PrefabBuilder b) {
        for (int z = 0; z < b.sizeZ; z++) {
            buildCorridorSlice(b, z, z == 1 || z == 3);
        }
        // Center bulkhead frame & twin airlock doors at z=2
        b.fill(2, 1, 2, 6, 4, 2, ModBlocks.STARSHIP_WALL.get());
        b.fill(2, 0, 2, 6, 0, 2, ModBlocks.HAZARD_STRIPE_BLOCK.get());
        b.set(3, 3, 2, ModBlocks.ALARM_BEACON.get());
        b.set(5, 3, 2, ModBlocks.ALARM_BEACON.get());
        b.air(3, 1, 2, 5, 2, 2);
        b.door(3, 1, 2, ModBlocks.STARSHIP_AIRLOCK_DOOR.get(), Direction.SOUTH, DoorHingeSide.LEFT);
        b.door(5, 1, 2, ModBlocks.STARSHIP_AIRLOCK_DOOR.get(), Direction.SOUTH, DoorHingeSide.RIGHT);
        b.set(4, 1, 2, ModBlocks.STARSHIP_BULKHEAD_WALL.get());
        b.set(4, 2, 2, ModBlocks.STARSHIP_BULKHEAD_WALL.get());
        b.prop(2, 2, 1, ModBlocks.AMBER_LIGHT_STRIP.get(), Direction.SOUTH);
        b.prop(6, 2, 3, ModBlocks.AMBER_LIGHT_STRIP.get(), Direction.NORTH);
    }

    private static void buildStairsDeck(PrefabBuilder b) {
        Block hullDark = ModBlocks.STARSHIP_HULL_DARK.get();
        Block wall = ModBlocks.STARSHIP_WALL.get();
        Block floor = ModBlocks.STARSHIP_FLOOR.get();
        Block stair = ModBlocks.STARSHIP_FLOOR_STAIRS.get();
        Block rail = ModBlocks.STARSHIP_RAILING.get();
        Block light = ModBlocks.STARSHIP_LIGHT.get();

        // Full side shell Y=0..10
        for (int z = 0; z < b.sizeZ; z++) {
            for (int sideX : new int[]{0, 8}) {
                b.fill(sideX, 0, z, sideX, 10, z, (z % 4 == 0) ? ModBlocks.STARSHIP_DARK_RIB.get() : hullDark);
            }
            for (int sideX : new int[]{1, 7}) {
                b.fill(sideX, 1, z, sideX, 9, z, (z % 4 == 0) ? ModBlocks.STARSHIP_PILLAR_LIGHT.get() : wall);
            }
            b.fill(1, 10, z, 7, 10, z, hullDark);
            b.fill(2, 9, z, 6, 9, z, ModBlocks.STARSHIP_CEILING_TILE.get());
            if (z % 3 == 1) b.set(4, 9, z, light);
            b.air(2, 1, z, 6, 8, z);
        }

        // Bottom landing z=0..2 (walk on y=1), 5-step climb z=3..7 (y=1..5), upper landing z=8..11 (walk on y=6)
        b.fill(2, 0, 0, 6, 0, 2, floor);
        for (int step = 0; step < 5; step++) {
            int z = 3 + step;
            int y = 1 + step;
            b.fill(2, 0, z, 6, y - 1, z, hullDark);
            b.stair(3, y, z, stair, Direction.SOUTH, Half.BOTTOM);
            b.stair(4, y, z, stair, Direction.SOUTH, Half.BOTTOM);
            b.stair(5, y, z, stair, Direction.SOUTH, Half.BOTTOM);
            b.set(2, y, z, rail);
            b.set(6, y, z, rail);
        }
        b.fill(2, 0, 8, 6, 4, 11, hullDark);
        b.fill(2, 5, 8, 6, 5, 11, floor);
        b.set(4, 5, 10, ModBlocks.HAZARD_STRIPE_BLOCK.get());
    }

    private static void buildViewportBay(PrefabBuilder b) {
        Block pane = ModBlocks.STARSHIP_VIEWPORT_PANE.get();
        Block glass = ModBlocks.STARSHIP_REINFORCED_GLASS.get();
        for (int z = 0; z < b.sizeZ; z++) {
            buildCorridorSlice(b, z, z == 0 || z == b.sizeZ - 1);
            if (z >= 1 && z <= b.sizeZ - 2) {
                for (int sideX : new int[]{0, 8}) {
                    b.set(sideX, 2, z, glass);
                    b.set(sideX, 3, z, glass);
                }
                for (int sideX : new int[]{1, 7}) {
                    b.set(sideX, 2, z, pane);
                    b.set(sideX, 3, z, pane);
                }
            }
        }
        b.fill(3, 0, 2, 5, 0, 7, ModBlocks.STARSHIP_GRATE_FLOOR.get());
        b.prop(2, 1, 4, ModBlocks.PROP_CREW_CHAIR.get(), Direction.WEST);
        b.prop(6, 1, 5, ModBlocks.PROP_CREW_CHAIR.get(), Direction.EAST);
    }

    private static void buildCrewCabin(PrefabBuilder b) {
        Block white = ModBlocks.STARSHIP_WHITE_PANEL.get();
        Block hullDark = ModBlocks.STARSHIP_HULL_DARK.get();
        Block floor = ModBlocks.STARSHIP_FLOOR.get();

        b.fill(0, 0, 0, 10, 0, 10, floor);
        b.air(1, 1, 1, 9, 5, 9);
        b.boxWall(0, 1, 0, 10, 5, 10, white);
        // Reinforced corners & overhead shell
        for (int x : new int[]{0, 10}) {
            for (int z : new int[]{0, 10}) {
                b.fill(x, 0, z, x, 6, z, hullDark);
            }
        }
        b.fill(0, 6, 0, 10, 6, 10, hullDark);
        b.fill(1, 5, 1, 9, 5, 9, ModBlocks.STARSHIP_CEILING_TILE.get());
        b.set(5, 5, 5, ModBlocks.STARSHIP_LIGHT.get());
        b.set(3, 5, 3, ModBlocks.STARSHIP_LIGHT.get());
        b.set(7, 5, 7, ModBlocks.STARSHIP_LIGHT.get());

        // Front entrance doorway (z=0, centered x=5) matching 5x4 alignment
        b.fill(4, 1, 0, 4, 2, 0, ModBlocks.STARSHIP_VIEWPORT_PANE.get());
        b.fill(6, 1, 0, 6, 2, 0, ModBlocks.STARSHIP_VIEWPORT_PANE.get());
        b.door(5, 1, 0, ModBlocks.STARSHIP_AIRLOCK_DOOR.get(), Direction.SOUTH, DoorHingeSide.LEFT);

        // Side window on west wall
        b.fill(0, 2, 4, 0, 3, 6, ModBlocks.STARSHIP_REINFORCED_GLASS.get());

        // Interior furnishings
        b.prop(1, 1, 2, ModBlocks.PROP_BUNK_FRAME.get(), Direction.EAST);
        b.prop(1, 1, 8, ModBlocks.PROP_BUNK_FRAME.get(), Direction.EAST);
        b.prop(9, 1, 3, ModBlocks.PROP_MONITOR_DESK.get(), Direction.WEST);
        b.prop(8, 1, 3, ModBlocks.PROP_CREW_CHAIR.get(), Direction.EAST);
        b.prop(9, 1, 7, ModBlocks.PROP_MED_CABINET.get(), Direction.WEST);
        b.set(5, 1, 6, ModBlocks.PROP_DINING_TABLE.get());
    }

    private static void buildHangarArch(PrefabBuilder b) {
        Block hullDark = ModBlocks.STARSHIP_HULL_DARK.get();
        Block hull = ModBlocks.STARSHIP_HULL.get();
        Block floor = ModBlocks.STARSHIP_FLOOR.get();
        Block hazard = ModBlocks.HAZARD_STRIPE_BLOCK.get();
        Block rib = ModBlocks.STARSHIP_DARK_RIB.get();
        Block wall = ModBlocks.STARSHIP_WALL.get();
        Block light = ModBlocks.STARSHIP_LIGHT.get();
        Block rail = ModBlocks.STARSHIP_RAILING.get();

        // Wide open floor + side gantry walls
        b.fill(0, 0, 0, 24, 0, 7, floor);
        b.fill(3, 0, 0, 3, 0, 7, hazard);
        b.fill(21, 0, 0, 21, 0, 7, hazard);
        b.fill(11, 0, 0, 13, 0, 7, ModBlocks.STARSHIP_GRATE_FLOOR.get());

        for (int z = 0; z < b.sizeZ; z++) {
            boolean frame = (z == 0 || z == 4);
            // Outer lower bulkhead y=1..12
            b.fill(0, 1, z, 0, 12, z, frame ? rib : hullDark);
            b.fill(24, 1, z, 24, 12, z, frame ? rib : hullDark);

            // Second-tier service catwalks along x=1..2 and x=22..23 at y=5
            b.fillSlab(1, 5, z, 2, 5, z, ModBlocks.STARSHIP_CATWALK_SLAB.get(), SlabType.TOP);
            b.fillSlab(22, 5, z, 23, 5, z, ModBlocks.STARSHIP_CATWALK_SLAB.get(), SlabType.TOP);
            b.set(2, 6, z, rail);
            b.set(22, 6, z, rail);

            // Upper glass observation gallery y=7..9 on outer wall
            b.fill(0, 7, z, 0, 9, z, frame ? rib : ModBlocks.STARSHIP_REINFORCED_GLASS.get());
            b.fill(24, 7, z, 24, 9, z, frame ? rib : ModBlocks.STARSHIP_REINFORCED_GLASS.get());

            // Diagonal arched steel canopy roof y=13..16
            for (int dx = 0; dx <= 3; dx++) {
                int y = 13 + dx;
                b.set(dx, y, z, frame ? hullDark : hull);
                b.set(24 - dx, y, z, frame ? hullDark : hull);
            }
            b.fill(4, 16, z, 20, 16, z, frame ? hullDark : wall);
            if (!frame && z % 2 == 1) {
                b.set(8, 16, z, light);
                b.set(16, 16, z, light);
                b.set(12, 16, z, light);
            }
        }
        b.pillarCol(1, 1, 12, 0, ModBlocks.STARSHIP_PILLAR_LIGHT.get());
        b.pillarCol(23, 1, 12, 0, ModBlocks.STARSHIP_PILLAR_LIGHT.get());
        b.ladder(2, 1, 5, 1, ModBlocks.STARSHIP_LADDER.get(), Direction.EAST);
        b.ladder(22, 1, 5, 1, ModBlocks.STARSHIP_LADDER.get(), Direction.WEST);
        b.prop(1, 6, 3, ModBlocks.PROP_FLOODLIGHT.get(), Direction.EAST);
        b.prop(23, 6, 3, ModBlocks.PROP_FLOODLIGHT.get(), Direction.WEST);
    }

    private static void buildEscapeBay(PrefabBuilder b) {
        Block hullDark = ModBlocks.STARSHIP_HULL_DARK.get();
        Block hazard = ModBlocks.HAZARD_STRIPE_BLOCK.get();
        Block grate = ModBlocks.STARSHIP_GRATE_FLOOR.get();
        Block rail = ModBlocks.STARSHIP_RAILING.get();

        b.fill(0, 0, 0, 10, 0, 10, ModBlocks.STARSHIP_FLOOR.get());
        b.fill(2, 0, 2, 8, 0, 8, hazard);
        b.fill(3, 0, 3, 7, 0, 7, grate);
        b.fill(4, 0, 4, 6, 0, 6, ModBlocks.ALARM_BEACON.get());

        // Tall perimeter gantries + corner light towers
        for (int x : new int[]{0, 10}) {
            for (int z : new int[]{0, 10}) {
                b.fill(x, 1, z, x, 9, z, hullDark);
            }
        }
        b.pillarCol(2, 1, 8, 2, ModBlocks.STARSHIP_PILLAR_LIGHT.get());
        b.pillarCol(8, 1, 8, 2, ModBlocks.STARSHIP_PILLAR_LIGHT.get());
        b.pillarCol(2, 1, 8, 8, ModBlocks.STARSHIP_PILLAR_LIGHT.get());
        b.pillarCol(8, 1, 8, 8, ModBlocks.STARSHIP_PILLAR_LIGHT.get());

        // Safety railing ring with entry openings on North & South
        for (int i = 2; i <= 8; i++) {
            if (i < 4 || i > 6) {
                b.set(i, 1, 1, rail);
                b.set(i, 1, 9, rail);
            }
            b.set(1, 1, i, rail);
            b.set(9, 1, i, rail);
        }

        // Launch gantry top collar y=8..9
        b.boxWall(1, 8, 1, 9, 8, 9, hullDark);
        b.boxWall(2, 9, 2, 8, 9, 8, hazard);
        b.set(5, 1, 5, ModBlocks.PROP_WARNING_SIGN.get());
    }
}
