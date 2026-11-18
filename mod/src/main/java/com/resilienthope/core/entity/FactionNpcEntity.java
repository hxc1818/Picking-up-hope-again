package com.resilienthope.core.entity;

import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketOpenNpcDialogue;
import com.resilienthope.core.quest.QuestManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class FactionNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC_ID =
            SynchedEntityData.defineId(FactionNpcEntity.class, EntityDataSerializers.STRING);

    public FactionNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NPC_ID, "zheng");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12.0F));
    }

    public String getNpcId() {
        return this.entityData.get(NPC_ID);
    }

    public void setNpcId(String id) {
        this.entityData.set(NPC_ID, id != null ? id : "zheng");
        updateDisplayName();
    }

    private void updateDisplayName() {
        String id = getNpcId();
        String label = switch (id) {
            case "chen" -> "§a[巡检员] §f陈风";
            case "su" -> "§b[医疗员] §f苏晴";
            case "han" -> "§c[狙击手] §f韩冰";
            default -> "§6[技术员] §f老郑";
        };
        this.setCustomName(new TextComponent(label));
        this.setCustomNameVisible(true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level.isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                    new PacketOpenNpcDialogue(getNpcId(), QuestManager.getQuestStage()));
        }
        return InteractionResult.sidedSuccess(this.level.isClientSide());
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcId", getNpcId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcId")) {
            setNpcId(tag.getString("NpcId"));
        }
    }
}
