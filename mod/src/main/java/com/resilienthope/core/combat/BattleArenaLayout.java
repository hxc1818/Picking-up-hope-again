package com.resilienthope.core.combat;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared spatial layout and dimensions for the real-world 3D turn-based battle arena.
 * Common-safe: never imports client-only classes.
 */
public final class BattleArenaLayout {
    public static final int FLOOR_Y = 150;
    public static final double UNIT_Y = 151.0D;

    public static final double ARENA_CENTER_X = 14.0D;
    public static final double ARENA_CENTER_Z = 12.0D;

    public static final double PLAYER_WAIT_X = 2.5D;
    public static final double PLAYER_WAIT_Z = 12.0D;

    /** Allies stand on the West side (X ~ 9) facing East (+X -> yaw -90 in Minecraft). */
    public static final float ALLY_YAW = -90.0F;
    /** Enemies stand on the East side (X ~ 19) facing West (-X -> yaw +90 in Minecraft). */
    public static final float ENEMY_YAW = 90.0F;

    private BattleArenaLayout() {}

    public static Vec3 allyHomePos(int idx, int total) {
        int safeTotal = Math.max(1, total);
        double spread = Math.max(1.8D, Math.min(2.6D, 7.2D / safeTotal));
        double z = ARENA_CENTER_Z + (idx - (safeTotal - 1) / 2.0D) * spread;
        double x = 9.0D - Math.abs(idx - (safeTotal - 1) / 2.0D) * 0.55D;
        return new Vec3(x, UNIT_Y, z);
    }

    public static Vec3 enemyHomePos(int idx, int total) {
        int safeTotal = Math.max(1, total);
        double spread = Math.max(2.0D, Math.min(3.0D, 7.6D / safeTotal));
        double z = ARENA_CENTER_Z + (idx - (safeTotal - 1) / 2.0D) * spread;
        double x = 19.0D + Math.abs(idx - (safeTotal - 1) / 2.0D) * 0.55D;
        return new Vec3(x, UNIT_Y, z);
    }

    public static String resolveEnemyArchetype(BattleState.EnemyUnit e) {
        if (e == null) return "drone";
        if (e.archetype != null && !e.archetype.isEmpty()) {
            return e.archetype;
        }
        String id = e.id != null ? e.id : "";
        String name = e.name != null ? e.name : "";
        if (e.isBossCore || id.contains("boss_core")) return "boss_core";
        if (e.isBossArm || id.contains("boss_left") || id.contains("boss_right")) {
            return id.contains("left") ? "boss_arm_l" : "boss_arm_r";
        }
        if (id.contains("cleaner") || name.contains("清扫")) return "cleaner";
        if (id.contains("hound") || name.contains("猎犬")) return "hound";
        if (id.contains("driller") || id.contains("md_") || name.contains("钻探") || name.contains("开采")) return "driller";
        if (id.contains("guard") || id.contains("cmd") || id.contains("trial") || id.contains("sub_")
                || name.contains("防卫") || name.contains("重装") || name.contains("统御") || name.contains("守卫") || name.contains("哨戒")) {
            return "guard";
        }
        return "drone";
    }

    public static float getModelScale(String modelKey) {
        if (modelKey == null) return 1.0F;
        return switch (modelKey) {
            case "boss_core" -> 1.45F;
            case "driller" -> 1.22F;
            case "guard" -> 1.18F;
            case "hound", "boss_arm_l", "boss_arm_r" -> 1.08F;
            case "cleaner", "drone" -> 1.0F;
            default -> 0.94F; // Humanoids
        };
    }

    public static float getOverheadBarHeight(String modelKey) {
        if (modelKey == null) return 2.15F;
        return switch (modelKey) {
            case "boss_core" -> 3.4F;
            case "driller" -> 2.3F;
            case "guard" -> 2.55F;
            case "boss_arm_l", "boss_arm_r" -> 2.3F;
            case "hound" -> 1.85F;
            case "cleaner" -> 1.65F;
            default -> 2.15F;
        };
    }

    public static AABB getClickHitbox(Vec3 basePos, String modelKey) {
        float h = getOverheadBarHeight(modelKey);
        double halfW = "boss_core".equals(modelKey) ? 1.25D
                : ("guard".equals(modelKey) || "driller".equals(modelKey) ? 0.92D : 0.72D);
        return new AABB(
                basePos.x - halfW, basePos.y - 0.1D, basePos.z - halfW,
                basePos.x + halfW, basePos.y + h + 0.15D, basePos.z + halfW
        );
    }
}
