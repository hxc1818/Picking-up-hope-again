package com.resilienthope.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * Dedicated invisible camera anchor entity used exclusively client-side during turn-based combat
 * via Minecraft.setCameraEntity(...) so elevated overviews and cinematic Ult tracks bypass ShoulderSurfing.
 */
public class BattleCameraEntity extends Entity {

    public BattleCameraEntity(EntityType<? extends BattleCameraEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        // Pose is driven explicitly each client tick by BattleCameraController
    }

    public void snapPose(double x, double y, double z, float yaw, float pitch) {
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setPosRaw(x, y, z);
        this.yRotO = yaw;
        this.setYRot(yaw);
        this.xRotO = pitch;
        this.setXRot(pitch);
    }

    public void advancePose(double x, double y, double z, float targetYaw, float targetPitch) {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        // Keep yaw unwrapped continuously so partialTicks Mth.lerp never wraps +/-358 degrees
        float continuousYaw = this.getYRot() + Mth.wrapDegrees(targetYaw - this.getYRot());
        this.setPosRaw(x, y, z);
        this.setYRot(continuousYaw);
        this.setXRot(Mth.clamp(targetPitch, -85.0F, 85.0F));
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions size) {
        return 0.0F;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
