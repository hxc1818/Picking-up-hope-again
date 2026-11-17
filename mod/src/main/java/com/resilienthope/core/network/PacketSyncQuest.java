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
    private final int kills;
    private final int required;
    private final int flags;
    private final String ending;

    public PacketSyncQuest(String title, String objective, BlockPos targetPos, int stage) {
        this(title, objective, targetPos, stage, QuestManager.getDronesKilled(), QuestManager.getRequiredDrones(), QuestManager.getSubFlags(), QuestManager.getEndingFaction());
    }

    public PacketSyncQuest(String title, String objective, BlockPos targetPos, int stage, int kills, int required, int flags, String ending) {
        this.title = title;
        this.objective = objective;
        this.targetPos = targetPos;
        this.stage = stage;
        this.kills = kills;
        this.required = required;
        this.flags = flags;
        this.ending = ending != null ? ending : "";
    }

    public static void encode(PacketSyncQuest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.title);
        buf.writeUtf(msg.objective);
        buf.writeBlockPos(msg.targetPos);
        buf.writeInt(msg.stage);
        buf.writeInt(msg.kills);
        buf.writeInt(msg.required);
        buf.writeInt(msg.flags);
        buf.writeUtf(msg.ending);
    }

    public static PacketSyncQuest decode(FriendlyByteBuf buf) {
        return new PacketSyncQuest(
                buf.readUtf(),
                buf.readUtf(),
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf()
        );
    }

    public static void handle(PacketSyncQuest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            QuestManager.setQuestData(msg.title, msg.objective, msg.targetPos, msg.stage, msg.kills, msg.required, msg.flags, msg.ending);
        });
        ctx.get().setPacketHandled(true);
    }
}
