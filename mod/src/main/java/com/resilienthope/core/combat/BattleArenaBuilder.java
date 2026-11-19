package com.resilienthope.core.combat;

import com.resilienthope.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the high-ceiling Starship Tactical Hangar Deck battle arena at Y=150..168
 * plus deep-space exterior framing outside the north viewport (Z=-14..-4).
 * Common-safe: zero client imports.
 */
public final class BattleArenaBuilder {
    private static final int MIN_X = -4;
    private static final int MAX_X = 32;
    private static final int MIN_Z = -4;
    private static final int MAX_Z = 28;
    private static final int FLOOR_Y = BattleArenaLayout.FLOOR_Y; // 150
    private static final int ROOF_Y = 168;

    private BattleArenaBuilder() {}

    public static void ensureArena(ServerLevel level, ArenaTheme theme) {
        if (level == null) return;
        ArenaTheme resolved = theme != null ? theme : ArenaTheme.STARSHIP_HANGAR;
        if (isSignatureIntact(level)) {
            return;
        }
        if (resolved == ArenaTheme.STARSHIP_HANGAR) {
            buildStarshipHangar(level);
        }
    }

    private static boolean isSignatureIntact(ServerLevel level) {
        return level.getBlockState(new BlockPos(14, ROOF_Y, 12)).is(ModBlocks.STARSHIP_HULL.get())
                && level.getBlockState(new BlockPos(14, 155, -4)).is(ModBlocks.VIEWPORT_GLASS.get())
                && level.getBlockState(new BlockPos(14, FLOOR_Y, 12)).is(ModBlocks.STARSHIP_FLOOR.get())
                && level.getBlockState(new BlockPos(-2, 156, 12)).is(Blocks.POLISHED_DEEPSLATE_SLAB);
    }

    private static void buildStarshipHangar(ServerLevel level) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState hull = ModBlocks.STARSHIP_HULL.get().defaultBlockState();
        BlockState wall = ModBlocks.STARSHIP_WALL.get().defaultBlockState();
        BlockState floor = ModBlocks.STARSHIP_FLOOR.get().defaultBlockState();
        BlockState hazard = ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState();
        BlockState shipLight = ModBlocks.STARSHIP_LIGHT.get().defaultBlockState();
        BlockState alarm = ModBlocks.ALARM_BEACON.get().defaultBlockState();
        BlockState glass = ModBlocks.VIEWPORT_GLASS.get().defaultBlockState();
        BlockState conduit = ModBlocks.CONDUIT_PILLAR.get().defaultBlockState();

        BlockState deep = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState deepTiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState seaLantern = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState ironBars = Blocks.IRON_BARS.defaultBlockState();
        BlockState cyanAccent = Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
        BlockState orangeAccent = Blocks.ORANGE_TERRACOTTA.defaultBlockState();

        List<BlockPos> deferredBars = new ArrayList<>();

        // 0. Clear generous bounding volume including exterior vista so legacy box blocks never remain
        for (int x = -6; x <= 34; x++) {
            for (int z = -16; z <= 30; z++) {
                for (int y = FLOOR_Y - 2; y <= ROOF_Y + 3; y++) {
                    level.setBlock(new BlockPos(x, y, z), air, 2);
                }
            }
        }

        // 1. Subfloor support (Y=149) & Deck Floor (Y=150) + Roof Ceiling (Y=168)
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                boolean isCombatBowl = (x >= 5 && x <= 23 && z >= 4 && z <= 20);
                boolean isCombatRim = isCombatBowl && (x == 5 || x == 23 || z == 4 || z == 20);
                boolean isCornerMarker = (x >= 7 && x <= 10 && (z == 6 || z == 18))
                        || (x >= 18 && x <= 21 && (z == 6 || z == 18));
                boolean isUnderGlowStrip = (z == 5 || z == 19) && (x >= 7 && x <= 21);

                BlockState subBlock = isUnderGlowStrip ? seaLantern : hull;
                level.setBlock(new BlockPos(x, FLOOR_Y - 1, z), subBlock, 2);

                BlockState topBlock;
                if (isUnderGlowStrip) {
                    topBlock = glass;
                } else if (isCombatRim) {
                    topBlock = hazard;
                } else if (x >= 7 && x <= 10 && (z == 6 || z == 18)) {
                    topBlock = cyanAccent;
                } else if (x >= 18 && x <= 21 && (z == 6 || z == 18)) {
                    topBlock = orangeAccent;
                } else if (isCombatBowl) {
                    topBlock = (x == 14 && (z % 4 == 0)) ? deepTiles : floor;
                } else if ((x == -1 || x == 29) && z >= 2 && z <= 22) {
                    topBlock = hazard;
                } else {
                    topBlock = ((x + z) % 3 == 0) ? deep : floor;
                }
                if (isCornerMarker && isUnderGlowStrip) {
                    topBlock = glass;
                }
                level.setBlock(new BlockPos(x, FLOOR_Y, z), topBlock, 2);

                // Vaulted roof shell
                boolean roofStrip = (z == 4 || z == 12 || z == 20) && (x >= 2 && x <= 26 && x % 4 == 2);
                level.setBlock(new BlockPos(x, ROOF_Y, z), roofStrip ? shipLight : hull, 2);
            }
        }

        // 2. Perimeter Bulkhead Walls (East, West, South) & Hero North Space Viewport (Z=-4)
        for (int z = MIN_Z; z <= MAX_Z; z++) {
            for (int y = FLOOR_Y + 1; y < ROOF_Y; y++) {
                buildSideBulkhead(level, MIN_X, y, z, Direction.EAST, wall, hull, hazard, shipLight, alarm, glass, deferredBars);
                buildSideBulkhead(level, MAX_X, y, z, Direction.WEST, wall, hull, hazard, shipLight, alarm, glass, deferredBars);
            }
        }

        for (int x = MIN_X + 1; x <= MAX_X - 1; x++) {
            for (int y = FLOOR_Y + 1; y < ROOF_Y; y++) {
                // Hero North Wall (Z = -4): expansive 3-bay observation viewport overlooking deep space
                boolean inGlassBand = (x >= 1 && x <= 27) && (y >= FLOOR_Y + 3 && y <= FLOOR_Y + 14);
                boolean isMullion = (x == 7 || x == 14 || x == 21);
                boolean isCrossbar = (y == FLOOR_Y + 8);
                if (inGlassBand && !isMullion && !isCrossbar) {
                    level.setBlock(new BlockPos(x, y, MIN_Z), glass, 2);
                } else if ((y == FLOOR_Y + 2 || y == ROOF_Y - 1) && x >= 1 && x <= 27 && x % 2 == 0) {
                    level.setBlock(new BlockPos(x, y, MIN_Z), shipLight, 2);
                } else if (isMullion && y >= FLOOR_Y + 2 && y <= ROOF_Y - 2) {
                    level.setBlock(new BlockPos(x, y, MIN_Z), (y == FLOOR_Y + 8) ? alarm : hull, 2);
                } else {
                    level.setBlock(new BlockPos(x, y, MIN_Z), (y <= FLOOR_Y + 2 || y >= ROOF_Y - 2) ? hull : wall, 2);
                }

                // South Wall (Z = 28): observation control strip high up, solid lower bulkhead
                boolean southWindow = (x >= 6 && x <= 22) && (y >= FLOOR_Y + 9 && y <= FLOOR_Y + 12) && (x != 14);
                boolean southTrim = (y == FLOOR_Y + 5 && x % 5 == 0);
                level.setBlock(new BlockPos(x, y, MAX_Z),
                        southWindow ? glass : (southTrim ? shipLight : ((y <= FLOOR_Y + 2) ? hull : wall)), 2);
            }
        }

        // 3. Exterior Deep-Space Architecture Outside North Viewport (Z = -14..-5)
        buildExteriorSpaceBackdrop(level, hull, deep, seaLantern, alarm);

        // 4. Vaulted Ceiling Arches (Y=164..167), Gantry Crane Beam & Hanging Hangar Lights
        int[] ribXs = {2, 8, 14, 20, 26};
        for (int rx : ribXs) {
            for (int z = MIN_Z + 1; z <= MAX_Z - 1; z++) {
                level.setBlock(new BlockPos(rx, ROOF_Y - 1, z), hull, 2);
                if (z <= MIN_Z + 4 || z >= MAX_Z - 4) {
                    level.setBlock(new BlockPos(rx, ROOF_Y - 2, z), deepTiles, 2);
                }
                if (z == MIN_Z + 2 || z == MAX_Z - 2) {
                    level.setBlock(new BlockPos(rx, ROOF_Y - 3, z),
                            stairs(Blocks.DEEPSLATE_TILE_STAIRS, z < 12 ? Direction.SOUTH : Direction.NORTH, Half.TOP), 2);
                    level.setBlock(new BlockPos(rx, ROOF_Y - 4, z),
                            slabTop(Blocks.POLISHED_DEEPSLATE_SLAB), 2);
                } else if (z % 4 == 0) {
                    level.setBlock(new BlockPos(rx, ROOF_Y - 2, z), chain(Direction.Axis.Y), 2);
                    level.setBlock(new BlockPos(rx, ROOF_Y - 3, z), shipLight, 2);
                    level.setBlock(new BlockPos(rx, ROOF_Y - 4, z),
                            Blocks.END_ROD.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN), 2);
                }
            }
        }

        // Overhead yellow-striped gantry crane rail along Z = -1 at Y = 163 (safe behind combat area)
        for (int x = MIN_X + 1; x <= MAX_X - 1; x++) {
            level.setBlock(new BlockPos(x, ROOF_Y - 5, -1), (x % 3 == 0) ? hull : hazard, 2);
            level.setBlock(new BlockPos(x, ROOF_Y - 4, -1), deferredBarMark(deferredBars, new BlockPos(x, ROOF_Y - 4, -1), ironBars), 2);
        }
        // Suspended crane hoist above North service bay
        for (int y = ROOF_Y - 8; y <= ROOF_Y - 6; y++) {
            level.setBlock(new BlockPos(14, y, -1), chain(Direction.Axis.Y), 2);
        }
        level.setBlock(new BlockPos(14, ROOF_Y - 9, -1),
                Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, Boolean.TRUE), 2);

        // 5. Raised East & West Catwalks (X = -3..-1 and X = 29..31 at Y = 156) with safety railings & consoles
        for (int z = 0; z <= 24; z++) {
            for (int cx : new int[]{-3, -2, -1}) {
                level.setBlock(new BlockPos(cx, FLOOR_Y + 6, z), slabTop(Blocks.POLISHED_DEEPSLATE_SLAB), 2);
            }
            for (int cx : new int[]{29, 30, 31}) {
                level.setBlock(new BlockPos(cx, FLOOR_Y + 6, z), slabTop(Blocks.POLISHED_DEEPSLATE_SLAB), 2);
            }
            if (z % 6 == 0) {
                // Support brackets under catwalk
                level.setBlock(new BlockPos(-1, FLOOR_Y + 5, z),
                        stairs(Blocks.POLISHED_DEEPSLATE_STAIRS, Direction.WEST, Half.TOP), 2);
                level.setBlock(new BlockPos(29, FLOOR_Y + 5, z),
                        stairs(Blocks.POLISHED_DEEPSLATE_STAIRS, Direction.EAST, Half.TOP), 2);
            }
            deferredBars.add(new BlockPos(-1, FLOOR_Y + 7, z));
            level.setBlock(new BlockPos(-1, FLOOR_Y + 7, z), ironBars, 2);
            deferredBars.add(new BlockPos(29, FLOOR_Y + 7, z));
            level.setBlock(new BlockPos(29, FLOOR_Y + 7, z), ironBars, 2);
        }

        // Catwalk Terminals & Holotables
        level.setBlock(new BlockPos(-2, FLOOR_Y + 7, 12), ModBlocks.HOLOTABLE_PROP.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(30, FLOOR_Y + 7, 12), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);

        // 6. Structural Conduit Columns & North-Bay Cover Props (outside combat bowl & camera path)
        for (int y = FLOOR_Y + 1; y < ROOF_Y; y++) {
            level.setBlock(new BlockPos(0, y, 0), conduit, 2);
            level.setBlock(new BlockPos(28, y, 0), conduit, 2);
            level.setBlock(new BlockPos(0, y, 24), conduit, 2);
            level.setBlock(new BlockPos(28, y, 24), conduit, 2);
        }

        // Low tactical cargo crates & service machinery along Z=-2..0 and East/West alcoves
        level.setBlock(new BlockPos(4, FLOOR_Y + 1, -1), hull, 2);
        level.setBlock(new BlockPos(5, FLOOR_Y + 1, -1), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(4, FLOOR_Y + 2, -1), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(9, FLOOR_Y + 1, -2), ModBlocks.CRYO_SLEEP_POD.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(10, FLOOR_Y + 1, -2), ModBlocks.CRYO_SLEEP_POD.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(18, FLOOR_Y + 1, -2), Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlastFurnaceBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(new BlockPos(19, FLOOR_Y + 1, -2), Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST), 2);
        level.setBlock(new BlockPos(23, FLOOR_Y + 1, -1), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(24, FLOOR_Y + 1, -1), Blocks.COPPER_BLOCK.defaultBlockState(), 2);
        level.setBlock(new BlockPos(24, FLOOR_Y + 2, -1), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);

        // West & East Deck Props under catwalks
        level.setBlock(new BlockPos(-2, FLOOR_Y + 1, 6), ModBlocks.CRYO_SLEEP_POD.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(-2, FLOOR_Y + 1, 18), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(30, FLOOR_Y + 1, 6), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(30, FLOOR_Y + 1, 18), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(2, FLOOR_Y + 1, 2), Blocks.LIGHTNING_ROD.defaultBlockState(), 2);
        level.setBlock(new BlockPos(26, FLOOR_Y + 1, 2), Blocks.LIGHTNING_ROD.defaultBlockState(), 2);

        // 7. Re-trigger neighbor shape updates on Iron Bars so railings link seamlessly
        for (BlockPos barPos : deferredBars) {
            BlockState cur = level.getBlockState(barPos);
            if (cur.is(Blocks.IRON_BARS)) {
                level.setBlock(barPos, air, 2);
                level.setBlock(barPos, ironBars, 3);
            }
        }
    }

    private static BlockState deferredBarMark(List<BlockPos> list, BlockPos pos, BlockState bar) {
        list.add(pos);
        return bar;
    }

    private static void buildSideBulkhead(ServerLevel level, int x, int y, int z, Direction inwardDir,
                                          BlockState wall, BlockState hull, BlockState hazard,
                                          BlockState light, BlockState alarm, BlockState glass,
                                          List<BlockPos> deferredBars) {
        boolean isBlastDoor = (z >= 10 && z <= 14) && (y >= FLOOR_Y + 1 && y <= FLOOR_Y + 5);
        boolean isDoorRim = (z == 9 || z == 15) && (y >= FLOOR_Y + 1 && y <= FLOOR_Y + 6)
                || (z >= 10 && z <= 14 && y == FLOOR_Y + 6);
        boolean isRib = (z % 6 == 0);
        boolean isUpperWindow = !isRib && !isBlastDoor && !isDoorRim && (y >= FLOOR_Y + 10 && y <= FLOOR_Y + 12) && z >= 2 && z <= 22;

        if (isDoorRim) {
            level.setBlock(new BlockPos(x, y, z), (y == FLOOR_Y + 6 && z == 12) ? alarm : hazard, 2);
        } else if (isBlastDoor) {
            level.setBlock(new BlockPos(x, y, z), hull, 2);
        } else if (isUpperWindow) {
            level.setBlock(new BlockPos(x, y, z), glass, 2);
        } else if (isRib) {
            level.setBlock(new BlockPos(x, y, z), (y == FLOOR_Y + 4 || y == FLOOR_Y + 14) ? light : hull, 2);
        } else {
            level.setBlock(new BlockPos(x, y, z), (y <= FLOOR_Y + 2) ? hull : wall, 2);
        }

        // Add subtle inward rib buttress columns at Z=6, 18
        if ((z == 6 || z == 18) && y >= FLOOR_Y + 7 && y <= ROOF_Y - 1) {
            int ix = x + inwardDir.getStepX();
            if (y == FLOOR_Y + 7) {
                level.setBlock(new BlockPos(ix, y, z), stairs(Blocks.POLISHED_DEEPSLATE_STAIRS, inwardDir.getOpposite(), Half.TOP), 2);
            } else if (y == ROOF_Y - 1) {
                level.setBlock(new BlockPos(ix, y, z), hull, 2);
            } else {
                level.setBlock(new BlockPos(ix, y, z), wall, 2);
            }
        }
    }

    private static void buildExteriorSpaceBackdrop(ServerLevel level, BlockState hull, BlockState deep,
                                                   BlockState seaLantern, BlockState alarm) {
        BlockState darkHull = Blocks.BLACKSTONE.defaultBlockState();
        BlockState cryObsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();

        // Twin external starship structural wing-ribs flanking the viewport
        for (int ribX : new int[]{3, 25}) {
            for (int z = -12; z <= -5; z++) {
                level.setBlock(new BlockPos(ribX, FLOOR_Y + 4, z), hull, 2);
                level.setBlock(new BlockPos(ribX, FLOOR_Y + 5, z), (z == -12) ? alarm : deep, 2);
                level.setBlock(new BlockPos(ribX, FLOOR_Y + 12, z), hull, 2);
                if (z == -12) {
                    for (int y = FLOOR_Y + 5; y <= FLOOR_Y + 12; y++) {
                        level.setBlock(new BlockPos(ribX, y, z), (y == FLOOR_Y + 8) ? seaLantern : hull, 2);
                    }
                }
            }
        }

        // Distant floating orbital array / station silhouette outside Z = -14..-11
        for (int x = 9; x <= 19; x++) {
            for (int y = FLOOR_Y + 7; y <= FLOOR_Y + 10; y++) {
                boolean ring = (x == 9 || x == 19 || y == FLOOR_Y + 7 || y == FLOOR_Y + 10);
                boolean core = (x >= 13 && x <= 15 && y >= FLOOR_Y + 8 && y <= FLOOR_Y + 9);
                if (ring) {
                    level.setBlock(new BlockPos(x, y, -13), (x == 14) ? cryObsidian : darkHull, 2);
                } else if (core) {
                    level.setBlock(new BlockPos(x, y, -14), seaLantern, 2);
                }
            }
        }
    }

    private static BlockState slabTop(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private static BlockState stairs(net.minecraft.world.level.block.Block block, Direction facing, Half half) {
        return block.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, half);
    }

    private static BlockState chain(Direction.Axis axis) {
        return Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }
}
