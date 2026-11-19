package com.resilienthope.core.account;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.network.PacketSyncTeamData;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.quest.StoryDirector;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Lightweight RPG save payload serialized to JSON for cloud synchronization and local profile backup.
 * Common-safe: never imports net.minecraft.client.* classes.
 */
public class CloudSaveSnapshot {
    public int schemaVersion = 1;
    public long updatedAt = 0L;
    public int revision = 0;
    public String reason = "auto";
    public Summary summary = new Summary();
    public Payload payload = new Payload();

    public static class Summary {
        public int playerLevel = 1;
        public int worldLevel = 0;
        public int questStage = 0;
        public String questTitle = "【休眠区断电】";
        public List<String> activePartyNames = new ArrayList<>();
        public int starTicketsCount = 0;
    }

    public static class Payload {
        public String teamNbtBase64 = "";
        public String inventoryNbtBase64 = "";
        public String questNbtBase64 = "";
        public List<String> playerTags = new ArrayList<>();
        public double posX = 6.5;
        public double posY = 61.0;
        public double posZ = 10.5;
        public float yaw = 270.0f;
        public float pitch = 0.0f;
        public float health = 20.0f;
    }

    public static CloudSaveSnapshot captureFromPlayer(ServerPlayer player, String reason) {
        CloudSaveSnapshot snap = new CloudSaveSnapshot();
        snap.schemaVersion = 1;
        snap.updatedAt = System.currentTimeMillis();
        snap.reason = reason != null ? reason : "auto";

        // 1. Capture TeamCapability
        player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
            snap.summary.playerLevel = team.getPlayerLevel();
            snap.summary.worldLevel = team.getWorldLevel();
            for (CharacterData member : team.getActivePartyMembers()) {
                snap.summary.activePartyNames.add(member.getName());
            }
            snap.payload.teamNbtBase64 = encodeTag(team.serializeNBT());
        });

        // 2. Capture full Inventory & count star tickets
        int tickets = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == ModItems.STAR_TICKET.get()) {
                tickets += stack.getCount();
            }
        }
        snap.summary.starTicketsCount = tickets;

        CompoundTag invWrap = new CompoundTag();
        invWrap.put("Items", player.getInventory().save(new ListTag()));
        snap.payload.inventoryNbtBase64 = encodeTag(invWrap);

        // 3. Capture Quest state directly from QuestManager (also ensures PickingHopeQuest NBT stays fresh)
        CompoundTag qTag = new CompoundTag();
        qTag.putInt("Stage", QuestManager.getQuestStage());
        qTag.putString("Title", QuestManager.getQuestTitle());
        qTag.putString("Objective", QuestManager.getObjectiveText());
        BlockPos target = QuestManager.getTargetPos();
        qTag.putInt("TX", target.getX());
        qTag.putInt("TY", target.getY());
        qTag.putInt("TZ", target.getZ());
        qTag.putInt("Kills", QuestManager.getDronesKilled());
        qTag.putInt("Req", QuestManager.getRequiredDrones());
        qTag.putInt("Flags", QuestManager.getSubFlags());
        qTag.putString("Ending", QuestManager.getEndingFaction());
        player.getPersistentData().put("PickingHopeQuest", qTag);

        snap.summary.questStage = QuestManager.getQuestStage();
        snap.summary.questTitle = QuestManager.getQuestTitle();
        snap.payload.questNbtBase64 = encodeTag(qTag);

        // 4. Capture progression & crate tags
        for (String tag : player.getTags()) {
            if (tag.startsWith("picking_hope_") || tag.startsWith("hope_crate_")) {
                snap.payload.playerTags.add(tag);
            }
        }

        // 5. Capture safe overworld coordinates (never save inside the Y=150 battle arena)
        if (player.getY() >= 140.0 || player.getY() < 15.0) {
            double[] safe = getSafeFallbackPos(snap.summary.questStage);
            snap.payload.posX = safe[0];
            snap.payload.posY = safe[1];
            snap.payload.posZ = safe[2];
            snap.payload.yaw = (float) safe[3];
            snap.payload.pitch = 0.0f;
        } else {
            snap.payload.posX = player.getX();
            snap.payload.posY = player.getY();
            snap.payload.posZ = player.getZ();
            snap.payload.yaw = player.getYRot();
            snap.payload.pitch = player.getXRot();
        }
        snap.payload.health = player.getHealth();
        return snap;
    }

    public static void applyToPlayer(ServerPlayer player, CloudSaveSnapshot snap) {
        if (player == null || snap == null || snap.payload == null) return;

        // 1. Restore Inventory
        if (snap.payload.inventoryNbtBase64 != null && !snap.payload.inventoryNbtBase64.isEmpty()) {
            CompoundTag invWrap = decodeTag(snap.payload.inventoryNbtBase64);
            if (invWrap != null && invWrap.contains("Items", Tag.TAG_LIST)) {
                player.getInventory().clearContent();
                player.getInventory().load(invWrap.getList("Items", Tag.TAG_COMPOUND));
            }
        }

        // 2. Restore TeamCapability
        if (snap.payload.teamNbtBase64 != null && !snap.payload.teamNbtBase64.isEmpty()) {
            CompoundTag teamTag = decodeTag(snap.payload.teamNbtBase64);
            if (teamTag != null) {
                player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                    team.deserializeNBT(teamTag);
                    team.syncHealthToPlayer(player);
                });
            }
        }

        // 3. Restore Quest state
        if (snap.payload.questNbtBase64 != null && !snap.payload.questNbtBase64.isEmpty()) {
            CompoundTag qTag = decodeTag(snap.payload.questNbtBase64);
            if (qTag != null) {
                player.getPersistentData().put("PickingHopeQuest", qTag);
                StoryDirector.loadQuest(player);
            }
        }

        // 4. Restore tags and mark clean v2 initialization flags
        List<String> stale = player.getTags().stream()
                .filter(t -> t.startsWith("picking_hope_") || t.startsWith("hope_crate_"))
                .toList();
        for (String s : stale) {
            player.removeTag(s);
        }
        if (snap.payload.playerTags != null) {
            for (String tag : snap.payload.playerTags) {
                if (tag != null && !tag.isEmpty()) {
                    player.addTag(tag);
                }
            }
        }
        player.addTag("picking_hope_v2_clean_init_v4");
        player.addTag("picking_hope_v2_custom_world_v4");

        // 5. Restore safe overworld position & health
        double tx = snap.payload.posX;
        double ty = snap.payload.posY;
        double tz = snap.payload.posZ;
        float yaw = snap.payload.yaw;
        float pitch = snap.payload.pitch;
        if (ty < 15.0 || ty >= 140.0) {
            double[] safe = getSafeFallbackPos(QuestManager.getQuestStage());
            tx = safe[0];
            ty = safe[1];
            tz = safe[2];
            yaw = (float) safe[3];
            pitch = 0.0f;
        }
        player.teleportTo(player.getLevel(), tx, ty, tz, yaw, pitch);
        if (snap.payload.health > 0.5f) {
            player.setHealth(Math.min(player.getMaxHealth(), snap.payload.health));
        }

        player.getPersistentData().putLong("PickingHopeCloudSyncAt", snap.updatedAt);
        PacketSyncTeamData.syncToPlayer(player);
    }

    private static double[] getSafeFallbackPos(int stage) {
        if (stage >= 15) {
            return new double[]{114.5, 61.0, 28.5, 270.0};
        }
        if (stage >= 4) {
            return new double[]{34.5, 61.0, 12.5, 270.0};
        }
        return new double[]{6.5, 61.0, 10.5, 270.0};
    }

    private static String encodeTag(CompoundTag tag) {
        if (tag == null) return "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            NbtIo.writeCompressed(tag, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            PickingHopeCore.LOGGER.error("Failed to compress NBT for cloud save: {}", e.getMessage());
            return "";
        }
    }

    private static CompoundTag decodeTag(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(b64);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                return NbtIo.readCompressed(bais);
            }
        } catch (Exception e) {
            PickingHopeCore.LOGGER.error("Failed to decode NBT from cloud save: {}", e.getMessage());
            return null;
        }
    }
}
