package com.resilienthope.core.quest;

import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.entity.SecurityDroneEntity;
import com.resilienthope.core.init.ModBlocks;
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

import java.util.ArrayList;
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

    public static void openSleepCorridorGate(ServerLevel level) {
        for (int y = 61; y <= 63; y++) {
            for (int z = 9; z <= 10; z++) {
                level.setBlock(new BlockPos(12, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    public static void openHallCorridorGate(ServerLevel level) {
        for (int y = 61; y <= 63; y++) {
            for (int z = 9; z <= 10; z++) {
                level.setBlock(new BlockPos(25, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    public static void openElevatorShaft(ServerLevel level) {
        for (int y = 36; y <= 60; y++) {
            level.setBlock(new BlockPos(34, y, 15), ModBlocks.STARSHIP_WALL.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(35, y, 15), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), 2);
            level.setBlock(new BlockPos(36, y, 15), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public static void openMineElevatorShaft(ServerLevel level) {
        for (int y = 36; y <= 60; y++) {
            level.setBlock(new BlockPos(145, y, 28), ModBlocks.PERMAFROST_ROCK.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(146, y, 28), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), 2);
            level.setBlock(new BlockPos(147, y, 28), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public static void spawnOverworldDrone(ServerLevel level, double x, double y, double z, String customName) {
        AABB box = new AABB(x - 3, y - 3, z - 3, x + 3, y + 4, z + 3);
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

    public static void onSupplyCrateOpened(ServerPlayer player, BlockPos pos) {
        String crateTag = "hope_crate_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        if (player.getTags().contains(crateTag)) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("装甲物资箱", "该物资箱内的补给已全部搜刮完毕。", 80));
            return;
        }
        player.addTag(crateTag);
        ServerLevel level = player.getLevel();
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.9f, 1.35f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 18, 0.3, 0.3, 0.3, 0.08);

        List<String> got = new ArrayList<>();
        int stage = QuestManager.getQuestStage();

        if (pos.getX() <= 10 && pos.getY() >= 55) {
            // 1. Sleep Room Crate (7, 61, 10)
            QuestManager.setFlag(QuestManager.FLAG_CHEST_SLEEP);
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.FUSION_CORE_CHIP.get(), 2, player.getRandom()));
            grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 4));
            grantItem(player, new ItemStack(ModItems.CREDIT_COIN.get(), 30));
            got.add("2★生命芯片 x1、经验瓶 x4、信用币 x30");
            if (stage == 0) {
                QuestManager.advanceQuest(1, "【抢修走廊闸门】", "按 [F] 启动休眠室东门旁的【走廊闸门开关】", new BlockPos(11, 61, 8));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("林野",
                        "拿到基础芯片与经验瓶了！按 [B] 查看背包或按 [C] 装配。现在去门口按 [F] 打开走廊应急闸门！", 170));
            }
        } else if (pos.getX() >= 17 && pos.getX() <= 24 && pos.getY() >= 55) {
            // 2. Storage Room Crate (20, 61, 3)
            QuestManager.setFlag(QuestManager.FLAG_CHEST_STORAGE);
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.VOID_DRIVE_CHIP.get(), 2, player.getRandom()));
            grantItem(player, new ItemStack(ModItems.CHAR_MAT_1.get(), 3));
            grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 2));
            got.add("2★攻击驱动芯片 x1、1级人物零件 x3、经验瓶 x2");
            if (stage == 3) {
                openHallCorridorGate(level);
                ensureNpc(level, "chen", 31.5, 61.0, 14.5, -145.0f);
                QuestManager.advanceQuest(4, "【解救巡检员】", "穿过走廊东门，前往中继大厅与【巡检员·陈风】按 [F] 交谈", new BlockPos(31, 61, 14));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("无线电频道",
                        "干得好！我是陈风，就在前面阀门中继大厅！东侧通道门解锁了，我被巡逻机困在里头，快过来！", 180));
            }
        } else if (pos.getX() >= 40 && pos.getX() <= 48 && pos.getY() >= 55) {
            // 3. Bridge Data Room Crate (44, 61, 3)
            QuestManager.setFlag(QuestManager.FLAG_CHEST_BRIDGE);
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.SERVO_BATTERY_CHIP.get(), 3, player.getRandom()));
            grantItem(player, new ItemStack(ModItems.WEAPON_MAT_1.get(), 3));
            grantItem(player, new ItemStack(ModItems.CREDIT_COIN.get(), 40));
            got.add("3★能源电池芯片 x1、1级武器构件 x3、信用币 x40");
            if (stage == 7) {
                QuestManager.advanceQuest(8, "【同步中继锚点】", "返回大厅按 [F] 激活【传送锚点】回满血量并开启下层升降梯", new BlockPos(34, 61, 10));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                        "拿到下层权限密钥和 3★ 电池芯片了！去中继大厅同步那座【传送锚点】，好打开下层通道！", 170));
            }
        } else if (pos.getX() < 85 && pos.getY() < 45) {
            // 4. Lower Deck Medbay Crate (32, 36, 5)
            QuestManager.setFlag(QuestManager.FLAG_CHEST_MEDBAY);
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.FUSION_CORE_CHIP.get(), 3, player.getRandom()));
            grantItem(player, new ItemStack(ModItems.TALENT_MAT_1.get(), 3));
            grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 4));
            got.add("3★生命控制芯片 x1、1级天赋手册 x3、经验瓶 x4");
            if (stage == 11) {
                checkStage11Progress(player);
            }
        } else if (pos.getX() >= 105 && pos.getX() <= 120 && pos.getY() >= 55) {
            // 5. Ice Wreckage Crate (110, 61, 25)
            QuestManager.setFlag(QuestManager.FLAG_CHEST_WRECK);
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.VOID_DRIVE_CHIP.get(), 2, player.getRandom()));
            grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 6));
            grantItem(player, new ItemStack(ModItems.CHAR_MAT_1.get(), 4));
            grantItem(player, new ItemStack(ModItems.EQUIP_ESSENCE.get(), 2));
            got.add("2★攻击驱动芯片 x1、经验瓶 x6、人物零件 x4、强化粉尘 x2");
            if (stage == 15) {
                checkStage15Progress(player);
            }
        } else if (pos.getX() >= 125 && pos.getY() >= 55) {
            // 6. Outpost Supply Crate (133, 61, 33)
            QuestManager.setFlag(QuestManager.FLAG_CHEST_OUTPOST);
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.OVERCLOCK_CHIP.get(), 3, player.getRandom()));
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.OVERCLOCK_CHIP.get(), 2, player.getRandom()));
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.OVERCLOCK_CHIP.get(), 2, player.getRandom()));
            grantItem(player, new ItemStack(ModItems.EQUIP_ESSENCE.get(), 3));
            grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 4));
            got.add("3★增幅芯片 x1、2★备用芯片 x2、强化粉尘 x3、经验瓶 x4");
            if (stage == 18) {
                checkStage18Progress(player);
            }
        } else {
            // 7. Mine Engineering Crate (158, 36, 24)
            QuestManager.setFlag(QuestManager.FLAG_CHEST_MINE);
            grantItem(player, EquipmentStatsHelper.createChipWithRarity(ModItems.OVERCLOCK_CHIP.get(), 4, player.getRandom()));
            grantItem(player, new ItemStack(ModItems.EQUIP_ESSENCE.get(), 6));
            grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 6));
            grantItem(player, new ItemStack(ModItems.CHAR_MAT_2.get(), 2));
            got.add("4★属性增幅芯片 x1、强化粉尘 x6、经验瓶 x6、2级人物零件 x2");
            if (stage == 21) {
                checkStage21Progress(player);
            }
        }

        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("搜寻物资箱",
                "已获得：" + String.join("，", got) + "（按 [B] 查看背包）", 150));
        saveAndSyncQuest(player);
    }

    public static void onCorridorSwitchActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        openSleepCorridorGate(level);
        if (QuestManager.getQuestStage() <= 1) {
            spawnOverworldDrone(level, 17.5, 61.0, 10.0, "故障清扫机");
            QuestManager.advanceQuest(2, "【走廊初战】", "左键攻击走廊里的【故障清扫机】，先手切入回合制战斗", new BlockPos(17, 61, 10));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("船舱警报",
                    "走廊闸门已强制开启！前方有故障清扫机挡路，靠近左键挥刀先手进战，实战体验林野的技能连招！", 170));
            saveAndSyncQuest(player);
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("闸门控制端", "休眠区走廊闸门维持开启状态。", 80));
        }
    }

    public static void onBattleWon(ServerPlayer player, String encounterType) {
        if (player == null) return;
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();

        if (encounterType != null && encounterType.startsWith("domain_")) {
            QuestManager.setFlag(QuestManager.FLAG_DOMAIN_CLEARED);
            if (stage == 19) {
                spawnOverworldDrone(level, 141.0, 61.0, 28.0, "配电室重装守卫");
                QuestManager.advanceQuest(20, "【夺回东侧配电室】", "向东清理【配电室重装守卫】，并按 [F] 合闸打开矿坑货运梯", new BlockPos(141, 61, 28));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                        "演练手感不错！现在去安全屋东侧配电房解决守卫机并合闸通电，咱们下矿坑重启通讯塔！", 170));
                saveAndSyncQuest(player);
            }
            return;
        }

        switch (stage) {
            case 2 -> {
                openHallCorridorGate(level);
                QuestManager.advanceQuest(3, "【搜寻维修储藏室】", "进入走廊北侧储藏室，按 [F] 开启【储藏室物资箱】", new BlockPos(20, 61, 3));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("林野",
                        "清扫机摆平了。北侧有个维修储藏室亮着灯，去拿上里面的攻击芯片，再去前边大厅找人！", 160));
                saveAndSyncQuest(player);
            }
            case 5 -> {
                QuestManager.incrementDroneKill();
                int k = QuestManager.getDronesKilled();
                if (k < 2) {
                    QuestManager.advanceQuest(5, "【双人清障】", "配合陈风清除中继大厅巡逻机 (" + k + "/2)", new BlockPos(37, 61, 11));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                            "漂亮！东边还有一台巡逻机，用我的 [气·对群] 战技一口气清理掉！", 140));
                } else {
                    spawnOverworldDrone(level, 42.0, 61.0, 5.0, "舰桥防卫统御机");
                    QuestManager.advanceQuest(6, "【舰桥数据室平叛】", "前往东北侧舰桥数据室，击败【舰桥防卫统御机】", new BlockPos(42, 61, 5));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                            "大厅总算干净了！下层升降梯权限还锁在东北角的舰桥数据室里，咱俩一起打过去！", 160));
                }
                saveAndSyncQuest(player);
            }
            case 6 -> {
                if (QuestManager.hasFlag(QuestManager.FLAG_CHEST_BRIDGE)) {
                    QuestManager.advanceQuest(8, "【同步中继锚点】", "前往大厅按 [F] 激活【传送锚点】并开启下层升降梯", new BlockPos(34, 61, 10));
                } else {
                    QuestManager.advanceQuest(7, "【获取舰桥密钥】", "按 [F] 开启数据室里的【舰桥物资箱】拿取权限与芯片", new BlockPos(44, 61, 3));
                }
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                        "统御机倒下了！快打开角落的舰桥物资箱拿上电池芯片和密钥！", 150));
                saveAndSyncQuest(player);
            }
            case 9 -> {
                ensureNpc(level, "su", 30.5, 36.0, 7.5, -45.0f);
                QuestManager.advanceQuest(10, "【解救医疗员】", "在动力室医务隔间与【医疗员·苏晴】按 [F] 交谈", new BlockPos(30, 36, 7));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                        "终于有人下来了！我是医务室的苏晴，快来我这边会合，机房深处还有重装守卫把持着发电机！", 170));
                saveAndSyncQuest(player);
            }
            case 12 -> {
                QuestManager.advanceQuest(13, "【重启备用电源】", "按 [F] 启动动力机房中央的【备用发电机组】", new BlockPos(34, 36, 10));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                        "重装守卫机清理掉了！快去旁边按 [F] 拉起【备用发电机组】的合闸开关！", 150));
                saveAndSyncQuest(player);
            }
            case 16 -> {
                QuestManager.incrementDroneKill();
                int k = QuestManager.getDronesKilled();
                if (k < 2) {
                    QuestManager.advanceQuest(16, "【突破冰谷防线】", "清理拦路的冰原巡逻单位 (" + k + "/2)", new BlockPos(124, 61, 28));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                            "还有一波机械猎犬守在科考站门口，注意多用全队护血和属性破盾！", 140));
                } else {
                    ensureNpc(level, "zheng", 131.5, 61.0, 31.5, 135.0f);
                    QuestManager.advanceQuest(17, "【会见留守技术员】", "进入废弃科考站与【技术员·老郑】按 [F] 交谈", new BlockPos(131, 61, 31));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                            "外头总算消停了！小伙子们快进安全屋暖和一下，我有要紧事跟你们交代！", 160));
                }
                saveAndSyncQuest(player);
            }
            case 20 -> {
                QuestManager.setFlag(QuestManager.FLAG_SUBSTATION_GUARD_DEAD);
                checkStage20Progress(player);
            }
            case 21 -> {
                QuestManager.setFlag(QuestManager.FLAG_MINE_PATROL_DEAD);
                checkStage21Progress(player);
            }
            case 22 -> {
                QuestManager.advanceQuest(23, "【决战大型工程挖掘机】", "登上矿坑高台按 [F] 启动【深空通讯塔控制台】，击败大型挖掘机", new BlockPos(168, 36, 28));
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                        "钻探重机解决了！前面就是通讯塔控制台，大家补好状态、检查好装备和战技点，准备迎战最后的双阶段大Boss！", 180));
                saveAndSyncQuest(player);
            }
            default -> {
                if ("boss_excavator".equals(encounterType)) {
                    onBossDefeated(player);
                }
            }
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

        if (pos.getX() > 90) {
            QuestManager.setFlag(QuestManager.FLAG_WAYPOINT_ICE);
            if (QuestManager.getQuestStage() == 15) {
                checkStage15Progress(player);
            } else if (QuestManager.getQuestStage() >= 24) {
                player.teleportTo(level, 34.5, 61.0, 12.5, 270.0f, 0.0f);
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                        "已传送返回重拾希望号中继大厅，全队状态已恢复满格。", 110));
            } else {
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("冰原锚点",
                        "全队生命与状态已恢复。", 100));
            }
            return;
        }

        QuestManager.setFlag(QuestManager.FLAG_WAYPOINT_SHIP);
        if (QuestManager.getQuestStage() == 8) {
            openElevatorShaft(level);
            spawnOverworldDrone(level, 29.0, 36.0, 10.0, "机房巡逻队长");
            QuestManager.advanceQuest(9, "【深入下层机房】", "沿东侧升降梯下到动力机房，击败【机房巡逻队长】", new BlockPos(29, 36, 10));
            level.playSound(null, new BlockPos(35, 60, 15), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.1f, 0.85f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                    "锚点同步成功、全队回满！旁边的下层升降梯竖井已开启，沿梯下去救援苏晴！", 170));
            saveAndSyncQuest(player);
        } else if (QuestManager.getQuestStage() >= 24) {
            player.teleportTo(level, 114.5, 61.0, 28.5, 270.0f, 0.0f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                    "已快捷传送至冰原科考站前哨锚点。", 100));
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("传送锚点",
                    "全队生命已恢复满格。", 90));
        }
    }

    public static void handleNpcAction(ServerPlayer player, String npcId, String actionKey) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();

        switch (actionKey) {
            case "join_chen" -> {
                if (stage <= 4) {
                    player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                        team.unlockRosterCharacter("chenfeng", true);
                        PacketSyncTeamData.syncToPlayer(player);
                    });
                    grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 2));
                    grantItem(player, new ItemStack(ModItems.CREDIT_COIN.get(), 20));
                    QuestManager.resetDroneKills(2);
                    spawnOverworldDrone(level, 32.0, 61.0, 6.0, "安保巡逻机·甲");
                    spawnOverworldDrone(level, 37.0, 61.0, 11.0, "安保巡逻机·乙");
                    QuestManager.advanceQuest(5, "【双人清障】", "配合陈风清理大厅里的安保巡逻机 (0/2)", new BlockPos(32, 61, 6));
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                            "我入队了！我是【气属性·对群】输出，待会打巡逻机我给你演示【旋风横扫】与破盾自动【乘风追击】！", 180));
                    saveAndSyncQuest(player);
                }
            }
            case "join_su" -> {
                if (stage <= 10) {
                    player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                        team.unlockRosterCharacter("suqing", true);
                        PacketSyncTeamData.syncToPlayer(player);
                    });
                    grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 2));
                    QuestManager.advanceQuest(11, "【医疗补给与整备】", "打开医务室物资箱并用经验瓶将任意角色升至 Lv.2", new BlockPos(32, 36, 5));
                    checkStage11Progress(player);
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                            "我入队啦！我是【水属性·治疗】，待会清重装守卫时正好给你演示全队【纳米水雾急救】与大招回点！先开医疗箱并升到 Lv.2 吧！", 190));
                    saveAndSyncQuest(player);
                }
            }
            case "zheng_outpost_guide" -> {
                if (stage <= 17) {
                    QuestManager.advanceQuest(18, "【装备溶解与强化】", "开启安全屋物资箱，按 [B] 溶解芯片并在 [C] 强化任意芯片到 +3", new BlockPos(133, 61, 33));
                    checkStage18Progress(player);
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                            "先拿屋角物资箱里的备用芯片，按 [B] 溶解不需要的 2★ 芯片换强化粉尘，然后按 [C] 把主力芯片升到 +3 解锁副词条！", 190));
                    saveAndSyncQuest(player);
                }
            }
            case "finish_tutorial" -> {
                if (stage == 24) {
                    grantItem(player, new ItemStack(ModItems.STAR_TICKET.get(), 20));
                    grantItem(player, new ItemStack(ModItems.CREDIT_COIN.get(), 150));
                    grantItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 6));
                    QuestManager.advanceQuest(25, "【自由探索·星海远征】",
                            "按 [G] 抽取新同伴，按 [E] 挑战 6 大模拟副本并突破世界等级！", new BlockPos(130, 61, 33));
                    level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("重拾希望号通讯网",
                            "主线信号接通！恭喜获得 20 张【抽卡通行券】，按 [G] 招募韩冰/周敏/老郑，按 [E] 挑战更高难度副本吧！", 240));
                    saveAndSyncQuest(player);
                }
            }
        }
    }

    public static void onCharacterUpgraded(ServerPlayer player, int newCharLevel) {
        if (newCharLevel >= 2) QuestManager.setFlag(QuestManager.FLAG_CHAR_LV2);
        if (newCharLevel >= 3) QuestManager.setFlag(QuestManager.FLAG_CHAR_LV3);

        if (QuestManager.getQuestStage() == 11) {
            checkStage11Progress(player);
        } else if (QuestManager.getQuestStage() == 15) {
            checkStage15Progress(player);
        }
    }

    public static void onChipEnhanced(ServerPlayer player, int newEnhanceLevel) {
        if (newEnhanceLevel >= 3) {
            QuestManager.setFlag(QuestManager.FLAG_CHIP_PLUS3);
        }
        if (QuestManager.getQuestStage() == 18) {
            checkStage18Progress(player);
        }
    }

    private static void checkStage11Progress(ServerPlayer player) {
        if (QuestManager.getQuestStage() != 11) return;
        boolean crate = QuestManager.hasFlag(QuestManager.FLAG_CHEST_MEDBAY);
        boolean lv2 = QuestManager.hasFlag(QuestManager.FLAG_CHAR_LV2);
        if (crate && lv2) {
            spawnOverworldDrone(player.getLevel(), 33.0, 36.0, 13.0, "重装机房守卫机");
            QuestManager.advanceQuest(12, "【肃清主动力室】", "击败守卫发电机的【重装机房守卫机】", new BlockPos(33, 36, 13));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("医疗员·苏晴",
                    "补给与战力准备就绪！前面的重装守卫护甲较厚，注意先破盾再集火！", 160));
            saveAndSyncQuest(player);
        } else {
            String msg = !crate ? "按 [F] 开启【医疗物资箱】 (32,36,5)" : "按 [C] 用经验瓶将任意角色升到 Lv.2";
            QuestManager.advanceQuest(11, "【医疗补给与整备】", msg, new BlockPos(32, 36, 5));
            saveAndSyncQuest(player);
        }
    }

    private static void checkStage15Progress(ServerPlayer player) {
        if (QuestManager.getQuestStage() != 15) return;
        boolean crate = QuestManager.hasFlag(QuestManager.FLAG_CHEST_WRECK);
        boolean wp = QuestManager.hasFlag(QuestManager.FLAG_WAYPOINT_ICE);
        if (crate && wp) {
            QuestManager.resetDroneKills(2);
            spawnOverworldDrone(player.getLevel(), 119.0, 61.0, 28.0, "冰原机械猎犬");
            spawnOverworldDrone(player.getLevel(), 124.0, 61.0, 28.0, "科考站外围防卫机");
            QuestManager.advanceQuest(16, "【突破冰谷防线】", "向东推进科考站，清理沿途封锁的机械单位 (0/2)", new BlockPos(119, 61, 28));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                    "冰原锚点接通了，物资也拿齐！东边有科考站灯光，咱们杀过去打探情况！", 170));
            saveAndSyncQuest(player);
        } else {
            String msg = !crate ? "按 [F] 搜刮逃生舱旁【残骸物资箱】" : "按 [F] 激活冰裂谷前方的【冰原传送锚点】";
            QuestManager.advanceQuest(15, "【迫降冰裂谷】", msg, !crate ? new BlockPos(110, 61, 25) : new BlockPos(114, 61, 28));
            saveAndSyncQuest(player);
        }
    }

    private static void checkStage18Progress(ServerPlayer player) {
        if (QuestManager.getQuestStage() != 18) return;
        boolean crate = QuestManager.hasFlag(QuestManager.FLAG_CHEST_OUTPOST);
        boolean plus3 = QuestManager.hasFlag(QuestManager.FLAG_CHIP_PLUS3);
        if (crate && plus3) {
            if (QuestManager.hasFlag(QuestManager.FLAG_DOMAIN_CLEARED)) {
                spawnOverworldDrone(player.getLevel(), 141.0, 61.0, 28.0, "配电室重装守卫");
                QuestManager.advanceQuest(20, "【夺回东侧配电室】", "击败【配电室重装守卫】并合闸打开矿坑梯", new BlockPos(141, 61, 28));
            } else {
                QuestManager.advanceQuest(19, "【模拟终端实战演练】", "按 [E] 或访问【模拟终端】通关任意 1 次模拟副本热身", new BlockPos(130, 61, 33));
            }
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                    "芯片升到 +3 解锁新副词条了！顺便按 [E] 去终端体验一把【模拟训练副本】，刷完材料就去东配电房！", 180));
            saveAndSyncQuest(player);
        } else {
            String msg = !crate ? "按 [F] 开启安全屋【科考站物资箱】拿备用芯片" : "按 [B] 溶解芯片拿粉尘，再按 [C] 强化任意芯片到 +3";
            QuestManager.advanceQuest(18, "【装备溶解与强化】", msg, new BlockPos(133, 61, 33));
            saveAndSyncQuest(player);
        }
    }

    private static void checkStage20Progress(ServerPlayer player) {
        if (QuestManager.getQuestStage() != 20) return;
        boolean dead = QuestManager.hasFlag(QuestManager.FLAG_SUBSTATION_GUARD_DEAD);
        boolean core = QuestManager.hasFlag(QuestManager.FLAG_SUBSTATION_CORE_ON);
        if (dead && core) {
            openMineElevatorShaft(player.getLevel());
            spawnOverworldDrone(player.getLevel(), 154.0, 36.0, 28.0, "矿坑轨道巡逻机");
            QuestManager.advanceQuest(21, "【深入冻土矿坑】", "乘货运梯下行矿坑，击败【矿坑轨道巡逻机】并搜寻【矿坑物资箱】", new BlockPos(154, 36, 28));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                    "东配电闸门通电，地下矿坑货运梯已打开！下去拿上中继站的 4★ 芯片再直捣深处通讯塔！", 180));
            saveAndSyncQuest(player);
        } else if (dead && !core) {
            QuestManager.advanceQuest(20, "【夺回东侧配电室】", "按 [F] 启动东配电房内的【配电合闸核心】", new BlockPos(143, 61, 25));
            saveAndSyncQuest(player);
        }
    }

    private static void checkStage21Progress(ServerPlayer player) {
        if (QuestManager.getQuestStage() != 21) return;
        boolean patrolDead = QuestManager.hasFlag(QuestManager.FLAG_MINE_PATROL_DEAD);
        boolean crate = QuestManager.hasFlag(QuestManager.FLAG_CHEST_MINE);
        if (patrolDead && crate) {
            spawnOverworldDrone(player.getLevel(), 162.0, 36.0, 28.0, "矿坑钻探重机");
            QuestManager.advanceQuest(22, "【通讯塔前哨战】", "击败封锁深处高台的【矿坑钻探重机】", new BlockPos(162, 36, 28));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("技术员·老郑",
                    "拿到 4★ 增幅芯片了吧！前方矿坑钻探重机正在挡路，清理它之后就直面通讯塔里的巨型挖掘机！", 170));
            saveAndSyncQuest(player);
        } else if (patrolDead) {
            QuestManager.advanceQuest(21, "【深入冻土矿坑】", "按 [F] 开启矿道北侧的【矿坑工程物资箱】拿取 4★ 芯片", new BlockPos(158, 36, 24));
            saveAndSyncQuest(player);
        } else {
            QuestManager.advanceQuest(21, "【深入冻土矿坑】", "击败矿坑轨道上的【矿坑轨道巡逻机】", new BlockPos(154, 36, 28));
            saveAndSyncQuest(player);
        }
    }

    public static void onCoreRepaired(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        if (QuestManager.getQuestStage() == 13) {
            level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 45, 0.5, 0.5, 0.5, 0.15);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 1.5f);

            QuestManager.advanceQuest(14, "【船体断裂紧急撤离】", "主龙骨断裂！立刻前往西侧搭乘【轨道逃生舱】脱离飞船", new BlockPos(22, 36, 10));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("船体警报",
                    "【紧急】主龙骨结构断裂不可逆！本舱段将在 60 秒后解体，全员立刻撤往西侧【轨道逃生舱】！", 200));
            saveAndSyncQuest(player);
        } else {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.2f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("备用发电机组", "备用供电单元稳定运行中。", 80));
        }
    }

    public static void onSubstationActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 35, 0.4, 0.5, 0.4, 0.12);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.1f, 1.4f);

        QuestManager.setFlag(QuestManager.FLAG_SUBSTATION_CORE_ON);
        if (QuestManager.getQuestStage() == 20) {
            checkStage20Progress(player);
        } else if (QuestManager.getQuestStage() > 20) {
            openMineElevatorShaft(level);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("东侧配电核心", "矿坑货运梯电力维持连通。", 80));
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("东侧配电核心", "配电开关锁定中，请先完成前哨准备工作。", 100));
        }
    }

    public static void onDropPodActivated(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        int stage = QuestManager.getQuestStage();
        if (stage < 14) {
            level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.8f, 0.7f);
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("轨道逃生舱",
                    "逃生舱弹出供电尚未就绪，请先重启机房备用发电机组！", 110));
            return;
        }
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2f, 0.9f);
        player.teleportTo(level, 108.5, 61.0, 28.5, 270.0f, 0.0f);
        level.playSound(null, new BlockPos(108, 61, 28), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.7f, 1.2f);

        if (stage == 14) {
            QuestManager.advanceQuest(15, "【迫降冰裂谷】", "搜索逃生舱旁的【残骸物资箱】并按 [F] 激活【冰原传送锚点】", new BlockPos(110, 61, 25));
            checkStage15Progress(player);
        }
        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("巡检员·陈风",
                "咳咳……总算砸进雪地停稳了！大家快翻翻旁边的残骸物资箱，再把前面那座冰原锚点激活！", 190));
    }

    public static void onPrecursorAltarActivated(ServerPlayer player, BlockPos pos) {
        int stage = QuestManager.getQuestStage();
        if (stage < 23) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("深空通讯塔控制台",
                    "矿坑深处警备未清，请先突破前哨再启动控制台！", 100));
            return;
        }
        TurnBasedCombatManager.startBossBattle(player);
    }

    public static void onBossDefeated(ServerPlayer player) {
        int stage = QuestManager.getQuestStage();
        if (stage <= 23) {
            ensureNpc(player.getLevel(), "zheng", 131.5, 61.0, 31.5, 135.0f);
            QuestManager.advanceQuest(24, "【重连母舰·凯旋报信】", "返回科考站安全屋向【技术员·老郑】按 [F] 报信领取 20 张抽卡券", new BlockPos(131, 61, 31));
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("通讯塔控制台",
                    "大型工程挖掘机已彻底拆解！深空通讯信号满格，快回科考站安全屋找老郑报信领取 20 张抽卡通行券！", 210));
            saveAndSyncQuest(player);
        } else {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("战利品系统",
                    "大型工程挖掘机已再度击破！高阶奖励已存入背包。", 120));
        }
    }
}
