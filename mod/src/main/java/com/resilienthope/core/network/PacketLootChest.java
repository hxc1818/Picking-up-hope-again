package com.resilienthope.core.network;

import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.quest.StoryDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

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
                        ItemStack copy = stack.copy();
                        EquipmentStatsHelper.ensureStats(copy, player.getRandom());
                        if (!player.getInventory().add(copy)) {
                            player.drop(copy, false);
                        }
                        container.setItem(i, ItemStack.EMPTY);
                        count++;
                    }
                }
                if (count > 0) {
                    player.getLevel().playSound(null, msg.chestPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f);
                    PacketHandler.sendToPlayer(player, new PacketRadioDialogue("物资箱拾取",
                            "物资已存入背包！按 [B] 查看装备与材料，按 [C] 为角色装配芯片或使用经验瓶。", 120));
                    if (QuestManager.getQuestStage() == 0) {
                        StoryDirector.onSupplyCrateOpened(player, msg.chestPos);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
