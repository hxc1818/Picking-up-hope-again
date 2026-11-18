package com.resilienthope.core.team;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

public class CharacterData {
    public static final String ELEM_WATER = "WATER";
    public static final String ELEM_EARTH = "EARTH";
    public static final String ELEM_AIR = "AIR";
    public static final String ELEM_FIRE = "FIRE";

    public static final String ROLE_HEALER = "HEALER";
    public static final String ROLE_SHIELDER = "SHIELDER";
    public static final String ROLE_SINGLE = "SINGLE";
    public static final String ROLE_AOE = "AOE";
    public static final String ROLE_BALANCED = "BALANCED";
    public static final String ROLE_BUFFER = "BUFFER";

    private String charId;
    private String name;
    private String element;
    private String role;
    private int rarity; // 4 or 5 stars
    private float baseMaxHp;
    private float baseAtkVal;
    private float baseDefVal;
    private int baseSpeed;
    private float currentHp;
    private float maxEnergy;
    private float currentEnergy;
    private int eCooldownTicks;
    private int level;      // 1..80
    private int ascension;  // 0..6 -> caps at 20, 30, 40, 50, 60, 70, 80
    private int talentLevel;// 1..10
    private int eidolon;    // 0..6
    private boolean unlocked;

    private ItemStack equippedWeapon;
    private final ItemStack[] equippedChips = new ItemStack[5];

    public CharacterData(String charId, String name, String element, String role, int rarity,
                         float baseMaxHp, float baseAtkVal, float baseDefVal, int baseSpeed,
                         float maxEnergy, boolean unlocked, ItemStack starterWeapon) {
        this.charId = charId;
        this.name = name;
        this.element = element;
        this.role = role;
        this.rarity = rarity;
        this.baseMaxHp = baseMaxHp;
        this.baseAtkVal = baseAtkVal;
        this.baseDefVal = baseDefVal;
        this.baseSpeed = baseSpeed;
        this.currentHp = baseMaxHp;
        this.maxEnergy = maxEnergy;
        this.currentEnergy = 0;
        this.eCooldownTicks = 0;
        this.level = 1;
        this.ascension = 0;
        this.talentLevel = 1;
        this.eidolon = 0;
        this.unlocked = unlocked;
        this.equippedWeapon = starterWeapon != null ? starterWeapon : ItemStack.EMPTY;
        if (!this.equippedWeapon.isEmpty()) {
            EquipmentStatsHelper.ensureStats(this.equippedWeapon);
        }
        for (int i = 0; i < 5; i++) {
            this.equippedChips[i] = ItemStack.EMPTY;
        }
    }

    public String getCharId() { return charId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getRarity() { return rarity; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(getMaxLevelForAscension(), level)); }

    public int getAscension() { return ascension; }
    public void setAscension(int ascension) { this.ascension = Math.max(0, Math.min(6, ascension)); }

    public int getMaxLevelForAscension() {
        return switch (ascension) {
            case 0 -> 20;
            case 1 -> 30;
            case 2 -> 40;
            case 3 -> 50;
            case 4 -> 60;
            case 5 -> 70;
            default -> 80;
        };
    }

    public int getTalentLevel() { return talentLevel; }
    public void setTalentLevel(int talentLevel) { this.talentLevel = Math.max(1, Math.min(10, talentLevel)); }

    public int getEidolon() { return eidolon; }
    public void setEidolon(int eidolon) { this.eidolon = Math.max(0, Math.min(6, eidolon)); }

    public float getTalentScale() {
        return 1.0f + (talentLevel - 1) * 0.08f + eidolon * 0.05f;
    }

    public ItemStack getEquippedWeapon() { return equippedWeapon; }
    public void setEquippedWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.equippedWeapon = ItemStack.EMPTY;
        } else {
            this.equippedWeapon = stack.copy();
            EquipmentStatsHelper.ensureStats(this.equippedWeapon);
        }
    }

    public ItemStack getEquippedChip(int slot) {
        if (slot >= 0 && slot < 5) return equippedChips[slot];
        return ItemStack.EMPTY;
    }

    public void setEquippedChip(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 5) {
            if (stack == null || stack.isEmpty()) {
                equippedChips[slot] = ItemStack.EMPTY;
            } else {
                equippedChips[slot] = stack.copy();
                EquipmentStatsHelper.ensureStats(equippedChips[slot]);
            }
        }
    }

    public float getMaxHp() { return getTotalMaxHp(); }
    public void setMaxHp(float baseMaxHp) { this.baseMaxHp = baseMaxHp; }

    public float getCurrentHp() { return Math.min(currentHp, getTotalMaxHp()); }
    public void setCurrentHp(float currentHp) {
        this.currentHp = Math.max(0, Math.min(currentHp, getTotalMaxHp()));
    }

    public float getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(float maxEnergy) { this.maxEnergy = maxEnergy; }

    public float getCurrentEnergy() { return currentEnergy; }
    public void setCurrentEnergy(float currentEnergy) {
        this.currentEnergy = Math.max(0, Math.min(currentEnergy, maxEnergy));
    }

    public void addEnergy(float delta) {
        this.currentEnergy = Math.max(0, Math.min(this.currentEnergy + delta, this.maxEnergy));
    }

    public int getECooldownTicks() { return eCooldownTicks; }
    public void setECooldownTicks(int ticks) { this.eCooldownTicks = Math.max(0, ticks); }

    private float sumStat(String mainType, String subTag) {
        float total = 0f;
        if (!equippedWeapon.isEmpty()) {
            if (mainType != null && mainType.equals(EquipmentStatsHelper.getMainType(equippedWeapon))) {
                total += EquipmentStatsHelper.getMainValue(equippedWeapon);
            }
            if (subTag != null) {
                total += EquipmentStatsHelper.getSubValue(equippedWeapon, subTag);
            }
        }
        for (int i = 0; i < 5; i++) {
            ItemStack chip = equippedChips[i];
            if (chip == null || chip.isEmpty()) continue;
            if (mainType != null && mainType.equals(EquipmentStatsHelper.getMainType(chip))) {
                total += EquipmentStatsHelper.getMainValue(chip);
            }
            if (subTag != null) {
                total += EquipmentStatsHelper.getSubValue(chip, subTag);
            }
        }
        return total;
    }

    public float getTotalMaxHp() {
        float scaledBase = baseMaxHp + (level - 1) * 45.0f + ascension * 220.0f;
        float flat = sumStat(EquipmentStatsHelper.MAIN_HP_FLAT, EquipmentStatsHelper.TAG_SUB_HP_FLAT);
        return scaledBase + flat;
    }

    public float getBaseAtk() {
        float charBase = baseAtkVal + (level - 1) * 5.5f + ascension * 30.0f;
        float weaponAtk = 0f;
        if (!equippedWeapon.isEmpty() && EquipmentStatsHelper.MAIN_WEAPON_ATK.equals(EquipmentStatsHelper.getMainType(equippedWeapon))) {
            weaponAtk = EquipmentStatsHelper.getMainValue(equippedWeapon);
        }
        return charBase + weaponAtk;
    }

    public float getExtraAtk() {
        float atkPct = sumStat(EquipmentStatsHelper.MAIN_ATK_PCT, EquipmentStatsHelper.TAG_SUB_ATK_PCT);
        float atkFlat = sumStat(EquipmentStatsHelper.MAIN_ATK_FLAT, null);
        return getBaseAtk() * (atkPct / 100.0f) + atkFlat;
    }

    public float getTotalAtk() {
        return getBaseAtk() + getExtraAtk();
    }

    public float getTotalDef() {
        int enhanceSum = 0;
        for (ItemStack chip : equippedChips) {
            if (chip != null && !chip.isEmpty()) {
                enhanceSum += EquipmentStatsHelper.getEnhanceLevel(chip);
            }
        }
        return baseDefVal + (level - 1) * 4.0f + ascension * 20.0f + enhanceSum * 2.5f;
    }

    public int getTotalSpeed() {
        int bonus = Math.round(sumStat(EquipmentStatsHelper.MAIN_SPD, EquipmentStatsHelper.TAG_SUB_SPD));
        return baseSpeed + bonus;
    }

    public float getCritRate() {
        float bonus = sumStat(EquipmentStatsHelper.MAIN_CRIT_RATE, EquipmentStatsHelper.TAG_SUB_CRIT_RATE);
        return Math.min(100.0f, 5.0f + bonus);
    }

    public float getCritDmg() {
        float bonus = sumStat(EquipmentStatsHelper.MAIN_CRIT_DMG, EquipmentStatsHelper.TAG_SUB_CRIT_DMG);
        return 50.0f + bonus;
    }

    public float getElemDmgBonus() {
        float bonus = sumStat(EquipmentStatsHelper.MAIN_ELEM_DMG, null);
        if (!equippedWeapon.isEmpty() && equippedWeapon.getItem() == ModItems.PRECURSOR_CLAYMORE.get()) {
            bonus += 20.0f;
        }
        return bonus;
    }

    public int countEquippedChips() {
        int c = 0;
        for (ItemStack chip : equippedChips) {
            if (chip != null && !chip.isEmpty()) c++;
        }
        return c;
    }

    public String getElementLabel() {
        return getElementLabel(this.element);
    }

    public static String getElementLabel(String elem) {
        return switch (elem) {
            case ELEM_WATER -> "水";
            case ELEM_AIR -> "气";
            case ELEM_FIRE -> "火";
            default -> "地";
        };
    }

    public static int getElementColor(String elem) {
        return switch (elem) {
            case ELEM_WATER -> 0xFF38BDF8;
            case ELEM_AIR -> 0xFF4ADE80;
            case ELEM_FIRE -> 0xFFEF4444;
            default -> 0xFFD97706;
        };
    }

    public String getRoleLabel() {
        return switch (role) {
            case ROLE_HEALER -> "治疗";
            case ROLE_SHIELDER -> "拉盾";
            case ROLE_SINGLE -> "对单";
            case ROLE_AOE -> "对群";
            case ROLE_BUFFER -> "增益";
            default -> "群单输出均衡";
        };
    }

    public String getBasicDesc() {
        return "普通攻击 (+1战技点)：造成 100% 攻击力的" + getElementLabel() + "属性伤害，削减 30 点韧性。";
    }

    public String getSkillDesc() {
        return switch (role) {
            case ROLE_HEALER -> "战技 (-1战技点)：为生命最低队友恢复大量血量并清除负面状态，同时微弱回血全队。";
            case ROLE_SHIELDER -> "战技 (-1战技点)：为我方全体套上厚实护盾，并吸引敌方部分火力。";
            case ROLE_SINGLE -> "战技 (-1战技点)：对指定目标发动 220% 高伤点杀，削减 75 点韧性。";
            case ROLE_AOE -> "战技 (-1战技点)：攻击敌方全体造成 115% 范围伤害，全体削减 40 点韧性。";
            case ROLE_BUFFER -> "战技 (-1战技点)：为我方全体提升 25% 攻击力与 15 点速度，持续 2 回合。";
            default -> "战技 (-1战技点)：主目标造成 155% 伤害(削韧60)，相邻目标承受 80% 溅射(削韧30)。";
        };
    }

    public String getUltDesc() {
        return switch (role) {
            case ROLE_HEALER -> "终结技 (满能量随时插队)：立刻为我方全体大幅恢复生命并解除全部异常状态！";
            case ROLE_SHIELDER -> "终结技 (满能量随时插队)：全队获得高额重型护盾，并对敌方全体造成" + getElementLabel() + "属性冲击！";
            case ROLE_SINGLE -> "终结技 (满能量随时插队)：对单体目标打出 360% 贯穿伤害，强力削减 110 点韧性！";
            case ROLE_AOE -> "终结技 (满能量随时插队)：对敌方全体造成 195% 覆盖打击，全体削减 70 点韧性！";
            case ROLE_BUFFER -> "终结技 (满能量随时插队)：立刻恢复 2 点战技点，并大幅提高全队暴击率与攻击力！";
            default -> "终结技 (满能量随时插队)：对主目标造成 250% 伤害并向两侧扩散 140% 伤害！";
        };
    }

    public DamageResult computeDamage(float skillMultiplier, Random rng) {
        float atk = getTotalAtk() * getTalentScale();
        float dmgBonusMult = 1.0f + getElemDmgBonus() / 100.0f;
        boolean isCrit = rng.nextFloat() * 100.0f < getCritRate();
        float critMult = isCrit ? (1.0f + getCritDmg() / 100.0f) : 1.0f;
        float rawMcDamage = Math.max(2.0f, (atk / 45.0f) * skillMultiplier * dmgBonusMult * critMult);
        int displayDamage = Math.max(1, Math.round(atk * skillMultiplier * dmgBonusMult * critMult));
        return new DamageResult(rawMcDamage, displayDamage, isCrit);
    }

    public record DamageResult(float mcDamage, int displayDamage, boolean isCrit) {}

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("CharId", charId);
        tag.putString("Name", name);
        tag.putString("Element", element);
        tag.putString("Role", role);
        tag.putInt("Rarity", rarity);
        tag.putFloat("BaseMaxHp", baseMaxHp);
        tag.putFloat("BaseAtkVal", baseAtkVal);
        tag.putFloat("BaseDefVal", baseDefVal);
        tag.putInt("BaseSpeed", baseSpeed);
        tag.putFloat("CurrentHp", currentHp);
        tag.putFloat("MaxEnergy", maxEnergy);
        tag.putFloat("CurrentEnergy", currentEnergy);
        tag.putInt("ECooldownTicks", eCooldownTicks);
        tag.putInt("Level", level);
        tag.putInt("Ascension", ascension);
        tag.putInt("TalentLevel", talentLevel);
        tag.putInt("Eidolon", eidolon);
        tag.putBoolean("Unlocked", unlocked);

        if (!equippedWeapon.isEmpty()) {
            tag.put("EquippedWeapon", equippedWeapon.save(new CompoundTag()));
        }
        ListTag chipsList = new ListTag();
        for (int i = 0; i < 5; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("Slot", i);
            if (equippedChips[i] != null && !equippedChips[i].isEmpty()) {
                slotTag.put("Item", equippedChips[i].save(new CompoundTag()));
            }
            chipsList.add(slotTag);
        }
        tag.put("EquippedChips", chipsList);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("CharId")) this.charId = tag.getString("CharId");
        if (tag.contains("Name")) this.name = tag.getString("Name");
        if (tag.contains("Element")) this.element = tag.getString("Element");
        if (tag.contains("Role")) this.role = tag.getString("Role");
        if (tag.contains("Rarity")) this.rarity = Math.max(4, tag.getInt("Rarity"));
        if (tag.contains("BaseMaxHp")) this.baseMaxHp = tag.getFloat("BaseMaxHp");
        if (tag.contains("BaseAtkVal")) this.baseAtkVal = tag.getFloat("BaseAtkVal");
        if (tag.contains("BaseDefVal")) this.baseDefVal = tag.getFloat("BaseDefVal");
        if (tag.contains("BaseSpeed")) this.baseSpeed = Math.max(80, tag.getInt("BaseSpeed"));
        this.currentHp = tag.getFloat("CurrentHp");
        if (tag.contains("MaxEnergy") && tag.getFloat("MaxEnergy") > 0) this.maxEnergy = tag.getFloat("MaxEnergy");
        this.currentEnergy = tag.getFloat("CurrentEnergy");
        this.eCooldownTicks = tag.getInt("ECooldownTicks");
        if (tag.contains("Level")) this.level = Math.max(1, Math.min(80, tag.getInt("Level")));
        if (tag.contains("Ascension")) this.ascension = Math.max(0, Math.min(6, tag.getInt("Ascension")));
        if (tag.contains("TalentLevel")) this.talentLevel = Math.max(1, Math.min(10, tag.getInt("TalentLevel")));
        if (tag.contains("Eidolon")) this.eidolon = Math.max(0, Math.min(6, tag.getInt("Eidolon")));
        if (tag.contains("Unlocked")) this.unlocked = tag.getBoolean("Unlocked");

        if (tag.contains("EquippedWeapon")) {
            this.equippedWeapon = ItemStack.of(tag.getCompound("EquippedWeapon"));
            if (!this.equippedWeapon.isEmpty()) {
                EquipmentStatsHelper.ensureStats(this.equippedWeapon);
            }
        }
        for (int i = 0; i < 5; i++) {
            this.equippedChips[i] = ItemStack.EMPTY;
        }
        if (tag.contains("EquippedChips", Tag.TAG_LIST)) {
            ListTag chipsList = tag.getList("EquippedChips", Tag.TAG_COMPOUND);
            for (int i = 0; i < chipsList.size(); i++) {
                CompoundTag slotTag = chipsList.getCompound(i);
                int idx = slotTag.getInt("Slot");
                if (idx >= 0 && idx < 5 && slotTag.contains("Item")) {
                    ItemStack st = ItemStack.of(slotTag.getCompound("Item"));
                    if (!st.isEmpty()) EquipmentStatsHelper.ensureStats(st);
                    this.equippedChips[idx] = st;
                }
            }
        }
    }
}
