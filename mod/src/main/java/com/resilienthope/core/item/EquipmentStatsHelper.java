package com.resilienthope.core.item;

import com.resilienthope.core.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper for reading, initializing, and upgrading dynamic NBT stats on weapons and 5-slot tech chips.
 */
public class EquipmentStatsHelper {
    public static final String TAG_INITIALIZED = "HopeInit";
    public static final String TAG_RARITY = "Rarity";
    public static final String TAG_ENHANCE_LV = "EnhanceLv";
    public static final String TAG_MAIN_TYPE = "MainType";
    public static final String TAG_MAIN_VAL = "MainVal";
    public static final String TAG_SUB_CRIT_RATE = "SubCritRate";
    public static final String TAG_SUB_CRIT_DMG = "SubCritDmg";
    public static final String TAG_SUB_ATK_PCT = "SubAtkPct";
    public static final String TAG_SUB_ER_PCT = "SubErPct";
    public static final String TAG_SUB_HP_FLAT = "SubHpFlat";

    public static final String MAIN_HP_FLAT = "HP_FLAT";
    public static final String MAIN_ATK_FLAT = "ATK_FLAT";
    public static final String MAIN_ATK_PCT = "ATK_PCT";
    public static final String MAIN_PLASMA_DMG = "PLASMA_DMG";
    public static final String MAIN_CRIT_RATE = "CRIT_RATE";
    public static final String MAIN_CRIT_DMG = "CRIT_DMG";
    public static final String MAIN_ER_PCT = "ER_PCT";
    public static final String MAIN_WEAPON_ATK = "WEAPON_ATK";

    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.PLASMA_CUTTER.get()
                || item == ModItems.CRYO_SAWBLADE.get()
                || item == ModItems.PRECURSOR_CLAYMORE.get();
    }

    /**
     * Returns 0..4 for chip slot 1..5, or -1 if not a tech chip.
     */
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
            case 0 -> "槽位 1 · 控制核心";
            case 1 -> "槽位 2 · 动力发生器";
            case 2 -> "槽位 3 · 能源电池";
            case 3 -> "槽位 4 · 超频芯片";
            case 4 -> "槽位 5 · 神经感知环";
            default -> "未知插槽";
        };
    }

    public static int getDefaultRarity(ItemStack stack) {
        if (stack.isEmpty()) return 3;
        Item item = stack.getItem();
        if (item == ModItems.PRECURSOR_CLAYMORE.get()
                || item == ModItems.OVERCLOCK_CHIP.get()
                || item == ModItems.NEURAL_HALO_CHIP.get()) {
            return 5;
        }
        return 4;
    }

    public static void ensureStats(ItemStack stack) {
        ensureStats(stack, new Random(stack.getItem().hashCode() * 31L + 1337L));
    }

    public static void ensureStats(ItemStack stack, Random rng) {
        if (stack == null || stack.isEmpty()) return;
        if (!isWeapon(stack) && !isChip(stack)) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(TAG_INITIALIZED)) return;

        int rarity = getDefaultRarity(stack);
        tag.putBoolean(TAG_INITIALIZED, true);
        tag.putInt(TAG_RARITY, rarity);
        tag.putInt(TAG_ENHANCE_LV, 0);

        if (isWeapon(stack)) {
            tag.putString(TAG_MAIN_TYPE, MAIN_WEAPON_ATK);
            if (stack.getItem() == ModItems.PRECURSOR_CLAYMORE.get()) {
                tag.putFloat(TAG_MAIN_VAL, 260.0f);
                tag.putFloat(TAG_SUB_CRIT_DMG, 24.0f);
                tag.putFloat(TAG_SUB_CRIT_RATE, 8.0f);
            } else if (stack.getItem() == ModItems.CRYO_SAWBLADE.get()) {
                tag.putFloat(TAG_MAIN_VAL, 175.0f);
                tag.putFloat(TAG_SUB_ATK_PCT, 18.0f);
                tag.putFloat(TAG_SUB_CRIT_RATE, 6.0f);
            } else {
                tag.putFloat(TAG_MAIN_VAL, 150.0f);
                tag.putFloat(TAG_SUB_ATK_PCT, 14.0f);
                tag.putFloat(TAG_SUB_ER_PCT, 12.0f);
            }
            return;
        }

        int slot = getChipSlot(stack);
        switch (slot) {
            case 0 -> {
                tag.putString(TAG_MAIN_TYPE, MAIN_HP_FLAT);
                tag.putFloat(TAG_MAIN_VAL, rarity == 5 ? 717.0f : 560.0f);
            }
            case 1 -> {
                tag.putString(TAG_MAIN_TYPE, MAIN_ATK_FLAT);
                tag.putFloat(TAG_MAIN_VAL, rarity == 5 ? 47.0f : 38.0f);
            }
            case 2 -> {
                String[] pool = {MAIN_ATK_PCT, MAIN_ER_PCT};
                String chosen = pool[rng.nextInt(pool.length)];
                tag.putString(TAG_MAIN_TYPE, chosen);
                tag.putFloat(TAG_MAIN_VAL, chosen.equals(MAIN_ER_PCT) ? 14.0f : 12.5f);
            }
            case 3 -> {
                String[] pool = {MAIN_PLASMA_DMG, MAIN_ATK_PCT};
                String chosen = pool[rng.nextInt(pool.length)];
                tag.putString(TAG_MAIN_TYPE, chosen);
                tag.putFloat(TAG_MAIN_VAL, 12.5f);
            }
            case 4 -> {
                String[] pool = {MAIN_CRIT_RATE, MAIN_CRIT_DMG};
                String chosen = pool[rng.nextInt(pool.length)];
                tag.putString(TAG_MAIN_TYPE, chosen);
                tag.putFloat(TAG_MAIN_VAL, chosen.equals(MAIN_CRIT_RATE) ? 9.5f : 19.0f);
            }
        }

        // Roll 3 initial substats
        float[] critRates = {2.7f, 3.1f, 3.5f, 3.9f};
        float[] critDmgs = {5.4f, 6.2f, 7.0f, 7.8f};
        float[] atkPcts = {4.1f, 4.7f, 5.3f, 5.8f};
        float[] erPcts = {4.5f, 5.2f, 5.8f, 6.5f};

        tag.putFloat(TAG_SUB_CRIT_RATE, critRates[rng.nextInt(critRates.length)]);
        tag.putFloat(TAG_SUB_CRIT_DMG, critDmgs[rng.nextInt(critDmgs.length)]);
        if (rng.nextBoolean()) {
            tag.putFloat(TAG_SUB_ATK_PCT, atkPcts[rng.nextInt(atkPcts.length)]);
        } else {
            tag.putFloat(TAG_SUB_ER_PCT, erPcts[rng.nextInt(erPcts.length)]);
        }
        if (rarity == 5) {
            tag.putFloat(TAG_SUB_HP_FLAT, 209.0f + rng.nextInt(90));
        }
    }

    public static int getRarity(ItemStack stack) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        return tag != null ? Math.max(3, tag.getInt(TAG_RARITY)) : getDefaultRarity(stack);
    }

    public static int getEnhanceLevel(ItemStack stack) {
        ensureStats(stack);
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_ENHANCE_LV) : 0;
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
        String type = tag.getString(TAG_MAIN_TYPE);
        if (MAIN_WEAPON_ATK.equals(type)) {
            return base + lv * (getRarity(stack) == 5 ? 24.0f : 16.0f);
        }
        return switch (type) {
            case MAIN_HP_FLAT -> base + lv * 204.0f;
            case MAIN_ATK_FLAT -> base + lv * 13.2f;
            case MAIN_ATK_PCT, MAIN_PLASMA_DMG -> base + lv * 1.7f;
            case MAIN_ER_PCT -> base + lv * 1.9f;
            case MAIN_CRIT_RATE -> base + lv * 1.08f;
            case MAIN_CRIT_DMG -> base + lv * 2.16f;
            default -> base + lv * 1.5f;
        };
    }

    public static boolean enhanceByOne(ItemStack stack, Random rng) {
        if (!isWeapon(stack) && !isChip(stack)) return false;
        ensureStats(stack, rng);
        CompoundTag tag = stack.getOrCreateTag();
        int lv = tag.getInt(TAG_ENHANCE_LV);
        if (lv >= 20) return false;
        int nextLv = lv + 1;
        tag.putInt(TAG_ENHANCE_LV, nextLv);

        if (isWeapon(stack)) {
            if (nextLv % 4 == 0) {
                if (tag.contains(TAG_SUB_CRIT_DMG)) {
                    tag.putFloat(TAG_SUB_CRIT_DMG, tag.getFloat(TAG_SUB_CRIT_DMG) + 4.5f);
                }
                if (tag.contains(TAG_SUB_ATK_PCT)) {
                    tag.putFloat(TAG_SUB_ATK_PCT, tag.getFloat(TAG_SUB_ATK_PCT) + 3.5f);
                }
            }
        } else if (nextLv % 4 == 0) {
            int roll = rng.nextInt(4);
            switch (roll) {
                case 0 -> tag.putFloat(TAG_SUB_CRIT_RATE, tag.getFloat(TAG_SUB_CRIT_RATE) + 3.1f);
                case 1 -> tag.putFloat(TAG_SUB_CRIT_DMG, tag.getFloat(TAG_SUB_CRIT_DMG) + 6.2f);
                case 2 -> tag.putFloat(TAG_SUB_ATK_PCT, tag.getFloat(TAG_SUB_ATK_PCT) + 4.7f);
                default -> tag.putFloat(TAG_SUB_ER_PCT, tag.getFloat(TAG_SUB_ER_PCT) + 5.2f);
            }
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
            case MAIN_WEAPON_ATK -> String.format("基础攻击力 +%.0f", val);
            case MAIN_HP_FLAT -> String.format("生命值 +%.0f", val);
            case MAIN_ATK_FLAT -> String.format("攻击力 +%.0f", val);
            case MAIN_ATK_PCT -> String.format("攻击力 +%.1f%%", val);
            case MAIN_PLASMA_DMG -> String.format("电浆伤害加成 +%.1f%%", val);
            case MAIN_CRIT_RATE -> String.format("暴击率 +%.1f%%", val);
            case MAIN_CRIT_DMG -> String.format("暴击伤害 +%.1f%%", val);
            case MAIN_ER_PCT -> String.format("聚变充能效率 +%.1f%%", val);
            default -> String.format("属性 +%.1f", val);
        };
    }

    public static List<String> getSubStatLines(ItemStack stack) {
        ensureStats(stack);
        List<String> lines = new ArrayList<>();
        float cr = getSubValue(stack, TAG_SUB_CRIT_RATE);
        float cd = getSubValue(stack, TAG_SUB_CRIT_DMG);
        float ap = getSubValue(stack, TAG_SUB_ATK_PCT);
        float er = getSubValue(stack, TAG_SUB_ER_PCT);
        float hp = getSubValue(stack, TAG_SUB_HP_FLAT);
        if (cr > 0.01f) lines.add(String.format("暴击率 +%.1f%%", cr));
        if (cd > 0.01f) lines.add(String.format("暴击伤害 +%.1f%%", cd));
        if (ap > 0.01f) lines.add(String.format("攻击力 +%.1f%%", ap));
        if (er > 0.01f) lines.add(String.format("充能效率 +%.1f%%", er));
        if (hp > 0.01f) lines.add(String.format("固定生命 +%.0f", hp));
        return lines;
    }

    public static String getStarString(int rarity) {
        return rarity >= 5 ? "★★★★★" : (rarity == 4 ? "★★★★" : "★★★");
    }

    public static int getRarityColor(int rarity) {
        return rarity >= 5 ? 0xFFFFB732 : (rarity == 4 ? 0xFFBB77FF : 0xFF55BBFF);
    }

    public static String getWeaponPassiveDesc(ItemStack stack) {
        if (stack.isEmpty()) return "";
        if (stack.getItem() == ModItems.PRECURSOR_CLAYMORE.get()) {
            return "先驱超导：战技与爆发造成额外 35% 电浆裂变伤害";
        }
        if (stack.getItem() == ModItems.CRYO_SAWBLADE.get()) {
            return "高频寒刃：攻击命中使目标减速并回复 4 点爆发能量";
        }
        return "电浆过载：普攻与战技附加稳定电浆灼烧打击";
    }
}
