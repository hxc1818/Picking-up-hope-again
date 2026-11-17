package com.resilienthope.core.entity;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class SecurityDroneEntity extends Monster {

    public SecurityDroneEntity(EntityType<? extends SecurityDroneEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 15, true);
        this.setNoGravity(true);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3, 0.2, 0.2, 0.2, 0.05);
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0f, 1.5f);

            // Quest Hook: Stage 1 Drone Elimination
            if (QuestManager.getQuestStage() == 1) {
                QuestManager.incrementDroneKill();
                int killed = QuestManager.getDronesKilled();
                int required = QuestManager.getRequiredDrones();

                ServerPlayer player = (cause.getEntity() instanceof ServerPlayer sp) ? sp :
                        (this.getKillCredit() instanceof ServerPlayer kp ? kp :
                        (serverLevel.getNearestPlayer(this, 30.0D) instanceof ServerPlayer np ? np : null));

                if (player != null) {
                    if (killed < required) {
                        String progressMsg = "【序章·熄灭的炉火】失控无人机核心已摧毁 (" + killed + "/" + required + ")";
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                                new PacketSyncQuest(QuestManager.getQuestTitle(), progressMsg, QuestManager.getTargetPos(), 1));
                    } else {
                        // All drones defeated! Advance to Stage 2: Rendezvous with Engineer Rhode
                        QuestManager.advanceQuest(2, "【序章·熄灭的炉火】", "前往重工业控制台与工程师罗德会面", new BlockPos(36, 61, 14));
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                                new PacketSyncQuest(QuestManager.getQuestTitle(), QuestManager.getObjectiveText(), QuestManager.getTargetPos(), 2));

                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                                new PacketRadioDialogue("机械公会 罗德", "呼……警报解除！通道重新稳定了，快到工程主控台这边来，我们必须立刻手动重启副反应堆！", 160));
                    }
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(Items.IRON_INGOT, 1));
        this.spawnAtLocation(new ItemStack(Items.REDSTONE, 2));
    }
}
