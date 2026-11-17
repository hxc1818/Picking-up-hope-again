package com.resilienthope.core.quest;

import net.minecraft.core.BlockPos;

public class QuestManager {
    private static String questTitle = "【序章·熄灭的炉火】";
    private static String objectiveText = "前往重工业机房连接传送锚点";
    private static BlockPos targetPos = new BlockPos(32, 61, 10);
    private static int questStage = 0;
    private static int dronesKilled = 0;
    private static final int REQUIRED_DRONES = 2;

    public static String getQuestTitle() {
        return questTitle;
    }

    public static String getObjectiveText() {
        return objectiveText;
    }

    public static BlockPos getTargetPos() {
        return targetPos;
    }

    public static int getQuestStage() {
        return questStage;
    }

    public static int getDronesKilled() {
        return dronesKilled;
    }

    public static int getRequiredDrones() {
        return REQUIRED_DRONES;
    }

    public static void incrementDroneKill() {
        dronesKilled++;
    }

    public static void resetDroneKills() {
        dronesKilled = 0;
    }

    public static void advanceQuest(int stage, String title, String objective, BlockPos target) {
        questStage = stage;
        questTitle = title;
        objectiveText = objective;
        targetPos = target;
    }

    public static void setQuestData(String title, String objective, BlockPos target, int stage) {
        questTitle = title;
        objectiveText = objective;
        targetPos = target;
        questStage = stage;
    }
}
