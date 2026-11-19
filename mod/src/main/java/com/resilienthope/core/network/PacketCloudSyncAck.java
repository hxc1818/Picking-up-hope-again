package com.resilienthope.core.network;

import com.resilienthope.core.account.AccountSessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCloudSyncAck {
    private final boolean success;
    private final String message;
    private final long syncedAt;

    public PacketCloudSyncAck(boolean success, String message, long syncedAt) {
        this.success = success;
        this.message = message != null ? message : "";
        this.syncedAt = syncedAt;
    }

    public static void encode(PacketCloudSyncAck msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.message);
        buf.writeLong(msg.syncedAt);
    }

    public static PacketCloudSyncAck decode(FriendlyByteBuf buf) {
        return new PacketCloudSyncAck(buf.readBoolean(), buf.readUtf(), buf.readLong());
    }

    public static void handle(PacketCloudSyncAck msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    AccountSessionManager.applyClientSyncStatus(msg.success, msg.message, msg.syncedAt));
        });
        ctx.get().setPacketHandled(true);
    }
}
