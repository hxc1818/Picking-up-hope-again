package com.resilienthope.core.quest;

import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.entity.PrecursorMechBossEntity;
import com.resilienthope.core.entity.SecurityDroneEntity;
import com.resilienthope.core.init.ModEntities;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.network.PacketSyncQuest;
import com.resilienthope.core.network.PacketSyncTeamData;
import com.resilienthope.core.team.CharacterData;
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
            level.setBlock(new BlockPos(32, y, 14), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(33, y, 14), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), 2);
            level.setBlock(new BlockPos(34, y, 14), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(35, y, 14), Blocks.AIR.defaultBlockState(), 2);
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

        QuestManager.resetDroneKills(2);
        QuestManager.advanceQuest(1, "【序章·失控防线】", "清除维修连廊中的失控安保无人机 (0/2)", new BlockPos(18, 62, 10));

        // Spawn 2 drones in the corridor ahead of the player
        AABB corridorBox = new AABB(13, 60, 5, 25, 67, 15);
        List<SecurityDroneEntity> existing = level.getEntitiesOfClass(SecurityDroneEntity.class, corridorBox);
        if (existing.isEmpty()) {
            SecurityDroneEntity d1 = ModEntities.SECURITY_DRONE.get().create(level);
            if (d1 != null) {
                d1.moveTo(18.5, 62.5, 9.0, 90.0f, 0.0f);
                level.addFreshEntity(d1);
            }
            SecurityDroneEntity d2 = ModEntities.SECURITY_DRONE.get().create(level);
            if (d2 != null) {
                d2.moveTo(20.0, 62.5, 11.5, 90.0f, 0.0f);
                level.addFreshEntity(d2);
            }
        }

        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("重拾希望号警报系统",
                "【警报】B区维修连廊安保无人机核心已被未知信号覆写！请拔出【等离子重切剑】迎敌！", 140));
        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("机械工会·郑铁山",
                "特工破晓！左键连击、按 [E] 释放等离子突进斩、能量满按 [Q] 释放终极爆发！快清出通道！", 180));

        saveAndSyncQuest(player);
    }

    public static void onDroneKilled(ServerPlayer player) {
        if (player == null) return;
        int stage = QuestManager.getQuestStage();
        if (stage == 1) {
            QuestManager.incrementDroneKill();
            int killed = QuestManager.getDronesKilled();
            int req = 2;
            if (killed < req) {
                QuestManager.advanceQuest(1, "【序章·失控防线】",
                        "清除维修连廊中的失控安保无人机 (" + killed + "/" + req + ")", new BlockPos(18, 62, 10));
            } else {
                QuestManager.advanceQuest(2, "【序章·重工火种】",
                        "前往聚变大厅激活传送锚点，并与总工郑铁山交谈", new BlockPos(32, 61, 10));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("机械工会·郑铁山",
                        "干得漂亮！连廊危机解除了！快进聚变大厅先同步传送锚点，然后来工程台找老郑！", 170));
            }
            saveAndSyncQuest(player);
        } else if (stage == 3) {
            QuestManager.incrementDroneKill();
            int killed = QuestManager.getDronesKilled();
            int req = 3;
            if (killed < req) {
                QuestManager.advanceQuest(3, "【第一幕·深空暗流】",
                        "清剿下层工业甲板重装巡检机 (" + killed + "/" + req + ")", new BlockPos(28, 36, 10));
            } else {
                QuestManager.advanceQuest(4, "【第一幕·三方对峙】",
                        "修复下层【辅助聚变能源核心】", new BlockPos(32, 36, 10));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("深空科学院·苏挽星",
                        "守卫机群已经肃清。特工，立刻按 [F] 重启中央【辅助聚变能源核心】！", 160));
            }
            saveAndSyncQuest(player);
        }
    }

    public static void onWaypointActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        player.setHealth(player.getMaxHealth());
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            CharacterData active = team.getCharacter(team.getActiveSlotIndex());
            active.setCurrentHp(active.getTotalMaxHp());
            PacketSyncTeamData.syncToPlayer(player);
        });
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 25, 0.4, 0.5, 0.4, 0.1);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.4f);

        // Alien surface return waypoint at (112, 61, 28)
        if (pos.getX() > 90) {
            QuestManager.setFlag(QuestManager.FLAG_ALIEN_WAYPOINT);
            if (QuestManager.getQuestStage() == 5) {
                QuestManager.advanceQuest(5, "【第二幕·极寒空投】",
                        "前往中央【先驱高能祭坛】唤醒并击败守护机甲", new BlockPos(132, 61, 28));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("异星地表信标",
                        "极寒矿坑锚点已连接母舰跃迁坐标。前方高能反应接近中——准备迎战【先驱挖掘机甲】！", 160));
                saveAndSyncQuest(player);
            } else if (QuestManager.getQuestStage() >= 7) {
                // Teleport back up to Starship Fusion Hub!
                player.teleportTo(level, 32.5, 61.0, 10.5, 270.0f, 0.0f);
                ensureNpc(level, "han", 30.5, 61.0, 12.5, -60.0f);
                ensureNpc(level, "su", 30.5, 61.0, 8.5, -120.0f);
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("星舰信标中枢",
                        "已跃迁返回【重拾希望号】聚变大厅。请与郑铁山、韩振岳或苏挽星交谈做出终局抉择。", 170));
            } else {
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("异星地表信标",
                        "小队生命体征已恢复。击败先驱祭坛机甲即可开启返舰通道。", 120));
            }
            return;
        }

        // Upper Starship Waypoint at (32, 61, 10)
        QuestManager.setFlag(QuestManager.FLAG_WAYPOINT_ACTIVATED);
        ensureNpc(level, "zheng", 35.5, 61.0, 12.5, 135.0f);

        if (QuestManager.getQuestStage() == 2) {
            QuestManager.advanceQuest(2, "【序章·重工火种】",
                    "与工程台旁的【总工·郑铁山】按 [F] 面对面交谈", new BlockPos(35, 61, 13));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("星舰信标中枢",
                    "星舰主锚点同步完成！小队生命已回满。请走到主控台旁找总工郑铁山接取下层通行权限。", 150));
            saveAndSyncQuest(player);
        } else if (QuestManager.getQuestStage() == 8) {
            // Post-ending quick shuttle to Alien Mine!
            player.teleportTo(level, 108.5, 61.0, 28.5, 270.0f, 0.0f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("星舰信标中枢",
                    "已传送至极寒异星采矿坑前哨站。", 100));
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("星舰信标中枢",
                    "锚点能量充盈，生命维持系统运转正常。", 90));
        }
    }

    public static void handleNpcAction(ServerPlayer player, String npcId, String actionKey) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();

        switch (actionKey) {
            case "advance_stage_2" -> {
                if (stage <= 2) {
                    openElevatorShaft(level);
                    grantItem(player, new ItemStack(ModItems.VOID_DRIVE_CHIP.get()));
                    grantItem(player, new ItemStack(ModItems.ALLOY_PLATE.get(), 6));
                    grantItem(player, new ItemStack(ModItems.QUANTUM_CORE.get(), 10));

                    QuestManager.resetDroneKills(3);
                    QuestManager.advanceQuest(3, "【第一幕·深空暗流】",
                            "乘升降梯进入下层甲板，清剿重装巡检机 (0/3)", new BlockPos(28, 36, 10));

                    // Spawn 3 drones on Lower Deck if absent
                    AABB lowerBox = new AABB(18, 34, 4, 39, 42, 16);
                    if (level.getEntitiesOfClass(SecurityDroneEntity.class, lowerBox).isEmpty()) {
                        double[][] coords = {{27.5, 37.5, 8.0}, {26.0, 37.5, 10.5}, {27.5, 37.5, 13.0}};
                        for (double[] c : coords) {
                            SecurityDroneEntity d = ModEntities.SECURITY_DRONE.get().create(level);
                            if (d != null) {
                                d.moveTo(c[0], c[1], c[2], 0f, 0f);
                                level.addFreshEntity(d);
                            }
                        }
                    }

                    ensureNpc(level, "su", 30.5, 36.0, 8.5, -45.0f);
                    ensureNpc(level, "han", 30.5, 36.0, 11.5, -135.0f);

                    level.playSound(null, new BlockPos(34, 60, 14), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.2f, 0.85f);
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("机械工会·郑铁山",
                            "2号位【脉冲动力发生器】和强化合金已经打进你背包了！按 [B] 或 [C] 装上后，从旁边升降梯井道下去！", 200));
                    saveAndSyncQuest(player);
                }
            }
            case "advance_stage_4" -> {
                if (stage == 4) {
                    grantItem(player, new ItemStack(ModItems.SERVO_BATTERY_CHIP.get()));
                    grantItem(player, new ItemStack(ModItems.OVERCLOCK_CHIP.get()));
                    grantItem(player, new ItemStack(ModItems.CRYO_SAWBLADE.get()));
                    grantItem(player, new ItemStack(ModItems.ALLOY_PLATE.get(), 8));
                    grantItem(player, new ItemStack(ModItems.QUANTUM_CORE.get(), 20));

                    QuestManager.advanceQuest(5, "【第二幕·极寒空投】",
                            "前往下层【近地轨道空投舱】，空降极寒异星矿坑", new BlockPos(22, 36, 10));

                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("深空科学院·苏挽星",
                            "已为你配发 3/4 号位超频芯片与【极寒高频裂解刃】！先按 [B]/[C] 强化装备，再到西侧轨道空投舱出发！", 210));
                    saveAndSyncQuest(player);
                }
            }
            case "choose_zheng", "choose_han", "choose_su" -> {
                if (stage == 7) {
                    String faction = switch (actionKey) {
                        case "choose_han" -> "han";
                        case "choose_su" -> "su";
                        default -> "zheng";
                    };
                    QuestManager.setEndingFaction(faction);
                    grantItem(player, new ItemStack(ModItems.QUANTUM_CORE.get(), 64));
                    grantItem(player, new ItemStack(ModItems.ALLOY_PLATE.get(), 16));

                    String msg = switch (faction) {
                        case "han" -> "【终局达成 · 铁律远征】舰桥重整军事秩序，先驱火种驱动主引擎全速跃迁，人类以钢铁纪律驶向新星域！";
                        case "su" -> "【终局达成 · 新人类黎明】深空科学院解析先驱火种基因共鸣，重拾希望号载着进化与群星的火光启航！";
                        default -> "【终局达成 · 众生薪火】机械工会全面重启民用供暖与公平配给，万家灯火在深空星舰中重燃希望！";
                    };

                    QuestManager.advanceQuest(8, "【终局·星海远征】",
                            "主线完结！按 [G] 终端复现祈愿，或经锚点前往祭坛重战Boss", new BlockPos(32, 61, 10));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("重拾希望号·全舰广播", msg, 260));
                    level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    saveAndSyncQuest(player);
                }
            }
        }
    }

    public static void onCoreRepaired(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        if (QuestManager.getQuestStage() == 4 && !QuestManager.hasFlag(QuestManager.FLAG_CORE_REPAIRED)) {
            QuestManager.setFlag(QuestManager.FLAG_CORE_REPAIRED);
            ensureNpc(level, "su", 30.5, 36.0, 8.5, -45.0f);
            ensureNpc(level, "han", 30.5, 36.0, 11.5, -135.0f);

            level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 45, 0.5, 0.5, 0.5, 0.15);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 1.5f);

            QuestManager.advanceQuest(4, "【第一幕·三方对峙】",
                    "与核心室的【苏挽星】或【韩振岳】按 [F] 交谈", new BlockPos(30, 36, 10));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("舰桥防卫总长·韩振岳",
                    "辅助反应堆重燃了！但主炉燃料只剩最后两周——苏首席，你提议空降极寒死星，究竟有多少把握？", 190));
            saveAndSyncQuest(player);
        } else {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.2f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("辅助聚变核心",
                    "磁场约束稳定，下层甲板供电输出 100%。", 80));
        }
    }

    public static void onDropPodActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();
        if (stage < 5) {
            level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.8f, 0.7f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("近地轨道空投舱",
                    "【发射锁定】尚未获得下层核心数据授权，请先推进当前主线阶段！", 110));
            return;
        }
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2f, 0.9f);
        player.teleportTo(level, 108.5, 61.0, 28.5, 270.0f, 0.0f);
        level.playSound(null, new BlockPos(108, 61, 28), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.7f, 1.2f);

        if (stage == 5 && !QuestManager.hasFlag(QuestManager.FLAG_ALIEN_WAYPOINT)) {
            QuestManager.advanceQuest(5, "【第二幕·极寒空投】",
                    "激活异星地表锚点，并前往【先驱高能祭坛】", new BlockPos(112, 61, 28));
            saveAndSyncQuest(player);
        }
        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("极寒空投着陆指示",
                "已抵达【极寒死星·先驱采矿坑】！地表零下 68℃，能量雷达侦测到高能机甲信号！", 170));
    }

    public static void onPrecursorAltarActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();
        if (stage < 5) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("先驱高能祭坛",
                    "尚未解析先驱频段，请先完成前序主线任务。", 90));
            return;
        }
        AABB arenaBox = new AABB(104, 58, 4, 152, 80, 52);
        if (!level.getEntitiesOfClass(PrecursorMechBossEntity.class, arenaBox).isEmpty()) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("先驱高能祭坛",
                    "【警告】先驱重型挖掘机甲·泰坦已在场中激活！", 80));
            return;
        }

        PrecursorMechBossEntity boss = ModEntities.PRECURSOR_MECH_BOSS.get().create(level);
        if (boss != null) {
            boss.moveTo(136.5, 62.0, 28.5, 90.0f, 0.0f);
            level.addFreshEntity(boss);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.1f, 0.9f);

        if (stage == 5) {
            QuestManager.setFlag(QuestManager.FLAG_BOSS_SPAWNED);
            QuestManager.advanceQuest(6, "【第二幕·冻土机神】",
                    "击败【先驱重型挖掘机甲·泰坦】，夺取先驱聚变火种", new BlockPos(132, 61, 28));
            saveAndSyncQuest(player);
        }
        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("深空科学院·苏挽星",
                "小心！沉睡千年的先驱挖掘机甲苏醒了！注意躲避它的地震砸击，用全套战技与爆发击穿装甲！", 190));
    }

    public static void onBossDefeated(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();

        grantItem(player, new ItemStack(ModItems.QUANTUM_CORE.get(), 32));
        grantItem(player, new ItemStack(ModItems.ALLOY_PLATE.get(), 10));

        if (stage == 6) {
            grantItem(player, new ItemStack(ModItems.PRECURSOR_CLAYMORE.get()));
            grantItem(player, new ItemStack(ModItems.NEURAL_HALO_CHIP.get()));
            grantItem(player, new ItemStack(ModItems.PRECURSOR_SPARK.get()));
            grantItem(player, new ItemStack(ModItems.QUANTUM_CORE.get(), 32));

            ensureNpc(level, "zheng", 35.5, 61.0, 12.5, 135.0f);
            ensureNpc(level, "han", 30.5, 61.0, 12.5, -60.0f);
            ensureNpc(level, "su", 30.5, 61.0, 8.5, -120.0f);

            QuestManager.advanceQuest(7, "【终章·希望的航向】",
                    "经异星锚点返舰，与三方代表交谈决定人类命运", new BlockPos(35, 61, 13));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("重拾希望号·全舰通讯",
                    "【大捷】先驱机甲已击毁！五星专武【先驱超导重剑】与【先驱聚变火种】已回收！请经异星锚点返回星舰做出终局抉择！", 240));
            saveAndSyncQuest(player);
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("战利品系统",
                    "机甲Boss已再度击破！获得星辉算力芯片 x32 与重工强化合金 x10！", 140));
        }
    }
}
