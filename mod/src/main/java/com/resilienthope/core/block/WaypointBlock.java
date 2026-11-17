package com.resilienthope.core.block;

import com.resilienthope.core.entity.SecurityDroneEntity;
import com.resilienthope.core.init.ModEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

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
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Restore team health
            serverPlayer.setHealth(serverPlayer.getMaxHealth());

            ServerLevel serverLevel = (ServerLevel) level;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.1);
            serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.4f);

            if (QuestManager.getQuestStage() == 0) {
                QuestManager.resetDroneKills();
                QuestManager.advanceQuest(1, "【序章·熄灭的炉火】", "清除维修连廊中的失控安保无人机 (0/2)", new BlockPos(18, 62, 10));

                // Spawn 2 Corrupted Security Drones in the corridor
                SecurityDroneEntity drone1 = ModEntities.SECURITY_DRONE.get().create(serverLevel);
                if (drone1 != null) {
                    drone1.moveTo(18.5, 62.5, 9.5, 0.0f, 0.0f);
                    serverLevel.addFreshEntity(drone1);
                }

                SecurityDroneEntity drone2 = ModEntities.SECURITY_DRONE.get().create(serverLevel);
                if (drone2 != null) {
                    drone2.moveTo(19.5, 62.5, 11.5, 0.0f, 0.0f);
                    serverLevel.addFreshEntity(drone2);
                }

                // Sync quest to player
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketSyncQuest(QuestManager.getQuestTitle(), QuestManager.getObjectiveText(), QuestManager.getTargetPos(), 1));

                // Broadcast story transmissions
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("星舰信标中继站", "传送锚点同步成功！坐标信号已录入星图，小队状态已完全恢复。", 140));

                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("机械公会 罗德", "喂！听到广播了吗？维修通道的安保无人机核心过载叛变了，快用等离子切割刃解决它们！", 220));
            } else {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketRadioDialogue("星舰信标中继站", "锚点能量充盈。生命维持系统运转正常。", 80));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
