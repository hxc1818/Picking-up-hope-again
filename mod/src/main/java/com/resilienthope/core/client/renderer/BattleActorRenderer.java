package com.resilienthope.core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.client.battle.BattleAnimClip;
import com.resilienthope.core.client.battle.BattleSceneClientController;
import com.resilienthope.core.combat.BattleArenaLayout;
import com.resilienthope.core.entity.BattleActorEntity;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders live 3D turn-based battle entities (all 6 playable characters + weapons, and all 6 mech archetypes)
 * directly in the Minecraft world driven by BattleAnimClip keyframes and native entity glowing.
 */
public class BattleActorRenderer extends EntityRenderer<BattleActorEntity> {

    private static final ResourceLocation TEX_FX = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/block/starship_light.png");

    private static final ResourceLocation TEX_CHAR_LINYE = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/char_linye.png");
    private static final ResourceLocation TEX_CHAR_CHEN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/char_chenfeng.png");
    private static final ResourceLocation TEX_CHAR_SU = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/char_suqing.png");
    private static final ResourceLocation TEX_CHAR_ZHENG = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/char_laozheng.png");
    private static final ResourceLocation TEX_CHAR_HAN = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/char_hanbing.png");
    private static final ResourceLocation TEX_CHAR_ZHOU = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/char_zhoumin.png");

    private static final ResourceLocation TEX_CLEANER = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/cleaner_bot.png");
    private static final ResourceLocation TEX_DRONE = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/security_drone.png");
    private static final ResourceLocation TEX_HOUND = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/mech_hound.png");
    private static final ResourceLocation TEX_GUARD = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/heavy_guard_mech.png");
    private static final ResourceLocation TEX_DRILLER = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/mine_driller.png");
    private static final ResourceLocation TEX_BOSS = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/entity/precursor_mech.png");

    private boolean modelsBuilt = false;
    private ModelPart humanHead, humanHat, humanBody, humanRightArm, humanLeftArm, humanRightLeg, humanLeftLeg;
    private ModelPart weaponBlade, weaponHammer, weaponSniper, weaponPistol;
    private ModelPart cleanerBody, cleanerBrushL, cleanerBrushR;
    private ModelPart droneBody, droneRing, droneThrustL, droneThrustR;
    private ModelPart houndTorso, houndJaw, houndLegFL, houndLegFR, houndLegBL, houndLegBR, houndTail;
    private ModelPart guardHead, guardBody, guardArmL, guardArmR, guardLegL, guardLegR;
    private ModelPart drillerChassis, drillerDrill;
    private ModelPart bossHead, bossTorso, bossArmL, bossArmR, bossLegL, bossLegR;
    private ModelPart isolatedArmL, isolatedArmR;

    public BattleActorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55F;
    }

    private void ensureModelsBuilt() {
        if (modelsBuilt) return;
        modelsBuilt = true;

        // 1. Humanoid Rig (64x64 player skin UV)
        MeshDefinition hm = new MeshDefinition();
        PartDefinition hp = hm.getRoot();
        hp.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.ZERO);
        hp.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0.45F)), PartPose.ZERO);
        hp.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F), PartPose.ZERO);
        hp.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3F, -2F, -2F, 4F, 12F, 4F), PartPose.offset(-5F, 2F, 0F));
        hp.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1F, -2F, -2F, 4F, 12F, 4F), PartPose.offset(5F, 2F, 0F));
        hp.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F), PartPose.offset(-1.9F, 12F, 0F));
        hp.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2F, 0F, -2F, 4F, 12F, 4F), PartPose.offset(1.9F, 12F, 0F));
        ModelPart hRoot = LayerDefinition.create(hm, 64, 64).bakeRoot();
        this.humanHead = hRoot.getChild("head");
        this.humanHat = hRoot.getChild("hat");
        this.humanBody = hRoot.getChild("body");
        this.humanRightArm = hRoot.getChild("right_arm");
        this.humanLeftArm = hRoot.getChild("left_arm");
        this.humanRightLeg = hRoot.getChild("right_leg");
        this.humanLeftLeg = hRoot.getChild("left_leg");

        // 4 Distinct 3D Sculpted Weapons
        MeshDefinition wbm = new MeshDefinition();
        wbm.getRoot().addOrReplaceChild("w", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1F, -2F, -1F, 2F, 5F, 2F)
                        .texOffs(0, 0).addBox(-2.5F, -3F, -2F, 5F, 2F, 4F)
                        .texOffs(0, 0).addBox(-1F, -19F, -1.6F, 2F, 16F, 3.2F),
                PartPose.ZERO);
        this.weaponBlade = LayerDefinition.create(wbm, 16, 16).bakeRoot().getChild("w");

        MeshDefinition whm = new MeshDefinition();
        whm.getRoot().addOrReplaceChild("w", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1F, -18F, -1F, 2F, 22F, 2F)
                        .texOffs(0, 0).addBox(-4.5F, -23F, -4.5F, 9F, 6F, 9F),
                PartPose.ZERO);
        this.weaponHammer = LayerDefinition.create(whm, 16, 16).bakeRoot().getChild("w");

        MeshDefinition wsm = new MeshDefinition();
        wsm.getRoot().addOrReplaceChild("w", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.4F, -22F, -1.5F, 2.8F, 22F, 3F)
                        .texOffs(0, 0).addBox(-1F, -14F, -4.2F, 2F, 6F, 2.4F)
                        .texOffs(0, 0).addBox(-1.6F, 0F, -1.2F, 3.2F, 6F, 2.8F),
                PartPose.ZERO);
        this.weaponSniper = LayerDefinition.create(wsm, 16, 16).bakeRoot().getChild("w");

        MeshDefinition wpm = new MeshDefinition();
        wpm.getRoot().addOrReplaceChild("w", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.3F, -12F, -1.4F, 2.6F, 12F, 2.8F)
                        .texOffs(0, 0).addBox(-1F, -8F, -3.5F, 2F, 4F, 2F)
                        .texOffs(0, 0).addBox(-1F, 0F, -1F, 2F, 4F, 2F),
                PartPose.ZERO);
        this.weaponPistol = LayerDefinition.create(wpm, 16, 16).bakeRoot().getChild("w");

        // 2. Cleaner Bot Rig
        MeshDefinition cm = new MeshDefinition();
        PartDefinition cp = cm.getRoot();
        cp.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6F, 12F, -6F, 12F, 8F, 12F)
                        .texOffs(0, 24).addBox(-7F, 17F, -7F, 14F, 4F, 14F)
                        .texOffs(32, 24).addBox(-2F, 7F, -2F, 4F, 5F, 4F),
                PartPose.ZERO);
        cp.addOrReplaceChild("brush_l", CubeListBuilder.create().texOffs(0, 44).addBox(-2F, -1F, -2F, 4F, 3F, 4F), PartPose.offset(4.5F, 22F, -5F));
        cp.addOrReplaceChild("brush_r", CubeListBuilder.create().texOffs(0, 44).addBox(-2F, -1F, -2F, 4F, 3F, 4F), PartPose.offset(-4.5F, 22F, -5F));
        ModelPart cRoot = LayerDefinition.create(cm, 64, 64).bakeRoot();
        this.cleanerBody = cRoot.getChild("body");
        this.cleanerBrushL = cRoot.getChild("brush_l");
        this.cleanerBrushR = cRoot.getChild("brush_r");

        // 3. Security Drone Rig
        MeshDefinition dm = new MeshDefinition();
        PartDefinition dp = dm.getRoot();
        dp.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, 5F, -4.5F, 9F, 8F, 9F)
                        .texOffs(0, 44).addBox(-3F, 13F, -6F, 2F, 2F, 6F)
                        .texOffs(0, 44).addBox(1F, 13F, -6F, 2F, 2F, 6F),
                PartPose.ZERO);
        dp.addOrReplaceChild("ring", CubeListBuilder.create()
                        .texOffs(32, 24).addBox(-6F, -1F, -1F, 12F, 2F, 2F)
                        .texOffs(32, 28).addBox(-1F, -1F, -6F, 2F, 2F, 12F),
                PartPose.offset(0F, 3F, 0F));
        dp.addOrReplaceChild("tl", CubeListBuilder.create().texOffs(32, 24).addBox(0F, -3F, -2F, 3F, 7F, 4F), PartPose.offset(5.5F, 8F, 0F));
        dp.addOrReplaceChild("tr", CubeListBuilder.create().texOffs(32, 24).addBox(-3F, -3F, -2F, 3F, 7F, 4F), PartPose.offset(-5.5F, 8F, 0F));
        ModelPart dRoot = LayerDefinition.create(dm, 64, 64).bakeRoot();
        this.droneBody = dRoot.getChild("body");
        this.droneRing = dRoot.getChild("ring");
        this.droneThrustL = dRoot.getChild("tl");
        this.droneThrustR = dRoot.getChild("tr");

        // 4. Mech Hound Rig
        MeshDefinition hnd = new MeshDefinition();
        PartDefinition hnp = hnd.getRoot();
        hnp.addOrReplaceChild("torso", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, 10F, -7F, 8F, 6F, 14F)
                        .texOffs(32, 24).addBox(-1.5F, 7F, -3F, 3F, 3F, 8F),
                PartPose.ZERO);
        hnp.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -4F, -6F, 7F, 6F, 7F)
                        .texOffs(0, 44).addBox(-2F, 0F, -8F, 4F, 2F, 3F),
                PartPose.offset(0F, 12F, -7F));
        hnp.addOrReplaceChild("fl", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F), PartPose.offset(3.5F, 16F, -5F));
        hnp.addOrReplaceChild("fr", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F), PartPose.offset(-3.5F, 16F, -5F));
        hnp.addOrReplaceChild("bl", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F), PartPose.offset(3.5F, 16F, 5F));
        hnp.addOrReplaceChild("br", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F), PartPose.offset(-3.5F, 16F, 5F));
        hnp.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 44).addBox(-1F, -1F, 0F, 2F, 2F, 7F), PartPose.offset(0F, 11F, 7F));
        ModelPart hndRoot = LayerDefinition.create(hnd, 64, 64).bakeRoot();
        this.houndTorso = hndRoot.getChild("torso");
        this.houndJaw = hndRoot.getChild("jaw");
        this.houndLegFL = hndRoot.getChild("fl");
        this.houndLegFR = hndRoot.getChild("fr");
        this.houndLegBL = hndRoot.getChild("bl");
        this.houndLegBR = hndRoot.getChild("br");
        this.houndTail = hndRoot.getChild("tail");

        // 5. Heavy Guard / Bridge Commander Rig
        MeshDefinition gm = new MeshDefinition();
        PartDefinition gp = gm.getRoot();
        gp.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -7F, -4F, 8F, 6F, 8F), PartPose.ZERO);
        gp.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6F, -1F, -4F, 12F, 13F, 8F)
                        .texOffs(32, 24).addBox(-3F, 2F, -5F, 6F, 5F, 2F),
                PartPose.ZERO);
        gp.addOrReplaceChild("arml", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(0F, -2F, -2.5F, 4F, 12F, 5F)
                        .texOffs(0, 44).addBox(2F, -1F, -6F, 2F, 13F, 10F),
                PartPose.offset(6.5F, 1F, 0F));
        gp.addOrReplaceChild("armr", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-4F, -2F, -2.5F, 4F, 12F, 5F)
                        .texOffs(0, 44).addBox(-5F, 6F, -9F, 4F, 4F, 10F),
                PartPose.offset(-6.5F, 1F, 0F));
        gp.addOrReplaceChild("legl", CubeListBuilder.create().texOffs(0, 24).addBox(-2.5F, 0F, -2.5F, 5F, 12F, 5F), PartPose.offset(3.2F, 12F, 0F));
        gp.addOrReplaceChild("legr", CubeListBuilder.create().texOffs(0, 24).addBox(-2.5F, 0F, -2.5F, 5F, 12F, 5F), PartPose.offset(-3.2F, 12F, 0F));
        ModelPart gRoot = LayerDefinition.create(gm, 64, 64).bakeRoot();
        this.guardHead = gRoot.getChild("head");
        this.guardBody = gRoot.getChild("body");
        this.guardArmL = gRoot.getChild("arml");
        this.guardArmR = gRoot.getChild("armr");
        this.guardLegL = gRoot.getChild("legl");
        this.guardLegR = gRoot.getChild("legr");

        // 6. Mine Driller Rig
        MeshDefinition drm = new MeshDefinition();
        PartDefinition drp = drm.getRoot();
        drp.addOrReplaceChild("chassis", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6F, 7F, -3F, 12F, 11F, 11F)
                        .texOffs(0, 24).addBox(-7.5F, 16F, -5F, 4F, 8F, 15F)
                        .texOffs(0, 24).addBox(3.5F, 16F, -5F, 4F, 8F, 15F),
                PartPose.ZERO);
        drp.addOrReplaceChild("drill", CubeListBuilder.create()
                        .texOffs(0, 44).addBox(-4F, -4F, -4F, 8F, 8F, 4F)
                        .texOffs(32, 24).addBox(-3F, -3F, -9F, 6F, 6F, 5F)
                        .texOffs(0, 44).addBox(-1.5F, -1.5F, -14F, 3F, 3F, 5F),
                PartPose.offset(0F, 13F, -3F));
        ModelPart drRoot = LayerDefinition.create(drm, 64, 64).bakeRoot();
        this.drillerChassis = drRoot.getChild("chassis");
        this.drillerDrill = drRoot.getChild("drill");

        // 7. Heavy Excavator Boss Core + Standalone Arm Rigs
        MeshDefinition bm = new MeshDefinition();
        PartDefinition bp = bm.getRoot();
        bp.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -7F, -4.5F, 9F, 7F, 9F)
                        .texOffs(32, 24).addBox(-5.5F, -8.5F, -3.5F, 11F, 2F, 3F),
                PartPose.offset(0F, -9F, 0F));
        bp.addOrReplaceChild("torso", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-9F, -9F, -5F, 18F, 18F, 10F)
                        .texOffs(32, 24).addBox(-4F, -4F, -6F, 8F, 8F, 2F)
                        .texOffs(0, 24).addBox(-7.5F, -15F, 2.5F, 3F, 7F, 3F)
                        .texOffs(0, 24).addBox(4.5F, -15F, 2.5F, 3F, 7F, 3F),
                PartPose.ZERO);
        bp.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-5F, -2F, -3.5F, 5F, 18F, 7F)
                        .texOffs(0, 44).addBox(-6.5F, 14F, -6F, 8F, 7F, 12F),
                PartPose.offset(-10F, -6F, 0F));
        bp.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(0F, -2F, -3.5F, 5F, 18F, 7F)
                        .texOffs(0, 44).addBox(-1F, 14F, -7F, 7F, 6F, 10F),
                PartPose.offset(10F, -6F, 0F));
        bp.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, 0F, -4F, 7F, 14F, 8F), PartPose.offset(-5.5F, 9F, 0F));
        bp.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, 0F, -4F, 7F, 14F, 8F), PartPose.offset(5.5F, 9F, 0F));
        ModelPart bRoot = LayerDefinition.create(bm, 64, 64).bakeRoot();
        this.bossHead = bRoot.getChild("head");
        this.bossTorso = bRoot.getChild("torso");
        this.bossArmL = bRoot.getChild("left_arm");
        this.bossArmR = bRoot.getChild("right_arm");
        this.bossLegL = bRoot.getChild("left_leg");
        this.bossLegR = bRoot.getChild("right_leg");

        // Centered floating boss arms for Phase 1 left/right arm entities
        MeshDefinition iam = new MeshDefinition();
        iam.getRoot().addOrReplaceChild("al", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-2.5F, -6F, -3.5F, 5F, 18F, 7F)
                        .texOffs(0, 44).addBox(-3.5F, 10F, -7F, 7F, 6F, 10F),
                PartPose.offset(0F, 6F, 0F));
        iam.getRoot().addOrReplaceChild("ar", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-2.5F, -6F, -3.5F, 5F, 18F, 7F)
                        .texOffs(0, 44).addBox(-4F, 10F, -6F, 8F, 7F, 12F),
                PartPose.offset(0F, 6F, 0F));
        ModelPart iaRoot = LayerDefinition.create(iam, 64, 64).bakeRoot();
        this.isolatedArmL = iaRoot.getChild("al");
        this.isolatedArmR = iaRoot.getChild("ar");
    }

    @Override
    public boolean shouldRender(BattleActorEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(BattleActorEntity entity) {
        String key = entity.getModelKey();
        if (entity.getSide() == 0) {
            return switch (key != null ? key : "") {
                case "chenfeng" -> TEX_CHAR_CHEN;
                case "suqing" -> TEX_CHAR_SU;
                case "laozheng" -> TEX_CHAR_ZHENG;
                case "hanbing" -> TEX_CHAR_HAN;
                case "zhoumin" -> TEX_CHAR_ZHOU;
                default -> TEX_CHAR_LINYE;
            };
        }
        return switch (key != null ? key : "") {
            case "cleaner" -> TEX_CLEANER;
            case "hound" -> TEX_HOUND;
            case "driller" -> TEX_DRILLER;
            case "guard" -> TEX_GUARD;
            case "boss_core", "boss_arm_l", "boss_arm_r" -> TEX_BOSS;
            default -> TEX_DRONE;
        };
    }

    @Override
    public void render(BattleActorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.getSide() == 1 && !entity.isAliveActor()) {
            return;
        }
        ensureModelsBuilt();

        BattleSceneClientController.ActorVisualPose pose =
                BattleSceneClientController.computeActorPose(entity, partialTicks);
        BattleAnimClip clip = pose.clip;
        float p = pose.clipProgress;

        float baseYaw = entity.getSide() == 0 ? BattleArenaLayout.ALLY_YAW : BattleArenaLayout.ENEMY_YAW;
        float finalYaw = baseYaw + pose.yawOffset;
        float scale = entity.getActorScale();
        int light = pose.acting ? LightTexture.FULL_BRIGHT : Math.max(packedLight, LightTexture.pack(13, 13));

        float cr = 1.0F, cg = 1.0F, cb = 1.0F;
        if (pose.hurtFlash) {
            cr = 1.0F; cg = 0.35F; cb = 0.35F;
        } else if (entity.isBrokenState()) {
            cr = 0.65F; cg = 0.72F; cb = 0.92F;
        } else if (entity.isChargingState() && ((entity.tickCount / 4) % 2 == 0)) {
            cr = 1.0F; cg = 0.52F; cb = 0.42F;
        }

        poseStack.pushPose();
        poseStack.translate(pose.offsetX, pose.offsetY, pose.offsetZ);
        if (entity.getSide() == 0 && !entity.isAliveActor()) {
            poseStack.translate(0.0D, 0.25D, 0.0D);
        }
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - finalYaw));
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0.0D, -1.501D, 0.0D);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));

        if (entity.getSide() == 0) {
            renderHumanoid(poseStack, buffer, vc, light, entity, clip, p, cr, cg, cb);
        } else {
            renderEnemy(poseStack, vc, light, entity, clip, p, cr, cg, cb);
        }

        poseStack.popPose();
    }

    private void renderHumanoid(PoseStack ps, MultiBufferSource buffer, VertexConsumer vc, int light,
                                BattleActorEntity entity, BattleAnimClip clip, float p,
                                float cr, float cg, float cb) {
        if (!entity.isAliveActor()) {
            cr *= 0.55F; cg *= 0.55F; cb *= 0.55F;
        }
        BattleAnimClip.BonePose torsoP = sample(clip, "torso", p);
        BattleAnimClip.BonePose headP = sample(clip, "head", p);
        BattleAnimClip.BonePose raP = sample(clip, "right_arm", p);
        BattleAnimClip.BonePose laP = sample(clip, "left_arm", p);
        BattleAnimClip.BonePose rlP = sample(clip, "right_leg", p);
        BattleAnimClip.BonePose llP = sample(clip, "left_leg", p);
        BattleAnimClip.BonePose wepP = sample(clip, "weapon", p);

        applyPart(humanHead, 0F, 0F, 0F, headP);
        applyPart(humanHat, 0F, 0F, 0F, headP);
        applyPart(humanBody, 0F, 0F, 0F, BattleAnimClip.BonePose.ZERO);
        applyPart(humanRightArm, -5F, 2F, 0F, raP);
        applyPart(humanLeftArm, 5F, 2F, 0F, laP);
        applyPart(humanRightLeg, -1.9F, 12F, 0F, rlP);
        applyPart(humanLeftLeg, 1.9F, 12F, 0F, llP);

        ps.pushPose();
        if (!entity.isAliveActor()) {
            ps.translate(0.0, 1.15, 0.0);
            ps.mulPose(Vector3f.ZP.rotationDegrees(88.0F));
        }

        // Legs
        humanRightLeg.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        humanLeftLeg.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);

        // Upper body pivots at hip joint (y = 12 px) under "torso" track
        ps.pushPose();
        ps.translate(torsoP.px() / 16.0F, (12.0F + torsoP.py()) / 16.0F, torsoP.pz() / 16.0F);
        ps.mulPose(Vector3f.ZP.rotationDegrees(torsoP.rzDeg()));
        ps.mulPose(Vector3f.YP.rotationDegrees(torsoP.ryDeg()));
        ps.mulPose(Vector3f.XP.rotationDegrees(torsoP.rxDeg()));
        ps.translate(0.0F, -12.0F / 16.0F, 0.0F);

        humanHead.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        humanHat.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        humanBody.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        humanLeftArm.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        humanRightArm.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);

        if (entity.isAliveActor()) {
            ps.pushPose();
            humanRightArm.translateAndRotate(ps);
            ps.translate((-1.0F + wepP.px()) / 16.0F, (9.5F + wepP.py()) / 16.0F, (-1.5F + wepP.pz()) / 16.0F);
            ps.mulPose(Vector3f.XP.rotationDegrees(72.0F + wepP.rxDeg()));
            ps.mulPose(Vector3f.YP.rotationDegrees(wepP.ryDeg()));
            ps.mulPose(Vector3f.ZP.rotationDegrees(wepP.rzDeg()));

            VertexConsumer wvc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_FX));
            int elemCol = CharacterData.getElementColor(entity.getElementKey());
            float wr = Math.min(1F, ((elemCol >> 16) & 0xFF) / 255.0F + 0.25F);
            float wg = Math.min(1F, ((elemCol >> 8) & 0xFF) / 255.0F + 0.25F);
            float wb = Math.min(1F, (elemCol & 0xFF) / 255.0F + 0.25F);

            String charId = entity.getModelKey();
            ModelPart weaponModel = switch (charId != null ? charId : "") {
                case "laozheng" -> weaponHammer;
                case "hanbing" -> weaponSniper;
                case "suqing", "zhoumin" -> weaponPistol;
                default -> weaponBlade;
            };
            weaponModel.render(ps, wvc, light, OverlayTexture.NO_OVERLAY, wr, wg, wb, 1.0F);
            ps.popPose();
        }
        ps.popPose(); // torso pivot
        ps.popPose();
    }

    private void renderEnemy(PoseStack ps, VertexConsumer vc, int light,
                             BattleActorEntity entity, BattleAnimClip clip, float p,
                             float cr, float cg, float cb) {
        String key = entity.getModelKey();
        BattleAnimClip.BonePose bodyP = sample(clip, "body", p);
        BattleAnimClip.BonePose torsoP = sample(clip, "torso", p);

        switch (key != null ? key : "drone") {
            case "cleaner" -> {
                applyPart(cleanerBody, 0F, 0F, 0F, bodyP);
                applyPart(cleanerBrushL, 4.5F, 22F, -5F, sample(clip, "brush_l", p));
                applyPart(cleanerBrushR, -4.5F, 22F, -5F, sample(clip, "brush_r", p));
                cleanerBody.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                cleanerBrushL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                cleanerBrushR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
            }
            case "hound" -> {
                applyPart(houndTorso, 0F, 0F, 0F, bodyP);
                applyPart(houndJaw, 0F, 12F, -7F, sample(clip, "jaw", p));
                applyPart(houndLegFL, 3.5F, 16F, -5F, sample(clip, "leg_fl", p));
                applyPart(houndLegFR, -3.5F, 16F, -5F, sample(clip, "leg_fr", p));
                applyPart(houndLegBL, 3.5F, 16F, 5F, sample(clip, "leg_bl", p));
                applyPart(houndLegBR, -3.5F, 16F, 5F, sample(clip, "leg_br", p));
                applyPart(houndTail, 0F, 11F, 7F, sample(clip, "tail", p));
                houndTorso.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                houndJaw.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                houndLegFL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                houndLegFR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                houndLegBL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                houndLegBR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                houndTail.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
            }
            case "driller" -> {
                applyPart(drillerChassis, 0F, 0F, 0F, bodyP);
                applyPart(drillerDrill, 0F, 13F, -3F, sample(clip, "drill", p));
                drillerChassis.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                drillerDrill.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
            }
            case "guard" -> {
                applyPart(guardLegL, 3.2F, 12F, 0F, sample(clip, "leg_l", p));
                applyPart(guardLegR, -3.2F, 12F, 0F, sample(clip, "leg_r", p));
                applyPart(guardHead, 0F, 0F, 0F, sample(clip, "head", p));
                applyPart(guardBody, 0F, 0F, 0F, BattleAnimClip.BonePose.ZERO);
                applyPart(guardArmL, 6.5F, 1F, 0F, sample(clip, "arm_l", p));
                applyPart(guardArmR, -6.5F, 1F, 0F, sample(clip, "arm_r", p));

                guardLegL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                guardLegR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);

                ps.pushPose();
                ps.translate(torsoP.px() / 16.0F, (12.0F + torsoP.py()) / 16.0F, torsoP.pz() / 16.0F);
                ps.mulPose(Vector3f.ZP.rotationDegrees(torsoP.rzDeg()));
                ps.mulPose(Vector3f.YP.rotationDegrees(torsoP.ryDeg()));
                ps.mulPose(Vector3f.XP.rotationDegrees(torsoP.rxDeg()));
                ps.translate(0.0F, -12.0F / 16.0F, 0.0F);
                guardHead.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                guardBody.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                guardArmL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                guardArmR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                ps.popPose();
            }
            case "boss_arm_l" -> {
                applyPart(isolatedArmL, 0F, 6F, 0F, sample(clip, "arm_l", p));
                isolatedArmL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
            }
            case "boss_arm_r" -> {
                applyPart(isolatedArmR, 0F, 6F, 0F, sample(clip, "arm_r", p));
                isolatedArmR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
            }
            case "boss_core" -> {
                applyPart(bossLegL, 5.5F, 9F, 0F, sample(clip, "leg_l", p));
                applyPart(bossLegR, -5.5F, 9F, 0F, sample(clip, "leg_r", p));
                applyPart(bossHead, 0F, -9F, 0F, sample(clip, "head", p));
                applyPart(bossTorso, 0F, 0F, 0F, BattleAnimClip.BonePose.ZERO);
                applyPart(bossArmL, 10F, -6F, 0F, sample(clip, "arm_l", p));
                applyPart(bossArmR, -10F, -6F, 0F, sample(clip, "arm_r", p));

                bossLegL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                bossLegR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);

                ps.pushPose();
                ps.translate(torsoP.px() / 16.0F, (9.0F + torsoP.py()) / 16.0F, torsoP.pz() / 16.0F);
                ps.mulPose(Vector3f.ZP.rotationDegrees(torsoP.rzDeg()));
                ps.mulPose(Vector3f.YP.rotationDegrees(torsoP.ryDeg()));
                ps.mulPose(Vector3f.XP.rotationDegrees(torsoP.rxDeg()));
                ps.translate(0.0F, -9.0F / 16.0F, 0.0F);
                bossHead.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                bossTorso.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                bossArmL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                bossArmR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                ps.popPose();
            }
            default -> { // drone
                applyPart(droneBody, 0F, 0F, 0F, bodyP);
                applyPart(droneRing, 0F, 3F, 0F, sample(clip, "ring", p));
                applyPart(droneThrustL, 5.5F, 8F, 0F, sample(clip, "thrust_l", p));
                applyPart(droneThrustR, -5.5F, 8F, 0F, sample(clip, "thrust_r", p));
                droneBody.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                droneRing.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                droneThrustL.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
                droneThrustR.render(ps, vc, light, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
            }
        }
    }

    private static BattleAnimClip.BonePose sample(BattleAnimClip clip, String bone, float p) {
        return clip != null ? clip.sampleBone(bone, p) : BattleAnimClip.BonePose.ZERO;
    }

    private static void applyPart(ModelPart part, float defX, float defY, float defZ, BattleAnimClip.BonePose pose) {
        part.setPos(defX + pose.px(), defY + pose.py(), defZ + pose.pz());
        part.xRot = pose.rxDeg() * Mth.DEG_TO_RAD;
        part.yRot = pose.ryDeg() * Mth.DEG_TO_RAD;
        part.zRot = pose.rzDeg() * Mth.DEG_TO_RAD;
    }
}
