package com.resilienthope.core.world.prefab;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Common-safe registry of 28 built-in reusable architectural & terrain prefabs
 * grouped into 4 creative building categories.
 */
public final class PrefabRegistry {

    public enum Category {
        STARSHIP("starship", "星舰模块", 0xFF38BDF8),
        OUTPOST("outpost", "极地与工业", 0xFF4ADE80),
        MINE_GEO("mine_geo", "矿区与地热", 0xFFFB923C),
        RUINS_TERRAIN("ruins_terrain", "遗迹与地貌", 0xFFC084FC);

        public final String id;
        public final String label;
        public final int accentColor;

        Category(String id, String label, int accentColor) {
            this.id = id;
            this.label = label;
            this.accentColor = accentColor;
        }
    }

    public static final class PrefabDef {
        public final String id;
        public final String name;
        public final Category category;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        public final int repeatStep;
        public final String description;
        private final Consumer<PrefabBuilder> generator;

        public PrefabDef(String id, String name, Category category,
                         int sizeX, int sizeY, int sizeZ, int repeatStep,
                         String description, Consumer<PrefabBuilder> generator) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.repeatStep = repeatStep;
            this.description = description;
            this.generator = generator;
        }

        public Map<BlockPos, BlockState> buildBlocks() {
            PrefabBuilder builder = new PrefabBuilder(sizeX, sizeY, sizeZ);
            generator.accept(builder);
            return builder.build();
        }

        public String sizeLabel() {
            return sizeX + "×" + sizeY + "×" + sizeZ;
        }
    }

    /**
     * Compact local-coordinate builder supporting fill, air carving, rotated stairs, slabs,
     * pillars, two-half doors, ladders, and facing-aware 3D voxel props.
     */
    public static final class PrefabBuilder {
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        private final Map<BlockPos, BlockState> map = new LinkedHashMap<>();

        public PrefabBuilder(int sizeX, int sizeY, int sizeZ) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }

        public void set(int x, int y, int z, BlockState state) {
            if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) return;
            map.put(new BlockPos(x, y, z), state);
        }

        public void set(int x, int y, int z, Block block) {
            set(x, y, z, block.defaultBlockState());
        }

        public void air(int x0, int y0, int z0, int x1, int y1, int z1) {
            fill(x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
        }

        public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
            fill(x0, y0, z0, x1, y1, z1, block.defaultBlockState());
        }

        public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            int xa = Math.max(0, Math.min(x0, x1));
            int xb = Math.min(sizeX - 1, Math.max(x0, x1));
            int ya = Math.max(0, Math.min(y0, y1));
            int yb = Math.min(sizeY - 1, Math.max(y0, y1));
            int za = Math.max(0, Math.min(z0, z1));
            int zb = Math.min(sizeZ - 1, Math.max(z0, z1));
            for (int x = xa; x <= xb; x++) {
                for (int y = ya; y <= yb; y++) {
                    for (int z = za; z <= zb; z++) {
                        map.put(new BlockPos(x, y, z), state);
                    }
                }
            }
        }

        public void boxWall(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
            BlockState s = block.defaultBlockState();
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (x == x0 || x == x1 || z == z0 || z == z1) {
                            set(x, y, z, s);
                        }
                    }
                }
            }
        }

        public void stair(int x, int y, int z, Block stairBlock, Direction facing, Half half) {
            set(x, y, z, stairBlock.defaultBlockState()
                    .setValue(StairBlock.FACING, facing)
                    .setValue(StairBlock.HALF, half));
        }

        public void slab(int x, int y, int z, Block slabBlock, SlabType type) {
            set(x, y, z, slabBlock.defaultBlockState().setValue(SlabBlock.TYPE, type));
        }

        public void fillSlab(int x0, int y0, int z0, int x1, int y1, int z1, Block slabBlock, SlabType type) {
            fill(x0, y0, z0, x1, y1, z1, slabBlock.defaultBlockState().setValue(SlabBlock.TYPE, type));
        }

        public void pillar(int x, int y, int z, Block pillarBlock, Direction.Axis axis) {
            set(x, y, z, pillarBlock.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
        }

        public void pillarCol(int x, int y0, int y1, int z, Block pillarBlock) {
            fill(x, y0, z, x, y1, z, pillarBlock.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        }

        public void prop(int x, int y, int z, Block propBlock, Direction facing) {
            BlockState s = propBlock.defaultBlockState();
            if (s.hasProperty(HorizontalDirectionalBlock.FACING)) {
                s = s.setValue(HorizontalDirectionalBlock.FACING, facing);
            }
            set(x, y, z, s);
        }

        public void ladder(int x, int y0, int y1, int z, Block ladderBlock, Direction facing) {
            BlockState s = ladderBlock.defaultBlockState().setValue(LadderBlock.FACING, facing);
            fill(x, y0, z, x, y1, z, s);
        }

        public void hatch(int x, int y, int z, Block hatchBlock, Direction facing, Half half) {
            set(x, y, z, hatchBlock.defaultBlockState()
                    .setValue(TrapDoorBlock.FACING, facing)
                    .setValue(TrapDoorBlock.HALF, half)
                    .setValue(TrapDoorBlock.OPEN, false));
        }

        public void door(int x, int y, int z, Block doorBlock, Direction facing, DoorHingeSide hinge) {
            BlockState base = doorBlock.defaultBlockState()
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, hinge)
                    .setValue(DoorBlock.OPEN, false);
            set(x, y, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            set(x, y + 1, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        public Map<BlockPos, BlockState> build() {
            return Collections.unmodifiableMap(map);
        }
    }

    private static final List<PrefabDef> ALL = new ArrayList<>();
    private static final Map<String, PrefabDef> BY_ID = new LinkedHashMap<>();

    static {
        StarshipPrefabDefs.registerAll();
        OutpostMinePrefabDefs.registerAll();
        RuinTerrainPrefabDefs.registerAll();
    }

    private PrefabRegistry() {}

    static void register(PrefabDef def) {
        ALL.add(def);
        BY_ID.put(def.id, def);
    }

    public static List<PrefabDef> getAll() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<PrefabDef> getByCategory(Category category) {
        List<PrefabDef> list = new ArrayList<>();
        for (PrefabDef def : ALL) {
            if (def.category == category) list.add(def);
        }
        return list;
    }

    public static PrefabDef findById(String id) {
        if (id == null) return null;
        return BY_ID.get(id.trim().toLowerCase());
    }
}
