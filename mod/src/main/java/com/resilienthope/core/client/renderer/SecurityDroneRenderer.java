package com.resilienthope.core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.model.SecurityDroneModel;
import com.resilienthope.core.entity.SecurityDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class SecurityDroneRenderer extends LivingEntityRenderer<SecurityDroneEntity, SecurityDroneModel<SecurityDroneEntity>> {

    private static final ResourceLocation TEX_DRONE =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/security_drone.png");
    private static final ResourceLocation TEX_CLEANER =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/cleaner_bot.png");
    private static final ResourceLocation TEX_HOUND =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/mech_hound.png");
    private static final ResourceLocation TEX_GUARD =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/heavy_guard_mech.png");
    private static final ResourceLocation TEX_DRILLER =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/mine_driller.png");

    public SecurityDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityDroneModel<>(context.bakeLayer(SecurityDroneModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    protected void scale(SecurityDroneEntity entity, PoseStack poseStack, float partialTicks) {
        String n = entity.getName().getString();
        if (n.contains("重装") || n.contains("统御") || n.contains("钻探")) {
            poseStack.scale(1.32F, 1.32F, 1.32F);
        } else if (n.contains("猎犬")) {
            poseStack.scale(1.12F, 0.95F, 1.25F);
        } else {
            poseStack.scale(1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SecurityDroneEntity entity) {
        String n = entity.getName().getString();
        if (n.contains("清扫")) return TEX_CLEANER;
        if (n.contains("猎犬")) return TEX_HOUND;
        if (n.contains("钻探")) return TEX_DRILLER;
        if (n.contains("重装") || n.contains("统御") || n.contains("守卫")) return TEX_GUARD;
        return TEX_DRONE;
    }
}
