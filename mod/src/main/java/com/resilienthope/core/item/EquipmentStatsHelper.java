package com.resilienthope.core.item;

import com.resilienthope.core.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper for v2.0 weapon (Lv 1-80 with ascension) and equipment chip (Tier 2-5, max Lv 9/12/15/18, +3 substat).
 */
public class EquipmentStatsHelper {
    public static final String TAG_INITIALIZED = "HopeInit";
    public static final String TAG_RARITY = "Rarity";
    public static final String TAG_ENHANCE_LV = "EnhanceLv";
    public static final String TAG_ASCENSION = "Ascension";
    public static final String TAG_MAIN_TYPE = "MainType";
    public static final String TAG_MAIN_VAL = "MainVal";

    public static final String TAG_SUB_CRIT_RATE = "SubCritRate";
    public static final String TAG_SUB_CRIT_DMG = "SubCritDmg";
    public static final String TAG_SUB_ATK_PCT = "SubAtkPct";
    public static final String TAG_SUB_SPD = "SubSpd";
    public static final String TAG_SUB_HP_FLAT = "SubHpFlat";

    public static final String MAIN_HP_FLAT = "HP_FLAT";
    public static final String MAIN_ATK_FLAT = "ATK_FLAT";
    public static final String MAIN_ATK_PCT = "ATK_PCT";
    public static final String MAIN_ELEM_DMG = "ELEM_DMG";
    public static final String MAIN_CRIT_RATE = "CRIT_RATE";
    public static final String MAIN_CRIT_DMG = "CRIT_DMG";
    public static final String MAIN_SPD = "SPD";
    public static final String MAIN_WEAPON_ATK = "WEAPON_ATK";

    private static final String[] ALL_SUB_KEYS = {
            TAG_SUB_CRIT_RATE, TAG_SUB_CRIT_DMG, TAG_SUB_ATK_PCT, TAG_SUB_SPD, TAG_SUB_HP_FLAT
    };

    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.PLASMA_CUTTER.get()
                || item == ModItems.CRYO_SAWBLADE.get()
                || item == ModItems.HYDRO_PULSE_GUN.get()
                || item == ModItems.AERO_BLADE.get()
                || item == ModItems.PRECURSOR_CLAYMORE.get();
    }

    public static int getChipSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        Item item = stack.getItem();
        if (item == ModItems.FUSION_CORE_CHIP.get()) return 0;
        if (item == ModItems.VOID_DRIVE_CHIP.get()) return 1;
        if (item == ModItems.SERVO_BATTERY_CHIP.get()) return 2;
        if (item == ModItems.OVERCLOCK_CHIP.get()) return 3;
        if (item == ModItems.NEURAL_HALO_CHIP.get()) return 4;
        return -1;
    }

    public static boolean isChip(ItemStack stack) {
        return getChipSlot(stack) >= 0;
    }

    public static String getChipSlotTitle(int slot) {
        return switch (slot) {
            case 0 -> "槽位 1 · 生命模块";
            case 1 -> "槽位 2 · 攻击模块";
            case 2 -> "槽位 3 · 能源电池";
            case 3 -> "槽位 4 · 属性增幅";
            case 4 -> "槽位 5 · 战术核心";
            default -> "未知槽位";
        };
    }

    public static int getDefaultRarity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 2;
        Item item = stack.getItem();
        if (item == ModItems.PRECURSOR_CLAYMORE.get() || item == ModItems.NEURAL_HALO_CHIP.get()) {
            return 5;
        }
        if (item == ModItems.CRYO_SAWBLADE.get() || item == ModItems.OVERCLOCK_CHIP.get()) {
            return 4;
        }
        if (item == ModItems.HYDRO_PULSE_GUN.get() || item == ModItems.AERO_BLADE.get() || item == ModItems.SERVO_BATTERY_CHIP.get()) {
            return 3;
        }
        return 2;
    }

    public static ItemStack createChipWithRarity(Item chipItem, int rarity, Random rng) {
        ItemStack stack = new ItemStack(chipItem);
        int clamped = Math.max(2, Math.min(5, rarity));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_RARITY, clamped);
        ensureStats(stack, rng, clamped);
        return stack;
    }

    public static void ensureStats(ItemStack stack) {
        ensureStats(stack, new Random(stack.getItem().hashCode() * 31L + 1337L), -1);
    }

    public static void ensureStats(ItemStack stack, Random rng) {
        ensureStats(stack, rng, -1);
    }

    public static void ensureStats(ItemStack stack, Random rng, int forcedRarity) {
        if (stack == null || stack.isEmpty()) return;
        if (!isWeapon(stack) && !isChip(stack)) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(TAG_INITIALIZED)) return;

        int rarity = forcedRarity >= 2 && forcedRarity <= 5
                ? forcedRarity
                : (tag.contains(TAG_RARITY) ? Math.max(2, Math.min(5, tag.getInt(TAG_RARITY))) : getDefaultRarity(stack));

        tag.putBoolean(TAG_INITIALIZED, true);
        tag.putInt(TAG_RARITY, rarity);
        tag.putInt(TAG_ENHANCE_LV, isWeapon(stack) ? 1 : 0);
        tag.putInt(TAG_ASCENSION, 0);

        if (isWeapon(stack)) {
            tag.putString(TAG_MAIN_TYPE, MAIN_WEAPON_ATK);
            if (stack.getItem() == ModItems.PRECURSOR_CLAYMORE.get()) {
                tag.putFloat(TAG_MAIN_VAL, 75.0f);
                tag.putFloat(TAG_SUB_CRIT_DMG, 14.0f);
                tag.putFloat(TAG_SUB_CRIT_RATE, 5.0f);
            } else if (stack.getItem() == ModItems.CRYO_SAWBLADE.get()) {
                tag.putFloat(TAG_MAIN_VAL, 55.0f);
                tag.putFloat(TAG_SUB_ATK_PCT, 10.0f);
                tag.putFloat(TAG_SUB_CRIT_RATE, 4.0f);
            } else if (stack.getItem() == ModItems.HYDRO_PULSE_GUN.get()) {
                tag.putFloat(TAG_MAIN_VAL, 48.0f);
                tag.putFloat(TAG_SUB_SPD, 6.0f);
                tag.putFloat(TAG_SUB_HP_FLAT, 120.0f);
            } else if (stack.getItem() == ModItems.AERO_BLADE.get()) {
                tag.putFloat(TAG_MAIN_VAL, 50.0f);
                tag.putFloat(TAG_SUB_SPD, 8.0f);
                tag.putFloat(TAG_SUB_ATK_PCT, 8.0f);
            } else {
                tag.putFloat(TAG_MAIN_VAL, 42.0f);
                tag.putFloat(TAG_SUB_ATK_PCT, 6.0f);
            }
            return;
        }

        int slot = getChipSlot(stack);
        switch (slot) {
            case 0 -> {
                tag.putString(TAG_MAIN_TYPE, MAIN_HP_FLAT);
                tag.putFloat(TAG_MAIN_VAL, 80.0f + rarity * 40.0f);
            }
            case 1 -> {
                tag.putString(TAG_MAIN_TYPE, MAIN_ATK_FLAT);
                tag.putFloat(TAG_MAIN_VAL, 10.0f + rarity * 6.0f);
            }
            case 2 -> {
                String[] pool = {MAIN_ATK_PCT, MAIN_SPD};
                String chosen = pool[rng.nextInt(pool.length)];
                tag.putString(TAG_MAIN_TYPE, chosen);
                tag.putFloat(TAG_MAIN_VAL, chosen.equals(MAIN_SPD) ? (2.0f + rarity) : (4.0f + rarity * 1.5f));
            }
            case 3 -> {
                String[] pool = {MAIN_ELEM_DMG, MAIN_ATK_PCT};
                String chosen = pool[rng.nextInt(pool.length)];
                tag.putString(TAG_MAIN_TYPE, chosen);
                tag.putFloat(TAG_MAIN_VAL, 4.0f + rarity * 1.5f);
            }
            case 4 -> {
                String[] pool = {MAIN_CRIT_RATE, MAIN_CRIT_DMG};
                String chosen = pool[rng.nextInt(pool.length)];
                tag.putString(TAG_MAIN_TYPE, chosen);
                tag.putFloat(TAG_MAIN_VAL, chosen.equals(MAIN_CRIT_RATE) ? (3.0f + rarity) : (6.0f + rarity * 2.0f));
            }
        }

        // Initial substats count based on rarity (2->1, 3->1, 4->2, 5->3)
        int initSubCount = rarity <= 3 ? 1 : (rarity == 4 ? 2 : 3);
        for (int i = 0; i < initSubCount; i++) {
            unlockOrUpgradeSubstat(tag, rng);
        }
    }

    private static void unlockOrUpgradeSubstat(CompoundTag tag, Random rng) {
        List<String> missing = new ArrayList<>();
        List<String> present = new ArrayList<>();
        for (String key : ALL_SUB_KEYS) {
            if (tag.getFloat(key) <= 0.01f) {
                missing.add(key);
            } else {
                present.add(key);
            }
        }
        String chosenKey;
        if (present.size() < 4 && !missing.isEmpty()) {
            chosenKey = missing.get(rng.nextInt(missing.size()));
        } else if (!present.isEmpty()) {
            chosenKey = present.get(rng.nextInt(present.size()));
        } else {
            chosenKey = ALL_SUB_KEYS[rng.nextInt(ALL_SUB_KEYS.length)];
        }
        float addVal = switch (chosenKey) {
            case TAG_SUB_CRIT_RATE -> 2.4f + rng.nextInt(3) * 0.4f;
            case TAG_SUB_CRIT_DMG -> 4.8f + rng.nextInt(3) * 0.8f;
            case TAG_SUB_ATK_PCT -> 3.5f + rng.nextInt(3) * 0.6f;
            case TAG_SUB_SPD -> 2.0f + rng.nextInt(2);
            default -> 45.0f + rng.nextInt(3) * 15.0f;
        };
        tag.putFloat(chosenKey, tag.getFloat(chosenKey) + addVal);
    }

    public static int getRarity(ItemStack stack) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        return tag != null ? Math.max(2, Math.min(5, tag.getInt(TAG_RARITY))) : getDefaultRarity(stack);
    }

    public static int getEnhanceLevel(ItemStack stack) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_ENHANCE_LV) : (isWeapon(stack) ? 1 : 0);
    }

    public static int getAscension(ItemStack stack) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        return tag != null ? Math.max(0, Math.min(6, tag.getInt(TAG_ASCENSION))) : 0;
    }

    public static void setAscension(ItemStack stack, int asc) {
        ensureStats(stack);
        stack.getOrCreateTag().putInt(TAG_ASCENSION, Math.max(0, Math.min(6, asc)));
    }

    /**
     * Tier 2 chip -> max 9, Tier 3 -> 12, Tier 4 -> 15, Tier 5 -> 18.
     * Weapon -> 20 + ascension * 10 (up to 80).
     */
    public static int getMaxEnhanceLevel(ItemStack stack) {
        if (isWeapon(stack)) {
            return 20 + getAscension(stack) * 10;
        }
        if (isChip(stack)) {
            int r = getRarity(stack);
            return 9 + (r - 2) * 3;
        }
        return 0;
    }

    public static int getDissolveEssenceYield(ItemStack stack) {
        if (!isChip(stack)) return 0;
        int r = getRarity(stack);
        int lv = getEnhanceLevel(stack);
        int base = switch (r) {
            case 5 -> 8;
            case 4 -> 4;
            case 3 -> 2;
            default -> 1;
        };
        return base + lv;
    }

    public static int getEnhanceDustCost(ItemStack stack) {
        if (isChip(stack)) {
            int lv = getEnhanceLevel(stack);
            return 1 + (lv / 3);
        }
        return 1;
    }

    public static String getMainType(ItemStack stack) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_MAIN_TYPE) : MAIN_ATK_FLAT;
    }

    public static float getMainValue(ItemStack stack) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0f;
        float base = tag.getFloat(TAG_MAIN_VAL);
        int lv = tag.getInt(TAG_ENHANCE_LV);
        int r = getRarity(stack);
        String type = tag.getString(TAG_MAIN_TYPE);
        if (MAIN_WEAPON_ATK.equals(type)) {
            int asc = tag.getInt(TAG_ASCENSION);
            float perLv = 3.5f + r * 0.8f;
            return base + (lv - 1) * perLv + asc * 16.0f;
        }
        return switch (type) {
            case MAIN_HP_FLAT -> base + lv * (24.0f + r * 6.0f);
            case MAIN_ATK_FLAT -> base + lv * (4.0f + r * 1.2f);
            case MAIN_ATK_PCT, MAIN_ELEM_DMG -> base + lv * (0.8f + r * 0.25f);
            case MAIN_SPD -> base + lv * 0.8f;
            case MAIN_CRIT_RATE -> base + lv * (0.6f + r * 0.15f);
            case MAIN_CRIT_DMG -> base + lv * (1.2f + r * 0.3f);
            default -> base + lv * 1.0f;
        };
    }

    /**
     * Enhances chip (+1, max 9/12/15/18, unlocks/upgrades substat every +3) or weapon (+1, up to ascension cap).
     */
    public static boolean enhanceByOne(ItemStack stack, Random rng) {
        if (!isWeapon(stack) && !isChip(stack)) return false;
        ensureStats(stack, rng);
        CompoundTag tag = stack.getOrCreateTag();
        int lv = tag.getInt(TAG_ENHANCE_LV);
        int maxLv = getMaxEnhanceLevel(stack);
        if (lv >= maxLv) return false;
        int nextLv = lv + 1;
        tag.putInt(TAG_ENHANCE_LV, nextLv);

        if (isChip(stack) && nextLv % 3 == 0) {
            unlockOrUpgradeSubstat(tag, rng);
        }
        return true;
    }

    public static float getSubValue(ItemStack stack, String key) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getFloat(key) : 0f;
    }

    public static String formatMainStatLabel(ItemStack stack) {
        String type = getMainType(stack);
        float val = getMainValue(stack);
        return switch (type) {
            case MAIN_WEAPON_ATK -> String.format("武器攻击力 +%.0f", val);
            case MAIN_HP_FLAT -> String.format("生命值 +%.0f", val);
            case MAIN_ATK_FLAT -> String.format("攻击力 +%.0f", val);
            case MAIN_ATK_PCT -> String.format("攻击力 +%.1f%%", val);
            case MAIN_ELEM_DMG -> String.format("属性伤害加成 +%.1f%%", val);
            case MAIN_CRIT_RATE -> String.format("暴击率 +%.1f%%", val);
            case MAIN_CRIT_DMG -> String.format("暴击伤害 +%.1f%%", val);
            case MAIN_SPD -> String.format("速度 +%.0f", val);
            default -> String.format("属性 +%.1f", val);
        };
    }

    public static List<String> getSubStatLines(ItemStack stack) {
        ensureStats(stack);
        List<String> lines = new ArrayList<>();
        float cr = getSubValue(stack, TAG_SUB_CRIT_RATE);
        float cd = getSubValue(stack, TAG_SUB_CRIT_DMG);
        float ap = getSubValue(stack, TAG_SUB_ATK_PCT);
        float spd = getSubValue(stack, TAG_SUB_SPD);
        float hp = getSubValue(stack, TAG_SUB_HP_FLAT);
        if (cr > 0.01f) lines.add(String.format("暴击率 +%.1f%%", cr));
        if (cd > 0.01f) lines.add(String.format("暴击伤害 +%.1f%%", cd));
        if (ap > 0.01f) lines.add(String.format("攻击力 +%.1f%%", ap));
        if (spd > 0.01f) lines.add(String.format("速度 +%.0f", spd));
        if (hp > 0.01f) lines.add(String.format("生命值 +%.0f", hp));
        return lines;
    }

    public static String getStarString(int rarity) {
        return switch (rarity) {
            case 5 -> "★★★★★";
            case 4 -> "★★★★";
            case 3 -> "★★★";
            default -> "★★";
        };
    }

    public static int getRarityColor(int rarity) {
        return switch (rarity) {
            case 5 -> 0xFFFBBF24; // Gold
            case 4 -> 0xFFC084FC; // Purple
            case 3 -> 0xFF38BDF8; // Blue
            default -> 0xFF4ADE80; // Green (Tier 2)
        };
    }

    public static String getWeaponElementLabel(ItemStack stack) {
        if (stack.isEmpty()) return "无";
        if (stack.getItem() == ModItems.HYDRO_PULSE_GUN.get()) return "水";
        if (stack.getItem() == ModItems.AERO_BLADE.get()) return "气";
        if (stack.getItem() == ModItems.CRYO_SAWBLADE.get()) return "火";
        if (stack.getItem() == ModItems.PRECURSOR_CLAYMORE.get()) return "全属性";
        return "地";
    }

    public static String getWeaponPassiveDesc(ItemStack stack) {
        if (stack.isEmpty()) return "";
        if (stack.getItem() == ModItems.PRECURSOR_CLAYMORE.get()) {
            return "武器效果：所有技能伤害提升 20%，弱点击破伤害额外提升 25%";
        }
        if (stack.getItem() == ModItems.CRYO_SAWBLADE.get()) {
            return "武器效果：火属性技能伤害提升 15%，受击灼烧伤害加深";
        }
        if (stack.getItem() == ModItems.HYDRO_PULSE_GUN.get()) {
            return "武器效果：治疗量或水属性技能效果提升 15%";
        }
        if (stack.getItem() == ModItems.AERO_BLADE.get()) {
            return "武器效果：释放战技后自身速度额外提升 10 点";
        }
        return "武器效果：常规作业工具改装，普攻伤害稳定提升 10%";
    }
}
