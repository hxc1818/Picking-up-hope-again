package com.resilienthope.core.network;

import com.resilienthope.core.account.AccountSessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCloudSyncRequest {
    public PacketCloudSyncRequest() {}

    public static void encode(PacketCloudSyncRequest msg, FriendlyByteBuf buf) {}

    public static PacketCloudSyncRequest decode(FriendlyByteBuf buf) {
        return new PacketCloudSyncRequest();
    }

    public static void handle(PacketCloudSyncRequest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                AccountSessionManager.pushManualFromServer(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
