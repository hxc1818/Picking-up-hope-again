package com.resilienthope.core.client.builder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.resilienthope.core.client.gui.BuilderPrefabScreen;
import com.resilienthope.core.network.PacketBuilderPrefabAction;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.world.prefab.PrefabTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Renders 3D bounding wireframe previews in Creative mode while holding the Blueprint Prefab Projector
 * and maps Left-Click (library) / Shift+Left-Click (undo) cleanly without breaking blocks.
 */
public final class BuilderPrefabOverlayRenderer {
    private static long lastLeftClickMs = 0L;

    private BuilderPrefabOverlayRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !mc.player.isCreative()) {
            return;
        }
        if (!BuilderPrefabClientState.isHoldingProjector(mc.player)) {
            return;
        }

        BlockPos anchor = BuilderPrefabClientState.computeTargetAnchor(mc);
        int sx = BuilderPrefabClientState.getActiveSizeX();
        int sy = BuilderPrefabClientState.getActiveSizeY();
        int sz = BuilderPrefabClientState.getActiveSizeZ();
        int step = BuilderPrefabClientState.getActiveStep();
        int rot = BuilderPrefabClientState.getRotSteps();
        int rep = BuilderPrefabClientState.getRepeatCount();
        int dy = BuilderPrefabClientState.getYOffset();

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        for (int seg = 0; seg < rep; seg++) {
            int[] b = PrefabTransform.computeSegmentBounds(anchor, sx, sy, sz, rot, seg, step, dy);
            AABB box = new AABB(b[0], b[1], b[2], b[3], b[4], b[5]).inflate(0.015D);
            if (seg == 0) {
                LevelRenderer.renderLineBox(ps, vc, box, 0.22F, 0.85F, 1.00F, 0.95F);
            } else {
                LevelRenderer.renderLineBox(ps, vc, box, 0.98F, 0.75F, 0.14F, 0.85F);
            }
        }

        // Highlight base placement anchor block in bright green
        BlockPos adjAnchor = anchor.offset(0, dy, 0);
        LevelRenderer.renderLineBox(ps, vc, new AABB(adjAnchor).inflate(0.03D), 0.29F, 0.92F, 0.50F, 1.0F);

        // Draw directional nose box protruding 1 block ahead of segment 0 center front
        BlockPos nosePos = PrefabTransform.localToWorld(anchor, sx, sx / 2, 0, sz, rot, 0, step, dy);
        AABB noseBox = new AABB(nosePos).contract(0.25D, 0.25D, 0.25D).move(0.125D, 0.125D, 0.125D);
        LevelRenderer.renderLineBox(ps, vc, noseBox, 0.29F, 0.95F, 0.55F, 1.0F);

        ps.popPose();
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getPlayer().level.isClientSide || mc.player != event.getPlayer()) return;
        if (mc.screen != null || !mc.player.isCreative() || !BuilderPrefabClientState.isHoldingProjector(mc.player)) {
            return;
        }
        event.setCanceled(true);
        handleProjectorLeftClick(mc);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getPlayer().level.isClientSide || mc.player != event.getPlayer()) return;
        if (mc.screen != null || !mc.player.isCreative() || !BuilderPrefabClientState.isHoldingProjector(mc.player)) {
            return;
        }
        handleProjectorLeftClick(mc);
    }

    private static void handleProjectorLeftClick(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastLeftClickMs < 180L) return;
        lastLeftClickMs = now;

        if (mc.player != null && mc.player.isShiftKeyDown()) {
            PacketHandler.INSTANCE.sendToServer(PacketBuilderPrefabAction.undo());
        } else {
            mc.setScreen(new BuilderPrefabScreen());
        }
    }
}
