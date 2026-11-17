package com.resilienthope.core.client.renderer;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.entity.FactionNpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FactionNpcRenderer extends MobRenderer<FactionNpcEntity, HumanoidModel<FactionNpcEntity>> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(PickingHopeCore.MOD_ID, "faction_npc"), "main");

    private static final ResourceLocation TEX_ZHENG =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/npc_zheng.png");
    private static final ResourceLocation TEX_HAN =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/npc_han.png");
    private static final ResourceLocation TEX_SU =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/npc_su.png");

    public FactionNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FactionNpcEntity entity) {
        return switch (entity.getNpcId()) {
            case "han" -> TEX_HAN;
            case "su" -> TEX_SU;
            default -> TEX_ZHENG;
        };
    }
}
