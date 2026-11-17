package com.resilienthope.core.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class QuestManager {
    private static String questTitle = "【序章·冻眠苏醒】";
    private static String objectiveText = "开启休眠舱冷冻物资箱，获取初始芯片";
    private static BlockPos targetPos = new BlockPos(6, 62, 10);
    private static int questStage = 0;
    private static int dronesKilled = 0;
    private static int requiredDrones = 2;
    private static int subFlags = 0; // bitmask: 1=chestLooted, 2=waypointActivated, 4=coreRepaired, 8=alienWaypoint, 16=bossSpawned
    private static String endingFaction = "";

    public static final int FLAG_CHEST_LOOTED = 1;
    public static final int FLAG_WAYPOINT_ACTIVATED = 2;
    public static final int FLAG_CORE_REPAIRED = 4;
    public static final int FLAG_ALIEN_WAYPOINT = 8;
    public static final int FLAG_BOSS_SPAWNED = 16;

    public static String getQuestTitle() { return questTitle; }
    public static String getObjectiveText() { return objectiveText; }
    public static BlockPos getTargetPos() { return targetPos; }
    public static int getQuestStage() { return questStage; }
    public static int getDronesKilled() { return dronesKilled; }
    public static int getRequiredDrones() { return requiredDrones; }
    public static int getSubFlags() { return subFlags; }
    public static String getEndingFaction() { return endingFaction; }

    public static boolean hasFlag(int flag) {
        return (subFlags & flag) != 0;
    }

    public static void setFlag(int flag) {
        subFlags |= flag;
    }

    public static void setRequiredDrones(int req) {
        requiredDrones = Math.max(1, req);
    }

    public static void incrementDroneKill() {
        dronesKilled++;
    }

    public static void resetDroneKills(int required) {
        dronesKilled = 0;
        requiredDrones = required;
    }

    public static void setEndingFaction(String faction) {
        endingFaction = faction != null ? faction : "";
    }

    public static void advanceQuest(int stage, String title, String objective, BlockPos target) {
        questStage = stage;
        questTitle = title;
        objectiveText = objective;
        targetPos = target;
    }

    public static void setQuestData(String title, String objective, BlockPos target, int stage, int kills, int required, int flags, String ending) {
        questTitle = title;
        objectiveText = objective;
        targetPos = target;
        questStage = stage;
        dronesKilled = kills;
        requiredDrones = Math.max(1, required);
        subFlags = flags;
        endingFaction = ending != null ? ending : "";
    }

    public static void resetAll() {
        questStage = 0;
        questTitle = "【序章·冻眠苏醒】";
        objectiveText = "开启休眠舱冷冻物资箱，获取初始芯片";
        targetPos = new BlockPos(6, 62, 10);
        dronesKilled = 0;
        requiredDrones = 2;
        subFlags = 0;
        endingFaction = "";
    }

    /**
     * Smart Breadcrumb Guide: routes player cleanly through airlocks, elevator shafts,
     * and the orbital drop pod instead of pointing directly through floors or into the void.
     */
    public static BlockPos getEffectiveBreadcrumbPos(Player player) {
        if (player == null || targetPos == null) return targetPos;

        double px = player.getX();
        double py = player.getY();

        if (px < 85.0) {
            // Player is aboard the Starship
            if (targetPos.getX() >= 90) {
                // Target is on the Alien Permafrost Mine
                if (py >= 50.0) {
                    return new BlockPos(34, 61, 14); // Go to elevator shaft first
                } else {
                    return new BlockPos(22, 36, 10); // Then go to Orbital Drop Pod
                }
            }
            if (targetPos.getY() < 45 && py >= 50.0) {
                // Target is on Lower Deck while player is on Upper Deck
                return new BlockPos(34, 61, 14);
            }
            if (targetPos.getY() >= 55 && py < 50.0) {
                // Target is on Upper Deck while player is on Lower Deck
                return new BlockPos(33, 36, 14);
            }
            if (px < 12.0 && targetPos.getX() > 14) {
                return new BlockPos(12, 62, 10); // Awakening Bay airlock
            }
            if (px < 24.0 && py >= 50.0 && targetPos.getX() > 26) {
                return new BlockPos(24, 62, 10); // Corridor -> Fusion Hub airlock
            }
        } else {
            // Player is on the Alien Permafrost Mine
            if (targetPos.getX() < 85) {
                return new BlockPos(112, 61, 28); // Return via Alien Waypoint
            }
        }
        return targetPos;
    }

    public static String getBreadcrumbHint(Player player) {
        if (player == null || targetPos == null) return "";
        BlockPos eff = getEffectiveBreadcrumbPos(player);
        if (!eff.equals(targetPos)) {
            if (eff.getX() == 34 && eff.getY() == 61) return " · 经升降梯前往下层";
            if (eff.getX() == 33 && eff.getY() == 36) return " · 乘梯返回上层大厅";
            if (eff.getX() == 22 && eff.getY() == 36) return " · 前往轨道空投舱";
            if (eff.getX() == 112 && eff.getZ() == 28) return " · 经异星锚点返舰";
            if (eff.getX() == 12 || eff.getX() == 24) return " · 穿过气闸通道";
        }
        return "";
    }
}
