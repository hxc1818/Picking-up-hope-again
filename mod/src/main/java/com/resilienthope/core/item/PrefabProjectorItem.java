package com.resilienthope.core.item;

import com.resilienthope.core.client.builder.BuilderPrefabClientActions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Creative builder wand that renders a 3D wireframe preview of the active reusable structure / prefab
 * and lets builders rotate, stamp, repeat, undo, or open the Prefab Library GUI.
 */
public class PrefabProjectorItem extends Item {
    public PrefabProjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide && player != null) {
            boolean shift = player.isShiftKeyDown();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BuilderPrefabClientActions.onRightClick(shift));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            boolean shift = player.isShiftKeyDown();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BuilderPrefabClientActions.onRightClick(shift));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(new TextComponent("右键: 放置当前预览结构").withStyle(ChatFormatting.AQUA));
        tooltip.add(new TextComponent("Shift+右键: 顺时针旋转 90°").withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent("左键 或 [K]: 打开可复用结构库 (28种)").withStyle(ChatFormatting.GREEN));
        tooltip.add(new TextComponent("Shift+左键: 撤回上一步 (/hope undo)").withStyle(ChatFormatting.GOLD));
    }
}
