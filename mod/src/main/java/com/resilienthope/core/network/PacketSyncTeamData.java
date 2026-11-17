package com.resilienthope.core.network;

import com.resilienthope.core.client.ClientPacketHandler;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PacketSyncTeamData {
    private final CompoundTag data;

    public PacketSyncTeamData(CompoundTag data) {
        this.data = data;
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncTeamData(team.serializeNBT()));
        });
    }

    public static void encode(PacketSyncTeamData msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static PacketSyncTeamData decode(FriendlyByteBuf buf) {
        return new PacketSyncTeamData(buf.readNbt());
    }

    public static void handle(PacketSyncTeamData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.applyTeamData(msg.data));
        });
        ctx.get().setPacketHandled(true);
    }
}
