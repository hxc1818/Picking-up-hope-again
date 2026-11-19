package com.resilienthope.core;

import com.resilienthope.core.account.AccountSessionManager;
import com.resilienthope.core.client.KeyInit;
import com.resilienthope.core.client.battle.BattleAnimCommand;
import com.resilienthope.core.client.battle.BattleSceneClientController;
import com.resilienthope.core.client.gui.GenshinHudOverlay;
import com.resilienthope.core.client.gui.InventoryOverrideHandler;
import com.resilienthope.core.client.gui.TitleScreenOverrideHandler;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.entity.PrecursorMechBossEntity;
import com.resilienthope.core.entity.SecurityDroneEntity;
import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.init.ModEntities;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
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
        ModEntities.ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::setupCommon);
        modEventBus.addListener(this::registerEntityAttributes);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(this::setupClient);
            MinecraftForge.EVENT_BUS.register(GenshinHudOverlay.class);
            MinecraftForge.EVENT_BUS.register(InventoryOverrideHandler.class);
            MinecraftForge.EVENT_BUS.register(TitleScreenOverrideHandler.class);
            MinecraftForge.EVENT_BUS.register(BattleSceneClientController.class);
            MinecraftForge.EVENT_BUS.register(BattleAnimCommand.class);
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

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SECURITY_DRONE.get(), SecurityDroneEntity.createAttributes().build());
        event.put(ModEntities.PRECURSOR_MECH_BOSS.get(), PrecursorMechBossEntity.createAttributes().build());
        event.put(ModEntities.FACTION_NPC.get(), FactionNpcEntity.createAttributes().build());
    }

    private void setupClient(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AccountSessionManager.initOnStartup();
            KeyInit.registerKeybinds();
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VIEWPORT_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CRYO_SLEEP_POD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HOLOTABLE_PROP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TELEPORT_WAYPOINT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENGINEERING_TERMINAL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SUPPLY_CRATE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CONDUIT_PILLAR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUXILIARY_ENERGY_CORE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ORBITAL_DROP_POD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PRECURSOR_ALTAR.get(), RenderType.cutout());
        });
        LOGGER.info("Picking Hope Core: Client HUD & Keybinds initialized.");
    }
}
