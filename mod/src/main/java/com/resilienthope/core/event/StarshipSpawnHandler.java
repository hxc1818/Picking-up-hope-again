package com.resilienthope.core.event;

import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.init.ModBlocks;
import com.resilienthope.core.init.ModItems;
import com.resilienthope.core.item.EquipmentStatsHelper;
import com.resilienthope.core.network.PacketHandler;
import com.resilienthope.core.network.PacketRadioDialogue;
import com.resilienthope.core.network.PacketSyncTeamData;
import com.resilienthope.core.quest.QuestManager;
import com.resilienthope.core.quest.StoryDirector;
import com.resilienthope.core.team.CharacterData;
import com.resilienthope.core.team.TeamCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class StarshipSpawnHandler {
    private static final String SPAWN_TAG = "picking_hope_v2_spawned";
    private static final String V2_WORLD_TAG = "picking_hope_v2_map_built";

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

            buildExpandedStarship(level, origin);
            player.teleportTo(level, 6.5, 62.0, 10.5, 270.0f, 0.0f);
            player.setRespawnPosition(level.dimension(), new BlockPos(6, 62, 10), 270.0f, true, false);
            player.addTag(SPAWN_TAG);
            QuestManager.resetAll();
        } else {
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

        if (!player.getTags().contains(V2_WORLD_TAG) || level.getBlockState(new BlockPos(168, 36, 28)).isAir()) {
            buildExpandedStarship(level, origin);
            ensureUpperShipProps(level, origin, player);
            buildLowerDeck(level);
            buildIceOutpostAndMine(level, player);
            player.addTag(V2_WORLD_TAG);
        }

        StoryDirector.loadQuest(player);
        int stage = QuestManager.getQuestStage();
        if (stage >= 2) StoryDirector.ensureNpc(level, "chen", 31.5, 61.0, 14.5, -145.0f);
        if (stage >= 4) StoryDirector.openElevatorShaft(level);
        if (stage >= 5) StoryDirector.ensureNpc(level, "su", 30.5, 36.0, 7.5, -45.0f);
        if (stage >= 10) StoryDirector.ensureNpc(level, "zheng", 131.5, 61.0, 31.5, 135.0f);
        if (stage >= 12) StoryDirector.openMineElevatorShaft(level);

        PacketSyncTeamData.syncToPlayer(player);

        if (stage == 0) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("船舱应急广播",
                    "警告：重拾希望号遭遇不明撞击，B区供电中断。林野，先打开身旁的物资箱检查装备！", 200));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            TurnBasedCombatManager.clearSessionOnLogout(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide() && event.player instanceof ServerPlayer player) {
            if (!player.isCreative() && !player.isSpectator() && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                player.setGameMode(GameType.ADVENTURE);
            }

            if (QuestManager.getQuestStage() == 0 && player.getX() >= 12.0 && player.getY() >= 55.0) {
                StoryDirector.triggerStage0To1(player, false);
            }

            // Void rescue
            if (player.getY() < 10.0) {
                if (player.getX() > 85.0) {
                    player.teleportTo(player.getLevel(), 110.5, 61.0, 28.5, 270.0f, 0.0f);
                } else {
                    player.teleportTo(player.getLevel(), 34.5, 61.0, 10.5, 90.0f, 0.0f);
                }
            }

            // Regenerate 1 Stamina every 6 seconds and sync team data periodically
            if (player.tickCount % 120 == 0) {
                player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                    team.setStamina(team.getStamina() + 1);
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
            if ((x >= 32.0 && x <= 38.0 && z >= 12.0 && z <= 18.0)
                    || (x >= 143.0 && x <= 149.0 && z >= 25.0 && z <= 31.0)
                    || (x >= 104.0 && x <= 116.0)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level.isClientSide()) return;

        // Player left-clicks Monster -> Ambush Turn-Based Battle!
        if (event.getSource().getDirectEntity() instanceof ServerPlayer player
                && event.getEntity() instanceof Monster target) {
            event.setCanceled(true);
            if (!TurnBasedCombatManager.isInBattle(player)) {
                TurnBasedCombatManager.startOverworldBattle(player, target, true);
            }
            return;
        }

        // Monster hits Player -> Surprised Turn-Based Battle!
        if (event.getSource().getEntity() instanceof Monster attacker
                && event.getEntity() instanceof ServerPlayer player) {
            event.setCanceled(true);
            if (!TurnBasedCombatManager.isInBattle(player)) {
                TurnBasedCombatManager.startOverworldBattle(player, attacker, false);
            }
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
        level.setBlock(origin.offset(6, 1, 10), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        BlockPos chestPos = origin.offset(6, 2, 10);
        if (!level.getBlockState(chestPos).is(Blocks.CHEST)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        }
        if (level.getBlockEntity(chestPos) instanceof Container container && isContainerEmpty(container)) {
            ItemStack chip1 = EquipmentStatsHelper.createChipWithRarity(ModItems.FUSION_CORE_CHIP.get(), 2, player.getRandom());
            container.setItem(0, chip1);
            container.setItem(1, new ItemStack(ModItems.EXP_BOTTLE.get(), 4));
            container.setItem(2, new ItemStack(ModItems.CREDIT_COIN.get(), 20));
        }

        // Waypoint & Terminal in Relay Hall
        level.setBlock(origin.offset(34, 1, 10), ModBlocks.TELEPORT_WAYPOINT.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(38, 1, 14), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);

        if (QuestManager.getQuestStage() < 4) {
            level.setBlock(new BlockPos(35, 60, 15), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(36, 60, 15), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
        }
    }

    private static boolean isContainerEmpty(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!c.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private static void buildExpandedStarship(ServerLevel level, BlockPos origin) {
        int sizeX = 44;
        int sizeZ = 20;

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                boolean isHazard = (x == 12 || x == 13 || x == 24 || x == 25) && (z >= 7 && z <= 12);
                level.setBlock(origin.offset(x, 0, z), isHazard ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState(), 2);

                boolean isLight = (x % 6 == 3) && (z % 6 == 3);
                boolean isRedAlarm = (x >= 13 && x <= 23) && (x % 4 == 1) && (z == 8 || z == 11);
                level.setBlock(origin.offset(x, 6, z),
                        isRedAlarm ? Blocks.REDSTONE_LAMP.defaultBlockState() :
                                (isLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState()), 2);

                for (int y = 1; y < 6; y++) {
                    if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if ((x >= 13 && x <= 24) && !(z >= 7 && z <= 12)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else if ((x == 12 || x == 24) && !(z >= 8 && z <= 11)) {
                        level.setBlock(origin.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Cryo Pods & Pipes inside Awakening Room (X: 2..10)
        for (int x : new int[]{3, 6, 9}) {
            level.setBlock(origin.offset(x, 1, 2), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(origin.offset(x, 2, 2), Blocks.GLASS.defaultBlockState(), 2);
            level.setBlock(origin.offset(x, 1, 17), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(origin.offset(x, 2, 17), Blocks.GLASS.defaultBlockState(), 2);
        }
        // Overhead industrial pipes in corridor
        for (int x = 13; x <= 24; x++) {
            level.setBlock(origin.offset(x, 5, 7), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(origin.offset(x, 5, 12), Blocks.IRON_BARS.defaultBlockState(), 2);
        }
    }

    private static void buildLowerDeck(ServerLevel level) {
        for (int x = 18; x <= 42; x++) {
            for (int z = 4; z <= 17; z++) {
                boolean isDropZone = (x >= 20 && x <= 24 && z >= 8 && z <= 12);
                level.setBlock(new BlockPos(x, 35, z),
                        isDropZone ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);

                boolean isLight = (x % 5 == 2) && (z % 4 == 2);
                level.setBlock(new BlockPos(x, 41, z),
                        isLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState(), 2);

                for (int y = 36; y <= 40; y++) {
                    if (x == 18 || x == 42 || z == 4 || z == 17) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Backup Generator & Escape Pod
        level.setBlock(new BlockPos(34, 35, 10), Blocks.NETHERITE_BLOCK.defaultBlockState(), 2);
        level.setBlock(new BlockPos(34, 36, 10), ModBlocks.AUXILIARY_ENERGY_CORE.get().defaultBlockState(), 2);

        level.setBlock(new BlockPos(22, 35, 10), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        level.setBlock(new BlockPos(22, 36, 10), ModBlocks.ORBITAL_DROP_POD.get().defaultBlockState(), 2);

        // Pillars & Pipes
        for (int y = 36; y <= 40; y++) {
            level.setBlock(new BlockPos(20, y, 7), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(20, y, 13), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(38, y, 7), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(38, y, 13), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        }

        // Elevator Enclosure Y=36..59
        for (int y = 36; y <= 59; y++) {
            level.setBlock(new BlockPos(34, y, 15), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(37, y, 15), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(new BlockPos(35, y, 14), Blocks.IRON_BARS.defaultBlockState(), 2);
            level.setBlock(new BlockPos(35, y, 16), Blocks.IRON_BARS.defaultBlockState(), 2);
        }
    }

    private static void buildIceOutpostAndMine(ServerLevel level, ServerPlayer player) {
        // 1. Ice Canyon + Ground Research Outpost (X: 102..152, Z: 10..46, Y: 60..72)
        for (int x = 102; x <= 152; x++) {
            for (int z = 10; z <= 46; z++) {
                boolean isRim = (x <= 103 || x >= 151 || z <= 11 || z >= 45);
                boolean isSafeHouse = (x >= 126 && x <= 136 && z >= 20 && z <= 36);
                boolean isPath = (z >= 26 && z <= 30);

                if (isSafeHouse) {
                    level.setBlock(new BlockPos(x, 60, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                } else if (isPath) {
                    level.setBlock(new BlockPos(x, 60, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
                } else {
                    level.setBlock(new BlockPos(x, 60, z), ((x + z) % 5 == 0) ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState(), 2);
                }

                for (int y = 61; y <= 72; y++) {
                    if (isSafeHouse && y == 65) {
                        level.setBlock(new BlockPos(x, y, z), ((x % 4 == 0 && z % 4 == 0) ? Blocks.SEA_LANTERN : Blocks.IRON_BLOCK).defaultBlockState(), 2);
                    } else if (isSafeHouse && y < 65 && (x == 126 || x == 136 || z == 20 || z == 36) && !isPath) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
                    } else if (isRim && y <= 65) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.PACKED_ICE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Crashed Escape Pod debris near (108, 61, 28)
        level.setBlock(new BlockPos(107, 61, 26), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        level.setBlock(new BlockPos(108, 62, 25), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);

        // Wreckage Chest at (110, 61, 25)
        BlockPos wreckChest = new BlockPos(110, 61, 25);
        level.setBlock(wreckChest, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(wreckChest) instanceof Container c && isContainerEmpty(c)) {
            c.setItem(0, new ItemStack(ModItems.EXP_BOTTLE.get(), 6));
            c.setItem(1, new ItemStack(ModItems.CHAR_MAT_1.get(), 4));
            c.setItem(2, EquipmentStatsHelper.createChipWithRarity(ModItems.VOID_DRIVE_CHIP.get(), 2, player.getRandom()));
        }

        // Ice Waypoint at (114, 61, 28)
        level.setBlock(new BlockPos(114, 61, 28), ModBlocks.TELEPORT_WAYPOINT.get().defaultBlockState(), 2);

        // Outpost Safe Room Terminal & Chest
        level.setBlock(new BlockPos(130, 61, 33), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);
        BlockPos outpostChest = new BlockPos(133, 61, 33);
        level.setBlock(outpostChest, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(outpostChest) instanceof Container oc && isContainerEmpty(oc)) {
            oc.setItem(0, EquipmentStatsHelper.createChipWithRarity(ModItems.SERVO_BATTERY_CHIP.get(), 3, player.getRandom()));
            oc.setItem(1, EquipmentStatsHelper.createChipWithRarity(ModItems.OVERCLOCK_CHIP.get(), 2, player.getRandom()));
            oc.setItem(2, EquipmentStatsHelper.createChipWithRarity(ModItems.OVERCLOCK_CHIP.get(), 2, player.getRandom()));
            oc.setItem(3, new ItemStack(ModItems.EQUIP_ESSENCE.get(), 2));
            oc.setItem(4, new ItemStack(ModItems.EXP_BOTTLE.get(), 3));
        }

        // Seal Mine Freight Elevator at (146, 60, 28) until Stage 12
        if (QuestManager.getQuestStage() < 12) {
            level.setBlock(new BlockPos(146, 60, 28), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
        }

        // 2. Underground Frozen Mine (X: 144..176, Z: 14..42, Y: 35..43)
        for (int x = 144; x <= 176; x++) {
            for (int z = 14; z <= 42; z++) {
                boolean isBossDais = (x >= 164 && x <= 172 && z >= 24 && z <= 32);
                level.setBlock(new BlockPos(x, 35, z),
                        isBossDais ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                boolean isLight = (x % 6 == 0) && (z % 6 == 0);
                level.setBlock(new BlockPos(x, 43, z),
                        isLight ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState(), 2);

                for (int y = 36; y < 43; y++) {
                    if (x == 144 || x == 176 || z == 14 || z == 42) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.DEEPSLATE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Mine railway details
        for (int x = 148; x <= 163; x++) {
            level.setBlock(new BlockPos(x, 36, 28), Blocks.RAIL.defaultBlockState(), 2);
        }
        // Deep-Space Comms Console (Boss trigger) at (168, 36, 28)
        level.setBlock(new BlockPos(168, 36, 28), ModBlocks.PRECURSOR_ALTAR.get().defaultBlockState(), 2);
    }
}
