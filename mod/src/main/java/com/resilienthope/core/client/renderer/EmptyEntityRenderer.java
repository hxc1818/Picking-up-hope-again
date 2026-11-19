package com.resilienthope.core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.entity.BattleCameraEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * No-op renderer for the invisible BattleCameraEntity anchor.
 */
public class EmptyEntityRenderer extends EntityRenderer<BattleCameraEntity> {
    public EmptyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(BattleCameraEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public void render(BattleCameraEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {}

    @Override
    public ResourceLocation getTextureLocation(BattleCameraEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
