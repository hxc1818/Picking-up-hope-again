package com.resilienthope.core.client.battle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resilienthope.core.client.gui.TurnBasedBattleScreen;
import com.resilienthope.core.combat.BattleArenaLayout;
import com.resilienthope.core.combat.BattleCombatPreview;
import com.resilienthope.core.combat.BattleState;
import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.entity.BattleActorEntity;
import com.resilienthope.core.team.CharacterData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * Client-side orchestrator for native 3D turn-based battle entities, keyframed animation playback,
 * camera choreography integration, multi-color native glowing, and VFX triggers.
 */
public final class BattleSceneClientController {

    public static final class ActorVisualPose {
        public double offsetX, offsetY, offsetZ;
        public float yawOffset;
        public BattleAnimClip clip;
        public float clipProgress;
        public boolean acting;
        public boolean hurtFlash;
    }

    private static final AABB ARENA_ENTITY_SCAN = new AABB(-8.0, 145.0, -8.0, 38.0, 180.0, 34.0);

    private static boolean active = false;
    private static BattleState currentState = null;
    private static TurnBasedBattleScreen activeScreen = null;

    private static int selectedEnemyIdx = 0;
    private static int pendingActionMode = TurnBasedCombatManager.ACTION_BASIC;
    private static int pendingUltSlot = -1;

    private static final Deque<BattleState.AnimEvent> ANIM_QUEUE = new ArrayDeque<>();
    private static BattleState.AnimEvent currentEvent = null;
    private static BattleAnimClip currentClip = null;
    private static float animTick = 0f;
    private static float prevAnimTick = 0f;
    private static int hitStopRemaining = 0;
    private static boolean popupSpawnedForCurrent = false;
    private static final Set<Integer> FIRED_MARKERS = new HashSet<>();

    private static final int[][] HURT_TICKS = new int[2][8];
    private static final boolean[] VISUAL_ENEMY_ALIVE = new boolean[8];
    private static final int[] ENEMY_DYING_TICKS = new int[8];
    private static int finishRevealDelayTicks = 0;
    private static boolean resultModalReady = false;
    private static int globalTick = 0;
    private static BattleCombatPreview.PreviewResult lastPreview = new BattleCombatPreview.PreviewResult();

    private BattleSceneClientController() {}

    public static boolean isActive() {
        return active;
    }

    public static BattleState getCurrentState() {
        return currentState;
    }

    public static void setActiveScreen(TurnBasedBattleScreen screen) {
        activeScreen = screen;
    }

    public static void updateSelectionContext(int enemyIdx, int actionMode, int ultSlot) {
        selectedEnemyIdx = enemyIdx;
        pendingActionMode = actionMode;
        pendingUltSlot = ultSlot;
    }

    public static void onBattleStateSync(BattleState state) {
        Minecraft mc = Minecraft.getInstance();
        if (state == null || !state.active) {
            deactivate();
            return;
        }
        currentState = state;
        boolean hasEvents = state.events != null && !state.events.isEmpty();
        if (!active) {
            active = true;
            ANIM_QUEUE.clear();
            currentEvent = null;
            currentClip = null;
            hitStopRemaining = 0;
            finishRevealDelayTicks = 0;
            resultModalReady = false;
            BattleVfxManager.clear();
            for (int[] row : HURT_TICKS) Arrays.fill(row, 0);
            Arrays.fill(VISUAL_ENEMY_ALIVE, false);
            Arrays.fill(ENEMY_DYING_TICKS, 0);
            for (int i = 0; i < Math.min(8, state.enemies.size()); i++) {
                VISUAL_ENEMY_ALIVE[i] = state.enemies.get(i).isAlive();
            }
            BattleCameraController.activate(mc);
        } else {
            for (int i = 0; i < 8; i++) {
                if (i >= state.enemies.size()) {
                    VISUAL_ENEMY_ALIVE[i] = false;
                    ENEMY_DYING_TICKS[i] = 0;
                } else {
                    boolean aliveNow = state.enemies.get(i).isAlive();
                    if (aliveNow) {
                        VISUAL_ENEMY_ALIVE[i] = true;
                        ENEMY_DYING_TICKS[i] = 0;
                    } else if (!hasEvents && ENEMY_DYING_TICKS[i] == 0) {
                        VISUAL_ENEMY_ALIVE[i] = false;
                    }
                }
            }
        }
        if (hasEvents) {
            ANIM_QUEUE.addAll(state.events);
        }
        if (state.finished) {
            if (!resultModalReady) {
                finishRevealDelayTicks = (hasEvents || isAnimating() || anyEnemyDying()) ? 14 : 6;
            }
        } else {
            resultModalReady = false;
            finishRevealDelayTicks = 0;
        }
        recomputePreview();
        updateActorGlowColors(mc);
    }

    public static void deactivate() {
        active = false;
        currentState = null;
        activeScreen = null;
        ANIM_QUEUE.clear();
        currentEvent = null;
        currentClip = null;
        hitStopRemaining = 0;
        finishRevealDelayTicks = 0;
        resultModalReady = false;
        Arrays.fill(VISUAL_ENEMY_ALIVE, false);
        Arrays.fill(ENEMY_DYING_TICKS, 0);
        pendingUltSlot = -1;
        BattleVfxManager.clear();
        BattleCameraController.deactivate(Minecraft.getInstance());
    }

    public static boolean isAnimating() {
        return currentEvent != null || !ANIM_QUEUE.isEmpty();
    }

    public static boolean shouldShowResultModal() {
        return currentState != null && currentState.finished && resultModalReady;
    }

    public static boolean isEnemyVisuallyAlive(int slot) {
        if (slot < 0 || slot >= VISUAL_ENEMY_ALIVE.length) return false;
        return VISUAL_ENEMY_ALIVE[slot] || ENEMY_DYING_TICKS[slot] > 0;
    }

    public static float getEnemyDeathFade(int slot, float partialTicks) {
        if (slot < 0 || slot >= ENEMY_DYING_TICKS.length || ENEMY_DYING_TICKS[slot] <= 0) return 1.0F;
        return Mth.clamp((ENEMY_DYING_TICKS[slot] - partialTicks) / 10.0F, 0.05F, 1.0F);
    }

    private static boolean anyEnemyDying() {
        for (int t : ENEMY_DYING_TICKS) {
            if (t > 0) return true;
        }
        return false;
    }

    public static BattleState.AnimEvent getCurrentEvent() {
        return currentEvent;
    }

    public static float getCurrentAnimProgress(float partialTicks) {
        if (currentClip == null || currentEvent == null) return 0f;
        float t = Mth.lerp(partialTicks, prevAnimTick, animTick);
        return Mth.clamp(t / Math.max(1, currentClip.durationTicks), 0f, 1f);
    }

    public static BattleCombatPreview.PreviewResult getLastPreview() {
        return lastPreview;
    }

    public static void skipAllAnimations() {
        ANIM_QUEUE.clear();
        currentEvent = null;
        currentClip = null;
        animTick = 0f;
        prevAnimTick = 0f;
        hitStopRemaining = 0;
        FIRED_MARKERS.clear();
        Arrays.fill(ENEMY_DYING_TICKS, 0);
        if (currentState != null) {
            for (int i = 0; i < 8; i++) {
                VISUAL_ENEMY_ALIVE[i] = i < currentState.enemies.size() && currentState.enemies.get(i).isAlive();
            }
            if (currentState.finished && !resultModalReady) {
                finishRevealDelayTicks = 0;
                resultModalReady = true;
                if (activeScreen != null) {
                    activeScreen.onResultModalRevealed();
                }
            }
        }
    }

    public static void stopCurrentAnimation() {
        skipAllAnimations();
    }

    public static boolean playDebugClip(String clipId, int side, int actorSlot, int targetSlot) {
        if (!active || currentState == null) return false;
        skipAllAnimations();
        BattleState.AnimEvent dbg = new BattleState.AnimEvent();
        dbg.actorSide = Mth.clamp(side, 0, 1);
        dbg.actorIdx = Math.max(0, actorSlot);
        dbg.targetIdx = targetSlot;
        dbg.clipId = clipId;
        dbg.animType = clipId.contains("ult") ? "ULT" : (clipId.contains("skill") ? "SKILL" : "BASIC");
        if (dbg.actorSide == 0 && dbg.actorIdx < currentState.allies.size()) {
            var a = currentState.allies.get(dbg.actorIdx);
            dbg.actorId = a.charId;
            dbg.actorName = a.name;
            dbg.element = a.element;
        } else if (dbg.actorSide == 1 && dbg.actorIdx < currentState.enemies.size()) {
            var e = currentState.enemies.get(dbg.actorIdx);
            dbg.actorId = e.id;
            dbg.actorName = e.name;
            dbg.element = "FIRE";
        }
        dbg.skillTitle = clipId;
        dbg.value = 0;
        ANIM_QUEUE.add(dbg);
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            deactivate();
            return;
        }
        if (mc.isPaused()) return;

        globalTick++;
        BattleVfxManager.tick();
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < HURT_TICKS[s].length; i++) {
                if (HURT_TICKS[s][i] > 0) HURT_TICKS[s][i]--;
            }
        }
        for (int i = 0; i < ENEMY_DYING_TICKS.length; i++) {
            if (ENEMY_DYING_TICKS[i] > 0) {
                ENEMY_DYING_TICKS[i]--;
                if (ENEMY_DYING_TICKS[i] == 0) {
                    VISUAL_ENEMY_ALIVE[i] = false;
                }
            }
        }

        // Advance or pop queued choreography events
        if (currentEvent == null && !ANIM_QUEUE.isEmpty()) {
            popNextAnimation(mc);
        }

        if (currentEvent != null && currentClip != null) {
            prevAnimTick = animTick;
            if (hitStopRemaining > 0) {
                hitStopRemaining--;
            } else {
                animTick += 1.0F;
                float p = Mth.clamp(animTick / Math.max(1, currentClip.durationTicks), 0f, 1f);
                processClipMarkers(mc, p);
                if (animTick >= currentClip.durationTicks) {
                    currentEvent = null;
                    currentClip = null;
                    animTick = 0f;
                    prevAnimTick = 0f;
                    FIRED_MARKERS.clear();
                    if (!ANIM_QUEUE.isEmpty()) {
                        popNextAnimation(mc);
                    }
                }
            }
        }

        // Catch any enemies defeated by DoT / phase transition once combat queue drains
        if (!isAnimating() && currentState != null) {
            for (int i = 0; i < Math.min(8, currentState.enemies.size()); i++) {
                if (VISUAL_ENEMY_ALIVE[i] && !currentState.enemies.get(i).isAlive() && ENEMY_DYING_TICKS[i] == 0) {
                    ENEMY_DYING_TICKS[i] = 8;
                }
            }
        }

        if (currentState != null && currentState.finished && !resultModalReady) {
            if (!isAnimating() && !anyEnemyDying()) {
                if (finishRevealDelayTicks > 0) {
                    finishRevealDelayTicks--;
                }
                if (finishRevealDelayTicks <= 0) {
                    resultModalReady = true;
                    if (activeScreen != null) {
                        activeScreen.onResultModalRevealed();
                    }
                }
            }
        }

        recomputePreview();
        updateActorGlowColors(mc);
        BattleCameraController.tick(
                mc, currentState, selectedEnemyIdx, pendingUltSlot,
                currentClip, currentEvent,
                currentClip != null ? Mth.clamp(animTick / Math.max(1, currentClip.durationTicks), 0f, 1f) : 0f
        );
    }

    private static void popNextAnimation(Minecraft mc) {
        while (!ANIM_QUEUE.isEmpty()) {
            BattleState.AnimEvent next = ANIM_QUEUE.poll();
            if ("BREAK".equals(next.animType)) {
                // Instant punchy break shockwave + popup without locking camera in an extra long cut
                triggerBreakFeedback(mc, next);
                continue;
            }
            currentEvent = next;
            String clipId = resolveClipIdForEvent(next);
            currentClip = BattleAnimationRegistry.INSTANCE.getClip(clipId);
            animTick = 0f;
            prevAnimTick = 0f;
            hitStopRemaining = 0;
            popupSpawnedForCurrent = false;
            FIRED_MARKERS.clear();
            processClipMarkers(mc, 0.0f);
            return;
        }
    }

    private static void triggerBreakFeedback(Minecraft mc, BattleState.AnimEvent brk) {
        Vec3 pos = getTargetCenter(currentState, brk);
        BattleVfxManager.spawnVfx("earth_cleave", pos, pos, brk.element, 1.35f);
        if (mc.level != null) {
            BattleVfxManager.spawnParticles(mc.level, pos, "explosion", 8, 0.6f);
        }
        BattleVfxManager.playSound(mc, "shield_break", 1.0f, 0.95f);
        BattleCameraController.addShake(0.45f);
        if (brk.value > 0) {
            BattleVfxManager.addPopup(pos.add(0, 2.35, 0), "弱点击破 -" + brk.value, 0xFFFF9F1C, 34, true);
        }
        if (currentState != null && brk.targetIdx >= 0 && brk.targetIdx < HURT_TICKS[1].length) {
            HURT_TICKS[1][brk.targetIdx] = 12;
        }
    }

    private static String resolveClipIdForEvent(BattleState.AnimEvent ev) {
        if (ev.clipId != null && !ev.clipId.isEmpty() && BattleAnimationRegistry.INSTANCE.hasClip(ev.clipId)) {
            return ev.clipId;
        }
        if (ev.clipId != null && !ev.clipId.isEmpty()) {
            return ev.clipId;
        }
        if (ev.actorSide == 0) {
            String cid = (ev.actorId != null && !ev.actorId.isEmpty()) ? ev.actorId : "linye";
            return switch (ev.animType) {
                case "ULT" -> cid + "_ult";
                case "FOLLOWUP" -> "chenfeng_followup";
                case "SKILL", "HEAL", "SHIELD", "BUFF" -> cid + "_skill";
                default -> cid + "_basic";
            };
        }
        return "BOSS_AOE".equals(ev.animType) ? "guard_shockwave" : "drone_pulse";
    }

    private static void processClipMarkers(Minecraft mc, float p) {
        if (currentClip == null || currentEvent == null) return;
        for (int i = 0; i < currentClip.events.size(); i++) {
            if (FIRED_MARKERS.contains(i)) continue;
            BattleAnimClip.EventMarker m = currentClip.events.get(i);
            if (p >= m.t) {
                FIRED_MARKERS.add(i);
                executeEventMarker(mc, m);
            }
        }
    }

    private static void executeEventMarker(Minecraft mc, BattleAnimClip.EventMarker m) {
        if (m.shake > 0.01f) {
            BattleCameraController.addShake(m.shake);
        }
        if (m.hitStop > 0) {
            hitStopRemaining = Math.max(hitStopRemaining, m.hitStop);
        }
        if (m.sound != null && !m.sound.isEmpty()) {
            BattleVfxManager.playSound(mc, m.sound, m.volume, m.pitch);
        }

        Vec3 actorOrigin = getActorWorldHome(currentState, currentEvent.actorSide, currentEvent.actorIdx);
        List<Vec3> anchorPositions = resolveMarkerPositions(m.anchor, currentEvent);

        for (Vec3 pos : anchorPositions) {
            if (m.vfx != null && !m.vfx.isEmpty()) {
                BattleVfxManager.spawnVfx(m.vfx, actorOrigin, pos, currentEvent.element, m.scale);
            }
            if (mc.level != null && m.particle != null && !m.particle.isEmpty()) {
                BattleVfxManager.spawnParticles(mc.level, pos, m.particle, m.count, m.spread);
            }
        }

        if ("impact".equals(m.type)) {
            triggerHurtReactions(currentEvent);
            if (!m.suppressDamagePopup && !popupSpawnedForCurrent && currentEvent.value > 0) {
                popupSpawnedForCurrent = true;
                spawnImpactPopups(currentEvent);
            }
        }
    }

    private static List<Vec3> resolveMarkerPositions(String markerAnchor, BattleState.AnimEvent ev) {
        List<Vec3> out = new ArrayList<>();
        if (currentState == null) {
            out.add(new Vec3(BattleArenaLayout.ARENA_CENTER_X, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z));
            return out;
        }
        String a = markerAnchor != null ? markerAnchor : "target";
        switch (a) {
            case "actor", "weapon_tip" -> out.add(getActorWorldHome(currentState, ev.actorSide, ev.actorIdx));
            case "arena_center" -> out.add(new Vec3(BattleArenaLayout.ARENA_CENTER_X, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z));
            case "all_allies" -> {
                for (int i = 0; i < currentState.allies.size(); i++) {
                    if (currentState.allies.get(i).isAlive()) {
                        out.add(BattleArenaLayout.allyHomePos(i, currentState.allies.size()));
                    }
                }
            }
            case "all_enemies" -> {
                for (int i = 0; i < currentState.enemies.size(); i++) {
                    if (currentState.enemies.get(i).isAlive()) {
                        out.add(BattleArenaLayout.enemyHomePos(i, currentState.enemies.size()));
                    }
                }
            }
            default -> {
                if (ev.targetIdx >= 0) {
                    out.add(getTargetSlotPos(currentState, ev.actorSide, ev.targetIdx));
                    for (int sec : parseSecondaryIndices(ev.secondaryTargets)) {
                        out.add(getTargetSlotPos(currentState, ev.actorSide, sec));
                    }
                } else {
                    if (ev.actorSide == 0) {
                        for (int i = 0; i < currentState.enemies.size(); i++) {
                            if (currentState.enemies.get(i).isAlive() || currentState.enemies.get(i).hp <= 0f) {
                                out.add(BattleArenaLayout.enemyHomePos(i, currentState.enemies.size()));
                            }
                        }
                    } else {
                        for (int i = 0; i < currentState.allies.size(); i++) {
                            if (currentState.allies.get(i).isAlive()) {
                                out.add(BattleArenaLayout.allyHomePos(i, currentState.allies.size()));
                            }
                        }
                    }
                }
            }
        }
        if (out.isEmpty()) {
            out.add(new Vec3(BattleArenaLayout.ARENA_CENTER_X, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z));
        }
        return out;
    }

    private static boolean hasUpcomingAllyHit(int enemyIdx) {
        for (BattleState.AnimEvent q : ANIM_QUEUE) {
            if (q.actorSide == 0 && !"BREAK".equals(q.animType)
                    && !"HEAL".equals(q.animType) && !"SHIELD".equals(q.animType) && !"BUFF".equals(q.animType)) {
                if (q.targetIdx < 0 || q.targetIdx == enemyIdx || parseSecondaryIndices(q.secondaryTargets).contains(enemyIdx)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void triggerDeathCheckForSlot(int enemySlot) {
        if (currentState == null || enemySlot < 0 || enemySlot >= currentState.enemies.size() || enemySlot >= ENEMY_DYING_TICKS.length) {
            return;
        }
        if (VISUAL_ENEMY_ALIVE[enemySlot] && !currentState.enemies.get(enemySlot).isAlive()
                && ENEMY_DYING_TICKS[enemySlot] == 0 && !hasUpcomingAllyHit(enemySlot)) {
            ENEMY_DYING_TICKS[enemySlot] = 10;
            Minecraft mc = Minecraft.getInstance();
            Vec3 pos = BattleArenaLayout.enemyHomePos(enemySlot, currentState.enemies.size()).add(0, 1.0, 0);
            if (mc.level != null) {
                BattleVfxManager.spawnParticles(mc.level, pos, "explosion", 10, 0.55f);
            }
            BattleVfxManager.playSound(mc, "shield_break", 0.85f, 1.15f);
        }
    }

    private static void triggerHurtReactions(BattleState.AnimEvent ev) {
        if (currentState == null) return;
        if ("HEAL".equals(ev.animType) || "SHIELD".equals(ev.animType) || "BUFF".equals(ev.animType)) return;
        int vSide = ev.actorSide == 0 ? 1 : 0;
        if (ev.targetIdx >= 0 && ev.targetIdx < HURT_TICKS[vSide].length) {
            HURT_TICKS[vSide][ev.targetIdx] = 9;
            if (vSide == 1) triggerDeathCheckForSlot(ev.targetIdx);
            for (int sec : parseSecondaryIndices(ev.secondaryTargets)) {
                if (sec >= 0 && sec < HURT_TICKS[vSide].length) {
                    HURT_TICKS[vSide][sec] = 9;
                    if (vSide == 1) triggerDeathCheckForSlot(sec);
                }
            }
        } else {
            int max = vSide == 1 ? currentState.enemies.size() : currentState.allies.size();
            for (int i = 0; i < Math.min(max, HURT_TICKS[vSide].length); i++) {
                HURT_TICKS[vSide][i] = 9;
                if (vSide == 1) triggerDeathCheckForSlot(i);
            }
        }
    }

    private static void spawnImpactPopups(BattleState.AnimEvent ev) {
        if (currentState == null) return;
        if ("HEAL".equals(ev.animType) || "SHIELD".equals(ev.animType) || "BUFF".equals(ev.animType)) {
            int col = "HEAL".equals(ev.animType) ? 0xFF4ADE80 : ("SHIELD".equals(ev.animType) ? 0xFF7DD3FC : 0xFF38BDF8);
            String txt = "BUFF".equals(ev.animType) ? "+25%攻 +15速" : ("SHIELD".equals(ev.animType) ? "护盾 +" + ev.value : "+" + ev.value);
            if (ev.targetIdx >= 0 && ev.targetIdx < currentState.allies.size()) {
                Vec3 p = BattleArenaLayout.allyHomePos(ev.targetIdx, currentState.allies.size()).add(0, 2.2, 0);
                BattleVfxManager.addPopup(p, txt, col, 30, false);
            } else {
                for (int i = 0; i < currentState.allies.size(); i++) {
                    if (currentState.allies.get(i).isAlive()) {
                        Vec3 p = BattleArenaLayout.allyHomePos(i, currentState.allies.size()).add(0, 2.2, 0);
                        BattleVfxManager.addPopup(p, txt, col, 30, false);
                    }
                }
            }
            return;
        }

        if (ev.actorSide == 1) {
            if (ev.targetIdx >= 0 && ev.targetIdx < currentState.allies.size()) {
                Vec3 p = BattleArenaLayout.allyHomePos(ev.targetIdx, currentState.allies.size()).add(0, 2.2, 0);
                BattleVfxManager.addPopup(p, "-" + ev.value, 0xFFEF4444, 28, ev.crit);
                for (int sec : parseSecondaryIndices(ev.secondaryTargets)) {
                    Vec3 sp = BattleArenaLayout.allyHomePos(sec, currentState.allies.size()).add(0, 2.1, 0);
                    BattleVfxManager.addPopup(sp, "波及", 0xFFFCA5A5, 24, false);
                }
            } else {
                for (int i = 0; i < currentState.allies.size(); i++) {
                    if (currentState.allies.get(i).isAlive()) {
                        Vec3 p = BattleArenaLayout.allyHomePos(i, currentState.allies.size()).add(0, 2.2, 0);
                        BattleVfxManager.addPopup(p, "-" + ev.value, 0xFFEF4444, 28, ev.crit);
                    }
                }
            }
        } else {
            int col = ev.crit ? 0xFFFBBF24 : 0xFFFFFFFF;
            String txt = ev.crit ? ("暴击 -" + ev.value + "!") : ("-" + ev.value);
            if (ev.targetIdx >= 0 && ev.targetIdx < currentState.enemies.size()) {
                Vec3 p = BattleArenaLayout.enemyHomePos(ev.targetIdx, currentState.enemies.size()).add(0, 2.3, 0);
                BattleVfxManager.addPopup(p, txt, col, 30, ev.crit);
            } else {
                for (int i = 0; i < currentState.enemies.size(); i++) {
                    Vec3 p = BattleArenaLayout.enemyHomePos(i, currentState.enemies.size()).add(0, 2.3, 0);
                    BattleVfxManager.addPopup(p, txt, col, 30, ev.crit);
                }
            }
            // Extra secondary ally effects embedded in secondaryTargets (e.g. "heal:2:150" or "shield:all:320")
            if (ev.secondaryTargets != null) {
                if (ev.secondaryTargets.startsWith("heal:")) {
                    String[] parts = ev.secondaryTargets.split(":");
                    if (parts.length == 3) {
                        try {
                            int slot = Integer.parseInt(parts[1]);
                            String amt = parts[2];
                            if (slot >= 0 && slot < currentState.allies.size()) {
                                Vec3 hpPos = BattleArenaLayout.allyHomePos(slot, currentState.allies.size()).add(0, 2.25, 0);
                                BattleVfxManager.addPopup(hpPos, "+" + amt, 0xFF4ADE80, 30, false);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                } else if (ev.secondaryTargets.startsWith("shield:all:")) {
                    String amt = ev.secondaryTargets.substring("shield:all:".length());
                    for (int i = 0; i < currentState.allies.size(); i++) {
                        if (currentState.allies.get(i).isAlive()) {
                            Vec3 sp = BattleArenaLayout.allyHomePos(i, currentState.allies.size()).add(0, 2.25, 0);
                            BattleVfxManager.addPopup(sp, "护盾 +" + amt, 0xFF7DD3FC, 30, false);
                        }
                    }
                }
            }
        }
    }

    private static void recomputePreview() {
        if (currentState == null || currentState.finished) {
            lastPreview = new BattleCombatPreview.PreviewResult();
            return;
        }
        BattleState.AllyUnit actor = null;
        int mode = pendingActionMode;
        if (pendingUltSlot >= 0 && pendingUltSlot < currentState.allies.size()) {
            actor = currentState.allies.get(pendingUltSlot);
            mode = TurnBasedCombatManager.ACTION_ULT;
        } else if (currentState.activeAllyIndex >= 0 && currentState.activeAllyIndex < currentState.allies.size()) {
            actor = currentState.allies.get(currentState.activeAllyIndex);
        }
        lastPreview = BattleCombatPreview.computePreview(currentState, actor, mode, selectedEnemyIdx);
    }

    private static void updateActorGlowColors(Minecraft mc) {
        if (mc == null || mc.level == null) return;
        boolean suppressGlow = isAnimating() || (currentState != null && currentState.finished);
        for (BattleActorEntity entity : mc.level.getEntitiesOfClass(BattleActorEntity.class, ARENA_ENTITY_SCAN)) {
            if (suppressGlow || !entity.isAliveActor()) {
                entity.clientGlowColor = 0;
                continue;
            }
            int side = entity.getSide();
            int slot = entity.getSlotIndex();
            if (side == 1) {
                var hl = lastPreview.enemyHighlights.getOrDefault(slot, BattleCombatPreview.TargetHighlight.NONE);
                entity.clientGlowColor = switch (hl) {
                    case PRIMARY_ENEMY -> 0xFFD700;
                    case SECONDARY_ENEMY -> 0xFF5533;
                    default -> 0;
                };
            } else {
                if (lastPreview.allyHighlights.contains(slot)) {
                    entity.clientGlowColor = 0x38F2B0;
                } else if (pendingUltSlot == slot || (pendingUltSlot < 0 && currentState != null && slot == currentState.activeAllyIndex)) {
                    entity.clientGlowColor = 0x55B8FF;
                } else {
                    entity.clientGlowColor = 0;
                }
            }
        }
    }

    public static ActorVisualPose computeActorPose(BattleActorEntity entity, float partialTicks) {
        ActorVisualPose pose = new ActorVisualPose();
        int side = entity.getSide();
        int slot = entity.getSlotIndex();
        String key = entity.getModelKey();

        boolean isActing = currentEvent != null && currentClip != null
                && currentEvent.actorSide == side && currentEvent.actorIdx == slot;
        boolean isHurting = side < 2 && slot >= 0 && slot < HURT_TICKS[side].length && HURT_TICKS[side][slot] > 0;
        pose.hurtFlash = isHurting && (HURT_TICKS[side][slot] % 4 >= 2);

        if (isActing) {
            float t = Mth.lerp(partialTicks, prevAnimTick, animTick);
            float p = Mth.clamp(t / Math.max(1, currentClip.durationTicks), 0f, 1f);
            pose.acting = true;
            pose.clip = currentClip;
            pose.clipProgress = p;

            Vec3 home = getActorWorldHome(currentState, side, slot);
            Vec3 animatedWorld = evaluateRootMotion(currentClip, currentState, currentEvent, p, home);
            pose.offsetX = animatedWorld.x - home.x;
            pose.offsetY = animatedWorld.y - home.y;
            pose.offsetZ = animatedWorld.z - home.z;
            pose.yawOffset = sampleRootYawOffset(currentClip, p);
        } else if (side == 0 && !entity.isAliveActor()) {
            pose.clip = BattleAnimationRegistry.INSTANCE.getClip("humanoid_down");
            pose.clipProgress = 0.5f;
        } else if (side == 0 && pendingUltSlot == slot && currentState != null && !isAnimating()) {
            BattleAnimClip readyClip = BattleAnimationRegistry.INSTANCE.getClip(key + "_ready");
            if (!BattleAnimationRegistry.INSTANCE.hasClip(key + "_ready")) {
                readyClip = BattleAnimationRegistry.INSTANCE.getClip("humanoid_ready");
            }
            pose.clip = readyClip;
            pose.clipProgress = ((globalTick + partialTicks) % Math.max(1, readyClip.durationTicks)) / Math.max(1, readyClip.durationTicks);
        } else if (isHurting) {
            BattleAnimClip hurtClip = BattleAnimationRegistry.INSTANCE.getClip(side == 0 ? "humanoid_hurt" : "enemy_hurt");
            pose.clip = hurtClip;
            float hp = 1.0f - Mth.clamp((HURT_TICKS[side][slot] - partialTicks) / 9.0f, 0f, 1f);
            pose.clipProgress = hp;
            pose.offsetX = (side == 0 ? -0.32 : 0.32) * Mth.sin(hp * (float) Math.PI);
        } else if (side == 1 && entity.isBrokenState()) {
            BattleAnimClip brkClip = BattleAnimationRegistry.INSTANCE.getClip("enemy_break");
            pose.clip = brkClip;
            pose.clipProgress = ((globalTick + partialTicks) % Math.max(1, brkClip.durationTicks)) / Math.max(1, brkClip.durationTicks);
        } else {
            String idleId = (side == 0) ? "humanoid_idle" : switch (key) {
                case "cleaner" -> "enemy_idle_cleaner";
                case "hound" -> "enemy_idle_hound";
                case "guard" -> "enemy_idle_guard";
                case "driller" -> "enemy_idle_driller";
                case "boss_core", "boss_arm_l", "boss_arm_r" -> "enemy_idle_boss";
                default -> "enemy_idle_drone";
            };
            BattleAnimClip idleClip = BattleAnimationRegistry.INSTANCE.getClip(idleId);
            pose.clip = idleClip;
            int dur = Math.max(1, idleClip.durationTicks);
            pose.clipProgress = (((globalTick + slot * 7) + partialTicks) % dur) / dur;
        }

        entity.clientOffsetX = pose.offsetX;
        entity.clientOffsetY = pose.offsetY;
        entity.clientOffsetZ = pose.offsetZ;
        entity.clientYawOffset = pose.yawOffset;
        return pose;
    }

    private static Vec3 evaluateRootMotion(BattleAnimClip clip, BattleState state,
                                           BattleState.AnimEvent ev, float p, Vec3 home) {
        if (clip.rootMotion.isEmpty()) return home;
        var track = clip.rootMotion;
        if (track.size() == 1 || p <= track.get(0).t) {
            return resolveRootPoint(track.get(0), state, ev, home);
        }
        var last = track.get(track.size() - 1);
        if (p >= last.t) {
            return resolveRootPoint(last, state, ev, home);
        }
        for (int i = 0; i < track.size() - 1; i++) {
            var a = track.get(i);
            var b = track.get(i + 1);
            if (p >= a.t && p <= b.t) {
                float span = Math.max(1e-4f, b.t - a.t);
                float u = BattleAnimClip.applyEase(b.ease, (p - a.t) / span);
                Vec3 pa = resolveRootPoint(a, state, ev, home);
                Vec3 pb = resolveRootPoint(b, state, ev, home);
                return pa.lerp(pb, u);
            }
        }
        return home;
    }

    private static Vec3 resolveRootPoint(BattleAnimClip.RootKeyframe kf, BattleState state,
                                         BattleState.AnimEvent ev, Vec3 home) {
        Vec3 base = switch (kf.anchor != null ? kf.anchor : "home") {
            case "target" -> getTargetCenter(state, ev);
            case "arena_center" -> new Vec3(BattleArenaLayout.ARENA_CENTER_X, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
            default -> home;
        };
        double fwdSign = (ev != null && ev.actorSide == 1) ? -1.0 : 1.0;
        return base.add(kf.offset[0] * fwdSign, kf.offset[1], kf.offset[2] * fwdSign);
    }

    private static float sampleRootYawOffset(BattleAnimClip clip, float p) {
        if (clip.rootMotion.isEmpty()) return 0f;
        var track = clip.rootMotion;
        if (track.size() == 1 || p <= track.get(0).t) return track.get(0).yawOffset;
        var last = track.get(track.size() - 1);
        if (p >= last.t) return last.yawOffset;
        for (int i = 0; i < track.size() - 1; i++) {
            var a = track.get(i);
            var b = track.get(i + 1);
            if (p >= a.t && p <= b.t) {
                float u = BattleAnimClip.applyEase(b.ease, (p - a.t) / Math.max(1e-4f, b.t - a.t));
                return Mth.lerp(u, a.yawOffset, b.yawOffset);
            }
        }
        return 0f;
    }

    public static Vec3 getActorWorldHome(BattleState state, int side, int idx) {
        if (state == null) {
            return side == 0
                    ? BattleArenaLayout.allyHomePos(idx, 4)
                    : BattleArenaLayout.enemyHomePos(idx, 3);
        }
        if (side == 0) {
            return BattleArenaLayout.allyHomePos(Mth.clamp(idx, 0, Math.max(0, state.allies.size() - 1)), Math.max(1, state.allies.size()));
        }
        return BattleArenaLayout.enemyHomePos(Mth.clamp(idx, 0, Math.max(0, state.enemies.size() - 1)), Math.max(1, state.enemies.size()));
    }

    public static Vec3 getTargetCenter(BattleState state, BattleState.AnimEvent ev) {
        if (state == null || ev == null) {
            return new Vec3(BattleArenaLayout.ARENA_CENTER_X, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
        }
        if ("HEAL".equals(ev.animType) || "SHIELD".equals(ev.animType) || "BUFF".equals(ev.animType)) {
            if (ev.targetIdx >= 0 && ev.targetIdx < state.allies.size()) {
                return BattleArenaLayout.allyHomePos(ev.targetIdx, state.allies.size());
            }
            return new Vec3(9.0D, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
        }
        if (ev.targetIdx >= 0) {
            return getTargetSlotPos(state, ev.actorSide, ev.targetIdx);
        }
        return ev.actorSide == 0
                ? new Vec3(18.6D, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z)
                : new Vec3(9.4D, BattleArenaLayout.UNIT_Y, BattleArenaLayout.ARENA_CENTER_Z);
    }

    private static Vec3 getTargetSlotPos(BattleState state, int actorSide, int targetIdx) {
        if (actorSide == 0) {
            return BattleArenaLayout.enemyHomePos(Mth.clamp(targetIdx, 0, Math.max(0, state.enemies.size() - 1)), Math.max(1, state.enemies.size()));
        }
        return BattleArenaLayout.allyHomePos(Mth.clamp(targetIdx, 0, Math.max(0, state.allies.size() - 1)), Math.max(1, state.allies.size()));
    }

    private static List<Integer> parseSecondaryIndices(String s) {
        List<Integer> list = new ArrayList<>();
        if (s == null || s.isEmpty() || s.contains(":")) return list;
        for (String part : s.split(",")) {
            try {
                list.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (active) {
            BattleCameraController.onCameraSetup(event);
        }
    }

    @SubscribeEvent
    public static void onFieldOfView(EntityViewRenderEvent.FieldOfView event) {
        if (active) {
            BattleCameraController.onFieldOfView(event);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        BattleCameraController.captureRenderMatrices(event);
        PoseStack ps = event.getPoseStack();
        BattleVfxManager.renderWorldVfx(
                ps,
                event.getCamera().getPosition(),
                currentState,
                lastPreview,
                isAnimating(),
                event.getPartialTick()
        );
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (active) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (active && Minecraft.getInstance().player != null && event.getPlayer() == Minecraft.getInstance().player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenOpenEvent event) {
        if (!active) return;
        if (event.getScreen() instanceof PauseScreen) return;
        if (event.getScreen() == null && activeScreen != null) {
            event.setScreen(activeScreen);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        deactivate();
    }
}
