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
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isBorder = (x == minX || x == maxX || z == minZ || z == maxZ);
                boolean isAllyLane = (x >= 5 && x <= 7 && z >= 6 && z <= 18);
                boolean isEnemyLane = (x >= 19 && x <= 22 && z >= 5 && z <= 19);
                var floorBlock = isAllyLane ? Blocks.LIGHT_BLUE_CONCRETE :
                        (isEnemyLane ? Blocks.RED_CONCRETE : (iceTheme ? Blocks.PACKED_ICE : Blocks.POLISHED_DEEPSLATE));
                level.setBlock(new BlockPos(x, floorY, z), floorBlock.defaultBlockState(), 2);

                boolean isCeilingLight = (x % 7 == 0 && z % 6 == 0);
                level.setBlock(new BlockPos(x, floorY + 12, z),
                        isCeilingLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState(), 2);

                for (int y = floorY + 1; y < floorY + 12; y++) {
                    if (isBorder) {
                        level.setBlock(new BlockPos(x, y, z),
                                (y == floorY + 5) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
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
        int stage = QuestManager.getQuestStage();
        if (target != null && target.getName().getString().contains("挖掘机")) {
            return "boss_excavator";
        }
        if (stage <= 1) return "corridor_cleaner";
        if (stage <= 4) return "corridor_patrol";
        if (stage <= 7) return "lower_guard";
        return "ice_hound";
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
                u.hp = Math.max(u.maxHp * 0.5f, c.getCurrentHp());
                u.shield = 0f;
                u.maxEnergy = c.getMaxEnergy();
                u.energy = Math.min(u.maxEnergy, Math.max(30f, c.getCurrentEnergy()));
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
        state.addLog(ambush ? "先手突袭成功！敌方全体韧性受损，我方抢占先机！" : "遭遇敌人拦截！准备迎战！");

        session.state = state;
        ACTIVE_SESSIONS.put(player.getUUID(), session);

        advanceTimelineUntilPlayerTurn(player, session);
        PacketHandler.sendToPlayer(player, new PacketBattleSync(state.toNbt()));
    }

    private static void populateEnemies(BattleState state, String encounterType, int worldLv, boolean ambush) {
        int lvScale = 1 + worldLv * 10;
        switch (encounterType) {
            case "corridor_cleaner" -> {
                state.enemies.add(makeEnemy("cleaner_1", "故障清扫机 A", 2, 190f, 60f, 38f, 92, ambush, "EARTH", "AIR"));
                state.enemies.add(makeEnemy("cleaner_2", "故障清扫机 B", 2, 190f, 60f, 38f, 88, ambush, "EARTH", "FIRE"));
            }
            case "corridor_patrol" -> {
                state.enemies.add(makeEnemy("drone_1", "安保巡逻机·甲", 4, 260f, 90f, 48f, 96, ambush, "AIR", "WATER"));
                state.enemies.add(makeEnemy("drone_2", "安保巡逻机·乙", 4, 260f, 90f, 48f, 94, ambush, "AIR", "EARTH"));
            }
            case "lower_guard" -> {
                state.enemies.add(makeEnemy("guard_1", "机房巡逻机", 6, 300f, 90f, 56f, 98, ambush, "WATER", "AIR"));
                state.enemies.add(makeEnemy("guard_2", "重装守卫机", 7, 460f, 120f, 68f, 92, ambush, "EARTH", "WATER"));
                state.enemies.add(makeEnemy("guard_3", "机房巡逻机", 6, 300f, 90f, 56f, 95, ambush, "AIR", "FIRE"));
            }
            case "ice_hound" -> {
                state.enemies.add(makeEnemy("hound_1", "旧式机械猎犬·左", 10, 420f, 90f, 76f, 108, ambush, "FIRE", "EARTH"));
                state.enemies.add(makeEnemy("hound_2", "科考防卫机", 11, 580f, 120f, 85f, 98, ambush, "WATER", "AIR"));
                state.enemies.add(makeEnemy("hound_3", "旧式机械猎犬·右", 10, 420f, 90f, 76f, 105, ambush, "FIRE", "WATER"));
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
                // Simulation domains scale cleanly with worldLv
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

    public static void handleBattleAction(ServerPlayer player, int actionType, int targetEnemyIndex, int allySlotIndex) {
        if (player == null) return;
        BattleSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null) return;

        BattleState st = session.state;
        if (actionType == ACTION_EXIT_FINISHED) {
            exitBattleArena(player, session);
            return;
        }
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
                    executeAllyUltimate(player, st, actor, targetEnemyIndex);
                    checkBattleEndOrPhaseTransition(player, session);
                    if (!st.finished && st.activeAllyIndex < 0) {
                        advanceTimelineUntilPlayerTurn(player, session);
                    }
                }
            }
            PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
            return;
        }

        // Standard Turn Action (Basic Q or Skill E)
        if (st.activeAllyIndex < 0 || st.activeAllyIndex >= st.allies.size()) {
            advanceTimelineUntilPlayerTurn(player, session);
            PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
            return;
        }

        BattleState.AllyUnit actor = st.allies.get(st.activeAllyIndex);
        if (actionType == ACTION_SKILL && st.skillPoints <= 0) {
            st.addLog("战技点不足！请使用 [Q] 普通攻击回复战技点。");
            PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
            return;
        }

        if (actionType == ACTION_BASIC) {
            executeAllyBasic(player, st, actor, targetEnemyIndex);
        } else if (actionType == ACTION_SKILL) {
            executeAllySkill(player, st, actor, targetEnemyIndex);
        }

        // Tick actor buffs & reset AV
        if (actor.buffAtkTurns > 0) actor.buffAtkTurns--;
        if (actor.buffSpdTurns > 0) actor.buffSpdTurns--;
        if (actor.debuffBurnTurns > 0) {
            actor.debuffBurnTurns--;
            applyDamageToAlly(actor, 35.0f);
        }
        actor.actionValue = 10000.0f / Math.max(60, actor.getEffectiveSpeed());
        st.activeAllyIndex = -1;

        checkBattleEndOrPhaseTransition(player, session);
        if (!st.finished) {
            advanceTimelineUntilPlayerTurn(player, session);
        }
        PacketHandler.sendToPlayer(player, new PacketBattleSync(st.toNbt()));
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

    private static void executeAllyBasic(ServerPlayer player, BattleState st, BattleState.AllyUnit actor, int targetIdx) {
        BattleState.EnemyUnit target = pickLivingEnemy(st, targetIdx);
        if (target == null) return;

        st.skillPoints = Math.min(st.maxSkillPoints, st.skillPoints + 1);
        actor.energy = Math.min(actor.maxEnergy, actor.energy + 20f);

        HitResult hit = dealDamageToEnemy(player, st, actor, target, 1.0f, 30.0f);
        st.addLog(actor.name + " 普攻命中 " + target.name + "，造成 " + hit.damage + (hit.crit ? "(暴击)" : "") + " 伤害，战技点 +1");
    }

    private static void executeAllySkill(ServerPlayer player, BattleState st, BattleState.AllyUnit actor, int targetIdx) {
        st.skillPoints = Math.max(0, st.skillPoints - 1);
        actor.energy = Math.min(actor.maxEnergy, actor.energy + 35f);

        switch (actor.role) {
            case CharacterData.ROLE_HEALER -> {
                BattleState.AllyUnit lowest = st.allies.stream()
                        .filter(BattleState.AllyUnit::isAlive)
                        .min(Comparator.comparingDouble(a -> a.hp / Math.max(1f, a.maxHp)))
                        .orElse(actor);
                float healAmt = (180f + actor.maxHp * 0.25f) * actor.talentScale;
                lowest.hp = Math.min(lowest.maxHp, lowest.hp + healAmt);
                lowest.debuffBurnTurns = 0;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive() && a != lowest) {
                        a.hp = Math.min(a.maxHp, a.hp + healAmt * 0.35f);
                        a.debuffBurnTurns = 0;
                    }
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.3f);
                st.addLog(actor.name + " 施放治疗战技，为 " + lowest.name + " 恢复 " + Math.round(healAmt) + " 生命并净化异常！");
            }
            case CharacterData.ROLE_SHIELDER -> {
                float shieldAmt = (160f + actor.def * 1.4f) * actor.talentScale;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) a.shield = Math.max(a.shield, shieldAmt);
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.1f);
                st.addLog(actor.name + " 展开防护力场，全队获得 " + Math.round(shieldAmt) + " 点护盾！");
            }
            case CharacterData.ROLE_BUFFER -> {
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        a.buffAtkTurns = 2;
                        a.buffSpdTurns = 2;
                    }
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9f, 1.4f);
                st.addLog(actor.name + " 启动战术调度！全队攻击力 +25%、速度 +15（持续 2 回合）！");
            }
            case CharacterData.ROLE_SINGLE -> {
                BattleState.EnemyUnit target = pickLivingEnemy(st, targetIdx);
                if (target != null) {
                    HitResult hit = dealDamageToEnemy(player, st, actor, target, 2.2f, 75.0f);
                    st.addLog(actor.name + " 精准狙击 " + target.name + "，造成 " + hit.damage + (hit.crit ? "(暴击)" : "") + " 点高额伤害！");
                }
            }
            case CharacterData.ROLE_AOE -> {
                int totalDmg = 0;
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.isAlive()) {
                        totalDmg += dealDamageToEnemy(player, st, actor, e, 1.15f, 40.0f).damage;
                    }
                }
                st.addLog(actor.name + " 发动横扫战技，对敌方全体累计造成 " + totalDmg + " 点范围伤害！");
            }
            default -> { // BALANCED
                int mainIdx = (targetIdx >= 0 && targetIdx < st.enemies.size() && st.enemies.get(targetIdx).isAlive())
                        ? targetIdx : 0;
                BattleState.EnemyUnit main = pickLivingEnemy(st, mainIdx);
                if (main != null) {
                    int actualIdx = st.enemies.indexOf(main);
                    HitResult hit = dealDamageToEnemy(player, st, actor, main, 1.55f, 60.0f);
                    int splashDmg = 0;
                    if (actualIdx - 1 >= 0 && st.enemies.get(actualIdx - 1).isAlive()) {
                        splashDmg += dealDamageToEnemy(player, st, actor, st.enemies.get(actualIdx - 1), 0.80f, 30.0f).damage;
                    }
                    if (actualIdx + 1 < st.enemies.size() && st.enemies.get(actualIdx + 1).isAlive()) {
                        splashDmg += dealDamageToEnemy(player, st, actor, st.enemies.get(actualIdx + 1), 0.80f, 30.0f).damage;
                    }
                    st.addLog(actor.name + " 挥斩重击 " + main.name + " 造成 " + hit.damage + " 伤"
                            + (splashDmg > 0 ? "（溅射周围 " + splashDmg + " 伤）" : ""));
                }
            }
        }
    }

    private static void executeAllyUltimate(ServerPlayer player, BattleState st, BattleState.AllyUnit actor, int targetIdx) {
        actor.energy = 0f;
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8f, 1.35f);

        switch (actor.role) {
            case CharacterData.ROLE_HEALER -> {
                float healAmt = (280f + actor.maxHp * 0.38f) * actor.talentScale;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        a.hp = Math.min(a.maxHp, a.hp + healAmt);
                        a.debuffBurnTurns = 0;
                    }
                }
                st.addLog("【终结技插队】" + actor.name + " 全队急救！所有人恢复 " + Math.round(healAmt) + " 生命并净化负面！");
            }
            case CharacterData.ROLE_SHIELDER -> {
                float shieldAmt = (260f + actor.def * 2.0f) * actor.talentScale;
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) a.shield = Math.max(a.shield, shieldAmt);
                }
                int dmg = 0;
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.isAlive()) dmg += dealDamageToEnemy(player, st, actor, e, 1.3f, 50.0f).damage;
                }
                st.addLog("【终结技插队】" + actor.name + " 铸就全队重盾 " + Math.round(shieldAmt) + "，并震伤敌方 " + dmg + " 伤！");
            }
            case CharacterData.ROLE_BUFFER -> {
                st.skillPoints = Math.min(st.maxSkillPoints, st.skillPoints + 2);
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        a.buffAtkTurns = 3;
                        a.buffSpdTurns = 3;
                    }
                }
                st.addLog("【终结技插队】" + actor.name + " 紧急调度！立刻恢复 2 点战技点并强化全队 3 回合！");
            }
            case CharacterData.ROLE_SINGLE -> {
                BattleState.EnemyUnit target = pickLivingEnemy(st, targetIdx);
                if (target != null) {
                    HitResult hit = dealDamageToEnemy(player, st, actor, target, 3.6f, 110.0f);
                    st.addLog("【终结技插队】" + actor.name + " 致命穿透命中 " + target.name + "，打出 " + hit.damage + " 巨额伤害！");
                }
            }
            case CharacterData.ROLE_AOE -> {
                int dmg = 0;
                for (BattleState.EnemyUnit e : st.enemies) {
                    if (e.isAlive()) dmg += dealDamageToEnemy(player, st, actor, e, 1.95f, 70.0f).damage;
                }
                st.addLog("【终结技插队】" + actor.name + " 全场压制轰炸，共造成 " + dmg + " 点伤害！");
            }
            default -> { // BALANCED
                BattleState.EnemyUnit main = pickLivingEnemy(st, targetIdx);
                if (main != null) {
                    int actualIdx = st.enemies.indexOf(main);
                    HitResult hit = dealDamageToEnemy(player, st, actor, main, 2.55f, 90.0f);
                    int splash = 0;
                    if (actualIdx - 1 >= 0 && st.enemies.get(actualIdx - 1).isAlive()) {
                        splash += dealDamageToEnemy(player, st, actor, st.enemies.get(actualIdx - 1), 1.40f, 55.0f).damage;
                    }
                    if (actualIdx + 1 < st.enemies.size() && st.enemies.get(actualIdx + 1).isAlive()) {
                        splash += dealDamageToEnemy(player, st, actor, st.enemies.get(actualIdx + 1), 1.40f, 55.0f).damage;
                    }
                    st.addLog("【终结技插队】" + actor.name + " 全力重击 " + main.name + " 打出 " + hit.damage
                            + " 伤" + (splash > 0 ? " + 扩散 " + splash + " 伤！" : "！"));
                }
            }
        }
    }

    private record HitResult(int damage, boolean crit) {}

    private static HitResult dealDamageToEnemy(ServerPlayer player, BattleState st, BattleState.AllyUnit actor,
                                               BattleState.EnemyUnit enemy, float mult, float toughnessCost) {
        Random rng = player.getRandom();
        float atk = actor.getEffectiveAtk() * actor.talentScale;
        float bonus = 1.0f + actor.elemDmgBonus / 100.0f;
        boolean crit = rng.nextFloat() * 100.0f < actor.critRate;
        float critMult = crit ? (1.0f + actor.critDmg / 100.0f) : 1.0f;

        float defShred = "EARTH_SHRED".equals(enemy.statusEffect) ? 0.72f : 1.0f;
        float defMult = 240.0f / (240.0f + enemy.def * defShred);
        float shieldMult = enemy.shieldProtected ? 0.20f : (enemy.broken ? 1.15f : 0.90f);

        int dmg = Math.max(5, Math.round(atk * mult * bonus * critMult * defMult * shieldMult));
        enemy.hp = Math.max(0f, enemy.hp - dmg);

        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, crit ? 1.3f : 1.0f);

        // Toughness reduction if element matches enemy weakness
        if (!enemy.broken && enemy.hasWeakness(actor.element)) {
            enemy.toughness = Math.max(0f, enemy.toughness - toughnessCost);
            if (enemy.toughness <= 0f) {
                triggerWeaknessBreak(player, st, actor, enemy);
            }
        }
        return new HitResult(dmg, crit);
    }

    private static void triggerWeaknessBreak(ServerPlayer player, BattleState st, BattleState.AllyUnit actor, BattleState.EnemyUnit enemy) {
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

        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.1f, 0.9f);
        st.addLog("【弱点击破！】" + actor.name + "(" + BattleState.getElementShort(actor.element) + ") 击破了 "
                + enemy.name + "，附加【" + BattleState.getStatusDisplay(enemy.statusEffect) + "】并造成 " + breakDmg + " 破盾伤！");

        // Boss Phase 2 Ultimate Interrupt check
        if (enemy.isBossCore && enemy.chargingUlt) {
            enemy.chargingUlt = false;
            enemy.chargeCountdown = 0;
            int overloadDmg = Math.round(enemy.maxHp * 0.25f);
            enemy.hp = Math.max(0f, enemy.hp - overloadDmg);
            enemy.actionValue += 50.0f;
            st.addLog("【蓄力打断！】挖掘机炉心过载反噬，自损 " + overloadDmg + " 生命并陷入大硬直！");
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
        // Apply DoT status at start of enemy turn
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
            st.addLog(enemy.name + " 从弱点击破中恢复了行动能力，韧性条重置。");
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
                for (BattleState.AllyUnit a : st.allies) {
                    if (a.isAlive()) {
                        float dmg = aoeDmg * (140f / (140f + a.def));
                        applyDamageToAlly(a, dmg);
                        a.debuffBurnTurns = 2;
                        a.energy = Math.min(a.maxEnergy, a.energy + 15f);
                    }
                }
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.1f, 0.8f);
                st.addLog("【全屏高温喷射！】大型工程挖掘机完成蓄力释放烈焰冲击！我方全体受创并陷入灼烧！");
                return;
            } else {
                st.addLog("【炉心警报】" + enemy.name + " 蓄力倒计时剩余 " + enemy.chargeCountdown + " 回合！快用【水/火】击破打断！");
            }
        }

        // Normal Enemy Attack on a living ally (Shielders slightly favored if alive)
        List<BattleState.AllyUnit> living = st.allies.stream().filter(BattleState.AllyUnit::isAlive).toList();
        if (living.isEmpty()) return;
        BattleState.AllyUnit target = living.get(player.getRandom().nextInt(living.size()));
        for (BattleState.AllyUnit a : living) {
            if (CharacterData.ROLE_SHIELDER.equals(a.role) && player.getRandom().nextFloat() < 0.55f) {
                target = a;
                break;
            }
        }

        float dmg = Math.max(12f, enemy.atk * (150f / (150f + target.def)));
        applyDamageToAlly(target, dmg);
        target.energy = Math.min(target.maxEnergy, target.energy + 12f);
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
                // Transition to Phase 2!
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

        // Sync surviving ally HP back to TeamCapability + add playerExp
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

        // Strictly controlled, low-inflation material drops
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
                // Normal overworld encounter: only 1 low-tier part, zero equipment XP!
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
            // Defeat respawn at safe spot with 50% HP
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
