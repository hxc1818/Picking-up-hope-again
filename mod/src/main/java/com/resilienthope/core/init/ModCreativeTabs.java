package com.resilienthope.core.init;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Dedicated creative tabs grouping custom building blocks and small voxel props by biome/architecture theme.
 */
public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final CreativeModeTab TAB_STARSHIP = new CreativeModeTab("picking_hope_starship") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.STARSHIP_HULL.get());
        }
    };

    public static final CreativeModeTab TAB_PLANET = new CreativeModeTab("picking_hope_planet") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.PERMAFROST_BRICKS.get());
        }
    };

    public static final CreativeModeTab TAB_RUINS = new CreativeModeTab("picking_hope_ruins") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.RUIN_CIRCUIT_TEAL.get());
        }
    };

    public static final CreativeModeTab TAB_PROPS = new CreativeModeTab("picking_hope_props") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.HOLOTABLE_PROP.get());
        }
    };
}
