package com.resilienthope.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.entity.SecurityDroneEntity;
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

public class SecurityDroneModel<T extends SecurityDroneEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(PickingHopeCore.MOD_ID, "security_drone"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart scannerRing;
    private final ModelPart leftThruster;
    private final ModelPart rightThruster;
    private final ModelPart leftWeapon;
    private final ModelPart rightWeapon;

    public SecurityDroneModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.scannerRing = root.getChild("scanner_ring");
        this.leftThruster = root.getChild("left_thruster");
        this.rightThruster = root.getChild("right_thruster");
        this.leftWeapon = root.getChild("left_weapon");
        this.rightWeapon = root.getChild("right_weapon");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // Main octagonal core & optic eye (Zone 1: 0..48, 0..24)
        parts.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.5F, -4.0F, -4.5F, 9.0F, 8.0F, 9.0F, CubeDeformation.NONE)
                        .texOffs(0, 24)
                        .addBox(-5.5F, -1.5F, -3.5F, 11.0F, 3.0F, 7.0F, new CubeDeformation(0.15F)),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        // Rotating top halo scanner ring
        parts.addOrReplaceChild("scanner_ring",
                CubeListBuilder.create()
                        .texOffs(32, 24)
                        .addBox(-6.0F, -1.0F, -1.0F, 12.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                        .texOffs(32, 28)
                        .addBox(-1.0F, -1.0F, -6.0F, 2.0F, 2.0F, 12.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 6.5F, 0.0F));

        // Left & Right Hover Thrusters (Zone 3: 32..64, 24..44)
        parts.addOrReplaceChild("left_thruster",
                CubeListBuilder.create()
                        .texOffs(32, 24)
                        .addBox(0.0F, -3.0F, -2.0F, 3.0F, 7.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(6.0F, 11.0F, 0.0F, 0.15F, 0.0F, -0.18F));

        parts.addOrReplaceChild("right_thruster",
                CubeListBuilder.create()
                        .texOffs(32, 24)
                        .addBox(-3.0F, -3.0F, -2.0F, 3.0F, 7.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-6.0F, 11.0F, 0.0F, 0.15F, 0.0F, 0.18F));

        // Under-slung Twin Pulse Cannons / Sweepers (Zone 4: 0..64, 44..64)
        parts.addOrReplaceChild("left_weapon",
                CubeListBuilder.create()
                        .texOffs(0, 44)
                        .addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offset(4.0F, 15.5F, -1.5F));

        parts.addOrReplaceChild("right_weapon",
                CubeListBuilder.create()
                        .texOffs(0, 44)
                        .addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offset(-4.0F, 15.5F, -1.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float hover = Mth.sin(ageInTicks * 0.14F) * 1.4F;
        this.body.y = 12.0F + hover;
        this.body.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.35F;
        this.body.xRot = headPitch * ((float) Math.PI / 180F) * 0.25F;

        this.scannerRing.y = 6.5F + hover;
        this.scannerRing.yRot = ageInTicks * 0.12F;

        this.leftThruster.y = 11.0F + hover;
        this.rightThruster.y = 11.0F + hover;
        float thrustTilt = 0.15F + Mth.cos(ageInTicks * 0.18F) * 0.12F;
        this.leftThruster.xRot = thrustTilt;
        this.rightThruster.xRot = thrustTilt;

        this.leftWeapon.y = 15.5F + hover;
        this.rightWeapon.y = 15.5F + hover;
        this.leftWeapon.xRot = headPitch * ((float) Math.PI / 180F) * 0.5F;
        this.rightWeapon.xRot = headPitch * ((float) Math.PI / 180F) * 0.5F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
