package com.resilienthope.core.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Renders the interactive 3D turn-based tactical stage inside TurnBasedBattleScreen:
 * - Articulated 3D humanoid characters holding sculpted 3D weapons
 * - Multi-part 3D mechanical enemies (cleaners, rotor drones, hounds, heavy guards, drillers, excavator boss)
 * - Choreographed dash-and-strike animations, elemental slash/beam/heal VFX, target lock rings, and floating numbers
 */
public class BattleStageRenderer {

    private static final ResourceLocation TEX_FLOOR_SHIP = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/block/starship_floor.png");
    private static final ResourceLocation TEX_FLOOR_ICE = new ResourceLocation(PickingHopeCore.MOD_ID, "textures/block/permafrost_rock.png");
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

    // Screen-space projected anchors updated each frame for click hitboxes & floating enemy HP cards
    public record ScreenAnchor(int screenX, int screenY, int topY, int hitRadius) {}

    public final List<ScreenAnchor> enemyAnchors = new ArrayList<>();
    public final List<ScreenAnchor> allyAnchors = new ArrayList<>();

    private record FloatingText(double stageX, double stageY, double stageZ, String label, int color, int age, int maxAge) {}
    private final List<FloatingText> popups = new ArrayList<>();

    private final Deque<BattleState.AnimEvent> animQueue = new ArrayDeque<>();
    private BattleState.AnimEvent currentAnim = null;
    private int animTick = 0;
    private int animDuration = 12;
    private boolean impactTriggered = false;
    private float shakeIntensity = 0f;
    private int globalTick = 0;

    // Baked 3D ModelPart trees (lazy initialized on client thread)
    private boolean modelsBuilt = false;
    private ModelPart unitCube;
    private ModelPart humanRoot, humanHead, humanHat, humanBody, humanRightArm, humanLeftArm, humanRightLeg, humanLeftLeg;
    private ModelPart weaponBlade, weaponRifle;
    private ModelPart cleanerRoot, cleanerBrushL, cleanerBrushR;
    private ModelPart droneRoot, droneRing, droneThrustL, droneThrustR;
    private ModelPart houndRoot, houndJaw, houndLegFL, houndLegFR, houndLegBL, houndLegBR, houndTail;
    private ModelPart guardRoot, guardArmL, guardArmR, guardLegL, guardLegR;
    private ModelPart drillerRoot, drillerDrill;
    private ModelPart bossRoot, bossHead, bossArmL, bossArmR, bossLegL, bossLegR;

    private void ensureModelsBuilt() {
        if (modelsBuilt) return;
        modelsBuilt = true;

        // 16x16x16 unit cube (1x1x1 block in ModelPart space)
        MeshDefinition cubeMesh = new MeshDefinition();
        cubeMesh.getRoot().addOrReplaceChild("cube",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.ZERO);
        this.unitCube = LayerDefinition.create(cubeMesh, 16, 16).bakeRoot().getChild("cube");

        // Humanoid Character Rig (64x64 standard player UV)
        MeshDefinition hm = new MeshDefinition();
        PartDefinition hp = hm.getRoot();
        hp.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.ZERO);
        hp.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0.45F)), PartPose.ZERO);
        hp.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F), PartPose.ZERO);
        hp.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3F, -2F, -2F, 4F, 12F, 4F), PartPose.offset(-5F, 2F, 0F));
        hp.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1F, -2F, -2F, 4F, 12F, 4F), PartPose.offset(5F, 2F, 0F));
        hp.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F), PartPose.offset(-1.9F, 12F, 0F));
        hp.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2F, 0F, -2F, 4F, 12F, 4F), PartPose.offset(1.9F, 12F, 0F));
        this.humanRoot = LayerDefinition.create(hm, 64, 64).bakeRoot();
        this.humanHead = humanRoot.getChild("head");
        this.humanHat = humanRoot.getChild("hat");
        this.humanBody = humanRoot.getChild("body");
        this.humanRightArm = humanRoot.getChild("right_arm");
        this.humanLeftArm = humanRoot.getChild("left_arm");
        this.humanRightLeg = humanRoot.getChild("right_leg");
        this.humanLeftLeg = humanRoot.getChild("left_leg");

        // Sculpted 3D Weapons
        MeshDefinition wbm = new MeshDefinition();
        wbm.getRoot().addOrReplaceChild("w", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1F, -2F, -1F, 2F, 5F, 2F)
                        .texOffs(0, 0).addBox(-2.5F, -3F, -2F, 5F, 2F, 4F)
                        .texOffs(0, 0).addBox(-1F, -18F, -1.5F, 2F, 15F, 3F),
                PartPose.ZERO);
        this.weaponBlade = LayerDefinition.create(wbm, 16, 16).bakeRoot().getChild("w");

        MeshDefinition wrm = new MeshDefinition();
        wrm.getRoot().addOrReplaceChild("w", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.5F, -14F, -1.5F, 3F, 14F, 3F)
                        .texOffs(0, 0).addBox(-1F, -10F, -4F, 2F, 5F, 2.5F)
                        .texOffs(0, 0).addBox(-1F, 0F, -1F, 2F, 4F, 2F),
                PartPose.ZERO);
        this.weaponRifle = LayerDefinition.create(wrm, 16, 16).bakeRoot().getChild("w");

        // 1. Cleaner Bot Rig (wide chassis + front bumper + twin rotating sweeper drums)
        MeshDefinition cm = new MeshDefinition();
        PartDefinition cp = cm.getRoot();
        cp.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6F, 12F, -6F, 12F, 8F, 12F)
                        .texOffs(0, 24).addBox(-7F, 17F, -7F, 14F, 4F, 14F)
                        .texOffs(32, 24).addBox(-2F, 7F, -2F, 4F, 5F, 4F),
                PartPose.ZERO);
        cp.addOrReplaceChild("brush_l", CubeListBuilder.create().texOffs(0, 44).addBox(-2F, -1F, -2F, 4F, 3F, 4F), PartPose.offset(4.5F, 22F, -5F));
        cp.addOrReplaceChild("brush_r", CubeListBuilder.create().texOffs(0, 44).addBox(-2F, -1F, -2F, 4F, 3F, 4F), PartPose.offset(-4.5F, 22F, -5F));
        this.cleanerRoot = LayerDefinition.create(cm, 64, 64).bakeRoot();
        this.cleanerBrushL = cleanerRoot.getChild("brush_l");
        this.cleanerBrushR = cleanerRoot.getChild("brush_r");

        // 2. Security Drone Rig (hovering octagonal core + rotating radar ring + twin thrusters)
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
        this.droneRoot = LayerDefinition.create(dm, 64, 64).bakeRoot();
        this.droneRing = droneRoot.getChild("ring");
        this.droneThrustL = droneRoot.getChild("tl");
        this.droneThrustR = droneRoot.getChild("tr");

        // 3. Mech Hound Rig (quadruped body + snapping head + 4 legs + blade tail)
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
        this.houndRoot = LayerDefinition.create(hnd, 64, 64).bakeRoot();
        this.houndJaw = houndRoot.getChild("jaw");
        this.houndLegFL = houndRoot.getChild("fl");
        this.houndLegFR = houndRoot.getChild("fr");
        this.houndLegBL = houndRoot.getChild("bl");
        this.houndLegBR = houndRoot.getChild("br");
        this.houndTail = houndRoot.getChild("tail");

        // 4. Heavy Guard / Bridge Commander Mech Rig (armored shoulders, left shield, right cannon)
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
        this.guardRoot = LayerDefinition.create(gm, 64, 64).bakeRoot();
        this.guardArmL = guardRoot.getChild("arml");
        this.guardArmR = guardRoot.getChild("armr");
        this.guardLegL = guardRoot.getChild("legl");
        this.guardLegR = guardRoot.getChild("legr");

        // 5. Mine Driller Rig (tracked chassis + spinning 3-stage front conical drill bit)
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
        this.drillerRoot = LayerDefinition.create(drm, 64, 64).bakeRoot();
        this.drillerDrill = drillerRoot.getChild("drill");

        // 6. Heavy Excavator Boss Rig
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
        this.bossRoot = LayerDefinition.create(bm, 64, 64).bakeRoot();
        this.bossHead = bossRoot.getChild("head");
        this.bossArmL = bossRoot.getChild("left_arm");
        this.bossArmR = bossRoot.getChild("right_arm");
        this.bossLegL = bossRoot.getChild("left_leg");
        this.bossLegR = bossRoot.getChild("right_leg");
    }

    public void enqueueEvents(List<BattleState.AnimEvent> incoming) {
        if (incoming == null || incoming.isEmpty()) return;
        animQueue.addAll(incoming);
    }

    public boolean isAnimating() {
        return currentAnim != null || !animQueue.isEmpty();
    }

    public BattleState.AnimEvent getCurrentAnim() {
        return currentAnim;
    }

    public void skipAllAnimations() {
        animQueue.clear();
        currentAnim = null;
        animTick = 0;
        shakeIntensity = 0f;
    }

    public void tick(BattleState state) {
        globalTick++;
        if (shakeIntensity > 0f) {
            shakeIntensity = Math.max(0f, shakeIntensity - 0.8f);
        }
        popups.removeIf(p -> p.age >= p.maxAge);
        for (int i = 0; i < popups.size(); i++) {
            FloatingText ft = popups.get(i);
            popups.set(i, new FloatingText(ft.stageX, ft.stageY, ft.stageZ, ft.label, ft.color, ft.age + 1, ft.maxAge));
        }

        if (currentAnim == null && !animQueue.isEmpty()) {
            currentAnim = animQueue.poll();
            animTick = 0;
            impactTriggered = false;
            animDuration = switch (currentAnim.animType) {
                case "ULT", "BOSS_AOE" -> 22;
                case "SKILL", "FOLLOWUP", "HEAL", "SHIELD", "BUFF", "BREAK" -> 15;
                default -> 12;
            };
        }

        if (currentAnim != null) {
            animTick++;
            float p = (float) animTick / Math.max(1, animDuration);
            float impactThreshold = "ULT".equals(currentAnim.animType) ? 0.48f : 0.36f;
            if (!impactTriggered && p >= impactThreshold) {
                impactTriggered = true;
                spawnImpactPopup(state, currentAnim);
                if ("ULT".equals(currentAnim.animType) || "BREAK".equals(currentAnim.animType) || "BOSS_AOE".equals(currentAnim.animType)) {
                    shakeIntensity = 4.5f;
                } else {
                    shakeIntensity = 2.0f;
                }
            }
            if (animTick >= animDuration) {
                currentAnim = null;
                animTick = 0;
            }
        }
    }

    private void spawnImpactPopup(BattleState state, BattleState.AnimEvent ev) {
        if (state == null) return;
        if ("HEAL".equals(ev.animType) || "SHIELD".equals(ev.animType) || "BUFF".equals(ev.animType)) {
            int col = "HEAL".equals(ev.animType) ? 0xFF4ADE80 : ("SHIELD".equals(ev.animType) ? 0xFFE2E8F0 : 0xFF38BDF8);
            String prefix = "BUFF".equals(ev.animType) ? "+25%攻 " : ("SHIELD".equals(ev.animType) ? "🛡+" : "♥+");
            int idx = ev.targetIdx >= 0 ? ev.targetIdx : ev.actorIdx;
            double[] pos = allyHomePos(idx, state.allies.size());
            popups.add(new FloatingText(pos[0], -1.75, pos[2], prefix + ev.value, col, 0, 28));
            return;
        }
        if (ev.actorSide == 1) {
            // Enemy hitting Ally
            if (ev.targetIdx >= 0 && ev.targetIdx < state.allies.size()) {
                double[] pos = allyHomePos(ev.targetIdx, state.allies.size());
                popups.add(new FloatingText(pos[0], -1.65, pos[2], "-" + ev.value, 0xFFEF4444, 0, 26));
            } else {
                for (int i = 0; i < state.allies.size(); i++) {
                    if (state.allies.get(i).isAlive()) {
                        double[] pos = allyHomePos(i, state.allies.size());
                        popups.add(new FloatingText(pos[0], -1.65, pos[2], "-" + ev.value, 0xFFEF4444, 0, 26));
                    }
                }
            }
        } else {
            // Ally hitting Enemy
            int col = ev.crit ? 0xFFFBBF24 : ("BREAK".equals(ev.animType) ? 0xFFFF8A00 : 0xFFFFFFFF);
            String txt = "BREAK".equals(ev.animType) ? ("💥 破盾 -" + ev.value)
                    : (ev.crit ? ("✦ " + ev.value + "!") : ("-" + ev.value));
            if (ev.targetIdx >= 0 && ev.targetIdx < state.enemies.size()) {
                double[] pos = enemyHomePos(ev.targetIdx, state.enemies.size());
                popups.add(new FloatingText(pos[0], -1.85, pos[2], txt, col, 0, 28));
            } else {
                for (int i = 0; i < state.enemies.size(); i++) {
                    if (state.enemies.get(i).isAlive()) {
                        double[] pos = enemyHomePos(i, state.enemies.size());
                        popups.add(new FloatingText(pos[0], -1.85, pos[2], txt, col, 0, 28));
                    }
                }
            }
        }
    }

    private double[] allyHomePos(int idx, int total) {
        double spread = Math.max(1.4, Math.min(2.1, 5.2 / Math.max(1, total)));
        double z = (idx - (total - 1) / 2.0) * spread;
        double x = -3.3 + Math.abs(idx - (total - 1) / 2.0) * 0.25;
        return new double[]{x, 0.0, z};
    }

    private double[] enemyHomePos(int idx, int total) {
        double spread = Math.max(1.5, Math.min(2.4, 5.6 / Math.max(1, total)));
        double z = (idx - (total - 1) / 2.0) * spread;
        double x = 3.2 - Math.abs(idx - (total - 1) / 2.0) * 0.25;
        return new double[]{x, 0.0, z};
    }

    public void renderStage(PoseStack uiStack, Font font, int screenW, int screenH,
                            BattleState state, int selectedEnemyIdx, float partialTicks) {
        if (state == null) return;
        ensureModelsBuilt();

        int centerX = screenW / 2;
        int floorScreenY = (int) (screenH * 0.59f);
        float pxPerBlock = Math.max(18.0f, Math.min(34.0f, Math.min(screenW * 0.068f, screenH * 0.115f)));

        float animP = currentAnim != null ? Mth.clamp((animTick + partialTicks) / Math.max(1, animDuration), 0f, 1f) : 0f;
        boolean isUltBanner = currentAnim != null && "ULT".equals(currentAnim.animType) && animP < 0.32f;

        float zoom = 1.0f;
        if (currentAnim != null) {
            if ("ULT".equals(currentAnim.animType)) zoom = 1.15f;
            else if ("SKILL".equals(currentAnim.animType) || "FOLLOWUP".equals(currentAnim.animType)) zoom = 1.07f;
        }
        float scale = pxPerBlock * zoom;

        float sx = 0f, sy = 0f;
        if (shakeIntensity > 0.1f) {
            sx = Mth.sin((globalTick + partialTicks) * 2.4f) * shakeIntensity;
            sy = Mth.cos((globalTick + partialTicks) * 3.1f) * shakeIntensity * 0.6f;
        }

        // Camera angle: isometric look-down (-18 deg pitch, +18 deg yaw so Ally is front-left, Enemy back-right)
        float camPitchDeg = -18.0f;
        float camYawDeg = 18.0f;

        // Clear depth before 3D pass so 3D geometry sorts purely against each other above background fill
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(256, Minecraft.ON_OSX);

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        PoseStack stage = new PoseStack();
        stage.translate(centerX + sx, floorScreenY + sy, 420.0D);
        stage.scale(scale, scale, scale);
        stage.mulPose(Vector3f.XP.rotationDegrees(camPitchDeg));
        stage.mulPose(Vector3f.YP.rotationDegrees(camYawDeg));

        // 1. Render 3D Tactical Arena Deck Tiles
        renderArenaFloor(stage, buffers, state.iceTheme);

        // 2. Render Ally 3D Units + Sculpted Weapons + Target Rings
        allyAnchors.clear();
        for (int i = 0; i < state.allies.size(); i++) {
            BattleState.AllyUnit ally = state.allies.get(i);
            double[] home = allyHomePos(i, state.allies.size());
            double[] cur = computeUnitAnimOffset(state, true, i, home, animP);
            boolean acting = (currentAnim != null && currentAnim.actorSide == 0 && currentAnim.actorIdx == i);
            boolean hit = (currentAnim != null && currentAnim.actorSide == 1 && (currentAnim.targetIdx == i || currentAnim.targetIdx == -1)
                    && animP > 0.34f && animP < 0.68f);

            int ringCol = (i == state.activeAllyIndex) ? 0xFFFBBF24 : CharacterData.getElementColor(ally.element);
            renderBaseRing(stage, buffers, home[0], home[2], 0.58f, ringCol, i == state.activeAllyIndex);

            renderAllyUnit(stage, buffers, ally, i, cur[0], cur[1], cur[2], acting, hit, animP, partialTicks);
            if (ally.shield > 0f && ally.isAlive()) {
                renderShieldDome(stage, buffers, cur[0], cur[1] - 0.85, cur[2], 0.72f);
            }
            ScreenAnchor anc = projectStageToScreen(cur[0], cur[1] - 1.5, cur[2], centerX + sx, floorScreenY + sy, scale, camPitchDeg, camYawDeg);
            allyAnchors.add(anc);
        }

        // 3. Render Enemy 3D Mech Units + Selection Rings
        enemyAnchors.clear();
        for (int i = 0; i < state.enemies.size(); i++) {
            BattleState.EnemyUnit enemy = state.enemies.get(i);
            double[] home = enemyHomePos(i, state.enemies.size());
            double[] cur = computeUnitAnimOffset(state, false, i, home, animP);
            boolean acting = (currentAnim != null && currentAnim.actorSide == 1 && currentAnim.actorIdx == i);
            boolean hit = (currentAnim != null && currentAnim.actorSide == 0 && (currentAnim.targetIdx == i || currentAnim.targetIdx == -1)
                    && animP > 0.32f && animP < 0.68f);

            boolean selected = (i == selectedEnemyIdx && enemy.isAlive());
            if (enemy.isAlive()) {
                renderBaseRing(stage, buffers, home[0], home[2], enemy.isBossCore ? 0.95f : 0.62f,
                        selected ? 0xFFFBBF24 : (enemy.chargingUlt ? 0xFFFF4444 : 0xFFEF4444), selected);
                renderEnemyUnit(stage, buffers, enemy, cur[0], cur[1], cur[2], acting, hit, animP, partialTicks);
            }
            ScreenAnchor anc = projectStageToScreen(cur[0], cur[1] - (enemy.isBossCore ? 2.1 : 1.55), cur[2],
                    centerX + sx, floorScreenY + sy, scale, camPitchDeg, camYawDeg);
            enemyAnchors.add(anc);
        }

        // 4. Render 3D Action VFX (Slashes, Beams, Whirlwinds, Water Rings, Break Shockwaves)
        if (currentAnim != null && animP >= 0.25f) {
            renderActionVfx(stage, buffers, state, currentAnim, animP);
        }

        buffers.endBatch();
        Lighting.setupFor3DItems();
        RenderSystem.disableDepthTest();

        // 5. Render Floating Damage / Heal Numbers in crisp 2D over the 3D scene
        for (FloatingText ft : popups) {
            float prog = (float) ft.age / Math.max(1, ft.maxAge);
            ScreenAnchor p = projectStageToScreen(ft.stageX, ft.stageY - prog * 1.15, ft.stageZ,
                    centerX + sx, floorScreenY + sy, scale, camPitchDeg, camYawDeg);
            uiStack.pushPose();
            float s = ft.label.contains("✦") || ft.label.contains("💥") ? 1.25f : 1.0f;
            uiStack.translate(p.screenX, p.screenY, 0);
            uiStack.scale(s, s, 1.0f);
            font.drawShadow(uiStack, new TextComponent(ft.label), -font.width(ft.label) / 2.0f, 0, ft.color);
            uiStack.popPose();
        }

        // 6. Ultimate Cut-In Portrait Banner (first 32% of Ult animation)
        if (isUltBanner) {
            renderUltCutInBanner(uiStack, font, screenW, screenH, currentAnim, animP / 0.32f);
        }
    }

    private double[] computeUnitAnimOffset(BattleState state, boolean isAlly, int idx, double[] home, float p) {
        if (currentAnim == null) return new double[]{home[0], home[1], home[2]};
        boolean thisIsActor = (isAlly && currentAnim.actorSide == 0 && currentAnim.actorIdx == idx)
                || (!isAlly && currentAnim.actorSide == 1 && currentAnim.actorIdx == idx);

        if (thisIsActor) {
            // Healers/Buffers/Givers step forward slightly rather than lunging into melee
            boolean isSupport = "HEAL".equals(currentAnim.animType) || "SHIELD".equals(currentAnim.animType) || "BUFF".equals(currentAnim.animType);
            double[] dest;
            if (isSupport) {
                dest = new double[]{home[0] + 1.1, home[1], home[2]};
            } else if (currentAnim.targetIdx < 0) {
                dest = new double[]{isAlly ? 0.5 : -0.5, home[1], 0.0};
            } else if (isAlly) {
                double[] t = enemyHomePos(Math.min(currentAnim.targetIdx, Math.max(0, state.enemies.size() - 1)), Math.max(1, state.enemies.size()));
                dest = new double[]{t[0] - 1.35, t[1], t[2]};
            } else {
                double[] t = allyHomePos(Math.min(currentAnim.targetIdx, Math.max(0, state.allies.size() - 1)), Math.max(1, state.allies.size()));
                dest = new double[]{t[0] + 1.35, t[1], t[2]};
            }

            float moveFactor;
            if (p < 0.30f) {
                moveFactor = easeOutCubic(p / 0.30f);
            } else if (p < 0.65f) {
                moveFactor = 1.0f;
            } else {
                moveFactor = 1.0f - easeOutCubic((p - 0.65f) / 0.35f);
            }
            double jumpArc = (p > 0.25f && p < 0.68f && !"BASIC".equals(currentAnim.animType)) ? -Math.sin((p - 0.25f) / 0.43f * Math.PI) * 0.45 : 0.0;
            return new double[]{
                    Mth.lerp(moveFactor, home[0], dest[0]),
                    home[1] + jumpArc,
                    Mth.lerp(moveFactor, home[2], dest[2])
            };
        }

        // Flinch backward on hit
        boolean isVictim = (isAlly && currentAnim.actorSide == 1 && (currentAnim.targetIdx == idx || currentAnim.targetIdx == -1))
                || (!isAlly && currentAnim.actorSide == 0 && (currentAnim.targetIdx == idx || currentAnim.targetIdx == -1));
        if (isVictim && p > 0.34f && p < 0.66f) {
            float hitK = (float) Math.sin(((p - 0.34f) / 0.32f) * Math.PI);
            return new double[]{home[0] + (isAlly ? -0.35 : 0.35) * hitK, home[1], home[2]};
        }
        return new double[]{home[0], home[1], home[2]};
    }

    private void renderArenaFloor(PoseStack stage, MultiBufferSource.BufferSource buffers, boolean iceTheme) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(iceTheme ? TEX_FLOOR_ICE : TEX_FLOOR_SHIP));
        float r = iceTheme ? 0.85f : 0.90f;
        float g = iceTheme ? 0.95f : 0.94f;
        float b = 1.0f;
        for (int tx = -5; tx <= 5; tx++) {
            for (int tz = -3; tz <= 3; tz++) {
                stage.pushPose();
                stage.translate(tx * 1.05f, 0.10f, tz * 1.05f);
                stage.scale(1.0F, 0.20F, 1.0F);
                unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
                stage.popPose();
            }
        }
    }

    private void renderBaseRing(PoseStack stage, MultiBufferSource.BufferSource buffers,
                                double x, double z, float radius, int argbCol, boolean activeHighlight) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX_FX));
        float r = ((argbCol >> 16) & 0xFF) / 255.0f;
        float g = ((argbCol >> 8) & 0xFF) / 255.0f;
        float b = (argbCol & 0xFF) / 255.0f;
        float spin = (globalTick % 360) * 2.0f;
        float pulse = activeHighlight ? (1.0f + 0.10f * Mth.sin(globalTick * 0.25f)) : 1.0f;

        stage.pushPose();
        stage.translate(x, -0.02, z);
        stage.mulPose(Vector3f.YP.rotationDegrees(activeHighlight ? spin : 45.0f));
        stage.scale(radius * 2.0f * pulse, 0.04f, 0.14f);
        unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, activeHighlight ? 0.85f : 0.45f);
        stage.popPose();

        stage.pushPose();
        stage.translate(x, -0.02, z);
        stage.mulPose(Vector3f.YP.rotationDegrees((activeHighlight ? spin : 45.0f) + 90.0f));
        stage.scale(radius * 2.0f * pulse, 0.04f, 0.14f);
        unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, activeHighlight ? 0.85f : 0.45f);
        stage.popPose();
    }

    private void renderAllyUnit(PoseStack stage, MultiBufferSource.BufferSource buffers, BattleState.AllyUnit ally,
                                int slotIdx, double x, double y, double z, boolean acting, boolean hit, float animP, float partialTicks) {
        ResourceLocation skin = switch (ally.charId != null ? ally.charId : "") {
            case "chenfeng" -> TEX_CHAR_CHEN;
            case "suqing" -> TEX_CHAR_SU;
            case "laozheng" -> TEX_CHAR_ZHENG;
            case "hanbing" -> TEX_CHAR_HAN;
            case "zhoumin" -> TEX_CHAR_ZHOU;
            default -> TEX_CHAR_LINYE;
        };

        float t = globalTick + partialTicks;
        float breath = ally.isAlive() ? Mth.sin(t * 0.14f + slotIdx) * 0.06f : 0f;
        float swing = acting && animP > 0.28f && animP < 0.65f
                ? Mth.sin(((animP - 0.28f) / 0.37f) * (float) Math.PI) * 2.1f
                : 0f;

        this.humanHead.setPos(0F, 0F, 0F);
        this.humanHat.setPos(0F, 0F, 0F);
        this.humanRightArm.xRot = -0.35f - swing + breath;
        this.humanLeftArm.xRot = 0.25f + breath * 0.8f;
        this.humanRightArm.zRot = acting && animP > 0.28f ? -0.35f : 0.12f;
        this.humanLeftArm.zRot = -0.12f;
        this.humanRightLeg.xRot = acting && animP < 0.30f ? Mth.sin(t * 0.9f) * 0.7f : -0.1f;
        this.humanLeftLeg.xRot = acting && animP < 0.30f ? -Mth.sin(t * 0.9f) * 0.7f : 0.1f;

        if (!ally.isAlive()) {
            this.humanRoot.zRot = (float) (Math.PI / 2.0);
        } else {
            this.humanRoot.zRot = 0f;
        }

        stage.pushPose();
        stage.translate(x, y - (ally.isAlive() ? 1.50 : 0.35), z);
        stage.mulPose(Vector3f.YP.rotationDegrees(-105.0F)); // Face right (+X) slightly turned toward camera
        if ("FOLLOWUP".equals(currentAnim != null ? currentAnim.animType : "") && acting) {
            stage.mulPose(Vector3f.YP.rotationDegrees(animP * 720.0f));
        }

        float cr = hit ? 1.0f : (ally.isAlive() ? 1.0f : 0.45f);
        float cg = hit ? 0.35f : (ally.isAlive() ? 1.0f : 0.45f);
        float cb = hit ? 0.35f : (ally.isAlive() ? 1.0f : 0.45f);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
        this.humanRoot.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);

        // Attach Sculpted 3D Weapon to Right Hand Pivot
        if (ally.isAlive()) {
            VertexConsumer wvc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_FX));
            stage.pushPose();
            stage.translate(-5.0 / 16.0, 2.0 / 16.0, 0.0);
            stage.mulPose(Vector3f.ZP.rotation(this.humanRightArm.zRot));
            stage.mulPose(Vector3f.XP.rotation(this.humanRightArm.xRot));
            stage.translate(0.0, 9.5 / 16.0, -1.5 / 16.0);
            stage.mulPose(Vector3f.XP.rotationDegrees(72.0F));

            int elemCol = CharacterData.getElementColor(ally.element);
            float wr = ((elemCol >> 16) & 0xFF) / 255.0f;
            float wg = ((elemCol >> 8) & 0xFF) / 255.0f;
            float wb = (elemCol & 0xFF) / 255.0f;

            boolean usesGun = "suqing".equals(ally.charId) || "hanbing".equals(ally.charId) || "zhoumin".equals(ally.charId);
            ModelPart weaponModel = usesGun ? this.weaponRifle : this.weaponBlade;
            weaponModel.render(stage, wvc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    Math.min(1f, wr + 0.25f), Math.min(1f, wg + 0.25f), Math.min(1f, wb + 0.25f), 1.0F);
            stage.popPose();
        }
        stage.popPose();
    }

    private void renderEnemyUnit(PoseStack stage, MultiBufferSource.BufferSource buffers, BattleState.EnemyUnit enemy,
                                 double x, double y, double z, boolean acting, boolean hit, float animP, float partialTicks) {
        float t = globalTick + partialTicks;
        String id = enemy.id != null ? enemy.id : "";
        ResourceLocation tex;
        float eScale = 1.0f;
        if (id.contains("cleaner")) {
            tex = TEX_CLEANER;
        } else if (id.contains("hound")) {
            tex = TEX_HOUND;
            eScale = 1.05f;
        } else if (id.contains("driller")) {
            tex = TEX_DRILLER;
            eScale = 1.25f;
        } else if (id.contains("guard") || id.contains("cmd") || id.contains("trial")) {
            tex = TEX_GUARD;
            eScale = 1.18f;
        } else if (enemy.isBossCore || enemy.isBossArm) {
            tex = TEX_BOSS;
            eScale = enemy.isBossCore ? 1.48f : 1.05f;
        } else {
            tex = TEX_DRONE;
        }

        float cr = hit ? 1.0f : (enemy.broken ? 0.65f : 1.0f);
        float cg = hit ? 0.30f : (enemy.broken ? 0.72f : 1.0f);
        float cb = hit ? 0.30f : (enemy.broken ? 0.90f : 1.0f);
        if (enemy.chargingUlt && !hit && (globalTick % 10 < 5)) {
            cr = 1.0f; cg = 0.55f; cb = 0.45f;
        }

        float lungeTilt = acting && animP > 0.30f && animP < 0.62f ? -0.32f : (enemy.broken ? 0.28f : 0f);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));

        stage.pushPose();
        stage.translate(x, y - 1.50 * eScale, z);
        stage.scale(eScale, eScale, eScale);
        stage.mulPose(Vector3f.YP.rotationDegrees(105.0F)); // Face left (-X) toward Ally
        stage.mulPose(Vector3f.XP.rotation(lungeTilt));

        if (id.contains("cleaner")) {
            this.cleanerBrushL.yRot = t * 0.45f;
            this.cleanerBrushR.yRot = -t * 0.45f;
            this.cleanerRoot.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        } else if (id.contains("hound")) {
            this.houndJaw.xRot = acting ? Mth.sin(t * 0.8f) * 0.35f : Mth.sin(t * 0.12f) * 0.1f;
            float stride = acting ? Mth.sin(t * 0.9f) * 0.55f : 0.1f;
            this.houndLegFL.xRot = stride;
            this.houndLegBR.xRot = stride;
            this.houndLegFR.xRot = -stride;
            this.houndLegBL.xRot = -stride;
            this.houndTail.yRot = Mth.sin(t * 0.25f) * 0.35f;
            this.houndRoot.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        } else if (id.contains("driller")) {
            this.drillerDrill.zRot = t * 0.48f;
            this.drillerRoot.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        } else if (id.contains("guard") || id.contains("cmd") || id.contains("trial")) {
            this.guardArmR.xRot = -0.35f + (acting ? -0.8f * Mth.sin(animP * (float) Math.PI) : Mth.sin(t * 0.1f) * 0.08f);
            this.guardArmL.xRot = -0.25f;
            this.guardLegL.xRot = Mth.sin(t * 0.15f) * 0.06f;
            this.guardLegR.xRot = -this.guardLegL.xRot;
            this.guardRoot.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        } else if (enemy.isBossArm) {
            ModelPart singleArm = id.contains("left") ? this.bossArmL : this.bossArmR;
            singleArm.xRot = -0.45f + Mth.sin(t * 0.12f) * 0.15f;
            stage.translate(id.contains("left") ? -0.45 : 0.45, 0.15, 0.0);
            singleArm.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        } else if (enemy.isBossCore) {
            this.bossHead.yRot = Mth.sin(t * 0.08f) * 0.2f;
            this.bossArmL.xRot = acting ? -1.1f * Mth.sin(animP * (float) Math.PI) : Mth.sin(t * 0.1f) * 0.14f;
            this.bossArmR.xRot = -this.bossArmL.xRot;
            this.bossRoot.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        } else {
            float hoverY = enemy.broken ? 0.35f : Mth.sin(t * 0.18f) * 0.15f;
            stage.translate(0.0, hoverY, 0.0);
            this.droneRing.yRot = t * 0.22f;
            this.droneThrustL.xRot = 0.2f + Mth.cos(t * 0.25f) * 0.15f;
            this.droneThrustR.xRot = this.droneThrustL.xRot;
            this.droneRoot.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cr, cg, cb, 1.0F);
        }
        stage.popPose();
    }

    private void renderShieldDome(PoseStack stage, MultiBufferSource.BufferSource buffers,
                                  double x, double y, double z, float radius) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX_FX));
        stage.pushPose();
        stage.translate(x, y, z);
        stage.mulPose(Vector3f.YP.rotationDegrees((globalTick % 360) * 1.5f));
        stage.scale(radius * 1.6f, 1.35f, radius * 1.6f);
        unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.35f, 0.82f, 1.0f, 0.22f);
        stage.popPose();
    }

    private void renderActionVfx(PoseStack stage, MultiBufferSource.BufferSource buffers,
                                 BattleState state, BattleState.AnimEvent ev, float p) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX_FX));
        int elemCol = CharacterData.getElementColor(ev.element);
        float r = ((elemCol >> 16) & 0xFF) / 255.0f;
        float g = ((elemCol >> 8) & 0xFF) / 255.0f;
        float b = (elemCol & 0xFF) / 255.0f;
        float alpha = Mth.clamp(1.0f - Math.abs(p - 0.50f) * 2.2f, 0.1f, 0.9f);

        if ("HEAL".equals(ev.animType) || "SHIELD".equals(ev.animType) || "BUFF".equals(ev.animType)) {
            int col = "HEAL".equals(ev.animType) ? 0xFF4ADE80 : ("SHIELD".equals(ev.animType) ? 0xFFFBBF24 : 0xFF38BDF8);
            float hr = ((col >> 16) & 0xFF) / 255.0f;
            float hg = ((col >> 8) & 0xFF) / 255.0f;
            float hb = (col & 0xFF) / 255.0f;
            for (int i = 0; i < state.allies.size(); i++) {
                if (!state.allies.get(i).isAlive()) continue;
                double[] pos = allyHomePos(i, state.allies.size());
                stage.pushPose();
                stage.translate(pos[0], -0.25 - p * 1.1, pos[2]);
                stage.mulPose(Vector3f.YP.rotationDegrees(p * 360f));
                stage.scale(1.1f + p * 0.4f, 0.08f, 1.1f + p * 0.4f);
                unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, hr, hg, hb, alpha);
                stage.popPose();
            }
            return;
        }

        // Target positions for offensive slashes/bursts/beams
        List<double[]> hitTargets = new ArrayList<>();
        if (ev.targetIdx >= 0) {
            if (ev.actorSide == 0 && ev.targetIdx < state.enemies.size()) {
                hitTargets.add(enemyHomePos(ev.targetIdx, state.enemies.size()));
            } else if (ev.actorSide == 1 && ev.targetIdx < state.allies.size()) {
                hitTargets.add(allyHomePos(ev.targetIdx, state.allies.size()));
            }
        } else {
            if (ev.actorSide == 0) {
                for (int i = 0; i < state.enemies.size(); i++) {
                    if (state.enemies.get(i).isAlive()) hitTargets.add(enemyHomePos(i, state.enemies.size()));
                }
            } else {
                for (int i = 0; i < state.allies.size(); i++) {
                    if (state.allies.get(i).isAlive()) hitTargets.add(allyHomePos(i, state.allies.size()));
                }
            }
        }

        // Cross slash blades + expanding impact shockwave cube
        for (double[] t : hitTargets) {
            stage.pushPose();
            stage.translate(t[0], -0.85, t[2]);
            stage.mulPose(Vector3f.ZP.rotationDegrees(45.0f + p * 140.0f));
            stage.scale(0.14f + p * 0.25f, 1.85f * (0.5f + p), 0.14f);
            unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
            stage.popPose();

            if ("ULT".equals(ev.animType) || "BREAK".equals(ev.animType) || "FOLLOWUP".equals(ev.animType) || "BOSS_AOE".equals(ev.animType)) {
                stage.pushPose();
                stage.translate(t[0], -0.85, t[2]);
                stage.mulPose(Vector3f.ZP.rotationDegrees(-45.0f - p * 120.0f));
                stage.scale(0.16f, 2.2f * (0.5f + p), 0.16f);
                unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 0.95f, 0.65f, alpha);
                stage.popPose();

                stage.pushPose();
                stage.translate(t[0], -0.1, t[2]);
                float ringS = 0.6f + p * 2.2f;
                stage.scale(ringS, 0.08f, ringS);
                unitCube.render(stage, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha * 0.65f);
                stage.popPose();
            }
        }
    }

    private void renderUltCutInBanner(PoseStack ps, Font font, int width, int height,
                                      BattleState.AnimEvent ev, float cutP) {
        float slide = easeOutCubic(Math.min(1f, cutP * 2.0f));
        int bandH = 62;
        int bandY = height / 2 - bandH / 2 - 12;
        int elemCol = CharacterData.getElementColor(ev.element);

        GuiComponent.fill(ps, 0, bandY, width, bandY + bandH, 0xDF07101C);
        GuiComponent.fill(ps, 0, bandY, width, bandY + 2, elemCol);
        GuiComponent.fill(ps, 0, bandY + bandH - 2, width, bandY + bandH, 0xFFFBBF24);

        int px = (int) (Mth.lerp(slide, -70.0f, width / 2.0f - 105.0f));
        GenshinHudOverlay.drawVoxelPanel(ps, px, bandY + 5, 52, 52, 0xFF0A1422, elemCol, 0xFFFBBF24);
        GenshinHudOverlay.drawPortrait(ps, TurnBasedBattleScreen.getPortraitForChar(ev.actorId), px + 2, bandY + 7, 48, 48);

        int tx = (int) (Mth.lerp(slide, width + 30.0f, px + 62.0f));
        font.drawShadow(ps, new TextComponent("★ 终结技爆发 · " + ev.actorName), tx, bandY + 12, elemCol);
        ps.pushPose();
        ps.translate(tx, bandY + 28, 0);
        ps.scale(1.5f, 1.5f, 1.0f);
        font.drawShadow(ps, new TextComponent("【" + ev.skillTitle + "】"), 0, 0, 0xFFFBBF24);
        ps.popPose();
    }

    private ScreenAnchor projectStageToScreen(double sx, double sy, double sz,
                                              float centerX, float floorScreenY, float scale,
                                              float pitchDeg, float yawDeg) {
        double yawRad = Math.toRadians(yawDeg);
        double cosY = Math.cos(yawRad), sinY = Math.sin(yawRad);
        double x1 = sx * cosY + sz * sinY;
        double z1 = -sx * sinY + sz * cosY;

        double pitchRad = Math.toRadians(pitchDeg);
        double cosP = Math.cos(pitchRad), sinP = Math.sin(pitchRad);
        double y2 = sy * cosP - z1 * sinP;

        int outX = Math.round(centerX + (float) x1 * scale);
        int outY = Math.round(floorScreenY + (float) y2 * scale);
        int topY = Math.max(24, outY);
        int rad = Math.max(18, Math.round(scale * 0.72f));
        return new ScreenAnchor(outX, outY, topY, rad);
    }

    private float easeOutCubic(float t) {
        float inv = 1.0f - Mth.clamp(t, 0f, 1f);
        return 1.0f - inv * inv * inv;
    }
}
