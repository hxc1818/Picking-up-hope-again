package com.resilienthope.core.network;

import com.resilienthope.core.client.DamagePopupManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketDamagePopup {
    private final double x, y, z;
    private final int amount;
    private final boolean isCrit;
    private final boolean isSkill;

    public PacketDamagePopup(double x, double y, double z, int amount, boolean isCrit, boolean isSkill) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.amount = amount;
        this.isCrit = isCrit;
        this.isSkill = isSkill;
    }

    public static void encode(PacketDamagePopup msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeInt(msg.amount);
        buf.writeBoolean(msg.isCrit);
        buf.writeBoolean(msg.isSkill);
    }

    public static PacketDamagePopup decode(FriendlyByteBuf buf) {
        return new PacketDamagePopup(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(PacketDamagePopup msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    DamagePopupManager.addPopup(msg.x, msg.y, msg.z, msg.amount, msg.isCrit, msg.isSkill));
        });
        ctx.get().setPacketHandled(true);
    }
}
