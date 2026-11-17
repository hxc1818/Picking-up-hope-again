package com.resilienthope.core.event;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketDamagePopup;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.network.PacketSyncTeamData;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.quest.StoryDirector;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class StarshipSpawnHandler {
    private static final String SPAWN_TAG = "picking_hope_spawned";
    private static final String V2_WORLD_TAG = "picking_hope_v2_world";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        if (!player.isCreative() && !player.isSpectator()) {
            player.setGameMode(GameType.ADVENTURE);
        }

        ServerLevel level = player.getLevel();
        BlockPos origin = new BlockPos(0, 60, 0);

        if (!player.getTags().contains(SPAWN_TAG)) {
            player.getInventory().clearContent();
            ItemStack starterSword = new ItemStack(ModItems.PLASMA_CUTTER.get());
            EquipmentStatsHelper.ensureStats(starterSword, player.getRandom());
            player.getInventory().setItem(0, starterSword);
            player.setItemInHand(InteractionHand.MAIN_HAND, starterSword);

            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                team.getCharacter(0).setEquippedWeapon(starterSword);
            });

            StructureManager templateManager = level.getStructureManager();
            ResourceLocation structureId = new ResourceLocation(PickingHopeCore.MOD_ID, "starship_prologue");
            Optional<StructureTemplate> template = templateManager.get(structureId);
            if (template.isPresent()) {
                template.get().placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 2);
                PickingHopeCore.LOGGER.info("Picking Hope: Placed starship_prologue template.");
            } else {
                buildStarshipDirectly(level, origin);
            }

            player.teleportTo(level, 6.5, 62.0, 10.5, 90.0f, 0.0f);
            player.setRespawnPosition(level.dimension(), new BlockPos(6, 62, 10), 90.0f, true, false);
            player.addTag(SPAWN_TAG);
            QuestManager.resetAll();
        } else {
            // Ensure player always holds their equipped weapon cleanly without wiping inventory
            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                CharacterData active = team.getCharacter(team.getActiveSlotIndex());
                if (active.getEquippedWeapon().isEmpty()) {
                    ItemStack sword = new ItemStack(ModItems.PLASMA_CUTTER.get());
                    EquipmentStatsHelper.ensureStats(sword, player.getRandom());
                    active.setEquippedWeapon(sword);
                }
                if (player.getInventory().getItem(0).isEmpty()) {
                    player.getInventory().setItem(0, active.getEquippedWeapon().copy());
                    player.setItemInHand(InteractionHand.MAIN_HAND, active.getEquippedWeapon().copy());
                }
            });
        }

        // Always verify v2 expanded world areas (Upper Props, Lower Deck, Permafrost Mine, NPCs)
        if (!player.getTags().contains(V2_WORLD_TAG) || level.getBlockState(new BlockPos(132, 61, 28)).isAir()) {
            ensureUpperShipProps(level, origin, player);
            buildLowerDeck(level);
            buildPermafrostMine(level);
            player.addTag(V2_WORLD_TAG);
        }

        // Restore persistent quest state and sync team & quest to client
        StoryDirector.loadQuest(player);
        if (QuestManager.getQuestStage() >= 3) {
            StoryDirector.openElevatorShaft(level);
        }
        if (QuestManager.getQuestStage() >= 7) {
            StoryDirector.ensureNpc(level, "han", 30.5, 61.0, 12.5, -60.0f);
            StoryDirector.ensureNpc(level, "su", 30.5, 61.0, 8.5, -120.0f);
        }
        PacketSyncTeamData.syncToPlayer(player);

        if (QuestManager.getQuestStage() == 0) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("重拾希望号导航系统",
                    "检测到零号特工【破晓】生命体征复苏。请先开启面前物资箱，随后沿走廊前往聚变大厅！", 200));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide() && event.player instanceof ServerPlayer player) {
            if (!player.isCreative() && !player.isSpectator() && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                player.setGameMode(GameType.ADVENTURE);
            }

            // Stage 0 fallback: entering corridor triggers drone ambush automatically
            if (QuestManager.getQuestStage() == 0 && player.getX() >= 11.5 && player.getY() >= 55.0) {
                StoryDirector.triggerStage0To1(player, false);
            }

            // Void rescue
            if (player.getY() < 10.0) {
                if (player.getX() > 85.0) {
                    player.teleportTo(player.getLevel(), 108.5, 62.0, 28.5, 270.0f, 0.0f);
                } else {
                    player.teleportTo(player.getLevel(), 32.5, 62.0, 10.5, 90.0f, 0.0f);
                }
            }

            // Periodic health sync
            if (player.tickCount % 20 == 0) {
                player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                    team.syncHealthFromPlayer(player);
                    PacketSyncTeamData.syncToPlayer(player);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            double x = player.getX();
            double z = player.getZ();
            // Cancel fall damage inside Starship Elevator Shaft or Permafrost Landing Zone
            if ((x >= 31.0 && x <= 37.5 && z >= 12.0 && z <= 16.5) || (x >= 104.0 && x <= 116.0)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level.isClientSide()) return;
        // Scale normal melee / Better Combat attacks using Protagonist's real stats
        if (!event.getSource().isBypassArmor()
                && event.getSource().getDirectEntity() instanceof ServerPlayer player
                && event.getEntity() instanceof LivingEntity target
                && !(target instanceof Player)) {

            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                CharacterData active = team.getCharacter(team.getActiveSlotIndex());
                CharacterData.DamageResult res = active.computeDamage(1.0f, player.getRandom());
                event.setAmount(res.mcDamage());

                float energyGain = 7.0f;
                if (active.getEquippedWeapon().getItem() == ModItems.CRYO_SAWBLADE.get()) {
                    energyGain = 10.0f;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
                active.addEnergy(energyGain);

                PacketHandler.sendToPlayer(player, new PacketDamagePopup(
                        target.getX(), target.getY() + target.getBbHeight() + 0.25, target.getZ(),
                        res.displayDamage(), res.isCrit(), false));
                PacketSyncTeamData.syncToPlayer(player);
            });
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!event.getEntity().level.isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                team.syncHealthFromPlayer(player);
                PacketSyncTeamData.syncToPlayer(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getPlayer() instanceof ServerPlayer newPlayer && event.getOriginal() instanceof ServerPlayer oldPlayer) {
            oldPlayer.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(oldTeam -> {
                newPlayer.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(newTeam -> {
                    newTeam.deserializeNBT(oldTeam.serializeNBT());
                });
            });
            if (oldPlayer.getPersistentData().contains("PickingHopeQuest")) {
                newPlayer.getPersistentData().put("PickingHopeQuest",
                        oldPlayer.getPersistentData().getCompound("PickingHopeQuest").copy());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            StoryDirector.loadQuest(player);
            PacketSyncTeamData.syncToPlayer(player);
        }
    }

    private static void ensureUpperShipProps(ServerLevel level, BlockPos origin, ServerPlayer player) {
        // Starter Pedestal & Chest
        level.setBlock(origin.offset(6, 1, 10), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        BlockPos chestPos = origin.offset(6, 2, 10);
        if (!level.getBlockState(chestPos).is(Blocks.CHEST)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        }
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof Container container) {
            boolean empty = true;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (!container.getItem(i).isEmpty()) {
                    empty = false;
                    break;
                }
            }
            if (empty && QuestManager.getQuestStage() == 0) {
                ItemStack chip1 = new ItemStack(ModItems.FUSION_CORE_CHIP.get());
                EquipmentStatsHelper.ensureStats(chip1, player.getRandom());
                container.setItem(0, chip1);
                container.setItem(1, new ItemStack(ModItems.ALLOY_PLATE.get(), 6));
                container.setItem(2, new ItemStack(ModItems.QUANTUM_CORE.get(), 16));
            }
        }

        // Fusion Hub props
        level.setBlock(origin.offset(32, 1, 10), ModBlocks.TELEPORT_WAYPOINT.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(36, 1, 14), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);

        // Seal elevator hatch if before Stage 3
        if (QuestManager.getQuestStage() < 3) {
            level.setBlock(new BlockPos(34, 60, 14), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(35, 60, 14), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
        }

        // Ensure Chief Engineer Zheng Tieshan
        StoryDirector.ensureNpc(level, "zheng", 35.5, 61.0, 12.5, 135.0f);
    }

    private static void buildStarshipDirectly(ServerLevel level, BlockPos origin) {
        int sizeX = 40;
        int sizeZ = 20;

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                boolean isHazard = (x == 12 || x == 13 || x == 24 || x == 25) && (z >= 7 && z <= 12);
                level.setBlock(origin.offset(x, 0, z), isHazard ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState(), 2);

                boolean isLight = (x % 6 == 3) && (z % 6 == 3);
                level.setBlock(origin.offset(x, 6, z), isLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState(), 2);

                for (int y = 1; y < 6; y++) {
                    if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if ((x >= 13 && x <= 24) && !(z >= 7 && z <= 12)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if (x == 12 && !(z >= 8 && z <= 11)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if (x == 24 && !(z >= 8 && z <= 11)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void buildLowerDeck(ServerLevel level) {
        // Lower Industrial & Research Deck: Y=35..41, X=18..39, Z=4..16
        for (int x = 18; x <= 39; x++) {
            for (int z = 4; z <= 16; z++) {
                boolean isDropZone = (x >= 20 && x <= 24 && z >= 8 && z <= 12);
                level.setBlock(new BlockPos(x, 35, z),
                        isDropZone ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);

                boolean isLight = (x % 4 == 2) && (z % 4 == 2);
                level.setBlock(new BlockPos(x, 41, z),
                        isLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState(), 2);

                for (int y = 36; y <= 40; y++) {
                    if (x == 18 || x == 39 || z == 4 || z == 16) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Auxiliary Energy Core at (32, 36, 10)
        level.setBlock(new BlockPos(32, 35, 10), Blocks.NETHERITE_BLOCK.defaultBlockState(), 2);
        level.setBlock(new BlockPos(32, 36, 10), ModBlocks.AUXILIARY_ENERGY_CORE.get().defaultBlockState(), 2);

        // Orbital Drop Pod at (22, 36, 10)
        level.setBlock(new BlockPos(22, 35, 10), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        level.setBlock(new BlockPos(22, 36, 10), ModBlocks.ORBITAL_DROP_POD.get().defaultBlockState(), 2);

        // Structural pillars
        for (int y = 36; y <= 40; y++) {
            level.setBlock(new BlockPos(20, y, 7), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(20, y, 13), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(36, y, 7), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(36, y, 13), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        }

        // Elevator shaft enclosure Y=36..59
        for (int y = 36; y <= 59; y++) {
            level.setBlock(new BlockPos(32, y, 14), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(36, y, 14), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(new BlockPos(34, y, 13), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(new BlockPos(34, y, 15), Blocks.IRON_BARS.defaultBlockState(), 2);
        }

        StoryDirector.ensureNpc(level, "su", 30.5, 36.0, 8.5, -45.0f);
        StoryDirector.ensureNpc(level, "han", 30.5, 36.0, 11.5, -135.0f);
    }

    private static void buildPermafrostMine(ServerLevel level) {
        // Alien Permafrost Mining Pit Arena: X=104..152, Z=4..52, Y=60..72
        int minX = 104, maxX = 152, minZ = 4, maxZ = 52;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isRim = (x <= minX + 1 || x >= maxX - 1 || z <= minZ + 1 || z >= maxZ - 1);
                boolean isPad = (x >= 106 && x <= 114 && z >= 25 && z <= 31);
                boolean isDais = (x >= 130 && x <= 134 && z >= 26 && z <= 30);

                // Base floor at Y=60
                if (isDais) {
                    level.setBlock(new BlockPos(x, 60, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
                } else if (isPad) {
                    level.setBlock(new BlockPos(x, 60, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
                } else if ((x + z) % 7 == 0) {
                    level.setBlock(new BlockPos(x, 60, z), Blocks.PACKED_ICE.defaultBlockState(), 2);
                } else {
                    level.setBlock(new BlockPos(x, 60, z), Blocks.SNOW_BLOCK.defaultBlockState(), 2);
                }

                // Clear sky above
                for (int y = 61; y <= 74; y++) {
                    if (isRim && y <= 65) {
                        level.setBlock(new BlockPos(x, y, z), (y == 65) ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Arena Industrial Drill Towers & Beacon Lights
        int[][] towers = {{118, 12}, {118, 44}, {144, 12}, {144, 44}};
        for (int[] t : towers) {
            for (int y = 61; y <= 68; y++) {
                level.setBlock(new BlockPos(t[0], y, t[1]), (y == 68) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState(), 2);
            }
        }
        // Tactical Combat Cover Pillars
        int[][] covers = {{124, 22}, {124, 34}, {140, 22}, {140, 34}};
        for (int[] c : covers) {
            for (int y = 61; y <= 63; y++) {
                level.setBlock(new BlockPos(c[0], y, c[1]), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }

        // Alien Surface Return Waypoint
        level.setBlock(new BlockPos(112, 61, 28), ModBlocks.TELEPORT_WAYPOINT.get().defaultBlockState(), 2);

        // Central Precursor Energy Altar
        level.setBlock(new BlockPos(132, 61, 28), ModBlocks.PRECURSOR_ALTAR.get().defaultBlockState(), 2);
    }
}
