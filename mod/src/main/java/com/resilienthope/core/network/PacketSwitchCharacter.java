package com.resilienthope.core.network;

import com.resilienthope.core.team.TeamCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSwitchCharacter {
    private final int targetSlot;

    public PacketSwitchCharacter(int slot) {
        this.targetSlot = slot;
    }

    public static void encode(PacketSwitchCharacter msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.targetSlot);
    }

    public static PacketSwitchCharacter decode(FriendlyByteBuf buf) {
        return new PacketSwitchCharacter(buf.readInt());
    }

    public static void handle(PacketSwitchCharacter msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                    team.switchSlot(player, msg.targetSlot);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
