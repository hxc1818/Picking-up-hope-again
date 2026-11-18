package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.item.TechChipItem;
import com.resilienthope.core.item.TechWeaponItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PickingHopeCore.MOD_ID);

    // Block items
    public static final RegistryObject<Item> TELEPORT_WAYPOINT = ITEMS.register("teleport_waypoint",
            () -> new BlockItem(ModBlocks.TELEPORT_WAYPOINT.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> ENGINEERING_TERMINAL = ITEMS.register("engineering_terminal",
            () -> new BlockItem(ModBlocks.ENGINEERING_TERMINAL.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> AUXILIARY_ENERGY_CORE = ITEMS.register("auxiliary_energy_core",
            () -> new BlockItem(ModBlocks.AUXILIARY_ENERGY_CORE.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> ORBITAL_DROP_POD = ITEMS.register("orbital_drop_pod",
            () -> new BlockItem(ModBlocks.ORBITAL_DROP_POD.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> PRECURSOR_ALTAR = ITEMS.register("precursor_altar",
            () -> new BlockItem(ModBlocks.PRECURSOR_ALTAR.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    // Legacy & Base Valuables / Consumables
    public static final RegistryObject<Item> QUANTUM_CORE = ITEMS.register("quantum_core",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> ALLOY_PLATE = ITEMS.register("alloy_plate",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> PRECURSOR_SPARK = ITEMS.register("precursor_spark",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(16)));

    // v2.0 Currencies & Progression Consumables
    public static final RegistryObject<Item> CREDIT_COIN = ITEMS.register("credit_coin",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> EXP_BOTTLE = ITEMS.register("exp_bottle",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> EQUIP_ESSENCE = ITEMS.register("equip_essence",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> STAR_TICKET = ITEMS.register("star_ticket",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    // Character Ascension Materials (Tier 1..6)
    public static final RegistryObject<Item> CHAR_MAT_1 = registerMat("char_mat_1");
    public static final RegistryObject<Item> CHAR_MAT_2 = registerMat("char_mat_2");
    public static final RegistryObject<Item> CHAR_MAT_3 = registerMat("char_mat_3");
    public static final RegistryObject<Item> CHAR_MAT_4 = registerMat("char_mat_4");
    public static final RegistryObject<Item> CHAR_MAT_5 = registerMat("char_mat_5");
    public static final RegistryObject<Item> CHAR_MAT_6 = registerMat("char_mat_6");

    // Weapon Ascension Materials (Tier 1..6)
    public static final RegistryObject<Item> WEAPON_MAT_1 = registerMat("weapon_mat_1");
    public static final RegistryObject<Item> WEAPON_MAT_2 = registerMat("weapon_mat_2");
    public static final RegistryObject<Item> WEAPON_MAT_3 = registerMat("weapon_mat_3");
    public static final RegistryObject<Item> WEAPON_MAT_4 = registerMat("weapon_mat_4");
    public static final RegistryObject<Item> WEAPON_MAT_5 = registerMat("weapon_mat_5");
    public static final RegistryObject<Item> WEAPON_MAT_6 = registerMat("weapon_mat_6");

    // Talent Upgrade Materials (Tier 1..6)
    public static final RegistryObject<Item> TALENT_MAT_1 = registerMat("talent_mat_1");
    public static final RegistryObject<Item> TALENT_MAT_2 = registerMat("talent_mat_2");
    public static final RegistryObject<Item> TALENT_MAT_3 = registerMat("talent_mat_3");
    public static final RegistryObject<Item> TALENT_MAT_4 = registerMat("talent_mat_4");
    public static final RegistryObject<Item> TALENT_MAT_5 = registerMat("talent_mat_5");
    public static final RegistryObject<Item> TALENT_MAT_6 = registerMat("talent_mat_6");

    private static RegistryObject<Item> registerMat(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));
    }

    // Weapons (Earth, Fire, Water, Air, 5-star Heavy)
    public static final RegistryObject<Item> PLASMA_CUTTER = ITEMS.register("plasma_cutter",
            () -> new TechWeaponItem(4, -2.4f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> CRYO_SAWBLADE = ITEMS.register("cryo_sawblade",
            () -> new TechWeaponItem(5, -2.3f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> HYDRO_PULSE_GUN = ITEMS.register("hydro_pulse_gun",
            () -> new TechWeaponItem(4, -2.2f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> AERO_BLADE = ITEMS.register("aero_blade",
            () -> new TechWeaponItem(4, -2.1f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> PRECURSOR_CLAYMORE = ITEMS.register("precursor_claymore",
            () -> new TechWeaponItem(8, -2.2f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    // 5-Slot Equipment Chips
    public static final RegistryObject<Item> FUSION_CORE_CHIP = ITEMS.register("fusion_core_chip",
            () -> new TechChipItem(0, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> VOID_DRIVE_CHIP = ITEMS.register("void_drive_chip",
            () -> new TechChipItem(1, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> SERVO_BATTERY_CHIP = ITEMS.register("servo_battery_chip",
            () -> new TechChipItem(2, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> OVERCLOCK_CHIP = ITEMS.register("overclock_chip",
            () -> new TechChipItem(3, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> NEURAL_HALO_CHIP = ITEMS.register("neural_halo_chip",
            () -> new TechChipItem(4, new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    // Material Tier Helpers (1..6)
    public static Item getCharMat(int tier) {
        return switch (tier) {
            case 2 -> CHAR_MAT_2.get();
            case 3 -> CHAR_MAT_3.get();
            case 4 -> CHAR_MAT_4.get();
            case 5 -> CHAR_MAT_5.get();
            case 6 -> CHAR_MAT_6.get();
            default -> CHAR_MAT_1.get();
        };
    }

    public static Item getWeaponMat(int tier) {
        return switch (tier) {
            case 2 -> WEAPON_MAT_2.get();
            case 3 -> WEAPON_MAT_3.get();
            case 4 -> WEAPON_MAT_4.get();
            case 5 -> WEAPON_MAT_5.get();
            case 6 -> WEAPON_MAT_6.get();
            default -> WEAPON_MAT_1.get();
        };
    }

    public static Item getTalentMat(int tier) {
        return switch (tier) {
            case 2 -> TALENT_MAT_2.get();
            case 3 -> TALENT_MAT_3.get();
            case 4 -> TALENT_MAT_4.get();
            case 5 -> TALENT_MAT_5.get();
            case 6 -> TALENT_MAT_6.get();
            default -> TALENT_MAT_1.get();
        };
    }

    public static String getMaterialLine(Item item) {
        for (int t = 1; t <= 6; t++) {
            if (item == getCharMat(t)) return "char";
            if (item == getWeaponMat(t)) return "weapon";
            if (item == getTalentMat(t)) return "talent";
        }
        return null;
    }

    public static int getMaterialTier(Item item) {
        for (int t = 1; t <= 6; t++) {
            if (item == getCharMat(t) || item == getWeaponMat(t) || item == getTalentMat(t)) {
                return t;
            }
        }
        return 0;
    }

    public static Item getNextTierMaterial(Item item) {
        String line = getMaterialLine(item);
        int tier = getMaterialTier(item);
        if (line == null || tier <= 0 || tier >= 6) return null;
        return switch (line) {
            case "char" -> getCharMat(tier + 1);
            case "weapon" -> getWeaponMat(tier + 1);
            case "talent" -> getTalentMat(tier + 1);
            default -> null;
        };
    }

    public static boolean isMaterialItem(Item item) {
        return getMaterialLine(item) != null
                || item == ALLOY_PLATE.get()
                || item == EQUIP_ESSENCE.get()
                || item == EXP_BOTTLE.get();
    }
}
