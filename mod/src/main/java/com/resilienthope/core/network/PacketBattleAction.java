package com.resilienthope.core.network;

import com.resilienthope.core.combat.TurnBasedCombatManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBattleAction {
    private final int actionType;
    private final int targetIndex;
    private final int allySlotIndex;

    public PacketBattleAction(int actionType, int targetIndex, int allySlotIndex) {
        this.actionType = actionType;
        this.targetIndex = targetIndex;
        this.allySlotIndex = allySlotIndex;
    }

    public static void encode(PacketBattleAction msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.actionType);
        buf.writeInt(msg.targetIndex);
        buf.writeInt(msg.allySlotIndex);
    }

    public static PacketBattleAction decode(FriendlyByteBuf buf) {
        return new PacketBattleAction(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(PacketBattleAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TurnBasedCombatManager.handleBattleAction(player, msg.actionType, msg.targetIndex, msg.allySlotIndex);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
