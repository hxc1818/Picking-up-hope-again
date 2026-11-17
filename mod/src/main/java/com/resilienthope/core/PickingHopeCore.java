package com.resilienthope.core;

import com.resilienthope.core.client.KeyInit;
import com.resilienthope.core.client.gui.GenshinHudOverlay;
import com.resilienthope.core.client.gui.InventoryOverrideHandler;
import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.team.TeamCapability;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PickingHopeCore.MOD_ID)
public class PickingHopeCore {
    public static final String MOD_ID = "picking_hope_core";
    public static final Logger LOGGER = LogManager.getLogger();

    public PickingHopeCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::setupCommon);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(this::setupClient);
            MinecraftForge.EVENT_BUS.register(GenshinHudOverlay.class);
            MinecraftForge.EVENT_BUS.register(InventoryOverrideHandler.class);
            MinecraftForge.EVENT_BUS.register(KeyInit.class);
        });

        MinecraftForge.EVENT_BUS.register(TeamCapability.class);
        MinecraftForge.EVENT_BUS.register(com.resilienthope.core.event.StarshipSpawnHandler.class);
    }

    private void setupCommon(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PacketHandler.register();
            TeamCapability.register();
        });
        LOGGER.info("Picking Hope Core: Common setup initialized.");
    }

    private void setupClient(final FMLClientSetupEvent event) {
        event.enqueueWork(KeyInit::registerKeybinds);
        LOGGER.info("Picking Hope Core: Client HUD & Keybinds initialized.");
    }
}
