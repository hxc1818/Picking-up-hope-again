package com.resilienthope.core.entity;

import com.resilienthope.core.combat.BattleArenaLayout;
import com.resilienthope.core.combat.TurnBasedCombatManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Real-world 3D actor entity representing an ally character or enemy mech on the hangar battle deck.
 * Strictly common-safe: no client-only dependencies.
 */
public class BattleActorEntity extends Entity {
    private static final EntityDataAccessor<Integer> SIDE =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT_INDEX =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> MODEL_KEY =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ELEMENT =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> ALIVE =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BROKEN =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHIELDED =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ACTOR_SCALE =
            SynchedEntityData.defineId(BattleActorEntity.class, EntityDataSerializers.FLOAT);

    /** Client-only transient glow RGB (0 = off). Mutated only by client battle controller. */
    public int clientGlowColor = 0;
    /** Client-only animated root displacement from home pos during keyframe playback. */
    public double clientOffsetX = 0.0D;
    public double clientOffsetY = 0.0D;
    public double clientOffsetZ = 0.0D;
    public float clientYawOffset = 0.0F;

    public BattleActorEntity(EntityType<? extends BattleActorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SIDE, 0);
        this.entityData.define(SLOT_INDEX, 0);
        this.entityData.define(MODEL_KEY, "linye");
        this.entityData.define(ELEMENT, "EARTH");
        this.entityData.define(ALIVE, true);
        this.entityData.define(BROKEN, false);
        this.entityData.define(CHARGING, false);
        this.entityData.define(SHIELDED, false);
        this.entityData.define(ACTOR_SCALE, 1.0F);
    }

    public int getSide() { return this.entityData.get(SIDE); }
    public int getSlotIndex() { return this.entityData.get(SLOT_INDEX); }
    public String getModelKey() { return this.entityData.get(MODEL_KEY); }
    public String getElementKey() { return this.entityData.get(ELEMENT); }
    public boolean isAliveActor() { return this.entityData.get(ALIVE); }
    public boolean isBrokenState() { return this.entityData.get(BROKEN); }
    public boolean isChargingState() { return this.entityData.get(CHARGING); }
    public boolean isShieldedState() { return this.entityData.get(SHIELDED); }
    public float getActorScale() { return this.entityData.get(ACTOR_SCALE); }

    public void configure(int side, int slot, String modelKey, String element, float scale) {
        this.entityData.set(SIDE, side);
        this.entityData.set(SLOT_INDEX, slot);
        this.entityData.set(MODEL_KEY, modelKey != null ? modelKey : "drone");
        this.entityData.set(ELEMENT, element != null ? element : "EARTH");
        this.entityData.set(ACTOR_SCALE, scale > 0.1F ? scale : 1.0F);
        float yaw = side == 0 ? BattleArenaLayout.ALLY_YAW : BattleArenaLayout.ENEMY_YAW;
        this.setYRot(yaw);
        this.yRotO = yaw;
    }

    public void syncCombatFlags(boolean alive, boolean broken, boolean charging, boolean shielded) {
        this.entityData.set(ALIVE, alive);
        this.entityData.set(BROKEN, broken);
        this.entityData.set(CHARGING, charging);
        this.entityData.set(SHIELDED, shielded);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level.isClientSide() && this.tickCount > 40 && !TurnBasedCombatManager.hasAnyActiveSession()) {
            this.discard();
        }
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (this.level.isClientSide() && this.clientGlowColor != 0 && this.isAliveActor()) {
            return true;
        }
        return super.isCurrentlyGlowing();
    }

    @Override
    public int getTeamColor() {
        if (this.level.isClientSide() && this.clientGlowColor != 0) {
            return this.clientGlowColor & 0x00FFFFFF;
        }
        return super.getTeamColor();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // Generous culling box so actors performing long dash keyframes never disappear at frustum edges
        return this.getBoundingBox().inflate(14.0D);
    }

    @Override public boolean isPickable() { return true; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 16384.0D; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Side")) this.entityData.set(SIDE, tag.getInt("Side"));
        if (tag.contains("Slot")) this.entityData.set(SLOT_INDEX, tag.getInt("Slot"));
        if (tag.contains("ModelKey")) this.entityData.set(MODEL_KEY, tag.getString("ModelKey"));
        if (tag.contains("Element")) this.entityData.set(ELEMENT, tag.getString("Element"));
        if (tag.contains("Alive")) this.entityData.set(ALIVE, tag.getBoolean("Alive"));
        if (tag.contains("Broken")) this.entityData.set(BROKEN, tag.getBoolean("Broken"));
        if (tag.contains("Charging")) this.entityData.set(CHARGING, tag.getBoolean("Charging"));
        if (tag.contains("Shielded")) this.entityData.set(SHIELDED, tag.getBoolean("Shielded"));
        if (tag.contains("Scale")) this.entityData.set(ACTOR_SCALE, tag.getFloat("Scale"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Side", getSide());
        tag.putInt("Slot", getSlotIndex());
        tag.putString("ModelKey", getModelKey());
        tag.putString("Element", getElementKey());
        tag.putBoolean("Alive", isAliveActor());
        tag.putBoolean("Broken", isBrokenState());
        tag.putBoolean("Charging", isChargingState());
        tag.putBoolean("Shielded", isShieldedState());
        tag.putFloat("Scale", getActorScale());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
