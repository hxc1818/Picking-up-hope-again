package com.resilienthope.core.world;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Builder & dimension navigation commands:
 *   /hope builder on|off
 *   /hope map starship|planet|overworld
 *   /hope tp <s1..s5|p1..p8>
 *   /hope speed <1..10>
 *   /hope guides on|off|stamp
 *   /hope list
 *   /hope nv
 *   /hope time day|night|dusk
 */
public final class HopeMapCommand {
    public static final String BUILDER_TAG = "picking_hope_builder_mode";
    private static final List<String> ZONE_IDS = List.of(
            "s1", "s2", "s3", "s4", "s5",
            "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8"
    );

    private HopeMapCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("hope")
                        .then(Commands.literal("builder")
                                .then(Commands.literal("on").executes(ctx -> setBuilder(ctx.getSource().getPlayerOrException(), true)))
                                .then(Commands.literal("off").executes(ctx -> setBuilder(ctx.getSource().getPlayerOrException(), false))))
                        .then(Commands.literal("map")
                                .then(Commands.literal("starship").executes(ctx -> switchDim(ctx.getSource().getPlayerOrException(), "starship")))
                                .then(Commands.literal("planet").executes(ctx -> switchDim(ctx.getSource().getPlayerOrException(), "planet")))
                                .then(Commands.literal("overworld").executes(ctx -> switchDim(ctx.getSource().getPlayerOrException(), "overworld"))))
                        .then(Commands.literal("tp")
                                .then(Commands.argument("zone", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ZONE_IDS, builder))
                                        .executes(ctx -> tpZone(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "zone")))))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> setFlySpeed(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "level")))))
                        .then(Commands.literal("guides")
                                .then(Commands.literal("on").executes(ctx -> toggleGuides(ctx.getSource().getPlayerOrException(), true)))
                                .then(Commands.literal("off").executes(ctx -> toggleGuides(ctx.getSource().getPlayerOrException(), false)))
                                .then(Commands.literal("stamp").executes(ctx -> stampNearby(ctx.getSource().getPlayerOrException()))))
                        .then(Commands.literal("list").executes(ctx -> listZones(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("nv").executes(ctx -> toggleNightVision(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("time")
                                .then(Commands.literal("day").executes(ctx -> setTime(ctx.getSource().getPlayerOrException(), 6000L)))
                                .then(Commands.literal("night").executes(ctx -> setTime(ctx.getSource().getPlayerOrException(), 18000L)))
                                .then(Commands.literal("dusk").executes(ctx -> setTime(ctx.getSource().getPlayerOrException(), 12500L))))
        );
    }

    private static int setBuilder(ServerPlayer player, boolean on) {
        if (on) {
            player.addTag(BUILDER_TAG);
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().setFlyingSpeed(0.10F);
            player.onUpdateAbilities();
            MinecraftServer server = player.getServer();
            if (server != null) {
                MapZoningBuilder.setGuidesEnabled(server.getLevel(ModDimensions.STARSHIP), true);
                MapZoningBuilder.setGuidesEnabled(server.getLevel(ModDimensions.PLANET_CRYO), true);
            }
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("造景模式", "已开启造景创造模式，按 [M] 打开全图区域蓝图！"));
            player.sendMessage(new TextComponent("[造景工坊] 已切换创造模式。按 [M] 看全图并点击传送，/hope map starship|planet 切图，/hope speed 1~10 调飞行速度。"), player.getUUID());
        } else {
            player.removeTag(BUILDER_TAG);
            player.getAbilities().setFlyingSpeed(0.05F);
            player.onUpdateAbilities();
            player.setGameMode(GameType.ADVENTURE);
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("冒险模式", "已退出造景模式并恢复默认飞行速度。"));
        }
        return 1;
    }

    private static int switchDim(ServerPlayer player, String dimId) {
        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        if ("overworld".equals(dimId)) {
            ServerLevel ow = server.getLevel(Level.OVERWORLD);
            if (ow == null) return 0;
            player.teleportTo(ow, 6.5D, 61.0D, 10.5D, 270.0F, 0.0F);
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("切换维度", "已返回初始主世界。"));
            return 1;
        }

        if ("starship".equals(dimId)) {
            return tpZone(player, "s1");
        }
        if ("planet".equals(dimId)) {
            return tpZone(player, "p2");
        }
        return 0;
    }

    private static int tpZone(ServerPlayer player, String zoneId) {
        MapZoneRegistry.ZoneDef zone = MapZoneRegistry.findById(zoneId);
        if (zone == null) {
            player.sendMessage(new TextComponent("[造景工坊] 未找到区域: " + zoneId + "，可用列表: s1..s5, p1..p8"), player.getUUID());
            return 0;
        }
        MinecraftServer server = player.getServer();
        if (server == null) return 0;
        ServerLevel targetLevel = server.getLevel("planet".equals(zone.dimensionId) ? ModDimensions.PLANET_CRYO : ModDimensions.STARSHIP);
        if (targetLevel == null) {
            player.sendMessage(new TextComponent("[造景工坊] 目标维度未就绪，请退出并用启动器重新加载本存档。"), player.getUUID());
            return 0;
        }

        player.addTag(BUILDER_TAG);
        if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
            player.setGameMode(GameType.CREATIVE);
        }

        MapZoningBuilder.setGuidesEnabled(targetLevel, true);
        MapZoningBuilder.ensureZoneLandingPad(targetLevel, zone);
        player.teleportTo(targetLevel, zone.centerX + 2.5D, zone.centerY + 1.0D, zone.centerZ + 2.5D, player.getYRot(), 0.0F);
        MapZoningBuilder.stampLoadedChunksAround(player, 4);

        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("传送至 " + zone.code, zone.name + " (" + zone.sizeLabel() + ")"));
        player.sendMessage(new TextComponent("[" + zone.code + " · " + zone.name + "] " + zone.summary + " | " + zone.verticalNote), player.getUUID());
        return 1;
    }

    private static int setFlySpeed(ServerPlayer player, int level) {
        float speed = 0.05F * level;
        player.getAbilities().setFlyingSpeed(speed);
        player.onUpdateAbilities();
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("飞行速度", "当前飞行倍率：" + level + "x（推荐万格长途用 6~10x）"));
        return 1;
    }

    private static int toggleGuides(ServerPlayer player, boolean enabled) {
        ServerLevel level = player.getLevel();
        MapZoningBuilder.setGuidesEnabled(level, enabled);
        if (enabled) {
            int stamped = MapZoningBuilder.stampLoadedChunksAround(player, 5);
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("区域划线", "已开启动态划线（补划周围 " + stamped + " 个区块）"));
        } else {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("区域划线", "已暂停自动绘制新区域边界线"));
        }
        return 1;
    }

    private static int stampNearby(ServerPlayer player) {
        MapZoningBuilder.setGuidesEnabled(player.getLevel(), true);
        int stamped = MapZoningBuilder.stampLoadedChunksAround(player, 8);
        BlockPos pos = player.blockPosition();
        MapZoneRegistry.ZoneDef cur = MapZoneRegistry.findZoneAt(ModDimensions.toZoneDimensionId(player.getLevel().dimension()), pos.getX(), pos.getZ());
        if (cur != null) {
            MapZoningBuilder.ensureZoneLandingPad(player.getLevel(), cur);
        }
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("区域划线", "已扫描补划周围 " + stamped + " 个区块"));
        return 1;
    }

    private static int listZones(ServerPlayer player) {
        player.sendMessage(new TextComponent("=== 星舰「重拾希望号」(starship) 5区规划 ==="), player.getUUID());
        for (MapZoneRegistry.ZoneDef z : MapZoneRegistry.STARSHIP_ZONES) {
            player.sendMessage(new TextComponent(String.format("  %s · %s | 尺寸 %s | 中心(%d,%d,%d) | %s",
                    z.code, z.name, z.sizeLabel(), z.centerX, z.centerY, z.centerZ, z.summary)), player.getUUID());
        }
        player.sendMessage(new TextComponent("=== 第一颗星球 (planet_cryo, >10000x10000) 8区规划 ==="), player.getUUID());
        for (MapZoneRegistry.ZoneDef z : MapZoneRegistry.PLANET_ZONES) {
            player.sendMessage(new TextComponent(String.format("  %s · %s | 尺寸 %s | 中心(%d,%d,%d) | %s",
                    z.code, z.name, z.sizeLabel(), z.centerX, z.centerY, z.centerZ, z.summary)), player.getUUID());
        }
        return 1;
    }

    private static int toggleNightVision(ServerPlayer player) {
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("夜视照明", "已关闭永久夜视"));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60 * 60, 0, false, false));
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("夜视照明", "已开启永久夜视（方便地下造景）"));
        }
        return 1;
    }

    private static int setTime(ServerPlayer player, long dayTime) {
        ServerLevel level = player.getLevel();
        level.setDayTime(dayTime);
        String label = dayTime == 6000L ? "正午白昼" : (dayTime == 12500L ? "黄昏极光" : "极地深夜");
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("时间切换", "已设置为：" + label));
        return 1;
    }
}
