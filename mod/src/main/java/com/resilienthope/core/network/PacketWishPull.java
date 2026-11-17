package com.resilienthope.core.network;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class PacketWishPull {
    private final int count;

    public PacketWishPull(int count) {
        this.count = (count >= 10) ? 10 : 1;
    }

    public static void encode(PacketWishPull msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.count);
    }

    public static PacketWishPull decode(FriendlyByteBuf buf) {
        return new PacketWishPull(buf.readInt());
    }

    public static void handle(PacketWishPull msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            int cost = msg.count * 10;
            if (!PacketEquipItem.consumeItem(player, ModItems.QUANTUM_CORE.get(), cost)) {
                PacketHandler.sendToPlayer(player, new PacketRadioDialogue("终端数据复现",
                        "星辉算力芯片不足！单次复现需 10 枚，十连复现需 100 枚。", 90));
                return;
            }

            List<ItemStack> results = new ArrayList<>();
            Random rng = player.getRandom();
            for (int i = 0; i < msg.count; i++) {
                ItemStack reward = rollReward(rng, i == msg.count - 1 && msg.count == 10);
                EquipmentStatsHelper.ensureStats(reward, rng);
                results.add(reward.copy());
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
            }

            player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.3f);
            PacketHandler.sendToPlayer(player, new PacketWishResult(results));
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack rollReward(Random rng, boolean guaranteedHigh) {
        int roll = rng.nextInt(100);
        if (roll < 8 || (guaranteedHigh && roll < 25)) {
            int pick = rng.nextInt(3);
            return switch (pick) {
                case 0 -> new ItemStack(ModItems.PRECURSOR_CLAYMORE.get());
                case 1 -> new ItemStack(ModItems.NEURAL_HALO_CHIP.get());
                default -> new ItemStack(ModItems.OVERCLOCK_CHIP.get());
            };
        } else if (roll < 55 || guaranteedHigh) {
            int pick = rng.nextInt(5);
            return switch (pick) {
                case 0 -> new ItemStack(ModItems.CRYO_SAWBLADE.get());
                case 1 -> new ItemStack(ModItems.PLASMA_CUTTER.get());
                case 2 -> new ItemStack(ModItems.FUSION_CORE_CHIP.get());
                case 3 -> new ItemStack(ModItems.VOID_DRIVE_CHIP.get());
                default -> new ItemStack(ModItems.SERVO_BATTERY_CHIP.get());
            };
        } else {
            return new ItemStack(ModItems.ALLOY_PLATE.get(), 3 + rng.nextInt(3));
        }
    }
}
