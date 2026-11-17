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
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketSkillCast {
    private static class SkillDamageSource extends EntityDamageSource {
        public SkillDamageSource(Entity attacker) {
            super("player", attacker);
            this.bypassArmor();
        }
    }

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
                    executeTacticalSkill(player, level, character);
                } else if (msg.skillType == 1) {
                    executeUltimateBurst(player, level, character);
                }
                team.syncHealthFromPlayer(player);
                PacketSyncTeamData.syncToPlayer(player);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void executeTacticalSkill(ServerPlayer player, ServerLevel level, CharacterData character) {
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position();

        // Safe horizontal dash (~3.8 blocks forward without clipping into walls)
        Vec3 horizontalLook = new Vec3(look.x, 0, look.z);
        if (horizontalLook.lengthSqr() > 0.001) {
            horizontalLook = horizontalLook.normalize();
        } else {
            horizontalLook = new Vec3(1, 0, 0);
        }
        Vec3 dashPos = pos;
        for (double step = 0.8; step <= 3.6; step += 0.7) {
            Vec3 candidate = pos.add(horizontalLook.scale(step));
            if (level.noCollision(player, player.getBoundingBox().move(candidate.subtract(pos)))) {
                dashPos = candidate;
            } else {
                break;
            }
        }
        player.teleportTo(dashPos.x, dashPos.y, dashPos.z);

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, dashPos.x, dashPos.y + 1.0, dashPos.z, 35, 0.7, 0.6, 0.7, 0.15);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, dashPos.x + horizontalLook.x, dashPos.y + 1.1, dashPos.z + horizontalLook.z, 3, 0.4, 0.2, 0.4, 0.0);
        level.playSound(null, dashPos.x, dashPos.y, dashPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.35f);

        AABB hitBox = new AABB(dashPos.x - 4.2, dashPos.y - 2.0, dashPos.z - 4.2, dashPos.x + 4.2, dashPos.y + 2.5, dashPos.z + 4.2);
        List<Monster> targets = level.getEntitiesOfClass(Monster.class, hitBox);
        for (Monster m : targets) {
            CharacterData.DamageResult res = character.computeDamage(2.4f, player.getRandom());
            m.hurt(new SkillDamageSource(player), res.mcDamage());
            PacketHandler.sendToPlayer(player, new PacketDamagePopup(m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), res.displayDamage(), res.isCrit(), true));
        }

        // Gain energy & minor kinetic shield
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false));
        float energyGain = targets.isEmpty() ? 22.0f : 35.0f;
        character.addEnergy(energyGain);
    }

    private static void executeUltimateBurst(ServerPlayer player, ServerLevel level, CharacterData character) {
        if (character.getCurrentEnergy() < character.getMaxEnergy()) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("战术核心",
                    "爆发能量未满！(" + (int) character.getCurrentEnergy() + "/" + (int) character.getMaxEnergy() + ")，攻击与战技 [E] 可快速充能。", 70));
            return;
        }

        character.addEnergy(-character.getMaxEnergy());

        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.5, pos.z, 2, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.5, pos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.4, pos.z, 90, 2.5, 1.2, 2.5, 0.25);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.4f, 1.15f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.9f, 1.4f);

        AABB hitBox = player.getBoundingBox().inflate(9.0);
        List<Monster> targets = level.getEntitiesOfClass(Monster.class, hitBox);
        for (Monster m : targets) {
            CharacterData.DamageResult res = character.computeDamage(5.8f, player.getRandom());
            m.hurt(new SkillDamageSource(player), res.mcDamage());
            PacketHandler.sendToPlayer(player, new PacketDamagePopup(m.getX(), m.getY() + m.getBbHeight() + 0.4, m.getZ(), res.displayDamage(), res.isCrit(), true));
        }

        player.heal(10.0f);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 1, false, false));

        PacketHandler.sendToPlayer(player, new PacketRadioDialogue("零号特工·破晓",
                "【终极爆发·星火裁决】聚变超载释放！全范围电浆脉冲贯穿目标！", 110));
    }
}
