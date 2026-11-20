package com.resilienthope.core.network;

import com.resilienthope.core.client.builder.BuilderPrefabClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C packet syncing custom Copy/Paste clipboard availability and dimensions to client builder state.
 */
public class PacketBuilderPrefabSync {
    private final boolean hasClipboard;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public PacketBuilderPrefabSync(boolean hasClipboard, int sizeX, int sizeY, int sizeZ) {
        this.hasClipboard = hasClipboard;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public static void encode(PacketBuilderPrefabSync pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.hasClipboard);
        buf.writeVarInt(pkt.sizeX);
        buf.writeVarInt(pkt.sizeY);
        buf.writeVarInt(pkt.sizeZ);
    }

    public static PacketBuilderPrefabSync decode(FriendlyByteBuf buf) {
        return new PacketBuilderPrefabSync(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PacketBuilderPrefabSync pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BuilderPrefabClientState.setClipboard(pkt.hasClipboard, pkt.sizeX, pkt.sizeY, pkt.sizeZ)));
        ctx.setPacketHandled(true);
    }
}
