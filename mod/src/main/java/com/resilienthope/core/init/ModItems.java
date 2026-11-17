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

    // Materials & Key Valuables
    public static final RegistryObject<Item> QUANTUM_CORE = ITEMS.register("quantum_core",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> ALLOY_PLATE = ITEMS.register("alloy_plate",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> PRECURSOR_SPARK = ITEMS.register("precursor_spark",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(16)));

    // Weapons
    public static final RegistryObject<Item> PLASMA_CUTTER = ITEMS.register("plasma_cutter",
            () -> new TechWeaponItem(4, -2.4f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> CRYO_SAWBLADE = ITEMS.register("cryo_sawblade",
            () -> new TechWeaponItem(5, -2.3f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> PRECURSOR_CLAYMORE = ITEMS.register("precursor_claymore",
            () -> new TechWeaponItem(8, -2.2f, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1)));

    // 5-Slot Tech Chips
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
}
