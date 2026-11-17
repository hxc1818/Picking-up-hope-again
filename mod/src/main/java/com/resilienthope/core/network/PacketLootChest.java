package com.resilienthope.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PacketLootChest {
    private final BlockPos chestPos;

    public PacketLootChest(BlockPos chestPos) {
        this.chestPos = chestPos;
    }

    public static void encode(PacketLootChest msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.chestPos);
    }

    public static PacketLootChest decode(FriendlyByteBuf buf) {
        return new PacketLootChest(buf.readBlockPos());
    }

    public static void handle(PacketLootChest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.getLevel().getBlockEntity(msg.chestPos);
            if (be instanceof Container container) {
                int count = 0;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        player.getInventory().add(stack.copy());
                        container.setItem(i, ItemStack.EMPTY);
                        count++;
                    }
                }
                if (count > 0) {
                    player.getLevel().playSound(null, msg.chestPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f);
                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                            new PacketRadioDialogue("物资管理系统", "已将仓储箱战利品全部收入星舰战备背包！", 100));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
