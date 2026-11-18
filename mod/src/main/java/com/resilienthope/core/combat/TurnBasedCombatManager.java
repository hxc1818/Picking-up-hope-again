package com.resilienthope.core.combat;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketBattleSync;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.network.PacketSyncTeamData;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.quest.StoryDirector;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TurnBasedCombatManager {

    public static final int ACTION_BASIC = 0;
    public static final int ACTION_SKILL = 1;
    public static final int ACTION_ULT = 2;
    public static final int ACTION_FLEE = 3;
    public static final int ACTION_EXIT_FINISHED = 4;

    private static class BattleSession {
        BattleState state;
        Vec3 returnPos;
        float returnYaw;
        float returnPitch;
        UUID overworldEnemyUuid;
        boolean rewardsClaimed = false;
    }

    private static final Map<UUID, BattleSession> ACTIVE_SESSIONS = new HashMap<>();

    public static boolean isInBattle(ServerPlayer player) {
        return player != null && ACTIVE_SESSIONS.containsKey(player.getUUID());
    }

    public static void ensureBattleArena(ServerLevel level, boolean iceTheme) {
        int minX = 0, maxX = 28, minZ = 0, maxZ = 24, floorY = 150;
        var floorBase = iceTheme
                ? com.resilienthope.core.init.ModBlocks.PERMAFROST_ROCK.get().defaultBlockState()
                : com.resilienthope.core.init.ModBlocks.STARSHIP_FLOOR.get().defaultBlockState();
        var laneBlock = com.resilienthope.core.init.ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState();
        var wallBlock = iceTheme
                ? com.resilienthope.core.init.ModBlocks.PERMAFROST_ROCK.get().defaultBlockState()
                : com.resilienthope.core.init.ModBlocks.STARSHIP_WALL.get().defaultBlockState();
        var ceilingBlock = com.resilienthope.core.init.ModBlocks.STARSHIP_HULL.get().defaultBlockState();
        var lightBlock = com.resilienthope.core.init.ModBlocks.STARSHIP_LIGHT.get().defaultBlockState();
        var oreOrAlarm = iceTheme
                ? com.resilienthope.core.init.ModBlocks.FROZEN_ORE_VEIN.get().defaultBlockState()
                : com.resilienthope.core.init.ModBlocks.ALARM_BEACON.get().defaultBlockState();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isBorder = (x == minX || x == maxX || z == minZ || z == maxZ);
                boolean isAllyLane = (x >= 5 && x <= 7 && z >= 6 && z <= 18);
                boolean isEnemyLane = (x >= 19 && x <= 22 && z >= 5 && z <= 19);
                var floorBlock = (isAllyLane || isEnemyLane) ? laneBlock : floorBase;
                level.setBlock(new BlockPos(x, floorY, z), floorBlock, 2);

                boolean isCeilingLight = (x % 7 == 0 && z % 6 == 0);
                level.setBlock(new BlockPos(x, floorY + 12, z), isCeilingLight ? lightBlock : ceilingBlock, 2);

                for (int y = floorY + 1; y < floorY + 12; y++) {
                    if (isBorder) {
                        level.setBlock(new BlockPos(x, y, z),
                                (y == floorY + 5) ? lightBlock : ((y == floorY + 2 && (x + z) % 6 == 0) ? oreOrAlarm : wallBlock), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (int y = floorY + 1; y < floorY + 12; y++) {
            level.setBlock(new BlockPos(2, y, 2), com.resilienthope.core.init.ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(26, y, 2), com.resilienthope.core.init.ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(2, y, 22), com.resilienthope.core.init.ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(26, y, 22), com.resilienthope.core.init.ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
        }
    }

    public static void startOverworldBattle(ServerPlayer player, LivingEntity overworldTarget, boolean ambush) {
        if (player == null || isInBattle(player)) return;
        ServerLevel level = player.getLevel();

        String encounterType = resolveOverworldEncounterType(overworldTarget);
        boolean iceTheme = player.getX() > 90.0;
        startBattleInternal(player, level, encounterType, overworldTarget.getUUID(), ambush, iceTheme, true);
    }

    public static void startBossBattle(ServerPlayer player) {
        if (player == null || isInBattle(player)) return;
        startBattleInternal(player, player.getLevel(), "boss_excavator", null, false, true, false);
    }

    public static void startDomainBattle(ServerPlayer player, String domainType) {
        if (player == null || isInBattle(player)) return;

        final boolean[] allowed = {true};
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            if ("world_trial".equals(domainType)) {
                if (!team.canBreakthroughWorldLevel()) {
                    allowed[0] = false;
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("模拟终端",
                            "尚未达到当前世界等级上限（需玩家主等级 Lv." + team.getPlayerLevelCapForWorldLevel() + "），无法开启突破试炼。", 100));
                }
            } else {
                if (team.getStamina() < 20) {
                    allowed[0] = false;
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("模拟终端",
                            "当前体力不足 20 点！请等待自然恢复或继续推进主线。", 90));
                } else {
                    team.setStamina(team.getStamina() - 20);
                    PacketSyncTeamData.syncToPlayer(player);
                }
            }
        });

        if (!allowed[0]) return;
        boolean canFlee = !"world_trial".equals(domainType);
        startBattleInternal(player, player.getLevel(), domainType, null, true, false, canFlee);
    }

    private static String resolveOverworldEncounterType(LivingEntity target) {
        if (target != null) {
            String n = target.getName().getString();
            if (n.contains("挖掘机")) return "boss_excavator";
            if (n.contains("钻探")) return "mine_driller";
            if (n.contains("轨道巡逻")) return "mine_patrol";
            if (n.contains("配电室")) return "substation_guard";
            if (n.contains("统御")) return "bridge_commander";
            if (n.contains("重装守卫") || n.contains("重装机房")) return "lower_guard";
            if (n.contains("机房巡逻")) return "lower_patrol";
            if (n.contains("猎犬") || n.contains("外围防卫")) return "ice_hound";
            if (n.contains("安保巡逻")) return "corridor_patrol";
            if (n.contains("清扫机")) return "corridor_cleaner";
        }
        int stage = QuestManager.getQuestStage();
        if (stage <= 3) return "corridor_cleaner";
        if (stage <= 5) return "corridor_patrol";
        if (stage == 6) return "bridge_commander";
        if (stage <= 10) return "lower_patrol";
        if (stage <= 14) return "lower_guard";
        if (stage <= 18) return "ice_hound";
        if (stage == 20) return "substation_guard";
        if (stage == 21) return "mine_patrol";
        return "mine_driller";
    }

    private static void startBattleInternal(ServerPlayer player, ServerLevel level, String encounterType,
                                            UUID enemyUuid, boolean ambush, boolean iceTheme, boolean canFlee) {
        BattleSession session = new BattleSession();
        session.returnPos = player.position();
        session.returnYaw = player.getYRot();
        session.returnPitch = player.getXRot();
        session.overworldEnemyUuid = enemyUuid;

        ensureBattleArena(level, iceTheme);
        player.teleportTo(level, 6.5, 151.0, 12.5, 270.0f, 8.0f);
        level.playSound(null, 6.5, 151.0, 12.5, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.3f);

        BattleState state = new BattleState();
        state.active = true;
        state.encounterType = encounterType;
        state.canFlee = canFlee;
        state.iceTheme = iceTheme;
        state.skillPoints = 3;

        final int[] worldLv = {0};
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            worldLv[0] = team.getWorldLevel();
            for (CharacterData c : team.getActivePartyMembers()) {
                BattleState.AllyUnit u = new BattleState.AllyUnit();
                u.charId = c.getCharId();
                u.name = c.getName();
                u.element = c.getElement();
                u.role = c.getRole();
                u.maxHp = c.getTotalMaxHp();
                u.hp = Math.max(u.maxHp * 0.55f, c.getCurrentHp());
                u.shield = 0f;
                u.maxEnergy = c.getMaxEnergy();
                u.energy = Math.min(u.maxEnergy, Math.max(35f, c.getCurrentEnergy()));
                u.atk = c.getTotalAtk();
                u.def = c.getTotalDef();
                u.speed = c.getTotalSpeed();
                u.critRate = c.getCritRate();
                u.critDmg = c.getCritDmg();
                u.elemDmgBonus = c.getElemDmgBonus();
                u.talentScale = c.getTalentScale();
                u.actionValue = 10000.0f / Math.max(60, u.speed);
                state.allies.add(u);
            }
        });

        populateEnemies(state, encounterType, worldLv[0], ambush);
        setupTutorialIfApplicable(state, encounterType);

        if (state.tutorialId == 0) {
            state.addLog(ambush ? "先手突袭成功！敌方全体韧性受损，我方抢占先机！" : "遭遇敌人拦截！准备迎战！");
        }

        refreshAllEnemyIntents(state);
        session.state = state;
        ACTIVE_SESSIONS.put(player.getUUID(), session);

        advanceTimelineUntilPlayerTurn(player, session);
        refreshTutorialPrompt(state);
        PacketHandler.sendToPlayer(player, new PacketBattleSync(state.toNbt()));
    }

    private static void setupTutorialIfApplicable(BattleState st, String encounterType) {
        int stage = QuestManager.getQuestStage();

        // Tutorial 1: Lin Ye's Skill, Basic & Ult in Stage 2 (corridor_cleaner)
        if ("corridor_cleaner".equals(encounterType) && stage <= 2) {
            st.tutorialId = 1;
            st.tutorialStep = 1;
            st.skillPoints = 3;
            st.canFlee = false;
            if (!st.allies.isEmpty()) {
                st.allies.get(0).actionValue = 0f;
                st.allies.get(0).energy = 45f;
            }
            for (BattleState.EnemyUnit e : st.enemies) {
                e.maxHp = 280f;
                e.hp = 280f;
                e.maxToughness = 90f;
                e.toughness = 60f;
            }
            st.addLog("【林野教学】请先体验战技与普攻搭配！");
            return;
        }

        // Tutorial 2: Chen Feng's AOE Skill, Follow-Up Passive & Ult on first fight in Stage 5
        if ("corridor_patrol".equals(encounterType) && stage == 5 && QuestManager.getDronesKilled() == 0) {
            int chenIdx = findAllyIndex(st, "chenfeng");
            if (chenIdx >= 0) {
                st.tutorialId = 2;
                st.tutorialStep = 1;
                st.skillPoints = 3;
                st.canFlee = false;
                st.allies.get(chenIdx).actionValue = 0f;
                st.allies.get(chenIdx).energy = 40f;
                if (!st.allies.isEmpty()) {
                    st.allies.get(0).actionValue = 25f;
                }
                if (st.enemies.size() >= 2) {
                    st.enemies.get(0).maxHp = 330f;
                    st.enemies.get(0).hp = 330f;
                    st.enemies.get(0).weaknesses.clear();
                    st.enemies.get(0).weaknesses.addAll(List.of("AIR", "EARTH"));
                    st.enemies.get(0).toughness = 65f;

                    st.enemies.get(1).maxHp = 330f;
                    st.enemies.get(1).hp = 330f;
                    st.enemies.get(1).weaknesses.clear();
                    st.enemies.get(1).weaknesses.addAll(List.of("AIR", "WATER"));
                    st.enemies.get(1).toughness = 65f;
                }
                st.addLog("【陈风教学】陈风抢占先手，准备横扫敌阵！");
                return;
            }
        }

        // Tutorial 3: Su Qing's Team Heal/Cleanse/Buff, Basic Heal & Ult SP Recovery in Stage 12
        if ("lower_guard".equals(encounterType) && stage == 12) {
            int suIdx = findAllyIndex(st, "suqing");
            if (suIdx >= 0) {
                st.tutorialId = 3;
                st.tutorialStep = 1;
                st.skillPoints = 3;
                st.canFlee = false;
                // Opening suppressive volley from the Heavy Guard so healing is meaningful!
                int totalOpeningHit = 0;
                for (BattleState.AllyUnit a : st.allies) {
                    float drop = Math.max(85f, a.maxHp * 0.28f);
                    a.hp = Math.max(a.maxHp * 0.35f, a.hp - drop);
                    totalOpeningHit = Math.round(drop);
                }
                if (!st.allies.isEmpty()) {
                    st.allies.get(0).debuffBurnTurns = 2;
                }
                st.allies.get(suIdx).actionValue = 0f;
                st.allies.get(suIdx).energy = 45f;
                for (int i = 0; i < st.allies.size(); i++) {
                    if (i != suIdx) st.allies.get(i).actionValue = 20f + i * 8f;
                }
                for (BattleState.EnemyUnit e : st.enemies) {
                    e.actionValue = Math.max(e.actionValue, 65f);
                }
                if (st.enemies.size() >= 2) {
                    st.enemies.get(1).maxHp = 680f;
                    st.enemies.get(1).hp = 680f;
                }
                st.addEvent(1, Math.min(1, st.enemies.size() - 1), -1, "BOSS_AOE",
                        "heavy_guard_mech", "重装机房守卫机", "压制震荡炮", "FIRE", totalOpeningHit, false,
                        "敌方开局压制扫射！全队受伤且林野陷入灼烧！");
                st.addLog("【苏晴教学】守卫开场火力压制！轮到苏晴进行紧急救场！");
            }
        }
    }

    private static void refreshTutorialPrompt(BattleState st) {
        if (st.tutorialId == 0 || st.tutorialStep == 0 || st.finished) {
            st.tutorialTitle = "";
            st.tutorialPrompt = "";
            st.tutorialHighlightBtn = -1;
            return;
        }
        BattleState.AllyUnit actor = (st.activeAllyIndex >= 0 && st.activeAllyIndex < st.allies.size())
                ? st.allies.get(st.activeAllyIndex) : null;

        if (st.tutorialId == 1) {
            switch (st.tutorialStep) {
                case 1 -> {
                    st.tutorialTitle = "【第1课 · 林野战技】";
                    st.tutorialPrompt = "林野擅长多目标重击！请按 [E]（或点击战技按钮）施放【震地裂斩】，打出扩散伤害并给敌方挂上2回合【破甲】！";
                    st.tutorialHighlightBtn = 1;
                }
                case 2 -> {
                    st.tutorialTitle = "【第1课 · 普攻攒点回能】";
                    st.tutorialPrompt = "战技消耗战技点！现在按 [Q] 使用【工程重劈】，打伤害的同时回复 1 点战技点并攒满终结技能量！";
                    st.tutorialHighlightBtn = 0;
                }
                case 3 -> {
                    int idx = Math.max(0, findAllyIndex(st, "linye"));
                    st.tutorialTitle = "【第1课 · 终结技插队】";
                    st.tutorialPrompt = "林野能量已满！按数字键 [" + (idx + 1) + "]（或点击左下角林野头像）无需等待回合立刻插队打出【超载崩岩斩】！";
                    st.tutorialHighlightBtn = 10 + idx;
                }
            }
        } else if (st.tutorialId == 2) {
            int chenIdx = Math.max(0, findAllyIndex(st, "chenfeng"));
            switch (st.tutorialStep) {
                case 1 -> {
                    st.tutorialTitle = "【第2课 · 陈风群攻】";
                    st.tutorialPrompt = "现在由新同伴【陈风】主攻！按 [E] 施放【旋风横扫】，同时打击敌方全体削韧，命中2人以上还能额外回能！";
                    st.tutorialHighlightBtn = 1;
                }
                case 2 -> {
                    st.tutorialTitle = "【第2课 · 弱点击破与乘风追击】";
                    st.tutorialPrompt = "敌方【甲】只剩少量韧性且带地弱点！按 [Q] 或 [E] 让林野将其【弱点击破】，看陈风自动追加天赋【乘风追击】！";
                    st.tutorialHighlightBtn = 0;
                }
                case 3 -> {
                    st.tutorialTitle = "【第2课 · 暴风涡流斩】";
                    st.tutorialPrompt = "陈风能量已满！按数字键 [" + (chenIdx + 1) + "]（或点击左下角陈风卡片）插队打出全屏终结技【暴风涡流斩】！";
                    st.tutorialHighlightBtn = 10 + chenIdx;
                }
            }
        } else if (st.tutorialId == 3) {
            int suIdx = Math.max(0, findAllyIndex(st, "suqing"));
            switch (st.tutorialStep) {
                case 1 -> {
                    st.tutorialTitle = "【第3课 · 苏晴群奶与净化】";
                    st.tutorialPrompt = "队友血线告急！按 [E] 让苏晴施放战技【纳米水雾急救】：大幅恢复全队血量、净化灼烧并赋予全队+25%攻击力！";
                    st.tutorialHighlightBtn = 1;
                }
                case 2 -> {
                    if (actor != null && "suqing".equals(actor.charId)) {
                        st.tutorialTitle = "【第3课 · 边打边奶的普攻】";
                        st.tutorialPrompt = "轮到苏晴行动！请按 [Q] 使用【水冷脉冲】：打水属性伤害+回复1点战技点的同时，还能顺手治疗残血队友！";
                        st.tutorialHighlightBtn = 0;
                    } else {
                        String aname = actor != null ? actor.name : "队友";
                        st.tutorialTitle = "【第3课 · 等待苏晴轮次】";
                        st.tutorialPrompt = "当前为 " + aname + " 回合（已享苏晴+25%攻击增益）。请用 [E]/[Q] 攻击重装守卫，马上就轮到苏晴演示普攻！";
                        st.tutorialHighlightBtn = 0;
                    }
                }
                case 3 -> {
                    st.tutorialTitle = "【第3课 · 维生力场+回战技点】";
                    st.tutorialPrompt = "苏晴能量已满！按数字键 [" + (suIdx + 1) + "]（或点击苏晴卡片）插队大招【紧急维生力场】，全队回血套水盾并立刻回复1战技点！";
                    st.tutorialHighlightBtn = 10 + suIdx;
                }
            }
        }
    }

    private static int findAllyIndex(BattleState st, String charId) {
        for (int i = 0; i < st.allies.size(); i++) {
            if (charId.equals(st.allies.get(i).charId)) return i;
        }
        return -1;
    }

    private static void populateEnemies(BattleState state, String encounterType, int worldLv, boolean ambush) {
        int lvScale = 1 + worldLv * 10;
        switch (encounterType) {
            case "corridor_cleaner" -> {
                state.enemies.add(makeEnemy("cleaner_1", "故障清扫机 A", 2, 240f, 90f, 36f, 92, ambush, "EARTH", "AIR"));
                state.enemies.add(makeEnemy("cleaner_2", "故障清扫机 B", 2, 240f, 90f, 36f, 88, ambush, "EARTH", "FIRE"));
            }
            case "corridor_patrol" -> {
                state.enemies.add(makeEnemy("drone_1", "安保巡逻机·甲", 4, 290f, 90f, 48f, 96, ambush, "AIR", "EARTH"));
                state.enemies.add(makeEnemy("drone_2", "安保巡逻机·乙", 4, 290f, 90f, 48f, 94, ambush, "AIR", "WATER"));
            }
            case "bridge_commander" -> {
                state.enemies.add(makeEnemy("b_escort_1", "数据室僚机·左", 5, 240f, 60f, 44f, 98, ambush, "AIR", "EARTH"));
                state.enemies.add(makeEnemy("b_cmd", "舰桥防卫统御机", 6, 480f, 120f, 64f, 94, ambush, "EARTH", "WATER"));
                state.enemies.add(makeEnemy("b_escort_2", "数据室僚机·右", 5, 240f, 60f, 44f, 96, ambush, "AIR", "FIRE"));
            }
            case "lower_patrol" -> {
                state.enemies.add(makeEnemy("lp_1", "机房巡逻队长", 7, 380f, 90f, 58f, 98, ambush, "EARTH", "AIR"));
                state.enemies.add(makeEnemy("lp_2", "机房维修机", 6, 280f, 60f, 46f, 92, ambush, "WATER", "FIRE"));
            }
            case "lower_guard" -> {
                state.enemies.add(makeEnemy("guard_1", "机房巡逻机·左", 8, 340f, 90f, 58f, 98, ambush, "WATER", "AIR"));
                state.enemies.add(makeEnemy("guard_2", "重装机房守卫机", 9, 580f, 120f, 74f, 92, ambush, "EARTH", "WATER"));
                state.enemies.add(makeEnemy("guard_3", "机房巡逻机·右", 8, 340f, 90f, 58f, 95, ambush, "AIR", "FIRE"));
            }
            case "ice_hound" -> {
                state.enemies.add(makeEnemy("hound_1", "旧式机械猎犬·左", 10, 420f, 90f, 76f, 108, ambush, "FIRE", "EARTH"));
                state.enemies.add(makeEnemy("hound_2", "科考防卫机", 11, 580f, 120f, 85f, 98, ambush, "WATER", "AIR"));
                state.enemies.add(makeEnemy("hound_3", "旧式机械猎犬·右", 10, 420f, 90f, 76f, 105, ambush, "FIRE", "WATER"));
            }
            case "substation_guard" -> {
                state.enemies.add(makeEnemy("sub_1", "配电室哨戒机", 12, 460f, 90f, 80f, 102, ambush, "AIR", "WATER"));
                state.enemies.add(makeEnemy("sub_2", "配电室重装守卫", 13, 680f, 120f, 92f, 94, ambush, "EARTH", "FIRE"));
                state.enemies.add(makeEnemy("sub_3", "配电室哨戒机", 12, 460f, 90f, 80f, 100, ambush, "WATER", "EARTH"));
            }
            case "mine_patrol" -> {
                state.enemies.add(makeEnemy("mp_1", "矿坑轨道巡逻机·甲", 14, 520f, 120f, 88f, 104, ambush, "EARTH", "WATER"));
                state.enemies.add(makeEnemy("mp_2", "矿坑轨道巡逻机·乙", 14, 520f, 120f, 88f, 102, ambush, "AIR", "FIRE"));
            }
            case "mine_driller" -> {
                state.enemies.add(makeEnemy("md_1", "岩脉开采辅机", 15, 480f, 90f, 84f, 104, ambush, "WATER", "AIR"));
                state.enemies.add(makeEnemy("md_core", "矿坑钻探重机", 16, 860f, 150f, 102f, 96, ambush, "EARTH", "FIRE"));
            }
            case "boss_excavator" -> {
                BattleState.EnemyUnit leftArm = makeEnemy("boss_left", "左挖掘臂", 15, 480f, 90f, 78f, 98, false, "EARTH", "WATER");
                leftArm.isBossArm = true;
                BattleState.EnemyUnit core = makeEnemy("boss_core", "大型工程挖掘机·本体", 16, 1150f, 150f, 96f, 92, false, "EARTH", "AIR");
                core.isBossCore = true;
                core.shieldProtected = true;
                BattleState.EnemyUnit rightArm = makeEnemy("boss_right", "右重锤臂", 15, 480f, 90f, 78f, 95, false, "AIR", "FIRE");
                rightArm.isBossArm = true;
                state.enemies.add(leftArm);
                state.enemies.add(core);
                state.enemies.add(rightArm);
            }
            case "world_trial" -> {
                int lv = 18 + worldLv * 10;
                float mult = 1.0f + worldLv * 0.65f;
                state.enemies.add(makeEnemy("trial_1", "试炼防卫机·甲", lv, 620f * mult, 120f, 95f * mult, 104, false, "EARTH", "WATER"));
                state.enemies.add(makeEnemy("trial_2", "试炼重装机", lv + 2, 1050f * mult, 150f, 120f * mult, 98, false, "AIR", "FIRE"));
            }
            default -> {
                int lv = Math.max(5, lvScale + 4);
                float mult = 1.0f + worldLv * 0.6f;
                state.enemies.add(makeEnemy("dom_1", "模拟巡逻机·1型", lv, 380f * mult, 90f, 65f * mult, 96, ambush, "WATER", "EARTH"));
                state.enemies.add(makeEnemy("dom_2", "模拟重装机·核心", lv + 1, 560f * mult, 120f, 82f * mult, 92, ambush, "AIR", "FIRE"));
                state.enemies.add(makeEnemy("dom_3", "模拟巡逻机·2型", lv, 380f * mult, 90f, 65f * mult, 94, ambush, "EARTH", "FIRE"));
            }
        }
    }

    private static BattleState.EnemyUnit makeEnemy(String id, String name, int level, float maxHp, float maxTough,
                                                   float atk, int speed, boolean ambush, String... weaknesses) {
        BattleState.EnemyUnit e = new BattleState.EnemyUnit();
        e.id = id;
        e.name = name;
        e.level = level;
        e.maxHp = maxHp;
        e.hp = maxHp;
        e.maxToughness = maxTough;
        e.toughness = ambush ? Math.max(10f, maxTough - 30f) : maxTough;
        e.atk = atk;
        e.def = 80f + level * 12f;
        e.speed = speed;
        e.weaknesses.addAll(Arrays.asList(weaknesses));
        e.actionValue = (10000.0f / Math.max(50, speed)) * (ambush ? 1.45f : 0.75f);
        return e;
    }

    private static void refreshAllEnemyIntents(BattleState st) {
        for (BattleState.EnemyUnit e : st.enemies) {
            if (!e.isAlive()) {
                e.intentText = "已击毁";
            } else if (e.broken) {
                e.intentText = "💫 破盾硬直中";
            } else if (e.isBossCore && e.chargingUlt) {
                e.intentText = "🔥 蓄力(" + e.chargeCountdown + "回合)";
            } else if (e.shieldProtected) {
                e.intentText = "🛡 双臂减伤中";
            } else if (e.id.contains("guard") || e.id.contains("cmd") || e.id.contains("driller") || e.id.contains("boss")) {
                boolean nextAoe = ((st.turnCount + st.enemies.indexOf(e)) % 2 == 1);
                e.intentText = nextAoe ? "💥 全体震荡炮" : "⚔ 重锤压击";
            } else if (e.id.contains("hound")) {
                e.intentText = "⚡ 突袭撕咬";
            } else {
                e.intentText = "🔫 脉冲射击";
            }
        }
    }

    public static void handleBattleAction(ServerPlayer player, int actionType, int targetEnemyIndex, int allySlotIndex) {
        if (player == null) return;
        BattleSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null) return;

        BattleState st = session.state;
        if (actionType == ACTION_EXIT_FINISHED) {
            exitBattleArena(player, session);
            return;
        }

        // Reset animation queue for this new turn response
        st.events.clear();

        if (st.finished) {
            PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
            return;
        }
        if (actionType == ACTION_FLEE) {
            if (!st.canFlee) {
                st.addLog("当前关键战斗无法撤退！");
                PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
                return;
            }
            st.addLog("小队撤离了战斗！");
            exitBattleArena(player, session);
            return;
        }

        if (actionType == ACTION_ULT) {
            if (allySlotIndex >= 0 && allySlotIndex < st.allies.size()) {
                BattleState.AllyUnit actor = st.allies.get(allySlotIndex);
                if (actor.isAlive() && actor.energy >= actor.maxEnergy) {
                    executeAllyUltimate(player, st, allySlotIndex, actor, targetEnemyIndex);
                    advanceTutorialOnAction(st, actionType, allySlotIndex);
                    checkBattleEndOrPhaseTransition(player, session);
                    if (!st.finished && st.activeAllyIndex < 0) {
                        advanceTimelineUntilPlayerTurn(player, session);
                    }
                }
            }
            refreshAllEnemyIntents(st);
            refreshTutorialPrompt(st);
            PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
            return;
        }

        // Standard Turn Action (Basic Q or Skill E)
        if (st.activeAllyIndex < 0 || st.activeAllyIndex >= st.allies.size()) {
            advanceTimelineUntilPlayerTurn(player, session);
            refreshAllEnemyIntents(st);
            refreshTutorialPrompt(st);
            PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
            return;
        }

        int actorSlot = st.activeAllyIndex;
        BattleState.AllyUnit actor = st.allies.get(actorSlot);
        if (actionType == ACTION_SKILL && st.skillPoints <= 0) {
            st.addLog("战技点不足！请使用 [Q] 普通攻击回复战技点。");
            PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
            return;
        }

        boolean avBoostFromBasic = false;
        if (actionType == ACTION_BASIC) {
            executeAllyBasic(player, st, actorSlot, actor, targetEnemyIndex);
            if ("chenfeng".equals(actor.charId)) avBoostFromBasic = true;
        } else if (actionType == ACTION_SKILL) {
            executeAllySkill(player, st, actorSlot, actor, targetEnemyIndex);
        }

        // Tick actor buffs & reset AV
        if (actor.buffAtkTurns > 0) actor.buffAtkTurns--;
        if (actor.buffSpdTurns > 0) actor.buffSpdTurns--;
        if (actor.debuffBurnTurns > 0) {
            actor.debuffBurnTurns--;
            applyDamageToAlly(actor, 30.0f);
        }
        float baseAv = 10000.0f / Math.max(60, actor.getEffectiveSpeed());
        actor.actionValue = avBoostFromBasic ? baseAv * 0.85f : baseAv;
        st.activeAllyIndex = -1;

        advanceTutorialOnAction(st, actionType, actorSlot);
        checkBattleEndOrPhaseTransition(player, session);
        if (!st.finished) {
            advanceTimelineUntilPlayerTurn(player, session);
        }
        refreshAllEnemyIntents(st);
        refreshTutorialPrompt(st);
        PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
    }

    private static void advanceTutorialOnAction(BattleState st, int actionType, int slotIdx) {
        if (st.tutorialId == 0 || st.tutorialStep == 0) return;
        BattleState.AllyUnit actor = (slotIdx >= 0 && slotIdx < st.allies.size()) ? st.allies.get(slotIdx) : null;

        if (st.tutorialId == 1) {
            if (st.tutorialStep == 1 && actionType == ACTION_SKILL) {
                st.tutorialStep = 2;
            } else if (st.tutorialStep == 2 && actionType == ACTION_BASIC) {
                st.tutorialStep = 3;
                if (!st.allies.isEmpty()) st.allies.get(0).energy = st.allies.get(0).maxEnergy;
            } else if (st.tutorialStep == 3 && actionType == ACTION_ULT) {
                st.tutorialStep = 0;
                st.tutorialId = 0;
                return;
            }
            // Prevent accidental premature enemy wipe before Step 3 showcase
            if (st.tutorialStep > 0 && st.tutorialStep <= 3) {
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.hp <= 0f) e.hp = 45f;
                }
            }
        } else if (st.tutorialId == 2) {
            int chenIdx = Math.max(0, findAllyIndex(st, "chenfeng"));
            if (st.tutorialStep == 1 && actionType == ACTION_SKILL) {
                st.tutorialStep = 2;
                // Ensure either living drone is breakable so Lin Ye triggers Chen Feng's Follow-Up
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.isAlive()) {
                        e.toughness = Math.min(e.toughness, 25f);
                        if (!e.weaknesses.contains("EARTH")) e.weaknesses.add("EARTH");
                    }
                }
            } else if (st.tutorialStep == 2 && (actionType == ACTION_BASIC || actionType == ACTION_SKILL)) {
                st.tutorialStep = 3;
                st.allies.get(chenIdx).energy = st.allies.get(chenIdx).maxEnergy;
            } else if (st.tutorialStep == 3 && actionType == ACTION_ULT) {
                st.tutorialStep = 0;
                st.tutorialId = 0;
                return;
            }
            if (st.tutorialStep > 0 && st.tutorialStep <= 3) {
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.hp <= 0f) e.hp = 60f;
                }
            }
        } else if (st.tutorialId == 3) {
            int suIdx = Math.max(0, findAllyIndex(st, "suqing"));
            if (st.tutorialStep == 1 && actionType == ACTION_SKILL && actor != null && "suqing".equals(actor.charId)) {
                st.tutorialStep = 2;
                st.skillPoints = Math.max(st.skillPoints, 2);
                st.allies.get(suIdx).actionValue = Math.min(st.allies.get(suIdx).actionValue, 12f);
            } else if (st.tutorialStep == 2 && actionType == ACTION_BASIC && actor != null && "suqing".equals(actor.charId)) {
                st.tutorialStep = 3;
                st.allies.get(suIdx).energy = st.allies.get(suIdx).maxEnergy;
            } else if (st.tutorialStep == 3 && actionType == ACTION_ULT && actor != null && "suqing".equals(actor.charId)) {
                st.tutorialStep = 0;
                st.tutorialId = 0;
                return;
            }
            if (st.tutorialStep > 0 && st.tutorialStep <= 3) {
                st.skillPoints = Math.max(st.skillPoints, 1);
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.id.contains("guard_2") && e.hp <= 0f) e.hp = 120f;
                }
            }
        }
    }

    private static BattleState.EnemyUnit pickLivingEnemy(BattleState st, int preferredIdx) {
        if (preferredIdx >= 0 && preferredIdx < st.enemies.size() && st.enemies.get(preferredIdx).isAlive()) {
            return st.enemies.get(preferredIdx);
        }
        for (BattleState.EnemyUnit e : st.enemies) {
            if (e.isAlive()) return e;
        }
        return null;
    }

    private static void executeAllyBasic(ServerPlayer player, BattleState st, int actorSlot,
                                         BattleState.AllyUnit actor, int targetIdx) {
        BattleState.EnemyUnit target = pickLivingEnemy(st, targetIdx);
        if (target == null) return;
        int actualTargetIdx = st.enemies.indexOf(target);

        st.skillPoints = Math.min(st.maxSkillPoints, st.skillPoints + 1);
        actor.energy = Math.min(actor.maxEnergy, actor.energy + 22f);

        float toughCost = "suqing".equals(actor.charId) ? 35.0f : 30.0f;
        // Tutorial 2 Step 2 helper: guarantee Lin Ye breaks Drone 1 smoothly to trigger Chen Feng's Passive!
        if (st.tutorialId == 2 && st.tutorialStep == 2 && !target.broken && target.toughness <= 35f) {
            toughCost = Math.max(toughCost, target.toughness + 5f);
            if (!target.weaknesses.contains(actor.element)) {
                target.weaknesses.add(actor.element);
            }
        }

        String skillName = CharacterData.getBasicNameFor(actor.charId);
        HitResult hit = dealDamageToEnemy(player, st, actorSlot, actor, target, 1.0f, toughCost,
                "BASIC", skillName);

        // Su Qing passive: heal lowest ally slightly on basic attack
        if ("suqing".equals(actor.charId)) {
            BattleState.AllyUnit lowest = st.allies.stream()
                    .filter(BattleState.AllyUnit::isAlive)
                    .min(Comparator.comparingDouble(a -> a.hp / Math.max(1f, a.maxHp)))
                    .orElse(actor);
            float heal = (80f + actor.maxHp * 0.12f) * actor.talentScale;
            lowest.hp = Math.min(lowest.maxHp, lowest.hp + heal);
            st.addEvent(0, actorSlot, st.allies.indexOf(lowest), "HEAL", actor.charId, actor.name,
                    "应急协疗", actor.element, Math.round(heal), false,
                    actor.name + "【水冷脉冲】造成 " + hit.damage + " 伤并为 " + lowest.name + " 恢复 +" + Math.round(heal));
            st.addLog(actor.name + " 使用【水冷脉冲】命中 " + target.name + "（-" + hit.damage + "），顺带治疗 " + lowest.name + " +" + Math.round(heal));
        } else {
            st.addEvent(0, actorSlot, actualTargetIdx, "BASIC", actor.charId, actor.name, skillName,
                    actor.element, hit.damage, hit.crit,
                    actor.name + " 施放【" + skillName + "】命中 " + target.name + "（-" + hit.damage + "，战技点+1）");
            st.addLog(actor.name + "【" + skillName + "】命中 " + target.name + "，造成 " + hit.damage
                    + (hit.crit ? "(暴击)" : "") + " 伤，战技点+1");
        }
    }

    private static void executeAllySkill(ServerPlayer player, BattleState st, int actorSlot,
                                         BattleState.AllyUnit actor, int targetIdx) {
        st.skillPoints = Math.max(0, st.skillPoints - 1);
        actor.energy = Math.min(actor.maxEnergy, actor.energy + 35f);
        String skillName = CharacterData.getSkillNameFor(actor.charId);

        switch (actor.role) {
            case CharacterData.ROLE_HEALER -> {
                BattleState.AllyUnit lowest = st.allies.stream()
                        .filter(BattleState.AllyUnit::isAlive)
                        .min(Comparator.comparingDouble(a -> a.hp / Math.max(1f, a.maxHp)))
                        .orElse(actor);
                float healAmt = (210f + actor.maxHp * 0.28f) * actor.talentScale;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        float amt = (a == lowest) ? healAmt : (healAmt * 0.48f);
                        a.hp = Math.min(a.maxHp, a.hp + amt);
                        a.debuffBurnTurns = 0;
                        a.buffAtkTurns = Math.max(a.buffAtkTurns, 2);
                    }
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.3f);
                st.addEvent(0, actorSlot, -1, "HEAL", actor.charId, actor.name, skillName,
                        actor.element, Math.round(healAmt), false,
                        actor.name + " 施放【" + skillName + "】群疗净化并赋予全队加攻！");
                st.addLog(actor.name + "【" + skillName + "】为 " + lowest.name + " 回血 +" + Math.round(healAmt) + "，净化异常且全队加攻！");
            }
            case CharacterData.ROLE_SHIELDER -> {
                float shieldAmt = (170f + actor.def * 1.45f) * actor.talentScale;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) a.shield = Math.max(a.shield, shieldAmt);
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.1f);
                st.addEvent(0, actorSlot, -1, "SHIELD", actor.charId, actor.name, skillName,
                        actor.element, Math.round(shieldAmt), false,
                        actor.name + " 施放【" + skillName + "】全队护盾 +" + Math.round(shieldAmt));
                st.addLog(actor.name + " 展开【" + skillName + "】，全队获得 " + Math.round(shieldAmt) + " 点护盾！");
            }
            case CharacterData.ROLE_BUFFER -> {
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        a.buffAtkTurns = 2;
                        a.buffSpdTurns = 2;
                    }
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9f, 1.4f);
                st.addEvent(0, actorSlot, -1, "BUFF", actor.charId, actor.name, skillName,
                        actor.element, 25, false,
                        actor.name + " 施放【" + skillName + "】全队攻击+25%、速度+15！");
                st.addLog(actor.name + "【" + skillName + "】全队攻击力 +25%、速度 +15（持续 2 回合）！");
            }
            case CharacterData.ROLE_SINGLE -> {
                BattleState.EnemyUnit target = pickLivingEnemy(st, targetIdx);
                if (target != null) {
                    int tidx = st.enemies.indexOf(target);
                    HitResult hit = dealDamageToEnemy(player, st, actorSlot, actor, target, 2.3f, 75.0f, "SKILL", skillName);
                    st.addEvent(0, actorSlot, tidx, "SKILL", actor.charId, actor.name, skillName,
                            actor.element, hit.damage, hit.crit,
                            actor.name + " 施放【" + skillName + "】命中 " + target.name + " (-" + hit.damage + ")");
                    st.addLog(actor.name + "【" + skillName + "】点杀 " + target.name + " 造成 " + hit.damage + (hit.crit ? "(暴击)" : "") + " 伤！");
                }
            }
            case CharacterData.ROLE_AOE -> {
                int totalDmg = 0;
                int hitCount = 0;
                boolean anyCrit = false;
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.isAlive()) {
                        HitResult hr = dealDamageToEnemy(player, st, actorSlot, actor, e, 1.25f, 45.0f, "SKILL", skillName);
                        totalDmg += hr.damage;
                        if (hr.crit) anyCrit = true;
                        hitCount++;
                    }
                }
                if (hitCount >= 2) {
                    actor.energy = Math.min(actor.maxEnergy, actor.energy + 15f);
                }
                st.addEvent(0, actorSlot, -1, "SKILL", actor.charId, actor.name, skillName,
                        actor.element, totalDmg, anyCrit,
                        actor.name + " 施放【" + skillName + "】横扫敌阵 累计 -" + totalDmg);
                st.addLog(actor.name + "【" + skillName + "】对敌方全体造成累计 " + totalDmg + " 伤"
                        + (hitCount >= 2 ? "（命中多人额外+15能量）！" : "！"));
            }
            default -> { // BALANCED (Lin Ye: 震地裂斩 + 破甲)
                BattleState.EnemyUnit main = pickLivingEnemy(st, targetIdx);
                if (main != null) {
                    int actualIdx = st.enemies.indexOf(main);
                    HitResult hit = dealDamageToEnemy(player, st, actorSlot, actor, main, 1.60f, 60.0f, "SKILL", skillName);
                    main.debuffDefTurns = Math.max(main.debuffDefTurns, 2);

                    int splashDmg = 0;
                    if (actualIdx - 1 >= 0 && st.enemies.get(actualIdx - 1).isAlive()) {
                        BattleState.EnemyUnit left = st.enemies.get(actualIdx - 1);
                        splashDmg += dealDamageToEnemy(player, st, actorSlot, actor, left, 0.85f, 30.0f, "SKILL", skillName).damage;
                        left.debuffDefTurns = Math.max(left.debuffDefTurns, 2);
                    }
                    if (actualIdx + 1 < st.enemies.size() && st.enemies.get(actualIdx + 1).isAlive()) {
                        BattleState.EnemyUnit right = st.enemies.get(actualIdx + 1);
                        splashDmg += dealDamageToEnemy(player, st, actorSlot, actor, right, 0.85f, 30.0f, "SKILL", skillName).damage;
                        right.debuffDefTurns = Math.max(right.debuffDefTurns, 2);
                    }
                    st.addEvent(0, actorSlot, actualIdx, "SKILL", actor.charId, actor.name, skillName,
                            actor.element, hit.damage + splashDmg, hit.crit,
                            actor.name + " 施放【" + skillName + "】挂上破甲 造成 -" + (hit.damage + splashDmg));
                    st.addLog(actor.name + "【" + skillName + "】重击 " + main.name + " -" + hit.damage
                            + (splashDmg > 0 ? "（溅射 -" + splashDmg + "）" : "") + "，附加【破甲】2回合！");
                }
            }
        }
    }

    private static void executeAllyUltimate(ServerPlayer player, BattleState st, int actorSlot,
                                            BattleState.AllyUnit actor, int targetIdx) {
        actor.energy = 0f;
        String ultName = CharacterData.getUltNameFor(actor.charId);
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.85f, 1.35f);

        switch (actor.role) {
            case CharacterData.ROLE_HEALER -> {
                float healAmt = (280f + actor.maxHp * 0.36f) * actor.talentScale;
                float waterShield = 140f * actor.talentScale;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        a.hp = Math.min(a.maxHp, a.hp + healAmt);
                        a.shield = Math.max(a.shield, waterShield);
                        a.debuffBurnTurns = 0;
                    }
                }
                st.skillPoints = Math.min(st.maxSkillPoints, st.skillPoints + 1);
                st.addEvent(0, actorSlot, -1, "ULT", actor.charId, actor.name, ultName,
                        actor.element, Math.round(healAmt), true,
                        "★ 终结技【" + ultName + "】全队回血+" + Math.round(healAmt) + " 套盾且战技点+1！");
                st.addLog("【终结技插队】" + actor.name + " 展开【" + ultName + "】！全队回血 +" + Math.round(healAmt) + "、套盾 +140 并回复 1 战技点！");
            }
            case CharacterData.ROLE_SHIELDER -> {
                float shieldAmt = (270f + actor.def * 2.0f) * actor.talentScale;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) a.shield = Math.max(a.shield, shieldAmt);
                }
                int dmg = 0;
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.isAlive()) dmg += dealDamageToEnemy(player, st, actorSlot, actor, e, 1.35f, 50.0f, "ULT", ultName).damage;
                }
                st.addEvent(0, actorSlot, -1, "ULT", actor.charId, actor.name, ultName,
                        actor.element, Math.round(shieldAmt), true,
                        "★ 终结技【" + ultName + "】重盾 +" + Math.round(shieldAmt) + " 震伤敌方 -" + dmg);
                st.addLog("【终结技插队】" + actor.name + "【" + ultName + "】全队重盾 " + Math.round(shieldAmt) + "，震伤敌方 " + dmg + "！");
            }
            case CharacterData.ROLE_BUFFER -> {
                st.skillPoints = Math.min(st.maxSkillPoints, st.skillPoints + 2);
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        a.buffAtkTurns = 3;
                        a.buffSpdTurns = 3;
                    }
                }
                st.addEvent(0, actorSlot, -1, "ULT", actor.charId, actor.name, ultName,
                        actor.element, 2, true,
                        "★ 终结技【" + ultName + "】战技点+2 全队强化3回合！");
                st.addLog("【终结技插队】" + actor.name + "【" + ultName + "】立刻恢复 2 点战技点并强化全队 3 回合！");
            }
            case CharacterData.ROLE_SINGLE -> {
                BattleState.EnemyUnit target = pickLivingEnemy(st, targetIdx);
                if (target != null) {
                    int tidx = st.enemies.indexOf(target);
                    float mult = target.broken ? 4.15f : 3.60f;
                    HitResult hit = dealDamageToEnemy(player, st, actorSlot, actor, target, mult, 110.0f, "ULT", ultName);
                    st.addEvent(0, actorSlot, tidx, "ULT", actor.charId, actor.name, ultName,
                            actor.element, hit.damage, true,
                            "★ 终结技【" + ultName + "】穿透 " + target.name + " -" + hit.damage + "！");
                    st.addLog("【终结技插队】" + actor.name + "【" + ultName + "】穿透命中 " + target.name + "，打出 " + hit.damage + " 巨额伤害！");
                }
            }
            case CharacterData.ROLE_AOE -> {
                int dmg = 0;
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.isAlive()) {
                        dmg += dealDamageToEnemy(player, st, actorSlot, actor, e, 2.05f, 70.0f, "ULT", ultName).damage;
                        if (e.isAlive() && e.statusTurns == 0) {
                            e.statusEffect = "AIR_BLEED";
                            e.statusTurns = 2;
                        }
                    }
                }
                st.addEvent(0, actorSlot, -1, "ULT", actor.charId, actor.name, ultName,
                        actor.element, dmg, true,
                        "★ 终结技【" + ultName + "】全场风暴累计 -" + dmg + "！");
                st.addLog("【终结技插队】" + actor.name + "【" + ultName + "】全屏风暴打击，共造成 " + dmg + " 伤并附带撕裂！");
            }
            default -> { // BALANCED (Lin Ye: 超载崩岩斩 + 余震)
                BattleState.EnemyUnit main = pickLivingEnemy(st, targetIdx);
                if (main != null) {
                    int actualIdx = st.enemies.indexOf(main);
                    boolean bonusAftershock = main.broken || main.debuffDefTurns > 0;
                    HitResult hit = dealDamageToEnemy(player, st, actorSlot, actor, main, 2.60f, 90.0f, "ULT", ultName);
                    int splash = 0;
                    if (actualIdx - 1 >= 0 && st.enemies.get(actualIdx - 1).isAlive()) {
                        splash += dealDamageToEnemy(player, st, actorSlot, actor, st.enemies.get(actualIdx - 1), 1.45f, 55.0f, "ULT", ultName).damage;
                    }
                    if (actualIdx + 1 < st.enemies.size() && st.enemies.get(actualIdx + 1).isAlive()) {
                        splash += dealDamageToEnemy(player, st, actorSlot, actor, st.enemies.get(actualIdx + 1), 1.45f, 55.0f, "ULT", ultName).damage;
                    }
                    int total = hit.damage + splash;
                    if (bonusAftershock) {
                        for (BattleState.EnemyUnit e : st.enemies) {
                            if (e.isAlive()) {
                                total += dealDamageToEnemy(player, st, actorSlot, actor, e, 0.80f, 20.0f, "ULT", ultName).damage;
                            }
                        }
                    }
                    st.addEvent(0, actorSlot, actualIdx, "ULT", actor.charId, actor.name, ultName,
                            actor.element, total, true,
                            "★ 终结技【" + ultName + "】" + (bonusAftershock ? "+崩岩余震 " : "") + "总计 -" + total + "！");
                    st.addLog("【终结技插队】" + actor.name + "【" + ultName + "】打出 " + hit.damage
                            + (bonusAftershock ? "（触发全屏崩岩余震，合计 " + total + " 伤！）" : " 伤！"));
                }
            }
        }
    }

    private record HitResult(int damage, boolean crit) {}

    private static HitResult dealDamageToEnemy(ServerPlayer player, BattleState st, int actorSlot,
                                               BattleState.AllyUnit actor, BattleState.EnemyUnit enemy,
                                               float mult, float toughnessCost,
                                               String sourceAnimType, String skillName) {
        Random rng = player.getRandom();
        float atk = actor.getEffectiveAtk() * actor.talentScale;
        float bonus = 1.0f + actor.elemDmgBonus / 100.0f;
        boolean crit = rng.nextFloat() * 100.0f < actor.critRate;
        float critMult = crit ? (1.0f + actor.critDmg / 100.0f) : 1.0f;

        float defShred = ("EARTH_SHRED".equals(enemy.statusEffect) || enemy.debuffDefTurns > 0) ? 0.72f : 1.0f;
        float defMult = 240.0f / (240.0f + enemy.def * defShred);
        float shieldMult = enemy.shieldProtected ? 0.20f : (enemy.broken ? 1.15f : 0.90f);

        int dmg = Math.max(5, Math.round(atk * mult * bonus * critMult * defMult * shieldMult));
        enemy.hp = Math.max(0f, enemy.hp - dmg);

        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, crit ? 1.3f : 1.0f);

        if (!enemy.broken && enemy.hasWeakness(actor.element)) {
            enemy.toughness = Math.max(0f, enemy.toughness - toughnessCost);
            if (enemy.toughness <= 0f) {
                triggerWeaknessBreak(player, st, actorSlot, actor, enemy, sourceAnimType);
            }
        }
        return new HitResult(dmg, crit);
    }

    private static void triggerWeaknessBreak(ServerPlayer player, BattleState st, int breakerSlot,
                                             BattleState.AllyUnit actor, BattleState.EnemyUnit enemy,
                                             String sourceAnimType) {
        enemy.broken = true;
        enemy.actionValue += 38.0f;
        int breakDmg = Math.round(80f + actor.atk * 0.65f);
        enemy.hp = Math.max(0f, enemy.hp - breakDmg);

        enemy.statusTurns = 2;
        switch (actor.element) {
            case CharacterData.ELEM_WATER -> {
                enemy.statusEffect = "WATER_SLOW";
                enemy.actionValue += 25.0f;
            }
            case CharacterData.ELEM_EARTH -> enemy.statusEffect = "EARTH_SHRED";
            case CharacterData.ELEM_AIR -> enemy.statusEffect = "AIR_BLEED";
            default -> enemy.statusEffect = "FIRE_BURN";
        }

        int eIdx = st.enemies.indexOf(enemy);
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.1f, 0.9f);
        st.addEvent(0, breakerSlot, eIdx, "BREAK", actor.charId, actor.name, "弱点击破",
                actor.element, breakDmg, true,
                "💥 弱点击破！" + enemy.name + " 陷入【" + BattleState.getStatusDisplay(enemy.statusEffect) + "】 -" + breakDmg);
        st.addLog("【弱点击破！】" + actor.name + " 击破了 " + enemy.name + "，造成 " + breakDmg + " 破盾伤！");

        // Boss Phase 2 Ultimate Interrupt check
        if (enemy.isBossCore && enemy.chargingUlt) {
            enemy.chargingUlt = false;
            enemy.chargeCountdown = 0;
            int overloadDmg = Math.round(enemy.maxHp * 0.25f);
            enemy.hp = Math.max(0f, enemy.hp - overloadDmg);
            enemy.actionValue += 50.0f;
            st.addLog("【蓄力打断！】挖掘机炉心过载反噬，自损 " + overloadDmg + " 生命并陷入大硬直！");
        }

        // Chen Feng Passive Talent: 【乘风追击】 upon any ally triggering Weakness Break (no infinite self-recursion)
        if (!"FOLLOWUP".equals(sourceAnimType)) {
            int chenIdx = findAllyIndex(st, "chenfeng");
            if (chenIdx >= 0) {
                BattleState.AllyUnit chen = st.allies.get(chenIdx);
                if (chen.isAlive()) {
                    chen.energy = Math.min(chen.maxEnergy, chen.energy + 12f);
                    int fDmg = 0;
                    for (BattleState.EnemyUnit e : st.enemies) {
                        if (e.isAlive()) {
                            fDmg += dealDamageToEnemy(player, st, chenIdx, chen, e, 0.70f, 20.0f, "FOLLOWUP", "乘风追击").damage;
                        }
                    }
                    st.addEvent(0, chenIdx, -1, "FOLLOWUP", chen.charId, chen.name, "乘风追击",
                            chen.element, fDmg, false,
                            "⚡ 陈风天赋触发【乘风追击】！横扫全敌 -" + fDmg);
                    st.addLog("【天赋追击】陈风抓住破盾空档发动【乘风追击】，对敌全体追加 " + fDmg + " 点气伤！");
                }
            }
        }
    }

    private static void applyDamageToAlly(BattleState.AllyUnit ally, float rawDmg) {
        if (!ally.isAlive()) return;
        float rem = rawDmg;
        if (ally.shield > 0f) {
            float absorbed = Math.min(ally.shield, rem);
            ally.shield -= absorbed;
            rem -= absorbed;
        }
        if (rem > 0f) {
            ally.hp = Math.max(0f, ally.hp - rem);
        }
    }

    private static void advanceTimelineUntilPlayerTurn(ServerPlayer player, BattleSession session) {
        BattleState st = session.state;
        for (int guard = 0; guard < 30; guard++) {
            if (st.finished) return;

            float minAv = Float.MAX_VALUE;
            BattleState.AllyUnit nextAlly = null;
            int nextAllyIdx = -1;
            BattleState.EnemyUnit nextEnemy = null;

            for (int i = 0; i < st.allies.size(); i++) {
                BattleState.AllyUnit a = st.allies.get(i);
                if (a.isAlive() && a.actionValue < minAv) {
                    minAv = a.actionValue;
                    nextAlly = a;
                    nextAllyIdx = i;
                    nextEnemy = null;
                }
            }
            for (BattleState.EnemyUnit e : st.enemies) {
                if (e.isAlive() && e.actionValue < minAv) {
                    minAv = e.actionValue;
                    nextEnemy = e;
                    nextAlly = null;
                }
            }

            if (nextAlly == null && nextEnemy == null) return;

            for (BattleState.AllyUnit a : st.allies) {
                if (a.isAlive()) a.actionValue = Math.max(0f, a.actionValue - minAv);
            }
            for (BattleState.EnemyUnit e : st.enemies) {
                if (e.isAlive()) e.actionValue = Math.max(0f, e.actionValue - minAv);
            }

            if (nextAlly != null) {
                st.activeAllyIndex = nextAllyIdx;
                st.turnCount++;
                return;
            }

            // Enemy Turn Execution
            executeEnemyTurn(player, st, nextEnemy);
            nextEnemy.actionValue = 10000.0f / Math.max(45, nextEnemy.getEffectiveSpeed());
            checkBattleEndOrPhaseTransition(player, session);
        }
    }

    private static void executeEnemyTurn(ServerPlayer player, BattleState st, BattleState.EnemyUnit enemy) {
        int enemyIdx = st.enemies.indexOf(enemy);
        if (enemy.debuffDefTurns > 0) enemy.debuffDefTurns--;

        if (enemy.statusTurns > 0) {
            if ("AIR_BLEED".equals(enemy.statusEffect) || "FIRE_BURN".equals(enemy.statusEffect)) {
                int dot = Math.round(enemy.maxHp * 0.08f + 45f);
                enemy.hp = Math.max(0f, enemy.hp - dot);
                st.addLog(enemy.name + " 受到【" + BattleState.getStatusDisplay(enemy.statusEffect) + "】持续伤害 -" + dot);
                if (!enemy.isAlive()) return;
            }
            enemy.statusTurns--;
            if (enemy.statusTurns == 0) enemy.statusEffect = "";
        }

        if (enemy.broken) {
            enemy.broken = false;
            enemy.toughness = enemy.maxToughness;
            st.addLog(enemy.name + " 从弱点击破中恢复行动，韧性条重置。");
            if (enemy.isBossCore && st.bossPhase == 2 && !enemy.chargingUlt) {
                enemy.chargingUlt = true;
                enemy.chargeCountdown = 2;
                st.addLog("【警告】大型工程挖掘机再次启动【全屏高温喷射】蓄力！(2回合)");
            }
            return;
        }

        // Boss Phase 2 Charged Ultimate countdown
        if (enemy.isBossCore && enemy.chargingUlt) {
            enemy.chargeCountdown--;
            if (enemy.chargeCountdown <= 0) {
                enemy.chargingUlt = false;
                float aoeDmg = enemy.atk * 2.1f;
                int avgDmg = 0;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        float dmg = aoeDmg * (140f / (140f + a.def));
                        applyDamageToAlly(a, dmg);
                        a.debuffBurnTurns = 2;
                        a.energy = Math.min(a.maxEnergy, a.energy + 15f);
                        avgDmg = Math.round(dmg);
                    }
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.1f, 0.8f);
                st.addEvent(1, enemyIdx, -1, "BOSS_AOE", enemy.id, enemy.name, "全屏高温喷射", "FIRE", avgDmg, true,
                        "🔥 " + enemy.name + " 释放【全屏高温喷射】！我方全体受创并灼烧！");
                st.addLog("【全屏高温喷射！】" + enemy.name + " 蓄力完成释放烈焰冲击！我方全体受创并灼烧！");
                return;
            } else {
                st.addLog("【炉心警报】" + enemy.name + " 蓄力倒计时剩余 " + enemy.chargeCountdown + " 回合！快用【水/火】击破打断！");
            }
        }

        List<BattleState.AllyUnit> living = st.allies.stream().filter(BattleState.AllyUnit::isAlive).toList();
        if (living.isEmpty()) return;

        // If enemy intended an AOE震荡炮 attack, execute AOE
        if (enemy.intentText != null && enemy.intentText.contains("全体")) {
            int maxHit = 0;
            for (BattleState.AllyUnit a : living) {
                float dmg = Math.max(10f, enemy.atk * 0.72f * (150f / (150f + a.def)));
                applyDamageToAlly(a, dmg);
                a.energy = Math.min(a.maxEnergy, a.energy + 10f);
                maxHit = Math.max(maxHit, Math.round(dmg));
            }
            st.addEvent(1, enemyIdx, -1, "BOSS_AOE", enemy.id, enemy.name, "全体震荡炮", "FIRE", maxHit, false,
                    enemy.name + " 发射【全体震荡炮】命中我方全体 (-" + maxHit + ")");
            st.addLog(enemy.name + " 发射【全体震荡炮】，我方全员受到约 " + maxHit + " 点范围伤害。");
            return;
        }

        // Single-target attack
        BattleState.AllyUnit target = living.get(player.getRandom().nextInt(living.size()));
        for (BattleState.AllyUnit a : living) {
            if (CharacterData.ROLE_SHIELDER.equals(a.role) && player.getRandom().nextFloat() < 0.60f) {
                target = a;
                break;
            }
        }
        int targetSlot = st.allies.indexOf(target);
        float dmg = Math.max(12f, enemy.atk * (150f / (150f + target.def)));
        applyDamageToAlly(target, dmg);
        target.energy = Math.min(target.maxEnergy, target.energy + 12f);

        st.addEvent(1, enemyIdx, targetSlot, "ENEMY_ATK", enemy.id, enemy.name, "机械冲击", "EARTH", Math.round(dmg), false,
                enemy.name + " 攻击了 " + target.name + " (-" + Math.round(dmg) + ")");
        st.addLog(enemy.name + " 攻击了 " + target.name + "，造成 " + Math.round(dmg) + " 点伤害。");
    }

    private static void checkBattleEndOrPhaseTransition(ServerPlayer player, BattleSession session) {
        BattleState st = session.state;
        if (st.finished) return;

        // Boss Phase 1 mechanics check
        if ("boss_excavator".equals(st.encounterType) && st.bossPhase == 1) {
            boolean anyArmAlive = false;
            BattleState.EnemyUnit core = null;
            for (BattleState.EnemyUnit e : st.enemies) {
                if (e.isBossArm && e.isAlive()) anyArmAlive = true;
                if (e.isBossCore) core = e;
            }
            if (core != null && core.shieldProtected && !anyArmAlive) {
                core.shieldProtected = false;
                core.broken = true;
                core.toughness = 0f;
                core.actionValue += 45.0f;
                core.hp = Math.max(0f, core.hp - 150f);
                st.addLog("【双臂拆解！】护罩已破碎！大型工程挖掘机本体瘫痪暴露，全员集火输出！");
            }
            if (core != null && !core.isAlive()) {
                st.bossPhase = 2;
                st.enemies.clear();
                BattleState.EnemyUnit p2 = makeEnemy("boss_core_p2", "大型工程挖掘机·过载核心", 18, 1250f, 120f, 118f, 98, false, "WATER", "FIRE");
                p2.isBossCore = true;
                p2.chargingUlt = true;
                p2.chargeCountdown = 2;
                st.enemies.add(p2);
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.9f, 1.2f);
                st.addLog("【进入二阶段：炉心过载】弱点切换为【水 / 火】！必须在 2 回合内削空韧性打断蓄力！");
                return;
            }
        }

        boolean anyEnemyAlive = st.enemies.stream().anyMatch(BattleState.EnemyUnit::isAlive);
        boolean anyAllyAlive = st.allies.stream().anyMatch(BattleState.AllyUnit::isAlive);

        if (!anyEnemyAlive) {
            st.finished = true;
            st.victory = true;
            st.activeAllyIndex = -1;
            grantVictoryRewards(player, session);
        } else if (!anyAllyAlive) {
            st.finished = true;
            st.victory = false;
            st.activeAllyIndex = -1;
            st.rewardSummary = "全员失去战斗能力，将撤回安全锚点重整装备。";
        }
    }

    private static void grantVictoryRewards(ServerPlayer player, BattleSession session) {
        if (session.rewardsClaimed) return;
        session.rewardsClaimed = true;

        BattleState st = session.state;
        String enc = st.encounterType;
        Random rng = player.getRandom();
        List<String> drops = new ArrayList<>();

        int playerExpGain = switch (enc) {
            case "boss_excavator" -> 120;
            case "world_trial" -> 80;
            case "domain_credit", "domain_exp", "domain_char", "domain_weapon", "domain_talent" -> 45;
            default -> 28;
        };

        int credits = switch (enc) {
            case "domain_credit" -> 120;
            case "boss_excavator" -> 100;
            default -> 18 + rng.nextInt(12);
        };

        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            for (BattleState.AllyUnit au : st.allies) {
                CharacterData cd = team.getCharacterById(au.charId);
                if (cd != null) {
                    cd.setCurrentHp(Math.max(cd.getTotalMaxHp() * 0.25f, au.hp));
                    cd.setCurrentEnergy(au.energy);
                }
            }
            if ("world_trial".equals(enc) && team.canBreakthroughWorldLevel()) {
                team.setWorldLevel(team.getWorldLevel() + 1);
                drops.add("世界等级突破至 Lv." + team.getWorldLevel() + "（开放更高角色等级上限！）");
            }
            team.addPlayerExp(playerExpGain);
            team.syncHealthToPlayer(player);
            PacketSyncTeamData.syncToPlayer(player);
        });

        drops.add("主等级经验 +" + playerExpGain);
        giveItem(player, new ItemStack(ModItems.CREDIT_COIN.get(), credits));
        drops.add("信用币 x" + credits);

        switch (enc) {
            case "domain_exp" -> {
                giveItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 3));
                drops.add("角色经验瓶 x3");
            }
            case "domain_char" -> {
                giveItem(player, new ItemStack(ModItems.CHAR_MAT_1.get(), 2));
                drops.add("1级人物零件 x2");
            }
            case "domain_weapon" -> {
                giveItem(player, new ItemStack(ModItems.WEAPON_MAT_1.get(), 2));
                drops.add("1级武器构件 x2");
            }
            case "domain_talent" -> {
                giveItem(player, new ItemStack(ModItems.TALENT_MAT_1.get(), 2));
                drops.add("1级天赋手册 x2");
            }
            case "boss_excavator" -> {
                giveItem(player, new ItemStack(ModItems.EXP_BOTTLE.get(), 4));
                giveItem(player, new ItemStack(ModItems.PRECURSOR_SPARK.get(), 1));
                ItemStack goldChip = EquipmentStatsHelper.createChipWithRarity(ModItems.NEURAL_HALO_CHIP.get(), 5, rng);
                giveItem(player, goldChip);
                giveItem(player, new ItemStack(ModItems.EQUIP_ESSENCE.get(), 5));
                drops.add("高能反应堆零件 x1");
                drops.add("5级战术核心装备 x1");
                drops.add("角色经验瓶 x4 · 强化粉尘 x5");
            }
            default -> {
                int pick = rng.nextInt(3);
                if (pick == 0) {
                    giveItem(player, new ItemStack(ModItems.CHAR_MAT_1.get(), 1));
                    drops.add("1级人物零件 x1");
                } else if (pick == 1) {
                    giveItem(player, new ItemStack(ModItems.WEAPON_MAT_1.get(), 1));
                    drops.add("1级武器构件 x1");
                } else {
                    giveItem(player, new ItemStack(ModItems.TALENT_MAT_1.get(), 1));
                    drops.add("1级天赋手册 x1");
                }
                if (rng.nextFloat() < 0.25f) {
                    ItemStack chip2 = EquipmentStatsHelper.createChipWithRarity(
                            rng.nextBoolean() ? ModItems.FUSION_CORE_CHIP.get() : ModItems.VOID_DRIVE_CHIP.get(), 2, rng);
                    giveItem(player, chip2);
                    drops.add("2级芯片装备 x1（可装配或溶解）");
                }
            }
        }

        st.rewardSummary = String.join("  |  ", drops);
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void exitBattleArena(ServerPlayer player, BattleSession session) {
        ACTIVE_SESSIONS.remove(player.getUUID());
        ServerLevel level = player.getLevel();
        BattleState st = session.state;

        if (st.victory && session.overworldEnemyUuid != null) {
            Entity overworldTarget = level.getEntity(session.overworldEnemyUuid);
            if (overworldTarget != null) {
                overworldTarget.discard();
            }
        }

        if (!st.victory && st.finished) {
            if (player.getX() > 90 || session.returnPos.x > 90) {
                player.teleportTo(level, 110.5, 61.0, 28.5, 270.0f, 0.0f);
            } else {
                player.teleportTo(level, session.returnPos.x, session.returnPos.y, session.returnPos.z, session.returnYaw, session.returnPitch);
            }
            player.setHealth(Math.max(4.0f, player.getMaxHealth() * 0.5f));
        } else {
            player.teleportTo(level, session.returnPos.x, session.returnPos.y, session.returnPos.z, session.returnYaw, session.returnPitch);
        }

        BattleState closed = new BattleState();
        closed.active = false;
        PacketHandler.sendToPlayer(player, new PacketBattleSync(closed.toNbt()));

        if (st.victory) {
            StoryDirector.onBattleWon(player, st.encounterType);
        }
    }

    public static void clearSessionOnLogout(ServerPlayer player) {
        if (player != null) {
            BattleSession session = ACTIVE_SESSIONS.remove(player.getUUID());
            if (session != null) {
                player.teleportTo(player.getLevel(), session.returnPos.x, session.returnPos.y, session.returnPos.z, session.returnYaw, session.returnPitch);
            }
        }
    }
}
