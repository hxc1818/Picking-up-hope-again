package com.resilienthope.core.network;

import com.resilienthope.core.account.AccountSessionManager;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
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

            boolean paid = PacketEquipItem.consumeItem(player, ModItems.STAR_TICKET.get(), msg.count)
                    || PacketEquipItem.consumeItem(player, ModItems.QUANTUM_CORE.get(), msg.count * 10);

            if (!paid) {
                PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("抽卡系统",
                        "抽卡通行券不足！单抽需 1 张通行券，十连需 10 张（完成主线或探索可得）。"));
                return;
            }

            List<ItemStack> results = new ArrayList<>();
            Random rng = player.getRandom();

            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                for (int i = 0; i < msg.count; i++) {
                    int pity = team.getPityCounter() + 1;
                    boolean forceFive = (pity >= 80);
                    boolean forceFourOrFive = (i == msg.count - 1 && msg.count == 10);

                    ItemStack displayCard = rollReward(player, team, rng, forceFive, forceFourOrFive);
                    results.add(displayCard);

                    int rarity = displayCard.getOrCreateTag().getInt("WishRarity");
                    if (rarity >= 5) {
                        team.setPityCounter(0);
                    } else {
                        team.setPityCounter(pity);
                    }
                }
                PacketSyncTeamData.syncToPlayer(player);
            });

            player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.3f);
            PacketHandler.sendToPlayer(player, new PacketWishResult(results));
            AccountSessionManager.scheduleAutoPush(player, "wish");
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack rollReward(ServerPlayer player, TeamCapability.ITeamData team, Random rng,
                                        boolean forceFive, boolean forceFourOrFive) {
        int roll = rng.nextInt(100);
        if (forceFive || roll < 6) {
            // 5-Star Pool: Characters (Han Bing, Zhou Min) or 5-star Weapon / 5-tier Chip
            int pick = rng.nextInt(4);
            if (pick == 0) {
                return unlockCharCard(player, team, "hanbing", "狙击手·韩冰", "火·对单", 5);
            } else if (pick == 1) {
                return unlockCharCard(player, team, "zhoumin", "调度员·周敏", "水·增益", 5);
            } else if (pick == 2) {
                ItemStack w = new ItemStack(ModItems.PRECURSOR_CLAYMORE.get());
                EquipmentStatsHelper.ensureStats(w, rng, 5);
                giveToPlayer(player, w.copy());
                return tagWishCard(w, 5, "");
            } else {
                ItemStack chip = EquipmentStatsHelper.createChipWithRarity(ModItems.NEURAL_HALO_CHIP.get(), 5, rng);
                giveToPlayer(player, chip.copy());
                return tagWishCard(chip, 5, "");
            }
        } else if (forceFourOrFive || roll < 26) {
            // 4-Star Pool: Character Lao Zheng, 4-star Weapons, or 4-tier Chips
            int pick = rng.nextInt(5);
            if (pick == 0) {
                return unlockCharCard(player, team, "laozheng", "防护员·老郑", "火·拉盾", 4);
            } else if (pick == 1) {
                ItemStack w = new ItemStack(ModItems.CRYO_SAWBLADE.get());
                EquipmentStatsHelper.ensureStats(w, rng, 4);
                giveToPlayer(player, w.copy());
                return tagWishCard(w, 4, "");
            } else {
                ItemStack chip = EquipmentStatsHelper.createChipWithRarity(ModItems.OVERCLOCK_CHIP.get(), 4, rng);
                giveToPlayer(player, chip.copy());
                return tagWishCard(chip, 4, "");
            }
        } else {
            // 3-Star Pool: 3-star Weapons, 3-tier Chips, or EXP Bottle
            int pick = rng.nextInt(4);
            ItemStack item;
            if (pick == 0) item = new ItemStack(ModItems.HYDRO_PULSE_GUN.get());
            else if (pick == 1) item = new ItemStack(ModItems.AERO_BLADE.get());
            else if (pick == 2) item = EquipmentStatsHelper.createChipWithRarity(ModItems.SERVO_BATTERY_CHIP.get(), 3, rng);
            else item = new ItemStack(ModItems.EXP_BOTTLE.get(), 2);

            EquipmentStatsHelper.ensureStats(item, rng, 3);
            giveToPlayer(player, item.copy());
            return tagWishCard(item, 3, "");
        }
    }

    private static ItemStack unlockCharCard(ServerPlayer player, TeamCapability.ITeamData team,
                                            String charId, String name, String roleInfo, int rarity) {
        boolean already = team.getCharacterById(charId) != null && team.getCharacterById(charId).isUnlocked();
        team.unlockRosterCharacter(charId, true);
        if (already) {
            giveToPlayer(player, new ItemStack(ModItems.EQUIP_ESSENCE.get(), rarity == 5 ? 6 : 3));
        }
        ItemStack card = new ItemStack(ModItems.STAR_TICKET.get());
        card.setHoverName(new TextComponent(name + (already ? " (潜能+1)" : " [新角色]")));
        CompoundTag tag = card.getOrCreateTag();
        tag.putString("WishCharId", charId);
        tag.putString("WishSubLabel", roleInfo);
        tag.putInt("WishRarity", rarity);
        return card;
    }

    private static ItemStack tagWishCard(ItemStack stack, int rarity, String subLabel) {
        ItemStack copy = stack.copy();
        CompoundTag tag = copy.getOrCreateTag();
        tag.putInt("WishRarity", rarity);
        if (!subLabel.isEmpty()) tag.putString("WishSubLabel", subLabel);
        return copy;
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
