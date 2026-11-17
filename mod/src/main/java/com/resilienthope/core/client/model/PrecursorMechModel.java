package com.resilienthope.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.entity.PrecursorMechBossEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PrecursorMechModel<T extends PrecursorMechBossEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(PickingHopeCore.MOD_ID, "precursor_mech"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public PrecursorMechModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 10.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -8.0F, 0.0F));

        parts.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(16, 18).addBox(-10.0F, -8.0F, -6.0F, 20.0F, 18.0F, 12.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        parts.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, -2.0F, -4.0F, 6.0F, 22.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(-11.0F, -6.0F, 0.0F));

        parts.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(36, 32).addBox(-1.0F, -2.0F, -4.0F, 6.0F, 22.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(11.0F, -6.0F, 0.0F));

        parts.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 14.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offset(-5.5F, 10.0F, 0.0F));

        parts.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 14.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offset(5.5F, 10.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.1F * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
