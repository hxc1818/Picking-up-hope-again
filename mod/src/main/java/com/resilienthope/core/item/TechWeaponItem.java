package com.resilienthope.core.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class TechWeaponItem extends SwordItem {
    public TechWeaponItem(int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(Tiers.NETHERITE, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int lv = EquipmentStatsHelper.getEnhanceLevel(stack);
        int rarity = EquipmentStatsHelper.getRarity(stack);
        tooltip.add(new TextComponent(EquipmentStatsHelper.getStarString(rarity) + "  +" + lv).withStyle(ChatFormatting.GOLD));
        tooltip.add(new TextComponent(EquipmentStatsHelper.formatMainStatLabel(stack)).withStyle(ChatFormatting.AQUA));
        for (String sub : EquipmentStatsHelper.getSubStatLines(stack)) {
            tooltip.add(new TextComponent("  • " + sub).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(new TextComponent(EquipmentStatsHelper.getWeaponPassiveDesc(stack)).withStyle(ChatFormatting.YELLOW));
    }
}
