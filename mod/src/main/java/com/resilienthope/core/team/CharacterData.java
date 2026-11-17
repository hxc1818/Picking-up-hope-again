package com.resilienthope.core.team;

import net.minecraft.nbt.CompoundTag;

public class CharacterData {
    private String name;
    private float maxHp;
    private float currentHp;
    private float maxEnergy;
    private float currentEnergy;
    private int eCooldownTicks;

    public CharacterData(String name, float maxHp, float maxEnergy) {
        this.name = name;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.maxEnergy = maxEnergy;
        this.currentEnergy = 0;
        this.eCooldownTicks = 0;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public float getMaxHp() { return maxHp; }
    public void setMaxHp(float maxHp) { this.maxHp = maxHp; }

    public float getCurrentHp() { return currentHp; }
    public void setCurrentHp(float currentHp) { this.currentHp = Math.max(0, Math.min(currentHp, maxHp)); }

    public float getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(float maxEnergy) { this.maxEnergy = maxEnergy; }

    public float getCurrentEnergy() { return currentEnergy; }
    public void setCurrentEnergy(float currentEnergy) { this.currentEnergy = Math.max(0, Math.min(currentEnergy, maxEnergy)); }
    public void addEnergy(float delta) { this.currentEnergy = Math.max(0, Math.min(this.currentEnergy + delta, this.maxEnergy)); }

    public int getECooldownTicks() { return eCooldownTicks; }
    public void setECooldownTicks(int ticks) { this.eCooldownTicks = Math.max(0, ticks); }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putFloat("MaxHp", maxHp);
        tag.putFloat("CurrentHp", currentHp);
        tag.putFloat("MaxEnergy", maxEnergy);
        tag.putFloat("CurrentEnergy", currentEnergy);
        tag.putInt("ECooldownTicks", eCooldownTicks);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.name = tag.getString("Name");
        this.maxHp = tag.getFloat("MaxHp");
        this.currentHp = tag.getFloat("CurrentHp");
        this.maxEnergy = tag.getFloat("MaxEnergy");
        this.currentEnergy = tag.getFloat("CurrentEnergy");
        this.eCooldownTicks = tag.getInt("ECooldownTicks");
    }
}
