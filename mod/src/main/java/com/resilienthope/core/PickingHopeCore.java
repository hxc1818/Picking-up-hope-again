package com.resilienthope.core;

import com.resilienthope.core.account.AccountSessionManager;
import com.resilienthope.core.client.KeyInit;
import com.resilienthope.core.client.battle.BattleAnimCommand;
import com.resilienthope.core.client.battle.BattleSceneClientController;
import com.resilienthope.core.client.builder.BuilderPrefabOverlayRenderer;
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
import com.resilienthope.core.world.HopeMapCommand;
import com.resilienthope.core.world.MapZoningBuilder;
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
            MinecraftForge.EVENT_BUS.register(BuilderPrefabOverlayRenderer.class);
        });

        MinecraftForge.EVENT_BUS.register(TeamCapability.class);
        MinecraftForge.EVENT_BUS.register(com.resilienthope.core.event.StarshipSpawnHandler.class);
        MinecraftForge.EVENT_BUS.register(HopeMapCommand.class);
        MinecraftForge.EVENT_BUS.register(MapZoningBuilder.class);
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

            // Translucent glass, ice & hardlight blocks
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VIEWPORT_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_VIEWPORT_PANE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_REINFORCED_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OUTPOST_WINDOW_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLACIER_ICE_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUIN_HARDLIGHT_BRIDGE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUIN_HARDLIGHT_BARRIER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_ICE_CRYSTAL.get(), RenderType.translucent());

            // Cutout interactive machines, doors, hatches, trusses & 3D voxel props
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CRYO_SLEEP_POD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HOLOTABLE_PROP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TELEPORT_WAYPOINT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENGINEERING_TERMINAL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SUPPLY_CRATE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CONDUIT_PILLAR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUXILIARY_ENERGY_CORE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ORBITAL_DROP_POD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PRECURSOR_ALTAR.get(), RenderType.cutout());

            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_GRATE_FLOOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_CATWALK_SLAB.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_LADDER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_RAILING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_AIRLOCK_DOOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STARSHIP_VENT_HATCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CYAN_LIGHT_STRIP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AMBER_LIGHT_STRIP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MINE_STEEL_TRUSS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUIN_ANCIENT_STELE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUIN_FLOATING_PRISM.get(), RenderType.cutout());

            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_MONITOR_DESK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_WALL_SCREEN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_SERVER_RACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_CABLE_PIPE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_VENT_DUCT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_MEDICAL_BED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_MED_CABINET.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_STORAGE_RACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_LAB_DESK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_CREW_CHAIR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_CARGO_BARREL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_FLOOR_LAMP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_BREAKER_BOX.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_BLACKBOX_RECORDER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_RUIN_RELAY.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_WARNING_SIGN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_FLOODLIGHT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_ANTENNA_MAST.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_SOLAR_PANEL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_SUPPLY_PALLET.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_BUNK_FRAME.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_DINING_TABLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROP_HOLOGRAM_PROJECTOR.get(), RenderType.cutout());
        });
        LOGGER.info("Picking Hope Core: Client HUD & Keybinds initialized.");
    }
}
