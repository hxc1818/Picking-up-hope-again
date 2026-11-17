package com.resilienthope.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InventoryOverrideHandler {

    @SubscribeEvent
    public static void onScreenOpen(ScreenOpenEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative()) return;

        if (event.getScreen() instanceof InventoryScreen) {
            // Cancel vanilla survival inventory
            event.setCanceled(true);
            mc.setScreen(new InfiniteInventoryScreen());
        } else if (event.getScreen() instanceof ContainerScreen) {
            // Cancel vanilla chest container screen with 36 inventory slots
            event.setCanceled(true);
            if (mc.hitResult instanceof BlockHitResult bHit) {
                BlockPos pos = bHit.getBlockPos();
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof Container container) {
                    mc.setScreen(new LootPopupScreen(pos, container));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isClientSide()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative()) return;

        BlockPos pos = event.getPos();
        BlockEntity be = event.getWorld().getBlockEntity(pos);
        if (be instanceof Container container) {
            event.setCanceled(true);
            mc.setScreen(new LootPopupScreen(pos, container));
        }
    }
}
