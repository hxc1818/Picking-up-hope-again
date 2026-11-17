package com.resilienthope.core.network;

import com.resilienthope.core.quest.StoryDirector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketNpcAction {
    private final String npcId;
    private final String actionKey;

    public PacketNpcAction(String npcId, String actionKey) {
        this.npcId = npcId;
        this.actionKey = actionKey;
    }

    public static void encode(PacketNpcAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.npcId);
        buf.writeUtf(msg.actionKey);
    }

    public static PacketNpcAction decode(FriendlyByteBuf buf) {
        return new PacketNpcAction(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketNpcAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                StoryDirector.handleNpcAction(player, msg.npcId, msg.actionKey);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
