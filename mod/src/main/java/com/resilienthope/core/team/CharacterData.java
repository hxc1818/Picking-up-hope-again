package com.resilienthope.core.team;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

public class CharacterData {
    private String name;
    private float baseMaxHp;
    private float currentHp;
    private float maxEnergy;
    private float currentEnergy;
    private int eCooldownTicks;
    private int level;
    private boolean unlocked;

    private ItemStack equippedWeapon;
    private final ItemStack[] equippedChips = new ItemStack[5];

    public CharacterData(String name, float baseMaxHp, float maxEnergy, boolean unlocked) {
        this.name = name;
        this.baseMaxHp = baseMaxHp;
        this.currentHp = baseMaxHp;
        this.maxEnergy = maxEnergy;
        this.currentEnergy = 0;
        this.eCooldownTicks = 0;
        this.level = 20;
        this.unlocked = unlocked;
        this.equippedWeapon = unlocked ? new ItemStack(ModItems.PLASMA_CUTTER.get()) : ItemStack.EMPTY;
        if (!this.equippedWeapon.isEmpty()) {
            EquipmentStatsHelper.ensureStats(this.equippedWeapon);
        }
        for (int i = 0; i < 5; i++) {
            this.equippedChips[i] = ItemStack.EMPTY;
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(80, level)); }

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

    public float getMaxHp() {
        return getTotalMaxHp();
    }

    public void setMaxHp(float baseMaxHp) {
        this.baseMaxHp = baseMaxHp;
    }

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
        float effective = delta > 0 ? delta * (getEnergyRecharge() / 100.0f) : delta;
        this.currentEnergy = Math.max(0, Math.min(this.currentEnergy + effective, this.maxEnergy));
    }

    public int getECooldownTicks() { return eCooldownTicks; }
    public void setECooldownTicks(int ticks) { this.eCooldownTicks = Math.max(0, ticks); }

    // =====================================================
    // Dynamic RPG Attribute Aggregation (Weapon + 5 Chips)
    // =====================================================

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
        float flat = sumStat(EquipmentStatsHelper.MAIN_HP_FLAT, EquipmentStatsHelper.TAG_SUB_HP_FLAT);
        return baseMaxHp + flat;
    }

    public float getBaseAtk() {
        float charBase = 210.0f + level * 6.0f;
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
        return 460.0f + level * 11.0f + enhanceSum * 4.5f;
    }

    public float getCritRate() {
        float bonus = sumStat(EquipmentStatsHelper.MAIN_CRIT_RATE, EquipmentStatsHelper.TAG_SUB_CRIT_RATE);
        return Math.min(100.0f, 12.0f + bonus);
    }

    public float getCritDmg() {
        float bonus = sumStat(EquipmentStatsHelper.MAIN_CRIT_DMG, EquipmentStatsHelper.TAG_SUB_CRIT_DMG);
        return 60.0f + bonus;
    }

    public float getEnergyRecharge() {
        float bonus = sumStat(EquipmentStatsHelper.MAIN_ER_PCT, EquipmentStatsHelper.TAG_SUB_ER_PCT);
        return 100.0f + bonus;
    }

    public float getPlasmaDmgBonus() {
        float bonus = sumStat(EquipmentStatsHelper.MAIN_PLASMA_DMG, null);
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

    /**
     * Computes scaled Minecraft damage from character's real stats and skill multiplier.
     */
    public DamageResult computeDamage(float skillMultiplier, Random rng) {
        float atk = getTotalAtk();
        float dmgBonusMult = 1.0f + getPlasmaDmgBonus() / 100.0f;
        boolean isCrit = rng.nextFloat() * 100.0f < getCritRate();
        float critMult = isCrit ? (1.0f + getCritDmg() / 100.0f) : 1.0f;
        // Scale down from ~1000 ATK RPG numbers to MC 20-450 HP range
        float rawMcDamage = (atk / 45.0f) * skillMultiplier * dmgBonusMult * critMult;
        int displayDamage = Math.max(1, Math.round(atk * skillMultiplier * dmgBonusMult * critMult * 0.65f));
        return new DamageResult(Math.max(2.0f, rawMcDamage), displayDamage, isCrit);
    }

    public record DamageResult(float mcDamage, int displayDamage, boolean isCrit) {}

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putFloat("BaseMaxHp", baseMaxHp);
        tag.putFloat("CurrentHp", currentHp);
        tag.putFloat("MaxEnergy", maxEnergy);
        tag.putFloat("CurrentEnergy", currentEnergy);
        tag.putInt("ECooldownTicks", eCooldownTicks);
        tag.putInt("Level", level);
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
        if (tag.contains("Name")) this.name = tag.getString("Name");
        if (tag.contains("BaseMaxHp")) {
            this.baseMaxHp = tag.getFloat("BaseMaxHp");
        } else if (tag.contains("MaxHp")) {
            this.baseMaxHp = tag.getFloat("MaxHp");
        }
        this.currentHp = tag.getFloat("CurrentHp");
        if (tag.contains("MaxEnergy") && tag.getFloat("MaxEnergy") > 0) {
            this.maxEnergy = tag.getFloat("MaxEnergy");
        }
        this.currentEnergy = tag.getFloat("CurrentEnergy");
        this.eCooldownTicks = tag.getInt("ECooldownTicks");
        if (tag.contains("Level")) this.level = Math.max(1, tag.getInt("Level"));
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
