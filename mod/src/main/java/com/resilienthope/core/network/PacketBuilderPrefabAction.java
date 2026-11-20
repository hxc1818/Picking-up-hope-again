package com.resilienthope.core.network;

import com.resilienthope.core.world.prefab.PrefabPlacementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S builder prefab action packet for stamping prefabs, undoing, or requesting the projector tool.
 */
public class PacketBuilderPrefabAction {
    public static final int ACTION_PLACE = 0;
    public static final int ACTION_UNDO = 1;
    public static final int ACTION_GIVE_PROJECTOR = 2;
    public static final int ACTION_PLACE_AT_PLAYER = 3;

    private final int action;
    private final String prefabId;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final int rotSteps;
    private final int repeatCount;
    private final int yOffset;
    private final boolean airOnly;

    public PacketBuilderPrefabAction(int action, String prefabId, int anchorX, int anchorY, int anchorZ,
                                     int rotSteps, int repeatCount, int yOffset, boolean airOnly) {
        this.action = action;
        this.prefabId = prefabId == null ? "" : prefabId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.rotSteps = rotSteps;
        this.repeatCount = repeatCount;
        this.yOffset = yOffset;
        this.airOnly = airOnly;
    }

    public static PacketBuilderPrefabAction place(String prefabId, BlockPos anchor,
                                                  int rotSteps, int repeatCount, int yOffset, boolean airOnly) {
        return new PacketBuilderPrefabAction(ACTION_PLACE, prefabId,
                anchor.getX(), anchor.getY(), anchor.getZ(), rotSteps, repeatCount, yOffset, airOnly);
    }

    public static PacketBuilderPrefabAction placeAtPlayer(String prefabId,
                                                          int rotSteps, int repeatCount, int yOffset, boolean airOnly) {
        return new PacketBuilderPrefabAction(ACTION_PLACE_AT_PLAYER, prefabId, 0, 0, 0, rotSteps, repeatCount, yOffset, airOnly);
    }

    public static PacketBuilderPrefabAction undo() {
        return new PacketBuilderPrefabAction(ACTION_UNDO, "", 0, 0, 0, 0, 1, 0, false);
    }

    public static PacketBuilderPrefabAction giveProjector() {
        return new PacketBuilderPrefabAction(ACTION_GIVE_PROJECTOR, "", 0, 0, 0, 0, 1, 0, false);
    }

    public static void encode(PacketBuilderPrefabAction pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.action);
        buf.writeUtf(pkt.prefabId, 64);
        buf.writeInt(pkt.anchorX);
        buf.writeInt(pkt.anchorY);
        buf.writeInt(pkt.anchorZ);
        buf.writeVarInt(pkt.rotSteps);
        buf.writeVarInt(pkt.repeatCount);
        buf.writeVarInt(pkt.yOffset);
        buf.writeBoolean(pkt.airOnly);
    }

    public static PacketBuilderPrefabAction decode(FriendlyByteBuf buf) {
        return new PacketBuilderPrefabAction(
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public static void handle(PacketBuilderPrefabAction pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !player.isCreative()) return;

            switch (pkt.action) {
                case ACTION_UNDO -> PrefabPlacementManager.undoLast(player);
                case ACTION_GIVE_PROJECTOR -> PrefabPlacementManager.giveProjector(player);
                case ACTION_PLACE_AT_PLAYER -> PrefabPlacementManager.placePrefab(
                        player, pkt.prefabId, player.blockPosition(), pkt.rotSteps, pkt.repeatCount, pkt.yOffset, pkt.airOnly);
                case ACTION_PLACE -> PrefabPlacementManager.placePrefab(
                        player, pkt.prefabId, new BlockPos(pkt.anchorX, pkt.anchorY, pkt.anchorZ),
                        pkt.rotSteps, pkt.repeatCount, pkt.yOffset, pkt.airOnly);
                default -> {}
            }
        });
        ctx.setPacketHandled(true);
    }
}
