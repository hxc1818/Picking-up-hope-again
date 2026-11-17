package com.resilienthope.core.network;

import com.resilienthope.core.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncQuest {
    private final String title;
    private final String objective;
    private final BlockPos targetPos;
    private final int stage;

    public PacketSyncQuest(String title, String objective, BlockPos targetPos, int stage) {
        this.title = title;
        this.objective = objective;
        this.targetPos = targetPos;
        this.stage = stage;
    }

    public static void encode(PacketSyncQuest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.title);
        buf.writeUtf(msg.objective);
        buf.writeBlockPos(msg.targetPos);
        buf.writeInt(msg.stage);
    }

    public static PacketSyncQuest decode(FriendlyByteBuf buf) {
        return new PacketSyncQuest(buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readInt());
    }

    public static void handle(PacketSyncQuest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            QuestManager.setQuestData(msg.title, msg.objective, msg.targetPos, msg.stage);
        });
        ctx.get().setPacketHandled(true);
    }
}
