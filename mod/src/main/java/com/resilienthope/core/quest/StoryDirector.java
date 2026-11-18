package com.resilienthope.core.quest;

import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.entity.SecurityDroneEntity;
import com.resilienthope.core.init.ModEntities;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.network.PacketSyncQuest;
import com.resilienthope.core.network.PacketSyncTeamData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class StoryDirector {

    public static void saveAndSyncQuest(ServerPlayer player) {
        if (player == null) return;
        CompoundTag qTag = new CompoundTag();
        qTag.putInt("Stage", QuestManager.getQuestStage());
        qTag.putString("Title", QuestManager.getQuestTitle());
        qTag.putString("Objective", QuestManager.getObjectiveText());
        BlockPos t = QuestManager.getTargetPos();
        qTag.putInt("TX", t.getX());
        qTag.putInt("TY", t.getY());
        qTag.putInt("TZ", t.getZ());
        qTag.putInt("Kills", QuestManager.getDronesKilled());
        qTag.putInt("Req", QuestManager.getRequiredDrones());
        qTag.putInt("Flags", QuestManager.getSubFlags());
        qTag.putString("Ending", QuestManager.getEndingFaction());
        player.getPersistentData().put("PickingHopeQuest", qTag);

        PacketHandler.sendToPlayer(player, new PacketSyncQuest(
                QuestManager.getQuestTitle(),
                QuestManager.getObjectiveText(),
                QuestManager.getTargetPos(),
                QuestManager.getQuestStage(),
                QuestManager.getDronesKilled(),
                QuestManager.getRequiredDrones(),
                QuestManager.getSubFlags(),
                QuestManager.getEndingFaction()
        ));
    }

    public static void loadQuest(ServerPlayer player) {
        if (player == null) return;
        if (player.getPersistentData().contains("PickingHopeQuest")) {
            CompoundTag q = player.getPersistentData().getCompound("PickingHopeQuest");
            QuestManager.setQuestData(
                    q.getString("Title"),
                    q.getString("Objective"),
                    new BlockPos(q.getInt("TX"), q.getInt("TY"), q.getInt("TZ")),
                    q.getInt("Stage"),
                    q.getInt("Kills"),
                    Math.max(1, q.getInt("Req")),
                    q.getInt("Flags"),
                    q.getString("Ending")
            );
        } else {
            QuestManager.resetAll();
        }
        saveAndSyncQuest(player);
    }

    public static void ensureNpc(ServerLevel level, String npcId, double x, double y, double z, float yaw) {
        AABB box = new AABB(x - 10, y - 4, z - 10, x + 10, y + 6, z + 10);
        List<FactionNpcEntity> existing = level.getEntitiesOfClass(FactionNpcEntity.class, box);
        for (FactionNpcEntity npc : existing) {
            if (npcId.equals(npc.getNpcId())) {
                return;
            }
        }
        FactionNpcEntity npc = ModEntities.FACTION_NPC.get().create(level);
        if (npc != null) {
            npc.setNpcId(npcId);
            npc.moveTo(x, y, z, yaw, 0.0f);
            level.addFreshEntity(npc);
        }
    }

    public static void openElevatorShaft(ServerLevel level) {
        for (int y = 36; y <= 60; y++) {
            level.setBlock(new BlockPos(34, y, 15), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(35, y, 15), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), 2);
            level.setBlock(new BlockPos(36, y, 15), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public static void openMineElevatorShaft(ServerLevel level) {
        for (int y = 36; y <= 60; y++) {
            level.setBlock(new BlockPos(145, y, 28), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(146, y, 28), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), 2);
            level.setBlock(new BlockPos(147, y, 28), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public static void spawnOverworldDrone(ServerLevel level, double x, double y, double z, String customName) {
        AABB box = new AABB(x - 4, y - 3, z - 4, x + 4, y + 4, z + 4);
        if (!level.getEntitiesOfClass(SecurityDroneEntity.class, box).isEmpty()) return;
        SecurityDroneEntity d = ModEntities.SECURITY_DRONE.get().create(level);
        if (d != null) {
            d.moveTo(x, y, z, 90.0f, 0.0f);
            d.setCustomName(new net.minecraft.network.chat.TextComponent(customName));
            d.setCustomNameVisible(true);
            level.addFreshEntity(d);
        }
    }

    public static void grantItem(ServerPlayer player, ItemStack stack) {
        EquipmentStatsHelper.ensureStats(stack, player.getRandom());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void triggerStage0To1(ServerPlayer player, boolean fromChest) {
        if (QuestManager.getQuestStage() != 0) return;
        if (fromChest) {
            QuestManager.setFlag(QuestManager.FLAG_CHEST_LOOTED);
        }
        ServerLevel level = player.getLevel();
        spawnOverworldDrone(level, 18.5, 62.0, 10.0, "故障清扫机");

        QuestManager.advanceQuest(1, "【走廊初战】", "左键攻击走廊内的故障清扫机，切入回合制战斗", new BlockPos(18, 62, 10));
        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("船舱广播",
                "前方维修走廊有清扫机器短路拦路！靠近按鼠标左键挥刀，即可先手切入回合制战斗！", 170));
        saveAndSyncQuest(player);
    }

    public static void onBattleWon(ServerPlayer player, String encounterType) {
        if (player == null) return;
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();

        if (stage == 1) {
            ensureNpc(level, "chen", 31.5, 61.0, 14.5, -145.0f);
            QuestManager.advanceQuest(2, "【解救巡检员】", "前往中继大厅与被困的【巡检员·陈风】按 [F] 交谈", new BlockPos(31, 61, 14));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("无线电频道",
                    "干得好！我叫陈风，就在前面阀门中继大厅！我被两台巡逻机堵在里头了，快过来搭把手！", 170));
            saveAndSyncQuest(player);
        } else if (stage == 3) {
            QuestManager.incrementDroneKill();
            int k = QuestManager.getDronesKilled();
            if (k < 2) {
                QuestManager.advanceQuest(3, "【双人清障】", "与陈风配合清除大厅内的安保巡逻机 (" + k + "/2)", new BlockPos(37, 61, 10));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                        "漂亮！还有最后一台巡逻机在东侧出口附近，用我的 [气·对群] 战技收拾它！", 140));
            } else {
                QuestManager.advanceQuest(4, "【前往动力机房】", "按 [F] 同步中继大厅传送锚点并前往下层", new BlockPos(34, 61, 10));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                        "走廊和大厅总算安全了！快按 [F] 同步旁边的传送锚点恢复状态，顺便打开下层升降梯！", 160));
            }
            saveAndSyncQuest(player);
        } else if (stage == 5) {
            ensureNpc(level, "su", 30.5, 36.0, 7.5, -45.0f);
            QuestManager.advanceQuest(5, "【解救医疗员】", "与动力机房内的【医疗员·苏晴】按 [F] 交谈", new BlockPos(30, 36, 8));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                    "终于有人下来了！我是医务室的苏晴，快来我这边会合，备用发电机快撑不住了！", 160));
            saveAndSyncQuest(player);
        } else if (stage == 6) {
            QuestManager.advanceQuest(6, "【重启备用电源】", "击退守卫后，按 [F] 启动【备用发电机组】", new BlockPos(34, 36, 10));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                    "守卫机全清理掉了！快去旁边按 [F] 拉起备用发电机组的合闸开关！", 150));
            saveAndSyncQuest(player);
        } else if (stage == 9) {
            ensureNpc(level, "zheng", 131.5, 61.0, 31.5, 135.0f);
            QuestManager.advanceQuest(10, "【安全屋整备】", "进入科考站与【技术员·老郑】交谈并强化一件装备到 +3", new BlockPos(131, 61, 31));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                    "门口那几只旧机械狗吵死人了！快进安全屋里来，把路上捡到的废弃芯片溶解掉，把主力芯片升到 +3 解锁新词条！", 190));
            saveAndSyncQuest(player);
        } else if (stage == 11) {
            openMineElevatorShaft(level);
            spawnOverworldDrone(level, 154.0, 36.0, 28.0, "矿坑巡逻机");
            QuestManager.advanceQuest(12, "【深入地下矿坑】", "乘东侧矿坑货运梯下行，突破矿道抵达通讯塔控制台", new BlockPos(168, 36, 28));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                    "配电闸门通电了！东边的矿坑货运梯已经解锁，下到矿井深处找到通讯塔控制台，但小心下面那台大型挖掘机！", 180));
            saveAndSyncQuest(player);
        } else if (stage == 12) {
            QuestManager.advanceQuest(13, "【决战大型工程挖掘机】", "按 [F] 启动【深空通讯塔控制台】，击败大型工程挖掘机", new BlockPos(168, 36, 28));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                    "前面就是通讯塔控制台了！大家状态补满、调整好芯片和战技点，准备迎战大型挖掘机！", 160));
            saveAndSyncQuest(player);
        } else if ("boss_excavator".equals(encounterType)) {
            onBossDefeated(player);
        }
    }

    public static void onWaypointActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        player.setHealth(player.getMaxHealth());
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            for (var c : team.getActivePartyMembers()) {
                c.setCurrentHp(c.getTotalMaxHp());
            }
            PacketSyncTeamData.syncToPlayer(player);
        });
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 25, 0.4, 0.5, 0.4, 0.1);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.4f);

        // Ice Planet Outpost Waypoint (x > 90)
        if (pos.getX() > 90) {
            QuestManager.setFlag(QuestManager.FLAG_ICE_WAYPOINT);
            if (QuestManager.getQuestStage() == 8) {
                checkStage8Progress(player);
            } else if (QuestManager.getQuestStage() >= 15) {
                player.teleportTo(level, 34.5, 61.0, 10.5, 270.0f, 0.0f);
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                        "已返回重拾希望号中继大厅。全队生命已回满。", 120));
            } else {
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("冰原锚点",
                        "小队全员生命已恢复。继续向东探索地面科考站！", 110));
            }
            return;
        }

        // Starship Waypoint
        QuestManager.setFlag(QuestManager.FLAG_WAYPOINT_ACTIVATED);
        if (QuestManager.getQuestStage() == 4) {
            openElevatorShaft(level);
            spawnOverworldDrone(level, 28.5, 36.0, 10.0, "机房巡逻机");
            QuestManager.advanceQuest(5, "【解救医疗员】", "沿升降梯下到动力机房，击退守卫并救援【医疗员·苏晴】", new BlockPos(28, 36, 10));
            level.playSound(null, new BlockPos(35, 60, 15), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.1f, 0.85f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                    "锚点同步完成、全队满血！旁边的下层升降梯已解锁，下去救援苏晴！", 160));
            saveAndSyncQuest(player);
        } else if (QuestManager.getQuestStage() >= 15) {
            player.teleportTo(level, 114.5, 61.0, 28.5, 270.0f, 0.0f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                    "已快捷传送至冰原科考站前哨营地。", 100));
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                    "小队全员生命已补满。", 90));
        }
    }

    public static void handleNpcAction(ServerPlayer player, String npcId, String actionKey) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();

        switch (actionKey) {
            case "join_chen" -> {
                if (stage <= 2) {
                    player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                        team.unlockRosterCharacter("chenfeng", true);
                        PacketSyncTeamData.syncToPlayer(player);
                    });
                    grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 2));
                    grantItem(player, new ItemStack(ModItems.CREDIT_COIN.get(), 20));
                    QuestManager.resetDroneKills(2);
                    spawnOverworldDrone(level, 32.0, 61.0, 6.0, "安保巡逻机·甲");
                    spawnOverworldDrone(level, 37.0, 61.0, 10.0, "安保巡逻机·乙");
                    QuestManager.advanceQuest(3, "【双人清障】", "配合陈风击退大厅里的安保巡逻机 (0/2)", new BlockPos(32, 61, 6));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                            "我入队了！我是【气属性·对群】职业，战斗里让林野普攻攒战技点，我用战技扫平对面！", 180));
                    saveAndSyncQuest(player);
                }
            }
            case "join_su" -> {
                if (stage <= 5) {
                    player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                        team.unlockRosterCharacter("suqing", true);
                        PacketSyncTeamData.syncToPlayer(player);
                    });
                    grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 2));
                    spawnOverworldDrone(level, 31.0, 36.0, 12.0, "重装守卫机");
                    QuestManager.advanceQuest(6, "【重启备用电源】", "清理动力室重装守卫机，然后启动【备用发电机组】", new BlockPos(31, 36, 12));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                            "我加入小队！我是【水属性·治疗】职业，血线危险时用我的战技回血，能量满按 [1-4] 随时插队大招救场！", 190));
                    saveAndSyncQuest(player);
                }
            }
            case "zheng_outpost_guide" -> {
                if (stage == 10) {
                    checkStage10Progress(player);
                }
            }
            case "finish_tutorial" -> {
                if (stage == 14) {
                    grantItem(player, new ItemStack(ModItems.STAR_TICKET.get(), 20));
                    grantItem(player, new ItemStack(ModItems.CREDIT_COIN.get(), 100));
                    grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 5));
                    QuestManager.advanceQuest(15, "【新手篇完结·自由探索】",
                            "按 [G] 体验抽卡跃迁，或在终端按 [E] 挑战模拟副本提升世界等级！", new BlockPos(130, 61, 33));
                    level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("重拾希望号通讯网",
                            "已接通母舰主力信号！赠送 20 张【抽卡通行券】，按 [G] 招募更多队友，按 [E] 或访问终端刷取养成材料吧！", 240));
                    saveAndSyncQuest(player);
                }
            }
        }
    }

    public static void onCharacterUpgraded(ServerPlayer player, int newCharLevel) {
        if (QuestManager.getQuestStage() == 8 && newCharLevel >= 3) {
            QuestManager.setFlag(QuestManager.FLAG_CHAR_LEVELED);
            checkStage8Progress(player);
        }
    }

    public static void onChipEnhanced(ServerPlayer player, int newEnhanceLevel) {
        if (QuestManager.getQuestStage() == 10 && newEnhanceLevel >= 3) {
            QuestManager.setFlag(QuestManager.FLAG_CHIP_ENHANCED);
            checkStage10Progress(player);
        }
    }

    private static void checkStage8Progress(ServerPlayer player) {
        if (QuestManager.getQuestStage() != 8) return;
        boolean wpDone = QuestManager.hasFlag(QuestManager.FLAG_ICE_WAYPOINT);
        boolean lvDone = QuestManager.hasFlag(QuestManager.FLAG_CHAR_LEVELED);
        if (wpDone && lvDone) {
            spawnOverworldDrone(player.getLevel(), 123.5, 61.0, 28.0, "旧式机械猎犬");
            QuestManager.advanceQuest(9, "【前往地面科考站】", "向东前往废弃地面科考站，消灭堵门的旧式机械猎犬", new BlockPos(123, 61, 28));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                    "人物也升级了，冰原锚点也通了！前面灯光亮着的地方好像是个废弃科考站，我们过去看看！", 170));
            saveAndSyncQuest(player);
        } else {
            String need = !wpDone ? "按 [F] 激活冰原锚点" : "按 [C] 用经验瓶将任意角色升到 Lv.3";
            QuestManager.advanceQuest(8, "【迫降冰裂谷】", need + "（开残骸箱子拿经验瓶）",
                    wpDone ? new BlockPos(110, 61, 25) : new BlockPos(114, 61, 28));
            saveAndSyncQuest(player);
        }
    }

    private static void checkStage10Progress(ServerPlayer player) {
        if (QuestManager.getQuestStage() != 10) return;
        if (QuestManager.hasFlag(QuestManager.FLAG_CHIP_ENHANCED)) {
            spawnOverworldDrone(player.getLevel(), 141.5, 61.0, 28.0, "配电室守卫机");
            QuestManager.advanceQuest(11, "【打通配电开关】", "前往东侧配电房清理守卫机，打开地下矿坑升降梯", new BlockPos(141, 61, 28));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                    "装备强化到 +3 解锁副词条了吧！现在去东边配电房把机器清理了，下面矿坑里还有座深空通讯塔！", 180));
            saveAndSyncQuest(player);
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                    "拿好箱子里的备用芯片，按 [B] 溶解 2 个不需要的 2 级芯片换强化粉尘，再按 [C] 把常用芯片升到 +3！", 180));
        }
    }

    public static void onCoreRepaired(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        if (QuestManager.getQuestStage() == 6 && !QuestManager.hasFlag(QuestManager.FLAG_CORE_REPAIRED)) {
            QuestManager.setFlag(QuestManager.FLAG_CORE_REPAIRED);
            level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 45, 0.5, 0.5, 0.5, 0.15);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 1.5f);

            QuestManager.advanceQuest(7, "【船体断裂撤离】", "主龙骨开裂！立刻前往西侧搭乘【轨道逃生舱】脱离飞船", new BlockPos(22, 36, 10));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("船体警报",
                    "【紧急】主龙骨结构断裂不可逆！本舱段将在 60 秒后解体，全员立刻撤往西侧轨道逃生舱！", 200));
            saveAndSyncQuest(player);
        } else {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.2f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("备用发电机组", "备用电力维持运转中。", 80));
        }
    }

    public static void onDropPodActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();
        if (stage < 7) {
            level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.8f, 0.7f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("轨道逃生舱",
                    "逃生舱供电尚未恢复，请先完成当前机房任务！", 100));
            return;
        }
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2f, 0.9f);
        player.teleportTo(level, 108.5, 61.0, 28.5, 270.0f, 0.0f);
        level.playSound(null, new BlockPos(108, 61, 28), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.7f, 1.2f);

        if (stage == 7) {
            QuestManager.advanceQuest(8, "【迫降冰裂谷】", "搜索残骸物资箱，将任意角色升到 Lv.3 并激活冰原锚点", new BlockPos(110, 61, 25));
            saveAndSyncQuest(player);
        }
        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                "咳咳……总算砸进雪堆停下来了！大家先翻翻逃生舱旁的箱子拿经验瓶升级，再去激活旁边的冰原锚点！", 190));
    }

    public static void onPrecursorAltarActivated(ServerPlayer player, BlockPos pos) {
        int stage = QuestManager.getQuestStage();
        if (stage < 12) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("深空通讯塔控制台",
                    "请先完成地面科考站的准备工作再进入矿坑深处！", 90));
            return;
        }
        TurnBasedCombatManager.startBossBattle(player);
    }

    public static void onBossDefeated(ServerPlayer player) {
        int stage = QuestManager.getQuestStage();
        if (stage <= 13) {
            ensureNpc(player.getLevel(), "zheng", 131.5, 61.0, 31.5, 135.0f);
            QuestManager.advanceQuest(14, "【重连母舰·教程完成】", "返回科考站安全屋向【技术员·老郑】报信领取抽卡通行券", new BlockPos(131, 61, 31));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("通讯塔控制台",
                    "大型工程挖掘机已击毁！高能反应堆零件已装入，快回地面科考站找老郑启动深空广播！", 200));
            saveAndSyncQuest(player);
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("战利品系统",
                    "大型工程挖掘机已再度击破！奖励已存入背包。", 120));
        }
    }
}
