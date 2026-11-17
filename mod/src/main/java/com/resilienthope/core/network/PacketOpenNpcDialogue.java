package com.resilienthope.core.network;

import com.resilienthope.core.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketOpenNpcDialogue {
    private final String npcId;
    private final int questStage;

    public PacketOpenNpcDialogue(String npcId, int questStage) {
        this.npcId = npcId;
        this.questStage = questStage;
    }

    public static void encode(PacketOpenNpcDialogue msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.npcId);
        buf.writeInt(msg.questStage);
    }

    public static PacketOpenNpcDialogue decode(FriendlyByteBuf buf) {
        return new PacketOpenNpcDialogue(buf.readUtf(), buf.readInt());
    }

    public static void handle(PacketOpenNpcDialogue msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientPacketHandler.openNpcDialogue(msg.npcId, msg.questStage));
        });
        ctx.get().setPacketHandled(true);
    }
}
