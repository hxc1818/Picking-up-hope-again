package com.resilienthope.core.client.builder;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.world.prefab.PrefabPlacementManager;
import com.resilienthope.core.world.prefab.PrefabRegistry;
import com.resilienthope.core.world.prefab.PrefabTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Client-side selected prefab, rotation, tiling count, vertical offset, and raycast anchor state.
 */
public final class BuilderPrefabClientState {
    private static String selectedId = "ship_corridor";
    private static int rotSteps = 0;
    private static int repeatCount = 1;
    private static int yOffset = 0;
    private static boolean airOnly = false;

    private static boolean hasClipboard = false;
    private static int clipX = 0;
    private static int clipY = 0;
    private static int clipZ = 0;

    private BuilderPrefabClientState() {}

    public static String getSelectedId() { return selectedId; }
    public static int getRotSteps() { return rotSteps; }
    public static int getRepeatCount() { return repeatCount; }
    public static int getYOffset() { return yOffset; }
    public static boolean isAirOnly() { return airOnly; }

    public static boolean hasClipboard() { return hasClipboard; }
    public static int getClipX() { return clipX; }
    public static int getClipY() { return clipY; }
    public static int getClipZ() { return clipZ; }

    public static boolean isClipboardSelected() {
        return hasClipboard && PrefabPlacementManager.CLIPBOARD_ID.equals(selectedId);
    }

    public static void selectPrefab(String id) {
        if (PrefabPlacementManager.CLIPBOARD_ID.equals(id) && hasClipboard) {
            selectedId = PrefabPlacementManager.CLIPBOARD_ID;
            return;
        }
        if (PrefabRegistry.findById(id) != null) {
            selectedId = id;
        }
    }

    public static void setClipboard(boolean available, int sx, int sy, int sz) {
        hasClipboard = available;
        clipX = sx;
        clipY = sy;
        clipZ = sz;
        if (available) {
            selectedId = PrefabPlacementManager.CLIPBOARD_ID;
            repeatCount = 1;
            yOffset = 0;
        } else if (PrefabPlacementManager.CLIPBOARD_ID.equals(selectedId)) {
            selectedId = "ship_corridor";
        }
    }

    public static void rotateCW() {
        rotSteps = (rotSteps + 1) & 3;
    }

    public static void setRotSteps(int steps) {
        rotSteps = Math.floorMod(steps, 4);
    }

    public static void adjustRepeat(int delta) {
        repeatCount = Mth.clamp(repeatCount + delta, 1, 10);
    }

    public static void adjustYOffset(int delta) {
        yOffset = Mth.clamp(yOffset + delta, -4, 4);
    }

    public static void toggleAirOnly() {
        airOnly = !airOnly;
    }

    public static String getActiveName() {
        if (isClipboardSelected()) return "自定义剪贴板";
        PrefabRegistry.PrefabDef def = PrefabRegistry.findById(selectedId);
        return def != null ? def.name : "标准星舰主干走廊";
    }

    public static int getActiveSizeX() {
        if (isClipboardSelected()) return Math.max(1, clipX);
        PrefabRegistry.PrefabDef def = PrefabRegistry.findById(selectedId);
        return def != null ? def.sizeX : 9;
    }

    public static int getActiveSizeY() {
        if (isClipboardSelected()) return Math.max(1, clipY);
        PrefabRegistry.PrefabDef def = PrefabRegistry.findById(selectedId);
        return def != null ? def.sizeY : 7;
    }

    public static int getActiveSizeZ() {
        if (isClipboardSelected()) return Math.max(1, clipZ);
        PrefabRegistry.PrefabDef def = PrefabRegistry.findById(selectedId);
        return def != null ? def.sizeZ : 8;
    }

    public static int getActiveStep() {
        if (isClipboardSelected()) return Math.max(1, clipZ);
        PrefabRegistry.PrefabDef def = PrefabRegistry.findById(selectedId);
        return def != null ? def.repeatStep : 8;
    }

    public static String getFacingLabel() {
        return PrefabTransform.directionName(rotSteps);
    }

    public static boolean isHoldingProjector(Player player) {
        return player != null && (player.getMainHandItem().is(ModItems.PREFAB_PROJECTOR.get())
                || player.getOffhandItem().is(ModItems.PREFAB_PROJECTOR.get()));
    }

    public static BlockPos computeTargetAnchor(Minecraft mc) {
        if (mc.player == null) return BlockPos.ZERO;
        HitResult hit = mc.player.pick(16.0D, 0.0F, false);
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return bhr.getBlockPos().relative(bhr.getDirection());
        }
        return new BlockPos(mc.player.getEyePosition().add(mc.player.getLookAngle().scale(8.0D)));
    }
}
