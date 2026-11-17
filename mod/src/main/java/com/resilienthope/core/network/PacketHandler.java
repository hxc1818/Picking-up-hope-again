package com.resilienthope.core.network;

import com.resilienthope.core.PickingHopeCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PickingHopeCore.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.messageBuilder(PacketSwitchCharacter.class, packetId++)
                .encoder(PacketSwitchCharacter::encode)
                .decoder(PacketSwitchCharacter::decode)
                .consumer(PacketSwitchCharacter::handle)
                .add();

        INSTANCE.messageBuilder(PacketRadioDialogue.class, packetId++)
                .encoder(PacketRadioDialogue::encode)
                .decoder(PacketRadioDialogue::decode)
                .consumer(PacketRadioDialogue::handle)
                .add();

        INSTANCE.messageBuilder(PacketSyncQuest.class, packetId++)
                .encoder(PacketSyncQuest::encode)
                .decoder(PacketSyncQuest::decode)
                .consumer(PacketSyncQuest::handle)
                .add();

        INSTANCE.messageBuilder(PacketSkillCast.class, packetId++)
                .encoder(PacketSkillCast::encode)
                .decoder(PacketSkillCast::decode)
                .consumer(PacketSkillCast::handle)
                .add();

        INSTANCE.messageBuilder(PacketEquipItem.class, packetId++)
                .encoder(PacketEquipItem::encode)
                .decoder(PacketEquipItem::decode)
                .consumer(PacketEquipItem::handle)
                .add();

        INSTANCE.messageBuilder(PacketLootChest.class, packetId++)
                .encoder(PacketLootChest::encode)
                .decoder(PacketLootChest::decode)
                .consumer(PacketLootChest::handle)
                .add();
    }
}
