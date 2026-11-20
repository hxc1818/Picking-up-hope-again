package com.resilienthope.core.event;

import com.resilienthope.core.account.AccountSessionManager;
import com.resilienthope.core.account.CloudSaveSnapshot;
import com.resilienthope.core.combat.TurnBasedCombatManager;
import com.resilienthope.core.entity.FactionNpcEntity;
import com.resilienthope.core.entity.PrecursorMechBossEntity;
import com.resilienthope.core.entity.SecurityDroneEntity;
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
import com.resilienthope.core.world.HopeMapCommand;
import com.resilienthope.core.world.MapZoneRegistry;
import com.resilienthope.core.world.MapZoningBuilder;
import com.resilienthope.core.world.ModDimensions;
import com.resilienthope.core.world.prefab.PrefabPlacementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class StarshipSpawnHandler {
    private static final String SPAWN_TAG = "picking_hope_v2_clean_init_v4";
    private static final String V2_WORLD_TAG = "picking_hope_v2_custom_world_v4";
    private static final String BUILDER_INIT_TAG = "picking_hope_builder_inited_v1";

    private static boolean isBuilderSession(ServerPlayer player, ServerLevel level) {
        if (player == null) return false;
        if (player.getTags().contains(HopeMapCommand.BUILDER_TAG)) return true;
        if (level != null && level.getServer() != null) {
            String lvlName = level.getServer().getWorldData().getLevelName();
            if (lvlName != null && lvlName.contains("地图造景工坊")) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        ServerLevel level = player.getLevel();
        if (isBuilderSession(player, level)) {
            handleBuilderLogin(player, level);
            return;
        }
        BlockPos origin = new BlockPos(0, 60, 0);
        boolean freshReset = !player.getTags().contains(SPAWN_TAG);
        CloudSaveSnapshot pendingCloud = AccountSessionManager.consumePendingRestore();
        long localSyncedAt = player.getPersistentData().getLong("PickingHopeCloudSyncAt");
        boolean shouldRestoreCloud = pendingCloud != null && (freshReset || pendingCloud.updatedAt > localSyncedAt);
        boolean restoredFromCloud = false;

        if (shouldRestoreCloud) {
            player.setGameMode(GameType.ADVENTURE);
            clearOldWorldEntities(level);
            CloudSaveSnapshot.applyToPlayer(player, pendingCloud);
            if (!player.getTags().contains(V2_WORLD_TAG)
                    || level.getBlockState(new BlockPos(6, 60, 10)).isAir()
                    || level.getBlockState(new BlockPos(168, 36, 28)).isAir()) {
                buildExpandedStarship(level, origin);
                ensureUpperShipProps(level, origin);
                buildLowerDeck(level);
                buildIceOutpostAndMine(level);
                player.addTag(V2_WORLD_TAG);
            }
            restoredFromCloud = true;
        } else if (freshReset) {
            // Unconditionally reset legacy Creative mode, inventory, quest NBT, and Lv.20 capability!
            player.setGameMode(GameType.ADVENTURE);
            player.getInventory().clearContent();
            player.getPersistentData().remove("PickingHopeQuest");

            List<String> oldTags = player.getTags().stream()
                    .filter(t -> t.startsWith("picking_hope_") || t.startsWith("hope_crate_"))
                    .toList();
            for (String oldTag : oldTags) {
                player.removeTag(oldTag);
            }

            clearOldWorldEntities(level);

            ItemStack starterSword = new ItemStack(ModItems.PLASMA_CUTTER.get());
            EquipmentStatsHelper.ensureStats(starterSword, player.getRandom());
            player.getInventory().setItem(0, starterSword);
            player.setItemInHand(InteractionHand.MAIN_HAND, starterSword);

            player.getCapability(TeamCapability.TEAM_CAPABILITY).ifPresent(team -> {
                team.resetToFreshV2();
                team.getCharacter(0).setEquippedWeapon(starterSword);
                team.syncHealthToPlayer(player);
            });

            QuestManager.resetAll();
            StoryDirector.saveAndSyncQuest(player, false);

            buildExpandedStarship(level, origin);
            ensureUpperShipProps(level, origin);
            buildLowerDeck(level);
            buildIceOutpostAndMine(level);

            player.teleportTo(level, 6.5, 61.0, 10.5, 270.0f, 0.0f);
            player.setRespawnPosition(level.dimension(), new BlockPos(6, 61, 10), 270.0f, true, false);
            player.addTag(SPAWN_TAG);
            player.addTag(V2_WORLD_TAG);
            AccountSessionManager.scheduleAutoPush(player, "fresh_init");
        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                player.setGameMode(GameType.ADVENTURE);
            }
            StoryDirector.loadQuest(player);

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

            if (!player.getTags().contains(V2_WORLD_TAG)
                    || level.getBlockState(new BlockPos(6, 60, 10)).isAir()
                    || level.getBlockState(new BlockPos(168, 36, 28)).isAir()) {
                buildExpandedStarship(level, origin);
                ensureUpperShipProps(level, origin);
                buildLowerDeck(level);
                buildIceOutpostAndMine(level);
                player.addTag(V2_WORLD_TAG);
            }
            if (pendingCloud == null || localSyncedAt > pendingCloud.updatedAt) {
                AccountSessionManager.scheduleAutoPush(player, "login_sync");
            }
        }

        int stage = QuestManager.getQuestStage();
        if (stage >= 2) StoryDirector.openSleepCorridorGate(level);
        if (stage >= 3) StoryDirector.openHallCorridorGate(level);
        if (stage >= 4) StoryDirector.ensureNpc(level, "chen", 31.5, 61.0, 14.5, -145.0f);
        if (stage >= 9) StoryDirector.openElevatorShaft(level);
        if (stage >= 10) StoryDirector.ensureNpc(level, "su", 30.5, 36.0, 7.5, -45.0f);
        if (stage >= 17) StoryDirector.ensureNpc(level, "zheng", 131.5, 61.0, 31.5, 135.0f);
        if (stage >= 21) StoryDirector.openMineElevatorShaft(level);

        PacketSyncTeamData.syncToPlayer(player);

        if (stage == 0) {
            PacketHandler.sendToPlayer(player, new PacketRadioDialogue("船舱应急广播",
                    "警告：重拾希望号遭遇不明撞击，B区供电中断。林野，先按 [F] 开启身旁的【装甲物资箱】检查装备！", 200));
        } else if (restoredFromCloud) {
            PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("云存档同步",
                    "已加载账号进度（主线阶段 " + stage + "），欢迎回到舰区！"));
        }
    }

    private static void handleBuilderLogin(ServerPlayer player, ServerLevel currentLevel) {
        player.addTag(HopeMapCommand.BUILDER_TAG);
        player.setGameMode(GameType.CREATIVE);
        if (player.getAbilities().getFlyingSpeed() <= 0.055F) {
            player.getAbilities().setFlyingSpeed(0.10F);
        }
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        MinecraftServer server = currentLevel.getServer();
        ServerLevel starship = server != null ? server.getLevel(ModDimensions.STARSHIP) : null;
        ServerLevel planet = server != null ? server.getLevel(ModDimensions.PLANET_CRYO) : null;
        if (starship != null) MapZoningBuilder.setGuidesEnabled(starship, true);
        if (planet != null) MapZoningBuilder.setGuidesEnabled(planet, true);

        if (!player.getTags().contains(BUILDER_INIT_TAG)) {
            player.addTag(BUILDER_INIT_TAG);
            ServerLevel dest = starship != null ? starship : currentLevel;
            MapZoneRegistry.ZoneDef s1 = MapZoneRegistry.findById("s1");
            if (s1 != null) {
                MapZoningBuilder.ensureZoneLandingPad(dest, s1);
                player.teleportTo(dest, s1.centerX + 2.5D, s1.centerY + 1.0D, s1.centerZ + 2.5D, 270.0F, 0.0F);
            }
            MapZoningBuilder.stampLoadedChunksAround(player, 4);
        }

        PacketHandler.sendToPlayer(player, PacketRadioDialogue.toast("造景工坊",
                "已启用造景模式！按 [M] 看蓝图，[K] 选 28 种预制结构"));
        PrefabPlacementManager.syncClipboardToClient(player);
        boolean hasWand = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.PREFAB_PROJECTOR.get())) {
                hasWand = true;
                break;
            }
        }
        if (!hasWand) {
            PrefabPlacementManager.giveProjector(player);
        }
        player.sendMessage(new TextComponent("§a[造景工坊] §f已开启双维度（星舰5区 + 1万×1万第一颗星球8区）动态区域划线与 28 种模块化结构工具。"), player.getUUID());
        player.sendMessage(new TextComponent("§7常用操作：§e[M]§7 全图与传送 | §e[K]§7 可复用结构库 | §e[E]§7 130种方块库 | 手持§b[蓝图投影仪]§7左键选模、右键放置、Shift+右旋转、Shift+左撤回"), player.getUUID());
    }

    private static void clearOldWorldEntities(ServerLevel level) {
        AABB shipAndPlanet = new AABB(-10, 20, -10, 190, 90, 60);
        for (FactionNpcEntity npc : level.getEntitiesOfClass(FactionNpcEntity.class, shipAndPlanet)) {
            npc.discard();
        }
        for (SecurityDroneEntity drone : level.getEntitiesOfClass(SecurityDroneEntity.class, shipAndPlanet)) {
            drone.discard();
        }
        for (PrecursorMechBossEntity boss : level.getEntitiesOfClass(PrecursorMechBossEntity.class, shipAndPlanet)) {
            boss.discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            TurnBasedCombatManager.clearSessionOnLogout(player);
            if (!isBuilderSession(player, player.getLevel())) {
                AccountSessionManager.flushSyncBlocking(player, "logout", 2500);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide() && event.player instanceof ServerPlayer player) {
            if (player.isCreative() || isBuilderSession(player, player.getLevel())) {
                return;
            }
            if (!player.isSpectator() && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                player.setGameMode(GameType.ADVENTURE);
            }

            // Void rescue
            if (player.getY() < 10.0) {
                if (player.getX() > 85.0) {
                    player.teleportTo(player.getLevel(), 108.5, 61.0, 28.5, 270.0f, 0.0f);
                } else {
                    player.teleportTo(player.getLevel(), 34.5, 61.0, 12.5, 270.0f, 0.0f);
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

        // Player left-clicks Monster -> Ambush Turn-Based Battle! (Skip in creative builder mode)
        if (event.getSource().getDirectEntity() instanceof ServerPlayer player
                && event.getEntity() instanceof Monster target) {
            if (player.isCreative()) return;
            event.setCanceled(true);
            if (!TurnBasedCombatManager.isInBattle(player)) {
                TurnBasedCombatManager.startOverworldBattle(player, target, true);
            }
            return;
        }

        // Monster hits Player -> Surprised Turn-Based Battle!
        if (event.getSource().getEntity() instanceof Monster attacker
                && event.getEntity() instanceof ServerPlayer player) {
            if (player.isCreative()) return;
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

    private static void ensureUpperShipProps(ServerLevel level, BlockPos origin) {
        // Sleep Room Crate (7, 61, 10) & Gate Switch Terminal (11, 61, 8)
        level.setBlock(origin.offset(7, 1, 10), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(11, 1, 8), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);

        // North Storage Alcove Crate (20, 61, 3) & Conduits
        level.setBlock(origin.offset(20, 1, 3), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(18, 1, 2), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(21, 1, 2), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);

        // Relay Hall Props: Holotable (31, 61, 10), Waypoint (34, 61, 10), Terminal (38, 61, 14)
        level.setBlock(origin.offset(31, 1, 10), ModBlocks.HOLOTABLE_PROP.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(34, 1, 10), ModBlocks.TELEPORT_WAYPOINT.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(38, 1, 14), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);

        // Bridge Data Room Props: Holotable (43, 61, 4) & Crate (44, 61, 3)
        level.setBlock(origin.offset(43, 1, 4), ModBlocks.HOLOTABLE_PROP.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(44, 1, 3), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);

        // Seal Bulkhead Doors & Elevator until unlocked
        if (QuestManager.getQuestStage() < 2) {
            for (int y = 61; y <= 63; y++) {
                level.setBlock(new BlockPos(12, y, 9), ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(12, y, 10), ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState(), 2);
            }
        }
        if (QuestManager.getQuestStage() < 3) {
            for (int y = 61; y <= 63; y++) {
                level.setBlock(new BlockPos(25, y, 9), ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(25, y, 10), ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState(), 2);
            }
        }
        if (QuestManager.getQuestStage() < 9) {
            level.setBlock(new BlockPos(35, 60, 15), ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(36, 60, 15), ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState(), 2);
        }
    }

    private static void buildExpandedStarship(ServerLevel level, BlockPos origin) {
        int sizeX = 48;
        int sizeZ = 20;

        BlockState floor = ModBlocks.STARSHIP_FLOOR.get().defaultBlockState();
        BlockState wall = ModBlocks.STARSHIP_WALL.get().defaultBlockState();
        BlockState hull = ModBlocks.STARSHIP_HULL.get().defaultBlockState();
        BlockState hazard = ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState();
        BlockState light = ModBlocks.STARSHIP_LIGHT.get().defaultBlockState();
        BlockState alarm = ModBlocks.ALARM_BEACON.get().defaultBlockState();
        BlockState glass = ModBlocks.VIEWPORT_GLASS.get().defaultBlockState();

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                boolean isHazard = (x == 12 || x == 25 || (x >= 34 && x <= 37 && z >= 14 && z <= 16));
                level.setBlock(origin.offset(x, 0, z), isHazard ? hazard : floor, 2);

                boolean isLight = (x % 6 == 3) && (z % 5 == 2);
                boolean isRedAlarm = (x >= 13 && x <= 24) && (x % 4 == 1) && (z == 8 || z == 11);
                level.setBlock(origin.offset(x, 6, z), isRedAlarm ? alarm : (isLight ? light : hull), 2);

                boolean inStorageAlcove = (x >= 17 && x <= 22 && z >= 1 && z <= 6);
                boolean isBridgePartition = (x == 39 && z >= 0 && z <= 8 && !(z >= 3 && z <= 5));

                for (int y = 1; y < 6; y++) {
                    if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                        boolean isViewport = ((x == 0 && z >= 7 && z <= 12) || (z == sizeZ - 1 && x >= 29 && x <= 42 && x % 4 != 0))
                                && (y == 2 || y == 3);
                        level.setBlock(origin.offset(x, y, z), isViewport ? glass : hull, 2);
                    } else if ((x >= 13 && x <= 24) && !(z >= 7 && z <= 12) && !inStorageAlcove) {
                        level.setBlock(origin.offset(x, y, z), wall, 2);
                    } else if (x == 12 && !((z == 9 || z == 10) && y <= 3)) {
                        level.setBlock(origin.offset(x, y, z), wall, 2);
                    } else if (x == 25 && !((z == 9 || z == 10) && y <= 3)) {
                        level.setBlock(origin.offset(x, y, z), wall, 2);
                    } else if (isBridgePartition) {
                        boolean isWindow = (y == 2 || y == 3) && (z == 1 || z == 7);
                        level.setBlock(origin.offset(x, y, z), isWindow ? glass : wall, 2);
                    } else if (x >= 39 && z == 8) {
                        level.setBlock(origin.offset(x, y, z), (y == 2 || y == 3) ? glass : wall, 2);
                    } else {
                        level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Open doorway between Corridor (z=7) and North Storage Alcove (x=19..20, z=6)
        for (int y = 1; y <= 3; y++) {
            level.setBlock(origin.offset(19, y, 7), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(origin.offset(20, y, 7), Blocks.AIR.defaultBlockState(), 2);
        }

        // Cryo Sleep Pods & Structural Conduits in Awakening Room (X: 2..10)
        for (int x : new int[]{3, 6, 9}) {
            level.setBlock(origin.offset(x, 1, 2), ModBlocks.CRYO_SLEEP_POD.get().defaultBlockState(), 2);
            level.setBlock(origin.offset(x, 1, 17), ModBlocks.CRYO_SLEEP_POD.get().defaultBlockState(), 2);
        }
        for (int y = 1; y <= 5; y++) {
            level.setBlock(origin.offset(2, y, 5), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(origin.offset(2, y, 14), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(origin.offset(28, y, 3), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(origin.offset(28, y, 16), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(origin.offset(37, y, 16), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(origin.offset(45, y, 2), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
        }
    }

    private static void buildLowerDeck(ServerLevel level) {
        BlockState floor = ModBlocks.STARSHIP_FLOOR.get().defaultBlockState();
        BlockState wall = ModBlocks.STARSHIP_WALL.get().defaultBlockState();
        BlockState hull = ModBlocks.STARSHIP_HULL.get().defaultBlockState();
        BlockState hazard = ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState();
        BlockState light = ModBlocks.STARSHIP_LIGHT.get().defaultBlockState();
        BlockState glass = ModBlocks.VIEWPORT_GLASS.get().defaultBlockState();

        for (int x = 18; x <= 42; x++) {
            for (int z = 4; z <= 17; z++) {
                boolean isDropZone = (x >= 20 && x <= 24 && z >= 8 && z <= 12);
                level.setBlock(new BlockPos(x, 35, z), isDropZone ? hazard : floor, 2);

                boolean isLight = (x % 5 == 2) && (z % 4 == 2);
                level.setBlock(new BlockPos(x, 41, z), isLight ? light : hull, 2);

                for (int y = 36; y <= 40; y++) {
                    if (x == 18 || x == 42 || z == 4 || z == 17) {
                        boolean isWin = (x == 18 && z >= 8 && z <= 12 && (y == 37 || y == 38));
                        level.setBlock(new BlockPos(x, y, z), isWin ? glass : wall, 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Backup Generator Core & Orbital Escape Pod
        level.setBlock(new BlockPos(34, 35, 10), hull, 2);
        level.setBlock(new BlockPos(34, 36, 10), ModBlocks.AUXILIARY_ENERGY_CORE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(22, 35, 10), hazard, 2);
        level.setBlock(new BlockPos(22, 36, 10), ModBlocks.ORBITAL_DROP_POD.get().defaultBlockState(), 2);

        // Medbay Crate at (32, 36, 5)
        level.setBlock(new BlockPos(32, 36, 5), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);

        // Structural Conduits
        for (int y = 36; y <= 40; y++) {
            level.setBlock(new BlockPos(20, y, 7), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(20, y, 13), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(38, y, 7), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(38, y, 13), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
        }

        // Enclosed Elevator Shaft Y=36..59
        for (int y = 36; y <= 59; y++) {
            level.setBlock(new BlockPos(34, y, 15), wall, 2);
            level.setBlock(new BlockPos(37, y, 15), (y >= 41) ? wall : Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(35, y, 14), glass, 2);
            level.setBlock(new BlockPos(36, y, 14), glass, 2);
            level.setBlock(new BlockPos(35, y, 16), wall, 2);
            level.setBlock(new BlockPos(36, y, 16), wall, 2);
        }
    }

    private static void buildIceOutpostAndMine(ServerLevel level) {
        BlockState snow = ModBlocks.GLACIER_SNOW.get().defaultBlockState();
        BlockState permafrost = ModBlocks.PERMAFROST_ROCK.get().defaultBlockState();
        BlockState ore = ModBlocks.FROZEN_ORE_VEIN.get().defaultBlockState();
        BlockState floor = ModBlocks.STARSHIP_FLOOR.get().defaultBlockState();
        BlockState wall = ModBlocks.STARSHIP_WALL.get().defaultBlockState();
        BlockState hull = ModBlocks.STARSHIP_HULL.get().defaultBlockState();
        BlockState hazard = ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState();
        BlockState light = ModBlocks.STARSHIP_LIGHT.get().defaultBlockState();
        BlockState glass = ModBlocks.VIEWPORT_GLASS.get().defaultBlockState();

        // 1. Ice Canyon + Ground Research Outpost (X: 102..152, Z: 10..46, Y: 60..72)
        for (int x = 102; x <= 152; x++) {
            for (int z = 10; z <= 46; z++) {
                boolean isRim = (x <= 103 || x >= 151 || z <= 11 || z >= 45);
                boolean isSafeHouse = (x >= 126 && x <= 136 && z >= 20 && z <= 36);
                boolean isSubstation = (x >= 140 && x <= 145 && z >= 23 && z <= 32);
                boolean isPath = (z >= 26 && z <= 30);

                if (isSafeHouse || isSubstation) {
                    level.setBlock(new BlockPos(x, 60, z), isSubstation ? floor : hull, 2);
                } else if (isPath) {
                    level.setBlock(new BlockPos(x, 60, z), floor, 2);
                } else {
                    level.setBlock(new BlockPos(x, 60, z), ((x + z) % 4 == 0) ? permafrost : snow, 2);
                }

                for (int y = 61; y <= 72; y++) {
                    if (isSafeHouse && y == 65) {
                        level.setBlock(new BlockPos(x, y, z), (x % 4 == 0 && z % 4 == 0) ? light : hull, 2);
                    } else if (isSafeHouse && y < 65 && (x == 126 || x == 136 || z == 20 || z == 36) && !isPath) {
                        boolean isWin = (y == 62 || y == 63) && (z == 22 || z == 34 || x == 129 || x == 133);
                        level.setBlock(new BlockPos(x, y, z), isWin ? glass : wall, 2);
                    } else if (isRim && y <= 66) {
                        boolean isRimOre = (y == 63) && ((x + z) % 7 == 0);
                        level.setBlock(new BlockPos(x, y, z), isRimOre ? ore : ((y >= 65) ? snow : permafrost), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Crashed Escape Pod wreckage near (108, 61, 28)
        level.setBlock(new BlockPos(107, 61, 25), hull, 2);
        level.setBlock(new BlockPos(107, 62, 25), hazard, 2);
        level.setBlock(new BlockPos(108, 61, 24), wall, 2);

        // Wreckage Crate (110, 61, 25) & Ice Waypoint (114, 61, 28)
        level.setBlock(new BlockPos(110, 61, 25), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(114, 61, 28), ModBlocks.TELEPORT_WAYPOINT.get().defaultBlockState(), 2);

        // Outpost Safe Room Terminal, Holotable & Crate
        level.setBlock(new BlockPos(130, 61, 33), ModBlocks.ENGINEERING_TERMINAL.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(133, 61, 33), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(130, 61, 24), ModBlocks.HOLOTABLE_PROP.get().defaultBlockState(), 2);

        // East Substation Core (143, 61, 25)
        level.setBlock(new BlockPos(143, 60, 25), hazard, 2);
        level.setBlock(new BlockPos(143, 61, 25), ModBlocks.AUXILIARY_ENERGY_CORE.get().defaultBlockState(), 2);

        // Seal Mine Freight Elevator at (146..147, 60, 28) until Stage 21
        if (QuestManager.getQuestStage() < 21) {
            level.setBlock(new BlockPos(146, 60, 28), hazard, 2);
            level.setBlock(new BlockPos(147, 60, 28), hazard, 2);
        }

        // 2. Underground Frozen Mine (X: 144..176, Z: 14..42, Y: 35..43)
        for (int x = 144; x <= 176; x++) {
            for (int z = 14; z <= 42; z++) {
                boolean isBossDais = (x >= 164 && x <= 172 && z >= 24 && z <= 32);
                boolean isDaisRim = isBossDais && (x == 164 || x == 172 || z == 24 || z == 32);
                boolean isMineRoad = (z >= 27 && z <= 29 && x >= 148 && x <= 163);
                level.setBlock(new BlockPos(x, 35, z),
                        isDaisRim ? hazard : (isBossDais ? hull : (isMineRoad ? floor : permafrost)), 2);

                boolean isLight = (x % 6 == 0) && (z % 6 == 0);
                level.setBlock(new BlockPos(x, 43, z),
                        isLight ? light : (((x + z) % 9 == 0) ? ore : permafrost), 2);

                for (int y = 36; y < 43; y++) {
                    if (x == 144 || x == 176 || z == 14 || z == 42) {
                        boolean crystal = (y == 38 || y == 39) && ((x + z) % 5 == 0);
                        level.setBlock(new BlockPos(x, y, z), crystal ? ore : permafrost, 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Enclosed Mine Freight Elevator Shaft Y=36..59 around (146..147, y, 28)
        for (int y = 36; y <= 59; y++) {
            level.setBlock(new BlockPos(145, y, 28), permafrost, 2);
            level.setBlock(new BlockPos(148, y, 28), (y >= 41) ? permafrost : Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(146, y, 27), wall, 2);
            level.setBlock(new BlockPos(147, y, 27), wall, 2);
            level.setBlock(new BlockPos(146, y, 29), wall, 2);
            level.setBlock(new BlockPos(147, y, 29), wall, 2);
        }

        // Mine Conduits, Crate (158, 36, 24) & Comms Console (168, 36, 28)
        for (int y = 36; y <= 42; y++) {
            level.setBlock(new BlockPos(156, y, 23), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(156, y, 33), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(164, y, 23), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(164, y, 33), ModBlocks.CONDUIT_PILLAR.get().defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(158, 36, 24), ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(168, 36, 28), ModBlocks.PRECURSOR_ALTAR.get().defaultBlockState(), 2);
    }
}
