package com.resilienthope.core.client;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.model.SecurityDroneModel;
import com.resilienthope.core.client.renderer.SecurityDroneRenderer;
import com.resilienthope.core.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PickingHopeCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SECURITY_DRONE.get(), SecurityDroneRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SecurityDroneModel.LAYER_LOCATION, SecurityDroneModel::createBodyLayer);
    }
}
