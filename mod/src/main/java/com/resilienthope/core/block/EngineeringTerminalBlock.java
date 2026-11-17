package com.resilienthope.core.block;

import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketOpenNpcDialogue;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class EngineeringTerminalBlock extends Block {

    public EngineeringTerminalBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL)
                .strength(4.0f, 12.0f)
                .sound(SoundType.NETHERITE_BLOCK)
                .lightLevel(state -> 10)
                .noOcclusion());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            int stage = QuestManager.getQuestStage();
            if (stage < 2) {
                level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.8f, 0.8f);
                PacketHandler.sendToPlayer(serverPlayer, new PacketRadioDialogue("工程总控终端",
                        "【安全锁定】维修连廊警报尚未解除，请先消灭失控无人机并同步传送锚点！", 120));
            } else if (stage == 2 || stage == 7) {
                PacketHandler.sendToPlayer(serverPlayer, new PacketOpenNpcDialogue("zheng", stage));
            } else {
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.4f);
                PacketHandler.sendToPlayer(serverPlayer, new PacketRadioDialogue("工程总控终端",
                        "主控核心运转稳定。按 [G] 可随时开启星辉终端数据复现（抽卡），按 [C] 查看角色芯片面板！", 130));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
