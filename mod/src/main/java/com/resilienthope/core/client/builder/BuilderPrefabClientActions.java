package com.resilienthope.core.client.builder;

import com.resilienthope.core.network.PacketBuilderPrefabAction;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.subtitle.SystemToastManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

/**
 * Client-only interaction callbacks invoked via DistExecutor from PrefabProjectorItem.
 */
public final class BuilderPrefabClientActions {
    private BuilderPrefabClientActions() {}

    public static void onRightClick(boolean shiftDown) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        if (shiftDown) {
            BuilderPrefabClientState.rotateCW();
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.7f, 1.25f);
            SystemToastManager.show("旋转预览方向",
                    BuilderPrefabClientState.getActiveName() + " → " + BuilderPrefabClientState.getFacingLabel());
            return;
        }

        BlockPos anchor = BuilderPrefabClientState.computeTargetAnchor(mc);
        PacketHandler.INSTANCE.sendToServer(PacketBuilderPrefabAction.place(
                BuilderPrefabClientState.getSelectedId(),
                anchor,
                BuilderPrefabClientState.getRotSteps(),
                BuilderPrefabClientState.getRepeatCount(),
                BuilderPrefabClientState.getYOffset(),
                BuilderPrefabClientState.isAirOnly()
        ));
        mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.55f, 1.35f);
    }
}
