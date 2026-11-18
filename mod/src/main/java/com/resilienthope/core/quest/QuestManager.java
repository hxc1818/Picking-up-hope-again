package com.resilienthope.core.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class QuestManager {
    private static String questTitle = "【休眠区断电】";
    private static String objectiveText = "按 [F] 开启身旁的装甲物资箱";
    private static BlockPos targetPos = new BlockPos(7, 61, 10);
    private static int questStage = 0;
    private static int killsCount = 0;
    private static int requiredKills = 1;
    private static int subFlags = 0;
    private static String endingFaction = "";

    public static final int FLAG_CHEST_SLEEP = 1;
    public static final int FLAG_CHEST_STORAGE = 2;
    public static final int FLAG_CHEST_BRIDGE = 4;
    public static final int FLAG_CHEST_MEDBAY = 8;
    public static final int FLAG_CHEST_WRECK = 16;
    public static final int FLAG_CHEST_OUTPOST = 32;
    public static final int FLAG_CHEST_MINE = 64;

    public static final int FLAG_WAYPOINT_SHIP = 128;
    public static final int FLAG_WAYPOINT_ICE = 256;
    public static final int FLAG_CHAR_LV2 = 512;
    public static final int FLAG_CHAR_LV3 = 1024;
    public static final int FLAG_CHIP_PLUS3 = 2048;
    public static final int FLAG_DOMAIN_CLEARED = 4096;
    public static final int FLAG_SUBSTATION_GUARD_DEAD = 8192;
    public static final int FLAG_SUBSTATION_CORE_ON = 16384;
    public static final int FLAG_MINE_PATROL_DEAD = 32768;

    public static String getQuestTitle() { return questTitle; }
    public static String getObjectiveText() { return objectiveText; }
    public static BlockPos getTargetPos() { return targetPos; }
    public static int getQuestStage() { return questStage; }
    public static int getDronesKilled() { return killsCount; }
    public static int getRequiredDrones() { return requiredKills; }
    public static int getSubFlags() { return subFlags; }
    public static String getEndingFaction() { return endingFaction; }

    public static boolean hasFlag(int flag) { return (subFlags & flag) != 0; }
    public static void setFlag(int flag) { subFlags |= flag; }
    public static void setRequiredDrones(int req) { requiredKills = Math.max(1, req); }
    public static void incrementDroneKill() { killsCount++; }
    public static void resetDroneKills(int required) {
        killsCount = 0;
        requiredKills = Math.max(1, required);
    }
    public static void setEndingFaction(String faction) { endingFaction = faction != null ? faction : ""; }

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
        killsCount = kills;
        requiredKills = Math.max(1, required);
        subFlags = flags;
        endingFaction = ending != null ? ending : "";
    }

    public static void resetAll() {
        questStage = 0;
        questTitle = "【休眠区断电】";
        objectiveText = "按 [F] 开启身旁的装甲物资箱";
        targetPos = new BlockPos(7, 61, 10);
        killsCount = 0;
        requiredKills = 1;
        subFlags = 0;
        endingFaction = "";
    }

    public static BlockPos getEffectiveBreadcrumbPos(Player player) {
        if (player == null || targetPos == null) return targetPos;

        double px = player.getX();
        double py = player.getY();

        if (px < 85.0) {
            // Aboard the Starship
            if (targetPos.getX() >= 90) {
                if (py >= 50.0) return new BlockPos(35, 61, 15); // Upper -> Lower elevator
                return new BlockPos(22, 36, 10); // Lower -> Escape Pod
            }
            if (targetPos.getY() < 45 && py >= 50.0) {
                return new BlockPos(35, 61, 15);
            }
            if (targetPos.getY() >= 55 && py < 50.0) {
                return new BlockPos(35, 36, 15);
            }
            if (px < 12.0 && targetPos.getX() > 12) {
                return new BlockPos(12, 61, 10);
            }
            if (px < 25.0 && py >= 50.0 && targetPos.getX() > 26) {
                return new BlockPos(25, 61, 10);
            }
        } else {
            // On Ice Planet (Outpost + Mine)
            if (targetPos.getX() < 85) {
                return new BlockPos(114, 61, 28); // Ice Waypoint back to ship
            }
            if (targetPos.getY() < 45 && py >= 50.0) {
                return new BlockPos(146, 61, 28); // Mine elevator down
            }
            if (targetPos.getY() >= 55 && py < 50.0) {
                return new BlockPos(146, 36, 28); // Mine elevator up
            }
        }
        return targetPos;
    }

    public static String getBreadcrumbHint(Player player) {
        if (player == null || targetPos == null) return "";
        BlockPos eff = getEffectiveBreadcrumbPos(player);
        if (!eff.equals(targetPos)) {
            if (eff.getX() == 35 && eff.getY() == 61) return " · 沿升降梯下行";
            if (eff.getX() == 35 && eff.getY() == 36) return " · 乘梯返回上层";
            if (eff.getX() == 22 && eff.getY() == 36) return " · 前往逃生舱";
            if (eff.getX() == 114 && eff.getZ() == 28) return " · 经锚点返舰";
            if (eff.getX() == 146 && eff.getY() == 61) return " · 乘矿坑梯下行";
            if (eff.getX() == 146 && eff.getY() == 36) return " · 乘梯回科考站";
            if (eff.getX() == 12 || eff.getX() == 25) return " · 穿过走廊闸门";
        }
        return "";
    }
}
