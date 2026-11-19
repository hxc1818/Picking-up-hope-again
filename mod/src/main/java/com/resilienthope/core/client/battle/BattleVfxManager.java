package com.resilienthope.core.client.battle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.combat.BattleArenaLayout;
import com.resilienthope.core.combat.BattleCombatPreview;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages real-world 3D combat VFX geometry, native Minecraft particles,
 * tactical floor rings, shield domes, sound triggers, and world-anchored floating popups.
 */
public final class BattleVfxManager {
    private static final ResourceLocation TEX_FX =
            new ResourceLocation(PickingHopeCore.MOD_ID, "textures/block/starship_light.png");

    public static final class FloatingPopup {
        public final Vec3 worldPos;
        public final String text;
        public final int color;
        public int age;
        public final int maxAge;
        public final boolean big;

        public FloatingPopup(Vec3 worldPos, String text, int color, int maxAge, boolean big) {
            this.worldPos = worldPos;
            this.text = text;
            this.color = color;
            this.age = 0;
            this.maxAge = Math.max(1, maxAge);
            this.big = big;
        }
    }

    private static final class ActiveVfx {
        final String style;
        final Vec3 origin;
        final Vec3 target;
        final float scale;
        final float r, g, b;
        int age = 0;
        final int maxAge;

        ActiveVfx(String style, Vec3 origin, Vec3 target, int argbCol, float scale, int maxAge) {
            this.style = style != null ? style : "slash_arc";
            this.origin = origin;
            this.target = target;
            this.r = ((argbCol >> 16) & 0xFF) / 255.0F;
            this.g = ((argbCol >> 8) & 0xFF) / 255.0F;
            this.b = (argbCol & 0xFF) / 255.0F;
            this.scale = Math.max(0.4F, scale);
            this.maxAge = Math.max(4, maxAge);
        }
    }

    private static final List<ActiveVfx> ACTIVE_VFX = new ArrayList<>();
    private static final List<FloatingPopup> POPUPS = new ArrayList<>();
    private static final Random RNG = new Random();
    private static ModelPart unitCube;
    private static int tickCounter = 0;

    private BattleVfxManager() {}

    public static void clear() {
        ACTIVE_VFX.clear();
        POPUPS.clear();
    }

    public static void tick() {
        tickCounter++;
        ACTIVE_VFX.removeIf(v -> ++v.age >= v.maxAge);
        POPUPS.removeIf(p -> ++p.age >= p.maxAge);
    }

    public static List<FloatingPopup> getPopups() {
        return POPUPS;
    }

    public static void addPopup(Vec3 worldPos, String text, int color, int maxAge, boolean big) {
        if (worldPos == null || text == null || text.isEmpty()) return;
        double jx = (RNG.nextDouble() - 0.5) * 0.35;
        double jz = (RNG.nextDouble() - 0.5) * 0.35;
        POPUPS.add(new FloatingPopup(worldPos.add(jx, 0.0, jz), text, color, maxAge, big));
    }

    public static void spawnVfx(String style, Vec3 origin, Vec3 target, String element, float scale) {
        if (style == null || style.isEmpty() || target == null) return;
        int col = CharacterData.getElementColor(element != null ? element : "EARTH");
        if ("heal_pillar".equals(style)) col = 0xFF4ADE80;
        else if ("shield_dome".equals(style)) col = 0xFFFBBF24;
        else if ("buff_aura".equals(style)) col = 0xFF38BDF8;
        int dur = style.contains("beam") || style.contains("vortex") || style.contains("pillar") ? 18 : 13;
        ACTIVE_VFX.add(new ActiveVfx(style, origin != null ? origin : target, target, col, scale, dur));
    }

    public static void spawnParticles(ClientLevel level, Vec3 pos, String particleKey, int count, float spread) {
        if (level == null || pos == null || particleKey == null || particleKey.isEmpty()) return;
        ParticleOptions type = resolveParticle(particleKey);
        for (int i = 0; i < Math.max(1, count); i++) {
            double px = pos.x + (RNG.nextDouble() - 0.5) * 2.0 * spread;
            double py = pos.y + 0.6 + (RNG.nextDouble() - 0.2) * 1.3 * spread;
            double pz = pos.z + (RNG.nextDouble() - 0.5) * 2.0 * spread;
            double vx = (RNG.nextDouble() - 0.5) * 0.16;
            double vy = 0.05 + RNG.nextDouble() * 0.14;
            double vz = (RNG.nextDouble() - 0.5) * 0.16;
            level.addParticle(type, px, py, pz, vx, vy, vz);
        }
    }

    public static void playSound(Minecraft mc, String soundKey, float volume, float pitch) {
        if (mc == null || mc.level == null || soundKey == null || soundKey.isEmpty()) return;
        SoundEvent sound = resolveSound(soundKey);
        if (sound == null) return;
        Vec3 c = mc.gameRenderer.getMainCamera().getPosition();
        mc.level.playLocalSound(c.x, c.y, c.z, sound, SoundSource.MASTER, volume, pitch, false);
    }

    private static ParticleOptions resolveParticle(String key) {
        return switch (key) {
            case "sweep" -> ParticleTypes.SWEEP_ATTACK;
            case "crit" -> ParticleTypes.CRIT;
            case "enchanted_hit" -> ParticleTypes.ENCHANTED_HIT;
            case "flame" -> ParticleTypes.FLAME;
            case "lava" -> ParticleTypes.LAVA;
            case "soul_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "explosion" -> ParticleTypes.EXPLOSION;
            case "smoke" -> ParticleTypes.LARGE_SMOKE;
            case "splash" -> ParticleTypes.SPLASH;
            case "bubble" -> ParticleTypes.BUBBLE;
            case "heart" -> ParticleTypes.HEART;
            case "cloud" -> ParticleTypes.CLOUD;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "electric" -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.END_ROD;
        };
    }

    private static SoundEvent resolveSound(String key) {
        return switch (key) {
            case "sweep" -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case "crit" -> SoundEvents.PLAYER_ATTACK_CRIT;
            case "strong_hit" -> SoundEvents.PLAYER_ATTACK_STRONG;
            case "explode" -> SoundEvents.GENERIC_EXPLODE;
            case "shield_block" -> SoundEvents.SHIELD_BLOCK;
            case "shield_break" -> SoundEvents.SHIELD_BREAK;
            case "fire" -> SoundEvents.FIRECHARGE_USE;
            case "blaze" -> SoundEvents.BLAZE_SHOOT;
            case "shot" -> SoundEvents.ARROW_HIT;
            case "blast" -> SoundEvents.FIREWORK_ROCKET_BLAST;
            case "anvil" -> SoundEvents.ANVIL_LAND;
            case "beacon" -> SoundEvents.BEACON_POWER_SELECT;
            case "heal" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "lightning" -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case "mech_slam" -> SoundEvents.IRON_GOLEM_ATTACK;
            case "mech_hurt" -> SoundEvents.IRON_GOLEM_HURT;
            case "bite" -> SoundEvents.PHANTOM_BITE;
            case "piston" -> SoundEvents.PISTON_EXTEND;
            case "wither_shoot" -> SoundEvents.WITHER_SHOOT;
            default -> null;
        };
    }

    private static void ensureCube() {
        if (unitCube != null) return;
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.ZERO);
        unitCube = LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("cube");
    }

    public static void renderWorldVfx(PoseStack ps, Vec3 camPos, BattleState state,
                                      BattleCombatPreview.PreviewResult preview, boolean cinematicActive,
                                      float partialTicks) {
        ensureCube();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX_FX));

        if (state != null && !state.finished) {
            // Render ally shield domes & tactical selection markers on hangar floor
            for (int i = 0; i < state.allies.size(); i++) {
                BattleState.AllyUnit a = state.allies.get(i);
                if (!a.isAlive()) continue;
                Vec3 home = BattleArenaLayout.allyHomePos(i, state.allies.size());
                if (a.shield > 0f) {
                    renderShieldAura(ps, vc, camPos, home, partialTicks);
                }
                if (!cinematicActive) {
                    if (preview != null && preview.allyHighlights.contains(i)) {
                        renderFloorMarker(ps, vc, camPos, home, 0.72f, 0xFF38F2B0, true, partialTicks);
                    } else if (i == state.activeAllyIndex) {
                        renderFloorMarker(ps, vc, camPos, home, 0.65f, 0xFF55B8FF, true, partialTicks);
                    }
                }
            }

            if (!cinematicActive && preview != null) {
                for (int i = 0; i < state.enemies.size(); i++) {
                    BattleState.EnemyUnit e = state.enemies.get(i);
                    if (!e.isAlive()) continue;
                    BattleCombatPreview.TargetHighlight hl = preview.enemyHighlights.getOrDefault(i, BattleCombatPreview.TargetHighlight.NONE);
                    if (hl == BattleCombatPreview.TargetHighlight.PRIMARY_ENEMY) {
                        Vec3 home = BattleArenaLayout.enemyHomePos(i, state.enemies.size());
                        float r = e.isBossCore ? 1.15f : 0.78f;
                        renderFloorMarker(ps, vc, camPos, home, r, 0xFFFFD700, true, partialTicks);
                    } else if (hl == BattleCombatPreview.TargetHighlight.SECONDARY_ENEMY) {
                        Vec3 home = BattleArenaLayout.enemyHomePos(i, state.enemies.size());
                        float r = e.isBossCore ? 1.05f : 0.72f;
                        renderFloorMarker(ps, vc, camPos, home, r, 0xFFFF5533, false, partialTicks);
                    }
                }
            }
        }

        // Render active 3D attack/skill/ult VFX
        for (ActiveVfx vfx : ACTIVE_VFX) {
            float p = Mth.clamp((vfx.age + partialTicks) / vfx.maxAge, 0f, 1f);
            renderSingleVfx(ps, vc, camPos, vfx, p);
        }

        buffers.endBatch();
    }

    private static void renderFloorMarker(PoseStack ps, VertexConsumer vc, Vec3 camPos, Vec3 worldPos,
                                          float radius, int argb, boolean spin, float partialTicks) {
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float t = tickCounter + partialTicks;
        float pulse = spin ? (1.0f + 0.08f * Mth.sin(t * 0.25f)) : 1.0f;
        float angle = spin ? (t * 2.5f) % 360.0f : 45.0f;

        for (int pass = 0; pass < 2; pass++) {
            ps.pushPose();
            ps.translate(worldPos.x - camPos.x, worldPos.y + 0.04 - camPos.y, worldPos.z - camPos.z);
            ps.mulPose(Vector3f.YP.rotationDegrees(angle + pass * 90.0f));
            ps.scale(radius * 1.9f * pulse, 0.04f, 0.14f);
            unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, spin ? 0.78f : 0.55f);
            ps.popPose();
        }
    }

    private static void renderShieldAura(PoseStack ps, VertexConsumer vc, Vec3 camPos, Vec3 worldPos, float partialTicks) {
        float angle = ((tickCounter + partialTicks) * 1.6f) % 360.0f;
        ps.pushPose();
        ps.translate(worldPos.x - camPos.x, worldPos.y + 0.95 - camPos.y, worldPos.z - camPos.z);
        ps.mulPose(Vector3f.YP.rotationDegrees(angle));
        ps.scale(1.25f, 1.75f, 1.25f);
        unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.35f, 0.85f, 1.0f, 0.18f);
        ps.popPose();
    }

    private static void renderSingleVfx(PoseStack ps, VertexConsumer vc, Vec3 camPos, ActiveVfx v, float p) {
        float alpha = Mth.clamp(1.0f - Math.abs(p - 0.38f) * 1.55f, 0.08f, 0.92f);
        double tx = v.target.x - camPos.x;
        double ty = v.target.y + 1.0 - camPos.y;
        double tz = v.target.z - camPos.z;
        float s = v.scale;

        switch (v.style) {
            case "fire_beam", "heat_beam", "sniper_tracer", "pulse_bolts" -> {
                Vec3 diff = v.target.subtract(v.origin);
                double len = Math.max(0.4, diff.length());
                Vec3 mid = v.origin.add(v.target).scale(0.5).add(0, 1.05, 0);
                float yawRad = (float) Math.atan2(diff.x, diff.z);
                float hDist = (float) Math.sqrt(diff.x * diff.x + diff.z * diff.z);
                float pitchRad = (float) -Math.atan2(diff.y, hDist);
                float thickness = ("sniper_tracer".equals(v.style) ? 0.11f : ("heat_beam".equals(v.style) ? 0.48f : 0.24f)) * s * (1.15f - p * 0.45f);

                ps.pushPose();
                ps.translate(mid.x - camPos.x, mid.y - camPos.y, mid.z - camPos.z);
                ps.mulPose(Vector3f.YP.rotation(yawRad));
                ps.mulPose(Vector3f.XP.rotation(pitchRad));
                ps.scale(thickness, thickness, (float) len);
                unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha);
                ps.popPose();

                ps.pushPose();
                ps.translate(tx, ty, tz);
                float burst = (0.4f + p * 1.5f) * s;
                ps.scale(burst, burst, burst);
                unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 0.9f, 0.6f, alpha * 0.7f);
                ps.popPose();
            }
            case "earth_cleave", "drill_burst", "shockwave_ring" -> {
                ps.pushPose();
                ps.translate(tx, v.target.y + 0.08 - camPos.y, tz);
                float ringS = (0.8f + p * 2.8f) * s;
                ps.scale(ringS, 0.14f, ringS);
                unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha * 0.72f);
                ps.popPose();

                if (!"shockwave_ring".equals(v.style)) {
                    for (int k = -1; k <= 1; k++) {
                        ps.pushPose();
                        ps.translate(tx + k * 0.45 * s, v.target.y + 0.65 * (0.4f + p * 0.6f) - camPos.y, tz + (k % 2) * 0.35);
                        ps.mulPose(Vector3f.ZP.rotationDegrees(k * 16.0f));
                        ps.scale(0.32f * s, (0.9f + p * 1.4f) * s, 0.32f * s);
                        unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha);
                        ps.popPose();
                    }
                }
            }
            case "wind_vortex" -> {
                for (int k = 0; k < 4; k++) {
                    float rot = p * 540.0f + k * 90.0f;
                    double rad = (0.45 + p * 1.1) * s;
                    double ox = Math.cos(Math.toRadians(rot)) * rad;
                    double oz = Math.sin(Math.toRadians(rot)) * rad;
                    ps.pushPose();
                    ps.translate(tx + ox, v.target.y + 0.4 + p * 1.8 - camPos.y, tz + oz);
                    ps.mulPose(Vector3f.YP.rotationDegrees(-rot));
                    ps.scale(0.18f * s, 1.4f * s, 0.65f * s);
                    unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha);
                    ps.popPose();
                }
            }
            case "heal_pillar", "buff_aura" -> {
                ps.pushPose();
                ps.translate(tx, v.target.y + 0.15 + p * 1.4 - camPos.y, tz);
                ps.mulPose(Vector3f.YP.rotationDegrees(p * 260.0f));
                ps.scale(1.1f * s, 0.22f, 1.1f * s);
                unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha);
                ps.popPose();

                ps.pushPose();
                ps.translate(tx, v.target.y + 1.0 - camPos.y, tz);
                ps.scale(0.85f * s, 2.2f * s, 0.85f * s);
                unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha * 0.32f);
                ps.popPose();
            }
            case "shield_dome" -> {
                ps.pushPose();
                ps.translate(tx, ty + 0.15, tz);
                ps.mulPose(Vector3f.YP.rotationDegrees(p * 120.0f));
                float rS = (1.3f + p * 0.6f) * s;
                ps.scale(rS, 2.1f * s, rS);
                unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.4f, 0.85f, 1.0f, alpha * 0.48f);
                ps.popPose();
            }
            case "water_wave" -> {
                for (int rIdx = 0; rIdx < 2; rIdx++) {
                    float rp = Math.max(0f, p - rIdx * 0.15f);
                    ps.pushPose();
                    ps.translate(tx, v.target.y + 0.12 + rp * 0.8 - camPos.y, tz);
                    float wS = (0.7f + rp * 2.2f) * s;
                    ps.scale(wS, 0.18f, wS);
                    unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha * 0.78f);
                    ps.popPose();
                }
            }
            default -> { // "slash_arc", "claw_slash"
                ps.pushPose();
                ps.translate(tx, ty, tz);
                ps.mulPose(Vector3f.YP.rotationDegrees(45.0f));
                ps.mulPose(Vector3f.ZP.rotationDegrees(-35.0f + p * 125.0f));
                ps.scale(0.18f * s, (1.8f + p * 1.1f) * s, 0.45f * s);
                unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, v.r, v.g, v.b, alpha);
                ps.popPose();

                if ("claw_slash".equals(v.style) || s >= 1.25f) {
                    ps.pushPose();
                    ps.translate(tx, ty, tz);
                    ps.mulPose(Vector3f.YP.rotationDegrees(-45.0f));
                    ps.mulPose(Vector3f.ZP.rotationDegrees(35.0f - p * 125.0f));
                    ps.scale(0.18f * s, (1.8f + p * 1.1f) * s, 0.45f * s);
                    unitCube.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 0.92f, 0.65f, alpha * 0.85f);
                    ps.popPose();
                }
            }
        }
    }
}
