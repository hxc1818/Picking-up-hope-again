package com.resilienthope.core.network;

import com.resilienthope.core.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBattleSync {
    private final CompoundTag stateTag;

    public PacketBattleSync(CompoundTag stateTag) {
        this.stateTag = stateTag != null ? stateTag : new CompoundTag();
    }

    public static void encode(PacketBattleSync msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.stateTag);
    }

    public static PacketBattleSync decode(FriendlyByteBuf buf) {
        return new PacketBattleSync(buf.readNbt());
    }

    public static void handle(PacketBattleSync msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleBattleSync(msg.stateTag));
        });
        ctx.get().setPacketHandled(true);
    }
}
