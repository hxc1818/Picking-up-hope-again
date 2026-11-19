package com.resilienthope.core.network;

import com.resilienthope.core.world.MapZoneRegistry;
import com.resilienthope.core.world.MapZoningBuilder;
import com.resilienthope.core.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from BuilderRegionMapScreen when a builder clicks any zone or coordinate on the interactive world map.
 */
public class PacketBuilderTeleport {
    private final String dimensionId; // "starship" or "planet"
    private final int targetX;
    private final int targetZ;
    private final String zoneId;

    public PacketBuilderTeleport(String dimensionId, int targetX, int targetZ, String zoneId) {
        this.dimensionId = dimensionId != null ? dimensionId : "starship";
        this.targetX = targetX;
        this.targetZ = targetZ;
        this.zoneId = zoneId != null ? zoneId : "";
    }

    public static void encode(PacketBuilderTeleport msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.dimensionId);
        buf.writeInt(msg.targetX);
        buf.writeInt(msg.targetZ);
        buf.writeUtf(msg.zoneId);
    }

    public static PacketBuilderTeleport decode(FriendlyByteBuf buf) {
        return new PacketBuilderTeleport(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketBuilderTeleport msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!player.isCreative() && !player.hasPermissions(2)
                    && !player.getTags().contains("picking_hope_builder_mode")) {
                return;
            }
            MinecraftServer server = player.getServer();
            if (server == null) return;

            ResourceKey<Level> targetKey = "planet".equals(msg.dimensionId)
                    ? ModDimensions.PLANET_CRYO
                    : ModDimensions.STARSHIP;
            ServerLevel targetLevel = server.getLevel(targetKey);
            if (targetLevel == null) {
                PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("维度未加载",
                        "未找到维度 " + targetKey.location() + "，请确认使用最新版启动器进图。"));
                return;
            }

            if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                player.setGameMode(GameType.CREATIVE);
            }

            MapZoneRegistry.ZoneDef zone = !msg.zoneId.isEmpty() ? MapZoneRegistry.findById(msg.zoneId) : null;
            int tx = msg.targetX;
            int tz = msg.targetZ;
            int ty;

            if (zone != null && msg.dimensionId.equals(zone.dimensionId)) {
                MapZoningBuilder.setGuidesEnabled(targetLevel, true);
                MapZoningBuilder.ensureZoneLandingPad(targetLevel, zone);
                tx = zone.centerX + 2;
                tz = zone.centerZ + 2;
                ty = zone.centerY + 1;
            } else {
                MapZoningBuilder.setGuidesEnabled(targetLevel, true);
                if ("starship".equals(msg.dimensionId)) {
                    MapZoneRegistry.ZoneDef hovered = MapZoneRegistry.findZoneAt("starship", tx, tz);
                    int floorY = hovered != null ? hovered.centerY : 60;
                    MapZoningBuilder.ensureSafeFloorAt(targetLevel, tx, floorY, tz);
                    ty = floorY + 1;
                } else {
                    ty = 65;
                    for (int probeY = 120; probeY >= 10; probeY--) {
                        if (!targetLevel.getBlockState(new BlockPos(tx, probeY, tz)).isAir()) {
                            ty = probeY + 1;
                            break;
                        }
                    }
                }
            }

            player.teleportTo(targetLevel, tx + 0.5D, ty, tz + 0.5D, player.getYRot(), 0.0F);
            MapZoningBuilder.stampLoadedChunksAround(player, 4);

            String label = zone != null ? (zone.code + " · " + zone.name) : (ModDimensions.getDisplayName(targetKey) + " (" + tx + ", " + tz + ")");
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("造景传送", "已抵达 " + label));
        });
        ctx.get().setPacketHandled(true);
    }
}
