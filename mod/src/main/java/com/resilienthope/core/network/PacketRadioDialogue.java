package com.resilienthope.core.network;

import com.resilienthope.core.subtitle.RadioSubtitleManager;
import com.resilienthope.core.subtitle.SystemToastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRadioDialogue {
    private final String speaker;
    private final String text;
    private final int duration;
    private final boolean systemToast;

    public PacketRadioDialogue(String speaker, String text, int duration) {
        this(speaker, text, duration, false);
    }

    public PacketRadioDialogue(String speaker, String text, int duration, boolean systemToast) {
        this.speaker = speaker != null ? speaker : "";
        this.text = text != null ? text : "";
        this.duration = duration;
        this.systemToast = systemToast;
    }

    public static PacketRadioDialogue toast(String title, String text) {
        return new PacketRadioDialogue(title, text, 45, true);
    }

    public static void encode(PacketRadioDialogue msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.speaker);
        buf.writeUtf(msg.text);
        buf.writeInt(msg.duration);
        buf.writeBoolean(msg.systemToast);
    }

    public static PacketRadioDialogue decode(FriendlyByteBuf buf) {
        return new PacketRadioDialogue(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(PacketRadioDialogue msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (msg.systemToast) {
                SystemToastManager.show(msg.speaker, msg.text);
            } else {
                RadioSubtitleManager.postRadioMessage(msg.speaker, msg.text, msg.duration);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
