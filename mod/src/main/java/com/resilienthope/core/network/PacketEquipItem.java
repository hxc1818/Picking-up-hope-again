package com.resilienthope.core.network;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.quest.StoryDirector;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketEquipItem {
    public static final int ACTION_EQUIP_FROM_INV = 0;
    public static final int ACTION_UNEQUIP_CHIP = 1;
    public static final int ACTION_ENHANCE_INV = 2;
    public static final int ACTION_ENHANCE_EQUIPPED = 3;

    private final int actionType;
    private final int slotOrIndex;
    private final ItemStack fallbackStack;

    public PacketEquipItem(int actionType, int slotOrIndex, ItemStack fallbackStack) {
        this.actionType = actionType;
        this.slotOrIndex = slotOrIndex;
        this.fallbackStack = fallbackStack != null ? fallbackStack : ItemStack.EMPTY;
    }

    public static void encode(PacketEquipItem msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.actionType);
        buf.writeInt(msg.slotOrIndex);
        buf.writeItem(msg.fallbackStack);
    }

    public static PacketEquipItem decode(FriendlyByteBuf buf) {
        return new PacketEquipItem(buf.readInt(), buf.readInt(), buf.readItem());
    }

    public static void handle(PacketEquipItem msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                CharacterData active = team.getCharacter(team.getActiveSlotIndex());

                switch (msg.actionType) {
                    case ACTION_EQUIP_FROM_INV -> handleEquip(player, active, msg.slotOrIndex, msg.fallbackStack);
                    case ACTION_UNEQUIP_CHIP -> handleUnequipChip(player, active, msg.slotOrIndex);
                    case ACTION_ENHANCE_INV -> handleEnhanceInv(player, active, msg.slotOrIndex);
                    case ACTION_ENHANCE_EQUIPPED -> handleEnhanceEquipped(player, active, msg.slotOrIndex);
                }

                team.syncHealthToPlayer(player);
                PacketSyncTeamData.syncToPlayer(player);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleEquip(ServerPlayer player, CharacterData active, int invIdx, ItemStack fallback) {
        ItemStack target = ItemStack.EMPTY;
        int resolvedIdx = invIdx;
        if (invIdx >= 0 && invIdx < player.getInventory().getContainerSize()) {
            target = player.getInventory().getItem(invIdx);
        }
        if (target.isEmpty() && !fallback.isEmpty()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty() && s.getItem() == fallback.getItem()) {
                    target = s;
                    resolvedIdx = i;
                    break;
                }
            }
        }
        if (target.isEmpty()) return;

        EquipmentStatsHelper.ensureStats(target, player.getRandom());

        if (EquipmentStatsHelper.isWeapon(target)) {
            ItemStack oldWeapon = active.getEquippedWeapon();
            ItemStack newWeapon = target.copy();
            if (resolvedIdx != 0) {
                player.getInventory().setItem(resolvedIdx, oldWeapon.isEmpty() ? ItemStack.EMPTY : oldWeapon.copy());
                player.getInventory().setItem(0, newWeapon.copy());
                player.getInventory().selected = 0;
                player.setItemInHand(InteractionHand.MAIN_HAND, newWeapon.copy());
            }
            active.setEquippedWeapon(newWeapon);
            player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.0f, 1.25f);
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("装备管理",
                    "已为【" + active.getName() + "】装配武器：" + newWeapon.getHoverName().getString()));
        } else if (EquipmentStatsHelper.isChip(target)) {
            int chipSlot = EquipmentStatsHelper.getChipSlot(target);
            ItemStack oldChip = active.getEquippedChip(chipSlot);
            ItemStack newChip = target.copy();
            newChip.setCount(1);

            if (target.getCount() > 1) {
                target.shrink(1);
                if (!oldChip.isEmpty()) {
                    if (!player.getInventory().add(oldChip.copy())) {
                        player.drop(oldChip.copy(), false);
                    }
                }
            } else {
                player.getInventory().setItem(resolvedIdx, oldChip.isEmpty() ? ItemStack.EMPTY : oldChip.copy());
            }
            active.setEquippedChip(chipSlot, newChip);

            player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9f, 1.4f);
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("装备管理",
                    "已装入 " + EquipmentStatsHelper.getChipSlotTitle(chipSlot) + "：" + newChip.getHoverName().getString()));
        }
    }

    private static void handleUnequipChip(ServerPlayer player, CharacterData active, int chipSlot) {
        if (chipSlot < 0 || chipSlot >= 5) return;
        ItemStack oldChip = active.getEquippedChip(chipSlot);
        if (oldChip.isEmpty()) return;

        active.setEquippedChip(chipSlot, ItemStack.EMPTY);
        if (!player.getInventory().add(oldChip.copy())) {
            player.drop(oldChip.copy(), false);
        }
        player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 0.9f);
    }

    private static void handleEnhanceInv(ServerPlayer player, CharacterData active, int invIdx) {
        if (invIdx < 0 || invIdx >= player.getInventory().getContainerSize()) return;
        ItemStack stack = player.getInventory().getItem(invIdx);
        if (stack.isEmpty()) return;
        if (tryEnhanceStack(player, stack)) {
            if (invIdx == 0 && EquipmentStatsHelper.isWeapon(stack)) {
                active.setEquippedWeapon(stack.copy());
            }
        }
    }

    private static void handleEnhanceEquipped(ServerPlayer player, CharacterData active, int slot) {
        if (slot == -1) {
            ItemStack weapon = active.getEquippedWeapon();
            if (!weapon.isEmpty() && tryEnhanceStack(player, weapon)) {
                active.setEquippedWeapon(weapon);
                player.getInventory().setItem(0, weapon.copy());
                player.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
            }
        } else if (slot >= 0 && slot < 5) {
            ItemStack chip = active.getEquippedChip(slot);
            if (!chip.isEmpty() && tryEnhanceStack(player, chip)) {
                active.setEquippedChip(slot, chip);
            }
        }
    }

    private static boolean tryEnhanceStack(ServerPlayer player, ItemStack stack) {
        if (!EquipmentStatsHelper.isWeapon(stack) && !EquipmentStatsHelper.isChip(stack)) return false;
        int curLv = EquipmentStatsHelper.getEnhanceLevel(stack);
        int maxLv = EquipmentStatsHelper.getMaxEnhanceLevel(stack);
        if (curLv >= maxLv) {
            String hint = EquipmentStatsHelper.isWeapon(stack)
                    ? "该武器已达当前突破上限 Lv." + maxLv + "，请先消耗武器突破构件进行突破！"
                    : "该 " + EquipmentStatsHelper.getRarity(stack) + " 级装备已强化至满级 (+" + maxLv + ")！";
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("装备强化台", hint));
            return false;
        }

        if (EquipmentStatsHelper.isChip(stack)) {
            int cost = EquipmentStatsHelper.getEnhanceDustCost(stack);
            if (!consumeItem(player, ModItems.EQUIP_ESSENCE.get(), cost)) {
                PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("装备强化台",
                        "装备强化粉尘不足（需要 " + cost + " 份）！请在背包 [B] 溶解其他多余装备来换取强化粉尘。"));
                return false;
            }
        } else {
            boolean paid = consumeItem(player, ModItems.WEAPON_MAT_1.get(), 1)
                    || consumeItem(player, ModItems.EXP_BOTTLE.get(), 1)
                    || consumeItem(player, Items.EXPERIENCE_BOTTLE, 1);
            if (!paid) {
                PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("武器强化台",
                        "强化武器需要 1 个【1级武器构件】或【经验瓶】！"));
                return false;
            }
        }

        boolean ok = EquipmentStatsHelper.enhanceByOne(stack, player.getRandom());
        if (ok) {
            int newLv = EquipmentStatsHelper.getEnhanceLevel(stack);
            player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.75f, 1.35f);
            String extra = (EquipmentStatsHelper.isChip(stack) && newLv % 3 == 0) ? " · 已触发每3级副词条成长！" : "";
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("强化成功",
                    stack.getHoverName().getString() + " 升至 +" + newLv + "/" + maxLv + extra));
            if (EquipmentStatsHelper.isChip(stack)) {
                StoryDirector.onChipEnhanced(player, newLv);
            }
        }
        return ok;
    }

    public static boolean consumeItem(ServerPlayer player, Item item, int count) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        if (total < count) return false;
        int rem = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && rem > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                int take = Math.min(rem, s.getCount());
                s.shrink(take);
                rem -= take;
            }
        }
        return true;
    }
}
