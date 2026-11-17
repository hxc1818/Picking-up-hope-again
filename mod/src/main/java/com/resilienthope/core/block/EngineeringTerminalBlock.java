package com.resilienthope.core.block;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.network.PacketSyncQuest;
import com.resilienthope.core.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

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
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int stage = QuestManager.getQuestStage();
            if (stage == 2) {
                // Successfully complete prologue!
                QuestManager.advanceQuest(3, "【第一章·深空回响】", "前往下层工业甲板寻找星核能源", new BlockPos(32, 50, 10));

                // Reward: Fusion Core Chip
                serverPlayer.getInventory().add(new ItemStack(ModItems.FUSION_CORE_CHIP.get()));

                // Sound effect
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);

                // Dialogues
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("机械公会 罗德", "干得漂亮！副反应堆成功重燃，全舰应急电力恢复了30%。", 160));

                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("机械公会 罗德", "这是工会研发的「聚变脉冲核心芯片」，拿着！它能强化你的动力装甲。下层甲板已解锁，快去调查！", 220));

                // Sync Quest
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketSyncQuest(QuestManager.getQuestTitle(), QuestManager.getObjectiveText(), QuestManager.getTargetPos(), 3));

            } else if (stage < 2) {
                level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.8f, 0.8f);
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("工程总控终端", "【系统警告】警卫防线被突破，请先摧毁维修连廊中的失控安保无人机！", 120));
            } else {
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.4f);
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("机械公会 罗德", "副反应堆运转平稳。特工，尽快前往下层工业甲板，我们随时保持通讯！", 120));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
