package com.resilienthope.core.block;

import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.network.PacketSyncQuest;
import com.resilienthope.core.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

public class AuxiliaryEnergyCoreBlock extends Block {

    public AuxiliaryEnergyCoreBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL)
                .strength(5.0f, 15.0f)
                .sound(SoundType.BEACON)
                .lightLevel(state -> 15)
                .noOcclusion());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int stage = QuestManager.getQuestStage();
            if (stage == 3) {
                // Complete Chapter 1: Overload restored!
                QuestManager.advanceQuest(4, "【第一章·深空回响】", "辅助聚变核心已恢复，前往甲板准备跃迁", new BlockPos(32, 61, 10));

                ServerLevel sLevel = (ServerLevel) level;
                sLevel.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0, 0, 0, 0);
                sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.5, 0.5, 0.5, 0.1);
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 1.6f);

                // Rewards: 1600 Quantum Cores for gacha reproduction
                ItemStack cores = new ItemStack(ModItems.QUANTUM_CORE.get(), 64);
                serverPlayer.getInventory().add(cores);

                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("星舰导航中枢", "【全舰通告】辅助能源核心修复完成！空间跃迁引擎预热中，星图信号恢复正常！", 180));

                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("机械公会 罗德", "干得太出色了特工！这批「星辉算力芯片」是你应得的，去仓储终端启动祈愿复现吧！", 220));

                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketSyncQuest(QuestManager.getQuestTitle(), QuestManager.getObjectiveText(), QuestManager.getTargetPos(), 4));
            } else {
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.2f);
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("辅助能源核心", "核心磁场约束运转稳定，输出功率 100%。", 80));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
