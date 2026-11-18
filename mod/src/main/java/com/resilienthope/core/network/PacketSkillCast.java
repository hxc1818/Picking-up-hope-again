package com.resilienthope.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Legacy stub retained only to preserve packet registration ID alignment if needed.
 * Turn-based skills execute inside TurnBasedCombatManager via PacketBattleAction.
 */
public class PacketSkillCast {
    private final int skillType;

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
        ctx.get().setPacketHandled(true);
    }
}
