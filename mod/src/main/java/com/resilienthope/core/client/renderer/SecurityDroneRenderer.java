package com.resilienthope.core.client.renderer;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.model.SecurityDroneModel;
import com.resilienthope.core.entity.SecurityDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class SecurityDroneRenderer extends LivingEntityRenderer<SecurityDroneEntity, SecurityDroneModel<SecurityDroneEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/security_drone.png");

    public SecurityDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityDroneModel<>(context.bakeLayer(SecurityDroneModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(SecurityDroneEntity entity) {
        return TEXTURE;
    }
}
