package com.resilienthope.core.combat;

import com.resilienthope.core.team.CharacterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class BattleState {
    public static class AllyUnit {
        public String charId;
        public String name;
        public String element;
        public String role;
        public float hp;
        public float maxHp;
        public float shield;
        public float energy;
        public float maxEnergy;
        public float atk;
        public float def;
        public int speed;
        public float critRate;
        public float critDmg;
        public float elemDmgBonus;
        public float talentScale;
        public int buffAtkTurns;
        public int buffSpdTurns;
        public int debuffBurnTurns;
        public float actionValue;

        public boolean isAlive() { return hp > 0.0f; }

        public int getEffectiveSpeed() {
            return speed + (buffSpdTurns > 0 ? 15 : 0);
        }

        public float getEffectiveAtk() {
            return atk * (buffAtkTurns > 0 ? 1.25f : 1.0f);
        }

        public CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putString("CharId", charId);
            t.putString("Name", name);
            t.putString("Element", element);
            t.putString("Role", role);
            t.putFloat("Hp", hp);
            t.putFloat("MaxHp", maxHp);
            t.putFloat("Shield", shield);
            t.putFloat("Energy", energy);
            t.putFloat("MaxEnergy", maxEnergy);
            t.putFloat("Atk", atk);
            t.putFloat("Def", def);
            t.putInt("Speed", speed);
            t.putFloat("CritRate", critRate);
            t.putFloat("CritDmg", critDmg);
            t.putFloat("ElemDmgBonus", elemDmgBonus);
            t.putFloat("TalentScale", talentScale);
            t.putInt("BuffAtkTurns", buffAtkTurns);
            t.putInt("BuffSpdTurns", buffSpdTurns);
            t.putInt("DebuffBurnTurns", debuffBurnTurns);
            t.putFloat("AV", actionValue);
            return t;
        }

        public static AllyUnit fromNbt(CompoundTag t) {
            AllyUnit u = new AllyUnit();
            u.charId = t.getString("CharId");
            u.name = t.getString("Name");
            u.element = t.getString("Element");
            u.role = t.getString("Role");
            u.hp = t.getFloat("Hp");
            u.maxHp = t.getFloat("MaxHp");
            u.shield = t.getFloat("Shield");
            u.energy = t.getFloat("Energy");
            u.maxEnergy = t.getFloat("MaxEnergy");
            u.atk = t.getFloat("Atk");
            u.def = t.getFloat("Def");
            u.speed = t.getInt("Speed");
            u.critRate = t.getFloat("CritRate");
            u.critDmg = t.getFloat("CritDmg");
            u.elemDmgBonus = t.getFloat("ElemDmgBonus");
            u.talentScale = t.getFloat("TalentScale");
            u.buffAtkTurns = t.getInt("BuffAtkTurns");
            u.buffSpdTurns = t.getInt("BuffSpdTurns");
            u.debuffBurnTurns = t.getInt("DebuffBurnTurns");
            u.actionValue = t.getFloat("AV");
            return u;
        }
    }

    public static class EnemyUnit {
        public String id;
        public String name;
        public int level;
        public float hp;
        public float maxHp;
        public float toughness;
        public float maxToughness;
        public float atk;
        public float def;
        public int speed;
        public final List<String> weaknesses = new ArrayList<>();
        public boolean broken;
        public boolean shieldProtected;
        public String statusEffect = ""; // WATER_SLOW, EARTH_SHRED, AIR_BLEED, FIRE_BURN
        public int statusTurns;
        public boolean chargingUlt;
        public int chargeCountdown;
        public boolean isBossCore;
        public boolean isBossArm;
        public float actionValue;

        public boolean isAlive() { return hp > 0.0f; }

        public int getEffectiveSpeed() {
            int spd = speed;
            if ("WATER_SLOW".equals(statusEffect) && statusTurns > 0) {
                spd = Math.max(40, Math.round(spd * 0.78f));
            }
            return spd;
        }

        public boolean hasWeakness(String element) {
            return weaknesses.contains(element);
        }

        public CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putString("Id", id);
            t.putString("Name", name);
            t.putInt("Level", level);
            t.putFloat("Hp", hp);
            t.putFloat("MaxHp", maxHp);
            t.putFloat("Toughness", toughness);
            t.putFloat("MaxToughness", maxToughness);
            t.putFloat("Atk", atk);
            t.putFloat("Def", def);
            t.putInt("Speed", speed);
            t.putString("Weaknesses", String.join(",", weaknesses));
            t.putBoolean("Broken", broken);
            t.putBoolean("ShieldProtected", shieldProtected);
            t.putString("StatusEffect", statusEffect != null ? statusEffect : "");
            t.putInt("StatusTurns", statusTurns);
            t.putBoolean("ChargingUlt", chargingUlt);
            t.putInt("ChargeCountdown", chargeCountdown);
            t.putBoolean("IsBossCore", isBossCore);
            t.putBoolean("IsBossArm", isBossArm);
            t.putFloat("AV", actionValue);
            return t;
        }

        public static EnemyUnit fromNbt(CompoundTag t) {
            EnemyUnit e = new EnemyUnit();
            e.id = t.getString("Id");
            e.name = t.getString("Name");
            e.level = t.getInt("Level");
            e.hp = t.getFloat("Hp");
            e.maxHp = t.getFloat("MaxHp");
            e.toughness = t.getFloat("Toughness");
            e.maxToughness = t.getFloat("MaxToughness");
            e.atk = t.getFloat("Atk");
            e.def = t.getFloat("Def");
            e.speed = t.getInt("Speed");
            String w = t.getString("Weaknesses");
            if (!w.isEmpty()) {
                for (String s : w.split(",")) {
                    if (!s.isEmpty()) e.weaknesses.add(s);
                }
            }
            e.broken = t.getBoolean("Broken");
            e.shieldProtected = t.getBoolean("ShieldProtected");
            e.statusEffect = t.getString("StatusEffect");
            e.statusTurns = t.getInt("StatusTurns");
            e.chargingUlt = t.getBoolean("ChargingUlt");
            e.chargeCountdown = t.getInt("ChargeCountdown");
            e.isBossCore = t.getBoolean("IsBossCore");
            e.isBossArm = t.getBoolean("IsBossArm");
            e.actionValue = t.getFloat("AV");
            return e;
        }
    }

    public String encounterType = "corridor_cleaner";
    public boolean active = false;
    public boolean finished = false;
    public boolean victory = false;
    public boolean canFlee = true;
    public int skillPoints = 3;
    public int maxSkillPoints = 5;
    public int turnCount = 1;
    public int bossPhase = 1;
    public int activeAllyIndex = 0;
    public String rewardSummary = "";
    public final List<AllyUnit> allies = new ArrayList<>();
    public final List<EnemyUnit> enemies = new ArrayList<>();
    public final List<String> logs = new ArrayList<>();

    public void addLog(String line) {
        logs.add(line);
        while (logs.size() > 6) {
            logs.remove(0);
        }
    }

    public CompoundTag toNbt() {
        CompoundTag root = new CompoundTag();
        root.putString("EncounterType", encounterType);
        root.putBoolean("Active", active);
        root.putBoolean("Finished", finished);
        root.putBoolean("Victory", victory);
        root.putBoolean("CanFlee", canFlee);
        root.putInt("SkillPoints", skillPoints);
        root.putInt("MaxSkillPoints", maxSkillPoints);
        root.putInt("TurnCount", turnCount);
        root.putInt("BossPhase", bossPhase);
        root.putInt("ActiveAllyIndex", activeAllyIndex);
        root.putString("RewardSummary", rewardSummary);

        ListTag allyList = new ListTag();
        for (AllyUnit a : allies) allyList.add(a.toNbt());
        root.put("Allies", allyList);

        ListTag enemyList = new ListTag();
        for (EnemyUnit e : enemies) enemyList.add(e.toNbt());
        root.put("Enemies", enemyList);

        ListTag logList = new ListTag();
        for (String s : logs) {
            CompoundTag lt = new CompoundTag();
            lt.putString("Text", s);
            logList.add(lt);
        }
        root.put("Logs", logList);
        return root;
    }

    public static BattleState fromNbt(CompoundTag root) {
        BattleState st = new BattleState();
        if (root == null) return st;
        st.encounterType = root.getString("EncounterType");
        st.active = root.getBoolean("Active");
        st.finished = root.getBoolean("Finished");
        st.victory = root.getBoolean("Victory");
        st.canFlee = root.getBoolean("CanFlee");
        st.skillPoints = root.getInt("SkillPoints");
        st.maxSkillPoints = Math.max(1, root.getInt("MaxSkillPoints"));
        st.turnCount = root.getInt("TurnCount");
        st.bossPhase = Math.max(1, root.getInt("BossPhase"));
        st.activeAllyIndex = root.getInt("ActiveAllyIndex");
        st.rewardSummary = root.getString("RewardSummary");

        ListTag allyList = root.getList("Allies", Tag.TAG_COMPOUND);
        for (int i = 0; i < allyList.size(); i++) st.allies.add(AllyUnit.fromNbt(allyList.getCompound(i)));

        ListTag enemyList = root.getList("Enemies", Tag.TAG_COMPOUND);
        for (int i = 0; i < enemyList.size(); i++) st.enemies.add(EnemyUnit.fromNbt(enemyList.getCompound(i)));

        ListTag logList = root.getList("Logs", Tag.TAG_COMPOUND);
        for (int i = 0; i < logList.size(); i++) st.logs.add(logList.getCompound(i).getString("Text"));

        return st;
    }

    public static String getStatusDisplay(String statusEffect) {
        return switch (statusEffect) {
            case "WATER_SLOW" -> "减速";
            case "EARTH_SHRED" -> "破甲";
            case "AIR_BLEED" -> "撕裂";
            case "FIRE_BURN" -> "灼烧";
            default -> "";
        };
    }

    public static String getElementShort(String elem) {
        return CharacterData.getElementLabel(elem);
    }
}
