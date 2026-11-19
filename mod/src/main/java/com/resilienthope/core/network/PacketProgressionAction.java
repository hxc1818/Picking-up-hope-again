package com.resilienthope.core.network;

import com.resilienthope.core.account.AccountSessionManager;
import com.resilienthope.core.combat.TurnBasedCombatManager;
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

public class PacketProgressionAction {
    public static final int ACT_USE_EXP_BOTTLE = 0;
    public static final int ACT_ASCEND_CHAR = 1;
    public static final int ACT_UPGRADE_TALENT = 2;
    public static final int ACT_ASCEND_WEAPON = 3;
    public static final int ACT_DISSOLVE_CHIP = 4;
    public static final int ACT_CRAFT_MAT_UP = 5;
    public static final int ACT_START_DOMAIN = 6;
    public static final int ACT_SET_PARTY_SLOT = 7;

    private final int actionType;
    private final int paramA;
    private final int paramB;
    private final String strParam;

    public PacketProgressionAction(int actionType, int paramA, int paramB, String strParam) {
        this.actionType = actionType;
        this.paramA = paramA;
        this.paramB = paramB;
        this.strParam = strParam != null ? strParam : "";
    }

    public static void encode(PacketProgressionAction msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.actionType);
        buf.writeInt(msg.paramA);
        buf.writeInt(msg.paramB);
        buf.writeUtf(msg.strParam);
    }

    public static PacketProgressionAction decode(FriendlyByteBuf buf) {
        return new PacketProgressionAction(buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketProgressionAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                switch (msg.actionType) {
                    case ACT_USE_EXP_BOTTLE -> handleExpBottle(player, team);
                    case ACT_ASCEND_CHAR -> handleAscendChar(player, team);
                    case ACT_UPGRADE_TALENT -> handleUpgradeTalent(player, team);
                    case ACT_ASCEND_WEAPON -> handleAscendWeapon(player, team);
                    case ACT_DISSOLVE_CHIP -> handleDissolveChip(player, msg.paramA);
                    case ACT_CRAFT_MAT_UP -> handleCraftMatUp(player, msg.paramA);
                    case ACT_START_DOMAIN -> TurnBasedCombatManager.startDomainBattle(player, msg.strParam);
                    case ACT_SET_PARTY_SLOT -> {
                        team.setPartyRosterIndex(msg.paramA, msg.paramB);
                        team.syncHealthToPlayer(player);
                    }
                }
                PacketSyncTeamData.syncToPlayer(player);
                if (msg.actionType != ACT_START_DOMAIN) {
                    AccountSessionManager.scheduleAutoPush(player, "progression");
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleExpBottle(ServerPlayer player, TeamCapability.ITeamData team) {
        CharacterData active = team.getCharacter(team.getActiveSlotIndex());
        int cap = active.getMaxLevelForAscension();
        if (active.getLevel() >= cap) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("角色养成",
                    active.getName() + " 已达当前突破上限 Lv." + cap + "！请先进行角色突破（或提升世界等级）。"));
            return;
        }
        boolean consumed = PacketEquipItem.consumeItem(player, ModItems.EXP_BOTTLE.get(), 1)
                || PacketEquipItem.consumeItem(player, Items.EXPERIENCE_BOTTLE, 1);
        if (!consumed) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("角色养成",
                    "角色经验瓶不足！可前往模拟训练副本或探索物资箱获取。"));
            return;
        }
        active.setLevel(active.getLevel() + 1);
        active.setCurrentHp(active.getTotalMaxHp());
        team.syncHealthToPlayer(player);
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9f, 1.4f);
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("升级成功",
                active.getName() + " 升至 Lv." + active.getLevel() + "/" + cap));
        StoryDirector.onCharacterUpgraded(player, active.getLevel());
    }

    private static void handleAscendChar(ServerPlayer player, TeamCapability.ITeamData team) {
        CharacterData active = team.getCharacter(team.getActiveSlotIndex());
        int asc = active.getAscension();
        if (asc >= 6) return;
        if (asc >= team.getMaxAscensionAllowed()) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("突破限制",
                    "受当前世界等级 Lv." + team.getWorldLevel() + " 限制，需先完成【世界等级突破】才可继续突破角色！"));
            return;
        }
        int tier = Math.min(6, asc + 1);
        int reqMat = 2 + asc;
        Item mat = ModItems.getCharMat(tier);
        if (!PacketEquipItem.consumeItem(player, mat, reqMat)) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("材料不足",
                    "角色突破需要 " + reqMat + " 个【" + tier + "级人物零件】，可在背包合成或刷取副本获得。"));
            return;
        }
        active.setAscension(asc + 1);
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("突破成功",
                active.getName() + " 已突破！等级上限提升至 Lv." + active.getMaxLevelForAscension()));
    }

    private static void handleUpgradeTalent(ServerPlayer player, TeamCapability.ITeamData team) {
        CharacterData active = team.getCharacter(team.getActiveSlotIndex());
        int tLv = active.getTalentLevel();
        if (tLv >= 10) return;
        int tier = Math.min(6, 1 + (tLv / 2));
        int reqCount = 1 + (tLv % 2);
        Item mat = ModItems.getTalentMat(tier);
        if (!PacketEquipItem.consumeItem(player, mat, reqCount)) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("天赋升级",
                    "升级天赋需要 " + reqCount + " 本【" + tier + "级天赋手册】，人物/武器/天赋材料彼此独立！"));
            return;
        }
        active.setTalentLevel(tLv + 1);
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 1.3f);
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("天赋升级",
                active.getName() + " 技能天赋已升至 Lv." + active.getTalentLevel() + "！"));
    }

    private static void handleAscendWeapon(ServerPlayer player, TeamCapability.ITeamData team) {
        CharacterData active = team.getCharacter(team.getActiveSlotIndex());
        ItemStack weapon = active.getEquippedWeapon();
        if (weapon.isEmpty()) return;
        int asc = EquipmentStatsHelper.getAscension(weapon);
        if (asc >= 6) return;
        if (asc >= team.getMaxAscensionAllowed()) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("武器突破限制",
                    "需先完成【世界等级突破】才可继续提升武器阶数！"));
            return;
        }
        int tier = Math.min(6, asc + 1);
        int reqCount = 2 + asc;
        Item mat = ModItems.getWeaponMat(tier);
        if (!PacketEquipItem.consumeItem(player, mat, reqCount)) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("材料不足",
                    "武器突破需要 " + reqCount + " 个【" + tier + "级武器构件】！"));
            return;
        }
        EquipmentStatsHelper.setAscension(weapon, asc + 1);
        active.setEquippedWeapon(weapon);
        player.getInventory().setItem(0, weapon.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9f, 1.3f);
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("武器突破成功",
                weapon.getHoverName().getString() + " 等级上限开放至 Lv." + EquipmentStatsHelper.getMaxEnhanceLevel(weapon)));
    }

    private static void handleDissolveChip(ServerPlayer player, int invIdx) {
        if (invIdx < 0 || invIdx >= player.getInventory().getContainerSize()) return;
        ItemStack stack = player.getInventory().getItem(invIdx);
        if (!EquipmentStatsHelper.isChip(stack)) return;

        int yield = EquipmentStatsHelper.getDissolveEssenceYield(stack);
        player.getInventory().setItem(invIdx, ItemStack.EMPTY);
        ItemStack dust = new ItemStack(ModItems.EQUIP_ESSENCE.get(), yield);
        if (!player.getInventory().add(dust)) {
            player.drop(dust, false);
        }
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.9f, 1.2f);
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("装备溶解",
                "已溶解多余装备，提炼出【装备强化粉尘】 x" + yield + "！"));
    }

    private static void handleCraftMatUp(ServerPlayer player, int invIdx) {
        if (invIdx < 0 || invIdx >= player.getInventory().getContainerSize()) return;
        ItemStack stack = player.getInventory().getItem(invIdx);
        if (stack.isEmpty()) return;
        Item next = ModItems.getNextTierMaterial(stack.getItem());
        if (next == null) return;
        if (!PacketEquipItem.consumeItem(player, stack.getItem(), 3)) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("材料合成",
                    "合成高一级材料需要 3 个同系列低级材料！"));
            return;
        }
        ItemStack crafted = new ItemStack(next, 1);
        if (!player.getInventory().add(crafted)) {
            player.drop(crafted, false);
        }
        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("材料合成成功",
                "已合成高阶材料：" + crafted.getHoverName().getString()));
    }
}
