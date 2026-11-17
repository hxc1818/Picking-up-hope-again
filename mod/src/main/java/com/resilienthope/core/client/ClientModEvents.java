package com.resilienthope.core.client;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.model.PrecursorMechModel;
import com.resilienthope.core.client.model.SecurityDroneModel;
import com.resilienthope.core.client.renderer.FactionNpcRenderer;
import com.resilienthope.core.client.renderer.PrecursorMechRenderer;
import com.resilienthope.core.client.renderer.SecurityDroneRenderer;
import com.resilienthope.core.init.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PickingHopeCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SECURITY_DRONE.get(), SecurityDroneRenderer::new);
        event.registerEntityRenderer(ModEntities.PRECURSOR_MECH_BOSS.get(), PrecursorMechRenderer::new);
        event.registerEntityRenderer(ModEntities.FACTION_NPC.get(), FactionNpcRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SecurityDroneModel.LAYER_LOCATION, SecurityDroneModel::createBodyLayer);
        event.registerLayerDefinition(PrecursorMechModel.LAYER_LOCATION, PrecursorMechModel::createBodyLayer);
        event.registerLayerDefinition(FactionNpcRenderer.LAYER_LOCATION,
                () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));
    }
}
