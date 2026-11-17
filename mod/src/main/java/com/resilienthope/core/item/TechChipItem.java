package com.resilienthope.core.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class TechChipItem extends Item {
    private final int chipSlot;

    public TechChipItem(int chipSlot, Properties properties) {
        super(properties);
        this.chipSlot = chipSlot;
    }

    public int getChipSlot() {
        return chipSlot;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int lv = EquipmentStatsHelper.getEnhanceLevel(stack);
        int rarity = EquipmentStatsHelper.getRarity(stack);
        tooltip.add(new TextComponent(EquipmentStatsHelper.getChipSlotTitle(chipSlot)).withStyle(ChatFormatting.AQUA));
        tooltip.add(new TextComponent(EquipmentStatsHelper.getStarString(rarity) + "  +" + lv).withStyle(ChatFormatting.GOLD));
        tooltip.add(new TextComponent("主词条: " + EquipmentStatsHelper.formatMainStatLabel(stack)).withStyle(ChatFormatting.GREEN));
        for (String sub : EquipmentStatsHelper.getSubStatLines(stack)) {
            tooltip.add(new TextComponent("  • " + sub).withStyle(ChatFormatting.GRAY));
        }
    }
}
