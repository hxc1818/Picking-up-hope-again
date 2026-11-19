package com.resilienthope.core.client.battle;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Lightweight client command for previewing & hot-reloading battle keyframe JSON animations in-game:
 *   /battle_anim reload
 *   /battle_anim list
 *   /battle_anim stop
 *   /battle_anim play <clipId> [side actorSlot targetSlot]
 */
public final class BattleAnimCommand {
    private BattleAnimCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("battle_anim")
                        .then(Commands.literal("reload").executes(ctx -> {
                            int count = BattleAnimationRegistry.INSTANCE.hotReloadNow();
                            reply("已重载战斗关键帧动画，共 " + count + " 份片段。");
                            return count;
                        }))
                        .then(Commands.literal("list").executes(ctx -> {
                            var ids = BattleAnimationRegistry.INSTANCE.getSortedClipIds();
                            reply("已加载战斗动画(" + ids.size() + "): " + String.join(", ", ids));
                            return ids.size();
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            BattleSceneClientController.stopCurrentAnimation();
                            reply("已停止当前战斗动作回放。");
                            return 1;
                        }))
                        .then(Commands.literal("play")
                                .then(Commands.argument("clipId", StringArgumentType.word())
                                        .executes(ctx -> runPlay(
                                                StringArgumentType.getString(ctx, "clipId"), 0, 0, 0))
                                        .then(Commands.argument("side", IntegerArgumentType.integer(0, 1))
                                                .then(Commands.argument("actorSlot", IntegerArgumentType.integer(0, 5))
                                                        .then(Commands.argument("targetSlot", IntegerArgumentType.integer(-1, 5))
                                                                .executes(ctx -> runPlay(
                                                                        StringArgumentType.getString(ctx, "clipId"),
                                                                        IntegerArgumentType.getInteger(ctx, "side"),
                                                                        IntegerArgumentType.getInteger(ctx, "actorSlot"),
                                                                        IntegerArgumentType.getInteger(ctx, "targetSlot")
                                                                )))))))
        );
    }

    private static int runPlay(String clipId, int side, int actorSlot, int targetSlot) {
        if (!BattleSceneClientController.isActive()) {
            reply("请先进入战斗场景后再使用 /battle_anim play 回放动作。");
            return 0;
        }
        if (!BattleAnimationRegistry.INSTANCE.hasClip(clipId)) {
            reply("未找到动画片段 [" + clipId + "]，可先执行 /battle_anim reload 或 /battle_anim list。");
            return 0;
        }
        BattleSceneClientController.playDebugClip(clipId, side, actorSlot, targetSlot);
        reply("正在回放关键帧动画: " + clipId + " (side=" + side + ", actor=" + actorSlot + ", target=" + targetSlot + ")");
        return 1;
    }

    private static void reply(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponent("[战斗动画] " + msg), Util.NIL_UUID);
        }
    }
}
