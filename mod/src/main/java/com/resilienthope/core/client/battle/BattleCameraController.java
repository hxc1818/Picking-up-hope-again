package com.resilienthope.core.client.battle;

import com.mojang.blaze3d.platform.Window;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import com.resilienthope.core.combat.BattleArenaLayout;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.entity.BattleCameraEntity;
import com.resilienthope.core.init.ModEntities;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.Optional;

/**
 * Manages the dedicated client-side BattleCameraEntity, ShoulderSurfing-safe perspective takeover,
 * keyframed camera choreography, screen shake, FOV dynamics, world->screen projection, and 3D mouse raycasting.
 */
public final class BattleCameraController {
    private static boolean active = false;
    private static CameraType savedCameraType = CameraType.THIRD_PERSON_BACK;
    private static BattleCameraEntity cameraEntity = null;

    private static Vec3 curPos = new Vec3(11.4, 156.9, 22.8);
    private static Vec3 curLook = new Vec3(14.4, 152.0, 11.8);
    private static float curFov = 54.0F;
    private static float prevFov = 54.0F;
    private static float curRoll = 0.0F;
    private static float prevRoll = 0.0F;

    private static float shakeIntensity = 0.0F;
    private static int timeTicks = 0;

    private static Matrix4f cachedViewProj = null;
    private static Vec3 cachedCamPos = Vec3.ZERO;

    private BattleCameraController() {}

    public static boolean isActive() {
        return active;
    }

    public static void activate(Minecraft mc) {
        if (mc == null || mc.level == null) return;
        if (!active) {
            savedCameraType = mc.options.getCameraType();
            active = true;
            curPos = new Vec3(11.4, 156.9, 22.8);
            curLook = new Vec3(14.4, 152.0, 11.8);
            curFov = 54.0F;
            prevFov = 54.0F;
            curRoll = 0.0F;
            prevRoll = 0.0F;
            shakeIntensity = 0.0F;
            cachedViewProj = null;
        }
        ensureCameraEntity(mc);
    }

    public static void deactivate(Minecraft mc) {
        if (!active && cameraEntity == null) return;
        active = false;
        shakeIntensity = 0.0F;
        cachedViewProj = null;
        if (mc != null) {
            if (mc.player != null && mc.getCameraEntity() == cameraEntity) {
                mc.setCameraEntity(mc.player);
            }
            if (savedCameraType != null) {
                mc.options.setCameraType(savedCameraType);
            }
        }
        if (cameraEntity != null) {
            cameraEntity.discard();
            cameraEntity = null;
        }
    }

    public static void addShake(float strength) {
        shakeIntensity = Math.max(shakeIntensity, strength);
    }

    private static void ensureCameraEntity(Minecraft mc) {
        if (mc.level == null) return;
        if (cameraEntity == null || cameraEntity.isRemoved() || cameraEntity.level != mc.level) {
            cameraEntity = new BattleCameraEntity(ModEntities.BATTLE_CAMERA.get(), mc.level);
            float[] yp = computeYawPitch(curPos, curLook);
            cameraEntity.snapPose(curPos.x, curPos.y, curPos.z, yp[0], yp[1]);
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (mc.getCameraEntity() != cameraEntity) {
            mc.setCameraEntity(cameraEntity);
        }
    }

    public static void tick(Minecraft mc, BattleState state, int selectedEnemyIdx, int pendingUltSlot,
                            BattleAnimClip activeClip, BattleState.AnimEvent activeEvent, float clipProgress) {
        if (!active || mc == null || mc.level == null) return;
        timeTicks++;
        if (shakeIntensity > 0.01F) {
            shakeIntensity = Math.max(0.0F, shakeIntensity - 0.085F);
        }
        ensureCameraEntity(mc);

        prevFov = curFov;
        prevRoll = curRoll;

        Vec3 targetPos = new Vec3(11.4, 156.9, 22.8);
        Vec3 targetLook = new Vec3(14.4, 152.0, 11.8);
        float targetFov = 54.0F;
        float targetRoll = 0.0F;
        float lerpRate = 0.24F;

        if (activeClip != null && activeEvent != null && !activeClip.camera.isEmpty()) {
            CameraTarget sampled = sampleClipCamera(state, activeClip, activeEvent, clipProgress);
            targetPos = sampled.pos;
            targetLook = sampled.look;
            targetFov = sampled.fov;
            targetRoll = sampled.roll;
            lerpRate = 0.42F;
        } else if (pendingUltSlot >= 0 && state != null && pendingUltSlot < state.allies.size()) {
            Vec3 allyHome = BattleArenaLayout.allyHomePos(pendingUltSlot, state.allies.size());
            Vec3 enemyFocus = computeEnemyFocus(state, selectedEnemyIdx);
            Vec3 dirToEnemy = enemyFocus.subtract(allyHome);
            if (dirToEnemy.lengthSqr() < 1e-4) dirToEnemy = new Vec3(1, 0, 0);
            Vec3 fwd = new Vec3(dirToEnemy.x, 0, dirToEnemy.z).normalize();
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
            // Heroic shoulder-level aiming angle beside the Ult caster looking toward the enemy formation
            targetPos = allyHome.subtract(fwd.scale(2.8)).add(right.scale(2.1)).add(0, 2.05, 0);
            targetLook = enemyFocus.add(0, 1.15, 0);
            targetFov = 46.0F;
            targetRoll = -2.2F;
            lerpRate = 0.26F;
        } else if (state != null) {
            Vec3 enemyFocus = computeEnemyFocus(state, selectedEnemyIdx);
            double shiftZ = (enemyFocus.z - BattleArenaLayout.ARENA_CENTER_Z) * 0.24;
            double shiftX = (enemyFocus.x - 19.0) * 0.12;
            targetPos = new Vec3(11.4 + shiftX, 156.9, 22.8 + shiftZ * 0.65);
            targetLook = new Vec3(14.4 + shiftX * 0.8, 152.05, 11.8 + shiftZ);
            targetFov = 54.0F;
            targetRoll = 0.0F;
            lerpRate = 0.22F;
        }

        targetPos = clampToArenaAir(targetPos);

        curPos = curPos.lerp(targetPos, lerpRate);
        curLook = curLook.lerp(targetLook, lerpRate);
        curFov = Mth.lerp(lerpRate, curFov, targetFov);
        curRoll = Mth.lerp(lerpRate, curRoll, targetRoll);

        float[] yp = computeYawPitch(curPos, curLook);
        if (cameraEntity != null) {
            cameraEntity.advancePose(curPos.x, curPos.y, curPos.z, yp[0], yp[1]);
        }
    }

    private static Vec3 computeEnemyFocus(BattleState state, int selectedEnemyIdx) {
        if (state == null || state.enemies.isEmpty()) {
            return new Vec3(19.0, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
        }
        if (selectedEnemyIdx >= 0 && selectedEnemyIdx < state.enemies.size() && state.enemies.get(selectedEnemyIdx).isAlive()) {
            return BattleArenaLayout.enemyHomePos(selectedEnemyIdx, state.enemies.size());
        }
        for (int i = 0; i < state.enemies.size(); i++) {
            if (state.enemies.get(i).isAlive()) {
                return BattleArenaLayout.enemyHomePos(i, state.enemies.size());
            }
        }
        return new Vec3(19.0, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
    }

    private record CameraTarget(Vec3 pos, Vec3 look, float fov, float roll) {}

    private static CameraTarget sampleClipCamera(BattleState state, BattleAnimClip clip,
                                                 BattleState.AnimEvent ev, float progress) {
        ListOfCamKeys: {
            var track = clip.camera;
            float p = Mth.clamp(progress, 0f, 1f);
            if (track.size() == 1 || p <= track.get(0).t) {
                return evalKeyframe(state, track.get(0), ev);
            }
            var last = track.get(track.size() - 1);
            if (p >= last.t) {
                return evalKeyframe(state, last, ev);
            }
            for (int i = 0; i < track.size() - 1; i++) {
                var a = track.get(i);
                var b = track.get(i + 1);
                if (p >= a.t && p <= b.t) {
                    float span = Math.max(1e-4f, b.t - a.t);
                    float u = BattleAnimClip.applyEase(b.ease, (p - a.t) / span);
                    CameraTarget ca = evalKeyframe(state, a, ev);
                    CameraTarget cb = evalKeyframe(state, b, ev);
                    return new CameraTarget(
                            ca.pos.lerp(cb.pos, u),
                            ca.look.lerp(cb.look, u),
                            Mth.lerp(u, ca.fov, cb.fov),
                            Mth.lerp(u, ca.roll, cb.roll)
                    );
                }
            }
            break ListOfCamKeys;
        }
        return new CameraTarget(new Vec3(11.4, 156.9, 22.8), new Vec3(14.4, 152.0, 11.8), 54.0F, 0.0F);
    }

    private static CameraTarget evalKeyframe(BattleState state, BattleAnimClip.CameraKeyframe kf,
                                             BattleState.AnimEvent ev) {
        Vec3 actorHome = BattleSceneClientController.getActorWorldHome(state, ev.actorSide, ev.actorIdx);
        Vec3 targetHome = BattleSceneClientController.getTargetCenter(state, ev);
        // Facing unit vector along X from actor toward opponent (+1 for ally attacking East, -1 for enemy attacking West)
        double fwdSign = (ev.actorSide == 0) ? 1.0 : -1.0;

        Vec3 posBase = resolveAnchor(kf.posAnchor, actorHome, targetHome);
        Vec3 lookBase = resolveAnchor(kf.lookAnchor, actorHome, targetHome);

        Vec3 posOff = kf.localToActorFacing
                ? new Vec3(kf.posOffset[0] * fwdSign, kf.posOffset[1], kf.posOffset[2] * fwdSign)
                : new Vec3(kf.posOffset[0], kf.posOffset[1], kf.posOffset[2]);
        Vec3 lookOff = kf.localToActorFacing
                ? new Vec3(kf.lookOffset[0] * fwdSign, kf.lookOffset[1], kf.lookOffset[2] * fwdSign)
                : new Vec3(kf.lookOffset[0], kf.lookOffset[1], kf.lookOffset[2]);

        return new CameraTarget(posBase.add(posOff), lookBase.add(lookOff), kf.fov, kf.roll);
    }

    private static Vec3 resolveAnchor(String anchor, Vec3 actorHome, Vec3 targetHome) {
        if (anchor == null) return new Vec3(BattleArenaLayout.ARENA_CENTER_X, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
        return switch (anchor) {
            case "overview" -> new Vec3(11.4, 156.9, 22.8);
            case "actor" -> actorHome;
            case "target" -> targetHome;
            default -> new Vec3(BattleArenaLayout.ARENA_CENTER_X, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
        };
    }

    private static Vec3 clampToArenaAir(Vec3 p) {
        // Keep camera well inside the open hangar volume (floor Y=150..roof Y=168, walls X=-4..32, Z=-4..28)
        double x = Mth.clamp(p.x, 1.2, 26.8);
        double y = Mth.clamp(p.y, 151.6, 163.5);
        double z = Mth.clamp(p.z, 1.5, 25.0);
        return new Vec3(x, y, z);
    }

    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!active) return;
        float pt = (float) event.getPartialTicks();
        float t = timeTicks + pt;
        float sx = 0f, sy = 0f, sr = 0f;
        if (shakeIntensity > 0.01f) {
            sx = Mth.sin(t * 2.9f) * shakeIntensity * 3.8f;
            sy = Mth.cos(t * 3.7f) * shakeIntensity * 2.9f;
            sr = Mth.sin(t * 4.3f) * shakeIntensity * 2.2f;
        }
        float roll = Mth.lerp(pt, prevRoll, curRoll) + sr;
        event.setYaw(event.getYaw() + sx);
        event.setPitch(event.getPitch() + sy);
        event.setRoll(roll);
    }

    public static void onFieldOfView(EntityViewRenderEvent.FieldOfView event) {
        if (!active) return;
        float pt = (float) event.getPartialTicks();
        event.setFOV(Mth.lerp(pt, prevFov, curFov));
    }

    public static void captureRenderMatrices(RenderLevelStageEvent event) {
        if (!active) return;
        Matrix4f proj = event.getProjectionMatrix();
        Matrix4f mv = event.getPoseStack().last().pose();
        Matrix4f vp = new Matrix4f(proj);
        vp.multiply(mv);
        cachedViewProj = vp;
        cachedCamPos = event.getCamera().getPosition();
    }

    public static double[] projectWorldToScreen(Vec3 worldPos, int guiW, int guiH) {
        if (worldPos == null || guiW <= 0 || guiH <= 0) return null;
        if (cachedViewProj != null) {
            Vector4f v = new Vector4f(
                    (float) (worldPos.x - cachedCamPos.x),
                    (float) (worldPos.y - cachedCamPos.y),
                    (float) (worldPos.z - cachedCamPos.z),
                    1.0F
            );
            v.transform(cachedViewProj);
            if (v.w() <= 0.05F) return null;
            float ndcX = v.x() / v.w();
            float ndcY = v.y() / v.w();
            double sx = (ndcX * 0.5D + 0.5D) * guiW;
            double sy = (0.5D - ndcY * 0.5D) * guiH;
            return new double[]{sx, sy, v.w()};
        }
        return projectAnalyticFallback(worldPos, guiW, guiH);
    }

    private static double[] projectAnalyticFallback(Vec3 worldPos, int guiW, int guiH) {
        Minecraft mc = Minecraft.getInstance();
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        Vec3 d = worldPos.subtract(camPos);
        float yaw = cam.getYRot();
        float pitch = cam.getXRot();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        Vec3 fwd = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = fwd.cross(worldUp).normalize();
        Vec3 up = right.cross(fwd).normalize();

        double fDot = d.dot(fwd);
        if (fDot <= 0.08) return null;
        double rDot = d.dot(right);
        double uDot = d.dot(up);

        Window win = mc.getWindow();
        double aspect = (double) Math.max(1, win.getWidth()) / Math.max(1, win.getHeight());
        double halfH = Math.tan(Math.toRadians(curFov) * 0.5);
        double halfW = halfH * aspect;
        double sx = (guiW * 0.5) * (1.0 + (rDot / fDot) / halfW);
        double sy = (guiH * 0.5) * (1.0 - (uDot / fDot) / halfH);
        return new double[]{sx, sy, fDot};
    }

    public static int raycastEnemyEntity(BattleState state, double mouseX, double mouseY, int guiW, int guiH) {
        if (state == null || state.enemies.isEmpty() || guiW <= 0 || guiH <= 0) return -1;
        if (cachedViewProj == null) return -1;

        Matrix4f inv = new Matrix4f(cachedViewProj);
        if (!inv.invert()) return -1;

        float ndcX = (float) ((mouseX / guiW) * 2.0D - 1.0D);
        float ndcY = (float) (1.0D - (mouseY / guiH) * 2.0D);

        Vector4f nearPt = new Vector4f(ndcX, ndcY, -1.0F, 1.0F);
        Vector4f farPt = new Vector4f(ndcX, ndcY, 1.0F, 1.0F);
        nearPt.transform(inv);
        farPt.transform(inv);
        if (Math.abs(nearPt.w()) < 1e-6f || Math.abs(farPt.w()) < 1e-6f) return -1;

        Vec3 worldNear = new Vec3(
                cachedCamPos.x + nearPt.x() / nearPt.w(),
                cachedCamPos.y + nearPt.y() / nearPt.w(),
                cachedCamPos.z + nearPt.z() / nearPt.w()
        );
        Vec3 worldFar = new Vec3(
                cachedCamPos.x + farPt.x() / farPt.w(),
                cachedCamPos.y + farPt.y() / farPt.w(),
                cachedCamPos.z + farPt.z() / farPt.w()
        );
        Vec3 rayEnd = worldNear.add(worldFar.subtract(worldNear).normalize().scale(48.0D));

        int hitIdx = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < state.enemies.size(); i++) {
            BattleState.EnemyUnit e = state.enemies.get(i);
            if (!e.isAlive()) continue;
            Vec3 base = BattleArenaLayout.enemyHomePos(i, state.enemies.size());
            String arch = BattleArenaLayout.resolveEnemyArchetype(e);
            AABB box = BattleArenaLayout.getClickHitbox(base, arch);
            Optional<Vec3> hit = box.clip(worldNear, rayEnd);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(worldNear);
                if (d < bestDist) {
                    bestDist = d;
                    hitIdx = i;
                }
            }
        }
        return hitIdx;
    }

    private static float[] computeYawPitch(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double dx = d.x;
        double dy = d.y;
        double dz = d.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(-dx, dz) * (180.0D / Math.PI));
        float pitch = (float) (-Math.atan2(dy, Math.max(1e-4D, horiz)) * (180.0D / Math.PI));
        return new float[]{yaw, pitch};
    }
}
