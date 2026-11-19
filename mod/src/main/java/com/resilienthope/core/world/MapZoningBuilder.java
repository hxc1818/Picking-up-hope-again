package com.resilienthope.core.world;

import com.resilienthope.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Zero-lag per-chunk zone guide stamper for 10k x 10k worlds.
 * Records each stamped chunk in per-dimension SavedData so boundaries are drawn once on-demand
 * as chunks load and never overwrite player-built structures or stall server startup.
 */
public final class MapZoningBuilder {
    private static final String DATA_NAME = "picking_hope_zone_guides";

    private record PendingChunk(WeakReference<ServerLevel> levelRef, ResourceKey<Level> dim, ChunkPos pos) {}

    private static final Deque<PendingChunk> PENDING_CHUNKS = new ArrayDeque<>();
    private static final Set<String> PENDING_KEYS = new HashSet<>();

    private MapZoningBuilder() {}

    public static class ZoneGuideSavedData extends SavedData {
        public boolean guidesEnabled = false;
        public final Set<Long> stampedChunks = new HashSet<>();

        public ZoneGuideSavedData() {}

        public static ZoneGuideSavedData load(CompoundTag tag) {
            ZoneGuideSavedData d = new ZoneGuideSavedData();
            d.guidesEnabled = tag.getBoolean("GuidesEnabled");
            long[] arr = tag.getLongArray("StampedChunks");
            for (long v : arr) {
                d.stampedChunks.add(v);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("GuidesEnabled", guidesEnabled);
            long[] arr = new long[stampedChunks.size()];
            int idx = 0;
            for (long v : stampedChunks) {
                arr[idx++] = v;
            }
            tag.putLongArray("StampedChunks", arr);
            return tag;
        }

        public static ZoneGuideSavedData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(ZoneGuideSavedData::load, ZoneGuideSavedData::new, DATA_NAME);
        }
    }

    public static void setGuidesEnabled(ServerLevel level, boolean enabled) {
        if (level == null) return;
        ZoneGuideSavedData data = ZoneGuideSavedData.get(level);
        data.guidesEnabled = enabled;
        data.setDirty();
    }

    public static boolean isGuidesEnabled(ServerLevel level) {
        if (level == null) return false;
        return ZoneGuideSavedData.get(level).guidesEnabled;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof ServerLevel level)) return;
        String dimId = ModDimensions.toZoneDimensionId(level.dimension());
        if (dimId.isEmpty()) return;

        ZoneGuideSavedData data = ZoneGuideSavedData.get(level);
        if (!data.guidesEnabled) return;

        ChunkAccess chunk = event.getChunk();
        ChunkPos cpos = chunk.getPos();
        if (data.stampedChunks.contains(cpos.toLong())) return;

        enqueueChunk(level, cpos);
    }

    private static void enqueueChunk(ServerLevel level, ChunkPos cpos) {
        String uid = level.dimension().location() + "|" + cpos.x + "|" + cpos.z;
        if (PENDING_KEYS.add(uid)) {
            PENDING_CHUNKS.offer(new PendingChunk(new WeakReference<>(level), level.dimension(), cpos));
        }
    }

    public static int stampLoadedChunksAround(ServerPlayer player, int chunkRadius) {
        if (player == null) return 0;
        ServerLevel level = player.getLevel();
        String dimId = ModDimensions.toZoneDimensionId(level.dimension());
        if (dimId.isEmpty()) return 0;

        ZoneGuideSavedData data = ZoneGuideSavedData.get(level);
        ChunkPos center = player.chunkPosition();
        int count = 0;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                if (!level.hasChunk(cx, cz)) continue;
                long key = ChunkPos.asLong(cx, cz);
                if (!data.stampedChunks.contains(key)) {
                    data.stampedChunks.add(key);
                    stampChunkColumns(level, dimId, cx, cz);
                    count++;
                }
            }
        }
        if (count > 0) data.setDirty();
        return count;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_CHUNKS.isEmpty()) return;

        int budget = 16;
        while (budget > 0 && !PENDING_CHUNKS.isEmpty()) {
            PendingChunk item = PENDING_CHUNKS.poll();
            String uid = item.dim.location() + "|" + item.pos.x + "|" + item.pos.z;
            PENDING_KEYS.remove(uid);

            ServerLevel level = item.levelRef.get();
            if (level == null || level.getServer() == null) continue;
            if (!level.hasChunk(item.pos.x, item.pos.z)) continue;

            String dimId = ModDimensions.toZoneDimensionId(level.dimension());
            if (dimId.isEmpty()) continue;

            ZoneGuideSavedData data = ZoneGuideSavedData.get(level);
            if (!data.guidesEnabled) continue;
            long key = item.pos.toLong();
            if (data.stampedChunks.contains(key)) continue;

            data.stampedChunks.add(key);
            data.setDirty();
            stampChunkColumns(level, dimId, item.pos.x, item.pos.z);
            budget--;
        }
    }

    private static void stampChunkColumns(ServerLevel level, String dimId, int cx, int cz) {
        List<MapZoneRegistry.ZoneDef> zones = MapZoneRegistry.getZonesForDimId(dimId);
        if (zones.isEmpty()) return;

        int minChunkX = cx << 4;
        int minChunkZ = cz << 4;
        int maxChunkX = minChunkX + 15;
        int maxChunkZ = minChunkZ + 15;

        int[] bounds = MapZoneRegistry.getBoundsForDimId(dimId);
        if (maxChunkX < bounds[0] - 8 || minChunkX > bounds[1] + 8
                || maxChunkZ < bounds[2] - 8 || minChunkZ > bounds[3] + 8) {
            return;
        }

        BlockState routeMarker = ModBlocks.HAZARD_STRIPE_BLOCK.get().defaultBlockState();
        BlockState lightBlock = Blocks.SEA_LANTERN.defaultBlockState();

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                boolean stampedZoneBorder = false;

                for (MapZoneRegistry.ZoneDef zone : zones) {
                    if (x < zone.minX || x > zone.maxX || z < zone.minZ || z > zone.maxZ) continue;

                    if (x == zone.centerX && z == zone.centerZ) {
                        ensureZoneLandingPad(level, zone);
                    }

                    if (zone.isOnOuterBorder(x, z)) {
                        stampedZoneBorder = true;
                        BlockState borderState = resolveBlockState(zone.borderBlockId, Blocks.CYAN_CONCRETE);
                        placeIfGuideReplaceable(level, new BlockPos(x, zone.centerY, z), borderState);
                        if ("s4".equals(zone.id)) {
                            placeIfGuideReplaceable(level, new BlockPos(x, 40, z), borderState);
                        }

                        if (zone.isLandmarkPillarSpot(x, z)) {
                            for (int dy = 1; dy <= 4; dy++) {
                                placeIfGuideReplaceable(level, new BlockPos(x, zone.centerY + dy, z), borderState);
                            }
                            placeIfGuideReplaceable(level, new BlockPos(x, zone.centerY + 5, z), lightBlock);
                            placeGuideSign(level, new BlockPos(x, zone.centerY + 6, z),
                                    "[" + zone.code + "] " + zone.name,
                                    "尺寸 " + zone.sizeLabel(),
                                    zone.verticalNote,
                                    "(" + x + ", " + z + ")");
                        }
                    }
                }

                if (!stampedZoneBorder && MapZoneRegistry.isOnRouteDashedLine(dimId, x, z)) {
                    int ry = "starship".equals(dimId) ? 60 : 63;
                    placeIfGuideReplaceable(level, new BlockPos(x, ry, z), routeMarker);
                }
            }
        }
    }

    public static void ensureZoneLandingPad(ServerLevel level, MapZoneRegistry.ZoneDef zone) {
        if (level == null || zone == null) return;
        BlockState pad = "starship".equals(zone.dimensionId)
                ? ModBlocks.STARSHIP_FLOOR.get().defaultBlockState()
                : ModBlocks.OUTPOST_INSULATED_WALL.get().defaultBlockState();
        BlockState rim = resolveBlockState(zone.borderBlockId, Blocks.CYAN_CONCRETE);

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos p = new BlockPos(zone.centerX + dx, zone.centerY, zone.centerZ + dz);
                boolean isCornerOrEdge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                placeIfGuideReplaceable(level, p, isCornerOrEdge ? rim : pad);
            }
        }
        BlockPos beacon = new BlockPos(zone.centerX, zone.centerY + 1, zone.centerZ);
        placeIfGuideReplaceable(level, beacon, Blocks.SEA_LANTERN.defaultBlockState());
        placeGuideSign(level, new BlockPos(zone.centerX, zone.centerY + 2, zone.centerZ),
                "== " + zone.code + " 区中心 ==",
                zone.name,
                zone.sizeLabel() + " H=" + zone.guideHeight,
                "(" + zone.centerX + "," + zone.centerY + "," + zone.centerZ + ")");
    }

    public static void ensureSafeFloorAt(ServerLevel level, int x, int floorY, int z) {
        if (level == null) return;
        BlockState pad = ModBlocks.STARSHIP_FLOOR.get().defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(x + dx, floorY, z + dz);
                if (level.getBlockState(p).isAir()) {
                    level.setBlock(p, pad, 2);
                }
            }
        }
    }

    private static void placeIfGuideReplaceable(ServerLevel level, BlockPos pos, BlockState target) {
        BlockState cur = level.getBlockState(pos);
        if (cur.isAir() || cur.is(Blocks.SNOW_BLOCK) || cur.is(Blocks.SNOW)) {
            level.setBlock(pos, target, 2);
        }
    }

    private static void placeGuideSign(ServerLevel level, BlockPos pos, String l0, String l1, String l2, String l3) {
        if (!level.getBlockState(pos).isAir()) return;
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            sign.setMessage(0, new TextComponent(trimSign(l0)));
            sign.setMessage(1, new TextComponent(trimSign(l1)));
            sign.setMessage(2, new TextComponent(trimSign(l2)));
            sign.setMessage(3, new TextComponent(trimSign(l3)));
            sign.setColor(DyeColor.WHITE);
            sign.setHasGlowingText(true);
            sign.setChanged();
        }
    }

    private static String trimSign(String s) {
        if (s == null) return "";
        return s.length() > 14 ? s.substring(0, 14) : s;
    }

    private static BlockState resolveBlockState(String id, Block fallback) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Block b = rl != null ? ForgeRegistries.BLOCKS.getValue(rl) : null;
        return (b != null && b != Blocks.AIR) ? b.defaultBlockState() : fallback.defaultBlockState();
    }
}
