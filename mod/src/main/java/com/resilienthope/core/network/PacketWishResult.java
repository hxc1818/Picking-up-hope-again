package com.resilienthope.core.network;

import com.resilienthope.core.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketWishResult {
    private final List<ItemStack> pulls;

    public PacketWishResult(List<ItemStack> pulls) {
        this.pulls = pulls;
    }

    public static void encode(PacketWishResult msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.pulls.size());
        for (ItemStack s : msg.pulls) {
            buf.writeItem(s);
        }
    }

    public static PacketWishResult decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readItem());
        }
        return new PacketWishResult(list);
    }

    public static void handle(PacketWishResult msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleWishResult(msg.pulls));
        });
        ctx.get().setPacketHandled(true);
    }
}
