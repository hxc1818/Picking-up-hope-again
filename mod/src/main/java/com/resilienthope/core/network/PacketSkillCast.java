package com.resilienthope.core.network;

import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public class PacketSkillCast {
    private final int skillType; // 0 = E (Tactical), 1 = Q (Burst)

    public PacketSkillCast(int skillType) {
        this.skillType = skillType;
    }

    public static void encode(PacketSkillCast msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.skillType);
    }

    public static PacketSkillCast decode(FriendlyByteBuf buf) {
        return new PacketSkillCast(buf.readInt());
    }

    public static void handle(PacketSkillCast msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.isDeadOrDying()) return;

            ServerLevel level = player.getLevel();

            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                int activeSlot = team.getActiveSlotIndex();
                CharacterData character = team.getCharacter(activeSlot);

                if (msg.skillType == 0) {
                    // Tactical Skill (E)
                    executeTacticalSkill(player, level, activeSlot, character);
                } else if (msg.skillType == 1) {
                    // Ultimate Burst (Q)
                    executeUltimateBurst(player, level, activeSlot, character);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void executeTacticalSkill(ServerPlayer player, ServerLevel level, int slot, CharacterData character) {
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position();

        switch (slot) {
            case 0 -> {
                // Vanguard: Plasma Blade Dash (5m forward, 15 AoE damage)
                Vec3 dashTarget = pos.add(look.x * 5.0, 0, look.z * 5.0);
                player.teleportTo(dashTarget.x, dashTarget.y, dashTarget.z);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, dashTarget.x, dashTarget.y + 1, dashTarget.z, 30, 0.5, 0.5, 0.5, 0.1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.4f);

                AABB hitBox = new AABB(dashTarget.x - 4, dashTarget.y - 2, dashTarget.z - 4, dashTarget.x + 4, dashTarget.y + 2, dashTarget.z + 4);
                List<Monster> targets = level.getEntitiesOfClass(Monster.class, hitBox);
                for (Monster m : targets) {
                    m.hurt(DamageSource.playerAttack(player), 15.0f);
                }
                character.addEnergy(35.0f);
            }
            case 1 -> {
                // Shadow: EMP Shockwave (Knockback + Stun + 12 AoE damage)
                level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 1, pos.z, 15, 0.8, 0.8, 0.8, 0.05);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.8f);

                AABB hitBox = player.getBoundingBox().inflate(6.0);
                List<Monster> targets = level.getEntitiesOfClass(Monster.class, hitBox);
                for (Monster m : targets) {
                    m.hurt(DamageSource.playerAttack(player), 12.0f);
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    Vec3 push = m.position().subtract(pos).normalize().scale(1.2);
                    m.setDeltaMovement(push.x, 0.4, push.z);
                }
                character.addEnergy(35.0f);
            }
            case 2 -> {
                // Titan: Kinetic Absorption Barrier (Shield 8 HP + Resistance)
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y + 1, pos.z, 25, 0.6, 0.6, 0.6, 0.1);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
                character.addEnergy(30.0f);
            }
            case 3 -> {
                // Medic: Nano Restoration Burst (Heal 8 HP + Regeneration)
                player.heal(8.0f);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1, pos.z, 20, 0.5, 0.5, 0.5, 0.05);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.5f);
                character.addEnergy(30.0f);
            }
        }
    }

    private static void executeUltimateBurst(ServerPlayer player, ServerLevel level, int slot, CharacterData character) {
        if (character.getCurrentEnergy() < character.getMaxEnergy()) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new PacketRadioDialogue("战术核心", "能量不足！终极爆发尚未充能完毕 (" + (int)character.getCurrentEnergy() + "/100)。", 60));
            return;
        }

        // Consume Burst Energy
        character.addEnergy(-character.getMaxEnergy());

        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.5, pos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.5, pos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.5, pos.z, 80, 2.0, 1.0, 2.0, 0.2);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0f, 1.4f);

        AABB hitBox = player.getBoundingBox().inflate(10.0);
        List<Monster> targets = level.getEntitiesOfClass(Monster.class, hitBox);
        for (Monster m : targets) {
            m.hurt(DamageSource.playerAttack(player), 35.0f);
        }

        player.heal(12.0f);

        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketRadioDialogue("星舰战术中枢", "【终极爆发·星火裁决】全功率释放！辐射冲击波已清空范围威胁！", 140));
    }
}
