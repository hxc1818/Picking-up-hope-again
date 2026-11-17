package com.resilienthope.core.entity;

import com.resilienthope.core.init.ModEntities;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.quest.StoryDirector;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class PrecursorMechBossEntity extends Monster {
    private int skillCooldown = 100;
    private boolean phaseTwoTriggered = false;

    public PrecursorMechBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.setCustomName(new TextComponent("§e【世界首领】先驱重型挖掘机甲·泰坦"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 360.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide() || !this.isAlive()) return;

        ServerLevel serverLevel = (ServerLevel) this.level;
        if (!phaseTwoTriggered && this.getHealth() <= this.getMaxHealth() * 0.5f) {
            phaseTwoTriggered = true;
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.9f, 1.4f);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.8, getZ(), 50, 1.2, 1.0, 1.2, 0.2);

            // Summon 2 escort drones
            for (int i = -1; i <= 1; i += 2) {
                SecurityDroneEntity escort = ModEntities.SECURITY_DRONE.get().create(serverLevel);
                if (escort != null) {
                    escort.moveTo(getX() + i * 3.0, getY() + 1.5, getZ() + 2.0, 0f, 0f);
                    serverLevel.addFreshEntity(escort);
                }
            }
            Player nearest = serverLevel.getNearestPlayer(this, 45.0D);
            if (nearest instanceof ServerPlayer sp) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                        new PacketRadioDialogue("先驱机甲·警戒核心", "【二阶段过载】护卫无人机阵列展开！地震冲击波蓄能中！", 120));
            }
        }

        if (skillCooldown > 0) {
            skillCooldown--;
        } else {
            Player target = serverLevel.getNearestPlayer(this, 12.0D);
            if (target instanceof ServerPlayer sp) {
                skillCooldown = 110;
                // Seismic Shockwave attack
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.4, getZ(), 6, 2.5, 0.2, 2.5, 0.05);
                serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.1f, 0.6f);

                AABB aoe = this.getBoundingBox().inflate(5.5);
                List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, aoe);
                for (ServerPlayer hit : players) {
                    hit.hurt(DamageSource.mobAttack(this), 4.0f);
                    hit.setDeltaMovement(hit.position().subtract(this.position()).normalize().scale(0.8).add(0, 0.35, 0));
                }
            } else {
                skillCooldown = 40;
            }
        }
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.2, getZ(), 2, 0.5, 0.5, 0.5, 0.1);
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4f, 0.8f);

            ServerPlayer killer = (cause.getEntity() instanceof ServerPlayer sp) ? sp :
                    (serverLevel.getNearestPlayer(this, 50.0D) instanceof ServerPlayer np ? np : null);
            if (killer != null) {
                StoryDirector.onBossDefeated(killer);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
