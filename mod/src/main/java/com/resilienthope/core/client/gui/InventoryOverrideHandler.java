package com.resilienthope.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InventoryOverrideHandler {

    @SubscribeEvent
    public static void onScreenOpen(ScreenOpenEvent event) {
        if (event.getScreen() instanceof InventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.player.isCreative()) {
                // Cancel vanilla survival inventory
                event.setCanceled(true);
                // Open Genshin-style Infinite Categorized Bag
                mc.setScreen(new InfiniteInventoryScreen());
            }
        }
    }
}
