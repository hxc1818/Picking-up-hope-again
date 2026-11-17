package com.resilienthope.core.init;

import com.resilienthope.core.PickingHopeCore;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PickingHopeCore.MOD_ID);

    public static final RegistryObject<Item> TELEPORT_WAYPOINT = ITEMS.register("teleport_waypoint",
            () -> new BlockItem(ModBlocks.TELEPORT_WAYPOINT.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> ENGINEERING_TERMINAL = ITEMS.register("engineering_terminal",
            () -> new BlockItem(ModBlocks.ENGINEERING_TERMINAL.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> AUXILIARY_ENERGY_CORE = ITEMS.register("auxiliary_energy_core",
            () -> new BlockItem(ModBlocks.AUXILIARY_ENERGY_CORE.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> QUANTUM_CORE = ITEMS.register("quantum_core",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> PLASMA_CUTTER = ITEMS.register("plasma_cutter",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1).durability(1200)));

    public static final RegistryObject<Item> FUSION_CORE_CHIP = ITEMS.register("fusion_core_chip",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(16)));
}
