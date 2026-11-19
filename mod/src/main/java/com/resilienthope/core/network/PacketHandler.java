package com.resilienthope.core.network;

import com.resilienthope.core.PickingHopeCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "4";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PickingHopeCore.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        if (player != null) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

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

        INSTANCE.messageBuilder(PacketSyncTeamData.class, packetId++)
                .encoder(PacketSyncTeamData::encode)
                .decoder(PacketSyncTeamData::decode)
                .consumer(PacketSyncTeamData::handle)
                .add();

        INSTANCE.messageBuilder(PacketDamagePopup.class, packetId++)
                .encoder(PacketDamagePopup::encode)
                .decoder(PacketDamagePopup::decode)
                .consumer(PacketDamagePopup::handle)
                .add();

        INSTANCE.messageBuilder(PacketOpenNpcDialogue.class, packetId++)
                .encoder(PacketOpenNpcDialogue::encode)
                .decoder(PacketOpenNpcDialogue::decode)
                .consumer(PacketOpenNpcDialogue::handle)
                .add();

        INSTANCE.messageBuilder(PacketNpcAction.class, packetId++)
                .encoder(PacketNpcAction::encode)
                .decoder(PacketNpcAction::decode)
                .consumer(PacketNpcAction::handle)
                .add();

        INSTANCE.messageBuilder(PacketWishPull.class, packetId++)
                .encoder(PacketWishPull::encode)
                .decoder(PacketWishPull::decode)
                .consumer(PacketWishPull::handle)
                .add();

        INSTANCE.messageBuilder(PacketWishResult.class, packetId++)
                .encoder(PacketWishResult::encode)
                .decoder(PacketWishResult::decode)
                .consumer(PacketWishResult::handle)
                .add();

        INSTANCE.messageBuilder(PacketBattleSync.class, packetId++)
                .encoder(PacketBattleSync::encode)
                .decoder(PacketBattleSync::decode)
                .consumer(PacketBattleSync::handle)
                .add();

        INSTANCE.messageBuilder(PacketBattleAction.class, packetId++)
                .encoder(PacketBattleAction::encode)
                .decoder(PacketBattleAction::decode)
                .consumer(PacketBattleAction::handle)
                .add();

        INSTANCE.messageBuilder(PacketProgressionAction.class, packetId++)
                .encoder(PacketProgressionAction::encode)
                .decoder(PacketProgressionAction::decode)
                .consumer(PacketProgressionAction::handle)
                .add();

        INSTANCE.messageBuilder(PacketCloudSyncRequest.class, packetId++)
                .encoder(PacketCloudSyncRequest::encode)
                .decoder(PacketCloudSyncRequest::decode)
                .consumer(PacketCloudSyncRequest::handle)
                .add();

        INSTANCE.messageBuilder(PacketCloudSyncAck.class, packetId++)
                .encoder(PacketCloudSyncAck::encode)
                .decoder(PacketCloudSyncAck::decode)
                .consumer(PacketCloudSyncAck::handle)
                .add();

        INSTANCE.messageBuilder(PacketBuilderTeleport.class, packetId++)
                .encoder(PacketBuilderTeleport::encode)
                .decoder(PacketBuilderTeleport::decode)
                .consumer(PacketBuilderTeleport::handle)
                .add();
    }
}
