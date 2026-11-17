package com.resilienthope.core.network;

import com.resilienthope.core.subtitle.RadioSubtitleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRadioDialogue {
    private final String speaker;
    private final String text;
    private final int duration;

    public PacketRadioDialogue(String speaker, String text, int duration) {
        this.speaker = speaker;
        this.text = text;
        this.duration = duration;
    }

    public static void encode(PacketRadioDialogue msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.speaker);
        buf.writeUtf(msg.text);
        buf.writeInt(msg.duration);
    }

    public static PacketRadioDialogue decode(FriendlyByteBuf buf) {
        return new PacketRadioDialogue(buf.readUtf(), buf.readUtf(), buf.readInt());
    }

    public static void handle(PacketRadioDialogue msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RadioSubtitleManager.postRadioMessage(msg.speaker, msg.text, msg.duration);
        });
        ctx.get().setPacketHandled(true);
    }
}
