package com.resilienthope.core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.model.PrecursorMechModel;
import com.resilienthope.core.entity.PrecursorMechBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PrecursorMechRenderer extends MobRenderer<PrecursorMechBossEntity, PrecursorMechModel<PrecursorMechBossEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/precursor_mech.png");

    public PrecursorMechRenderer(EntityRendererProvider.Context context) {
        super(context, new PrecursorMechModel<>(context.bakeLayer(PrecursorMechModel.LAYER_LOCATION)), 1.1F);
    }

    @Override
    protected void scale(PrecursorMechBossEntity entity, PoseStack poseStack, float partialTicks) {
        poseStack.scale(1.35F, 1.35F, 1.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrecursorMechBossEntity entity) {
        return TEXTURE;
    }
}
