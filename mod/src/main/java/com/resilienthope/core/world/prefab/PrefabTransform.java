package com.resilienthope.core.world.prefab;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

/**
 * Common-safe coordinate transformation math shared between server-side prefab stamping
 * and client-side 3D wireframe blueprint previews.
 * Canonical local orientation (rotSteps = 0) extends along +Z (South), centered horizontally on cx = sizeX / 2.
 */
public final class PrefabTransform {
    private PrefabTransform() {}

    public static Rotation toRotation(int rotSteps) {
        return switch (Math.floorMod(rotSteps, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static String directionName(int rotSteps) {
        return switch (Math.floorMod(rotSteps, 4)) {
            case 1 -> "西(-X)";
            case 2 -> "北(-Z)";
            case 3 -> "东(+X)";
            default -> "南(+Z)";
        };
    }

    public static int rotStepsFromPlayerYaw(float yRot) {
        Direction dir = Direction.fromYRot(yRot);
        return switch (dir) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    public static int[] rotateXZ(int dx, int dz, int rotSteps) {
        return switch (Math.floorMod(rotSteps, 4)) {
            case 1 -> new int[]{-dz, dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{dz, -dx};
            default -> new int[]{dx, dz};
        };
    }

    public static int[] forwardUnitXZ(int rotSteps) {
        return rotateXZ(0, 1, rotSteps);
    }

    public static BlockPos localToWorld(BlockPos anchor, int sizeX, int lx, int ly, int lz,
                                        int rotSteps, int segIdx, int repeatStep, int yOffset) {
        int cx = sizeX / 2;
        int dx = lx - cx;
        int dz = lz + segIdx * repeatStep;
        int[] xz = rotateXZ(dx, dz, rotSteps);
        return new BlockPos(anchor.getX() + xz[0], anchor.getY() + yOffset + ly, anchor.getZ() + xz[1]);
    }

    /**
     * Returns integer world bounding box [minX, minY, minZ, maxXExcl, maxYExcl, maxZExcl] for one segment.
     */
    public static int[] computeSegmentBounds(BlockPos anchor, int sizeX, int sizeY, int sizeZ,
                                             int rotSteps, int segIdx, int repeatStep, int yOffset) {
        BlockPos c1 = localToWorld(anchor, sizeX, 0, 0, 0, rotSteps, segIdx, repeatStep, yOffset);
        BlockPos c2 = localToWorld(anchor, sizeX, Math.max(0, sizeX - 1), Math.max(0, sizeY - 1), Math.max(0, sizeZ - 1),
                rotSteps, segIdx, repeatStep, yOffset);
        return new int[]{
                Math.min(c1.getX(), c2.getX()),
                Math.min(c1.getY(), c2.getY()),
                Math.min(c1.getZ(), c2.getZ()),
                Math.max(c1.getX(), c2.getX()) + 1,
                Math.max(c1.getY(), c2.getY()) + 1,
                Math.max(c1.getZ(), c2.getZ()) + 1
        };
    }
}
