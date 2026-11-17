package com.resilienthope.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PacketEquipItem {
    private final ItemStack stack;

    public PacketEquipItem(ItemStack stack) {
        this.stack = stack;
    }

    public static void encode(PacketEquipItem msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.stack);
    }

    public static PacketEquipItem decode(FriendlyByteBuf buf) {
        return new PacketEquipItem(buf.readItem());
    }

    public static void handle(PacketEquipItem msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || msg.stack.isEmpty()) return;

            player.setItemInHand(InteractionHand.MAIN_HAND, msg.stack.copy());
            player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.0f, 1.2f);

            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new PacketRadioDialogue("战术装备库", "武器/芯片装备成功：" + msg.stack.getHoverName().getString(), 80));
        });
        ctx.get().setPacketHandled(true);
    }
}
