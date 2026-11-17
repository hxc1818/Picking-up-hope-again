package com.resilienthope.core.block;

import com.resilienthope.core.subtitle.RadioSubtitleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;

public class WaypointBlock extends Block {

    public WaypointBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL)
                .strength(3.0f, 10.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> 12)
                .noOcclusion());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            RadioSubtitleManager.postRadioMessage("星舰信标中继站", "传送锚点同步成功！坐标信号已录入星图，小队状态已完全恢复。", 120);
        } else {
            player.setHealth(player.getMaxHealth());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
