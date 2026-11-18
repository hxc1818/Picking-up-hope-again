package com.resilienthope.core.block;

import com.resilienthope.core.client.gui.SimulationDomainScreen;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.quest.QuestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

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
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) {
            if (QuestManager.getQuestStage() >= 10) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        Minecraft.getInstance().setScreen(new SimulationDomainScreen()));
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            if (QuestManager.getQuestStage() < 10) {
                PacketHandler.sendToPlayer(serverPlayer, new PacketRadioDialogue("管理终端",
                        "模拟训练副本模块将在抵达地面科考站安全屋（第 10 阶段）后正式解锁。", 110));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
